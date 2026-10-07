package com.israadev.nuxlauncher.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.core.crash.CrashUtils
import com.israadev.nuxlauncher.core.crash.GameCrashInfo
import com.israadev.nuxlauncher.core.crash.MCLogsUploader
import com.israadev.nuxlauncher.ui.components.*
import com.israadev.nuxlauncher.ui.theme.NuxColors
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun NuxCrashDialog(
    crashInfo: GameCrashInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }
    var uploadSuccessUrl by remember { mutableStateOf<String?>(null) }

    val settings by com.israadev.nuxlauncher.core.settings.SettingsManager.settings.collectAsState()
    var aiState by remember { mutableStateOf<com.israadev.nuxlauncher.core.crash.AIStreamState>(com.israadev.nuxlauncher.core.crash.AIStreamState.Idle) }
    var rightViewMode by remember { mutableStateOf("ai") } // "ai" or "raw"
    val isAutoAnalyze = settings.aiAutoAnalyze

    var remainingQuota by remember {
        mutableStateOf(com.israadev.nuxlauncher.core.crash.AICrashQuotaManager.getRemainingQuota(context, settings))
    }

    LaunchedEffect(Unit) {
        val synced = com.israadev.nuxlauncher.core.crash.AICrashQuotaManager.syncQuotaFromDatabase(context, settings)
        remainingQuota = synced
    }

    LaunchedEffect(settings.aiApiKey) {
        remainingQuota = com.israadev.nuxlauncher.core.crash.AICrashQuotaManager.getRemainingQuota(context, settings)
    }

    fun runAiAnalysis() {
        rightViewMode = "ai"
        if (!com.israadev.nuxlauncher.core.crash.AICrashQuotaManager.hasQuota(context, settings)) {
            aiState = com.israadev.nuxlauncher.core.crash.AIStreamState.QuotaExceeded
            return
        }
        coroutineScope.launch {
            com.israadev.nuxlauncher.core.crash.AICrashAnalyzer.analyzeCrashStreaming(context, crashInfo, settings).collect { state ->
                aiState = state
                if (state is com.israadev.nuxlauncher.core.crash.AIStreamState.Connecting ||
                    state is com.israadev.nuxlauncher.core.crash.AIStreamState.Streaming ||
                    state is com.israadev.nuxlauncher.core.crash.AIStreamState.Completed
                ) {
                    remainingQuota = com.israadev.nuxlauncher.core.crash.AICrashQuotaManager.getRemainingQuota(context, settings)
                }
            }
        }
    }

    LaunchedEffect(crashInfo, isAutoAnalyze) {
        if (isAutoAnalyze) {
            runAiAnalysis()
        }
    }

    var isCursorBlinkVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            isCursorBlinkVisible = !isCursorBlinkVisible
        }
    }

    val logFile = remember(crashInfo.fullLogPath) {
        if (crashInfo.fullLogPath.isNotBlank()) File(crashInfo.fullLogPath)
        else File(context.filesDir, "latestlog.txt")
    }

    val exitDetailMessage = remember(crashInfo) {
        if (crashInfo.isLauncherCrash) {
            "Peluncur mengalami kesalahan internal pada komponen UI atau proses background sistem."
        } else {
            CrashUtils.getExitMessage(crashInfo.exitCode, crashInfo.isSignal)
        }
    }

    val statusBadge = remember(crashInfo) {
        crashInfo.getStatusBadgeText()
    }

    val mainMessage = remember(crashInfo) {
        crashInfo.getMainMessage()
    }

    val outerShape = RoundedCornerShape(20.dp)
    val cardShape = RoundedCornerShape(14.dp)
    val innerShape = RoundedCornerShape(10.dp)

    NuxDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.96f),
        fillMaxHeight = true
    ) {
        // Outer Shell Container with subtle Crimson/Rose ambient glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF181014),
                            Color(0xFF0F0B0E)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFF43F5E), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "INCIDENT DIAGNOSTICS · JVM TERMINATION",
                                color = Color(0xFFF43F5E).copy(alpha = 0.85f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                letterSpacing = 1.2.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = crashInfo.getDisplayTitle(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    letterSpacing = (-0.3).sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFF43F5E).copy(alpha = 0.2f))
                                        .border(1.dp, Color(0xFFF43F5E).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = statusBadge,
                                        color = Color(0xFFFDA4AF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }

                    // Close Button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF26191E))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFFDA4AF),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Two-Column Content
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ==========================================
                    // LEFT COLUMN: Details, Diagnosis & Actions
                    // ==========================================
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(cardShape)
                            .background(Color(0xFF140D11))
                            .border(1.dp, Color(0x26F43F5E), cardShape)
                            .padding(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. Session Information Card
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(innerShape)
                                        .background(Color(0xFF1C1318))
                                        .border(1.dp, Color(0x1AFFFFFF), innerShape)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = if (crashInfo.isLauncherCrash) "STATUS PELUNCUR" else "INFORMASI SESI GAME",
                                        color = Color(0xFFA1A1AA),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.5.sp,
                                        letterSpacing = 0.8.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = if (crashInfo.isLauncherCrash) "NUX Launcher Android Engine" else "${crashInfo.instanceName} (MC ${crashInfo.mcVersion})",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF2E1C24))
                                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        ) {
                                            Text(
                                                text = "${crashInfo.loader.uppercase()} ${crashInfo.loaderVersion?.let { "v$it" } ?: ""}".trim(),
                                                color = Color(0xFF38BDF8),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 8.sp
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF2E1C24))
                                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        ) {
                                            Text(
                                                text = "${crashInfo.installedMods.size} Mod",
                                                color = Color(0xFFA1A1AA),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 8.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = mainMessage,
                                        color = Color(0xFFFDA4AF),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                    if (crashInfo.fullLogPath.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF0F0A0D))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Log: ${File(crashInfo.fullLogPath).name}",
                                                color = Color(0xFF9CA3AF),
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                // 2. Root Cause & Process Termination Status Card
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(innerShape)
                                        .background(Color(0xFF261017))
                                        .border(1.dp, Color(0xFFF43F5E).copy(alpha = 0.45f), innerShape)
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⚠️", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "DIAGNOSA KELUAR SISTEM",
                                            color = Color(0xFFFDA4AF),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            letterSpacing = 0.8.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = exitDetailMessage,
                                        color = Color(0xFFF4F4F5),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // 3. Analysis Note Card
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(innerShape)
                                        .background(Color(0xFF140E13))
                                        .border(1.dp, Color(0x1AFFFFFF), innerShape)
                                        .padding(9.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("💡", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "SOLUSI & SARAN PEMULIHAN",
                                            color = Color(0xFFA1A1AA),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.5.sp,
                                            letterSpacing = 0.8.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = CrashUtils.CRASH_LOG_NOTE,
                                        color = Color(0xFF71717A),
                                        fontSize = 9.sp,
                                        lineHeight = 13.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Action Buttons
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Primary Action: Upload to mclo.gs
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (uploadSuccessUrl != null) Brush.horizontalGradient(
                                                listOf(Color(0xFF059669), Color(0xFF10B981))
                                            ) else Brush.horizontalGradient(
                                                listOf(Color(0xFFBE123C), Color(0xFFE11D48))
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            if (uploadSuccessUrl != null) Color(0x6634D399) else Color(0x66FDA4AF),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            if (isUploading) return@clickable
                                            if (uploadSuccessUrl != null) {
                                                try {
                                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uploadSuccessUrl)).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(browserIntent)
                                                } catch (_: Exception) {}
                                                return@clickable
                                            }

                                            isUploading = true
                                            coroutineScope.launch {
                                                val result = if (logFile.exists()) {
                                                    MCLogsUploader.uploadLogFile(logFile)
                                                } else {
                                                    MCLogsUploader.uploadLog(crashInfo.logSnippet)
                                                }
                                                isUploading = false
                                                result.fold(
                                                    onSuccess = { url ->
                                                        uploadSuccessUrl = url
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("mclo.gs link", url))
                                                        Toast.makeText(context, "Log berhasil diunggah & tautan disalin!", Toast.LENGTH_LONG).show()
                                                        try {
                                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            }
                                                            context.startActivity(browserIntent)
                                                        } catch (_: Exception) {}
                                                    },
                                                    onFailure = { err ->
                                                        Toast.makeText(context, "Gagal mengunggah: ${err.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            }
                                        }
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        if (isUploading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(13.dp),
                                                color = Color.White,
                                                strokeWidth = 1.5.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "MENGUNGGAH LOG...",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp
                                            )
                                        } else if (uploadSuccessUrl != null) {
                                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "BUKA TAUTAN MCLO.GS",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp
                                            )
                                        } else {
                                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "UNGGAH TAUTAN LOG (MCLO.GS)",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                // Secondary Actions Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Bagikan
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E1419))
                                            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(8.dp))
                                            .clickable { CrashUtils.shareLogFile(context, logFile) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color(0xFFA1A1AA), modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "BAGIKAN",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    // Salin
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E1419))
                                            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(8.dp))
                                            .clickable {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Crash Log", crashInfo.logSnippet))
                                                Toast.makeText(context, "Log disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFFA1A1AA), modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "SALIN",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    // Tutup
                                    Box(
                                        modifier = Modifier
                                            .weight(0.9f)
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF27272A))
                                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                            .clickable { onDismiss() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "TUTUP",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // RIGHT COLUMN: AI Crash Analyst (Realtime Streaming)
                    // ==========================================
                    val effectiveModel = com.israadev.nuxlauncher.core.crash.AICrashAnalyzer.getEffectiveModel(settings)
                    val activeAiText = when (val s = aiState) {
                        is com.israadev.nuxlauncher.core.crash.AIStreamState.Streaming -> s.fullText
                        is com.israadev.nuxlauncher.core.crash.AIStreamState.Completed -> s.fullText
                        else -> ""
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .clip(cardShape)
                            .background(Color(0xFF080B11))
                            .border(
                                1.dp,
                                Brush.linearGradient(
                                    listOf(
                                        Color(0x3310B981),
                                        Color(0x2238BDF8),
                                        Color(0x1410B981)
                                    )
                                ),
                                cardShape
                            )
                            .padding(8.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Header Bar with Dots, AI Badge & Controls
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(modifier = Modifier.size(7.dp).background(Color(0xFFEF4444), CircleShape))
                                    Box(modifier = Modifier.size(7.dp).background(Color(0xFFF59E0B), CircleShape))
                                    Box(modifier = Modifier.size(7.dp).background(Color(0xFF10B981), CircleShape))
                                    Spacer(modifier = Modifier.width(4.dp))

                                    Text(
                                        text = "AI CRASH ANALYST",
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.8.sp
                                    )

                                    // Status Pill
                                    val (badgeText, badgeBg, badgeColor) = when (aiState) {
                                        is com.israadev.nuxlauncher.core.crash.AIStreamState.Idle -> Triple("STANDBY", Color(0x2294A3B8), Color(0xFF94A3B8))
                                        is com.israadev.nuxlauncher.core.crash.AIStreamState.Connecting -> Triple("CONNECTING...", Color(0x33F59E0B), Color(0xFFFBBF24))
                                        is com.israadev.nuxlauncher.core.crash.AIStreamState.Streaming -> Triple("LIVE STREAMING", Color(0x3310B981), Color(0xFF34D399))
                                        is com.israadev.nuxlauncher.core.crash.AIStreamState.Completed -> Triple("SELESAI", Color(0x2610B981), Color(0xFF4ADE80))
                                        is com.israadev.nuxlauncher.core.crash.AIStreamState.Error -> Triple("ERROR", Color(0x33EF4444), Color(0xFFF87171))
                                        is com.israadev.nuxlauncher.core.crash.AIStreamState.QuotaExceeded -> Triple("LIMIT HABIS", Color(0x33F43F5E), Color(0xFFF43F5E))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(badgeBg)
                                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            color = badgeColor,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    // Quota Badge
                                    val quotaText = if (remainingQuota < 0) "UNLIMITED" else "$remainingQuota/5"
                                    val quotaColor = when {
                                        remainingQuota < 0 -> Color(0xFF38BDF8)
                                        remainingQuota > 1 -> Color(0xFF10B981)
                                        remainingQuota == 1 -> Color(0xFFFBBF24)
                                        else -> Color(0xFFF43F5E)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(quotaColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    ) {
                                        Text(
                                            text = "KUOTA: $quotaText",
                                            color = quotaColor,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                // Quick Actions (Toggle Raw Log, Retry, Copy)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Toggle Raw Log
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF1E293B))
                                            .clickable {
                                                rightViewMode = if (rightViewMode == "ai") "raw" else "ai"
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (rightViewMode == "ai") "LOG MENTAH" else "AI ANALISIS",
                                            color = if (rightViewMode == "ai") Color(0xFF94A3B8) else Color(0xFF38BDF8),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Refresh / Retry AI
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF1E293B))
                                            .clickable { runAiAnalysis() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Analisis Ulang",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }

                                    // Copy AI Analysis / Log
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF1E293B))
                                            .clickable {
                                                val textToCopy = if (rightViewMode == "ai" && activeAiText.isNotBlank()) activeAiText else crashInfo.logSnippet
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cm.setPrimaryClip(ClipData.newPlainText("Crash Analysis", textToCopy))
                                                Toast.makeText(context, "Disalin ke papan klip!", Toast.LENGTH_SHORT).show()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Salin",
                                            tint = Color(0xFFA1A1AA),
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Content Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(innerShape)
                                    .background(Color(0xFF040608))
                                    .border(1.dp, Color(0x1AFFFFFF), innerShape)
                                    .padding(8.dp)
                            ) {
                                if (rightViewMode == "raw") {
                                    // View Raw Terminal Log
                                    val verticalScroll = rememberScrollState()
                                    val horizontalScroll = rememberScrollState()
                                    Text(
                                        text = crashInfo.logSnippet,
                                        color = Color(0xFFCBD5E1),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.5.sp,
                                        lineHeight = 13.5.sp,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(verticalScroll)
                                            .horizontalScroll(horizontalScroll)
                                    )
                                } else {
                                    // View AI Analysis
                                    when (val state = aiState) {
                                        is com.israadev.nuxlauncher.core.crash.AIStreamState.Idle -> {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(if (remainingQuota == 0) Color(0x22F43F5E) else Color(0x2210B981))
                                                        .border(1.dp, if (remainingQuota == 0) Color(0x44F43F5E) else Color(0x4410B981), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(if (remainingQuota == 0) "⏱️" else "⚡", fontSize = 16.sp)
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = if (remainingQuota == 0) "KUOTA HARIAN AI HABIS" else "DIAGNOSA CRASH OTOMATIS",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 11.sp,
                                                    color = Color.White
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = if (remainingQuota == 0)
                                                        "Batas 5 kali penggunaan AI hari ini telah tercapai. Kuota di-reset besok atau salin log ke Discord kami."
                                                    else
                                                        "AI akan menganalisis cuplikan error game untuk mendeteksi penyebab pasti & solusi perbaikan.",
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 9.sp,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                    lineHeight = 12.sp
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                NuxButton(
                                                    onClick = { runAiAnalysis() },
                                                    backgroundColor = if (remainingQuota == 0) Color(0xFFF43F5E) else Color(0xFF10B981),
                                                    contentColor = if (remainingQuota == 0) Color.White else Color.Black,
                                                    cornerRadius = 6.dp,
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(if (remainingQuota == 0) "⚠️" else "✨", fontSize = 10.sp)
                                                        Text(
                                                            text = if (remainingQuota == 0) "LIHAT SOLUSI & TIKET DISCORD" else "MULAI ANALISIS DENGAN AI",
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 9.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        is com.israadev.nuxlauncher.core.crash.AIStreamState.Connecting -> {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(16.dp),
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    color = Color(0xFF38BDF8),
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = "AI sedang menganalisis crash...",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = "Membedah log error dan metadata instance ($effectiveModel)",
                                                    color = Color(0xFF64748B),
                                                    fontSize = 8.5.sp
                                                )
                                            }
                                        }

                                        is com.israadev.nuxlauncher.core.crash.AIStreamState.Streaming -> {
                                            val verticalScroll = rememberScrollState()
                                            LaunchedEffect(state.fullText) {
                                                verticalScroll.animateScrollTo(verticalScroll.maxValue)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .verticalScroll(verticalScroll)
                                            ) {
                                                NuxMarkdownView(
                                                    markdownText = state.fullText,
                                                    isStreaming = true,
                                                    showCursor = isCursorBlinkVisible
                                                )
                                            }
                                        }

                                        is com.israadev.nuxlauncher.core.crash.AIStreamState.Completed -> {
                                            val verticalScroll = rememberScrollState()
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .weight(1f)
                                                        .verticalScroll(verticalScroll)
                                                ) {
                                                    NuxMarkdownView(
                                                        markdownText = state.fullText,
                                                        isStreaming = false,
                                                        showCursor = false
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "• Model: $effectiveModel",
                                                        fontSize = 8.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                    Text(
                                                        text = "Analisis Selesai ✓",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF10B981)
                                                    )
                                                }
                                            }
                                        }

                                        is com.israadev.nuxlauncher.core.crash.AIStreamState.Error -> {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0x33EF4444)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("⚠️", fontSize = 14.sp)
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Gagal Menganalisis Log",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFF87171)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = state.errorMessage,
                                                    color = Color(0xFFCBD5E1),
                                                    fontSize = 8.5.sp,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                    lineHeight = 11.5.sp
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                NuxButton(
                                                    onClick = { runAiAnalysis() },
                                                    backgroundColor = Color(0xFF334155),
                                                    contentColor = Color.White,
                                                    cornerRadius = 6.dp,
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("COBA LAGI", fontWeight = FontWeight.Bold, fontSize = 8.5.sp)
                                                }
                                            }
                                        }

                                        is com.israadev.nuxlauncher.core.crash.AIStreamState.QuotaExceeded -> {
                                            val verticalScroll = rememberScrollState()
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .verticalScroll(verticalScroll)
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0x22F43F5E))
                                                        .border(1.dp, Color(0x55F43F5E), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("⏱️", fontSize = 16.sp)
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "KUOTA HARIAN AI TELAH HABIS",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFF43F5E),
                                                    letterSpacing = 0.5.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Batas penggunaan analisis AI gratis adalah 5 kali per hari. Kuota harian Anda akan di-reset otomatis besok.",
                                                    color = Color(0xFFCBD5E1),
                                                    fontSize = 9.sp,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                    lineHeight = 12.5.sp
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF0F172A))
                                                        .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(8.dp))
                                                        .padding(10.dp)
                                                ) {
                                                    Text(
                                                        text = "Silakan klik tombol SALIN LOG di bawah, lalu buka tiket pengaduan di server Discord resmi kami agar tim developer atau komunitas dapat membantu mendiagnosa penyebab crash game Anda.",
                                                        color = Color(0xFF93C5FD),
                                                        fontSize = 8.5.sp,
                                                        lineHeight = 12.sp,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Tombol Salin Log
                                                    NuxButton(
                                                        onClick = {
                                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                            val textToCopy = if (crashInfo.fullLogPath.isNotBlank() && logFile.exists()) {
                                                                try { logFile.readText() } catch (_: Exception) { crashInfo.logSnippet }
                                                            } else {
                                                                crashInfo.logSnippet
                                                            }
                                                            cm.setPrimaryClip(ClipData.newPlainText("Crash Log", textToCopy))
                                                            Toast.makeText(context, "Log crash berhasil disalin ke papan klip!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        backgroundColor = Color(0xFF334155),
                                                        contentColor = Color.White,
                                                        cornerRadius = 6.dp,
                                                        modifier = Modifier.height(30.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.ContentCopy,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(11.dp),
                                                                tint = Color.White
                                                            )
                                                            Text("SALIN LOG", fontWeight = FontWeight.Bold, fontSize = 8.5.sp)
                                                        }
                                                    }

                                                    // Tombol Open Ticket Discord
                                                    NuxButton(
                                                        onClick = {
                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/UW4wBQg6X5"))
                                                            context.startActivity(intent)
                                                        },
                                                        backgroundColor = Color(0xFF5865F2),
                                                        contentColor = Color.White,
                                                        cornerRadius = 6.dp,
                                                        modifier = Modifier.height(30.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.OpenInNew,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(11.dp),
                                                                tint = Color.White
                                                            )
                                                            Text("OPEN TICKET DISCORD", fontWeight = FontWeight.Black, fontSize = 8.5.sp)
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = "💡 Tips: Anda dapat memasukkan API Key pribadi (OpenRouter / Gemini) di Pengaturan > Integrasi AI untuk penggunaan tanpa batas.",
                                                    fontSize = 7.5.sp,
                                                    color = Color(0xFF64748B),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
    }
}
