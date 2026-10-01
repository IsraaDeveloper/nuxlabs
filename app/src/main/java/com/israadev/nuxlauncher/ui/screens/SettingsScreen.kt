package com.israadev.nuxlauncher.ui.screens

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.R
import com.israadev.nuxlauncher.GameActivity
import java.io.File
import java.io.FileOutputStream
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.auth.AuthService
import com.israadev.nuxlauncher.core.models.LauncherSettings
import com.israadev.nuxlauncher.core.renderer.NuxRendererRegistry
import com.israadev.nuxlauncher.core.renderer.NuxRendererPluginManager
import com.israadev.nuxlauncher.core.renderer.NuxRendererInfo
import com.israadev.nuxlauncher.core.settings.SettingsManager
import com.israadev.nuxlauncher.core.utils.NuxVersionUtils
import com.israadev.nuxlauncher.ui.components.*
import com.israadev.nuxlauncher.ui.dialogs.NuxRendererV2ConfigDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxAboutDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxPremiumDialog
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenGuiEditor: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentSettings by SettingsManager.settings.collectAsState()
    val launcherUser by AccountManager.launcherUser.collectAsState()

    var activeSettingsTab by remember { mutableStateOf("profile") } // "profile" or "game"

    // Profile state
    var editUsername by remember(launcherUser?.username) { mutableStateOf(launcherUser?.username ?: "") }
    var isSavingUsername by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var selectedPreviewUri by remember { mutableStateOf<Uri?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var premiumInitialPrompt by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && launcherUser != null) {
            selectedPreviewUri = uri
            scope.launch {
                isUploadingAvatar = true
                val uploadRes = AuthService.uploadProfileImage(context, uri)
                uploadRes.onSuccess { cloudUrl ->
                    val updateRes = AuthService.updateProfile(
                        uid = launcherUser!!.uid,
                        photoURL = cloudUrl
                    )
                    updateRes.onSuccess {
                        AccountManager.updateProfile(context, newPhotoUrl = cloudUrl)
                        Toast.makeText(context, "Foto profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(context, "Gagal update profil: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                }.onFailure { err ->
                    Toast.makeText(context, "Gagal upload gambar: ${err.message}", Toast.LENGTH_LONG).show()
                }
                isUploadingAvatar = false
            }
        }
    }

    var heroAnimationEnabled by remember(currentSettings.heroAnimationEnabled) { mutableStateOf(currentSettings.heroAnimationEnabled) }
    var heroAnimationVideoPath by remember(currentSettings.heroAnimationVideoPath) { mutableStateOf(currentSettings.heroAnimationVideoPath) }
    var heroAnimationRotation by remember(currentSettings.heroAnimationRotation) { mutableIntStateOf(currentSettings.heroAnimationRotation) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val durationMs = durationStr?.toLongOrNull() ?: 0L
                    retriever.release()

                    if (durationMs > 20_500L) {
                        val seconds = (durationMs / 1000f).roundToInt()
                        Toast.makeText(context, "Durasi video terlalu panjang ($seconds dtk)! Maksimum 20 detik.", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    // Save 100% locally to private app filesDir (zero cloud)
                    val destFile = File(context.filesDir, "hero_banner.mp4")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    heroAnimationVideoPath = destFile.absolutePath
                    heroAnimationEnabled = true
                    val updated = currentSettings.copy(
                        heroAnimationEnabled = true,
                        heroAnimationVideoPath = destFile.absolutePath
                    )
                    SettingsManager.updateSettings(context, updated)
                    Toast.makeText(context, "Animasi banner berhasil disimpan & diaktifkan!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Gagal memproses video: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun handleSaveUsername() {
        val cleanName = editUsername.trim()
        if (cleanName.isBlank()) {
            usernameError = "Username tidak boleh kosong"
            return
        }
        if (cleanName.length < 3 || cleanName.length > 20) {
            usernameError = "Panjang username harus 3 - 20 karakter"
            return
        }
        if (!cleanName.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            usernameError = "Hanya huruf, angka, dan garis bawah (_)"
            return
        }
        val user = launcherUser ?: return
        val uid = user.uid
        if (cleanName == user.username) {
            usernameError = null
            Toast.makeText(context, "Username tidak berubah.", Toast.LENGTH_SHORT).show()
            return
        }

        usernameError = null
        isSavingUsername = true
        scope.launch {
            val res = AuthService.updateProfile(uid = uid, newUsername = cleanName)
            res.onSuccess {
                AccountManager.updateProfile(context, newUsername = cleanName)
                Toast.makeText(context, "Username berhasil diubah menjadi $cleanName!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                usernameError = err.message ?: "Gagal memperbarui username"
                Toast.makeText(context, err.message ?: "Gagal memperbarui username", Toast.LENGTH_LONG).show()
            }
            isSavingUsername = false
        }
    }

    // Local mutable state for instant responsive UI before saving
    var ramMb by remember(currentSettings.ramMb) { mutableIntStateOf(currentSettings.ramMb) }
    var initialHeapMb by remember(currentSettings.initialHeapMb) { mutableIntStateOf(currentSettings.initialHeapMb) }
    var customJvmArgs by remember(currentSettings.customJvmArgs) { mutableStateOf(currentSettings.customJvmArgs) }
    var defaultRuntime by remember(currentSettings.defaultJavaRuntime) { mutableStateOf(currentSettings.defaultJavaRuntime) }

    var mouseMode by remember(currentSettings.mouseControlMode) { mutableStateOf(currentSettings.mouseControlMode) }
    var cursorSensitivity by remember(currentSettings.cursorSensitivity) { mutableIntStateOf(currentSettings.cursorSensitivity) }
    var captureSensitivity by remember(currentSettings.captureSensitivity) { mutableIntStateOf(currentSettings.captureSensitivity) }
    var mouseSizeDp by remember(currentSettings.mouseSizeDp) { mutableIntStateOf(currentSettings.mouseSizeDp) }

    var resolutionRatio by remember(currentSettings.resolutionRatio) { mutableIntStateOf(currentSettings.resolutionRatio) }
    var autoOptimizeMC by remember(currentSettings.autoOptimizeMinecraft) { mutableStateOf(currentSettings.autoOptimizeMinecraft) }
    var sustainedPerf by remember(currentSettings.sustainedPerformanceMode) { mutableStateOf(currentSettings.sustainedPerformanceMode) }
    var selectedRenderer by remember(currentSettings.selectedRenderer) { mutableStateOf(currentSettings.selectedRenderer) }
    var vulkanDriver by remember(currentSettings.vulkanDriver) { mutableStateOf(currentSettings.vulkanDriver) }
    var graphicsApi by remember(currentSettings.graphicsApi) { mutableStateOf(currentSettings.graphicsApi) }
    var zinkPreferSystemDriver by remember(currentSettings.zinkPreferSystemDriver) { mutableStateOf(currentSettings.zinkPreferSystemDriver) }
    var vsyncInZink by remember(currentSettings.vsyncInZink) { mutableStateOf(currentSettings.vsyncInZink) }
    var showRendererDialog by remember { mutableStateOf(false) }
    var showRendererConfigDialog by remember { mutableStateOf(false) }
    var selectedConfigRenderer by remember { mutableStateOf<NuxRendererInfo?>(null) }
    var showAdrenoWarningDialog by remember { mutableStateOf(false) }

    val totalRamMb = remember { SettingsManager.getTotalDeviceMemoryMb(context) }
    val maxAllocatableRam = remember(totalRamMb) { (totalRamMb * 0.85f).toInt().coerceAtLeast(1024) }
    val cpuCores = remember { SettingsManager.getCpuCoreCount() }

    fun commitSettings() {
        val updated = currentSettings.copy(
            ramMb = ramMb,
            initialHeapMb = initialHeapMb,
            customJvmArgs = customJvmArgs,
            defaultJavaRuntime = defaultRuntime,
            mouseControlMode = mouseMode,
            cursorSensitivity = cursorSensitivity,
            captureSensitivity = captureSensitivity,
            mouseSizeDp = mouseSizeDp,
            resolutionRatio = resolutionRatio,
            autoOptimizeMinecraft = autoOptimizeMC,
            sustainedPerformanceMode = sustainedPerf,
            selectedRenderer = selectedRenderer,
            vulkanDriver = vulkanDriver,
            graphicsApi = graphicsApi,
            zinkPreferSystemDriver = zinkPreferSystemDriver,
            vsyncInZink = vsyncInZink,
            heroAnimationEnabled = heroAnimationEnabled,
            heroAnimationVideoPath = heroAnimationVideoPath,
            heroAnimationRotation = heroAnimationRotation
        )
        SettingsManager.updateSettings(context, updated)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NuxColors.Background)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
    ) {
        // =========================================================================
        // 1. TOP HEADER BAR (Minimalist Cyber-Glass matching Home, Accounts & Mods)
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Back to Dashboard Button (Standard 26.dp)
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NuxColors.SurfaceElevated, RoundedCornerShape(6.dp))
                        .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                        .clickable { onNavigateBack() }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Kembali",
                            tint = NuxColors.ForestGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "DASHBOARD",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Text(
                    text = "SETTINGS",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 0.8.sp
                )

                // Segmented Mode Tabs: [ PROFIL AKUN | PREFERENSI GAME ]
                Row(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NuxColors.SurfaceElevated, RoundedCornerShape(6.dp))
                        .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isProfileTab = activeSettingsTab == "profile"
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isProfileTab) NuxColors.ForestGreen else Color.Transparent
                            )
                            .clickable { activeSettingsTab = "profile" }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccountCircle,
                                contentDescription = null,
                                tint = if (isProfileTab) Color.White else NuxColors.GrayNeutral,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PROFIL AKUN",
                                color = if (isProfileTab) Color.White else NuxColors.GrayNeutral,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp
                            )
                        }
                    }

                    val isGameTab = activeSettingsTab == "game"
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isGameTab) NuxColors.ForestGreen else Color.Transparent
                            )
                            .clickable { activeSettingsTab = "game" }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = null,
                                tint = if (isGameTab) Color.White else NuxColors.GrayNeutral,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PREFERENSI GAME",
                                color = if (isGameTab) Color.White else NuxColors.GrayNeutral,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp
                            )
                        }
                    }

                    val isBannerTab = activeSettingsTab == "banner"
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isBannerTab) NuxColors.ForestGreen else Color.Transparent
                            )
                            .clickable { activeSettingsTab = "banner" }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.VideoLibrary,
                                contentDescription = null,
                                tint = if (isBannerTab) Color.White else NuxColors.GrayNeutral,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ANIMASI BANNER",
                                color = if (isBannerTab) Color.White else NuxColors.GrayNeutral,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp
                            )
                        }
                    }
                }
            }

            // Hardware Specs Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NuxColors.SurfaceElevated, RoundedCornerShape(6.dp))
                        .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${totalRamMb / 1024} GB RAM",
                        color = NuxColors.MintGreen,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NuxColors.SurfaceElevated, RoundedCornerShape(6.dp))
                        .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$cpuCores CORES",
                        color = NuxColors.GrayNeutral,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // =========================================================================
        // 2. MAIN CONTENT PANELS (Profile Tab vs Game Preferences Tab)
        // =========================================================================
        if (activeSettingsTab == "profile") {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // LEFT CARD: FOTO PROFIL & IDENTITAS
                NuxCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    backgroundColor = NuxColors.SurfaceElevated,
                    borderColor = NuxColors.CardBorder,
                    borderWidth = 1.dp,
                    cornerRadius = 8.dp,
                    fillMaxHeight = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccountCircle,
                                contentDescription = null,
                                tint = NuxColors.ForestGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FOTO PROFIL & IDENTITAS",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }

                        // Avatar Display with change button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Avatar Box
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(8.dp))
                            ) {
                                NuxNetworkImage(
                                    model = selectedPreviewUri ?: launcherUser?.photoURL?.takeIf { it.isNotBlank() },
                                    contentDescription = "Avatar Profil",
                                    modifier = Modifier.fillMaxSize(),
                                    fallbackInitials = launcherUser?.username ?: "NUX",
                                    shape = RoundedCornerShape(8.dp)
                                )

                                if (isUploadingAvatar) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }

                            // Upload Button & Hint
                            Column(modifier = Modifier.weight(1f)) {
                                NuxButton(
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp),
                                    backgroundColor = NuxColors.ForestGreen,
                                    contentColor = Color.White,
                                    cornerRadius = 6.dp,
                                    enabled = !isUploadingAvatar
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PhotoCamera,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isUploadingAvatar) "MENGUNGGAH..." else "GANTI FOTO PROFIL",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Pilih gambar dari galeri HP. Otomatis disinkronkan ke cloud & voice room.",
                                    fontSize = 9.5.sp,
                                    color = NuxColors.GrayNeutral,
                                    lineHeight = 12.sp
                                )
                            }
                        }

                        // Identity Specs Card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Email Terdaftar", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NuxColors.GrayNeutral)
                                Text(
                                    text = launcherUser?.email?.ifBlank { "Offline / Guest" } ?: "-",
                                    fontSize = 10.5.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            HorizontalDivider(color = NuxColors.CardBorder, thickness = 0.5.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Status Lisensi", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NuxColors.GrayNeutral)
                                val isVip = launcherUser?.isActivated == true
                                val tierText = if (isVip) {
                                    when (launcherUser?.tier?.lowercase()) {
                                        "monthly" -> "VIP BULANAN"
                                        "yearly" -> "VIP TAHUNAN"
                                        else -> "VIP LIFETIME"
                                    }
                                } else {
                                    "FREE MEMBER"
                                }
                                NuxBadge(
                                    text = tierText,
                                    backgroundColor = if (isVip) Color(0xFFF59E0B).copy(alpha = 0.2f) else NuxColors.SurfaceElevated,
                                    textColor = if (isVip) Color(0xFFFBBF24) else Color(0xFF94A3B8)
                                )
                            }

                            // Tombol Buka Showcase & Upgrade NUX Premium
                            val isUserVip = launcherUser?.isActivated == true
                            NuxButton(
                                onClick = {
                                    premiumInitialPrompt = if (isUserVip) "Status NUX VIP Anda saat ini aktif!" else null
                                    showPremiumDialog = true
                                },
                                backgroundColor = if (isUserVip) Color(0xFF1E1710) else Color(0xFFF59E0B),
                                contentColor = if (isUserVip) Color(0xFFFBBF24) else Color.Black,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(if (isUserVip) "👑" else "★", fontSize = 11.sp)
                                    Text(
                                        text = if (isUserVip) "STATUS VIP & KELOLA KEY" else "UPGRADE KE NUX PREMIUM",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            HorizontalDivider(color = NuxColors.CardBorder, thickness = 0.5.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Platform Akun", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NuxColors.GrayNeutral)
                                NuxBadge(
                                    text = "ANDROID MOBILE",
                                    backgroundColor = NuxColors.SurfaceElevated,
                                    textColor = Color.White
                                )
                            }

                            HorizontalDivider(color = NuxColors.CardBorder, thickness = 0.5.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Akun UID", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NuxColors.GrayNeutral)
                                Text(
                                    text = launcherUser?.uid?.take(16) ?: "-",
                                    fontSize = 9.5.sp,
                                    color = NuxColors.GrayNeutral
                                )
                            }

                            HorizontalDivider(color = NuxColors.CardBorder, thickness = 0.5.dp)

                            // Tombol Dialog Tentang & Lisensi Open Source (GPL-3.0 & Zalith Compliance)
                            NuxButton(
                                onClick = { showAboutDialog = true },
                                backgroundColor = NuxColors.SurfaceInput,
                                contentColor = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = NuxColors.ForestGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "TENTANG & LISENSI OPEN SOURCE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // RIGHT CARD: GANTI USERNAME & MANAJEMEN AKUN
                NuxCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    backgroundColor = NuxColors.SurfaceElevated,
                    borderColor = NuxColors.CardBorder,
                    borderWidth = 1.dp,
                    cornerRadius = 8.dp,
                    fillMaxHeight = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Badge,
                                contentDescription = null,
                                tint = NuxColors.ForestGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GANTI USERNAME NUX",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "Ubah Display Name Anda. Username berlaku unik global dan digunakan untuk nama in-game Minecraft serta obrolan voice room.",
                            fontSize = 9.5.sp,
                            color = NuxColors.GrayNeutral,
                            lineHeight = 13.sp
                        )

                        // Form Ganti Username
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Username Baru",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NuxColors.GrayNeutral
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            NuxTextField(
                                value = editUsername,
                                onValueChange = {
                                    editUsername = it
                                    usernameError = null
                                },
                                placeholder = "Contoh: AlexGamer99"
                            )

                            if (usernameError != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = usernameError!!,
                                    color = NuxColors.Coral,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "3 - 20 karakter alfanumerik & garis bawah (_).",
                                fontSize = 9.sp,
                                color = NuxColors.GrayNeutral
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            NuxButton(
                                onClick = { handleSaveUsername() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp),
                                backgroundColor = NuxColors.ForestGreen,
                                contentColor = Color.White,
                                cornerRadius = 6.dp,
                                enabled = !isSavingUsername
                            ) {
                                if (isSavingUsername) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("MENYIMPAN...", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White)
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SIMPAN USERNAME", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }

                        HorizontalDivider(color = NuxColors.CardBorder, thickness = 0.5.dp)

                        // Logout & Session Management
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "MANAJEMEN SESI AKUN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Keluar dari sesi akun saat ini di HP ini.",
                                fontSize = 9.5.sp,
                                color = NuxColors.GrayNeutral
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            NuxButton(
                                onClick = { showLogoutDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp),
                                backgroundColor = NuxColors.Coral.copy(alpha = 0.15f),
                                contentColor = NuxColors.Coral,
                                borderColor = NuxColors.Coral.copy(alpha = 0.35f),
                                cornerRadius = 6.dp
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                                    contentDescription = null,
                                    tint = NuxColors.Coral,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("KELUAR DARI AKUN (LOGOUT)", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = NuxColors.Coral)
                            }
                        }
                    }
                }
            }
        } else if (activeSettingsTab == "game") {
            // =====================================================================
            // GAME PREFERENCES TAB (Memory/Java/Graphics + Controls/Mouse)
            // =====================================================================
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // LEFT COLUMN: Memory & Java + Graphics
                NuxCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    backgroundColor = NuxColors.SurfaceElevated,
                    borderColor = NuxColors.CardBorder,
                    borderWidth = 1.dp,
                    cornerRadius = 8.dp,
                    fillMaxHeight = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Section 1: Java Memory (RAM)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Memory,
                                contentDescription = null,
                                tint = NuxColors.ForestGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ALOKASI RAM & JVM MEMORY",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }

                        // RAM Slider & Value Display
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Maksimum RAM (-Xmx)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (ramMb > (totalRamMb * 0.75f)) NuxColors.Coral.copy(alpha = 0.2f) else NuxColors.ForestGreen.copy(alpha = 0.2f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (ramMb > (totalRamMb * 0.75f)) NuxColors.Coral.copy(alpha = 0.4f) else NuxColors.MintGreen.copy(alpha = 0.3f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$ramMb MB (${String.format("%.1f", ramMb / 1024f)} GB)",
                                        color = if (ramMb > (totalRamMb * 0.75f)) NuxColors.Coral else NuxColors.MintGreen,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Slider(
                                value = ramMb.toFloat(),
                                onValueChange = {
                                    ramMb = ((it / 128).roundToInt() * 128).coerceIn(512, maxAllocatableRam)
                                    commitSettings()
                                },
                                valueRange = 512f..maxAllocatableRam.toFloat(),
                                steps = ((maxAllocatableRam - 512) / 128) - 1,
                                colors = SliderDefaults.colors(
                                    thumbColor = NuxColors.MintGreen,
                                    activeTrackColor = NuxColors.ForestGreen,
                                    inactiveTrackColor = NuxColors.CardBorder
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Quick RAM Presets
                            Text(
                                text = "Preset Cepat:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NuxColors.GrayNeutral
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(1024, 1536, 2048, 3072, 4096).forEach { preset ->
                                    if (preset <= maxAllocatableRam) {
                                        val isSelected = ramMb == preset
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(24.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (isSelected) NuxColors.ForestGreen else NuxColors.SurfaceElevated,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) NuxColors.MintGreen else NuxColors.CardBorder,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .clickable {
                                                    ramMb = preset
                                                    commitSettings()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (preset >= 1024) "${preset / 1024}G" else "${preset}M",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else NuxColors.GrayNeutral
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Custom JVM Arguments
                        Column {
                            Text(
                                text = "Custom JVM Arguments (Opsional)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NuxColors.GrayNeutral
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            NuxTextField(
                                value = customJvmArgs,
                                onValueChange = {
                                    customJvmArgs = it
                                    commitSettings()
                                },
                                placeholder = "Contoh: -XX:+UseG1GC -Dminecraft.applet.TargetDirectory=..."
                            )
                        }

                        HorizontalDivider(color = NuxColors.CardBorder, thickness = 0.5.dp)

                        // Section 2: Graphics & Rendering
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = null,
                                tint = NuxColors.ForestGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GRAFIK & OPTIMASI MINECRAFT",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }

                        // Renderer Selection Card
                        val activeRendererInfo = remember(selectedRenderer) {
                            NuxRendererRegistry.findRendererById(selectedRenderer)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(NuxColors.SurfaceInput)
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            // Top Row: Section label & GANTI button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Renderer Grafik (Backend)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (activeRendererInfo.isConfigurable) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(NuxColors.SurfaceElevated)
                                                .border(1.dp, NuxColors.MintGreen.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                .clickable {
                                                    selectedConfigRenderer = activeRendererInfo
                                                    showRendererConfigDialog = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Konfigurasi Renderer",
                                                tint = NuxColors.MintGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .height(24.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NuxColors.SurfaceElevated)
                                            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(4.dp))
                                            .clickable {
                                                NuxRendererPluginManager.scanPlugins(context)
                                                showRendererDialog = true
                                            }
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "GANTI",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NuxColors.MintGreen
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Active Renderer Name & Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = activeRendererInfo.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NuxColors.MintGreen
                                )
                                Box(
                                    modifier = Modifier
                                        .background(NuxColors.ForestGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .border(1.dp, NuxColors.MintGreen.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = activeRendererInfo.badge,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NuxColors.MintGreen,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = activeRendererInfo.summary,
                                fontSize = 9.5.sp,
                                color = NuxColors.GrayNeutral,
                                lineHeight = 13.sp
                            )
                        }

                        // Auto Optimize options.txt Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Optimize options.txt",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Otomatis optimasi setting visual (Clouds, Shadows) untuk FPS maksimal",
                                    fontSize = 9.5.sp,
                                    color = NuxColors.GrayNeutral
                                )
                            }
                            Switch(
                                checked = autoOptimizeMC,
                                onCheckedChange = {
                                    autoOptimizeMC = it
                                    commitSettings()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NuxColors.ForestGreen,
                                    uncheckedTrackColor = NuxColors.SurfaceElevated
                                )
                            )
                        }

                        // Resolution Scaling
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Skala Resolusi Layar",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "$resolutionRatio%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NuxColors.MintGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(50, 75, 100, 125).forEach { ratio ->
                                    val isSelected = resolutionRatio == ratio
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(24.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (isSelected) NuxColors.ForestGreen else NuxColors.SurfaceElevated,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) NuxColors.MintGreen else NuxColors.CardBorder,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .clickable {
                                                resolutionRatio = ratio
                                                commitSettings()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$ratio%",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else NuxColors.GrayNeutral
                                        )
                                    }
                                }
                            }
                        }

                        // Driver Vulkan Selector
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Driver Vulkan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Pilih implementasi driver Vulkan yang digunakan sistem/game",
                                fontSize = 9.5.sp,
                                color = NuxColors.GrayNeutral
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("auto" to "Auto", "system" to "Sistem", "turnip" to "Turnip").forEach { (id, label) ->
                                    val isSelected = vulkanDriver == id
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(24.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (isSelected) NuxColors.ForestGreen else NuxColors.SurfaceElevated,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) NuxColors.MintGreen else NuxColors.CardBorder,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .clickable {
                                                vulkanDriver = id
                                                commitSettings()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else NuxColors.GrayNeutral
                                        )
                                    }
                                }
                            }
                        }

                        // API Grafis Selector
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "API Grafis",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Atur API grafis yang digunakan oleh Minecraft modern 26.2+",
                                fontSize = 9.5.sp,
                                color = NuxColors.GrayNeutral
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("DEFAULT" to "Default", "OPENGL" to "OpenGL", "VULKAN" to "Vulkan").forEach { (id, label) ->
                                    val isSelected = graphicsApi == id
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(24.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (isSelected) NuxColors.ForestGreen else NuxColors.SurfaceElevated,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) NuxColors.MintGreen else NuxColors.CardBorder,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .clickable {
                                                graphicsApi = id
                                                commitSettings()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else NuxColors.GrayNeutral
                                        )
                                    }
                                }
                            }
                        }

                        // Gunakan Driver Vulkan Sistem (Zink)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gunakan Driver Vulkan Sistem",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Paksa menggunakan driver Vulkan bawaan sistem HP (bukan Turnip). Mempengaruhi perender Zink.",
                                    fontSize = 9.5.sp,
                                    color = NuxColors.GrayNeutral
                                )
                            }
                            Switch(
                                checked = zinkPreferSystemDriver,
                                onCheckedChange = { checked ->
                                    if (checked && NuxVersionUtils.isAdrenoGPU()) {
                                        showAdrenoWarningDialog = true
                                    } else {
                                        zinkPreferSystemDriver = checked
                                        commitSettings()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NuxColors.ForestGreen,
                                    uncheckedTrackColor = NuxColors.SurfaceElevated
                                )
                            )
                        }

                        // Sinkronisasi Vertikal Zink (V-Sync)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sinkronisasi Vertikal Zink",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Aktifkan sinkronisasi vertikal (V-Sync) untuk perender Kopper Zink via antarmuka sistem.",
                                    fontSize = 9.5.sp,
                                    color = NuxColors.GrayNeutral
                                )
                            }
                            Switch(
                                checked = vsyncInZink,
                                onCheckedChange = {
                                    vsyncInZink = it
                                    commitSettings()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NuxColors.ForestGreen,
                                    uncheckedTrackColor = NuxColors.SurfaceElevated
                                )
                            )
                        }
                    }
                }

                // RIGHT COLUMN: In-Game Mouse & Touch Controls
                NuxCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    backgroundColor = NuxColors.SurfaceElevated,
                    borderColor = NuxColors.CardBorder,
                    borderWidth = 1.dp,
                    cornerRadius = 8.dp,
                    fillMaxHeight = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header for Mouse settings
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Mouse,
                                contentDescription = null,
                                tint = NuxColors.ForestGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "KONTROL MOUSE & SENTUHAN IN-GAME",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "Pengaturan mode mouse untuk menu Minecraft (Inventory, Pause Menu, Chat, Crafting).",
                            fontSize = 9.5.sp,
                            color = NuxColors.GrayNeutral
                        )

                        // CUSTOM CONTROLS / GUI EDITOR ACTION CARD
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Tune,
                                        contentDescription = null,
                                        tint = NuxColors.ForestGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "KUSTOMISASI KONTROL VIRTUAL",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ubah tata letak tombol, ukuran, transparansi, scroll hotbar, dan mapping input key Minecraft.",
                                    fontSize = 9.5.sp,
                                    color = NuxColors.GrayNeutral,
                                    lineHeight = 12.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                NuxButton(
                                    onClick = onOpenGuiEditor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp),
                                    backgroundColor = NuxColors.ForestGreen,
                                    contentColor = Color.White,
                                    cornerRadius = 6.dp
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("BUKA GUI EDITOR", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }

                        // Mouse Control Mode Selection (SLIDE vs CLICK)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Trackpad / Slide Card
                            val isSlide = mouseMode == "SLIDE"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSlide) NuxColors.ForestGreen.copy(alpha = 0.15f) else NuxColors.SurfaceInput,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSlide) NuxColors.MintGreen else NuxColors.CardBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        mouseMode = "SLIDE"
                                        commitSettings()
                                    }
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.TouchApp,
                                            contentDescription = null,
                                            tint = if (isSlide) NuxColors.MintGreen else Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "SLIDE (Trackpad)",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSlide) NuxColors.MintGreen else Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Geser jari untuk kursor virtual, ketuk layar untuk klik item.",
                                        fontSize = 9.sp,
                                        color = NuxColors.GrayNeutral,
                                        lineHeight = 11.sp
                                    )
                                }
                            }

                            // Direct Touch / Click Card
                            val isClick = mouseMode == "CLICK"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isClick) NuxColors.ForestGreen.copy(alpha = 0.15f) else NuxColors.SurfaceInput,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isClick) NuxColors.MintGreen else NuxColors.CardBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        mouseMode = "CLICK"
                                        commitSettings()
                                    }
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.NearMe,
                                            contentDescription = null,
                                            tint = if (isClick) NuxColors.MintGreen else Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "CLICK (Direct)",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isClick) NuxColors.MintGreen else Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Sentuh tombol atau slot menu langsung di mana jari menekan.",
                                        fontSize = 9.sp,
                                        color = NuxColors.GrayNeutral,
                                        lineHeight = 11.sp
                                    )
                                }
                            }
                        }

                        // In-Game Camera Look Sensitivity
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sensitivitas Kamera Game (Rotasi)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .background(NuxColors.ForestGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .border(1.dp, NuxColors.MintGreen.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$captureSensitivity%",
                                        color = NuxColors.MintGreen,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Slider(
                                value = captureSensitivity.toFloat(),
                                onValueChange = {
                                    captureSensitivity = it.roundToInt()
                                    commitSettings()
                                },
                                valueRange = 50f..300f,
                                steps = 25,
                                colors = SliderDefaults.colors(
                                    thumbColor = NuxColors.MintGreen,
                                    activeTrackColor = NuxColors.ForestGreen,
                                    inactiveTrackColor = NuxColors.CardBorder
                                )
                            )
                        }

                        // Virtual Cursor Sensitivity
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sensitivitas Kursor Virtual (Menu)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .background(NuxColors.ForestGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .border(1.dp, NuxColors.MintGreen.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$cursorSensitivity%",
                                        color = NuxColors.MintGreen,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Slider(
                                value = cursorSensitivity.toFloat(),
                                onValueChange = {
                                    cursorSensitivity = it.roundToInt()
                                    commitSettings()
                                },
                                valueRange = 50f..300f,
                                steps = 25,
                                colors = SliderDefaults.colors(
                                    thumbColor = NuxColors.MintGreen,
                                    activeTrackColor = NuxColors.ForestGreen,
                                    inactiveTrackColor = NuxColors.CardBorder
                                )
                            )
                        }

                        // Virtual Cursor Size with Preview
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ukuran Kursor Virtual",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${mouseSizeDp}dp",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NuxColors.MintGreen
                                )
                            }
                            Slider(
                                value = mouseSizeDp.toFloat(),
                                onValueChange = {
                                    mouseSizeDp = it.roundToInt()
                                    commitSettings()
                                },
                                valueRange = 16f..48f,
                                steps = 16,
                                colors = SliderDefaults.colors(
                                    thumbColor = NuxColors.MintGreen,
                                    activeTrackColor = NuxColors.ForestGreen,
                                    inactiveTrackColor = NuxColors.CardBorder
                                )
                            )
                        }

                        // Device Info & Reset Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            NuxButton(
                                onClick = {
                                    SettingsManager.resetToDefaults(context)
                                    val reset = SettingsManager.settings.value
                                    ramMb = reset.ramMb
                                    initialHeapMb = reset.initialHeapMb
                                    customJvmArgs = reset.customJvmArgs
                                    defaultRuntime = reset.defaultJavaRuntime
                                    mouseMode = reset.mouseControlMode
                                    cursorSensitivity = reset.cursorSensitivity
                                    captureSensitivity = reset.captureSensitivity
                                    mouseSizeDp = reset.mouseSizeDp
                                    resolutionRatio = reset.resolutionRatio
                                    autoOptimizeMC = reset.autoOptimizeMinecraft
                                    sustainedPerf = reset.sustainedPerformanceMode
                                    heroAnimationEnabled = reset.heroAnimationEnabled
                                    heroAnimationVideoPath = reset.heroAnimationVideoPath
                                    heroAnimationRotation = reset.heroAnimationRotation
                                    Toast.makeText(context, "Pengaturan di-reset ke default!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                backgroundColor = NuxColors.Coral.copy(alpha = 0.15f),
                                contentColor = NuxColors.Coral,
                                borderColor = NuxColors.Coral.copy(alpha = 0.35f),
                                cornerRadius = 6.dp
                            ) {
                                Text(
                                    text = "RESET DEFAULT",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    color = NuxColors.Coral
                                )
                            }

                            NuxButton(
                                onClick = {
                                    commitSettings()
                                    Toast.makeText(context, "Semua pengaturan tersimpan!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                backgroundColor = NuxColors.ForestGreen,
                                contentColor = Color.White,
                                cornerRadius = 6.dp
                            ) {
                                Text(
                                    text = "SIMPAN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // =====================================================================
            // ANIMASI BANNER BOX TAB (Hero Video MP4 + Live Interactive Preview)
            // =====================================================================
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // LEFT CARD: Pengaturan & Upload Video
                NuxCard(
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxHeight(),
                    backgroundColor = NuxColors.SurfaceElevated,
                    borderColor = NuxColors.CardBorder,
                    borderWidth = 1.dp,
                    cornerRadius = 8.dp,
                    fillMaxHeight = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.VideoLibrary,
                                contentDescription = null,
                                tint = NuxColors.ForestGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ANIMASI BANNER HERO",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.5.sp,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "Ganti background banner Hero di Dashboard dengan video MP4 animasi berulang (looping). Jika dinonaktifkan, launcher akan kembali memakai gambar flat.",
                            fontSize = 9.sp,
                            color = NuxColors.GrayNeutral,
                            lineHeight = 12.sp
                        )

                        // 1. Switch Aktifkan Animasi
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Aktifkan Animasi Banner",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (heroAnimationEnabled) "Status: Aktif (Memutar video MP4)" else "Status: Nonaktif (Gambar Flat)",
                                    fontSize = 8.5.sp,
                                    color = if (heroAnimationEnabled) NuxColors.MintGreen else NuxColors.GrayNeutral
                                )
                            }
                            Switch(
                                checked = heroAnimationEnabled,
                                onCheckedChange = { checked ->
                                    if (checked && launcherUser?.isActivated != true) {
                                        premiumInitialPrompt = "Kustomisasi Video Hero Banner MP4 hanya tersedia untuk member NUX Premium."
                                        showPremiumDialog = true
                                        return@Switch
                                    }
                                    val videoFile = File(context.filesDir, "hero_banner.mp4")
                                    if (checked && (!videoFile.exists() || heroAnimationVideoPath.isBlank())) {
                                        Toast.makeText(context, "Silakan pilih video MP4 terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                        videoPickerLauncher.launch("video/mp4")
                                        return@Switch
                                    }
                                    heroAnimationEnabled = checked
                                    commitSettings()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NuxColors.ForestGreen,
                                    uncheckedTrackColor = NuxColors.SurfaceElevated
                                )
                            )
                        }

                        // 2. Rotasi / Orientasi Video
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Rotasi Video",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${heroAnimationRotation}°",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NuxColors.MintGreen
                                )
                            }
                            Text(
                                text = "Sesuaikan orientasi jika video terbalik atau vertikal:",
                                fontSize = 8.sp,
                                color = NuxColors.GrayNeutral
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val rotationAngles = listOf(0, 90, 180, 270)
                                rotationAngles.forEach { angle ->
                                    val isSelected = heroAnimationRotation == angle
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (isSelected) NuxColors.ForestGreen else NuxColors.SurfaceElevated
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) NuxColors.MintGreen else NuxColors.CardBorder,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .clickable {
                                                heroAnimationRotation = angle
                                                commitSettings()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$angle°",
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else NuxColors.GrayNeutral
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Tombol Pilih / Ganti / Hapus Video
                        val hasVideo = heroAnimationVideoPath.isNotBlank() && File(heroAnimationVideoPath).exists()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            NuxButton(
                                onClick = {
                                    if (launcherUser?.isActivated != true) {
                                        premiumInitialPrompt = "Kustomisasi Video Hero Banner MP4 hanya tersedia untuk member NUX Premium."
                                        showPremiumDialog = true
                                    } else {
                                        videoPickerLauncher.launch("video/mp4")
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp),
                                backgroundColor = NuxColors.ForestGreen,
                                contentColor = Color.White,
                                cornerRadius = 6.dp
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FileUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (hasVideo) "GANTI VIDEO MP4" else "PILIH VIDEO DARI HP",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp
                                )
                            }

                            if (hasVideo) {
                                NuxButton(
                                    onClick = {
                                        try {
                                            val file = File(heroAnimationVideoPath)
                                            if (file.exists()) file.delete()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        heroAnimationVideoPath = ""
                                        heroAnimationEnabled = false
                                        commitSettings()
                                        Toast.makeText(context, "Animasi banner dihapus, kembali ke gambar flat.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(0.6f)
                                        .height(30.dp),
                                    backgroundColor = NuxColors.Coral.copy(alpha = 0.15f),
                                    contentColor = NuxColors.Coral,
                                    borderColor = NuxColors.Coral.copy(alpha = 0.35f),
                                    cornerRadius = 6.dp
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = NuxColors.Coral
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("HAPUS", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = NuxColors.Coral)
                                }
                            }
                        }
                    }
                }

                // RIGHT CARD: Live Interactive Preview
                NuxCard(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight(),
                    backgroundColor = NuxColors.SurfaceElevated,
                    borderColor = NuxColors.CardBorder,
                    borderWidth = 1.dp,
                    cornerRadius = 8.dp,
                    fillMaxHeight = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    tint = NuxColors.ForestGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "PRATINJAU BANNER",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.5.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }

                            val isPreviewVideoActive = heroAnimationEnabled &&
                                    heroAnimationVideoPath.isNotBlank() &&
                                    File(heroAnimationVideoPath).exists()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isPreviewVideoActive) NuxColors.ForestGreen.copy(alpha = 0.2f) else NuxColors.SurfaceInput,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isPreviewVideoActive) NuxColors.MintGreen.copy(alpha = 0.4f) else NuxColors.CardBorder,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isPreviewVideoActive) "• VIDEO AKTIF" else "• GAMBAR FLAT",
                                    color = if (isPreviewVideoActive) NuxColors.MintGreen else NuxColors.GrayNeutral,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Text(
                            text = "Pratinjau real-time tampilan banner di Dashboard utama:",
                            fontSize = 8.5.sp,
                            color = NuxColors.GrayNeutral
                        )

                        // Box Mockup (True Replica of Dashboard Hero Banner)
                        val mockupShape = RoundedCornerShape(16.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(175.dp)
                                .clip(mockupShape)
                                .background(Color(0xFF0D0F14), mockupShape)
                                .border(1.dp, NuxColors.CardBorder, mockupShape)
                        ) {
                            val isPreviewVideoActive = heroAnimationEnabled &&
                                    heroAnimationVideoPath.isNotBlank() &&
                                    File(heroAnimationVideoPath).exists()

                            if (isPreviewVideoActive) {
                                HeroBannerVideoPlayer(
                                    videoPath = heroAnimationVideoPath,
                                    rotationDegrees = heroAnimationRotation,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.mc_hero_bg),
                                    contentDescription = "Minecraft Scenery",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    alpha = 0.35f
                                )
                            }

                            // Dark gradient overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xF509090B),
                                                Color(0xDC0D0F14),
                                                Color(0x550D0F14)
                                            )
                                        )
                                    )
                                    .padding(horizontal = 18.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        // • FABRIC EDITION pill badge
                                        Row(
                                            modifier = Modifier
                                                .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(NuxColors.ForestGreen, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "FABRIC EDITION",
                                                color = NuxColors.MintGreen,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.8.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(5.dp))

                                        // Large instance title
                                        Text(
                                            text = "MINECRAFT 1.21.4",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp,
                                            letterSpacing = (-0.5).sp,
                                            maxLines = 1
                                        )

                                        Spacer(modifier = Modifier.height(3.dp))

                                        // Emerald gradient accent line
                                        Box(
                                            modifier = Modifier
                                                .size(width = 36.dp, height = 3.dp)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(NuxColors.ForestGreen, NuxColors.MintGreen)
                                                    ),
                                                    CircleShape
                                                )
                                        )

                                        Spacer(modifier = Modifier.height(5.dp))

                                        // Subtitle
                                        Text(
                                            text = "Version 1.21.4 — Click PLAY to launch this instance and craft seamlessly.",
                                            color = Color(0xFFA1A1AA),
                                            fontSize = 10.sp,
                                            lineHeight = 13.sp,
                                            maxLines = 2
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Minimalist Cyber-Glass Play Button (Matching Dashboard)
                                    val playBtnShape = RoundedCornerShape(14.dp)
                                    Row(
                                        modifier = Modifier
                                            .clip(playBtnShape)
                                            .background(Color(0xFF181B22), playBtnShape)
                                            .border(
                                                width = 1.dp,
                                                color = NuxColors.ForestGreen.copy(alpha = 0.5f),
                                                shape = playBtnShape
                                            )
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(NuxColors.ForestGreen),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.PlayArrow,
                                                contentDescription = "Action",
                                                tint = Color(0xFF09090B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Text(
                                            text = "PLAY",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = {
                    Text(
                        text = "Keluar dari Akun?",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        text = "Anda akan keluar dari sesi akun ${launcherUser?.username ?: ""}. Anda dapat login kembali kapan saja.",
                        color = NuxColors.GrayNeutral,
                        fontSize = 11.sp
                    )
                },
                confirmButton = {
                    NuxButton(
                        onClick = {
                            showLogoutDialog = false
                            AccountManager.logout(context)
                            onNavigateBack()
                            Toast.makeText(context, "Berhasil logout", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.height(32.dp),
                        backgroundColor = NuxColors.Coral,
                        contentColor = Color.White,
                        cornerRadius = 6.dp
                    ) {
                        Text("YA, KELUAR", fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                    }
                },
                dismissButton = {
                    NuxButton(
                        onClick = { showLogoutDialog = false },
                        modifier = Modifier.height(32.dp),
                        backgroundColor = NuxColors.SurfaceInput,
                        contentColor = NuxColors.GrayNeutral,
                        cornerRadius = 6.dp
                    ) {
                        Text("BATAL", fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                    }
                },
                containerColor = NuxColors.SurfaceElevated,
                shape = RoundedCornerShape(8.dp)
            )
        }

        // Renderer Selection Dialog (Landscape Optimized with NuxDialog)
        if (showRendererDialog) {
            LaunchedEffect(Unit) {
                NuxRendererPluginManager.scanPlugins(context)
            }
            NuxDialog(
                onDismissRequest = { showRendererDialog = false },
                modifier = Modifier.fillMaxWidth(0.72f),
                fillMaxHeight = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Layers,
                                contentDescription = null,
                                tint = NuxColors.ForestGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "PILIH RENDERER GRAFIK",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Sesuaikan backend grafis dengan Minecraft & GPU perangkat",
                                    fontSize = 9.5.sp,
                                    color = NuxColors.GrayNeutral
                                )
                            }
                        }
                        IconButton(
                            onClick = { showRendererDialog = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Scrollable Renderer List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(NuxRendererRegistry.availableRenderers) { rendererItem ->
                            val isSelected = selectedRenderer.equals(rendererItem.id, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) NuxColors.ForestGreen.copy(alpha = 0.15f) else NuxColors.SurfaceInput
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) NuxColors.MintGreen else NuxColors.CardBorder,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        selectedRenderer = rendererItem.id
                                        commitSettings()
                                        showRendererDialog = false
                                        Toast.makeText(context, "Renderer diubah ke: ${rendererItem.displayName}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = {
                                                    selectedRenderer = rendererItem.id
                                                    commitSettings()
                                                    showRendererDialog = false
                                                    Toast.makeText(context, "Renderer diubah ke: ${rendererItem.displayName}", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = NuxColors.ForestGreen,
                                                    unselectedColor = NuxColors.CardBorder
                                                ),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = rendererItem.displayName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (isSelected) NuxColors.MintGreen else Color.White
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (rendererItem.isConfigurable) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(NuxColors.SurfaceElevated)
                                                        .border(1.dp, NuxColors.MintGreen.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                        .clickable {
                                                            selectedConfigRenderer = rendererItem
                                                            showRendererDialog = false
                                                            showRendererConfigDialog = true
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Settings,
                                                        contentDescription = "Konfigurasi ${rendererItem.displayName}",
                                                        tint = NuxColors.MintGreen,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (isSelected) NuxColors.ForestGreen else NuxColors.SurfaceElevated,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = rendererItem.badge,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else NuxColors.GrayNeutral
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = rendererItem.summary,
                                        fontSize = 9.5.sp,
                                        color = NuxColors.GrayNeutral,
                                        lineHeight = 12.sp,
                                        modifier = Modifier.padding(start = 24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Kesesuaian: ${rendererItem.compatibility}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NuxColors.MintGreen,
                                        modifier = Modifier.padding(start = 24.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        NuxButton(
                            onClick = { showRendererDialog = false },
                            modifier = Modifier.height(30.dp),
                            backgroundColor = NuxColors.SurfaceInput,
                            contentColor = Color.White,
                            cornerRadius = 6.dp
                        ) {
                            Text("TUTUP", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    // Adreno GPU Warning Dialog (Persis seperti Zalith)
    if (showAdrenoWarningDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAdrenoWarningDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                NuxCard(
                    modifier = Modifier
                        .width(420.dp)
                        .wrapContentHeight(),
                    backgroundColor = NuxColors.SurfaceElevated,
                    borderColor = Color(0x33FFFFFF),
                    cornerRadius = NuxSizes.CornerRadiusLarge,
                    fillMaxHeight = false
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PERINGATAN DRIVER ADRENO",
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "Launcher mendeteksi perangkatmu menggunakan GPU Adreno. Mengaktifkan opsi ini dapat menyebabkan perender Zink tidak berfungsi atau crash. Apakah Anda yakin ingin mengaktifkannya?",
                            color = Color(0xFFD4D4D8),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                NuxButton(
                                    onClick = {
                                        zinkPreferSystemDriver = false
                                        commitSettings()
                                        showAdrenoWarningDialog = false
                                    },
                                    backgroundColor = NuxColors.SurfaceInput,
                                    contentColor = Color.White,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "BATAL",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                NuxButton(
                                    onClick = {
                                        zinkPreferSystemDriver = true
                                        commitSettings()
                                        showAdrenoWarningDialog = false
                                    },
                                    backgroundColor = Color(0xFFF59E0B),
                                    contentColor = Color.Black,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "AKTIFKAN",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Konfigurasi Renderer V2 (persis Zalith untuk MobileGlues, NGG dll)
    if (showRendererConfigDialog && selectedConfigRenderer != null) {
        NuxRendererV2ConfigDialog(
            rendererInfo = selectedConfigRenderer,
            onDismiss = { showRendererConfigDialog = false }
        )
    }

    // Dialog Tentang & Lisensi Open Source (GPL-3.0 & Zalith Compliance)
    if (showAboutDialog) {
        NuxAboutDialog(
            onDismissRequest = { showAboutDialog = false }
        )
    }

    // Dialog NUX Premium & Showcase (Fitur 1 - 7 Eksklusif)
    if (showPremiumDialog) {
        NuxPremiumDialog(
            initialPrompt = premiumInitialPrompt,
            onDismissRequest = {
                showPremiumDialog = false
                premiumInitialPrompt = null
            }
        )
    }
}
