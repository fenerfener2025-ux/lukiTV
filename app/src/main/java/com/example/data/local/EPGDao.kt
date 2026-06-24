package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.domain.model.EPGProgram

@Dao
interface EPGDao {
    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId ORDER BY startTimeMs ASC")
    suspend fun getProgramsForChannel(channelId: String): List<EPGProgram>

    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId AND endTimeMs > :now ORDER BY startTimeMs ASC")
    suspend fun getUpcomingProgramsForChannel(channelId: String, now: Long): List<EPGProgram>

    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId AND startTimeMs <= :time AND endTimeMs > :time LIMIT 1")
    suspend fun getProgramAtTime(channelId: String, time: Long): EPGProgram?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<EPGProgram>)

    @Query("DELETE FROM epg_programs WHERE endTimeMs < :now")
    suspend fun deleteOldPrograms(now: Long)

    @Query("DELETE FROM epg_programs")
    suspend fun clearAllEPG()
}
