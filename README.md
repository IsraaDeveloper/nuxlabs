<div align="center">

# 🚀 NUX Launcher Android

**Launcher Minecraft Java Edition modern, cepat, dan kaya fitur untuk perangkat Android.**  
Dibangun dari awal menggunakan teknologi modern Android (**Jetpack Compose**), sistem kontrol sentuh yang fleksibel, manajemen mod terintegrasi, dan fitur sosial real-time lintas platform.

[![Android Version](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com)

</div>

---

## 📖 Daftar Isi
- [✨ Fitur Utama](#-fitur-utama)
- [🛠️ Arsitektur & Tech Stack](#️-arsitektur--tech-stack)
- [📂 Struktur Direktori Proyek](#-struktur-direktori-proyek)
- [🚀 Panduan Kompilasi & Build](#-panduan-kompilasi--build)
  - [Prasyarat](#prasyarat)
  - [Langkah Build](#langkah-build)
- [🔒 Keamanan & Isolasi Endpoint Server](#-keamanan--isolasi-endpoint-server)
- [📜 Lisensi & Atribusi (Credits)](#-lisensi--atribusi-credits)
- [⚠️ Penafian Hukum (Disclaimer)](#️-penafian-hukum-disclaimer)

---

## ✨ Fitur Utama

### 🎮 Antarmuka Pengguna Modern (Next-Gen UI)
- **100% Jetpack Compose & Material 3**: Tampilan modern bertema gelap (*Dark Tech Gaming Aesthetics*) dengan transisi halus dan animasi performa tinggi.
- **Responsif**: Mendukung berbagai ukuran layar, mulai dari smartphone, foldable, tablet, hingga emulator Android.

### ⚡ Mesin Eksekusi & Runtime Fleksibel
- **Multi-Java Runtime**: Dukungan Java Runtime Environment terisolasi (Java 8, 17, hingga Java 21) untuk kompatibilitas versi Minecraft lama maupun terbaru.
- **Pilihan Graphics Renderer**: Dukungan renderer canggih seperti **Holy GL4ES**, **VirGL**, **Zink**, dan **ANGLE** untuk memaksimalkan FPS di berbagai GPU (Adreno, Mali, PowerVR).
- **Kustomisasi Argument JVM**: Pengaturan alokasi memori RAM fleksibel, custom JVM flags, dan resolusi render layar.

### 📦 Manajemen Versi, Mod & Instance
- **Pemasang Modloader 1-Klik**: Instalasi instan untuk **Fabric**, **Forge**, **Quilt**, dan **NeoForge**.
- **Integrasi Browser Modrinth**: Cari, unduh, dan pasang Mod, Modpack, Resource Pack, dan Shaderpack langsung dari dalam aplikasi tanpa perlu browser luar.
- **Isolasi Instance**: Buat banyak profil permainan terpisah dengan versi dan set mod yang berbeda tanpa tumpang tindih.

### 👥 Fitur Sosial & Komunitas Real-Time
- **Cross-Platform Presence**: Sinkronisasi status bermain real-time (Online, Bermain Minecraft, Idle).
- **Teman & Chat Global**: Obrolan teks real-time dengan teman sesama pengguna NUX Launcher (baik versi Android maupun Desktop).
- **Voice Room (LiveKit WebRTC)**: Ruang obrolan suara langsung (*voice chat*) berlatensi rendah untuk mabar bersama teman.

### 🕹️ Kontrol Sentuh Kustom & Gamepad
- **Layout Editor Visual**: Sesuaikan posisi, ukuran, transparansi, dan binding tombol virtual di layar.
- **Mouse & Gyroscope Emulation**: Kontrol kursor menggunakan drag sentuh atau sensor giroskop perangkat.
- **Dukungan Hardware Gamepad / OTG Controller**: Dukungan kontroler fisik (Xbox, DualShock/DualSense, dan controller Bluetooth generic).

### 🔐 Manajemen Akun
- **Dukungan Microsoft Account**: Login resmi menggunakan OAuth Microsoft.
- **Akun Offline / Lokal**: Kemudahan bermain tanpa akun internet untuk pengujian lokal.

---

## 🛠️ Arsitektur & Tech Stack

| Komponen | Teknologi yang Digunakan |
|---|---|
| **Bahasa Utama** | Kotlin |
| **User Interface** | Jetpack Compose, Material 3, Compose Navigation |
| **Asynchronous & State** | Kotlin Coroutines, StateFlow, SharedFlow |
| **Networking & HTTP** | OkHttp 4, Gson, Custom DNS Resolver (`NuxDns`) |
| **Realtime Presence** | Firebase Realtime Database (REST Auth Context) |
| **Voice Streaming** | LiveKit WebRTC via Android WebView Secure Asset Bridge |
| **Graphics & Execution** | Android NDK, JNI Bridge, LWJGL Mobile, OpenJDK Mobile |

---

## 📂 Struktur Direktori Proyek

```text
Launcher-Android-Final/
├── app/
│   ├── src/main/
│   │   ├── java/com/israadev/nuxlauncher/
│   │   │   ├── core/
│   │   │   │   ├── account/      # Manajemen akun lokal & Microsoft
│   │   │   │   ├── auth/         # Autentikasi server & verifikasi
│   │   │   │   ├── controls/     # Tata letak & pemrosesan input tombol sentuh
│   │   │   │   ├── download/     # Engine unduhan aset Minecraft & pustaka
│   │   │   │   ├── game/         # Penanganan siklus hidup game Minecraft
│   │   │   │   ├── instance/     # Manajemen profil & folder instance
│   │   │   │   ├── launch/       # Persiapan argumen peluncuran JVM
│   │   │   │   ├── mods/         # Integrasi API Modrinth & pengelola mod
│   │   │   │   ├── network/      # NuxConfig, DNS custom, dan utilitas jaringan
│   │   │   │   ├── renderer/     # Konfigurasi backend grafis (GL4ES, VirGL, Zink)
│   │   │   │   ├── runtime/      # Manajemen runtime Java (OpenJDK 8/17/21)
│   │   │   │   ├── social/       # Chat real-time, status teman & voice room
│   │   │   │   └── update/       # Pengecekan pembaruan APK otomatis
│   │   │   └── ui/
│   │   │       ├── components/   # Komponen Compose atomik & reusable
│   │   │       ├── screens/      # Layar utama (Home, Auth, Mods, Settings, dsb.)
│   │   │       └── theme/        # Skema warna, tipografi, dan tema aplikasi
│   │   ├── assets/               # File aset statis & bridge WebRTC LiveKit
│   │   └── AndroidManifest.xml   # Konfigurasi permission & aktivitas Android
│   └── build.gradle.kts          # Konfigurasi build Gradle modul app
├── local.properties.example      # Template konfigurasi rahasia untuk publik
├── .gitignore                    # Pengaman file rahasia & artifact build
└── README.md                     # Dokumentasi resmi proyek
```

---

## 🚀 Panduan Kompilasi & Build

### Prasyarat
1. **Android Studio**: Android Studio Ladybug (2024.2.1) atau versi lebih baru disarankan.
2. **Java Development Kit (JDK)**: JDK 17 (disediakan oleh Android Studio / Temurin).
3. **Android SDK**:
   - Compile SDK: `37`
   - Target SDK: `34`
   - Min SDK: `26` (Android 8.0 Oreo)
4. **Android NDK**: NDK r25+ untuk kompilasi modul native.

### Langkah Build

1. **Clone Repositori**:
   ```bash
   git clone https://github.com/username/nux-launcher-android.git
   cd nux-launcher-android
   ```

2. **Siapkan `local.properties`**:
   Salin template `local.properties.example` menjadi `local.properties`:
   ```bash
   cp local.properties.example local.properties
   ```
   Buka file `local.properties` dan tentukan lokasi Android SDK di komputer Anda:
   ```properties
   sdk.dir=C\:\\Users\\NamaUser\\AppData\\Local\\Android\\Sdk

   # Opsional: Tentukan server kustom Anda jika menggunakan backend pribadi
   # nux.server.url=https://backend-anda.com
   ```
   > 💡 **Catatan**: Jika `nux.server.url` tidak diisi, aplikasi akan tetap berhasil dibuild dan berjalan dalam mode mandiri/offline.

3. **Kompilasi APK (Debug)**:
   - Di Windows (PowerShell/CMD):
     ```cmd
     .\gradlew.bat assembleDebug
     ```
   - Di Linux / macOS:
     ```bash
     chmod +x gradlew
     ./gradlew assembleDebug
     ```

4. **Lokasi File APK**:
   Setelah proses kompilasi sukses, file APK siap dipasang akan berada di:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🔒 Keamanan & Isolasi Endpoint Server

Proyek ini menggunakan arsitektur pemisahan kredensial yang ketat:
- **Zero Production Secrets in Repo**: URL server produksi resmi dan kredensial API sensitif diisolasi ke dalam `local.properties` (yang diabaikan oleh `.gitignore`).
- **Dynamic Build Injection**: Class [`NuxConfig`](file:///d:/Project/antigravity/NUX-LAUNCHER/Launcher-Android-Final/app/src/main/java/com/israadev/nuxlauncher/core/network/NuxConfig.kt) mengambil URL backend saat kompilasi via `BuildConfig.SERVER_BASE_URL`.
- Siapa pun yang melakukan *fork* atau *clone* repositori publik ini dapat mengompilasi aplikasi dengan lancar tanpa risiko mengakses atau membebani server produksi milik pengelola asli.

---

## 📜 Lisensi & Atribusi (Credits)

Proyek ini merupakan perangkat lunak bebas yang dirilis di bawah ketentuan **GNU General Public License v3.0 (GPL-3.0)**.  
Anda bebas menggunakan, memodifikasi, dan mendistribusikan kode ini dengan syarat tetap menyertakan lisensi yang sama dan membuka sumber kodenya.

### 💖 Ucapan Terima Kasih & Proyek Hulu (Upstream):
- **[PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher)**: Proyek pionir yang memungkinkan eksekusi Minecraft Java Edition di Android. Berisi jembatan LWJGL, GLFW, dan runtime OpenJDK Mobile berlisensi GPL-3.0.
- **[Zalith Launcher](https://github.com)**: Komponen optimasi runtime dan adaptasi lingkungan JVM di sistem operasi Android.
- **[Modrinth API](https://modrinth.com)**: Penyedia infrastruktur API terbuka untuk penelusuran dan pengunduhan mod komunitas.
- **[LiveKit](https://livekit.io)**: Komponen infrastruktur audio dan WebRTC real-time.
- **Tim FabricMC, Minecraft Forge, NeoForged, & QuiltMC**: Atas dedikasi pengembangan ekosistem modding Minecraft Java Edition.

---

## ⚠️ Penafian Hukum (Disclaimer)

- **BUKAN PRODUK RESMI MINECRAFT.**
- Aplikasi ini adalah perangkat lunak pihak ketiga independen dan **TIDAK DISETUJUI OLEH ATAU TERKAIT DENGAN MOJANG STUDIOS ATAU MICROSOFT**.
- *Minecraft* adalah merek dagang terdaftar milik Mojang AB / Microsoft Corporation.
- Seluruh aset, pustaka, dan file game Minecraft yang diunduh melalui launcher ini diunduh langsung dari server distribusi resmi Mojang sesuai dengan ketentuan kepemilikan akun pengguna.
