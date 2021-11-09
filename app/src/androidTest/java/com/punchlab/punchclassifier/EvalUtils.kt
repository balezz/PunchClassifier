package com.punchlab.punchclassifier

import android.graphics.*
import android.media.ImageReader
import android.media.MediaDataSource
import android.media.MediaMetadataRetriever

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.punchlab.punchclassifier.data.BodyPart
import com.punchlab.punchclassifier.data.Person

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.math.pow


object EvalUtils {

    private lateinit var yuvConverter: YuvToRgbConverter
    private val out = mutableListOf<Bitmap>()

    /** [Handler] corresponding to [imageReaderThread] */

    private const val PREVIEW_WIDTH = 640
    private const val PREVIEW_HEIGHT = 480
    private const val NUM_VIDEO_FRAMES = 30
    private const val TAG = "EvalUtils"

    /**
     * Assert whether the detected person from the image match with the expected result.
     * Detection result is accepted as correct if it is within the acceptableError range from the
     * expected result.
     */
    fun assertPoseDetectionResult(
            person: Person,
            expectedResult: Map<BodyPart, PointF>,
            acceptableError: Float
    ){
        // Check if the model is confident enough in detecting the person
        assertThat(person.score).isGreaterThan(0.5f)

        for((bodyPart, expectedPointF) in expectedResult){
            val keypoint = person.keyPoints.firstOrNull {it.bodyPart == bodyPart}
            assertWithMessage("$bodyPart must exist").that(keypoint).isNotNull()

            val detectedPointF = keypoint!!.coordinate
            val distanceFromExpectedPointF = distance(detectedPointF, expectedPointF)
            assertWithMessage("Detected $bodyPart must be close to expected result")
                .that(distanceFromExpectedPointF).isAtMost(acceptableError)
        }
    }


    /** Load an image from assets folder using its name. */
    fun loadBitmapAssetByName(name: String): Bitmap {
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val testInput = testContext.assets.open(name)
        return BitmapFactory.decodeStream(testInput)
    }

    /** Load an video from assets folder using its name. */
    fun loadVideoAssetImageReader(name: String): List<Bitmap> {

        val testContext = InstrumentationRegistry.getInstrumentation().context
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val afd = testContext.assets.openFd(name)
        Log.d(TAG, "Asset file descriptor: $afd")

        yuvConverter = YuvToRgbConverter(targetContext)

        val imageReader = ImageReader.newInstance(
                PREVIEW_WIDTH, PREVIEW_HEIGHT, ImageFormat.YUV_420_888, 32)

        val mediaPlayer = MediaPlayer()
        mediaPlayer.setSurface(imageReader.surface)
        mediaPlayer.setDataSource(afd)

        mediaPlayer.prepare()
        mediaPlayer.start()
        TimeUnit.MILLISECONDS.sleep(2000)
        mediaPlayer.pause()
        mediaPlayer.release()


        val imageBitmap = imageReader.acquireNextImage()
        return out
    }

    fun loadVideoAssetMediaRetriever(name: String): List<Bitmap> {
        val retriever = MediaMetadataRetriever()
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val afd = testContext.assets.openFd(name)
        Log.d(TAG, "Asset file descriptor: $afd")
        retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        return retriever.getFramesAtIndex(0, NUM_VIDEO_FRAMES)
    }

    /** Load csv from assets folder */
    fun loadCSVAsset(name: String): List<Map<BodyPart, PointF>> {
        val data = mutableListOf<Map<BodyPart, PointF>>()
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val testInput = testContext.assets.open(name)
        val inputStreamReader = InputStreamReader(testInput)
        val reader = BufferedReader(inputStreamReader)
        // Skip header line
        reader.readLine()

        // Read coordinates from each lines
        reader.forEachLine {
            val listPoint = it.split(",")
            val map = mutableMapOf<BodyPart, PointF>()
            for( i in listPoint.indices step 2) {
                map[BodyPart.fromInt(i / 2)] =
                    PointF(listPoint[i].toFloat(), listPoint[i + 1].toFloat())
            }
            data.add(map)
        }
        return data
    }


    private fun distance(point1: PointF, point2: PointF): Float {

        return ((point1.x - point2.x).pow(2) + (point1.y - point2.y).pow(2)).pow(0.5f)
    }
}