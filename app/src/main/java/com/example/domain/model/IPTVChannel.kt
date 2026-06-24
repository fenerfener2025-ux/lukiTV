package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "channels")
data class IPTVChannel(
    @PrimaryKey val id: String, // tvgId or derived from name/URL hash
    val name: String,
    val normalizedName: String,
    val logoUrl: String,
    val category: String, // Calculated / Smart Category
    val groupTitle: String, // Original group-title from M3U
    val streamUrl: String, // Current active stream URL
    val streamMirrors: List<String>, // Fallback streams
    val tvgId: String,
    val isFavorite: Boolean = false,
    val lastWatchedTimestamp: Long = 0,
    val isCustom: Boolean = false,
    val country: String = "TR",
    val language: String = "tr"
) {
    companion object {
        fun normalize(name: String): String {
            return name.lowercase(Locale.getDefault())
                .replace(Regex("[hd|fhd|4k|sd|3d|uhd|vip|\\s]+"), " ")
                .replace("ı", "i")
                .replace("ş", "s")
                .replace("ğ", "g")
                .replace("ü", "u")
                .replace("ö", "o")
                .replace("ç", "c")
                .trim()
        }
    }
}
