package com.punchlab.punchclassifier

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.media.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.TimeUnit

class TestDecoderAsync(testContext: Context, appContext: Context, fileName: String) {
    companion object {
        private const val TARGET_WIDTH = 384
        private const val TARGET_HEIGHT = 256
        private const val TAG = "DecoderAsync"
    }
    private val mExtractor = MediaExtractor()
    private val mDecoder = MediaCodec.createDecoderByType("video/avc")
    private val converter = YuvToRgbConverter(appContext)

    val bitmapList = mutableListOf<Bitmap>()
    private var mOutputFormat: MediaFormat
    private var imageBitmap: Bitmap

    init {
        val afd = testContext.assets.openFd(fileName)
        Log.d(TAG, "Asset file descriptor: $afd")
        mExtractor.setDataSource(afd)
        mExtractor.selectTrack(0)

        mOutputFormat = mExtractor.getTrackFormat(0)
        Log.d(TAG, "Media format: $mOutputFormat")
        var width: Int = mOutputFormat.getInteger(MediaFormat.KEY_WIDTH)
        if (mOutputFormat.containsKey("crop-left") && mOutputFormat.containsKey("crop-right")) {
            width = mOutputFormat.getInteger("crop-right") + 1 - mOutputFormat.getInteger("crop-left")
        }
        var height: Int = mOutputFormat.getInteger(MediaFormat.KEY_HEIGHT)
        if (mOutputFormat.containsKey("crop-top") && mOutputFormat.containsKey("crop-bottom")) {
            height = mOutputFormat.getInteger("crop-bottom") + 1 - mOutputFormat.getInteger("crop-top")
        }

        imageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        mDecoder.configure(mOutputFormat, null, null, 0)
        Log.d(TAG, "Input format: ${mDecoder.inputFormat}")

        val cb = object: MediaCodec.Callback(){
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                val inBuffer = codec.getInputBuffer(index)!!
                val sampleSize = mExtractor.readSampleData(inBuffer, 0)
                if (mExtractor.advance() && sampleSize > 0) {
                    codec.queueInputBuffer(index, 0, sampleSize, mExtractor.sampleTime, 0)
                } else {
                    Log.d(TAG, "Input buffer BUFFER_FLAG_END_OF_STREAM")
                    codec.queueInputBuffer(index, 0, 0, 0,
                                                MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                }
            }

            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int,
                                                 info: MediaCodec.BufferInfo) {
                val image = codec.getOutputImage(index)
                image?.let { processImage(it) }
                codec.releaseOutputBuffer(index, false)
                //Log.d(TAG, "release buffer # $index")
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                Log.e(TAG, e.stackTraceToString())
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                Log.d(TAG, "Format changed to: $format")
                mOutputFormat = format
            }
        }

        mDecoder.setCallback(cb)

    }

    fun run() {
        mDecoder.start()
    }

    fun processImage(image: Image) {
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
    }

}