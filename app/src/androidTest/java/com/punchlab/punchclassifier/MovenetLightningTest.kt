package com.punchlab.punchclassifier

import android.content.Context
import android.graphics.PointF
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.punchlab.punchclassifier.data.BodyPart
import com.punchlab.punchclassifier.data.Device
import com.punchlab.punchclassifier.ml.ModelType
import com.punchlab.punchclassifier.ml.MoveNet
import com.punchlab.punchclassifier.ml.PoseDetector
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class MovenetLightningTest {

    companion object {
        private const val TEST_INPUT_IMAGE1 = "image1.png"
        private const val TEST_INPUT_IMAGE3 = "image3.jpg"
        private const val TEST_INPUT_VIDEO_1 = "id1_jab_2.mp4"
        private const val TEST_INPUT_VIDEO_2 = "id1_jab_2_cut.mp4"
        private const val ACCEPTABLE_ERROR = 20f
        private const val TAG = "MovenetLightningTest"
    }


    private lateinit var poseDetector: PoseDetector
    private lateinit var appContext: Context
    private lateinit var expectedDetectionResult: List<Map<BodyPart, PointF>>

    @Before
    fun setup() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        poseDetector = MoveNet.create(appContext, Device.CPU, ModelType.Lightning)
        expectedDetectionResult =
            EvalUtils.loadCSVAsset("pose_landmark_truth.csv")
    }

    @Test
    fun testPoseEstimationResultWithImage1() {
        val input = EvalUtils.loadBitmapAssetByName(TEST_INPUT_IMAGE1)

        // As Movenet use previous frame to optimize detection result, we run it multiple times
        // using the same image to improve result.
        poseDetector.estimateSinglePose(input)
        poseDetector.estimateSinglePose(input)
        poseDetector.estimateSinglePose(input)
        val person = poseDetector.estimateSinglePose(input)
        EvalUtils.assertPoseDetectionResult(
            person,
            expectedDetectionResult[0],
            ACCEPTABLE_ERROR
        )
    }

    @Test
    fun testPoseEstimationResultWithImage3() {
        val input = EvalUtils.loadBitmapAssetByName(TEST_INPUT_IMAGE3)
        val person = poseDetector.estimateSinglePose(input)
        EvalUtils.assertPoseDetectionResult(
            person,
            expectedDetectionResult[2],
            ACCEPTABLE_ERROR
        )
    }

    @Test
    fun testPoseEstimationResultWithVideo() {
        var startTime = System.nanoTime()
        // too long time, 2 sec for 30 frames
        val input = EvalUtils.loadVideoAssetWithMediaRetriever(TEST_INPUT_VIDEO_1)
        var endTime = System.nanoTime()
        Log.d(TAG, "Bitmap List len: ${input.size}, " +
                "time: ${(endTime - startTime)/1000_000} ms")
        startTime = System.nanoTime()
        val personList = input.map{ poseDetector.estimateSinglePose(it) }
        endTime = System.nanoTime()
        Log.d(TAG, "Pose estimation time: ${(endTime - startTime) / 1000_1000} ms")
        assert(personList.isNotEmpty())
    }

    @Test
    fun testResultWithVideoMediaCodec() {
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val decoder = TestDecoderAsync(testContext, appContext, TEST_INPUT_VIDEO_1)
        decoder.run()
        TimeUnit.SECONDS.sleep(25)
        val bitmapList = decoder.bitmapList
        assert(decoder.bitmapList.isNotEmpty())

        Log.d(TAG, "List size = ${bitmapList.size}, " +
                "width = ${bitmapList[0].width}, height = ${bitmapList[0].height}")

    }
}
