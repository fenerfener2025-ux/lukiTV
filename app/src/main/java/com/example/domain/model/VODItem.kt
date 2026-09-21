package com.example.domain.model

import com.example.domain.model.IPTVChannel

data class VODItem(
    val id: String,
    val name: String,
    val logoUrl: String,
    val category: String, // "Movies" or "Series"
    val groupTitle: String, // e.g. "Aksiyon", "Komedi", etc.
    val streamUrl: String,
    val sourceName: String,
    val overview: String? = null,
    val tmdbPosterUrl: String? = null,
    val tmdbGenre: String? = null,
    val tmdbRating: Double? = null
) {
    fun toIPTVChannel(): IPTVChannel {
        return IPTVChannel(
            id = id,
            name = name,
            normalizedName = IPTVChannel.normalize(name),
            logoUrl = tmdbPosterUrl ?: logoUrl,
            category = "VOD - $category",
            groupTitle = tmdbGenre ?: groupTitle,
            streamUrl = streamUrl,
            streamMirrors = emptyList(),
            tvgId = id,
            isFavorite = false,
            lastWatchedTimestamp = 0,
            isCustom = true,
            country = "VOD",
            language = "tr"
        )
    }
}
