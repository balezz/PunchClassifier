package com.punchlab.punchclassifier.ml

import android.graphics.Bitmap
import com.punchlab.punchclassifier.data.Person

interface PoseDetector : AutoCloseable {

    fun estimateSinglePose(bitmap: Bitmap): Person

    fun lastInferenceTimeNanos(): Long
}
