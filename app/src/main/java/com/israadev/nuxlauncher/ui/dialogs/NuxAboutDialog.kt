package com.israadev.nuxlauncher.ui.dialogs

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.ui.components.NuxBadge
import com.israadev.nuxlauncher.ui.components.NuxButton
import com.israadev.nuxlauncher.ui.components.NuxCard
import com.israadev.nuxlauncher.ui.components.NuxDialog
import com.israadev.nuxlauncher.ui.theme.LocalNuxScale
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
 * Redesigned with High-End Double-Bezel Architecture & Fluid Responsiveness
 */
@Composable
fun NuxAboutDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val isTablet = LocalNuxScale.current.isTablet
    var activeTab by remember { mutableStateOf("overview") } // "overview", "libraries", "gpl"

    NuxDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(if (isTablet) 0.82f else 0.94f),
        fillMaxHeight = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // ==========================================
            // 1. TOP HEADER BAR (Double-Bezel Aura)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Outer bezel icon ring
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(NuxColors.ForestGreen.copy(alpha = 0.28f), Color.Transparent)
                                ),
                                CircleShape
                            )
                            .border(1.2.dp, NuxColors.ForestGreen.copy(alpha = 0.6f), CircleShape)
                            .padding(2.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF141822), CircleShape)
                                .border(1.dp, Color(0x33FFFFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = NuxColors.MintGreen,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TENTANG & LISENSI OPEN SOURCE",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 0.6.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            NuxBadge(
                                text = "v1.0.6",
                                backgroundColor = NuxColors.ForestGreen.copy(alpha = 0.18f),
                                textColor = NuxColors.MintGreen,
                                borderColor = NuxColors.MintGreen.copy(alpha = 0.35f)
                            )
                        }
                        Text(
                            text = "Atribusi hak cipta, kepatuhan GNU GPL-3.0, dan proyek hulu",
                            color = NuxColors.GrayNeutral,
                            fontSize = 8.5.sp
                        )
                    }
                }

                // Minimalist Close Button
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF151821), CircleShape)
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable { onDismissRequest() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFFA1A1AA),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // 2. SEGMENTED TAB SWITCHER (Machined Pill Track)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFF0F1219))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(9.dp))
                    .padding(2.5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "overview" to "RINGKASAN & PROYEK HULU",
                    "libraries" to "DAFTAR LISENSI LENGKAP (${LIBRARIES.size})",
                    "gpl" to "KETENTUAN GNU GPL-3.0"
                ).forEach { (tabKey, title) ->
                    val isSelected = activeTab == tabKey
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else Color(0xFFA1A1AA),
                        animationSpec = tween(150),
                        label = "tabText"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (isSelected) Brush.horizontalGradient(
                                    listOf(Color(0xFF059669), Color(0xFF10B981))
                                ) else Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color.Transparent)
                                )
                            )
                            .clickable { activeTab = tabKey }
                            .padding(vertical = 5.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            fontSize = 8.5.sp,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // 3. SCROLLABLE CONTENT BODY (Double-Bezel)
            // ==========================================
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
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1C150A))
                                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                    .padding(9.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⚠️", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "PEMBERITAHUAN VERSI MODIFIKASI TIDAK RESMI",
                                            color = Color(0xFFFBBF24),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
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

                            // Core Upstream Credits Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(NuxColors.MintGreen, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PROYEK HULU UTAMA (CORE UPSTREAM):",
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.6.sp
                                )
                            }

                            // 1. Zalith Launcher 2 Card (Double-Bezel)
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

                            // 2. PojavLauncher Card (Double-Bezel)
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

                            // Disclaimer Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF12151E))
                                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                                    .padding(9.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "PENADIAN RESMI MINECRAFT & MOJANG:",
                                        color = Color(0xFFA1A1AA),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "BUKAN PRODUK RESMI MINECRAFT. TIDAK DISETUJUI OLEH ATAU TERKAIT DENGAN MOJANG STUDIOS ATAU MICROSOFT. Minecraft adalah merek dagang terdaftar milik Mojang AB / Microsoft Corporation. Seluruh aset game diunduh langsung dari server resmi distribusi Mojang.",
                                        color = Color(0xFF71717A),
                                        fontSize = 7.5.sp,
                                        lineHeight = 11.5.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
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
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(Color(0xFF131620))
                                        .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(9.dp))
                                        .padding(horizontal = 9.dp, vertical = 7.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = lib.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 9.5.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(NuxColors.ForestGreen.copy(alpha = 0.16f))
                                                        .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = lib.license,
                                                        color = NuxColors.MintGreen,
                                                        fontSize = 7.sp,
                                                        fontWeight = FontWeight.Bold
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
                                                color = Color(0xFFA1A1AA),
                                                fontSize = 7.5.sp
                                            )
                                        }

                                        // Action Link Button
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF1A2234))
                                                .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    try {
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(lib.url)))
                                                    } catch (_: Exception) {}
                                                }
                                                .padding(horizontal = 6.dp, vertical = 3.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Link", color = Color(0xFF38BDF8), fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Icon(
                                                    imageVector = Icons.Outlined.OpenInNew,
                                                    contentDescription = "Buka Link",
                                                    tint = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    "gpl" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF131620))
                                    .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "GNU GENERAL PUBLIC LICENSE v3.0 (GPL-3.0)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp
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
                                        color = Color(0xFFD4D4D8),
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
                                backgroundColor = Color(0xFF141A28),
                                contentColor = Color(0xFF38BDF8),
                                borderColor = Color(0x3338BDF8),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                            ) {
                                Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("BACA TEKS LISENSI RESMI GNU GPL-3.0 LENGKAP", color = Color(0xFF38BDF8), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ==========================================
            // 4. FOOTER CLOSE BUTTON (Cyber Glass Pill)
            // ==========================================
            NuxButton(
                onClick = onDismissRequest,
                backgroundColor = Color(0xFF1A1D27),
                borderColor = Color(0x33FFFFFF),
                contentColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                Text(
                    text = "TUTUP",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF131620))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(10.dp))
            .padding(9.dp)
    ) {
        Column {
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
                            .clip(RoundedCornerShape(4.dp))
                            .background(NuxColors.ForestGreen.copy(alpha = 0.16f))
                            .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = license,
                            color = NuxColors.MintGreen,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Interactive GitHub Pill Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1A2234))
                        .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(6.dp))
                        .clickable { onOpenUrl(url) }
                        .padding(horizontal = 6.dp, vertical = 2.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("GitHub", color = Color(0xFF38BDF8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = copyright,
                color = Color(0xFFE4E4E7),
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                color = Color(0xFFA1A1AA),
                fontSize = 7.5.sp,
                lineHeight = 11.sp
            )
        }
    }
}
