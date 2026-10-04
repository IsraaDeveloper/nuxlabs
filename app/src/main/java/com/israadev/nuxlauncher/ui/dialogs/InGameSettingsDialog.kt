package com.israadev.nuxlauncher.ui.dialogs

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.israadev.nuxlauncher.FpsMode
import com.israadev.nuxlauncher.ui.control.MouseControlMode
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.movtery.inputmap.keycodes.LwjglGlfwKeycode
import kotlin.math.roundToInt

@Composable
fun InGameSettingsDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    currentFps: Int,
    instanceName: String,
    mcVersion: String,
    currentResolutionRatio: Int,
    onResolutionChange: (Int) -> Unit,
    fpsMode: FpsMode,
    onFpsModeChange: (FpsMode) -> Unit,
    onOpenConsoleLog: () -> Unit,
    cursorSensitivity: Int,
    onCursorSensitivityChange: (Int) -> Unit,
    captureSensitivity: Int,
    onCaptureSensitivityChange: (Int) -> Unit,
    mouseControlMode: MouseControlMode,
    onMouseControlModeChange: (MouseControlMode) -> Unit,
    isControlVisible: Boolean,
    onToggleControlVisibility: () -> Unit,
    onRequestKeyboard: () -> Unit,
    onSendKeycode: (Int) -> Unit,
    onForceExitRequest: () -> Unit,
    onOpenCustomGui: () -> Unit = {}
) {
    if (!visible) return

    var selectedTab by remember { mutableIntStateOf(0) }
    var tempResolution by remember(currentResolutionRatio) { mutableFloatStateOf(currentResolutionRatio.toFloat()) }
    var tempCursorSens by remember(cursorSensitivity) { mutableFloatStateOf(cursorSensitivity.toFloat()) }
    var tempCaptureSens by remember(captureSensitivity) { mutableFloatStateOf(captureSensitivity.toFloat()) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogView = LocalView.current
        SideEffect {
            val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
            dialogWindow?.let { win ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    win.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    win.attributes = win.attributes.apply {
                        blurBehindRadius = 45
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    win.attributes = win.attributes.apply {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val cardShape = RoundedCornerShape(20.dp)
            Card(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .clip(cardShape)
                    .border(1.2.dp, Color(0x3810B981), cardShape),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xF20F1522)
                ),
                shape = cardShape
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // 1. Header Bar: Logo + Title + FPS Indicator + Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NuxColors.ForestGreen.copy(alpha = 0.25f))
                                    .border(1.dp, NuxColors.MintGreen.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "N",
                                    color = NuxColors.MintGreen,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "PENGATURAN DALAM GAME",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "$instanceName · v$mcVersion",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Realtime FPS Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x66000000),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3310B981))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                when {
                                                    currentFps >= 50 -> Color(0xFF10B981)
                                                    currentFps >= 25 -> Color(0xFFF59E0B)
                                                    currentFps > 0 -> Color(0xFFEF4444)
                                                    else -> Color(0xFF6B7280)
                                                },
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (currentFps > 0) "$currentFps FPS" else "FPS: --",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Custom GUI In-Game Quick Action Button
                            IconButton(
                                onClick = {
                                    onDismissRequest()
                                    onOpenCustomGui()
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NuxColors.ForestGreen.copy(alpha = 0.5f))
                                    .border(1.dp, NuxColors.MintGreen, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Tune,
                                    contentDescription = "Custom GUI In-Game",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Close Button
                            IconButton(
                                onClick = onDismissRequest,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup Menu",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Tab Navigation
                    val tabs = listOf("Grafis & Layar", "Kontrol & Mouse", "Pintas & Sistem")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x40000000))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NuxColors.ForestGreen else Color.Transparent)
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Tab Contents (Scrollable)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (selectedTab) {
                            0 -> {
                                // === TAB 0: GRAFIS & LAYAR ===
                                // A. Slider Skala Resolusi
                                InGameSectionCard(title = "Skala Resolusi Layar (Resolution Scale)") {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Resolusi Render:",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "${tempResolution.roundToInt()}%",
                                                color = NuxColors.MintGreen,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Slider(
                                            value = tempResolution,
                                            onValueChange = { tempResolution = it },
                                            onValueChangeFinished = {
                                                onResolutionChange(tempResolution.roundToInt())
                                            },
                                            valueRange = 40f..125f,
                                            steps = 16,
                                            colors = SliderDefaults.colors(
                                                thumbColor = NuxColors.MintGreen,
                                                activeTrackColor = NuxColors.ForestGreen,
                                                inactiveTrackColor = Color(0x33FFFFFF)
                                            )
                                        )

                                        // Presets
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(50, 75, 100).forEach { preset ->
                                                val isCur = tempResolution.roundToInt() == preset
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isCur) NuxColors.ForestGreen.copy(alpha = 0.5f) else Color(0x22FFFFFF))
                                                        .border(1.dp, if (isCur) NuxColors.MintGreen else Color.Transparent, RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            tempResolution = preset.toFloat()
                                                            onResolutionChange(preset)
                                                        }
                                                        .padding(vertical = 5.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "$preset% ${if (preset == 50) "Performa" else if (preset == 75) "Seimbang" else "Penuh"}",
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "💡 Resolusi langsung berubah secara dinamis di engine tanpa perlu restart.",
                                            color = Color(0xFF64748B),
                                            fontSize = 9.5.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // B. Pin FPS Toggle
                                InGameSectionCard(title = "Indikator FPS") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Pin FPS di Layar",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Tetap tampil meski kontrol tombol layar disembunyikan (Hide GUI)",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.sp
                                            )
                                        }
                                        Switch(
                                            checked = fpsMode == FpsMode.PINNED,
                                            onCheckedChange = { checked ->
                                                onFpsModeChange(if (checked) FpsMode.PINNED else FpsMode.NORMAL)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = NuxColors.ForestGreen
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // C. Live Console Log Button
                                InGameSectionCard(title = "Diagnostik Engine") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x3310B981))
                                            .border(1.dp, Color(0x4D10B981), RoundedCornerShape(8.dp))
                                            .clickable {
                                                onDismissRequest()
                                                onOpenConsoleLog()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Terminal,
                                                contentDescription = null,
                                                tint = NuxColors.MintGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Buka Live Console Log",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Outlined.ChevronRight,
                                            contentDescription = null,
                                            tint = NuxColors.MintGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            1 -> {
                                // === TAB 1: KONTROL & MOUSE ===
                                // A. Sensitivitas Kursor
                                InGameSectionCard(title = "Sensitivitas Kursor Virtual") {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Kecepatan Pointer:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                            Text("${tempCursorSens.roundToInt()}%", color = NuxColors.MintGreen, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                        }
                                        Slider(
                                            value = tempCursorSens,
                                            onValueChange = { tempCursorSens = it },
                                            onValueChangeFinished = {
                                                onCursorSensitivityChange(tempCursorSens.roundToInt())
                                            },
                                            valueRange = 25f..250f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = NuxColors.MintGreen,
                                                activeTrackColor = NuxColors.ForestGreen,
                                                inactiveTrackColor = Color(0x33FFFFFF)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // B. Sensitivitas Kamera (Capture)
                                InGameSectionCard(title = "Sensitivitas Kamera / Aim 360") {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Kecepatan Putar Pandangan:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                            Text("${tempCaptureSens.roundToInt()}%", color = NuxColors.MintGreen, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                        }
                                        Slider(
                                            value = tempCaptureSens,
                                            onValueChange = { tempCaptureSens = it },
                                            onValueChangeFinished = {
                                                onCaptureSensitivityChange(tempCaptureSens.roundToInt())
                                            },
                                            valueRange = 25f..250f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = NuxColors.MintGreen,
                                                activeTrackColor = NuxColors.ForestGreen,
                                                inactiveTrackColor = Color(0x33FFFFFF)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // C. Mode Navigasi Mouse & Visibilitas GUI
                                InGameSectionCard(title = "Metode Kontrol Sentuh") {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Mode Mouse:",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                listOf(
                                                    MouseControlMode.SLIDE to "Geser (Slide)",
                                                    MouseControlMode.CLICK to "Sentuh (Click)"
                                                ).forEach { (mode, label) ->
                                                    val isSelected = mouseControlMode == mode
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(if (isSelected) NuxColors.ForestGreen else Color(0x22FFFFFF))
                                                            .clickable { onMouseControlModeChange(mode) }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = Color(0x1AFFFFFF))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Tampilkan Tombol Layar (GUI)",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "Tombol virtual di layar on-screen",
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 9.5.sp
                                                )
                                            }
                                            Switch(
                                                checked = isControlVisible,
                                                onCheckedChange = { onToggleControlVisibility() },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = NuxColors.ForestGreen
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // D. Kustomisasi Tombol (Custom GUI In-Game)
                                InGameSectionCard(title = "Kustomisasi Tombol (Custom GUI)") {
                                    Column {
                                        Text(
                                            text = "Buka editor GUI langsung di dalam game dengan background blur transparan. Geser dan atur tombol sesuai HUD Minecraft secara live!",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(
                                                            Color(0xFF10B981),
                                                            Color(0xFF059669)
                                                        )
                                                    )
                                                )
                                                .clickable {
                                                    onDismissRequest()
                                                    onOpenCustomGui()
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Tune,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = "EDIT TOMBOL IN-GAME",
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                    Text(
                                                        text = "Background live blur · Atur posisi tombol sekarang",
                                                        color = Color(0xFFD1FAE5),
                                                        fontSize = 9.sp
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.Outlined.ChevronRight,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            2 -> {
                                // === TAB 2: PINTAS & SISTEM ===
                                InGameSectionCard(title = "Tombol Pintas Cepat (Quick Keycodes)") {
                                    Column {
                                        Text(
                                            text = "Kirim input tombol keyboard langsung ke Minecraft:",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        val quickKeys = listOf(
                                            Triple("F3", "Debug", LwjglGlfwKeycode.GLFW_KEY_F3),
                                            Triple("F5", "Kamera", LwjglGlfwKeycode.GLFW_KEY_F5),
                                            Triple("ESC", "Pause", LwjglGlfwKeycode.GLFW_KEY_ESCAPE),
                                            Triple("T", "Chat", LwjglGlfwKeycode.GLFW_KEY_T),
                                            Triple("TAB", "Players", LwjglGlfwKeycode.GLFW_KEY_TAB),
                                            Triple("F1", "Hide HUD", LwjglGlfwKeycode.GLFW_KEY_F1)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            quickKeys.take(3).forEach { (label, desc, code) ->
                                                QuickKeyButton(
                                                    modifier = Modifier.weight(1f),
                                                    label = label,
                                                    desc = desc,
                                                    onClick = {
                                                        onSendKeycode(code)
                                                    }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            quickKeys.drop(3).forEach { (label, desc, code) ->
                                                QuickKeyButton(
                                                    modifier = Modifier.weight(1f),
                                                    label = label,
                                                    desc = desc,
                                                    onClick = {
                                                        onSendKeycode(code)
                                                    }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Keyboard Toggle
                                        Button(
                                            onClick = {
                                                onDismissRequest()
                                                onRequestKeyboard()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0x3338BDF8)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Keyboard,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Buka Keyboard Virtual (IME)",
                                                color = Color(0xFFBAE6FD),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // B. Force Exit Section
                                InGameSectionCard(title = "Manajemen Proses") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x26EF4444))
                                            .border(1.dp, Color(0x4DEF4444), RoundedCornerShape(8.dp))
                                            .clickable {
                                                onDismissRequest()
                                                onForceExitRequest()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.PowerSettingsNew,
                                                contentDescription = null,
                                                tint = Color(0xFFFCA5A5),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Paksa Berhenti (Kill Game)",
                                                    color = Color(0xFFFCA5A5),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Hentikan JVM seketika jika Minecraft macet",
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Outlined.ChevronRight,
                                            contentDescription = null,
                                            tint = Color(0xFFFCA5A5),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InGameSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0x59000000),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                color = NuxColors.MintGreen,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun QuickKeyButton(
    modifier: Modifier = Modifier,
    label: String,
    desc: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = Color(0x33FFFFFF),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp
            )
            Text(
                text = desc,
                color = Color(0xFF94A3B8),
                fontSize = 8.5.sp
            )
        }
    }
}
