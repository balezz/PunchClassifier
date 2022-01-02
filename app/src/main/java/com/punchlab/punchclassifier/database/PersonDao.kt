package com.punchlab.punchclassifier.database

import android.graphics.PointF
import androidx.room.*
import com.punchlab.punchclassifier.data.BodyPart
import com.punchlab.punchclassifier.data.KeyPoint
import com.punchlab.punchclassifier.data.Person
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(person: Person)

    @Delete
    suspend fun delete(person: Person)

    @Query("SELECT * from persons")
    fun getPersonList(): Flow<List<Person>>

}


class PersonConverters {
    @TypeConverter
    fun stringToKeypoints(value: String?): List<KeyPoint> {
        val keyPoints = mutableListOf<KeyPoint>()
        val points = value!!.split(";")
        for (point in points){
            val p = point.split(", ")
            val position = p[0].toInt()
            val x = p[1].toFloat()
            val y = p[2].toFloat()
            val score = p[3].toFloat()
            val keyPoint = KeyPoint(BodyPart.fromInt(position), PointF(x, y), score)
            keyPoints.add(keyPoint)
        }
        return keyPoints
    }

    @TypeConverter
    fun keypointsToString(keyPoints: List<KeyPoint>): String {
        return keyPoints.joinToString(separator = ";") {
            "${it.bodyPart.position}, ${it.coordinate.x}, ${it.coordinate.y}, ${it.score}" }
    }
}