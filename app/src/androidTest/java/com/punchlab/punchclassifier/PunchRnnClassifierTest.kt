package com.punchlab.punchclassifier

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import com.punchlab.punchclassifier.data.Device
import com.punchlab.punchclassifier.ml.*

@RunWith(AndroidJUnit4::class)
class PunchRnnClassifierTest {

    companion object {
        private const val TEST_INPUT_VIDEO = "id1_jab_2.mp4"
    }

    private lateinit var appContext: Context
    private lateinit var poseDetector: PoseDetector
    private lateinit var poseRnnClassifier: PunchRnnClassifier

    @Before
    fun setup() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        poseDetector = MoveNet.create(appContext, Device.CPU, ModelType.Lightning)
        poseRnnClassifier = PunchRnnClassifier.create(appContext)
    }

    @Test
    fun testPunchClassifier() {
        val input = TestUtils.loadVideoAssetWithMediaRetriever(TEST_INPUT_VIDEO).slice(0..29)
        val height = input[0].height
        val width = input[0].width
        val personList = input.map{ poseDetector.estimateSinglePose(it) }
        val keyPoints = personList
            .map {TestUtils.prepareKeyPoints(it, height, width)}
        val classificationResult = poseRnnClassifier.classify(keyPoints)

        assert(classificationResult.size == 30)
        assert(classificationResult.contains(1))
        assert(classificationResult.contains(0))
    }



}
