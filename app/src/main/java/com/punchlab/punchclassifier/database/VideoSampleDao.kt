package com.punchlab.punchclassifier.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.punchlab.punchclassifier.data.Punch
import com.punchlab.punchclassifier.data.VideoSample
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoSampleDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(videoSample: VideoSample): Long

    @Query("SELECT * FROM video_samples WHERE videoId = :id")
    fun getVideoSample(id: Int): Flow<VideoSample>

    @Query("SELECT * FROM video_samples WHERE uri = :uri")
    suspend fun getVideoSampleByUri(uri: String): VideoSample

    @Query("SELECT * FROM video_samples")
    fun getVideoSamples(): Flow<List<VideoSample>>

    @Query("SELECT * FROM video_samples " +
            "JOIN punches ON video_samples.videoId = punches.punchId")
    fun getVideoSamplesWithPunches(): Flow<Map<VideoSample, List<Punch>>>
}