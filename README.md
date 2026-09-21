# PinpirikTV - Modern IPTV & Canlı Yayın Oynatıcı

**PinpirikTV**, Android Mobil ve Android TV (Leanback 10-foot) cihazlar için geliştirilmiş, yüksek performanslı, akılcı ve kullanıcı dostu bir canlı IPTV oynatıcı uygulamasıdır. Maskotunda sevimli bir ginepig yer alır.

---

## 🌟 Öne Çıkan Özellikler

- **ExoPlayer (Media3) Oynatma Motoru:** HLS (.m3u8), TS, MP4 canlı yayın akışları için donanım hızlandırmalı, ultra düşük gecikmeli oynatma.
- **M3U & TXT Çalma Listesi Desteği:** Çoklu kaynak desteği. Standart `#EXTINF` M3U etiketlerinin yanı sıra metin tabanlı (`Kanal,URL`) çalma listelerini de otomatik olarak ayrıştırır.
- **Çoklu Hazır Kaynaklar & Özel Playlist Ekleme:**
  - *vbskycn/iptv* (IPv4 M3U, IPv4 TXT, IPv6 M3U, GitHub Raw)
  - *iptv-org* (Türkiye, Dünya geneli, Avrupa)
  - *Ömer Denizhan & itasli* (Türk ulusal FTA seçkileri)
  - Kullanıcı tanımlı özel M3U/M3U8 URL girişi ve Xtream Codes API desteği.
- **Akılcı & Ferah Arayüz (Material 3 Dark Theme):**
  - Ekranı kaplamayan, kompakt ve ferah kanal ızgarası.
  - Canlı yayın kategori filtreleme (Tümü, Ulusal, Spor, Haber, Müzik, Belgesel, Çocuk, Dünya).
  - Anlık arama (Fuzzy arama motoru).
  - Sinema/VOD sekmesi kaldırılmış, sade ve amaca yönelik yapı.
- **Android TV & Kumanda Uyumluluğu:**
  - 10-foot UI düzeni ve tam D-Pad yön tuşları odaklanma (Focus border + hafif ölçeklenme).
  - Kumanda `[OK]` tuşu ile anında tam ekran oynatıcıya geçiş.
  - Kumanda yön tuşları (`YUKARI` / `AŞAĞI`) ile anında hızlı kanal değiştirme (Zapping).
- **Akıllı Hata Yönetimi & Yedek Akış:**
  - Yayın kesildiğinde veya çevrimdışı olduğunda **"Kanal şu an çalışmıyor"** uyarısı.
  - Yedek yayın aynalarına (stream mirrors) anında geçiş.
  - Yayının devam etmemesi durumunda otomatik bir sonraki kanala atlama opsiyonu.
- **Favoriler:** Room SQLite veritabanı ile kalıcı, hızlı favori kanal yönetimi.
- **Ağ & İzinler:**
  - İnternet erişim izinleri (`android.permission.INTERNET`, `ACCESS_NETWORK_STATE`).
  - Eski ve HTTP tabanlı yayınlar için `android:usesCleartextTraffic="true"` desteği.

---

## 🚀 Derleme ve Çalıştırma

### Gereksinimler
- Android SDK 34 (compileSdk: 34, minSdk: 24)
- Java 17 / JDK 17
- Gradle (Kotlin DSL)

### Debug APK Oluşturma
Terminal veya komut satırında şu komutu çalıştırın:
```bash
gradle assembleDebug
```
Üretilen APK dosyası şu konumda yer alır:
`app/build/outputs/apk/debug/app-debug.apk`

---

## ⚠️ Yasal Uyarı & Sorumluluk Reddi (Disclaimer)

PinpirikTV bir medya oynatıcı arayüzüdür. Uygulama kendi sunucularında herhangi bir video veya televizyon yayını barındırmaz. Uygulamada yer alan varsayılan linkler kamuya açık, yasal ve internet ortamında ücretsiz olarak dağıtılan (Free-to-Air) halka açık kaynaklardan derlenmiştir. Kullanıcıların telif hakkıyla korunan yayınları izlemek için geçerli lisanslara sahip olmaları kendi sorumluluklarındadır.
