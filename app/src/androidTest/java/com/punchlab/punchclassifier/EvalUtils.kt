package com.punchlab.punchclassifier

import android.graphics.*
import android.media.MediaMetadataRetriever

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.punchlab.punchclassifier.data.BodyPart
import com.punchlab.punchclassifier.data.Person

import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.pow


object EvalUtils {

    private val out = mutableListOf<Bitmap>()
    private const val NUM_VIDEO_FRAMES = 64
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

    fun loadVideoAssetWithMediaRetriever(name: String): List<Bitmap> {
        val retriever = MediaMetadataRetriever()
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val afd = testContext.assets.openFd(name)
        Log.d(TAG, "Asset file descriptor: $afd")
        retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        val bitmapList = retriever.getFramesAtIndex(0, NUM_VIDEO_FRAMES)
        Log.d(TAG, "List size = ${bitmapList.size}, " +
                "width = ${bitmapList[0].width}, height = ${bitmapList[0].height}")
        return bitmapList
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


    /** Extract key points from Person and prepare for tflite model:
     * normalize to bitmap width and height,
     * subtract middle point b/w left and right hips */
    fun prepareKeyPoints(person: Person, height: Int, width: Int): FloatArray {
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