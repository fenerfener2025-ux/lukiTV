package com.example.domain.search

import com.example.domain.model.IPTVChannel
import java.util.Locale
import kotlin.math.min

class FuzzySearchEngine {
    private val trigramIndex = mutableMapOf<String, MutableSet<String>>() // trigram -> set of channelIds
    private var channelsMap = emptyMap<String, IPTVChannel>()

    fun buildIndex(channels: List<IPTVChannel>) {
        trigramIndex.clear()
        channelsMap = channels.associateBy { it.id }

        for (channel in channels) {
            val normalized = channel.normalizedName
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

    // Levenshtein distance calculation
    private fun getLevenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) {
            dp[i][0] = i
        }
        for (j in 0..s2.length) {
            dp[0][j] = j
        }

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1, // deletion
                    dp[i][j - 1] + 1, // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    fun search(query: String): List<IPTVChannel> {
        val normalizedQuery = IPTVChannel.normalize(query)
        if (normalizedQuery.isEmpty()) return emptyList()

        val queryTrigrams = getTrigrams(normalizedQuery)
        val candidateScores = mutableMapOf<String, Double>() // channelId -> score

        // Phase 1: Retrieve candidate channels using Trigrams
        for (trigram in queryTrigrams) {
            val matchingIds = trigramIndex[trigram] ?: continue
            for (id in matchingIds) {
                val score = candidateScores.getOrDefault(id, 0.0)
                candidateScores[id] = score + 1.0
            }
        }

        val results = mutableListOf<ScoredResult>()

        // Phase 2: Refine and score candidates
        for ((id, trigramMatchCount) in candidateScores) {
            val channel = channelsMap[id] ?: continue
            val normName = channel.normalizedName

            // Trigram Score (ratio of matching trigrams)
            val queryTrigramCount = queryTrigrams.size.toDouble()
            val trigramScore = if (queryTrigramCount > 0) trigramMatchCount / queryTrigramCount else 0.0

            // Prefix Bonus: If channel starts with query
            val prefixBonus = if (normName.startsWith(normalizedQuery)) 1.0 else 0.0

            // Popularity Bonus: if favorite, give a slight boost
            val popularBonus = if (channel.isFavorite) 1.0 else 0.0

            // Calculate Levenshtein distance
            val distance = getLevenshteinDistance(normalizedQuery, normName)

            // Let's check edit distance threshold. We want candidates close in Levenshtein distance,
            // or if the query is a substring of the channel name.
            val isMatch = distance <= 2 || normName.contains(normalizedQuery) || trigramScore > 0.3

            if (isMatch) {
                // Score formula: trigramScore * 0.6 + prefixBonus * 0.3 + popularBonus * 0.1
                val score = (trigramScore * 0.6) + (prefixBonus * 0.3) + (popularBonus * 0.1)
                results.add(ScoredResult(channel, score))
            }
        }

        // Sort by score descending
        return results.sortedByDescending { it.score }.map { it.channel }
    }

    private data class ScoredResult(val channel: IPTVChannel, val score: Double)
}
