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
        description = "Resmi Türk ulusal ve tematik açık kaynak canlı yayınları",
        url = "https://iptv-org.github.io/iptv/countries/tr.m3u",
        category = "TR",
        defaultLanguage = "tr"
    ),
    PresetSource(
        name = "iptv-org Spor",
        description = "Dünya genelinden açık yayın spor kanalları",
        url = "https://iptv-org.github.io/iptv/categories/sports.m3u",
        category = "Spor",
        defaultLanguage = "en"
    ),
    PresetSource(
        name = "iptv-org Dünya Geneli",
        description = "8000+ uluslararası açık yayın ve kamu kanalları",
        url = "https://iptv-org.github.io/iptv/index.m3u",
        category = "Global",
        defaultLanguage = "en"
    ),
    PresetSource(
        name = "iptv-org Haber",
        description = "Uluslararası açık erişim haber kanalları",
        url = "https://iptv-org.github.io/iptv/categories/news.m3u",
        category = "Haber",
        defaultLanguage = "en"
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
