package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.domain.model.IPTVChannel
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels")
    fun getAllChannelsFlow(): Flow<List<IPTVChannel>>

    @Query("SELECT * FROM channels")
    suspend fun getAllChannels(): List<IPTVChannel>

    @Query("SELECT * FROM channels WHERE isFavorite = 1")
    fun getFavoriteChannelsFlow(): Flow<List<IPTVChannel>>

    @Query("SELECT * FROM channels WHERE lastWatchedTimestamp > 0 ORDER BY lastWatchedTimestamp DESC")
    fun getRecentlyWatchedChannelsFlow(): Flow<List<IPTVChannel>>

    @Query("SELECT * FROM channels WHERE category = :category")
    fun getChannelsByCategoryFlow(category: String): Flow<List<IPTVChannel>>

    @Query("SELECT DISTINCT category FROM channels")
    fun getCategoriesFlow(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<IPTVChannel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: IPTVChannel)

    @Update
    suspend fun updateChannel(channel: IPTVChannel)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)

    @Query("UPDATE channels SET lastWatchedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastWatchedTimestamp(id: String, timestamp: Long)

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getChannelById(id: String): IPTVChannel?

    @Query("DELETE FROM channels WHERE isCustom = 0")
    suspend fun clearPresetChannels()

    @Query("DELETE FROM channels")
    suspend fun clearAllChannels()
}
