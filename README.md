<div align="center">

# 🚀 NUX Launcher Android

### *An Unofficial, Modern & Feature-Rich Minecraft Java Edition Launcher for Android*

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Android Version](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Upstream: ZalithLauncher2](https://img.shields.io/badge/Forked%20From-ZalithLauncher2-purple.svg)](https://github.com/ZalithLauncher/ZalithLauncher2)

> **⚠️ PEMBERITAHUAN RESMI / IMPORTANT NOTICE:**  
> **NUX Launcher Android adalah proyek independen dan TIDAK RESMI (UNOFFICIAL FORK).**  
> Proyek ini dibangun dengan mengadaptasi dan memodifikasi basis kode terbuka dari **[ZalithLauncher2](https://github.com/ZalithLauncher/ZalithLauncher2)** dan **[PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher)** di bawah lisensi **GNU General Public License v3.0 (GPL-3.0)**.  
> Proyek ini **TIDAK berafiliasi, didukung, disponsori, atau disetujui** oleh Tim Pengembang Zalith (Movtery & tim), Tim PojavLauncher, Mojang Studios, maupun Microsoft Corporation.

</div>

---

## 📋 Daftar Isi
- [📜 Kepatuhan Lisensi Open Source (GPL-3.0 Compliance)](#-kepatuhan-lisensi-open-source-gpl-30-compliance)
- [💖 Ucapan Terima Kasih & Kredit Hulu (Upstream Credits)](#-ucapan-terima-kasih--kredit-hulu-upstream-credits)
- [✨ Fitur Utama NUX Launcher Android](#-fitur-utama-nux-launcher-android)
- [🛠️ Arsitektur & Teknologi](#️-arsitektur--teknologi)
- [📂 Struktur Proyek](#-struktur-proyek)
- [🚀 Panduan Kompilasi & Build Sendiri](#-panduan-kompilasi--build-sendiri)
- [🔒 Keamanan & Perlindungan Privasi](#-keamanan--perlindungan-privasi)
- [⚠️ Penafian Hukum & Merek Dagang (Legal Disclaimer)](#️-penafian-hukum--merek-dagang-legal-disclaimer)

---

## 📜 Kepatuhan Lisensi Open Source (GPL-3.0 Compliance)

Sebagai turunan dari proyek berlisensi **GNU General Public License v3.0 (GPL-3.0)**:
1. **100% Sumber Kode Terbuka**: Seluruh kode sumber dari aplikasi client NUX Launcher Android ini dipublikasikan secara utuh dan transparan di repositori publik ini tanpa ada komponen biner/eksekusi inti yang disembunyikan.
2. **Kebebasan Pengguna (Free Software Freedom)**: Siapa pun bebas menginspeksi, memodifikasi, mengompilasi, dan mendistribusikan ulang kode ini dengan mematuhi ketentuan lisensi GNU GPL v3.
3. **Inti Peluncur & Game Tetap Gratis**: Fungsi inti peluncur game—termasuk pengunduhan Minecraft Java Edition resmi dari Mojang, manajemen modloader (Fabric, Forge, NeoForge, Quilt), instalasi mod, dan eksekusi runtime Java di Android—bersifat 100% gratis, bebas, dan mandiri.
4. **Fitur Ekstra / Layanan Cloud**: Fitur premium opsional (seperti skin custom resolver, integrasi cloud backup, dan styling kosmetik profil) hanyalah layanan bernilai tambah independen yang tidak membatasi atau mengunci kebebasan fungsionalitas inti GPL game launcher.

---

## 💖 Ucapan Terima Kasih & Kredit Hulu (Upstream Credits)

Kami menyampaikan rasa hormat dan terima kasih sebesar-besarnya kepada para pengembang pionir di komunitas open source Minecraft Android:

- **[ZalithLauncher2](https://github.com/ZalithLauncher/ZalithLauncher2)** — Dibuat oleh **Movtery** dan para kontributor hebat ZalithLauncher. Merupakan basis arsitektur dan sistem runtime Android modern berlisensi **GNU GPL-3.0**.
- **[PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher)** — Dibuat oleh **artdeell**, **khanhtran**, dan **PojavLauncherTeam**. Proyek pionir yang memungkinkan eksekusi Minecraft Java Edition di Android menggunakan LWJGL, GLFW, dan Android NDK bridge berlisensi **GNU GPL-3.0**.
- **[MobileGlues](https://github.com/MobileGlues/MobileGlues)** — Pustaka penerjemah grafis modern (OpenGL ES ke DirectGLES / OpenGL 4.x) untuk eksekusi Minecraft versi modern di Android berlisensi **GNU LGPL-2.1**.
- **[GL4ES](https://github.com/ptitSeb/gl4es)** — Dibuat oleh **ptitSeb**. Pustaka penerjemah OpenGL ke OpenGL ES berlisensi **MIT**.
- **[Fold Craft Launcher (FCL)](https://github.com/FCL-Team/FoldCraftLauncher)** — Tim FCL atas inspirasi integrasi modul dan utilitas launcher Minecraft Android berlisensi **GNU GPL-3.0**.
- **[Ely.by](https://ely.by)** — Atas infrastruktur sistem skin dan autentikasi alternatif komunitas yang stabil.
- **[Modrinth](https://modrinth.com)** — Atas API publik yang cepat dan bersih untuk penelusuran Mod, Resource Pack, dan Shader komunitas.
- **[LiveKit](https://livekit.io)** — Infrastruktur WebRTC real-time untuk fitur voice room.

---

## ✨ Fitur Utama NUX Launcher Android

### 🎮 Antarmuka Pengguna Modern (Next-Gen UI)
- **100% Jetpack Compose & Material 3**: Tampilan modern, bersih, dan bertema gelap (*Dark Tech Gaming Aesthetics*) dengan transisi halus dan animasi berkinerja tinggi.
- **Label Unofficial Transparan**: Dilengkapi dengan tanda pengenal transparan dan layar *About & Lisensi* lengkap demi menjunjung etika dan transparansi kepada komunitas open source.
- **Responsif**: Dioptimalkan untuk ponsel pintar, perangkat layar lipat (*foldable*), tablet, dan emulator Android.

### ⚡ Mesin Eksekusi & Graphics Renderer Canggih
- **Multi-Java Runtime**: Dukungan Java Runtime Environment terisolasi (**Java 8, Java 17, hingga Java 21**) untuk kompatibilitas versi Minecraft lama (1.7 - 1.16) maupun rilis modern (1.17 - 1.21+).
- **Pilihan Graphics Backend Lengkap**:
  - **MobileGlues (DirectGLES / OpenGL 4.0 - 4.6)**: Pilihan utama untuk Minecraft modern dengan performa tinggi.
  - **Holy GL4ES**: Sangat stabil untuk versi lama dan perangkat dengan kompatibilitas GLES 2/3 terbatas.
  - **VirGL / Zink / ANGLE**: Rendering berbasis Vulkan dan Gallium untuk GPU Adreno (Turnip driver) dan Mali.
- **Kustomisasi Runtime**: Alokasi RAM fleksibel, flag optimasi JVM kustom, batas FPS, dan skala resolusi render layar dinamis.

### 📦 Manajemen Versi, Mod & Instance
- **Pemasang Modloader 1-Klik**: Instalasi instan dan otomatis untuk **Fabric**, **Forge**, **NeoForge**, dan **Quilt**.
- **Integrasi API Modrinth**: Cari, unduh, dan pasang ribuan Mod, Modpack, Resource Pack, serta Shaderpack langsung dari dalam aplikasi tanpa perlu browser eksternal.
- **Multi-Instance Terisolasi**: Buat profil permainan terpisah dengan versi dan set mod yang berbeda tanpa tumpang tindih.

### 🕹️ Kontrol Sentuh Kustom & Gamepad
- **Visual Control Editor**: Atur posisi, ukuran, transparansi, warna, dan pemetaan tombol virtual di layar dengan presisi tinggi.
- **Emulasi Mouse & Giroskop**: Kontrol kursor dan pembidikan menggunakan drag sentuh atau sensor giroskop perangkat.
- **Dukungan Controller / Gamepad Fisik**: Kompatibel dengan controller Bluetooth dan USB OTG (Xbox, PlayStation DualShock/DualSense, dan controller generic).

### 👥 Fitur Komunitas & Autentikasi
- **Dukungan Akun Resmi Microsoft**: Login aman menggunakan OAuth Microsoft resmi.
- **Dukungan Akun Ely.by & Akun Offline**: Login skin alternatif atau pengujian lokal tanpa internet.
- **Status Bermain & Komunitas**: Indikator status real-time untuk bermain bersama teman.

---

## 🛠️ Arsitektur & Teknologi

| Lapisan | Komponen & Teknologi |
|---|---|
| **Bahasa Pemrograman** | Kotlin 2.0+, Java (OpenJDK 8/17/21) |
| **User Interface** | Jetpack Compose, Material 3, Compose Navigation, Coroutine Flows |
| **Mesin Render & Native** | Android NDK (C/C++), JNI Bridge, MobileGlues, GL4ES, LWJGL Mobile |
| **Jaringan & Unduhan** | OkHttp 4, Gson, Custom Dns Resolver (`NuxDns`) |
| **Autentikasi Akun** | Microsoft OAuth 2.0 PKCE, Ely.by Authlib, Firebase REST Client |
| **Voice Streaming** | LiveKit WebRTC via Android WebView Secure Asset Bridge |

---

## 📂 Struktur Proyek

```text
Launcher-Android-Final/
├── app/
│   ├── src/main/
│   │   ├── java/com/israadev/nuxlauncher/
│   │   │   ├── core/
│   │   │   │   ├── account/      # Manajemen akun (Microsoft, Ely.by, Offline)
│   │   │   │   ├── auth/         # Autentikasi sesi & validasi lisensi
│   │   │   │   ├── controls/     # Tata letak & pemrosesan input sentuh/gamepad
│   │   │   │   ├── download/     # Engine pengunduh aset Minecraft & pustaka JVM
│   │   │   │   ├── game/         # Pengelola siklus hidup proses game Minecraft
│   │   │   │   ├── instance/     # Manajemen profil instance & direktori game
│   │   │   │   ├── launch/       # Penyusun argumen baris perintah peluncuran JVM
│   │   │   │   ├── mods/         # Integrasi API Modrinth & pengelola modpack
│   │   │   │   ├── network/      # NuxConfig, DNS kustom, dan utilitas HTTP
│   │   │   │   ├── renderer/     # Konfigurasi backend grafis (MobileGlues, GL4ES, Zink)
│   │   │   │   ├── runtime/      # Manajemen paket Java Runtime (OpenJDK 8/17/21)
│   │   │   │   ├── social/       # Chat real-time, status teman & voice room
│   │   │   │   └── update/       # Pengecekan pembaruan APK otomatis
│   │   │   └── ui/
│   │   │       ├── components/   # Komponen atomik Jetpack Compose
│   │   │       ├── dialogs/      # Dialog kustom (Add Instance, About, Renderer Config)
│   │   │       ├── screens/      # Layar utama (Dashboard, Akun, Mods, Pengaturan, dsb.)
│   │   │       └── theme/        # Skema tema warna Material 3 & tipografi
│   │   ├── assets/               # Aset statis & jembatan LiveKit WebRTC
│   │   └── AndroidManifest.xml   # Konfigurasi permission & aktivitas Android
│   └── build.gradle.kts          # Konfigurasi Gradle modul app
├── local.properties.example      # Template konfigurasi rahasia untuk developer
├── LICENSE                       # Salinan teks lengkap lisensi GNU GPL-3.0
└── README.md                     # Dokumentasi resmi proyek
```

---

## 🚀 Panduan Kompilasi & Build Sendiri

### Prasyarat Pengembangan
1. **Android Studio**: Android Studio Ladybug (2024.2.1) atau versi lebih baru disarankan.
2. **Java Development Kit (JDK)**: JDK 17 (disarankan Eclipse Temurin atau OpenJDK bawaan Android Studio).
3. **Android SDK**:
   - Compile SDK: `37`
   - Target SDK: `34`
   - Min SDK: `26` (Android 8.0 Oreo ke atas)
4. **Android NDK**: NDK r25+ untuk mengompilasi jembatan native C/C++.

### Langkah-Langkah Kompilasi:

1. **Clone Repositori**:
   ```bash
   git clone https://github.com/IsraaDeveloper/nuxlabs.git
   cd nuxlabs
   ```

2. **Siapkan `local.properties`**:
   Salin file `local.properties.example` menjadi `local.properties`:
   ```bash
   cp local.properties.example local.properties
   ```
   Buka file `local.properties` dan tentukan lokasi direktori Android SDK Anda:
   ```properties
   sdk.dir=C\:\\Users\\NamaUser\\AppData\\Local\\Android\\Sdk

   # Opsional: Tentukan server kustom Anda jika menggunakan server sendiri
   # nux.server.url=https://domain-server-anda.com
   ```

3. **Kompilasi APK (Debug Build)**:
   - **Windows (Command Prompt / PowerShell)**:
     ```cmd
     .\gradlew.bat assembleDebug
     ```
   - **Linux / macOS**:
     ```bash
     chmod +x gradlew
     ./gradlew assembleDebug
     ```

4. **Hasil Kompilasi File APK**:
   File APK hasil kompilasi akan berada di:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🔒 Keamanan & Perlindungan Privasi

- **Pemisahan Kredensial (Zero Leaks)**: Seluruh rahasia, signature key, dan variabel environment terisolasi di luar git history dan diamankan oleh file `.gitignore`.
- **Koneksi Terenkripsi**: Semua transmisi data akun, mod, dan unduhan aset game dienkripsi menggunakan protokol HTTPS/TLS modern.
- **Kepatuhan Privasi Data**: NUX Launcher tidak pernah mencatat atau menyimpan kata sandi akun Microsoft Anda; proses autentikasi ditangani langsung melalui jendela login resmi Microsoft OAuth.

---

## ⚠️ Penafian Hukum & Merek Dagang (Legal Disclaimer)

- **BUKAN PRODUK RESMI MINECRAFT.**
- **NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.**
- *Minecraft* adalah merek dagang terdaftar milik **Mojang AB / Microsoft Corporation**.
- Aplikasi ini adalah perangkat lunak pihak ketiga independen. Seluruh aset, file pustaka Java, dan client game Minecraft yang diunduh melalui aplikasi ini diambil langsung dari server distribusi resmi Mojang sesuai dengan lisensi kepemilikan akun pengguna.
- Proyek ini dirilis dan dilindungi secara legal di bawah ketentuan **GNU General Public License v3.0 (GPL-3.0)**.
