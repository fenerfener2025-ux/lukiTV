package com.example.domain.model

import androidx.media3.ui.AspectRatioFrameLayout

/**
 * Ekran Boyutu ve En-Boy Oranı Modları
 */
enum class AppAspectRatio(
    val key: String,
    val title: String,
    val subtitle: String,
    val resizeMode: Int
) {
    AUTO_FIT(
        key = "AUTO_FIT",
        title = "Otomatik Uyum (Fit)",
        subtitle = "Orijinal en-boy oranını korur, ekrana sığdırır (Önerilen)",
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
    ),
    FILL_STRETCH(
        key = "FILL_STRETCH",
        title = "Tam Ekran Esnet (Fill)",
        subtitle = "Görüntüyü tüm ekrana uzatarak siyah boşluk bırakmaz",
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
    ),
    ZOOM_CROP(
        key = "ZOOM_CROP",
        title = "Ekranı Kapla (Kırp / Zoom)",
        subtitle = "Siyah şeritleri yok eder, ekranı orantılı doldurur",
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    ),
    RATIO_16_9(
        key = "RATIO_16_9",
        title = "16:9 Geniş Ekran",
        subtitle = "Standart geniş ekran modern HD televizyon formatı",
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
    ),
    RATIO_4_3(
        key = "RATIO_4_3",
        title = "4:3 Klasik TV",
        subtitle = "Nostalji ve eski tüplü yayınlar için klasik televizyon formatı",
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
    ),
    RATIO_21_9(
        key = "RATIO_21_9",
        title = "21:9 Sinema / Ultra-Geniş",
        subtitle = "Panoramik sinema filmleri ve ultra geniş ekranlar",
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    );

    companion object {
        fun fromKey(key: String): AppAspectRatio = values().find { it.key == key } ?: AUTO_FIT
    }
}

/**
 * Tamponlama / Buffer Profili
 */
enum class BufferProfile(
    val key: String,
    val title: String,
    val subtitle: String,
    val bufferMs: Int
) {
    FAST(
        key = "FAST",
        title = "Hızlı / Düşük Gecikme (800ms)",
        subtitle = "Kanal geçişleri anında olur, hızlı internet için idealdir",
        bufferMs = 800
    ),
    BALANCED(
        key = "BALANCED",
        title = "Dengeli / Standart (2.5s)",
        subtitle = "Tüm bağlantı türleri için önerilen donma karşıtı profil",
        bufferMs = 2500
    ),
    HIGH_STABILITY(
        key = "HIGH_STABILITY",
        title = "Yüksek Kararlılık / Araç (8s)",
        subtitle = "Araçta hareket halindeyken veya zayıf 4G/3G çekiminde kesintiyi önler",
        bufferMs = 8000
    );

    companion object {
        fun fromKey(key: String): BufferProfile = values().find { it.key == key } ?: BALANCED
    }
}

/**
 * Varsayılan Oynatıcı Motoru
 */
enum class PlayerEngineType(
    val key: String,
    val title: String,
    val subtitle: String
) {
    MEDIA3(
        key = "MEDIA3",
        title = "Media3 ExoPlayer (Önerilen)",
        subtitle = "Ultra hızlı, Android TV ve araç uyumlu resmi Google motoru"
    ),
    VLC(
        key = "VLC",
        title = "VLC Engine (Yedek)",
        subtitle = "Eski veya nadir codec'li yayınlar için güçlü LibVLC motoru"
    ),
    IJK(
        key = "IJK",
        title = "IJK Player (Eski Donanım)",
        subtitle = "Eski TV kutuları ve düşük donanımlı cihazlar için FFmpeg tabanlı"
    );

    companion object {
        fun fromKey(key: String): PlayerEngineType = values().find { it.key == key } ?: MEDIA3
    }
}

/**
 * Uygulama Başlangıç Sekmesi
 */
enum class StartupTabOption(
    val key: String,
    val title: String,
    val tabIndex: Int
) {
    LIVE_TR("LIVE_TR", "Canlı TV (Yerli)", 0),
    FAVORITES("FAVORITES", "Favorilerim", 1),
    WORLD("WORLD", "Dünya Kanalları", 2),
    CAR_MODE("CAR_MODE", "Araç Sürüş Modu", 4);

    companion object {
        fun fromKey(key: String): StartupTabOption = values().find { it.key == key } ?: LIVE_TR
    }
}
