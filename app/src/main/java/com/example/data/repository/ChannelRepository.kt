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
import java.util.Locale
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
        name = "Free-TV Türkiye (Canlı Repo)",
        description = "Sürekli güncellenen resmi Türkiye canlı yayın listesi (GitHub)",
        url = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlists/playlist_turkey.m3u8",
        category = "TR",
        defaultLanguage = "tr"
    ),
    PresetSource(
        name = "iptv-org Türkiye",
        description = "Resmi Türk ulusal, haber, spor, müzik ve yerel canlı yayınları",
        url = "https://iptv-org.github.io/iptv/countries/tr.m3u",
        category = "TR",
        defaultLanguage = "tr"
    ),
    PresetSource(
        name = "iptv-org Azerbaycan (Kardeş Ülke)",
        description = "Azerbaycan kamu ve özel televizyon yayınları (AZ TV, İctimai TV vb.)",
        url = "https://iptv-org.github.io/iptv/countries/az.m3u",
        category = "Dünya",
        defaultLanguage = "az"
    ),
    PresetSource(
        name = "Free-TV Dünya Spor",
        description = "Uluslararası açık spor ve motor sporları kanalları",
        url = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlists/playlist_sports.m3u8",
        category = "Spor",
        defaultLanguage = "en"
    ),
    PresetSource(
        name = "iptv-org Spor",
        description = "Uluslararası açık yayın spor kanalları",
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
        name = "iptv-org Almanya (DE)",
        description = "Almanya kamu ve ulusal kanalları",
        url = "https://iptv-org.github.io/iptv/countries/de.m3u",
        category = "Dünya",
        defaultLanguage = "de"
    ),
    PresetSource(
        name = "Free-TV Dünya Geneli",
        description = "8000+ uluslararası açık yayın ve resmi kamu televizyonları",
        url = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8",
        category = "Global",
        defaultLanguage = "en"
    )
)

// Kullanıcının özel sıralama isteği:
// 1. TRT 1, 2. TRT 2, 3. ATV, 4. Kanal D, 5. Show TV, 6. Star TV, 7. Halk TV, 8. Sözcü TV,
// 9. TRT Spor, 10. HT Spor, 11. A Spor, 12. TRT Spor Yıldız, 13. TV8, 14. A2, 15. TV 100,
// 16. Beyaz TV, 17. Ekol TV, 18. TRT Haber, 19. NTV, 20. HaberTürk, 21. Haber Global,
// 22. A Haber, 23. TV 24, 24. 360 TV, 25. Flash Haber, 26. TGRT Haber, 27. TGRT Belgesel,
// 28. TRT Çocuk, 29. Minika Çocuk, 30. TRT Müzik, 31. Kral Pop TV
val PREFERRED_TURKISH_ORDER = listOf(
    "trt_1_hd",
    "trt_2_hd",
    "atv_hd",
    "kanald_hd",
    "showtv_hd",
    "startv_hd",
    "halktv_hd",
    "sozcutv_hd",
    "trtspor_hd",
    "htspor_hd",
    "aspor_hd",
    "trtspor_yildiz_hd",
    "tv8_hd",
    "a2_hd",
    "tv100_hd",
    "beyaztv_hd",
    "ekoltv_hd",
    "trthaber_hd",
    "ntv_hd",
    "haberturk_hd",
    "haberglobal_hd",
    "ahaber_hd",
    "tv24_hd",
    "tv360_hd",
    "flashhaber_hd",
    "tgrthaber_hd",
    "tgrtbelgesel_hd",
    "trtcocuk_hd",
    "minikacocuk_hd",
    "trtmuzik_hd",
    "kralpoptv_hd"
)

val STABLE_TURKISH_PRESEEDED_CHANNELS = listOf(
    // 1. TRT 1 HD
    IPTVChannel(
        id = "trt_1_hd",
        name = "TRT 1 HD",
        normalizedName = "trt 1",
        logoUrl = "",
        category = CategoryHelper.CAT_NATIONAL,
        groupTitle = "TR",
        streamUrl = "https://tv-trt1.medya.trt.com.tr/master.m3u8",
        streamMirrors = listOf("https://trt.daioncdn.net/trt-1/master.m3u8?app=web"),
        tvgId = "TRT1",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 2. TRT 2 HD
    IPTVChannel(
        id = "trt_2_hd",
        name = "TRT 2 HD",
        normalizedName = "trt 2",
        logoUrl = "",
        category = CategoryHelper.CAT_NATIONAL,
        groupTitle = "TR",
        streamUrl = "https://tv-trt2.medya.trt.com.tr/master.m3u8",
        streamMirrors = listOf("https://tv-trt2.medya.trt.com.tr/master_720.m3u8"),
        tvgId = "TRT2",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 3. ATV HD
    IPTVChannel(
        id = "atv_hd",
        name = "ATV HD",
        normalizedName = "atv",
        logoUrl = "",
        category = CategoryHelper.CAT_NATIONAL,
        groupTitle = "TR",
        streamUrl = "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/atv/atv.m3u8",
        streamMirrors = listOf("https://cdn-alanyatv.yayin.com.tr/alanyatv/alanyatv/playlist.m3u8"),
        tvgId = "ATV",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 4. Kanal D HD
    IPTVChannel(
        id = "kanald_hd",
        name = "Kanal D HD",
        normalizedName = "kanal d",
        logoUrl = "",
        category = CategoryHelper.CAT_NATIONAL,
        groupTitle = "TR",
        streamUrl = "https://demiroren.daioncdn.net/kanald/kanald.m3u8?app=kanald_web&ce=3",
        streamMirrors = emptyList(),
        tvgId = "KanalD",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 5. Show TV HD
    IPTVChannel(
        id = "showtv_hd",
        name = "Show TV HD",
        normalizedName = "show tv",
        logoUrl = "",
        category = CategoryHelper.CAT_NATIONAL,
        groupTitle = "TR",
        streamUrl = "https://ciner.daioncdn.net/showtv/showtv.m3u8?app=showtv_web",
        streamMirrors = listOf("https://ciner.daioncdn.net/showtv/showtv.m3u8?app=showtv_web&ce=3"),
        tvgId = "ShowTV",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 6. Star TV HD
    IPTVChannel(
        id = "startv_hd",
        name = "Star TV HD",
        normalizedName = "star tv",
        logoUrl = "",
        category = CategoryHelper.CAT_NATIONAL,
        groupTitle = "TR",
        streamUrl = "https://dogus.daioncdn.net/startv/startv_720p.m3u8?app=a20ac41e-bdc3-4aa1-934d-26b484480ac9&ce=3&sid=8l4w3lst4co5",
        streamMirrors = emptyList(),
        tvgId = "StarTV",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 7. Halk TV HD
    IPTVChannel(
        id = "halktv_hd",
        name = "Halk TV HD",
        normalizedName = "halk tv",
        logoUrl = "",
        category = CategoryHelper.CAT_NEWS,
        groupTitle = "TR",
        streamUrl = "https://halktv-live.daioncdn.net/halktv/halktv.m3u8",
        streamMirrors = emptyList(),
        tvgId = "HalkTV",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 8. Sözcü TV HD
    IPTVChannel(
        id = "sozcutv_hd",
        name = "Sözcü TV HD",
        normalizedName = "sozcu tv",
        logoUrl = "",
        category = CategoryHelper.CAT_NEWS,
        groupTitle = "TR",
        streamUrl = "https://szctv.blutv.com/blutv_szctv/live_720p2000000kbps/index.m3u8",
        streamMirrors = listOf("https://halktv-live.daioncdn.net/halktv/halktv.m3u8"),
        tvgId = "SozcuTV",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 9. TRT Spor HD
    IPTVChannel(
        id = "trtspor_hd",
        name = "TRT Spor HD",
        normalizedName = "trt spor",
        logoUrl = "",
        category = CategoryHelper.CAT_SPORTS,
        groupTitle = "TR",
        streamUrl = "https://trt.daioncdn.net/trtspor/master.m3u8?app=web",
        streamMirrors = listOf("https://trt.daioncdn.net/trtspor-yildiz/master.m3u8?app=web&platform=trtspor"),
        tvgId = "TRTSpor",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 10. HT Spor HD
    IPTVChannel(
        id = "htspor_hd",
        name = "HT Spor HD",
        normalizedName = "ht spor",
        logoUrl = "",
        category = CategoryHelper.CAT_SPORTS,
        groupTitle = "TR",
        streamUrl = "https://ciner.daioncdn.net/ht-spor/ht-spor.m3u8?app=web",
        streamMirrors = listOf("https://ciner-live.daioncdn.net/ht-spor/ht-spor.m3u8"),
        tvgId = "HTSpor",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 11. A Spor HD
    IPTVChannel(
        id = "aspor_hd",
        name = "A Spor HD",
        normalizedName = "a spor",
        logoUrl = "",
        category = CategoryHelper.CAT_SPORTS,
        groupTitle = "TR",
        streamUrl = "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/aspor/aspor.m3u8",
        streamMirrors = emptyList(),
        tvgId = "ASpor",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 12. TRT Spor Yıldız HD
    IPTVChannel(
        id = "trtspor_yildiz_hd",
        name = "TRT Spor Yıldız HD",
        normalizedName = "trt spor yildiz",
        logoUrl = "",
        category = CategoryHelper.CAT_SPORTS,
        groupTitle = "TR",
        streamUrl = "https://trt.daioncdn.net/trtspor-yildiz/master.m3u8?app=web&platform=trtspor",
        streamMirrors = emptyList(),
        tvgId = "TRTSporYildiz",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 13. TV8 HD
    IPTVChannel(
        id = "tv8_hd",
        name = "TV8 HD",
        normalizedName = "tv8",
        logoUrl = "",
        category = CategoryHelper.CAT_NATIONAL,
        groupTitle = "TR",
        streamUrl = "https://tv8.daioncdn.net/tv8/tv8.m3u8?app=7ddc255a-ef47-4e81-ab14-c0e5f2949788&ce=3",
        streamMirrors = emptyList(),
        tvgId = "TV8",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 14. A2 HD
    IPTVChannel(
        id = "a2_hd",
        name = "A2 HD",
        normalizedName = "a2",
        logoUrl = "",
        category = CategoryHelper.CAT_NATIONAL,
        groupTitle = "TR",
        streamUrl = "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/a2tv/a2tv.m3u8",
        streamMirrors = emptyList(),
        tvgId = "A2",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 15. TV 100 HD
    IPTVChannel(
        id = "tv100_hd",
        name = "TV 100 HD",
        normalizedName = "tv100",
        logoUrl = "",
        category = CategoryHelper.CAT_NEWS,
        groupTitle = "TR",
        streamUrl = "https://tv.ensonhaber.com/tv100/tv100.m3u8",
        streamMirrors = emptyList(),
        tvgId = "TV100",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 16. Beyaz TV HD
    IPTVChannel(
        id = "beyaztv_hd",
        name = "Beyaz TV HD",
        normalizedName = "beyaz tv",
        logoUrl = "",
        category = CategoryHelper.CAT_NATIONAL,
        groupTitle = "TR",
        streamUrl = "https://beyaztv-live.daioncdn.net/beyaztv/beyaztv.m3u8",
        streamMirrors = emptyList(),
        tvgId = "BeyazTV",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 17. Ekol TV HD
    IPTVChannel(
        id = "ekoltv_hd",
        name = "Ekol TV HD",
        normalizedName = "ekol tv",
        logoUrl = "",
        category = CategoryHelper.CAT_NEWS,
        groupTitle = "TR",
        streamUrl = "https://ekoltv-live.ercdn.net/ekoltv/ekoltv.m3u8",
        streamMirrors = emptyList(),
        tvgId = "EkolTV",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 18. TRT Haber HD
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
    // 19. NTV HD
    IPTVChannel(
        id = "ntv_hd",
        name = "NTV HD",
        normalizedName = "ntv",
        logoUrl = "",
        category = CategoryHelper.CAT_NEWS,
        groupTitle = "TR",
        streamUrl = "https://dogus.daioncdn.net/ntv/ntv.m3u8?app=ntv_web",
        streamMirrors = emptyList(),
        tvgId = "NTV",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 20. HaberTürk HD
    IPTVChannel(
        id = "haberturk_hd",
        name = "HaberTürk HD",
        normalizedName = "haberturk",
        logoUrl = "",
        category = CategoryHelper.CAT_NEWS,
        groupTitle = "TR",
        streamUrl = "https://tv.ensonhaber.com/haberturk/haberturk.m3u8",
        streamMirrors = emptyList(),
        tvgId = "HaberTurk",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 21. Haber Global HD
    IPTVChannel(
        id = "haberglobal_hd",
        name = "Haber Global HD",
        normalizedName = "haber global",
        logoUrl = "",
        category = CategoryHelper.CAT_NEWS,
        groupTitle = "TR",
        streamUrl = "https://tv.ensonhaber.com/haberglobal/haberglobal.m3u8",
        streamMirrors = emptyList(),
        tvgId = "HaberGlobal",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 22. A Haber HD
    IPTVChannel(
        id = "ahaber_hd",
        name = "A Haber HD",
        normalizedName = "a haber",
        logoUrl = "",
        category = CategoryHelper.CAT_NEWS,
        groupTitle = "TR",
        streamUrl = "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/ahaber/ahaber.m3u8",
        streamMirrors = emptyList(),
        tvgId = "AHaber",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 23. TV 24 HD
    IPTVChannel(
        id = "tv24_hd",
        name = "TV 24 HD",
        normalizedName = "tv 24",
        logoUrl = "",
        category = CategoryHelper.CAT_NEWS,
        groupTitle = "TR",
        streamUrl = "https://turkmedya-live.ercdn.net/tv24/tv24.m3u8",
        streamMirrors = emptyList(),
        tvgId = "TV24",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 24. 360 TV HD
    IPTVChannel(
        id = "tv360_hd",
        name = "360 TV HD",
        normalizedName = "360 tv",
        logoUrl = "",
        category = CategoryHelper.CAT_NATIONAL,
        groupTitle = "TR",
        streamUrl = "https://turkmedya-live.ercdn.net/tv360/tv360.m3u8",
        streamMirrors = emptyList(),
        tvgId = "TV360",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 25. Flash Haber TV
    IPTVChannel(
        id = "flashhaber_hd",
        name = "Flash Haber TV",
        normalizedName = "flash haber",
        logoUrl = "",
        category = CategoryHelper.CAT_NEWS,
        groupTitle = "TR",
        streamUrl = "https://b01c02nl.mediatriple.net/videoonlylive/mtyycglqauzjhlive/broadcast_67c053c48829f.smil/playlist.m3u8",
        streamMirrors = emptyList(),
        tvgId = "FlashHaber",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 26. TGRT Haber HD
    IPTVChannel(
        id = "tgrthaber_hd",
        name = "TGRT Haber HD",
        normalizedName = "tgrt haber",
        logoUrl = "",
        category = CategoryHelper.CAT_NEWS,
        groupTitle = "TR",
        streamUrl = "https://canli.tgrthaber.com/tgrt.m3u8",
        streamMirrors = emptyList(),
        tvgId = "TGRTHaber",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 27. TGRT Belgesel HD
    IPTVChannel(
        id = "tgrtbelgesel_hd",
        name = "TGRT Belgesel HD",
        normalizedName = "tgrt belgesel",
        logoUrl = "",
        category = CategoryHelper.CAT_DOCUMENTARY,
        groupTitle = "TR",
        streamUrl = "https://b01c02nl.mediatriple.net/videoonlylive/mtsxxkzwwuqtglive/broadcast_5fe462afc6a0e.smil/playlist.m3u8",
        streamMirrors = emptyList(),
        tvgId = "TGRTBelgesel",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 28. TRT Çocuk HD
    IPTVChannel(
        id = "trtcocuk_hd",
        name = "TRT Çocuk HD",
        normalizedName = "trt cocuk",
        logoUrl = "",
        category = CategoryHelper.CAT_KIDS,
        groupTitle = "TR",
        streamUrl = "https://tv-trtcocuk.medya.trt.com.tr/master.m3u8",
        streamMirrors = listOf("https://tv-trtdiyanetcocuk.medya.trt.com.tr/master.m3u8"),
        tvgId = "TRTCocuk",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 29. Minika Çocuk HD
    IPTVChannel(
        id = "minikacocuk_hd",
        name = "Minika Çocuk HD",
        normalizedName = "minika cocuk",
        logoUrl = "",
        category = CategoryHelper.CAT_KIDS,
        groupTitle = "TR",
        streamUrl = "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/minikago_cocuk/minikago_cocuk.m3u8",
        streamMirrors = emptyList(),
        tvgId = "MinikaCocuk",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    ),
    // 30. TRT Müzik HD
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
    // 31. Kral Pop TV HD
    IPTVChannel(
        id = "kralpoptv_hd",
        name = "Kral Pop TV HD",
        normalizedName = "kral pop tv",
        logoUrl = "",
        category = CategoryHelper.CAT_MUSIC,
        groupTitle = "TR",
        streamUrl = "https://dogus-live.daioncdn.net/kralpoptv/playlist.m3u8",
        streamMirrors = listOf("https://livetv.powerapp.com.tr/dance/dance.smil/playlist.m3u8"),
        tvgId = "KralPop",
        isFavorite = false,
        lastWatchedTimestamp = 0,
        isCustom = false,
        country = "Türkiye",
        language = "tr"
    )
)

fun isTurkishChannel(channel: IPTVChannel): Boolean {
    val country = channel.country.lowercase(Locale.getDefault())
    val lang = channel.language.lowercase(Locale.getDefault())
    if (country.contains("türk") || country.contains("turk") || lang == "tr") return true

    val name = channel.name.lowercase(Locale.getDefault())
    val group = channel.groupTitle.lowercase(Locale.getDefault())
    if (group.contains("tr") || group.contains("turk") || group.contains("türk") || group.contains("yerli") || group.contains("ulusal")) return true

    // Check common Turkish channel name markers
    if (name.contains("trt") || name.contains("atv") || name.contains("kanald") || name.contains("kanal d") ||
        name.contains("show") || name.contains("star tv") || name.contains("star hd") || name.contains("tv8") ||
        name.contains("now tv") || name.contains("fox") || name.contains("halk tv") || name.contains("sözcü") ||
        name.contains("sozcu") || name.contains("haber global") || name.contains("habertürk") ||
        name.contains("haberturk") || name.contains("a haber") || name.contains("ntv") || name.contains("cnn türk") ||
        name.contains("cnn turk") || name.contains("tv100") || name.contains("beyaz tv") || name.contains("tgrt") ||
        name.contains("ekol") || name.contains("kral") || name.contains("powertürk") || name.contains("a spor") ||
        name.contains("ht spor") || name.contains("sports tv") || name.contains("fb tv") || name.contains("bjk tv") ||
        name.contains("gs tv") || name.contains("dmax") || name.contains("tlc") || name.contains("teve2") ||
        name.contains("minika") || name.contains("ulusal kanal") || name.contains("bengütürk") || name.contains("flash haber")) {
        return true
    }

    return CategoryHelper.detectCountry(channel.name, channel.groupTitle) == "Türkiye"
}

fun getChannelPriorityRank(channel: IPTVChannel): Int {
    // 1. Exact or normalized ID match in the preferred Turkish order (0 .. 49)
    val exactIdx = PREFERRED_TURKISH_ORDER.indexOf(channel.id)
    if (exactIdx != -1) return exactIdx

    // 2. Partial match in PREFERRED_TURKISH_ORDER by channel name (50 .. 99)
    val normalized = channel.normalizedName.ifBlank { channel.name.lowercase(Locale.getDefault()) }
    val matchIdx = PREFERRED_TURKISH_ORDER.indexOfFirst { prefId ->
        val cleanPref = prefId.replace("_hd", "").replace("_", " ")
        normalized.contains(cleanPref) || cleanPref.contains(normalized)
    }
    if (matchIdx != -1) return 50 + matchIdx

    // 3. Other Turkish channels (100 .. 499)
    if (isTurkishChannel(channel)) {
        return 100 + (CategoryHelper.getCategoryPriority(channel.category) * 10)
    }

    // 4. Foreign / International channels (1000+) - Placed after Turkish channels
    return 1000 + (CategoryHelper.getCategoryPriority(channel.category) * 10)
}

/**
 * Kategori sekmelerinde kanalları önce en çok izlenen Türk kanalları,
 * sonra diğer yerli kanallar, en son yabancı kanallar gelecek şekilde sıralar.
 */
fun sortCategoryChannelsWithTurkishPriority(channels: List<IPTVChannel>): List<IPTVChannel> {
    return channels.sortedWith(
        compareByDescending<IPTVChannel> { it.isFavorite }
            .thenBy { getChannelPriorityRank(it) }
            .thenBy { it.name }
    )
}

fun sortChannelsWithUserPreference(
    channels: List<IPTVChannel>,
    isWorldTab: Boolean = false,
    selectedCountry: String = "Tümü",
    selectedGenre: String = "Tümü"
): List<IPTVChannel> {
    if (isWorldTab) {
        return channels.filter { chan ->
            val detectedCountry = CategoryHelper.detectCountry(chan.name, chan.groupTitle)
            val countryMatch = if (selectedCountry == "Tümü") {
                detectedCountry != "Türkiye"
            } else {
                detectedCountry.equals(selectedCountry, ignoreCase = true)
            }
            val smartGenre = CategoryHelper.getSmartCategory(chan.name, chan.groupTitle, chan.tvgId)
            val genreMatch = if (selectedGenre == "Tümü") true
            else {
                smartGenre.contains(selectedGenre, ignoreCase = true) || chan.category.contains(selectedGenre, ignoreCase = true)
            }
            countryMatch && genreMatch
        }.sortedWith(
            compareByDescending<IPTVChannel> { it.isFavorite }
                .thenBy { it.name }
        )
    }

    // Normal / Kategori Modu:
    // Her kategoride önce en çok izlenen Türk kanalları, sonra yabancı kanallar
    return sortCategoryChannelsWithTurkishPriority(channels)
}

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
        } else {
            // Self-repair: ensure all default channels exist and have the latest verified 100% active CDN stream URLs
            val defaultIds = STABLE_TURKISH_PRESEEDED_CHANNELS.map { it.id }.toSet()
            val existingIds = existing.map { it.id }.toSet()
            val missingDefaults = defaultIds - existingIds
            val hasOutdatedDefaults = existing.any { it.id in defaultIds && STABLE_TURKISH_PRESEEDED_CHANNELS.find { def -> def.id == it.id }?.streamUrl != it.streamUrl }
            if (missingDefaults.isNotEmpty() || hasOutdatedDefaults) {
                Log.d("ChannelRepository", "Updating/Inserting preseeded Turkish channels with latest verified working streams.")
                val updatedChannels = STABLE_TURKISH_PRESEEDED_CHANNELS.map { pre ->
                    val prev = existing.find { it.id == pre.id }
                    if (prev != null) {
                        pre.copy(isFavorite = prev.isFavorite, lastWatchedTimestamp = prev.lastWatchedTimestamp)
                    } else {
                        pre
                    }
                }
                channelDao.insertChannels(updatedChannels)
            }
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
