package com.israadev.nuxlauncher.ui.dialogs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.ui.components.NuxBadge
import com.israadev.nuxlauncher.ui.components.NuxButton
import com.israadev.nuxlauncher.ui.components.NuxCard
import com.israadev.nuxlauncher.ui.components.NuxDialog
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes

private data class OpenSourceLibrary(
    val name: String,
    val copyright: String,
    val license: String,
    val role: String,
    val url: String
)

private val LIBRARIES = listOf(
    OpenSourceLibrary(
        name = "Zalith Launcher 2",
        copyright = "Copyright © 2024-2026 MovTery & Contributors",
        license = "GPL-3.0 License",
        role = "Arsitektur runtime JVM, ZLBridge JNI, integrasi SDL3, direct gamepad, dan optimasi Android",
        url = "https://github.com/ZalithLauncher/ZalithLauncher2"
    ),
    OpenSourceLibrary(
        name = "PojavLauncher",
        copyright = "Copyright © 2020-present PojavLauncherTeam & Contributors",
        license = "GPL-3.0 License",
        role = "Mesin peluncuran Java Minecraft Android, LWJGL 3 Android Natives, GLFW Stub",
        url = "https://github.com/PojavLauncherTeam/PojavLauncher"
    ),
    OpenSourceLibrary(
        name = "MobileGlues",
        copyright = "Copyright © 2023-2026 MobileGlues Team",
        license = "LGPL-2.1 License",
        role = "Penerjemah modern OpenGL 4.0 ke OpenGL ES 3.2 untuk Minecraft 1.17+",
        url = "https://github.com/FCL-Team/MobileGlues"
    ),
    OpenSourceLibrary(
        name = "LWJGL - Lightweight Java Game Library",
        copyright = "Copyright © 2012-present LWJGL All rights reserved",
        license = "BSD 3-Clause License",
        role = "Pustaka binding grafis, audio (OpenAL), dan jendela game Java",
        url = "https://github.com/LWJGL/lwjgl3"
    ),
    OpenSourceLibrary(
        name = "SDL3 & sdl2-compat",
        copyright = "Copyright © 1997-2026 Sam Lantinga",
        license = "Zlib License",
        role = "Manajemen surface rendering native, touch input, dan audio output",
        url = "https://github.com/libsdl-org/SDL"
    ),
    OpenSourceLibrary(
        name = "Mesa 3D (Zink / Turnip / VirGL)",
        copyright = "Copyright © The Mesa Authors",
        license = "MIT License",
        role = "Vulkan-to-OpenGL translation & driver akselerasi GPU mobile",
        url = "https://mesa3d.org/"
    ),
    OpenSourceLibrary(
        name = "ANGLE",
        copyright = "Copyright © 2018 The ANGLE Project Authors",
        license = "BSD 3-Clause License",
        role = "Mesin penerjemah OpenGL ES berbasis backend Vulkan",
        url = "http://angleproject.org/"
    ),
    OpenSourceLibrary(
        name = "ByteHook",
        copyright = "Copyright © 2020-2024 ByteDance, Inc.",
        license = "MIT License",
        role = "Dynamic binary instrumentation dan hook Android library",
        url = "https://github.com/bytedance/bhook"
    ),
    OpenSourceLibrary(
        name = "Jetpack Compose & Material 3",
        copyright = "Copyright © The Android Open Source Project",
        license = "Apache 2.0",
        role = "Toolkit UI modern, deklaratif, dan responsif",
        url = "https://developer.android.com/jetpack/compose"
    ),
    OpenSourceLibrary(
        name = "LiveKit WebRTC",
        copyright = "Copyright © 2022-2026 LiveKit, Inc.",
        license = "Apache 2.0",
        role = "Infrastruktur audio real-time dan voice room mabar",
        url = "https://livekit.io"
    ),
    OpenSourceLibrary(
        name = "Modrinth API",
        copyright = "Copyright © Rinth, Inc. & Community",
        license = "Open API / AGPL",
        role = "Katalog publik mod, modpack, shaderpack, dan resource pack",
        url = "https://modrinth.com"
    ),
    OpenSourceLibrary(
        name = "sora-editor",
        copyright = "Copyright (C) 2020-2026 Rosemoe",
        license = "LGPL-2.1 License",
        role = "Editor teks dan log penampil performa tinggi",
        url = "https://github.com/Rosemoe/sora-editor"
    ),
    OpenSourceLibrary(
        name = "skinview3d",
        copyright = "Copyright © Kent Rasmussen & contributors",
        license = "MIT License",
        role = "Viewer interaktif 3D skin & jubah Minecraft berbasis WebGL",
        url = "https://github.com/bs-community/skinview3d"
    ),
    OpenSourceLibrary(
        name = "OkHttp & Okio",
        copyright = "Copyright © Square, Inc.",
        license = "Apache 2.0",
        role = "Klien HTTP jaringan performa tinggi",
        url = "https://github.com/square/okhttp"
    )
)

/**
 * Dialog Komprehensif Tentang Aplikasi & Atribusi Lisensi Open Source (GPL-3.0 & Zalith Compliance)
 */
@Composable
fun NuxAboutDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("overview") } // "overview", "libraries", "gpl"

    NuxDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(0.92f),
        fillMaxHeight = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(NuxColors.ForestGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = NuxColors.ForestGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TENTANG & LISENSI OPEN SOURCE",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.5.sp,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            NuxBadge(
                                text = "v1.0.6",
                                backgroundColor = NuxColors.ForestGreen.copy(alpha = 0.2f),
                                textColor = NuxColors.MintGreen
                            )
                        }
                        Text(
                            text = "Atribusi hak cipta, kepatuhan GNU GPL-3.0, dan proyek hulu",
                            color = NuxColors.GrayNeutral,
                            fontSize = 8.5.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(NuxColors.SurfaceInput, CircleShape)
                        .border(1.dp, NuxColors.CardBorder, CircleShape)
                        .clickable { onDismissRequest() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = NuxColors.GrayNeutral,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub Navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NuxColors.SurfaceInput, RoundedCornerShape(8.dp))
                    .padding(2.5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "overview" to "RINGKASAN & PROYEK HULU",
                    "libraries" to "DAFTAR LISENSI LENGKAP (${LIBRARIES.size})",
                    "gpl" to "KETENTUAN GNU GPL-3.0"
                ).forEach { (tabKey, title) ->
                    val isSelected = activeTab == tabKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) NuxColors.ForestGreen else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { activeTab = tabKey }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else NuxColors.GrayNeutral,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable Content Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeTab) {
                    "overview" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Unofficial Notice Box (Zalith & GPL 7(c) Compliance)
                            NuxCard(
                                backgroundColor = Color(0xFF1E1710),
                                borderColor = Color(0xFFF59E0B).copy(alpha = 0.4f),
                                cornerRadius = 10.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⚠️", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "PEMBERITAHUAN VERSI MODIFIKASI TIDAK RESMI",
                                            color = Color(0xFFFBBF24),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.5.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "NUX Launcher merupakan Versi Modifikasi Tidak Resmi (Unofficial Modified Version) yang dibangun dan diadaptasi berdasarkan proyek Zalith Launcher 2 dan PojavLauncher. Program ini BUKAN aplikasi resmi dari Zalith Launcher Team maupun Mojang Studios.",
                                        color = Color(0xFFE4E4E7),
                                        fontSize = 8.5.sp,
                                        lineHeight = 12.5.sp
                                    )
                                }
                            }

                            // Core Upstream Credits
                            Text(
                                text = "PROYEK HULU UTAMA (CORE UPSTREAM):",
                                color = NuxColors.GrayNeutral,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )

                            // 1. Zalith Launcher 2 Card
                            UpstreamProjectCard(
                                title = "Zalith Launcher 2",
                                copyright = "Copyright © 2024-2026 MovTery & Contributors",
                                license = "GNU General Public License v3.0 (GPL-3.0)",
                                description = "Komponen arsitektur bridge JNI native (ZLBridge), integrasi SDL3 surface, direct gamepad input, penanganan resolusi DNS, dan optimasi eksekusi OpenJDK mobile.",
                                url = "https://github.com/ZalithLauncher/ZalithLauncher2",
                                onOpenUrl = { url ->
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    } catch (_: Exception) {}
                                }
                            )

                            // 2. PojavLauncher Card
                            UpstreamProjectCard(
                                title = "PojavLauncher",
                                copyright = "Copyright © 2020-present PojavLauncherTeam & Contributors",
                                license = "GNU General Public License v3.0 (GPL-3.0)",
                                description = "Proyek pionir peluncuran Minecraft Java Edition di Android. Menyediakan porting LWJGL 3, GLFW Stubs, AWT stubs, dan integrasi grafis renderer mobile.",
                                url = "https://github.com/PojavLauncherTeam/PojavLauncher",
                                onOpenUrl = { url ->
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    } catch (_: Exception) {}
                                }
                            )

                            // Disclaimer
                            NuxCard(
                                backgroundColor = NuxColors.SurfaceInput,
                                borderColor = NuxColors.CardBorder,
                                cornerRadius = 10.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "PENADIAN RESMI MINECRAFT & MOJANG:",
                                        color = Color(0xFFA1A1AA),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "BUKAN PRODUK RESMI MINECRAFT. TIDAK DISETUJUI OLEH ATAU TERKAIT DENGAN MOJANG STUDIOS ATAU MICROSOFT. Minecraft adalah merek dagang terdaftar milik Mojang AB / Microsoft Corporation. Seluruh aset game diunduh langsung dari server resmi distribusi Mojang.",
                                        color = NuxColors.GrayNeutral,
                                        fontSize = 8.sp,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    "libraries" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LIBRARIES.forEach { lib ->
                                NuxCard(
                                    backgroundColor = NuxColors.SurfaceInput,
                                    borderColor = NuxColors.CardBorder,
                                    cornerRadius = 8.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = lib.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.5.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(NuxColors.ForestGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = lib.license,
                                                        color = NuxColors.ForestGreen,
                                                        fontSize = 7.5.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = lib.copyright,
                                                color = Color(0xFFD4D4D8),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = lib.role,
                                                color = NuxColors.GrayNeutral,
                                                fontSize = 7.5.sp
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Outlined.OpenInNew,
                                            contentDescription = "Buka Link",
                                            tint = NuxColors.GrayNeutral,
                                            modifier = Modifier
                                                .size(15.dp)
                                                .clickable {
                                                    try {
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(lib.url)))
                                                    } catch (_: Exception) {}
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "gpl" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NuxCard(
                                backgroundColor = NuxColors.SurfaceInput,
                                borderColor = NuxColors.CardBorder,
                                cornerRadius = 10.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "GNU GENERAL PUBLIC LICENSE v3.0 (GPL-3.0)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Proyek ini dilisensikan di bawah ketentuan GNU General Public License versi 3.0.\n\n" +
                                                "Prinsip 4 Kebebasan Perangkat Lunak Bebas:\n" +
                                                "1. Kebebasan untuk menjalankan program untuk keperluan apa pun.\n" +
                                                "2. Kebebasan untuk mempelajari cara kerja program dan menyesuaikannya dengan kebutuhan Anda.\n" +
                                                "3. Kebebasan untuk mendistribusikan kembali salinan untuk membantu sesama.\n" +
                                                "4. Kebebasan untuk menyempurnakan program dan merilis penyempurnaan tersebut kepada publik.\n\n" +
                                                "Ketentuan Tambahan Sesuai GPLv3 Pasal 7:\n" +
                                                "• Modifikasi tidak boleh menyalahgunakan nama dagang 'ZalithLauncher' atau 'ZL'.\n" +
                                                "• Modifikasi wajib menampilkan keterangan bahwa ini adalah 'Unofficial Modified Version'.\n" +
                                                "• Hak cipta penulis asli (Copyright © MovTery & PojavLauncherTeam) tidak boleh dihapus.",
                                        color = NuxColors.GrayNeutral,
                                        fontSize = 8.5.sp,
                                        lineHeight = 12.5.sp
                                    )
                                }
                            }

                            NuxButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gnu.org/licenses/gpl-3.0.html")))
                                    } catch (_: Exception) {}
                                },
                                backgroundColor = NuxColors.SurfaceInput,
                                contentColor = NuxColors.ForestGreen,
                                modifier = Modifier.fillMaxWidth().height(32.dp)
                            ) {
                                Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("BACA TEKS LISENSI RESMI GNU GPL-3.0 LENGKAP", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Footer Close Button
            NuxButton(
                onClick = onDismissRequest,
                backgroundColor = NuxColors.ForestGreen,
                contentColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                Text("TUTUP", fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun UpstreamProjectCard(
    title: String,
    copyright: String,
    license: String,
    description: String,
    url: String,
    onOpenUrl: (String) -> Unit
) {
    NuxCard(
        backgroundColor = NuxColors.SurfaceInput,
        borderColor = NuxColors.CardBorder,
        cornerRadius = 10.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(NuxColors.ForestGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = license,
                            color = NuxColors.ForestGreen,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clickable { onOpenUrl(url) }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("GitHub", color = Color(0xFF38BDF8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = copyright,
                color = Color(0xFFE4E4E7),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                color = NuxColors.GrayNeutral,
                fontSize = 8.sp,
                lineHeight = 11.5.sp
            )
        }
    }
}
