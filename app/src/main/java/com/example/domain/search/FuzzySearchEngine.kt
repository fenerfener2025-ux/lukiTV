package com.example.domain.search

import com.example.domain.model.IPTVChannel
import com.example.domain.util.SmartChannelSorter
import java.util.Locale
import kotlin.math.min

class FuzzySearchEngine {
    private val trigramIndex = mutableMapOf<String, MutableSet<String>>() // trigram -> set of channelIds
    private var channelsList = emptyList<IPTVChannel>()
    private var channelsMap = emptyMap<String, IPTVChannel>()

    fun buildIndex(channels: List<IPTVChannel>) {
        trigramIndex.clear()
        channelsList = channels
        channelsMap = channels.associateBy { it.id }

        for (channel in channels) {
            val normalized = IPTVChannel.normalize(channel.name)
            val trigrams = getTrigrams(normalized)
            for (trigram in trigrams) {
                trigramIndex.getOrPut(trigram) { mutableSetOf() }.add(channel.id)
            }
        }
    }

    private fun getTrigrams(text: String): Set<String> {
        if (text.length < 3) return setOf(text)
        val trigrams = mutableSetOf<String>()
        for (i in 0..text.length - 3) {
            trigrams.add(text.substring(i, i + 3))
        }
        return trigrams
    }

    private fun getLevenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Intelligent multi-token search filtering
     */
    fun search(query: String, categoryFilter: String = "Tümü", qualityFilter: String = "Tümü"): List<IPTVChannel> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty() && categoryFilter == "Tümü" && qualityFilter == "Tümü") {
            return emptyList()
        }

        val rawNormalizedQuery = IPTVChannel.normalize(trimmedQuery)
        val queryTokens = rawNormalizedQuery.split(" ").filter { it.isNotEmpty() }

        val candidateChannels = channelsList.filter { channel ->
            // Category filter
            val matchesCategory = when (categoryFilter) {
                "Tümü" -> true
                "Favoriler" -> channel.isFavorite
                else -> channel.category.contains(categoryFilter, ignoreCase = true) ||
                        channel.groupTitle.contains(categoryFilter, ignoreCase = true)
            }

            // Quality filter
            val matchesQuality = when (qualityFilter) {
                "Tümü" -> true
                "4K / UHD" -> channel.name.contains("4K", ignoreCase = true) || channel.name.contains("UHD", ignoreCase = true)
                "FHD / 1080p" -> channel.name.contains("FHD", ignoreCase = true) || channel.name.contains("1080", ignoreCase = true)
                "HD / 720p" -> channel.name.contains("HD", ignoreCase = true) || channel.name.contains("720", ignoreCase = true)
                "SD" -> !channel.name.contains("HD", ignoreCase = true) && !channel.name.contains("FHD", ignoreCase = true) && !channel.name.contains("4K", ignoreCase = true)
                else -> true
            }

            matchesCategory && matchesQuality
        }

        if (trimmedQuery.isEmpty()) {
            return SmartChannelSorter.sortSmartly(candidateChannels, option = "En Popüler")
        }

        val results = mutableListOf<ScoredResult>()

        for (channel in candidateChannels) {
            val nameRaw = channel.name.lowercase(Locale.ROOT)
            val normName = IPTVChannel.normalize(channel.name)
            val fullMeta = "$normName ${channel.category.lowercase(Locale.ROOT)} ${channel.groupTitle.lowercase(Locale.ROOT)}"

            var tokenMatchCount = 0
            var exactPrefixCount = 0

            for (token in queryTokens) {
                if (normName.contains(token) || nameRaw.contains(token) || fullMeta.contains(token)) {
                    tokenMatchCount++
                    if (normName.startsWith(token) || nameRaw.startsWith(token)) {
                        exactPrefixCount++
                    }
                } else if (token.length >= 3) {
                    // Check Levenshtein fuzzy match on words in normName
                    val words = normName.split(" ")
                    val hasFuzzyMatch = words.any { word -> getLevenshteinDistance(token, word) <= 1 }
                    if (hasFuzzyMatch) {
                        tokenMatchCount++
                    }
                }
            }

            if (tokenMatchCount > 0) {
                val tokenRatio = tokenMatchCount.toDouble() / queryTokens.size.toDouble()
                val prefixBonus = exactPrefixCount * 0.4
                val popularBonus = if (channel.isFavorite) 0.5 else 0.0
                val majorRankBonus = (1000 - SmartChannelSorter.getMajorChannelRank(channel)) / 1000.0
                val qualityBonus = SmartChannelSorter.getQualityScore(channel) / 100.0

                val totalScore = (tokenRatio * 2.0) + prefixBonus + popularBonus + majorRankBonus + qualityBonus

                // Must match at least majority of tokens unless query is single token
                if (queryTokens.size <= 1 || tokenMatchCount >= (queryTokens.size - 1)) {
                    results.add(ScoredResult(channel, totalScore))
                }
            }
        }

        return results.sortedByDescending { it.score }.map { it.channel }
    }

    private data class ScoredResult(val channel: IPTVChannel, val score: Double)
}
