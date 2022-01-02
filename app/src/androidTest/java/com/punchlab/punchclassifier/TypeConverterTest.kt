package com.punchlab.punchclassifier

import android.graphics.PointF
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.punchlab.punchclassifier.data.BodyPart
import com.punchlab.punchclassifier.data.KeyPoint
import com.punchlab.punchclassifier.database.PersonConverters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TypeConverterTest {

    @Test
    fun testConverter() {
        val keyPoints = listOf(
            KeyPoint(BodyPart.LEFT_ANKLE, PointF(0.1f, 0.1f), 0.1f),
            KeyPoint(BodyPart.RIGHT_ANKLE, PointF(0.2f, 0.5f), 0.5f),
            KeyPoint(BodyPart.NOSE, PointF(0.3f, 0.6f), 0.6f),
            KeyPoint(BodyPart.RIGHT_EAR, PointF(0.4f, 0.7f), 0.7f)
        )
        val keyString = PersonConverters().keypointsToString(keyPoints)
        val newKeyPoints = PersonConverters().stringToKeypoints(keyString)
        assert(keyPoints == newKeyPoints)
    }
}