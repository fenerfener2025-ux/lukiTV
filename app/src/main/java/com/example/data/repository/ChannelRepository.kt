package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.ChannelDao
import com.example.data.local.EPGDao
import com.example.data.parser.M3UParser
import com.example.data.parser.M3UParseResult
import com.example.data.parser.XMLTVParser
import com.example.data.remote.XtreamClient
import com.example.domain.model.EPGProgram
import com.example.domain.model.IPTVChannel
import com.example.domain.util.CategoryHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class PresetSource(
    val name: String,
    val description: String,
    val url: String,
    val category: String,
    val defaultLanguage: String = "tr"
)

// Yalnızca yasal, kamuya açık ve açık kaynak referans playlistleri (kullanıcı isteğe bağlı yükleyebilir)
val OPEN_SOURCE_PRESETS = listOf(
    PresetSource(
        name = "iptv-org Türkiye",
        description = "Resmi Türk ulusal, haber, spor, müzik ve yerel canlı yayınları (Öncelikli)",
        url = "https://iptv-org.github.io/iptv/countries/tr.m3u",
        category = "TR",
        defaultLanguage = "tr"
    ),
    PresetSource(
        name = "iptv-org Spor",
        description = "Uluslararası açık yayın spor ve motor sporları kanalları",
        url = "https://iptv-org.github.io/iptv/categories/sports.m3u",
        category = "Spor",
        defaultLanguage = "en"
    ),
    PresetSource(
        name = "iptv-org Sinema & Filmler",
        description = "Uluslararası kamuya açık sinema, bağımsız film ve dizi kanalları",
        url = "https://iptv-org.github.io/iptv/categories/movies.m3u",
        category = "Sinema",
        defaultLanguage = "en"
    ),
    PresetSource(
        name = "iptv-org Belgesel",
        description = "Doğa, bilim, tarih ve vahşi yaşam belgesel yayınları",
        url = "https://iptv-org.github.io/iptv/categories/documentary.m3u",
        category = "Belgesel",
        defaultLanguage = "en"
    ),
    PresetSource(
        name = "iptv-org Çocuk & Eğlence",
        description = "Eğitici çocuk programları, çizgi filmler ve animasyon kanalları",
        url = "https://iptv-org.github.io/iptv/categories/kids.m3u",
        category = "Çocuk",
        defaultLanguage = "en"
    ),
    PresetSource(
        name = "iptv-org Müzik",
        description = "Dünya genelinden pop, rock, klasik ve caz müzik televizyonları",
        url = "https://iptv-org.github.io/iptv/categories/music.m3u",
        category = "Müzik",
        defaultLanguage = "en"
    ),
    PresetSource(
        name = "iptv-org Haber",
        description = "Uluslararası açık erişim canlı haber ve gündem kanalları",
        url = "https://iptv-org.github.io/iptv/categories/news.m3u",
        category = "Haber",
        defaultLanguage = "en"
    ),
    PresetSource(
        name = "iptv-org Dünya Geneli",
        description = "8000+ uluslararası açık yayın ve resmi kamu televizyonları",
        url = "https://iptv-org.github.io/iptv/index.m3u",
        category = "Global",
        defaultLanguage = "en"
    )
)

val STABLE_TURKISH_PRESEEDED_CHANNELS = listOf(
    IPTVChannel(
        id = "trt_1_hd",
        name = "TRT 1 HD",
        normalizedName = "trt 1",
        logoUrl = "",
        category = CategoryHelper.CAT_NATIONAL,
        groupTitle = "TR",
        streamUrl = "https://trt.daioncdn.net/trt-1/master.m3u8?app=web",
        streamMirrors = listOf("https://tv-trt1.medya.trt.com.tr/master.m3u8"),
        tvgId = "TRT1",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "kanald_hd",
        name = "Kanal D HD",
        normalizedName = "kanal d",
        logoUrl = "",
        category = CategoryHelper.CAT_NATIONAL,
        groupTitle = "TR",
        streamUrl = "https://demiroren-live.daioncdn.net/kanald/kanald.m3u8",
        streamMirrors = emptyList(),
        tvgId = "KanalD",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "showtv_hd",
        name = "Show TV HD",
        normalizedName = "show tv",
        logoUrl = "",
        category = CategoryHelper.CAT_NATIONAL,
        groupTitle = "TR",
        streamUrl = "https://ciner-live.daioncdn.net/showtv/showtv.m3u8",
        streamMirrors = emptyList(),
        tvgId = "ShowTV",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "startv_hd",
        name = "Star TV HD",
        normalizedName = "star tv",
        logoUrl = "",
        category = CategoryHelper.CAT_NATIONAL,
        groupTitle = "TR",
        streamUrl = "https://dogus-live.daioncdn.net/startv/startv.m3u8",
        streamMirrors = emptyList(),
        tvgId = "StarTV",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "atv_hd",
        name = "ATV HD",
        normalizedName = "atv",
        logoUrl = "",
        category = CategoryHelper.CAT_NATIONAL,
        groupTitle = "TR",
        streamUrl = "https://trkvz-live.daioncdn.net/atv/atv.m3u8",
        streamMirrors = listOf("https://trkvz.daioncdn.net/atv/atv.m3u8"),
        tvgId = "ATV",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "tv8_hd",
        name = "TV8 HD",
        normalizedName = "tv8",
        logoUrl = "",
        category = CategoryHelper.CAT_NATIONAL,
        groupTitle = "TR",
        streamUrl = "https://tv8-live.daioncdn.net/tv8/tv8.m3u8",
        streamMirrors = emptyList(),
        tvgId = "TV8",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "a2_hd",
        name = "A2 HD",
        normalizedName = "a2",
        logoUrl = "",
        category = CategoryHelper.CAT_NATIONAL,
        groupTitle = "TR",
        streamUrl = "https://trkvz-live.daioncdn.net/a2/a2.m3u8",
        streamMirrors = emptyList(),
        tvgId = "A2",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "trthaber_hd",
        name = "TRT Haber HD",
        normalizedName = "trt haber",
        logoUrl = "",
        category = CategoryHelper.CAT_NEWS,
        groupTitle = "TR",
        streamUrl = "https://tv-trthaber.medya.trt.com.tr/master.m3u8",
        streamMirrors = emptyList(),
        tvgId = "TRTHaber",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "ntv_hd",
        name = "NTV HD",
        normalizedName = "ntv",
        logoUrl = "",
        category = CategoryHelper.CAT_NEWS,
        groupTitle = "TR",
        streamUrl = "https://dogus-live.daioncdn.net/ntv/ntv.m3u8",
        streamMirrors = emptyList(),
        tvgId = "NTV",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "haberturk_hd",
        name = "HaberTürk HD",
        normalizedName = "haberturk",
        logoUrl = "",
        category = CategoryHelper.CAT_NEWS,
        groupTitle = "TR",
        streamUrl = "https://ciner-live.daioncdn.net/haberturk/haberturk.m3u8",
        streamMirrors = emptyList(),
        tvgId = "HaberTurk",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "cnnturk_hd",
        name = "CNN Türk HD",
        normalizedName = "cnn turk",
        logoUrl = "",
        category = CategoryHelper.CAT_NEWS,
        groupTitle = "TR",
        streamUrl = "https://demiroren-live.daioncdn.net/cnnturk/cnnturk.m3u8",
        streamMirrors = emptyList(),
        tvgId = "CNNTurk",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "ahaber_hd",
        name = "A Haber HD",
        normalizedName = "a haber",
        logoUrl = "",
        category = CategoryHelper.CAT_NEWS,
        groupTitle = "TR",
        streamUrl = "https://trkvz-live.daioncdn.net/ahaber/ahaber.m3u8",
        streamMirrors = emptyList(),
        tvgId = "AHaber",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "trtspor_hd",
        name = "TRT Spor HD",
        normalizedName = "trt spor",
        logoUrl = "",
        category = CategoryHelper.CAT_SPORTS,
        groupTitle = "TR",
        streamUrl = "https://trt.daioncdn.net/trtspor/master.m3u8?app=web",
        streamMirrors = listOf("https://tv-trtspor.medya.trt.com.tr/master.m3u8"),
        tvgId = "TRTSpor",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "aspor_hd",
        name = "A Spor HD",
        normalizedName = "a spor",
        logoUrl = "",
        category = CategoryHelper.CAT_SPORTS,
        groupTitle = "TR",
        streamUrl = "https://trkvz-live.daioncdn.net/aspor/aspor.m3u8",
        streamMirrors = emptyList(),
        tvgId = "ASpor",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "trtbelgesel_hd",
        name = "TRT Belgesel HD",
        normalizedName = "trt belgesel",
        logoUrl = "",
        category = CategoryHelper.CAT_DOCUMENTARY,
        groupTitle = "TR",
        streamUrl = "https://tv-trtbelgesel-dai.medya.trt.com.tr/master.m3u8",
        streamMirrors = emptyList(),
        tvgId = "TRTBelgesel",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "tgrtbelgesel_hd",
        name = "TGRT Belgesel HD",
        normalizedName = "tgrt belgesel",
        logoUrl = "",
        category = CategoryHelper.CAT_DOCUMENTARY,
        groupTitle = "TR",
        streamUrl = "https://tgrt-live.daioncdn.net/tgrtbelgesel/tgrtbelgesel.m3u8",
        streamMirrors = emptyList(),
        tvgId = "TGRTBelgesel",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "trtcocuk_hd",
        name = "TRT Çocuk HD",
        normalizedName = "trt cocuk",
        logoUrl = "",
        category = CategoryHelper.CAT_KIDS,
        groupTitle = "TR",
        streamUrl = "https://tv-trtcocuk.medya.trt.com.tr/master.m3u8",
        streamMirrors = emptyList(),
        tvgId = "TRTCocuk",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "minikacocuk_hd",
        name = "Minika Çocuk HD",
        normalizedName = "minika cocuk",
        logoUrl = "",
        category = CategoryHelper.CAT_KIDS,
        groupTitle = "TR",
        streamUrl = "https://trkvz-live.daioncdn.net/minikacocuk/minikacocuk.m3u8",
        streamMirrors = emptyList(),
        tvgId = "MinikaCocuk",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "minikago_hd",
        name = "Minika Go HD",
        normalizedName = "minika go",
        logoUrl = "",
        category = CategoryHelper.CAT_KIDS,
        groupTitle = "TR",
        streamUrl = "https://trkvz-live.daioncdn.net/minikago/minikago.m3u8",
        streamMirrors = emptyList(),
        tvgId = "MinikaGo",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "trtmuzik_hd",
        name = "TRT Müzik HD",
        normalizedName = "trt muzik",
        logoUrl = "",
        category = CategoryHelper.CAT_MUSIC,
        groupTitle = "TR",
        streamUrl = "https://tv-trtmuzik.medya.trt.com.tr/master.m3u8",
        streamMirrors = emptyList(),
        tvgId = "TRTMuzik",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "kralpoptv_hd",
        name = "Kral Pop TV HD",
        normalizedName = "kral pop tv",
        logoUrl = "",
        category = CategoryHelper.CAT_MUSIC,
        groupTitle = "TR",
        streamUrl = "https://dogus-live.daioncdn.net/kralpoptv/kralpoptv.m3u8",
        streamMirrors = emptyList(),
        tvgId = "KralPop",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    IPTVChannel(
        id = "kraltv_hd",
        name = "Kral TV HD",
        normalizedName = "kral tv",
        logoUrl = "",
        category = CategoryHelper.CAT_MUSIC,
        groupTitle = "TR",
        streamUrl = "https://dogus-live.daioncdn.net/kraltv/kraltv.m3u8",
        streamMirrors = emptyList(),
        tvgId = "KralTV",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    )
)

class ChannelRepository(
    private val channelDao: ChannelDao,
    private val epgDao: EPGDao,
    private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun seedDefaultChannelsIfEmpty() = withContext(Dispatchers.IO) {
        val existing = channelDao.getAllChannels()
        if (existing.isEmpty()) {
            Log.d("ChannelRepository", "Database is empty. Seeding highly stable official CDN Turkish channels.")
            channelDao.insertChannels(STABLE_TURKISH_PRESEEDED_CHANNELS)
        }
    }

    val allChannelsFlow: Flow<List<IPTVChannel>> = channelDao.getAllChannelsFlow().flowOn(Dispatchers.IO)
    val favoriteChannelsFlow: Flow<List<IPTVChannel>> = channelDao.getFavoriteChannelsFlow().flowOn(Dispatchers.IO)
    val recentlyWatchedChannelsFlow: Flow<List<IPTVChannel>> = channelDao.getRecentlyWatchedChannelsFlow().flowOn(Dispatchers.IO)
    val categoriesFlow: Flow<List<String>> = channelDao.getCategoriesFlow().flowOn(Dispatchers.IO)

    fun getChannelsByCategoryFlow(category: String): Flow<List<IPTVChannel>> {
        return channelDao.getChannelsByCategoryFlow(category).flowOn(Dispatchers.IO)
    }

    suspend fun getChannelById(id: String): IPTVChannel? = withContext(Dispatchers.IO) {
        channelDao.getChannelById(id)
    }

    suspend fun toggleFavorite(id: String) = withContext(Dispatchers.IO) {
        val channel = channelDao.getChannelById(id) ?: return@withContext
        channelDao.updateFavoriteStatus(id, !channel.isFavorite)
    }

    suspend fun updateLastWatched(id: String) = withContext(Dispatchers.IO) {
        channelDao.updateLastWatchedTimestamp(id, System.currentTimeMillis())
    }

    suspend fun clearAllChannels() = withContext(Dispatchers.IO) {
        channelDao.clearAllChannels()
        epgDao.clearAllEPG()
    }

    private fun getQualityScore(name: String, url: String): Int {
        val search = "$name $url".lowercase()
        return when {
            search.contains("8k") -> 100
            search.contains("4k") || search.contains("uhd") -> 90
            search.contains("fhd") || search.contains("1080p") -> 80
            search.contains("hd") || search.contains("720p") -> 60
            search.contains("sd") || search.contains("hevc") -> 40
            else -> 50 // default
        }
    }

    // Merge & Deduplicate channels by normalized name to provide automatic fallback quality mirrors
    private fun mergeAndDeduplicate(channels: List<IPTVChannel>): List<IPTVChannel> {
        val grouped = channels.groupBy { channel ->
            "${channel.country}##${channel.normalizedName}"
        }

        return grouped.map { (_, groupChannels) ->
            // Sort by quality score descending so the best quality stream becomes primary
            val sortedChannels = groupChannels.sortedByDescending { getQualityScore(it.name, it.streamUrl) }
            val primary = sortedChannels.first()
            if (sortedChannels.size > 1) {
                val mirrors = sortedChannels.drop(1).map { it.streamUrl }
                primary.copy(streamMirrors = (primary.streamMirrors + mirrors).distinct())
            } else {
                primary
            }
        }
    }

    suspend fun addCustomPlaylistFromUrl(url: String, playlistName: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP Hatası: ${response.code}"))
                val stream = response.body?.byteStream() ?: return@withContext Result.failure(Exception("Boş yanıt alındı."))
                val parseResult = M3UParser.parseWithMetadata(stream, defaultLanguage = "tr", isCustom = true)
                if (parseResult.channels.isEmpty()) {
                    return@withContext Result.failure(Exception("M3U dosyasında geçerli kanal bulunamadı."))
                }

                val merged = mergeAndDeduplicate(parseResult.channels.map { it.copy(groupTitle = it.groupTitle.ifEmpty { playlistName }) })
                channelDao.insertChannels(merged)

                // Optional EPG found in #EXTM3U header
                parseResult.epgUrl?.let { epgUrl ->
                    try {
                        syncEPGFromUrl(epgUrl)
                    } catch (e: Exception) {
                        Log.e("ChannelRepository", "EPG senkronizasyon hatası: ${e.message}")
                    }
                }

                Result.success(merged.size)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addCustomPlaylistFromFile(inputStream: InputStream, playlistName: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val parseResult = M3UParser.parseWithMetadata(inputStream, defaultLanguage = "tr", isCustom = true)
            if (parseResult.channels.isEmpty()) {
                return@withContext Result.failure(Exception("Dosyada kanal akışı bulunamadı."))
            }

            val merged = mergeAndDeduplicate(parseResult.channels.map { it.copy(groupTitle = it.groupTitle.ifEmpty { playlistName }) })
            channelDao.insertChannels(merged)

            parseResult.epgUrl?.let { epgUrl ->
                try {
                    syncEPGFromUrl(epgUrl)
                } catch (e: Exception) {
                    Log.e("ChannelRepository", "Yerel dosya EPG hatası: ${e.message}")
                }
            }

            Result.success(merged.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addXtreamSource(serverUrl: String, username: String, password: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val fetched = XtreamClient.fetchChannels(serverUrl, username, password)
            if (fetched.isEmpty()) return@withContext Result.failure(Exception("Xtream sunucusunda kanal bulunamadı veya yetkilendirme başarısız."))
            val merged = mergeAndDeduplicate(fetched)
            channelDao.insertChannels(merged)
            Result.success(merged.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadPresetSource(source: PresetSource, onProgress: (String) -> Unit): Result<Int> = withContext(Dispatchers.IO) {
        try {
            onProgress("${source.name} indiriliyor...")
            val request = Request.Builder()
                .url(source.url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("Bağlantı hatası: ${response.code}"))
                val stream = response.body?.byteStream() ?: return@withContext Result.failure(Exception("Boş yanıt"))

                onProgress("${source.name} ayrıştırılıyor...")
                val parseResult = M3UParser.parseWithMetadata(stream, defaultLanguage = source.defaultLanguage, isCustom = false)
                val channels = parseResult.channels

                if (channels.isEmpty()) {
                    return@withContext Result.failure(Exception("Kanal listesi boş"))
                }

                val processed = channels.map { channel ->
                    val detectedCountry = CategoryHelper.detectCountry(channel.name, channel.groupTitle, source.defaultLanguage)
                    channel.copy(
                        country = detectedCountry,
                        groupTitle = channel.groupTitle.ifEmpty { source.category }
                    )
                }

                val merged = mergeAndDeduplicate(processed)
                channelDao.insertChannels(merged)

                // EPG if provided
                parseResult.epgUrl?.let { epgUrl ->
                    onProgress("Program rehberi (EPG) alınıyor...")
                    try {
                        syncEPGFromUrl(epgUrl)
                    } catch (e: Exception) {
                        Log.e("ChannelRepository", "EPG yükleme hatası: ${e.message}")
                    }
                }

                Result.success(merged.size)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncEPGFromUrl(epgUrl: String) = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(epgUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext
                var stream = response.body?.byteStream() ?: return@withContext
                if (epgUrl.endsWith(".gz", ignoreCase = true)) {
                    stream = java.util.zip.GZIPInputStream(stream)
                }
                val programs = XMLTVParser.parse(stream)
                if (programs.isNotEmpty()) {
                    epgDao.clearAllEPG()
                    epgDao.insertPrograms(programs)
                    Log.d("ChannelRepository", "XMLTV'den ${programs.size} program içeri aktarıldı.")
                }
            }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "EPG senkronizasyon hatası: ${e.message}", e)
        }
    }

    suspend fun getProgramsForChannel(channelId: String): List<EPGProgram> = withContext(Dispatchers.IO) {
        epgDao.getProgramsForChannel(channelId)
    }

    suspend fun getCurrentProgram(channelId: String): EPGProgram? = withContext(Dispatchers.IO) {
        epgDao.getProgramAtTime(channelId, System.currentTimeMillis())
    }

    suspend fun getUpcomingPrograms(channelId: String): List<EPGProgram> = withContext(Dispatchers.IO) {
        epgDao.getUpcomingProgramsForChannel(channelId, System.currentTimeMillis())
    }
}
