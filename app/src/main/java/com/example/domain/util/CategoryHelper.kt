package com.example.domain.util

import com.example.domain.model.IPTVChannel
import java.util.Locale

object CategoryHelper {
    const val CAT_TR = "🇹🇷 Türkiye"
    const val CAT_NEWS = "📰 Haber"
    const val CAT_SPORTS = "⚽ Spor"
    const val CAT_NATIONAL = "📺 Ulusal Kanallar"
    const val CAT_LOCAL = "📍 Yerel Kanallar"
    const val CAT_KIDS = "🧸 Çocuk"
    const val CAT_DOCUMENTARY = "🌍 Belgesel"
    const val CAT_WORLD = "🌐 Dünya"
    const val CAT_MOVIES = "🎬 Film/Dizi"
    const val CAT_MUSIC = "🎵 Müzik"
    const val CAT_FAVORITES = "❤️ Favoriler"
    const val CAT_RECENTS = "🕐 Son İzlenenler"

    val FOREIGN_COUNTRIES = listOf(
        "Azerbaycan",
        "ABD",
        "Almanya",
        "İngiltere",
        "Fransa",
        "İtalya",
        "İspanya",
        "Rusya",
        "Global / Diğer"
    )

    fun detectCountry(name: String, groupTitle: String, defaultLanguage: String = "tr"): String {
        val searchStr = "$name $groupTitle".lowercase(Locale.getDefault())
        return when {
            searchStr.contains("azerb") || searchStr.contains("azerbaycan") || searchStr.contains("az:") || searchStr.contains("[az]") -> "Azerbaycan"
            searchStr.contains("germany") || searchStr.contains("almanya") || searchStr.contains("de:") || searchStr.contains("[de]") || searchStr.contains(" de ") -> "Almanya"
            searchStr.contains("united kingdom") || searchStr.contains("ingiltere") || searchStr.contains("uk:") || searchStr.contains("[uk]") || searchStr.contains("uk ") || searchStr.contains("gb:") -> "İngiltere"
            searchStr.contains("united states") || searchStr.contains("usa") || searchStr.contains("us:") || searchStr.contains("[us]") || searchStr.contains("us ") || searchStr.contains("america") -> "ABD"
            searchStr.contains("france") || searchStr.contains("fransa") || searchStr.contains("fr:") || searchStr.contains("[fr]") || searchStr.contains("fr ") -> "Fransa"
            searchStr.contains("italy") || searchStr.contains("italya") || searchStr.contains("it:") || searchStr.contains("[it]") -> "İtalya"
            searchStr.contains("spain") || searchStr.contains("ispanya") || searchStr.contains("es:") || searchStr.contains("[es]") -> "İspanya"
            searchStr.contains("russia") || searchStr.contains("rusya") || searchStr.contains("ru:") || searchStr.contains("[ru]") -> "Rusya"
            searchStr.contains("türkiye") || searchStr.contains("turkey") || searchStr.contains("tr:") || searchStr.contains("[tr]") || searchStr.contains(" tr ") || defaultLanguage == "tr" -> "Türkiye"
            else -> "Global / Diğer"
        }
    }

    fun getSmartCategory(name: String, groupTitle: String, tvgId: String, sourceLanguage: String = "tr"): String {
        val searchStr = "$name $groupTitle $tvgId".lowercase(Locale.getDefault())

        // Keyword rules & scoring
        val trScore = if (searchStr.contains("türk") || searchStr.contains("turk") || searchStr.contains("tr ") || searchStr.contains("tr:") || sourceLanguage == "tr" || groupTitle.lowercase().contains("turk") || groupTitle.lowercase().contains("tr")) 5 else 0
        val newsScore = if (searchStr.contains("haber") || searchStr.contains("news") || searchStr.contains("cnn") || searchStr.contains("ntv") || searchStr.contains("trt haber") || searchStr.contains("haberturk") || searchStr.contains("sozcu") || searchStr.contains("ekol") || searchStr.contains("szc")) 8 else 0
        val sportsScore = if (searchStr.contains("spor") || searchStr.contains("sport") || searchStr.contains("bein") || searchStr.contains("match") || searchStr.contains("arena") || searchStr.contains("football") || searchStr.contains("aspor") || searchStr.contains("s sport")) 8 else 0
        val moviesScore = if (searchStr.contains("sinema") || searchStr.contains("movie") || searchStr.contains("film") || searchStr.contains("dizi") || searchStr.contains("cinema") || searchStr.contains("action") || searchStr.contains("comedy") || searchStr.contains("drama") || searchStr.contains("hbo")) 8 else 0
        val kidsScore = if (searchStr.contains("cocuk") || searchStr.contains("kids") || searchStr.contains("cartoon") || searchStr.contains("disney") || searchStr.contains("trt cocuk") || searchStr.contains("minika") || searchStr.contains("nickelodeon")) 8 else 0
        val docScore = if (searchStr.contains("belgesel") || searchStr.contains("docu") || searchStr.contains("nature") || searchStr.contains("history") || searchStr.contains("national") || searchStr.contains("geographic") || searchStr.contains("discovery") || searchStr.contains("wild")) 8 else 0
        val musicScore = if (searchStr.contains("muzik") || searchStr.contains("music") || searchStr.contains("power") || searchStr.contains("kral") || searchStr.contains("numberone") || searchStr.contains("mtv") || searchStr.contains("dream")) 8 else 0
        val nationalScore = if (searchStr.contains("show") || searchStr.contains("star") || searchStr.contains("tv8") || searchStr.contains("kanald") || searchStr.contains("kanal d") || searchStr.contains("atv") || searchStr.contains("now") || searchStr.contains("fox") || searchStr.contains("teve2") || searchStr.contains("tlc") || searchStr.contains("dmax") || searchStr.contains("ulusal")) 8 else 0
        val localScore = if (searchStr.contains("yerel") || searchStr.contains("local") || searchStr.contains("belediye") || searchStr.contains("kent") || searchStr.contains("sehir")) 9 else 0

        val maxScore = maxOf(newsScore, sportsScore, moviesScore, kidsScore, docScore, musicScore, nationalScore, localScore, trScore)

        return when {
            maxScore == 0 -> CAT_WORLD
            maxScore == newsScore -> CAT_NEWS
            maxScore == sportsScore -> CAT_SPORTS
            maxScore == moviesScore -> CAT_MOVIES
            maxScore == kidsScore -> CAT_KIDS
            maxScore == docScore -> CAT_DOCUMENTARY
            maxScore == musicScore -> CAT_MUSIC
            maxScore == localScore -> CAT_LOCAL
            maxScore == nationalScore -> CAT_NATIONAL
            maxScore == trScore -> CAT_TR
            else -> CAT_NATIONAL
        }
    }
}
