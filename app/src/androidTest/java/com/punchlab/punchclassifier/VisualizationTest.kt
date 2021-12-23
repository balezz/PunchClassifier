package com.punchlab.punchclassifier

import android.content.Context
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.punchlab.punchclassifier.data.Device
import com.punchlab.punchclassifier.ml.ModelType
import com.punchlab.punchclassifier.ml.MoveNet


/**
 * Instrumented test, which will execute on an Android device.
 * This test is used to visually verify detection results by the models.
 * You can put a breakpoint at the end of the method, debug this method, than use the
 * "View Bitmap" feature of the debugger to check the visualized detection result.
 */
@RunWith(AndroidJUnit4::class)
class VisualizationTest {
    companion object {
        private const val TEST_INPUT_IMAGE = "image3.jpg"
    }

    private lateinit var appContext: Context
    private lateinit var inputBitmap: Bitmap

    @Before
    fun setup() {
        // Context of the app under test.
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        inputBitmap = TestUtils.loadBitmapAssetByName(TEST_INPUT_IMAGE)
    }

    @Test
    fun testMovenetLightning() {
        val poseDetector = MoveNet.create(appContext, Device.CPU, ModelType.Lightning)
        // run few times to get good crop
        poseDetector.estimateSinglePose(inputBitmap)
        poseDetector.estimateSinglePose(inputBitmap)
        val person = poseDetector.estimateSinglePose(inputBitmap)
        val outputBitmap = VisualizationUtils.drawBodyKeypoints(inputBitmap, person)
        assertThat(outputBitmap).isNotNull()
    }
}