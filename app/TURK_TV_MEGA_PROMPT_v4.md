# 📺 PinpirikTV (Türk TV) — Android TV Mimari ve Gereksinim Spesifikasyonu (Mega Prompt v4.0)

Bu doküman, Android TV, Google TV ve mobil Android cihazlar için geliştirilen **PinpirikTV / Türk TV** uygulamasının nihai üretim seviyesi (production-grade) kodlama, veri kaynakları, oynatıcı parametreleri ve mimari gereksinimlerini tek parça halinde belgeler.

---

## 0. TEMEL İLKELER & UYDU ALICISI DENEYİMİ (NON-NEGOTIABLE)

1. **Hızlı Başlatma (< 1.0s):** Canlı yayınlar ultra düşük gecikmeli buffer ve `minBufferMs = 500ms`, `bufferForPlaybackMs = 500ms` parametreleriyle anında başlar.
2. **Kusursuz TV Kumandası (%100 DPAD):** 
   - `DPAD_UP / CH_UP` & `DPAD_DOWN / CH_DOWN`: Önceki/Sonraki kanal zapping.
   - `DPAD_LEFT`: Kalite sekmesi / Hızlı kanal rehberi.
   - `DPAD_RIGHT`: EPG Rehberi.
   - `0..9` Sayısal Tuşlar: Digiturk / Uydu alıcısı tarzı doğrudan kanal numarası ile anında atlama.
   - `DPAD_CENTER / ENTER / OK`: Oynat / Duraklat / Kanal Rehberi (Zap Drawer).
3. **Sıfır Sahte Veri:** Tüm M3U listeleri, HLS `.m3u8` akışları ve EPG XMLTV kaynakları gerçek, kamuya açık ve yasal canlı yayın CDN'lerinden beslenir.
4. **Self-Healing (Otomatik Onarım):** Birincil CDN adresi yanıt vermediğinde veya hata kodu döndüğünde oynatıcı otomatik olarak yedek akışa geçer ve arka planda alternatif güncel kaynağı devreye sokar.
5. **Hata Toleransı (Decoder Fallback):** Emülatör veya düşük bellekli TV Box cihazlarında donanım çözücü kaynak sorunu (`C2_NO_MEMORY`) yaşanmaması için `setEnableDecoderFallback(true)` aktiftir.

---

## 1. MİMARİ VE VERİ AKIŞI

```
Ağ / M3U / XMLTV / Xtream
       │
       ▼
OkHttpClient (Timeout: 5s, Cache-Control, User-Agent: Mozilla/5.0 PinpirikTV/1.0)
       │
       ▼
M3UParser (Grup, tvg-id, logo, çözünürlük etiketleri ve akıllı kategori tespiti)
       │
       ▼
Room Database (ChannelEntity, EPGProgramEntity, PlaylistEntity)
       │
       ▼
ChannelRepository (Öncelikli Türk Kanalları Sıralaması + Dünya Filtresi)
       │
       ▼
MainViewModel (StateFlow, Canlı Rehber, Kalıcı Dünya / Ülke Seçimi)
       │
       ▼
Jetpack Compose TV / Mobile UI & Media3 ExoPlayer Engine
```

---

## 2. SABİT VE KAMUYA AÇIK ÖNCELİKLİ KAYNAKLAR (PlaylistSources)

- **Free-TV Türkiye Canlı Repo:** `https://raw.githubusercontent.com/Free-TV/IPTV/master/playlists/playlist_turkey.m3u8`
- **iptv-org Türkiye:** `https://iptv-org.github.io/iptv/countries/tr.m3u`
- **Romaxa55 Stabil Dünya:** `https://romaxa55.github.io/world_ip_tv/output/index.m3u`
- **iptv-org Azerbaycan:** `https://iptv-org.github.io/iptv/countries/az.m3u`
- **iptv-org Spor & Haber:** `https://iptv-org.github.io/iptv/categories/sports.m3u`
- **XMLTV EPG Kaynağı:** `https://iptv-org.github.io/epg/guides/tr.xml`

---

## 3. TÜRKİYE KANALLARI RESMİ SIRALAMASI (Uydu Sırası)

1. **TRT 1 HD** (Varsayılan Açılış Kanalı)
2. **TRT 2 HD** (Kültür & Sanat)
3. **ATV HD**
4. **Kanal D HD**
5. **Show TV HD**
6. **Star TV HD**
7. **Halk TV HD**
8. **Sözcü TV HD**
9. **TRT Spor HD**
10. **HT Spor HD**
11. **A Spor HD**
12. **TRT Spor Yıldız HD**
13. **TV8 HD**
14. **A2 HD**
15. **TV 100 HD**
16. **Beyaz TV HD**
17. **Ekol TV HD**
18. **TRT Haber HD**
19. **NTV HD**
20. **Habertürk HD**
21. **Haber Global HD**
22. **A Haber HD**
23. **TRT Çocuk HD**
24. **TRT Müzik HD**
25. **TRT Belgesel HD**

---

## 4. MEDYA MOTORU (ExoPlayer Media3) OPTİMİZASYONU

```kotlin
val loadControl = DefaultLoadControl.Builder()
    .setBufferDurationsMs(
        500,   // minBufferMs (Ultra hızlı başlatma)
        3000,  // maxBufferMs (TV için optimize bellek tüketimi)
        500,   // bufferForPlaybackMs (Anında ilk kare)
        1000   // bufferForPlaybackAfterRebufferMs
    )
    .setPrioritizeTimeOverSizeThresholds(true)
    .build()

val renderersFactory = DefaultRenderersFactory(context).apply {
    setEnableDecoderFallback(true) // Donanım yetersizse yazılıma otomatik düşüş
    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
    setAllowedVideoJoiningTimeMs(4000)
}
```

---

## 5. YASAL BİLGİLENDİRME (LEGAL NOTICE)

Bu uygulama yalnızca kamuya açık ve ücretsiz erişilebilir yayın kaynaklarına yönlendirmektedir.
Herhangi bir içerik barındırılmamakta veya dağıtılmamaktadır.
Kaynaklar: iptv-org/iptv, Free-TV, kamu yayıncıları resmi CDN adresleri.
