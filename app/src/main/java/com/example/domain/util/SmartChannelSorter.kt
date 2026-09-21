package com.example.domain.util

import com.example.domain.model.IPTVChannel
import java.util.Locale

object SmartChannelSorter {

    // Main Turkish National & Top Priority Channels in Logical Order
    private val MAJOR_TURKISH_CHANNELS = listOf(
        "trt 1", "atv", "kanal d", "show tv", "show", "star tv", "star",
        "tv8", "now", "fox", "haberturk", "habertürk", "ntv", "cnn turk", "cnn türk",
        "trt spor", "bein sports 1", "bein sports", "s sport", "tivibu spor", "a haber",
        "trt haber", "tv8.5", "teve2", "tlc", "dmax", "halk tv", "tele1", "ekol tv",
        "kanal 7", "beyaz tv", "trt cocuk", "trt çocuk", "trt belgesel", "trt muzik", "trt müzik"
    )

    fun getQualityScore(channel: IPTVChannel): Int {
        val raw = "${channel.name} ${channel.streamUrl}".lowercase(Locale.ROOT)
        return when {
            raw.contains("4k") || raw.contains("uhd") || raw.contains("2160p") -> 50
            raw.contains("fhd") || raw.contains("1080p") -> 40
            raw.contains("hd") || raw.contains("720p") -> 30
            raw.contains("hevc") || raw.contains("h265") -> 20
            raw.contains("sd") -> 10
            else -> 15
        }
    }

    fun getMajorChannelRank(channel: IPTVChannel): Int {
        val nameLower = IPTVChannel.normalize(channel.name)
        val index = MAJOR_TURKISH_CHANNELS.indexOfFirst { major ->
            nameLower.contains(major) || major.contains(nameLower)
        }
        return if (index != -1) index else 999
    }

    fun sortSmartly(
        channels: List<IPTVChannel>,
        option: String = "Akıllı Sıralama",
        watchTimesMap: Map<String, Long> = emptyMap()
    ): List<IPTVChannel> {
        return when (option) {
            "A-Z" -> channels.sortedBy { it.name.lowercase(Locale.ROOT) }

            "En Popüler" -> {
                channels.sortedWith(
                    compareBy<IPTVChannel> { !it.isFavorite }
                        .thenBy { getMajorChannelRank(it) }
                        .thenByDescending { getQualityScore(it) }
                        .thenBy { it.name.lowercase(Locale.ROOT) }
                )
            }

            "Kaliteye Göre" -> {
                channels.sortedWith(
                    compareBy<IPTVChannel> { !it.isFavorite }
                        .thenByDescending { getQualityScore(it) }
                        .thenBy { getMajorChannelRank(it) }
                        .thenBy { it.name.lowercase(Locale.ROOT) }
                )
            }

            "Akıllı Sıralama", "Görsel Referans" -> {
                channels.sortedWith(
                    compareBy<IPTVChannel> { !it.isFavorite }
                        .thenByDescending { watchTimesMap[it.id] ?: 0L }
                        .thenBy { getMajorChannelRank(it) }
                        .thenByDescending { getQualityScore(it) }
                        .thenBy { it.name.lowercase(Locale.ROOT) }
                )
            }

            else -> channels.sortedWith(
                compareBy<IPTVChannel> { !it.isFavorite }
                    .thenBy { getMajorChannelRank(it) }
                    .thenByDescending { getQualityScore(it) }
            )
        }
    }
}
