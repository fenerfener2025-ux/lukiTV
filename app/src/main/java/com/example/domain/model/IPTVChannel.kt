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
            val lower = name.lowercase(Locale.ROOT)
                .replace("ı", "i")
                .replace("İ", "i")
                .replace("ş", "s")
                .replace("Ş", "s")
                .replace("ğ", "g")
                .replace("Ğ", "g")
                .replace("ü", "u")
                .replace("Ü", "u")
                .replace("ö", "o")
                .replace("Ö", "o")
                .replace("ç", "c")
                .replace("Ç", "c")
            // Remove quality tokens properly without regex character class mangling
            val cleaned = lower.replace(Regex("\\b(hd|fhd|4k|8k|sd|3d|uhd|vip|1080p|720p|hevc|raw|plus)\\b"), " ")
                .replace(Regex("[^a-z0-9\\s]"), " ")
            return cleaned.replace(Regex("\\s+"), " ").trim()
        }
    }
}
