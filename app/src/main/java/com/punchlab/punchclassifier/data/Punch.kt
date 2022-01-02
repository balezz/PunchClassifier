package com.punchlab.punchclassifier.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "punches")
data class Punch(
    @PrimaryKey(autoGenerate = true)
    val punchId: Long = 0,

    val punchTypeIndex: Int,

    var videoSampleId: Long = 0,
    var duration: Int = 0,
    var quality: Int = 0)


fun fromBounds(punchIdxs: List<Int>, bounds: Pair<Int, Int>) : Punch {
    val startTime: Double = bounds.first * 0.03333
    val duration: Int = (bounds.second - bounds.first) * 33
    val punchIndex: Int = punchIdxs
        .slice(bounds.first .. bounds.second)
        .groupBy { it }
        .mapValues { it.value.size }
        .maxByOrNull { it.value }!!.key
    return Punch(punchTypeIndex = punchIndex, duration =  duration, quality =  85)
}

enum class PunchClass(val position: Int) {
    NO_PUNCH(0),
    LEFT_JAB(1),
    RIGHT_JAB(2),
    LEFT_HOOK(3),
    RIGHT_HOOK(4),
    LEFT_UPPERCUT(5),
    RIGHT_UPPERCUT(6);
    companion object{
        private val map = values().associateBy(PunchClass::position)
        fun fromInt(position: Int): PunchClass = map.getValue(position)
    }
}
