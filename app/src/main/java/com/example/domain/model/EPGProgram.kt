package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "epg_programs")
data class EPGProgram(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val title: String,
    val description: String
) {
    fun isCurrent(): Boolean {
        val now = System.currentTimeMillis()
        return now in startTimeMs until endTimeMs
    }
}
