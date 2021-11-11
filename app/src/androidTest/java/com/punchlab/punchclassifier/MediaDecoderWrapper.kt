package com.punchlab.punchclassifier

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaExtractor
import android.util.Log

class MediaDecoderWrapper(testContext: Context, appContext: Context, fileName: String) {
    companion object {
        private const val TARGET_WIDTH = 384
        private const val TARGET_HEIGHT = 256
        private const val READER_BUFFER_SIZE = 30
        private const val TAG = "MediaCodecWrapper"
    }

    private val mExtractor = MediaExtractor()
    private val mDecoder = MediaCodec.createDecoderByType("video/avc")
    private val converter = YuvToRgbConverter(appContext)
    private var imageReader: ImageReader
    private var imageBitmap: Bitmap
    val bitmapList = mutableListOf<Bitmap>()

    init {
        val afd = testContext.assets.openFd(fileName)
        Log.d(TAG, "Asset file descriptor: $afd")
        mExtractor.setDataSource(afd)
        mExtractor.selectTrack(0)

        val mediaFormat = mExtractor.getTrackFormat(0)
        val width = mediaFormat.getInteger("width")
        val height = mediaFormat.getInteger("height")
        imageReader = ImageReader.newInstance(
            width, height, ImageFormat.YUV_420_888, READER_BUFFER_SIZE)
        imageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        mDecoder.configure(mediaFormat, imageReader.surface, null, 0)
        Log.d(TAG, "Input format: ${mDecoder.inputFormat}")

        mDecoder.start()
}

    fun run() {
        var isInput = true
        val info = MediaCodec.BufferInfo()
        while (true) {
            if (isInput) {
                val inputIndex = mDecoder.dequeueInputBuffer(10000)
                if (inputIndex >= 0) {
                    val inBuffer = mDecoder.getInputBuffer(inputIndex)!!
                    val sampleSize = mExtractor.readSampleData(inBuffer, 0)
                    if (mExtractor.advance() && sampleSize > 0) {
                        mDecoder.queueInputBuffer(
                            inputIndex, 0, sampleSize, mExtractor.sampleTime, 0)
                    } else {
                        Log.d(TAG, "Input buffer BUFFER_FLAG_END_OF_STREAM")
                        mDecoder.queueInputBuffer(
                            inputIndex, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isInput = false
                    }
                }
            }

            val outIndex = mDecoder.dequeueOutputBuffer(info, 10_000)
            if (outIndex >= 0) {
                mDecoder.releaseOutputBuffer(outIndex, true)

                while(true) {
                    val image = imageReader.acquireNextImage()

                    if (image != null) {
                        converter.yuvToRgb(image, imageBitmap)

                        val scaledBitmap = Bitmap.createScaledBitmap(imageBitmap,
                            TARGET_WIDTH, TARGET_HEIGHT, false)
                        val rotateMatrix = Matrix()
                        rotateMatrix.postRotate(90.0f)
                        val rotatedBitmap = Bitmap.createBitmap(
                            scaledBitmap, 0, 0, TARGET_WIDTH, TARGET_HEIGHT,
                            rotateMatrix, false
                        )
                        bitmapList.add(rotatedBitmap)
                        image.close()
                    } else break
                }
            }
            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM")
                break
            }

        }
        Log.d(TAG, "Output format: ${mDecoder.outputFormat}")
        mDecoder.stop()
        mDecoder.release()

    }
}