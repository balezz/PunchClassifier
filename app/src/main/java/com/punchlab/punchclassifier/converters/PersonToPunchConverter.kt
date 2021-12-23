package com.punchlab.punchclassifier.converters

import android.app.Application
import android.graphics.PointF
import com.punchlab.punchclassifier.converters.ConverterUtils.splitByZeros
import com.punchlab.punchclassifier.data.Person
import com.punchlab.punchclassifier.data.Punch
import com.punchlab.punchclassifier.ml.PunchRnnClassifier

class PersonToPunchConverter(application: Application) {

    private val punchClassifier = PunchRnnClassifier.create(application)

    private val punchIdxList = mutableListOf<Int>()
    private val punchList = mutableListOf<Punch>()

    private fun processPunches(personList: List<Person>,
                               width: Int,
                               height: Int,
                               sampleSize: Int = 30,
                               step: Int = 30) {
        val pointsList = personList.map{extractKeyPoints(it, width, height)}
        // Log.d(TAG, "processPunches: pointsList = $pointsList")
        val winPointsList = pointsList.windowed(sampleSize, step, partialWindows = false)
        val winPredicted = winPointsList.map{ punchClassifier.classify(it) }
        val numBatches = personList.size / sampleSize
        for (i in 0 until numBatches) { punchIdxList.addAll(winPredicted[i]) }
        val punchBounds = punchIdxList.splitByZeros()

        for (pair in punchBounds){
            punchList.add(Punch.fromBounds(punchIdxList, pair))
        }
//        sharedViewModel.setPunchList(punchList)

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
        private var INSTANCE: PersonToPunchConverter? = null

        fun getInstance(application: Application): PersonToPunchConverter {
            return INSTANCE ?: synchronized(this) {
                val instance  = PersonToPunchConverter(application)
                INSTANCE = instance
                instance
            }
        }
    }
}