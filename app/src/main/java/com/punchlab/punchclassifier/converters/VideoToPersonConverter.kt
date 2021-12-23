package com.punchlab.punchclassifier.converters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.Image
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.ParcelFileDescriptor
import android.util.Log
import com.punchlab.punchclassifier.TIMEOUT_US
import com.punchlab.punchclassifier.data.Device
import com.punchlab.punchclassifier.data.Person
import com.punchlab.punchclassifier.ml.ModelType
import com.punchlab.punchclassifier.ml.MoveNet


class VideoToPersonConverter(context: Context) {

    // Dependencies injected from MainActivity
    private val yuvToRgbConverter = YuvToRgbConverter(context)
    private val poseDetector = MoveNet.create(context, Device.CPU, ModelType.Lightning)

    // Work horse classes
    private lateinit var mOutputFormat: MediaFormat
    private lateinit var imageBitmap: Bitmap

    // Some shared state data
    private var mWidth: Int = -1
    private var mHeight: Int = -1
    private var frameTotalNumber = -1
    private var frameCounter = 0

    // Internal lists
    private val personList = mutableListOf<Person>()

    fun syncProcessing(fd: ParcelFileDescriptor){

        val codec = MediaCodec.createDecoderByType("video/avc")
        val extractor = buildExtractor(fd)
        codec.configure(mOutputFormat, null, null, 0)
        codec.start()

        for (frame in 0 .. frameTotalNumber){
            val inBufferId = codec.dequeueInputBuffer(TIMEOUT_US)
            if (inBufferId >= 0){
                val inBuffer = codec.getInputBuffer(inBufferId)
                val sampleSize = extractor.readSampleData(inBuffer!!, 0)
                val advance = extractor.advance()
                if (advance && sampleSize > 0) {
                    codec.queueInputBuffer(inBufferId, 0, sampleSize, extractor.sampleTime, 0)
                } else {
                    Log.d(TAG, "Advance: $advance , sampleSize: $sampleSize, index: $inBufferId BUFFER_FLAG_END_OF_STREAM")
                    codec.queueInputBuffer(inBufferId, 0, 0, 0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                }
            }

            val outBufferId = codec.dequeueOutputBuffer(MediaCodec.BufferInfo(), TIMEOUT_US)
            if (outBufferId >= 0) {
                val image = codec.getOutputImage(outBufferId)
                image?.let { processOneFrame(it) }
                codec.releaseOutputBuffer(outBufferId, false)
                Log.d(TAG, "release buffer # $outBufferId")
                }
            }

        Log.d(TAG, "Person list size: ${personList.size}")
        codec.stop()
        codec.release()
    }


    private fun buildExtractor(fd: ParcelFileDescriptor): MediaExtractor {
        val extractor = MediaExtractor()
        extractor.setDataSource(fd.fileDescriptor)

        extractor.selectTrack(0)
        mOutputFormat = extractor.getTrackFormat(0)
        frameTotalNumber = mOutputFormat.getInteger("frame-count")
        frameCounter = 0
        Log.d(TAG, "Media format: $mOutputFormat")

        mWidth = mOutputFormat.getInteger(MediaFormat.KEY_WIDTH)
        if (mOutputFormat.containsKey("crop-left") && mOutputFormat.containsKey("crop-right")) {
            mWidth = mOutputFormat.getInteger("crop-right") + 1 - mOutputFormat.getInteger("crop-left")
        }
        mHeight = mOutputFormat.getInteger(MediaFormat.KEY_HEIGHT)
        if (mOutputFormat.containsKey("crop-top") && mOutputFormat.containsKey("crop-bottom")) {
            mHeight = mOutputFormat.getInteger("crop-bottom") + 1 - mOutputFormat.getInteger("crop-top")
        }

        imageBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888)
        return extractor
    }

    private fun processOneFrame(image: Image) {
        yuvToRgbConverter.yuvToRgb(image, imageBitmap)

        val scaledBitmap = Bitmap.createScaledBitmap(
            imageBitmap, TARGET_WIDTH, TARGET_HEIGHT, false
        )

        val rotateMatrix = Matrix()
        rotateMatrix.postRotate(90.0f)
        val rotatedBitmap = Bitmap.createBitmap(
            scaledBitmap, 0, 0, TARGET_WIDTH, TARGET_HEIGHT, rotateMatrix, false
        )

        val person = poseDetector.estimateSinglePose(rotatedBitmap)
        personList.add(person)
    }

    companion object {
        @Volatile
        private var INSTANCE: VideoToPersonConverter? = null

        private const val TAG = "VideoToPersonConverter"
        private const val TARGET_WIDTH = 384
        private const val TARGET_HEIGHT = 256

        fun getInstance(context: Context): VideoToPersonConverter {
            return INSTANCE ?: synchronized(this) {
                val instance  = VideoToPersonConverter(context)
                INSTANCE = instance
                instance
            }
        }
    }
}


