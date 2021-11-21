package com.punchlab.punchclassifier

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.media.*
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.punchlab.punchclassifier.data.Device
import com.punchlab.punchclassifier.data.Person
import com.punchlab.punchclassifier.ml.ModelType
import com.punchlab.punchclassifier.ml.MoveNet
import com.punchlab.punchclassifier.ml.PunchRnnClassifier
import java.util.*
import kotlin.concurrent.schedule

class DecoderAsync(context: Context,
                   private val responseHandler: Handler,
                   onVideoProcessFinish: () -> Unit) {
    companion object {
        private const val TAG = "DecoderAsync"
        private const val TARGET_WIDTH = 384
        private const val TARGET_HEIGHT = 256
    }

    private var mWidth: Int = -1
    private var mHeight: Int = -1
    private var frameCounter = 0
    private var frameTotalNumber = -1
    private var appContext: Context = context

    private var decoderHandler: Handler
    private val decoderHandlerThread: HandlerThread = HandlerThread(TAG)


    private val uiCallback = onVideoProcessFinish
    private val converter: YuvToRgbConverter = YuvToRgbConverter(appContext)
    private val poseDetector: MoveNet = MoveNet.create(appContext, Device.CPU, ModelType.Lightning)
    private val punchClassifier: PunchRnnClassifier = PunchRnnClassifier.create(appContext)

    val bitmapList = mutableListOf<Bitmap>()
    val personList = mutableListOf<Person>()
    val punchIdxList = mutableListOf<Int>()

    private lateinit var mExtractor : MediaExtractor
    private lateinit var mDecoder: MediaCodec
    private lateinit var mOutputFormat: MediaFormat
    private lateinit var imageBitmap: Bitmap

    init {
        decoderHandlerThread.start()
        decoderHandlerThread.looper
        decoderHandler = Handler(decoderHandlerThread.looper)
        setupDecoder()
    }

    private fun setupDecoder() {
        mDecoder = MediaCodec.createDecoderByType("video/avc")
        val cb = object: MediaCodec.Callback(){
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                val inBuffer = codec.getInputBuffer(index)!!
                val sampleSize = mExtractor.readSampleData(inBuffer, 0)
                val advance = mExtractor.advance()
                if (advance && sampleSize > 0) {
                    codec.queueInputBuffer(index, 0, sampleSize, mExtractor.sampleTime, 0)
                } else {
                    Log.d(TAG, "Advance: $advance , sampleSize: $sampleSize, index: $index BUFFER_FLAG_END_OF_STREAM")
                    codec.queueInputBuffer(index, 0, 0, 0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM)

                }
            }

            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int,
                                                 info: MediaCodec.BufferInfo) {
                val image = codec.getOutputImage(index)
                image?.let { processFrame(it) }
                if (index >= 0) {
                    codec.releaseOutputBuffer(index, false)
                    Log.d(TAG, "release buffer # $index")
                }
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                Log.e(TAG, e.stackTraceToString())
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                Log.d(TAG, "Format changed to: $format")
                mOutputFormat = format
            }
        }
        mDecoder.setCallback(cb, decoderHandler)
    }


    fun processVideo(uri: Uri) {
        setupDecoder()
        val fd = appContext.contentResolver.openFileDescriptor(uri, "r")
        Log.d(TAG, "File descriptor: $fd")
        if (fd != null) {
            mExtractor = MediaExtractor()
            mExtractor.setDataSource(fd.fileDescriptor)
        } else {
            Log.e(TAG, "Can`t open video file")
            return
        }
        mExtractor.selectTrack(0)
        mOutputFormat = mExtractor.getTrackFormat(0)
        frameTotalNumber = mOutputFormat.getInteger("frame-count")
        frameCounter = 0
        Log.d(TAG, "Media format: $mOutputFormat")

        mWidth = mOutputFormat.getInteger(MediaFormat.KEY_WIDTH)
        if (mOutputFormat.containsKey("crop-left") && mOutputFormat.containsKey("crop-right")) {
            mWidth = mOutputFormat.getInteger("crop-right") + 1 - mOutputFormat.getInteger("crop-left")
        }
        var mHeight: Int = mOutputFormat.getInteger(MediaFormat.KEY_HEIGHT)
        if (mOutputFormat.containsKey("crop-top") && mOutputFormat.containsKey("crop-bottom")) {
            mHeight = mOutputFormat.getInteger("crop-bottom") + 1 - mOutputFormat.getInteger("crop-top")
        }

        imageBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888)

        mDecoder.configure(mOutputFormat, null, null, 0)
        Log.d(TAG, "Input format: ${mDecoder.inputFormat}")
        mDecoder.start()
    }

    private fun processFrame(image: Image) {
        converter.yuvToRgb(image, imageBitmap)

        val scaledBitmap = Bitmap.createScaledBitmap(
            imageBitmap,
            TARGET_WIDTH,
            TARGET_HEIGHT, false
        )

        val rotateMatrix = Matrix()
        rotateMatrix.postRotate(90.0f)
        val rotatedBitmap = Bitmap.createBitmap(
            scaledBitmap, 0, 0,
            TARGET_WIDTH,
            TARGET_HEIGHT,
            rotateMatrix, false
        )
        bitmapList.add(rotatedBitmap)
        val person = poseDetector.estimateSinglePose(rotatedBitmap)
        personList.add(person)

        frameCounter += 1
        if (frameCounter == frameTotalNumber) {
            Timer().schedule(1000){
                mDecoder.stop()
                mDecoder.release()
                Log.d(TAG, "All job is done!!!")
            }
            processPunches()
            responseHandler.post {
                uiCallback()
            }
        }
    }

    private fun processPunches(sampleSize: Int =30, step: Int =30) {
        val pointsList = personList.map{extractKeyPoints(it, mWidth, mHeight)}
        val winPointsList = pointsList.windowed(sampleSize, step, partialWindows = false)
        val winPredicted = winPointsList.map{ punchClassifier.classify(it) }

        val numBatches = personList.size / sampleSize
        for (i in 0 until numBatches) {
            punchIdxList.addAll(winPredicted[i])
        }
        // todo: add remainder to array

    }

    /** Extract key points from Person and prepare for tflite model:
     * normalize to bitmap width and height,
     * subtract middle point b/w left and right hips */
    fun extractKeyPoints(person: Person, height: Int, width: Int): FloatArray {
        val keyPointsF = person.keyPoints.map { it.coordinate}.toMutableList()
        val lHip = keyPointsF[11]
        val rHip = keyPointsF[12]
        val mPoint = PointF((lHip.x + rHip.x) / 2, (lHip.y + rHip.y) / 2)
        val flatKP = keyPointsF.flatMap {
            listOf((it.y - mPoint.y) / height, (it.x - mPoint.x) / width) }.toMutableList()
        flatKP.add(mPoint.y)
        flatKP.add(mPoint.x)
        assert(flatKP.size == 36)
        return flatKP.toFloatArray()
    }
}


