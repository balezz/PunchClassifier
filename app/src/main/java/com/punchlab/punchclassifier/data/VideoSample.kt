package com.punchlab.punchclassifier.data

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "video_samples")
data class VideoSample(
    @PrimaryKey(autoGenerate = true)
    val videoId: Long = 0,

    val uri: String,

    val duration: Int)
