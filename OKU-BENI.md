# Yırdaş (Android)

Telefondaki müzikleri ve klipleri **kendiliğinden bulan**, **uygulama kapalıyken de çalmaya devam eden**,
Spotify tarzı arayüzlü müzik çalar.

## Özellikler
- İlk açılışta izin ister, ardından telefondaki tüm müzikleri ve klipleri otomatik tarar. Yeni dosya eklenince liste kendiliğinden güncellenir.
- Arka planda çalma: bildirim ve kilit ekranı kontrolleriyle (Media3 MediaSession servisi). Uygulamayı kapatsan da çalar.
- Klipler tam ekran "Şimdi çalıyor" sayfasında görüntüyle oynar. Sadece ses olan parçalarda kapak görünür.
- Şarkı sözleri: İnternet varken lrclib.net'ten bulunur, telefona kaydedilir. Zamanlı sözler şarkıyla akar, bir satıra dokununca oraya atlar.
- En çok dinlediklerin: Bir parça yaklaşık 30 saniye dinlenince sayılır. Ana sayfada ve "En çok" sekmesinde görünür.
- Öneriler: İnternet varken en çok dinlediğin sanatçılara göre benzer sanatçılar (Deezer) "Tavsiye Edilen İstasyonlar" olarak çıkar. Dokununca 30 saniyelik önizlemeler çalar.

- **Yapay zekâ asistanı (Asistan sekmesi):** Ruh halini yaz ya da mikrofon simgesiyle söyle ("ruh halim kötü, solgun şarkılar aç", "iyileştirecek oyun havaları aç"). Asistan telefondaki listene ve en çok dinlediklerine bakıp şarkıları seçer, çalar; istersen listende olmayan yeni sanatçılardan öneri de katar. Asistan Google Gemini'nin ÜCRETSİZ katmanıyla çalışır (kart gerekmez): aistudio.google.com/apikey adresinden Google hesabınla anahtar al, Asistan sekmesindeki alana yapıştır. Anahtar yalnızca telefonda saklanır. Ücretsiz katmanın günlük sınırı vardır; sınıra takılırsan sonra tekrar dene.
- **Listelerin arasında öneriler:** Ana sayfada ve Kitaplık'ta, sıraya ve çalma listesine her 4-5 parçada bir "ÖNERİ" etiketli parça karışır (30 sn önizleme). Yanındaki kalbe basınca parça **Beğenilen** sekmesine kaydedilir. Oradan paylaş simgesiyle listeyi (sanatçı - şarkı + Deezer bağlantısı) alıp sonra indirebilirsin.

## APK nasıl alınır

### Yol A: GitHub ile (bilgisayara bir şey kurmadan)
1. github.com'da ücretsiz hesap aç, yeni bir **boş depo** oluştur.
2. Bu klasörün içindekileri (CepCalar klasörünün içi, `.github` klasörü dahil) depoya yükle.
3. Depoda **Actions** sekmesi > **APK derle** > **Run workflow**.
4. İş bitince (yaklaşık 5-8 dk) sayfanın altındaki **cep-calar-apk** dosyasını indir, zip'ten `app-debug.apk` çıkar.
5. APK'yı telefona gönder, aç. "Bilinmeyen kaynaklardan yükle" izni isterse ver.

### Yol B: Android Studio ile
1. Android Studio'yu kur, **Open** ile bu klasörü aç, Gradle eşitlemesini bekle.
2. **Build > Build APK(s)** ile APK'yı al ya da telefonu USB ile bağlayıp **Run**'a bas.

## Notlar
- Kod henüz gerçek bir derlemeyle denenmedi. Derleme hata verirse hata metnini gönder, düzeltirim.
- Telefon üreticilerinin pil tasarrufu ayarları arka plandaki çalmayı kesebilir. Kesilirse Ayarlar > Pil > Yırdaş için "Kısıtlama yok" seç.
- Telefonda sanatçı bilgisi olmayan dosyalarda, ad "Sanatçı - Şarkı" biçimindeyse sanatçı dosya adından okunur.
- Klipler: Sanatçı bilgisi olan veya Müzik, Klip gibi klasörlerdeki videolar listelenir. Kamera, WhatsApp vb. videolar gizlidir. Kitaplık sekmesindeki anahtarla tüm videoları gösterebilirsin.
