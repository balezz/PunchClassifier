package com.punchlab.puchclassifier.data

import android.graphics.PointF

data class KeyPoint(val bodyPart: BodyPart, var coordinate: PointF, val score: Float)

