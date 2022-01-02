package com.punchlab.punchclassifier.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persons")
data class Person(
    @PrimaryKey(autoGenerate = true)
    val personId: Int = 0,

    @ColumnInfo(name = "key_points")
    val keyPoints: List<KeyPoint>,

    @ColumnInfo(name = "score")
    val score: Float,

    @ColumnInfo(name = "video_sample_id")
    var videoSampleId: Long
    )
{
    override fun toString(): String {
        return "Person score: $score"
    }
}

