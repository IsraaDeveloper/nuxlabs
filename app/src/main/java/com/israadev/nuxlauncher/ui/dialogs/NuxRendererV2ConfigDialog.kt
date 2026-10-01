package com.israadev.nuxlauncher.ui.dialogs

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.israadev.nuxlauncher.core.renderer.NuxRendererInfo
import com.israadev.nuxlauncher.core.renderer.v2.EnvSettingUnit
import com.israadev.nuxlauncher.core.renderer.v2.NuxRendererV2Manager
import com.israadev.nuxlauncher.core.renderer.v2.RendererV2Data
import com.israadev.nuxlauncher.ui.components.NuxButton
import com.israadev.nuxlauncher.ui.components.NuxCard
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes

@Composable
fun NuxRendererV2ConfigDialog(
    rendererInfo: NuxRendererInfo?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val targetPkg = rendererInfo?.pluginPackageName ?: "top.mobilegl.plugin"
    val v2Data: RendererV2Data? = remember(targetPkg, rendererInfo) {
        NuxRendererV2Manager.getPluginData(targetPkg)
            ?: (rendererInfo?.id?.let { NuxRendererV2Manager.getPluginData(it) })
            ?: (rendererInfo?.displayName?.let { NuxRendererV2Manager.getPluginData(it) })
            ?: NuxRendererV2Manager.getPluginData("top.mobilegl.plugin")
            ?: NuxRendererV2Manager.getAllV2Plugins().firstOrNull()
    }

    val activePkg = v2Data?.packageName ?: rendererInfo?.pluginPackageName ?: "top.mobilegl.plugin"
    val isAppInstalled = remember(activePkg) {
        try {
            context.packageManager.getPackageInfo(activePkg, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .padding(horizontal = 24.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            NuxCard(
                modifier = Modifier
                    .width(580.dp)
                    .fillMaxHeight(0.94f),
                backgroundColor = NuxColors.SurfaceElevated,
                borderColor = Color(0x3310B981),
                cornerRadius = NuxSizes.CornerRadiusLarge,
                fillMaxHeight = false
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NuxColors.ForestGreen.copy(alpha = 0.15f))
                                    .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
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
                                    fontSize = 14.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = v2Data?.config?.displayName ?: rendererInfo?.displayName ?: "MobileGlues Engine",
                                    color = NuxColors.MintGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Close button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NuxColors.SurfaceInput)
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(8.dp))
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

                    // Scrollable Config Items Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Shortcut ke Aplikasi Plugin (jika terpasang)
                        if (isAppInstalled) {
                            val cardShape = RoundedCornerShape(10.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(cardShape)
                                    .background(Color(0xFF141923), cardShape)
                                    .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.35f), cardShape)
                                    .clickable {
                                        try {
                                            val launchIntent = context.packageManager.getLaunchIntentForPackage(activePkg)
                                            if (launchIntent != null) {
                                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(launchIntent)
                                            } else {
                                                Toast.makeText(context, "Tidak dapat membuka panel aplikasi", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(NuxColors.ForestGreen.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.OpenInNew,
                                                contentDescription = null,
                                                tint = NuxColors.MintGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Buka Panel Aplikasi ${v2Data?.config?.displayName ?: rendererInfo?.displayName ?: "MobileGL"}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "Akses pengaturan driver native, benchmark, dan preferensi aplikasi",
                                                color = Color(0xFFA1A1AA),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        tint = NuxColors.MintGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Daftar Environment Variables Dinamis (Selectable / Toggleable / Customizable)
                        val units = v2Data?.units ?: emptyList()
                        if (units.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tidak ada environment variable yang dapat dikonfigurasi untuk renderer ini.",
                                    color = Color(0xFF71717A),
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            Text(
                                text = "PENGATURAN ENVIRONMENT VARIABLES",
                                color = Color(0xFF71717A),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            units.forEach { unit ->
                                when (unit) {
                                    is EnvSettingUnit.Selectable -> {
                                        SelectableEnvItemCard(unit)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    is EnvSettingUnit.Toggleable -> {
                                        ToggleableEnvItemCard(unit)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    is EnvSettingUnit.Customizable -> {
                                        CustomizableEnvItemCard(unit)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sticky Footer Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tombol Reset ke Default
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    v2Data?.resetAll()
                                    Toast.makeText(context, "Pengaturan direset ke default", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RESET DEFAULT",
                                    color = Color(0xFFA1A1AA),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        NuxButton(
                            onClick = onDismiss,
                            backgroundColor = NuxColors.ForestGreen,
                            contentColor = Color.White,
                            cornerRadius = 8.dp,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "SIMPAN & TUTUP",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Komponen Kartu untuk Selectable Environment (Dropdown Picker)
 * Contoh: MOBILEGL_BACKEND_TYPE (DirectGLES, DirectVulkan)
 */
@Composable
private fun SelectableEnvItemCard(unit: EnvSettingUnit.Selectable) {
    var expanded by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color(0xFF13161F), cardShape)
            .border(1.dp, if (unit.isEnabled) Color(0x3010B981) else Color(0x18FFFFFF), cardShape)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (unit.isEnabled) expanded = !expanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox aktif/tidak
                Checkbox(
                    checked = unit.isEnabled,
                    onCheckedChange = { checked ->
                        unit.saveCheck(checked)
                        if (!checked) expanded = false
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = NuxColors.ForestGreen,
                        uncheckedColor = Color(0xFF52525B),
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = unit.key,
                        color = if (unit.isEnabled) Color.White else Color(0xFF71717A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (!unit.summary.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = unit.summary,
                            color = Color(0xFFA1A1AA),
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Nilai aktif: ",
                            color = Color(0xFF71717A),
                            fontSize = 10.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NuxColors.ForestGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = unit.state,
                                color = NuxColors.MintGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (unit.isEnabled) Color.White else Color(0xFF52525B),
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(if (expanded) 180f else 0f)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0D1017))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    unit.values.forEach { option ->
                        val isSelected = unit.state == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NuxColors.ForestGreen.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    unit.save(option)
                                    expanded = false
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = option,
                                color = if (isSelected) NuxColors.MintGreen else Color(0xFFA1A1AA),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NuxColors.MintGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Komponen Kartu untuk Toggleable Environment (Switch ON/OFF)
 * Contoh: MOBILEGL_DISABLE_TIMERQUERRY, MOBILEGL_MAGME_DISABLE_SUBGROUP
 */
@Composable
private fun ToggleableEnvItemCard(unit: EnvSettingUnit.Toggleable) {
    val cardShape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color(0xFF13161F), cardShape)
            .border(1.dp, if (unit.isEnabled) Color(0x3010B981) else Color(0x18FFFFFF), cardShape)
            .clickable { unit.setToggle(!unit.isEnabled) }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = unit.key,
                    color = if (unit.isEnabled) Color.White else Color(0xFFD4D4D8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (!unit.summary.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = unit.summary,
                        color = Color(0xFFA1A1AA),
                        fontSize = 10.sp
                    )
                }
            }

            Switch(
                checked = unit.isEnabled,
                onCheckedChange = { checked -> unit.setToggle(checked) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = NuxColors.ForestGreen,
                    uncheckedThumbColor = Color(0xFF71717A),
                    uncheckedTrackColor = Color(0xFF27272A)
                )
            )
        }
    }
}

/**
 * Komponen Kartu untuk Customizable Environment (Input Text)
 * Contoh: MOBILEGL_CUSTOM_FLAGS
 */
@Composable
private fun CustomizableEnvItemCard(unit: EnvSettingUnit.Customizable) {
    val cardShape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color(0xFF13161F), cardShape)
            .border(1.dp, Color(0x18FFFFFF), cardShape)
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = unit.key,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            if (!unit.summary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = unit.summary,
                    color = Color(0xFFA1A1AA),
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = unit.state,
                onValueChange = { unit.save(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                ),
                placeholder = {
                    Text(text = "Masukkan nilai (opsional)", color = Color(0xFF52525B), fontSize = 11.sp)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NuxColors.ForestGreen,
                    unfocusedBorderColor = Color(0x28FFFFFF),
                    focusedContainerColor = Color(0xFF0D1017),
                    unfocusedContainerColor = Color(0xFF0D1017)
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}
