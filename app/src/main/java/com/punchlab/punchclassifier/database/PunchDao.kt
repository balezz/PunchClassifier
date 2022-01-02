package com.punchlab.punchclassifier.database

import androidx.lifecycle.MutableLiveData
import androidx.room.*
import com.punchlab.punchclassifier.data.Punch
import kotlinx.coroutines.flow.Flow

@Dao
interface PunchDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(punch: Punch)

    @Delete
    suspend fun delete(punch: Punch)

    @Query("SELECT * from punches")
    suspend fun getPunchList(): List<Punch>

    @Query("SELECT * from punches WHERE videoSampleId = :videoId")
    suspend fun getPunchListByVideoId(videoId: Long): List<Punch>

}