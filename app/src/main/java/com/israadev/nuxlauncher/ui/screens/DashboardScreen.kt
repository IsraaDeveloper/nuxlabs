package com.israadev.nuxlauncher.ui.screens

import android.widget.Toast
import java.io.File
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.R
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.download.MinecraftDownloader
import com.israadev.nuxlauncher.core.instance.InstanceManager
import com.israadev.nuxlauncher.core.launch.GameLauncher
import com.israadev.nuxlauncher.core.runtime.JavaRuntimeManager
import com.israadev.nuxlauncher.core.crash.CrashManager
import com.israadev.nuxlauncher.core.settings.SettingsManager
import com.israadev.nuxlauncher.ui.components.*
import com.israadev.nuxlauncher.core.renderer.NuxRendererRegistry
import com.israadev.nuxlauncher.core.renderer.NuxRendererInfo
import com.israadev.nuxlauncher.ui.dialogs.NuxRendererWarningDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxAddInstanceDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxCrashDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxDeleteInstanceDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxDownloadProgressDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxUpdateDialog
import com.israadev.nuxlauncher.core.update.AndroidUpdateInfo
import com.israadev.nuxlauncher.core.update.UpdateManager
import com.israadev.nuxlauncher.core.mods.NuxAddonImportManager
import com.israadev.nuxlauncher.ui.dialogs.NuxAddonImportDialog
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.resp
import com.israadev.nuxlauncher.ui.theme.LocalNuxScale
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf("home") }

    val activeCrash by CrashManager.activeCrash.collectAsState()

    val instances by InstanceManager.instances.collectAsState()
    val selectedInstance by InstanceManager.selectedInstance.collectAsState()
    val currentAccount by AccountManager.currentAccount.collectAsState()
    val launcherUser by AccountManager.launcherUser.collectAsState()
    val launcherSettings by SettingsManager.settings.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var instanceToDelete by remember { mutableStateOf<com.israadev.nuxlauncher.core.models.Instance?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateDialogInfo by remember { mutableStateOf<AndroidUpdateInfo?>(null) }
    var pendingLaunchInstance by remember { mutableStateOf<com.israadev.nuxlauncher.core.models.Instance?>(null) }
    var unsupportedRendererInfo by remember { mutableStateOf<NuxRendererInfo?>(null) }
    val pendingImport by NuxAddonImportManager.pendingImport.collectAsState()

    // Auto check update every time launcher is opened
    LaunchedEffect(Unit) {
        val result = UpdateManager.checkForUpdate(context)
        result.onSuccess { info ->
            if (info.isUpdateAvailable) {
                updateDialogInfo = info
            }
        }
    }

    // Download progress state
    var isDownloading by remember { mutableStateOf(false) }
    var downloadTargetName by remember { mutableStateOf("") }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadMessage by remember { mutableStateOf("") }

    val downloader = remember { MinecraftDownloader(context) }

    if (currentTab == "gui_editor") {
        CustomGuiEditorScreen(
            onNavigateBack = { currentTab = "settings" },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NuxColors.Background)
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // --- 1. COMPACT LEFT SIDEBAR ---
                NuxSidebar(
                    activeTab = currentTab,
                    onTabSelected = { tabId ->
                        if (tabId == "home" || tabId == "accounts" || tabId == "settings" || tabId == "friends" || tabId == "mods") {
                            currentTab = tabId
                        } else {
                            Toast.makeText(context, "Fitur ${tabId.replaceFirstChar { it.uppercase() }} segera hadir di mobile!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    currentAccount = currentAccount,
                    launcherUser = launcherUser
                )

                // --- 2. MAIN CONTENT AREA ---
                if (currentTab == "accounts") {
                    AccountsScreen(
                        onNavigateBack = { currentTab = "home" },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                } else if (currentTab == "mods") {
                    ModsScreen(
                        onNavigateBack = { currentTab = "home" },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                } else if (currentTab == "settings") {
                    SettingsScreen(
                        onNavigateBack = { currentTab = "home" },
                        onOpenGuiEditor = { currentTab = "gui_editor" },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                } else if (currentTab == "friends") {
                    FriendsScreen(
                        onNavigateBack = { currentTab = "home" },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                } else {
                    // DASHBOARD SCREEN
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = (12.dp).resp(), end = (12.dp).resp(), top = (6.dp).resp(), bottom = (6.dp).resp())
                    ) {
                        // TOP BAR (Minimalist: No JAVA EDITION badge box, tight vertical padding)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = (5.dp).resp()),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DASHBOARD",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = (13.5.sp).resp(),
                                letterSpacing = (0.7.sp).resp()
                            )

                            // Compact Profile Indicator
                            val userShape = RoundedCornerShape((8.dp).resp())
                            Row(
                                modifier = Modifier
                                    .clip(userShape)
                                    .background(NuxColors.SurfaceElevated, userShape)
                                    .border(1.dp, NuxColors.CardBorder, userShape)
                                    .clickable { currentTab = "settings" }
                                    .padding(horizontal = (8.dp).resp(), vertical = (3.5.dp).resp()),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val photo = launcherUser?.photoURL
                                if (!photo.isNullOrBlank()) {
                                    NuxNetworkImage(
                                        model = photo,
                                        contentDescription = "Profile",
                                        fallbackInitials = launcherUser?.username ?: "User",
                                        modifier = Modifier.size((15.dp).resp()),
                                        shape = CircleShape
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size((6.5.dp).resp())
                                            .background(
                                                if (launcherUser?.isActivated == true) NuxColors.ForestGreen else NuxColors.Amber,
                                                CircleShape
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.width((5.dp).resp()))
                                Text(
                                    text = launcherUser?.username ?: "PROFIL",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = (10.5.sp).resp()
                                )
                            }
                        }

                        // MAIN TWO-COLUMN SPLIT (Landscape Optimized)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy((10.dp).resp())
                        ) {
                            // LEFT COLUMN: HERO CARD + 4 QUICK ACTION CARDS (Exactly matching PC version)
                            Column(
                                modifier = Modifier
                                    .weight(1.38f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy((7.dp).resp())
                            ) {
                                // 1. HERO CARD (Matching PC style)
                                val heroShape = RoundedCornerShape((15.dp).resp())
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1.35f)
                                        .clip(heroShape)
                                        .background(NuxColors.SurfaceElevated, heroShape)
                                        .border(1.dp, NuxColors.CardBorder, heroShape)
                                ) {
                                    if (selectedInstance != null) {
                                        val inst = selectedInstance!!
                                        val isFullyDownloaded = inst.isDownloaded && InstanceManager.isInstanceDownloaded(context, inst)

                                        // Scenery background / Animated video
                                        val hasValidHeroVideo = launcherSettings.heroAnimationEnabled &&
                                                launcherSettings.heroAnimationVideoPath.isNotBlank() &&
                                                File(launcherSettings.heroAnimationVideoPath).exists()

                                        if (hasValidHeroVideo) {
                                            HeroBannerVideoPlayer(
                                                videoPath = launcherSettings.heroAnimationVideoPath,
                                                rotationDegrees = launcherSettings.heroAnimationRotation,
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
                                                .padding(horizontal = (16.dp).resp(), vertical = (10.dp).resp())
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
                                                            .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape((7.dp).resp()))
                                                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape((7.dp).resp()))
                                                            .padding(horizontal = (7.dp).resp(), vertical = (2.5.dp).resp()),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size((5.dp).resp())
                                                                .background(NuxColors.ForestGreen, CircleShape)
                                                        )
                                                        Spacer(modifier = Modifier.width((5.dp).resp()))
                                                        Text(
                                                            text = "${inst.loader.uppercase()} EDITION",
                                                            color = NuxColors.MintGreen,
                                                            fontSize = (9.5.sp).resp(),
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = (0.7.sp).resp()
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height((4.dp).resp()))

                                                    // Large instance title
                                                    Text(
                                                        text = inst.name,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = (21.sp).resp(),
                                                        letterSpacing = (-0.4).sp,
                                                        maxLines = 1
                                                    )

                                                    Spacer(modifier = Modifier.height((2.dp).resp()))

                                                    // Emerald gradient accent line
                                                    Box(
                                                        modifier = Modifier
                                                            .size(width = (32.dp).resp(), height = (2.5.dp).resp())
                                                            .background(
                                                                Brush.horizontalGradient(
                                                                    colors = listOf(NuxColors.ForestGreen, NuxColors.MintGreen)
                                                                ),
                                                                CircleShape
                                                            )
                                                    )

                                                    Spacer(modifier = Modifier.height((4.dp).resp()))

                                                    // Subtitle
                                                    Text(
                                                        text = "Version ${inst.mcVersion} — Click PLAY to launch this instance and craft seamlessly.",
                                                        color = Color(0xFFA1A1AA),
                                                        fontSize = (10.sp).resp(),
                                                        lineHeight = (13.sp).resp(),
                                                        maxLines = 2
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width((12.dp).resp()))

                                                // Minimalist Cyber-Glass Play Button (Matching PC)
                                                val playBtnShape = RoundedCornerShape((14.dp).resp())
                                                Row(
                                                    modifier = Modifier
                                                        .clip(playBtnShape)
                                                        .background(Color(0xFF181B22), playBtnShape)
                                                        .border(
                                                            width = 1.dp,
                                                            color = NuxColors.ForestGreen.copy(alpha = 0.5f),
                                                            shape = playBtnShape
                                                        )
                                                        .clickable {
                                                            val account = currentAccount
                                                            if (account == null) {
                                                                Toast.makeText(context, "Silakan buat atau pilih akun terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                                                currentTab = "accounts"
                                                                return@clickable
                                                            }

                                                            if (!isFullyDownloaded) {
                                                                val targetRuntime = JavaRuntimeManager.getRecommendedRuntime(inst.mcVersion)
                                                                isDownloading = true
                                                                downloadTargetName = inst.name
                                                                downloadProgress = 0f
                                                                downloadMessage = "Menyiapkan OpenJDK (${JavaRuntimeManager.getRuntimeDisplayName(targetRuntime)})..."

                                                                scope.launch {
                                                                    JavaRuntimeManager.extractRuntime(context, targetRuntime) { msg ->
                                                                        downloadMessage = msg
                                                                    }

                                                                    val res = downloader.downloadInstance(inst) { p, msg ->
                                                                        downloadProgress = p
                                                                        downloadMessage = msg
                                                                    }
                                                                    isDownloading = false
                                                                    if (res.isSuccess) {
                                                                        Toast.makeText(context, "Instalasi selesai! Tekan PLAY untuk bermain.", Toast.LENGTH_SHORT).show()
                                                                    } else {
                                                                        Toast.makeText(context, "Gagal mengunduh: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                                                                    }
                                                                }
                                                            } else {
                                                                val currentRendererInfo = NuxRendererRegistry.findRendererById(launcherSettings.selectedRenderer)
                                                                val isSupported = NuxRendererRegistry.isSupportedForVersion(currentRendererInfo, inst.mcVersion)
                                                                if (!isSupported) {
                                                                    unsupportedRendererInfo = currentRendererInfo
                                                                    pendingLaunchInstance = inst
                                                                } else {
                                                                    Toast.makeText(context, "Meluncurkan ${inst.name}...", Toast.LENGTH_SHORT).show()
                                                                    GameLauncher.launch(context, inst, account)
                                                                }
                                                            }
                                                        }
                                                        .padding(horizontal = (14.dp).resp(), vertical = (9.dp).resp()),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy((10.dp).resp())
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size((28.dp).resp())
                                                            .clip(CircleShape)
                                                            .background(NuxColors.ForestGreen),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isFullyDownloaded) Icons.Default.PlayArrow else Icons.Default.Download,
                                                            contentDescription = "Action",
                                                            tint = Color(0xFF09090B),
                                                            modifier = Modifier.size((18.dp).resp())
                                                        )
                                                    }

                                                    Text(
                                                        text = if (isFullyDownloaded) "PLAY" else "UNDUH",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = (13.5.sp).resp(),
                                                        letterSpacing = (0.8.sp).resp()
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // EMPTY STATE HERO
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 20.dp, vertical = 14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "MULAI BERMAIN",
                                                    color = NuxColors.ForestGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    letterSpacing = 0.5.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Belum Ada Instance",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 18.sp,
                                                    maxLines = 1
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Buat instance Minecraft pertamamu untuk mulai bermain.",
                                                    color = Color(0xFFA1A1AA),
                                                    fontSize = 11.sp,
                                                    maxLines = 2
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            NuxButton(
                                                onClick = { showAddDialog = true },
                                                backgroundColor = NuxColors.ForestGreen,
                                                contentColor = Color.White,
                                                cornerRadius = 12.dp,
                                                modifier = Modifier.height(42.dp)
                                            ) {
                                                Text(
                                                    text = "+ BUAT INSTANCE",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // 2. 4 QUICK ACTION CARDS (Exact match with PC layout from Image 2)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.95f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Card 1: CHECK FOR UPDATES
                                    QuickActionCard(
                                        title = "CHECK FOR\nUPDATES",
                                        description = if (isCheckingUpdate) "Checking server..." else "Scan launcher patches.",
                                        icon = Icons.Outlined.Refresh,
                                        accentColor = NuxColors.ForestGreen,
                                        onClick = {
                                            if (isCheckingUpdate) return@QuickActionCard
                                            isCheckingUpdate = true
                                            Toast.makeText(context, "Memeriksa pembaruan NUX Launcher...", Toast.LENGTH_SHORT).show()
                                            scope.launch {
                                                val result = UpdateManager.checkForUpdate(context)
                                                isCheckingUpdate = false
                                                result.onSuccess { info ->
                                                    if (info.isUpdateAvailable) {
                                                        updateDialogInfo = info
                                                    } else {
                                                        Toast.makeText(context, "NUX Launcher v${info.localVersion} sudah versi terbaru!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }.onFailure { err ->
                                                    Toast.makeText(context, "Gagal cek update: ${err.localizedMessage ?: "Periksa koneksi internet"}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )

                                    // Card 2: OPEN GAME FOLDER
                                    QuickActionCard(
                                        title = "OPEN GAME\nFOLDER",
                                        description = "Inspect game directory.",
                                        icon = Icons.Outlined.Folder,
                                        accentColor = NuxColors.ForestGreen,
                                        onClick = {
                                            selectedInstance?.let { inst ->
                                                Toast.makeText(context, "Membuka folder instance...", Toast.LENGTH_SHORT).show()
                                                InstanceManager.openInstanceFolder(context, inst)
                                            } ?: run {
                                                Toast.makeText(context, "Pilih instance terlebih dahulu", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )

                                    // Card 3: RESOURCE MONITOR
                                    val activeRuntimeName = selectedInstance?.let { JavaRuntimeManager.getRecommendedRuntime(it.mcVersion) }
                                    val activeJreDisplay = if (activeRuntimeName != null) JavaRuntimeManager.getRuntimeDisplayName(activeRuntimeName) else "Auto (8-25)"
                                    QuickActionCard(
                                        title = "RESOURCE\nMONITOR",
                                        description = "${launcherSettings.ramMb} MB · $activeJreDisplay",
                                        icon = Icons.Outlined.BarChart,
                                        accentColor = NuxColors.ForestGreen,
                                        onClick = { currentTab = "settings" },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )

                                    // Card 4: DELETE INSTANCE (Rose Red Theme)
                                    QuickActionCard(
                                        title = "DELETE\nINSTANCE",
                                        description = "Erase this instance.",
                                        icon = Icons.Outlined.DeleteOutline,
                                        accentColor = Color(0xFFF43F5E),
                                        isDestructive = true,
                                        onClick = {
                                            selectedInstance?.let { inst ->
                                                instanceToDelete = inst
                                            } ?: run {
                                                Toast.makeText(context, "Pilih instance terlebih dahulu", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }
                            }

                            // RIGHT COLUMN: STREAMLINED INSTANCES LIST PANEL
                            val panelShape = RoundedCornerShape((15.dp).resp())
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(panelShape)
                                    .background(NuxColors.SurfaceElevated, panelShape)
                                    .border(1.dp, NuxColors.CardBorder, panelShape)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding((10.dp).resp())
                                ) {
                                    // Section Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "INSTANCE (${instances.size})",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = (11.5.sp).resp(),
                                            letterSpacing = (0.5.sp).resp()
                                        )

                                        val addPillShape = RoundedCornerShape((7.dp).resp())
                                        Box(
                                            modifier = Modifier
                                                .clip(addPillShape)
                                                .background(Color(0xFF10B981).copy(alpha = 0.14f), addPillShape)
                                                .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.45f), addPillShape)
                                                .clickable { showAddDialog = true }
                                                .padding(horizontal = (8.dp).resp(), vertical = (3.5.dp).resp())
                                        ) {
                                            Text(
                                                text = "+ TAMBAH",
                                                color = NuxColors.ForestGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = (9.5.sp).resp()
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height((6.dp).resp()))

                                    // Instance List
                                    if (instances.isEmpty()) {
                                        val emptyListShape = RoundedCornerShape((9.dp).resp())
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .clip(emptyListShape)
                                                .background(Color(0xFF12141A), emptyListShape)
                                                .border(1.dp, Color(0x14FFFFFF), emptyListShape)
                                                .padding((10.dp).resp()),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(text = "📦", fontSize = (18.sp).resp())
                                                Spacer(modifier = Modifier.height((3.dp).resp()))
                                                Text(
                                                    text = "Belum Ada Instance",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = (11.5.sp).resp()
                                                )
                                                Spacer(modifier = Modifier.height((2.dp).resp()))
                                                Text(
                                                    text = "Klik '+ TAMBAH' untuk membuat.",
                                                    color = Color(0xFFA1A1AA),
                                                    fontSize = (9.5.sp).resp()
                                                )
                                            }
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            verticalArrangement = Arrangement.spacedBy((5.dp).resp())
                                        ) {
                                            items(instances) { inst ->
                                                val isSelected = inst.id == selectedInstance?.id
                                                val itemShape = RoundedCornerShape((9.dp).resp())
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(itemShape)
                                                        .background(
                                                            if (isSelected) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFF12141A),
                                                            itemShape
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (isSelected) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0x12FFFFFF),
                                                            shape = itemShape
                                                        )
                                                        .clickable { InstanceManager.selectInstance(inst) }
                                                        .padding(horizontal = (10.dp).resp(), vertical = (6.5.dp).resp()),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Streamlined title & metadata
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = inst.name,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = (12.5.sp).resp(),
                                                            maxLines = 1
                                                        )
                                                        Spacer(modifier = Modifier.height((1.dp).resp()))
                                                        Text(
                                                            text = "v${inst.mcVersion} · ${inst.loader.uppercase()}",
                                                            color = if (isSelected) NuxColors.ForestGreen else Color(0xFF71717A),
                                                            fontSize = (9.5.sp).resp(),
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy((5.dp).resp())
                                                    ) {
                                                        if (isSelected) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size((16.dp).resp())
                                                                    .background(NuxColors.ForestGreen, CircleShape),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = "Selected",
                                                                    tint = Color(0xFF09090B),
                                                                    modifier = Modifier.size((11.dp).resp())
                                                                )
                                                            }
                                                        }

                                                        // Minimalist subtle ghost trash button
                                                        Box(
                                                            modifier = Modifier
                                                                .size((22.dp).resp())
                                                                .clip(RoundedCornerShape((6.dp).resp()))
                                                                .clickable { instanceToDelete = inst },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.DeleteOutline,
                                                                contentDescription = "Delete",
                                                                tint = Color(0xFF71717A),
                                                                modifier = Modifier.size((14.dp).resp())
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

            // Floating Voice Bar
            if (currentTab != "friends") {
                FloatingVoiceBar(
                    onOpenVoiceRoom = { currentTab = "friends" },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 12.dp)
                )
            }
        }

        // Add Instance Dialog
        if (showAddDialog) {
            NuxAddInstanceDialog(
                onDismiss = { showAddDialog = false },
                onInstanceCreated = { newInst ->
                    showAddDialog = false
                    InstanceManager.createInstance(context, newInst)
                    Toast.makeText(context, "Instance ${newInst.name} berhasil dibuat!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Delete Instance Confirmation Dialog
        instanceToDelete?.let { inst ->
            NuxDeleteInstanceDialog(
                instance = inst,
                onConfirm = {
                    instanceToDelete = null
                    InstanceManager.deleteInstance(context, inst.id)
                    Toast.makeText(context, "Instance ${inst.name} berhasil dihapus!", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { instanceToDelete = null }
            )
        }

        // Download Progress Dialog
        if (isDownloading) {
            NuxDownloadProgressDialog(
                instanceName = downloadTargetName,
                progress = downloadProgress,
                message = downloadMessage
            )
        }

        // Launcher Update Dialog
        updateDialogInfo?.let { info ->
            NuxUpdateDialog(
                updateInfo = info,
                onDismiss = { updateDialogInfo = null }
            )
        }

        // Unsupported Renderer Warning Dialog (Persis seperti Zalith)
        val warnRenderer = unsupportedRendererInfo
        val launchInst = pendingLaunchInstance
        if (warnRenderer != null && launchInst != null) {
            NuxRendererWarningDialog(
                renderer = warnRenderer,
                mcVersion = launchInst.mcVersion,
                onConfirm = {
                    val toLaunch = pendingLaunchInstance
                    val account = currentAccount
                    unsupportedRendererInfo = null
                    pendingLaunchInstance = null
                    if (toLaunch != null && account != null) {
                        Toast.makeText(context, "Meluncurkan ${toLaunch.name}...", Toast.LENGTH_SHORT).show()
                        GameLauncher.launch(context, toLaunch, account)
                    }
                },
                onDismiss = {
                    unsupportedRendererInfo = null
                    pendingLaunchInstance = null
                }
            )
        }

        // Game Crash Popup Dialog
        activeCrash?.let { crash ->
            NuxCrashDialog(
                crashInfo = crash,
                onDismiss = {
                    CrashManager.dismissCrash(context)
                }
            )
        }

        // External Addon Import Dialog (Open With from File Manager)
        pendingImport?.let { importItem ->
            NuxAddonImportDialog(
                pendingImport = importItem,
                onDismiss = {
                    NuxAddonImportManager.clearPendingImport()
                }
            )
        }
    }
}

/**
 * Quick Action Card matching the PC Launcher-Windows aesthetic (Image 2)
 */
@Composable
fun QuickActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    val cardShape = RoundedCornerShape((11.dp).resp())
    val bgColor = if (isDestructive) Color(0xFF191116) else Color(0xFF12141A)
    val borderColor = if (isDestructive) Color(0xFFF43F5E).copy(alpha = 0.25f) else Color(0x1FFFFFFF)

    Box(
        modifier = modifier
            .clip(cardShape)
            .background(bgColor, cardShape)
            .border(1.dp, borderColor, cardShape)
            .clickable { onClick() }
            .padding((8.dp).resp())
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Top icon container
                val iconShape = RoundedCornerShape((6.dp).resp())
                Box(
                    modifier = Modifier
                        .size((24.dp).resp())
                        .clip(iconShape)
                        .background(accentColor.copy(alpha = 0.12f), iconShape)
                        .border(1.dp, accentColor.copy(alpha = 0.25f), iconShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size((14.dp).resp())
                    )
                }

                Spacer(modifier = Modifier.height((4.dp).resp()))

                // Title
                Text(
                    text = title,
                    color = if (isDestructive) Color(0xFFFECDD3) else Color.White,
                    fontSize = (9.5.sp).resp(),
                    fontWeight = FontWeight.Black,
                    lineHeight = (11.5.sp).resp(),
                    letterSpacing = (0.3.sp).resp()
                )

                Spacer(modifier = Modifier.height((2.dp).resp()))

                // Accent line
                Box(
                    modifier = Modifier
                        .size(width = (16.dp).resp(), height = (1.5.dp).resp())
                        .background(accentColor.copy(alpha = 0.45f), CircleShape)
                )
            }

            // Description
            Text(
                text = description,
                color = Color(0xFFA1A1AA),
                fontSize = (8.sp).resp(),
                lineHeight = (9.5.sp).resp(),
                maxLines = 2
            )
        }
    }
}
