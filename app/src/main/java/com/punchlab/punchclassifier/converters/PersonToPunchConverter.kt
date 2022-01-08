package com.punchlab.punchclassifier.converters

import android.content.Context
import android.graphics.PointF
import androidx.lifecycle.MutableLiveData
import com.punchlab.punchclassifier.TARGET_HEIGHT
import com.punchlab.punchclassifier.TARGET_WIDTH
import com.punchlab.punchclassifier.converters.ConverterUtils.splitByZeros
import com.punchlab.punchclassifier.data.Person
import com.punchlab.punchclassifier.data.Punch
import com.punchlab.punchclassifier.data.fromBounds
import com.punchlab.punchclassifier.ml.PunchRnnClassifier

class PersonToPunchConverter(context: Context) {

    private val punchClassifier = PunchRnnClassifier.create(context)

    fun convertPersonsToPunchIndices(personList: List<Person>,
                                     sampleSize: Int = 30,
                                     step: Int = 30) : List<Int> {
        val punchIdxList = mutableListOf<Int>()
        val pointsList = personList.map{extractKeyPoints(it, TARGET_WIDTH, TARGET_HEIGHT)}
        // Log.d(TAG, "processPunches: pointsList = $pointsList")
        val winPointsList = pointsList.windowed(sampleSize, step, partialWindows = false)
        val winPredicted = winPointsList.map{ punchClassifier.classify(it) }
        val numBatches = personList.size / sampleSize
        for (i in 0 until numBatches) { punchIdxList.addAll(winPredicted[i]) }
        return punchIdxList
    }

    fun convertIndicesToPunch(punchIdxList: List<Int>): List<Punch>{
        val punchList = mutableListOf<Punch>()
        val punchBounds = punchIdxList.splitByZeros()
        for (pair in punchBounds){
            punchList.add(fromBounds(punchIdxList, pair))
        }
         return punchList
//        return listOf(Punch(punchTypeIndex = 1))    // mock return
    }

    /** Extract key points from Person and prepare for tflite model prediction:
     * normalize to bitmap width and height,
     * subtract middle point b/w left and right hips */
    private fun extractKeyPoints(person: Person, height: Int, width: Int): FloatArray {
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

    companion object {
        @Volatile
        private var INSTANCE: PersonToPunchConverter? = null

        fun getInstance(context: Context): PersonToPunchConverter {
            return INSTANCE ?: synchronized(this) {
                val instance  = PersonToPunchConverter(context)
                INSTANCE = instance
                instance
            }
        }
    }
}