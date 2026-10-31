# Klip

Prototipe aplikasi pengunduh video Android 10+ yang berjalan di perangkat. Menggunakan yt-dlp dan FFmpeg melalui youtubedl-android. Tersedia pemeriksaan informasi video, pilihan kualitas, audio MP3 atau asli, pembatalan, dan pilihan pembaruan mesin stable atau nightly. Browser opsional dapat mendeteksi beberapa URL media langsung atau streaming saat video diputar. Ketika media yang terdeteksi dipilih, mesin menggunakan referer, user agent, dan cookie yang cocok dengan host media tersebut. Cookie disimpan sementara di penyimpanan pribadi aplikasi dan dihapus ketika proses unduh selesai atau dibatalkan. Deteksi ini tidak mencakup semua situs. Proyek ini belum diverifikasi pada seluruh versi Android atau seluruh situs. Unduh hanya konten yang boleh Anda unduh.

## Membuat APK
Di GitHub buka tab Actions, pilih **Build Klip APK**, lalu pilih build pada branch `avapage-downloader`. Jika selesai, unduh artifact **Klip-debug**, ekstrak ZIP, dan instal `app-debug.apk`. GitHub mungkin meminta login untuk mengunduh artifact. Debug APK hanya untuk penggunaan pribadi dan belum ditandatangani dengan kunci rilis.

Dengan Android Studio dan SDK 35, buka folder ini dan jalankan `gradle :app:assembleDebug` (Gradle 8.10.2 dan JDK 17). Gunakan tombol Update untuk memperbarui yt-dlp. Simpan aplikasi terbuka selama mengunduh. File berada di Downloads/Klip.

Menggunakan [youtubedl-android](https://github.com/yausername/youtubedl-android), [yt-dlp](https://github.com/yt-dlp/yt-dlp), dan FFmpeg. Komponen tunduk pada lisensi masing-masing. Tidak ada jaminan semua situs, video yang memerlukan login, atau konten DRM didukung.
