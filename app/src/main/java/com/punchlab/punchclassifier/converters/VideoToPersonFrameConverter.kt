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
import androidx.lifecycle.MutableLiveData
import com.punchlab.punchclassifier.TARGET_HEIGHT
import com.punchlab.punchclassifier.TARGET_WIDTH
import com.punchlab.punchclassifier.TIMEOUT_US
import com.punchlab.punchclassifier.data.Device
import com.punchlab.punchclassifier.data.Person
import com.punchlab.punchclassifier.ml.ModelType
import com.punchlab.punchclassifier.ml.MoveNet


class VideoToPersonFrameConverter(context: Context) {

    // Dependencies injected from MainActivity
    private val yuvToRgbConverter = YuvToRgbConverter(context)
    private val poseDetector = MoveNet.create(context, Device.CPU, ModelType.Lightning)


    private lateinit var mOutputFormat: MediaFormat
    private lateinit var imageBitmap: Bitmap

    // Some shared state data, video properties
    private var frameWidth: Int = -1
    private var frameHeight: Int = -1
    private var frameTotalNumber = -1

    val personList = mutableListOf<Person>()
    val progress = MutableLiveData(0)


    fun syncProcessing(fd: ParcelFileDescriptor): List<Person>{
        personList.clear()

        val codec = MediaCodec.createDecoderByType("video/avc")
        val extractor = buildExtractor(fd)
        codec.configure(mOutputFormat, null, null, 0)
        codec.start()

        // We need some extra loops to guarantee process all frames, so frameTotalNumber*2
        for (frame in 0 .. frameTotalNumber*2){
            progress.postValue(frame )
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

        codec.stop()
        codec.release()
        return personList
    }

    private fun buildExtractor(fd: ParcelFileDescriptor): MediaExtractor {
        val extractor = MediaExtractor()
        extractor.setDataSource(fd.fileDescriptor)

        extractor.selectTrack(0)
        mOutputFormat = extractor.getTrackFormat(0)
        frameTotalNumber = mOutputFormat.getInteger("frame-count")

        Log.d(TAG, "Media format: $mOutputFormat")

        frameWidth = mOutputFormat.getInteger(MediaFormat.KEY_WIDTH)
        if (mOutputFormat.containsKey("crop-left") && mOutputFormat.containsKey("crop-right")) {
            frameWidth = mOutputFormat.getInteger("crop-right") + 1 - mOutputFormat.getInteger("crop-left")
        }
        frameHeight = mOutputFormat.getInteger(MediaFormat.KEY_HEIGHT)
        if (mOutputFormat.containsKey("crop-top") && mOutputFormat.containsKey("crop-bottom")) {
            frameHeight = mOutputFormat.getInteger("crop-bottom") + 1 - mOutputFormat.getInteger("crop-top")
        }

        imageBitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888)
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
        private var INSTANCE: VideoToPersonFrameConverter? = null

        private const val TAG = "VideoToPersonConverter"


        fun getInstance(context: Context): VideoToPersonFrameConverter {
            return INSTANCE ?: synchronized(this) {
                val instance  = VideoToPersonFrameConverter(context)
                INSTANCE = instance
                instance
            }
        }
    }
}


