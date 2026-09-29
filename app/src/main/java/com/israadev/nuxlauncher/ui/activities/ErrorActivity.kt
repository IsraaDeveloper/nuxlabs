package com.israadev.nuxlauncher.ui.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.israadev.nuxlauncher.MainActivity
import com.israadev.nuxlauncher.core.crash.CrashManager
import com.israadev.nuxlauncher.core.crash.CrashUtils
import com.israadev.nuxlauncher.core.crash.GameCrashInfo
import com.israadev.nuxlauncher.core.crash.MCLogsUploader
import com.israadev.nuxlauncher.ui.components.NuxBadge
import com.israadev.nuxlauncher.ui.components.NuxButton
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes
import kotlinx.coroutines.launch
import java.io.File

class ErrorActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CRASH_TYPE = "EXTRA_CRASH_TYPE"
        const val EXTRA_EXIT_CODE = "EXTRA_EXIT_CODE"
        const val EXTRA_IS_SIGNAL = "EXTRA_IS_SIGNAL"
        const val EXTRA_LOG_PATH = "EXTRA_LOG_PATH"
        const val EXTRA_INSTANCE_NAME = "EXTRA_INSTANCE_NAME"
        const val EXTRA_MC_VERSION = "EXTRA_MC_VERSION"
        const val EXTRA_THROWABLE_STRING = "EXTRA_THROWABLE_STRING"

        fun showGameCrash(
            context: Context,
            exitCode: Int,
            isSignal: Boolean,
            logPath: String,
            instanceName: String = "Minecraft",
            mcVersion: String = "Unknown"
        ) {
            val intent = Intent(context, ErrorActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_CRASH_TYPE, GameCrashInfo.CRASH_TYPE_GAME)
                putExtra(EXTRA_EXIT_CODE, exitCode)
                putExtra(EXTRA_IS_SIGNAL, isSignal)
                putExtra(EXTRA_LOG_PATH, logPath)
                putExtra(EXTRA_INSTANCE_NAME, instanceName)
                putExtra(EXTRA_MC_VERSION, mcVersion)
            }
            context.startActivity(intent)
        }

        fun showLauncherCrash(
            context: Context,
            throwable: Throwable
        ) {
            val stackTrace = android.util.Log.getStackTraceString(throwable)
            val intent = Intent(context, ErrorActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_CRASH_TYPE, GameCrashInfo.CRASH_TYPE_LAUNCHER)
                putExtra(EXTRA_THROWABLE_STRING, stackTrace)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashType = intent.getStringExtra(EXTRA_CRASH_TYPE) ?: GameCrashInfo.CRASH_TYPE_GAME
        val exitCode = intent.getIntExtra(EXTRA_EXIT_CODE, 1)
        val isSignal = intent.getBooleanExtra(EXTRA_IS_SIGNAL, false)
        val logPath = intent.getStringExtra(EXTRA_LOG_PATH) ?: ""
        val instanceName = intent.getStringExtra(EXTRA_INSTANCE_NAME) ?: "Minecraft"
        val mcVersion = intent.getStringExtra(EXTRA_MC_VERSION) ?: "Unknown"
        val throwableString = intent.getStringExtra(EXTRA_THROWABLE_STRING)

        val logFile = if (logPath.isNotBlank()) File(logPath) else File(filesDir, "latestlog.txt")

        // Memuat konten log untuk ditampilkan
        val logContent = when {
            throwableString != null -> throwableString
            logFile.exists() -> {
                try {
                    val lines = logFile.readLines()
                    if (lines.size > 300) lines.takeLast(300).joinToString("\n")
                    else lines.joinToString("\n")
                } catch (e: Exception) {
                    "Gagal membaca file log: ${e.message}"
                }
            }
            else -> "Tidak ada rincian log yang tersedia."
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NuxColors.Background
                ) {
                    ErrorContent(
                        crashType = crashType,
                        exitCode = exitCode,
                        isSignal = isSignal,
                        instanceName = instanceName,
                        mcVersion = mcVersion,
                        logFile = logFile,
                        logContent = logContent,
                        onRotate = {
                            val current = requestedOrientation
                            requestedOrientation = if (current == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                        },
                        onRestart = {
                            val mainIntent = Intent(this, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                            startActivity(mainIntent)
                            finish()
                        },
                        onExit = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    crashType: String,
    exitCode: Int,
    isSignal: Boolean,
    instanceName: String,
    mcVersion: String,
    logFile: File,
    logContent: String,
    onRotate: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isUploading by remember { mutableStateOf(false) }
    var uploadSuccessUrl by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val isLauncherCrash = crashType == GameCrashInfo.CRASH_TYPE_LAUNCHER
    val exitDetailMessage = remember(isLauncherCrash, isSignal, exitCode) {
        if (isLauncherCrash) {
            "Peluncur mengalami kesalahan internal pada komponen UI atau proses background. Silakan laporkan log stacktrace ini ke tim pengembang NUX."
        } else {
            CrashUtils.getExitMessage(exitCode, isSignal)
        }
    }

    val statusBadge = remember(isLauncherCrash, isSignal, exitCode) {
        when {
            isLauncherCrash -> "LAUNCHER ERROR"
            isSignal -> CrashUtils.getSignalName(exitCode)
            else -> "EXIT CODE $exitCode"
        }
    }

    val mainMessage = remember(isLauncherCrash, isSignal, exitCode) {
        when {
            isLauncherCrash -> "Peluncur mengalami kesalahan tak terduga (Uncaught Exception)."
            isSignal -> "JVM dihentikan karena sinyal fatal $exitCode (${CrashUtils.getSignalName(exitCode)})."
            else -> "JVM keluar dengan kode $exitCode (${CrashUtils.getExitCodeDescription(exitCode)})."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(14.dp)
    ) {
        // ================= TOP HEADER BAR =================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isLauncherCrash) "PELUNCUR MENGALAMI KESALAHAN" else "GAME MENGALAMI CRASH",
                    color = NuxColors.DarkGray,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                NuxBadge(
                    text = statusBadge,
                    backgroundColor = NuxColors.ErrorRed,
                    textColor = Color.White
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Tombol Rotasi Layar (Landscape <-> Portrait)
                IconButton(
                    onClick = onRotate,
                    modifier = Modifier
                        .size(34.dp)
                        .background(NuxColors.SurfaceWhite, NuxSizes.ShapeSmall)
                        .border(1.5.dp, NuxColors.DarkGray, NuxSizes.ShapeSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = "Rotasi Layar",
                        tint = NuxColors.DarkGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Tombol Exit X
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(NuxColors.SurfaceWhite, NuxSizes.ShapeSmall)
                        .border(1.5.dp, NuxColors.DarkGray, NuxSizes.ShapeSmall)
                        .clickable { onExit() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = NuxColors.DarkGray,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ================= MAIN BODY =================
        if (isLandscape) {
            // Layout Horizontal (2 Kolom)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Kolom Kiri: Ringkasan & Tombol Aksi
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CrashDetailCard(
                            instanceName = instanceName,
                            mcVersion = mcVersion,
                            isLauncherCrash = isLauncherCrash,
                            mainMessage = mainMessage,
                            logPath = logFile.absolutePath
                        )

                        DiagnosisCard(diagnosis = exitDetailMessage)

                        EducationNoteCard()
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ActionButtonsGrid(
                        isUploading = isUploading,
                        uploadSuccessUrl = uploadSuccessUrl,
                        onUploadClick = {
                            if (isUploading) return@ActionButtonsGrid
                            isUploading = true
                            coroutineScope.launch {
                                val result = if (logFile.exists()) {
                                    MCLogsUploader.uploadLogFile(logFile)
                                } else {
                                    MCLogsUploader.uploadLog(logContent)
                                }

                                isUploading = false
                                result.fold(
                                    onSuccess = { url ->
                                        uploadSuccessUrl = url
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("mclo.gs link", url))
                                        Toast.makeText(context, "Tautan berhasil diunggah & disalin!", Toast.LENGTH_LONG).show()
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
                        },
                        onShareFileClick = {
                            CrashUtils.shareLogFile(context, logFile)
                        },
                        onCopyLogClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Crash Log", logContent))
                            Toast.makeText(context, "Log berhasil disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        onRestartClick = onRestart,
                        onExitClick = onExit
                    )
                }

                // Kolom Kanan: Log Viewer Monospace
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                ) {
                    TerminalLogViewer(
                        logContent = logContent,
                        logFileName = logFile.name
                    )
                }
            }
        } else {
            // Layout Vertikal (Portret)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CrashDetailCard(
                    instanceName = instanceName,
                    mcVersion = mcVersion,
                    isLauncherCrash = isLauncherCrash,
                    mainMessage = mainMessage,
                    logPath = logFile.absolutePath
                )

                DiagnosisCard(diagnosis = exitDetailMessage)

                // Log viewer mengambil sisa layar di Portrait
                Box(modifier = Modifier.weight(1f)) {
                    TerminalLogViewer(
                        logContent = logContent,
                        logFileName = logFile.name
                    )
                }

                ActionButtonsGrid(
                    isUploading = isUploading,
                    uploadSuccessUrl = uploadSuccessUrl,
                    onUploadClick = {
                        if (isUploading) return@ActionButtonsGrid
                        isUploading = true
                        coroutineScope.launch {
                            val result = if (logFile.exists()) {
                                MCLogsUploader.uploadLogFile(logFile)
                            } else {
                                MCLogsUploader.uploadLog(logContent)
                            }

                            isUploading = false
                            result.fold(
                                onSuccess = { url ->
                                    uploadSuccessUrl = url
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("mclo.gs link", url))
                                    Toast.makeText(context, "Tautan berhasil diunggah & disalin!", Toast.LENGTH_LONG).show()
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
                    },
                    onShareFileClick = {
                        CrashUtils.shareLogFile(context, logFile)
                    },
                    onCopyLogClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Crash Log", logContent))
                        Toast.makeText(context, "Log berhasil disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    onRestartClick = onRestart,
                    onExitClick = onExit
                )
            }
        }
    }
}

@Composable
private fun CrashDetailCard(
    instanceName: String,
    mcVersion: String,
    isLauncherCrash: Boolean,
    mainMessage: String,
    logPath: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NuxColors.SurfaceWhite, NuxSizes.ShapeSmall)
            .border(1.5.dp, NuxColors.DarkGray, NuxSizes.ShapeSmall)
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (isLauncherCrash) "STATUS PELUNCUR" else "INFORMASI SESI GAME",
                color = NuxColors.GrayNeutral,
                fontWeight = FontWeight.Black,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            )
            Text(
                text = if (isLauncherCrash) "Aplikasi NUX Launcher Android" else "$instanceName (Minecraft $mcVersion)",
                color = NuxColors.DarkGray,
                fontWeight = FontWeight.Black,
                fontSize = 12.5.sp
            )
            Text(
                text = mainMessage,
                color = NuxColors.ErrorRed,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp
            )
            if (logPath.isNotBlank()) {
                Text(
                    text = "Lokasi Log: $logPath",
                    color = NuxColors.GrayNeutral,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DiagnosisCard(diagnosis: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NuxColors.ErrorRed.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .border(1.5.dp, NuxColors.ErrorRed, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚠️", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "STATUS PENGHENTIAN PROSES",
                    color = NuxColors.ErrorRed,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = diagnosis,
                color = NuxColors.DarkGray,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EducationNoteCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NuxColors.SurfaceWhite, RoundedCornerShape(8.dp))
            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📄", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CATATAN ANALISIS LOG",
                    color = NuxColors.DarkGray,
                    fontWeight = FontWeight.Black,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = CrashUtils.CRASH_LOG_NOTE,
                color = NuxColors.GrayNeutral,
                fontSize = 9.5.sp,
                lineHeight = 13.5.sp
            )
        }
    }
}

@Composable
private fun ActionButtonsGrid(
    isUploading: Boolean,
    uploadSuccessUrl: String?,
    onUploadClick: () -> Unit,
    onShareFileClick: () -> Unit,
    onCopyLogClick: () -> Unit,
    onRestartClick: () -> Unit,
    onExitClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Tombol Unggah ke mclo.gs
        NuxButton(
            onClick = onUploadClick,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = if (uploadSuccessUrl != null) NuxColors.ForestGreen else Color(0xFF2E7D32),
            contentColor = Color.White
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MENGUNGGAH LOG KE MCLO.GS...",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                } else if (uploadSuccessUrl != null) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BUKA TAUTAN MCLO.GS",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                } else {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "UNGGAH TAUTAN (MCLO.GS)",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Tombol Bagikan File Log Mentah
            NuxButton(
                onClick = onShareFileClick,
                modifier = Modifier.weight(1f),
                backgroundColor = NuxColors.SurfaceWhite,
                contentColor = NuxColors.DarkGray
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "BAGIKAN LOG",
                        fontWeight = FontWeight.Black,
                        fontSize = 10.5.sp
                    )
                }
            }

            // Tombol Salin Teks Log
            NuxButton(
                onClick = onCopyLogClick,
                modifier = Modifier.weight(1f),
                backgroundColor = NuxColors.SurfaceWhite,
                contentColor = NuxColors.DarkGray
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SALIN TEKS",
                        fontWeight = FontWeight.Black,
                        fontSize = 10.5.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Mulai Ulang
            NuxButton(
                onClick = onRestartClick,
                modifier = Modifier.weight(1f),
                backgroundColor = NuxColors.ForestGreen,
                contentColor = Color.White
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "MULAI ULANG",
                        fontWeight = FontWeight.Black,
                        fontSize = 10.5.sp
                    )
                }
            }

            // Tutup
            NuxButton(
                onClick = onExitClick,
                modifier = Modifier.weight(1f),
                backgroundColor = NuxColors.DarkGray,
                contentColor = Color.White
            ) {
                Text(
                    text = "TUTUP",
                    fontWeight = FontWeight.Black,
                    fontSize = 10.5.sp
                )
            }
        }
    }
}

@Composable
private fun TerminalLogViewer(
    logContent: String,
    logFileName: String
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TERMINAL LOG / STACK TRACE",
                color = NuxColors.GrayNeutral,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
            NuxBadge(text = logFileName, backgroundColor = NuxColors.DarkGray, textColor = Color.White)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF141414), NuxSizes.ShapeSmall)
                .border(1.5.dp, NuxColors.DarkGray, NuxSizes.ShapeSmall)
                .padding(8.dp)
        ) {
            val verticalScroll = rememberScrollState()
            val horizontalScroll = rememberScrollState()

            Text(
                text = logContent,
                color = Color(0xFFF1F1F1),
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
