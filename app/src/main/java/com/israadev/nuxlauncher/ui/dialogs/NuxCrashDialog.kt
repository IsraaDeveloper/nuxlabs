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
                    // RIGHT COLUMN: Terminal Log Snippet Viewer
                    // ==========================================
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .clip(cardShape)
                            .background(Color(0xFF0A0C10))
                            .border(1.dp, Color(0x26FFFFFF), cardShape)
                            .padding(8.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Terminal Header Bar with Dots
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
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
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "TERMINAL STACKTRACE",
                                        color = Color(0xFF71717A),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.8.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (crashInfo.crashReportPath != null) Color(0xFFF43F5E).copy(alpha = 0.2f) else Color(0xFF27272A))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (crashInfo.crashReportPath != null) "CRASH REPORT" else "LATEST LOG",
                                        color = if (crashInfo.crashReportPath != null) Color(0xFFFDA4AF) else Color(0xFFA1A1AA),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Terminal Content Core
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(innerShape)
                                    .background(Color(0xFF060709))
                                    .border(1.dp, Color(0x14FFFFFF), innerShape)
                                    .padding(8.dp)
                            ) {
                                val verticalScroll = rememberScrollState()
                                val horizontalScroll = rememberScrollState()

                                Text(
                                    text = crashInfo.logSnippet,
                                    color = Color(0xFFE4E4E7),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(verticalScroll)
                                        .horizontalScroll(horizontalScroll)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
