package com.israadev.nuxlauncher.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.israadev.nuxlauncher.core.renderer.MobileGluesConfig
import com.israadev.nuxlauncher.core.renderer.MobileGluesConfigManager
import com.israadev.nuxlauncher.core.renderer.NuxRendererInfo
import com.israadev.nuxlauncher.ui.components.NuxButton
import com.israadev.nuxlauncher.ui.components.NuxCard
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes

@Composable
fun NuxRendererConfigDialog(
    renderer: NuxRendererInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isMobileGlues = renderer.pluginPackageName == "com.fcl.plugin.mobileglues" ||
            renderer.id.contains("mobileglue", ignoreCase = true) ||
            renderer.displayName.contains("MobileGlues", ignoreCase = true)

    var config by remember { mutableStateOf(MobileGluesConfigManager.loadConfig()) }
    var enableAngle by remember(config) { mutableStateOf(config.enableANGLE) }
    var enableNoError by remember(config) { mutableStateOf(config.enableNoError) }
    var angleDepthFix by remember(config) { mutableStateOf(config.angleDepthClearFixMode) }
    var enableComputeDsa by remember(config) { mutableStateOf(config.enableExtComputeShader && config.enableExtDirectStateAccess) }
    var enableFsr by remember(config) { mutableStateOf(config.fsr1Setting > 0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            NuxCard(
                modifier = Modifier
                    .width(520.dp)
                    .fillMaxHeight(0.92f),
                backgroundColor = NuxColors.SurfaceElevated,
                borderColor = Color(0x33FFFFFF),
                cornerRadius = NuxSizes.CornerRadiusLarge,
                fillMaxHeight = false
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
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
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NuxColors.ForestGreen.copy(alpha = 0.2f))
                                    .border(1.dp, NuxColors.MintGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Renderer Config",
                                    tint = NuxColors.MintGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "KONFIGURASI RENDERER",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = renderer.displayName,
                                    color = NuxColors.MintGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Close "✕" Button
                        val closeShape = RoundedCornerShape(6.dp)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(closeShape)
                                .background(NuxColors.SurfaceInput, closeShape)
                                .border(1.dp, NuxColors.CardBorder, closeShape)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Open Native App Quick Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(NuxColors.ForestGreen.copy(alpha = 0.12f))
                            .border(1.dp, NuxColors.MintGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .clickable {
                                val opened = MobileGluesConfigManager.openMobileGluesApp(context)
                                if (!opened) {
                                    Toast.makeText(context, "Aplikasi MobileGlues tidak ditemukan", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Buka Panel Aplikasi MobileGlues",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NuxColors.MintGreen
                            )
                            Text(
                                text = "Akses seluruh pengaturan grafis, GLSL cache, dan multi-draw",
                                fontSize = 9.sp,
                                color = NuxColors.GrayNeutral
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = "Buka App",
                            tint = NuxColors.MintGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scrollable Config Options
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Option 1: ANGLE Driver
                        ConfigToggleItem(
                            title = "Gunakan ANGLE (OpenGL ES over Vulkan)",
                            description = "Menerjemahkan instruksi GL ke Vulkan untuk stabilitas & FPS lebih baik.",
                            checked = enableAngle,
                            onCheckedChange = { enableAngle = it }
                        )

                        // Option 2: NoError Mode
                        ConfigToggleItem(
                            title = "Mode Abaikan Error (OpenGL NoError)",
                            description = "Mencegah crash permainan akibat error framebuffer atau program shader.",
                            checked = enableNoError,
                            onCheckedChange = { enableNoError = it }
                        )

                        // Option 3: ANGLE Depth Clear Fix
                        ConfigToggleItem(
                            title = "Workaround Depth Clear Fix ANGLE",
                            description = "Memperbaiki glitch visual atau layar hitam pada perangkat / GPU tertentu.",
                            checked = angleDepthFix,
                            onCheckedChange = { angleDepthFix = it }
                        )

                        // Option 4: Compute Shader & DSA
                        ConfigToggleItem(
                            title = "Ekstensi Compute Shader & DSA",
                            description = "Mengaktifkan ekstensi modern OpenGL ES untuk shader pack Minecraft.",
                            checked = enableComputeDsa,
                            onCheckedChange = { enableComputeDsa = it }
                        )

                        // Option 5: FSR 1.0 Upscaling
                        ConfigToggleItem(
                            title = "Built-in FSR 1.0 Upscaling",
                            description = "Peningkatan resolusi spasial AMD FidelityFX bawaan MobileGlues untuk performa ekstra.",
                            checked = enableFsr,
                            onCheckedChange = { enableFsr = it }
                        )

                        // Info Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(NuxColors.SurfaceInput)
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Lokasi file konfigurasi tersimpan di: /sdcard/MG/config.json. Pengaturan ini otomatis dibaca oleh modul renderer saat permainan diluncurkan.",
                                fontSize = 8.5.sp,
                                color = NuxColors.GrayNeutral,
                                lineHeight = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Button (Simpan)
                    NuxButton(
                        onClick = {
                            val updated = config.copy(
                                enableANGLE = enableAngle,
                                enableNoError = enableNoError,
                                angleDepthClearFixMode = angleDepthFix,
                                enableExtComputeShader = enableComputeDsa,
                                enableExtDirectStateAccess = enableComputeDsa,
                                fsr1Setting = if (enableFsr) 1 else 0
                            )
                            val success = MobileGluesConfigManager.saveConfig(updated)
                            if (success) {
                                Toast.makeText(context, "Konfigurasi MobileGlues berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Gagal menulis file /sdcard/MG/config.json", Toast.LENGTH_LONG).show()
                            }
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        backgroundColor = NuxColors.ForestGreen,
                        contentColor = Color.White,
                        cornerRadius = 6.dp
                    ) {
                        Text("SIMPAN KONFIGURASI", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(NuxColors.SurfaceInput)
            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 8.5.sp,
                color = NuxColors.GrayNeutral,
                lineHeight = 11.5.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NuxColors.ForestGreen,
                uncheckedThumbColor = NuxColors.GrayNeutral,
                uncheckedTrackColor = NuxColors.SurfaceElevated
            )
        )
    }
}
