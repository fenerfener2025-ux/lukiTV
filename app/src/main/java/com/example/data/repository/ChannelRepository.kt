package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.ChannelDao
import com.example.data.local.EPGDao
import com.example.data.parser.M3UParser
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
import java.util.Locale

class ChannelRepository(
    private val channelDao: ChannelDao,
    private val epgDao: EPGDao,
    private val context: Context
) {
    private val client = OkHttpClient()

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

    // Merge logic: De-duplicates channels by normalized name & tvgId, putting mirrors in streamMirrors
    private fun mergeAndDeduplicate(channels: List<IPTVChannel>): List<IPTVChannel> {
        val grouped = channels.groupBy { channel ->
            val keyId = channel.tvgId.ifEmpty { "no_tvg" }
            "$keyId##${channel.normalizedName}"
        }

        return grouped.map { (_, groupChannels) ->
            val primary = groupChannels.first()
            if (groupChannels.size > 1) {
                val mirrors = groupChannels.drop(1).map { it.streamUrl }
                primary.copy(streamMirrors = (primary.streamMirrors + mirrors).distinct())
            } else {
                primary
            }
        }
    }

    suspend fun addCustomPlaylistFromUrl(url: String, playlistName: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP Error ${response.code}"))
                val stream = response.body?.byteStream() ?: return@withContext Result.failure(Exception("Empty body"))
                val parsed = M3UParser.parse(stream, defaultLanguage = "tr", isCustom = true)
                val merged = mergeAndDeduplicate(parsed.map { it.copy(groupTitle = playlistName) })
                channelDao.insertChannels(merged)
                Result.success(merged.size)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addCustomPlaylistFromFile(inputStream: InputStream, playlistName: String): Int = withContext(Dispatchers.IO) {
        val parsed = M3UParser.parse(inputStream, defaultLanguage = "tr", isCustom = true)
        val merged = mergeAndDeduplicate(parsed.map { it.copy(groupTitle = playlistName) })
        channelDao.insertChannels(merged)
        merged.size
    }

    suspend fun addXtreamSource(serverUrl: String, username: String, password: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val fetched = XtreamClient.fetchChannels(serverUrl, username, password)
            if (fetched.isEmpty()) return@withContext Result.failure(Exception("Hiç kanal bulunamadı veya bağlantı hatası."))
            val merged = mergeAndDeduplicate(fetched)
            channelDao.insertChannels(merged)
            Result.success(merged.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncPresetSources(onProgress: (String) -> Unit) = withContext(Dispatchers.IO) {
        val presets = mapOf(
            "TR_PRESET" to ("https://iptv-org.github.io/iptv/countries/tr.m3u" to "tr"),
            "AZ_PRESET" to ("https://iptv-org.github.io/iptv/countries/az.m3u" to "az"),
            "US_PRESET" to ("https://iptv-org.github.io/iptv/countries/us.m3u" to "en"),
            "DE_PRESET" to ("https://iptv-org.github.io/iptv/countries/de.m3u" to "de"),
            "GB_PRESET" to ("https://iptv-org.github.io/iptv/countries/uk.m3u" to "en"),
            "FR_PRESET" to ("https://iptv-org.github.io/iptv/countries/fr.m3u" to "fr"),
            "IT_PRESET" to ("https://iptv-org.github.io/iptv/countries/it.m3u" to "it"),
            "ES_PRESET" to ("https://iptv-org.github.io/iptv/countries/es.m3u" to "es"),
            "TURKTV" to ("https://itasli.github.io/TURKTV/index.m3u" to "tr")
        )

        val allParsed = mutableListOf<IPTVChannel>()
        for ((name, pair) in presets) {
            try {
                val (url, lang) = pair
                onProgress("$name yükleniyor...")
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.byteStream()?.let { stream ->
                            val parsed = M3UParser.parse(stream, defaultLanguage = lang, isCustom = false)
                            // Override country based on the preset name to ensure country filtering works flawlessly
                            val countryOverride = when (name) {
                                "TR_PRESET", "TURKTV" -> "Türkiye"
                                "AZ_PRESET" -> "Azerbaycan"
                                "US_PRESET" -> "ABD"
                                "DE_PRESET" -> "Almanya"
                                "GB_PRESET" -> "İngiltere"
                                "FR_PRESET" -> "Fransa"
                                "IT_PRESET" -> "İtalya"
                                "ES_PRESET" -> "İspanya"
                                else -> "Bilinmiyor"
                            }
                            allParsed.addAll(parsed.map { it.copy(country = countryOverride) })
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChannelRepository", "Failed to load preset: $name", e)
            }
        }

        if (allParsed.isNotEmpty()) {
            val merged = mergeAndDeduplicate(allParsed)
            channelDao.clearPresetChannels()
            channelDao.insertChannels(merged)
        } else {
            // If offline, populate beautiful default channels so the user gets an outstanding 4K preview immediately
            populateMockChannels()
        }
    }

    suspend fun populateMockChannels() = withContext(Dispatchers.IO) {
        val mockChannels = listOf(
            IPTVChannel(
                id = "trt1",
                name = "TRT 1 HD",
                normalizedName = IPTVChannel.normalize("TRT 1"),
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/82/TRT_1_logo_%282021%29.svg/1024px-TRT_1_logo_%282021%29.svg.png",
                category = CategoryHelper.CAT_TR,
                groupTitle = "Genel",
                streamUrl = "http://tv-trt1.live.trt.com.tr/trt1/index.m3u8", // Real or mock streaming URLs
                streamMirrors = listOf(
                    "https://m-trt1.live.trt.com.tr/trt1/index.m3u8",
                    "https://html5.api.trt.net.tr/vod/hls/trt1_hd_hls.m3u8"
                ),
                tvgId = "trt1.tr",
                country = "Türkiye"
            ),
            IPTVChannel(
                id = "trtspor",
                name = "TRT Spor HD",
                normalizedName = IPTVChannel.normalize("TRT Spor"),
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/TRT_Spor_logo.svg/1024px-TRT_Spor_logo.svg.png",
                category = CategoryHelper.CAT_SPORTS,
                groupTitle = "Spor",
                streamUrl = "http://tv-trtspor.live.trt.com.tr/trtspor/index.m3u8",
                streamMirrors = listOf("https://m-trtspor.live.trt.com.tr/trtspor/index.m3u8"),
                tvgId = "trtspor.tr",
                country = "Türkiye"
            ),
            IPTVChannel(
                id = "trthaber",
                name = "TRT Haber HD",
                normalizedName = IPTVChannel.normalize("TRT Haber"),
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2a/TRT_Haber_logo_%282021%29.svg/1024px-TRT_Haber_logo_%282021%29.svg.png",
                category = CategoryHelper.CAT_NEWS,
                groupTitle = "Haber",
                streamUrl = "http://tv-trthaber.live.trt.com.tr/trthaber/index.m3u8",
                streamMirrors = listOf("https://m-trthaber.live.trt.com.tr/trthaber/index.m3u8"),
                tvgId = "trthaber.tr",
                country = "Türkiye"
            ),
            IPTVChannel(
                id = "trtcocuk",
                name = "TRT Çocuk HD",
                normalizedName = IPTVChannel.normalize("TRT Cocuk"),
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b7/TRT_%C3%87ocuk_logo.svg/1024px-TRT_%C3%87ocuk_logo.svg.png",
                category = CategoryHelper.CAT_KIDS,
                groupTitle = "Çocuk",
                streamUrl = "http://tv-trtcocuk.live.trt.com.tr/trtcocuk/index.m3u8",
                streamMirrors = listOf("https://m-trtcocuk.live.trt.com.tr/trtcocuk/index.m3u8"),
                tvgId = "trtcocuk.tr",
                country = "Türkiye"
            ),
            IPTVChannel(
                id = "trtbelgesel",
                name = "TRT Belgesel HD",
                normalizedName = IPTVChannel.normalize("TRT Belgesel"),
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bf/TRT_Belgesel_logo.svg/1024px-TRT_Belgesel_logo.svg.png",
                category = CategoryHelper.CAT_DOCUMENTARY,
                groupTitle = "Belgesel",
                streamUrl = "http://tv-trtbelgesel.live.trt.com.tr/trtbelgesel/index.m3u8",
                streamMirrors = listOf("https://m-trtbelgesel.live.trt.com.tr/trtbelgesel/index.m3u8"),
                tvgId = "trtbelgesel.tr",
                country = "Türkiye"
            ),
            IPTVChannel(
                id = "trtmuzik",
                name = "TRT Müzik HD",
                normalizedName = IPTVChannel.normalize("TRT Muzik"),
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d7/TRT_M%C3%BCzik_logo_%282021%29.svg/1200px-TRT_M%C3%BCzik_logo_%282021%29.svg.png",
                category = CategoryHelper.CAT_MUSIC,
                groupTitle = "Müzik",
                streamUrl = "http://tv-trtmuzik.live.trt.com.tr/trtmuzik/index.m3u8",
                streamMirrors = listOf("https://m-trtmuzik.live.trt.com.tr/trtmuzik/index.m3u8"),
                tvgId = "trtmuzik.tr",
                country = "Türkiye"
            ),
            // World/Demo Big Buck Bunny streams for fully responsive live player checking
            IPTVChannel(
                id = "bunny_hd",
                name = "Aurora 4K Promo (Big Buck Bunny)",
                normalizedName = IPTVChannel.normalize("Aurora Promo Bunny"),
                logoUrl = "https://peach.blender.org/wp-content/uploads/title_an_vlogo.jpg",
                category = CategoryHelper.CAT_WORLD,
                groupTitle = "Tanıtım",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                streamMirrors = listOf(
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
                ),
                tvgId = "bunny",
                country = "Global / Diğer"
            ),
            IPTVChannel(
                id = "tears_of_steel",
                name = "Aurora Cinema (Tears of Steel)",
                normalizedName = IPTVChannel.normalize("Aurora Cinema"),
                logoUrl = "https://peach.blender.org/wp-content/uploads/title_an_vlogo.jpg",
                category = CategoryHelper.CAT_MOVIES,
                groupTitle = "Sinema",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                streamMirrors = emptyList(),
                tvgId = "tears_of_steel",
                country = "Global / Diğer"
            ),
            IPTVChannel(
                id = "bbc_news",
                name = "BBC News Global HD",
                normalizedName = IPTVChannel.normalize("BBC News Global"),
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/62/BBC_News_2022.svg/800px-BBC_News_2022.svg.png",
                category = CategoryHelper.CAT_NEWS,
                groupTitle = "Haber",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingToBullrun.mp4",
                streamMirrors = emptyList(),
                tvgId = "bbc_news",
                country = "İngiltere"
            ),
            IPTVChannel(
                id = "nasa_tv",
                name = "NASA Science TV",
                normalizedName = IPTVChannel.normalize("NASA Science TV"),
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e5/NASA_logo.svg/800px-NASA_logo.svg.png",
                category = CategoryHelper.CAT_DOCUMENTARY,
                groupTitle = "Belgesel",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                streamMirrors = emptyList(),
                tvgId = "nasa_tv",
                country = "ABD"
            ),
            IPTVChannel(
                id = "aztv_hd",
                name = "AzTV Azerbaijan HD",
                normalizedName = IPTVChannel.normalize("AzTV Azerbaijan HD"),
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/2/23/AZTV-LOGO-2021.png",
                category = CategoryHelper.CAT_NATIONAL,
                groupTitle = "Ulusal",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
                streamMirrors = emptyList(),
                tvgId = "aztv_hd",
                country = "Azerbaycan"
            ),
            IPTVChannel(
                id = "zdf_germany",
                name = "ZDF Deutschland HD",
                normalizedName = IPTVChannel.normalize("ZDF Deutschland HD"),
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d4/ZDF_logo.svg/1200px-ZDF_logo.svg.png",
                category = CategoryHelper.CAT_NATIONAL,
                groupTitle = "Ulusal",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                streamMirrors = emptyList(),
                tvgId = "zdf_germany",
                country = "Almanya"
            )
        )
        channelDao.insertChannels(mockChannels)

        // Seed some beautiful EPG too!
        val now = System.currentTimeMillis()
        val programs = mutableListOf<EPGProgram>()
        val channelsToSeed = listOf("trt1", "trtspor", "trthaber", "trtcocuk", "trtbelgesel", "trtmuzik", "bunny_hd", "tears_of_steel")

        for (channelId in channelsToSeed) {
            // Create EPG blocks for 2 hours each starting 4 hours ago until 24 hours in future
            var startTime = now - (4 * 3600 * 1000)
            val duration = 2 * 3600 * 1000L
            var blockCount = 1

            while (startTime < now + (24 * 3600 * 1000)) {
                val endTime = startTime + duration
                val programTitle = when (channelId) {
                    "trt1" -> listOf("Diriliş Ertuğrul", "Gönül Dağı", "TRT Haber Bülteni", "Ana Haber", "Teşkilat")[blockCount % 5]
                    "trtspor" -> listOf("Spor Stüdyosu", "Aktüel Futbol", "Canlı Maç Yayını", "Transfer Günlüğü", "Olimpiyat Özel")[blockCount % 5]
                    "trthaber" -> listOf("Gündem Özel", "Ekonomi Günlüğü", "Satır Başı", "Manşet", "Sıcak Gelişme")[blockCount % 5]
                    "trtcocuk" -> listOf("Rafadan Tayfa", "Maysa ve Bulut", "Keloğlan", "Pırıl", "İbi")[blockCount % 5]
                    "trtbelgesel" -> listOf("Doğanın Gücü", "Tarihin İzinde", "Savaşın Efsaneleri", "Yiyeceğin Serüveni", "Derin Dereler")[blockCount % 5]
                    "trtmuzik" -> listOf("Alaturka Esintiler", "Pop Saati", "Zeki Müren Özel", "Gençlik Konseri", "Türkülerle Anadolu")[blockCount % 5]
                    "bunny_hd" -> listOf("Big Buck Bunny 4K Screening", "Sintel Adventure", "Tears of Steel Showcase", "Cosmos Laundromat", "Caminandes Llama")[blockCount % 5]
                    else -> listOf("Sci-Fi Special Edition", "Behind the Scenes VFX", "Future of Compositing", "Tears of Steel", "Post-apocalyptic Amsterdam")[blockCount % 5]
                }

                val desc = "Bu program AuroraTV EPG akıllı zamanlayıcısı tarafından otomatik listelenmiştir. Detaylı yayın akışı ve 4K UHD keyfini çıkarın."

                programs.add(
                    EPGProgram(
                        channelId = channelId,
                        startTimeMs = startTime,
                        endTimeMs = endTime,
                        title = programTitle,
                        description = desc
                    )
                )
                startTime = endTime
                blockCount++
            }
        }
        epgDao.insertPrograms(programs)
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
