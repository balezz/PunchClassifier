package com.punchlab.punchclassifier

import android.graphics.PointF
import com.punchlab.punchclassifier.data.BodyPart
import com.punchlab.punchclassifier.data.KeyPoint
import com.punchlab.punchclassifier.data.Person
import org.junit.Test

import org.junit.Assert.*
import kotlin.random.Random

/**
 * Local unit test, which will execute on the development machine (host).
 *  List<Person> -> List<Punch>
 * []
 *
 */
class BatchPredictionUnitTest {

    private fun makeRandomList(numPerson: Int): List<Person> {
        // fill  List<Person> random values
        val mockPersonArray = mutableListOf<Person>()
        for (n in 0 until numPerson) {
            val keyPoints = mutableListOf<KeyPoint>()
            for (i in 0..16) {
                val keyPoint = KeyPoint(
                    BodyPart.fromInt(i),
                    PointF(Random.nextFloat(), Random.nextFloat()),
                    Random.nextFloat()
                )
                keyPoints.add(keyPoint)
            }
            mockPersonArray.add(Person(keyPoints, Random.nextFloat()))
        }
        return mockPersonArray.toList()
    }

    private fun mockPredict(personList: List<Person>): List <Int> {
        return personList.map{
            it.score.hashCode() % 8
        }
    }

    private fun mockBatchPredict(personList: List<Person>, step:Int=10, sampleSize:Int=10):List<Int>{
        val winPersonList = personList.windowed(sampleSize, step, partialWindows = false)
        val winPredicted = winPersonList.map{ mockPredict(it) }
        val out = mutableListOf<Int>()
        val numBatches = personList.size / sampleSize
        for (i in 0 until numBatches) {
            out += winPredicted[i]
        }
        // todo: add remainder to array
        return out
    }


    /** List<Person> -> Array<Int> */
    @Test
    fun batchPrediction() {
        val personList = makeRandomList(60)
        val listPredict = mockPredict(personList)
        val batchPredict = mockBatchPredict(personList)
        for (i in listPredict.indices ) {
            assertEquals(batchPredict[i], listPredict[i])
        }
    }

}