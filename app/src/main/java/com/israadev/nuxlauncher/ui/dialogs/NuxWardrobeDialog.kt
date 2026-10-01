package com.israadev.nuxlauncher.ui.dialogs

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.viewinterop.AndroidView
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.models.UserAccount
import com.israadev.nuxlauncher.core.skin.CapePreset
import com.israadev.nuxlauncher.core.skin.SkinUtils
import com.israadev.nuxlauncher.ui.components.NuxButton
import com.israadev.nuxlauncher.ui.components.NuxDialog
import com.israadev.nuxlauncher.ui.components.PlayerSkin
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun NuxWardrobeDialog(
    account: UserAccount,
    onDismissRequest: () -> Unit,
    onSaved: (UserAccount) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 3D Engine Instance
    val playerSkin = remember { PlayerSkin(context) }
    var isWebViewReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            playerSkin.destroy()
        }
    }

    // Tab state: "skin" or "cape"
    var activeTab by remember { mutableStateOf("skin") }

    // Wardrobe editing state
    var selectedModel by remember { mutableStateOf(account.safeSkinModel) }
    var currentSkinFile by remember {
        mutableStateOf(account.getSkinFile())
    }
    var currentCapeFile by remember {
        mutableStateOf(account.getCapeFile())
    }
    var selectedPresetCapeId by remember { mutableStateOf<String?>(null) }
    var isAutoRotating by remember { mutableStateOf(true) }

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var selectedAnim by remember { mutableStateOf("Walking") }

    val persistAccountChanges: (File?, File?, String) -> UserAccount = { skinFile, capeFile, model ->
        val updated = account.copy(
            skinModel = model,
            accessToken = account.safeAccessToken,
            refreshToken = account.safeRefreshToken,
            email = account.safeEmail,
            tier = account.safeTier,
            customSkinPath = skinFile?.absolutePath,
            customCapePath = capeFile?.absolutePath
        )
        SkinUtils.invalidateHeadCache(account.id)
        AccountManager.updateAccount(context, updated)
        AccountManager.selectAccount(updated)
        onSaved(updated)
        updated
    }

    // Pickers (Preview only - do not persist immediately)
    val skinPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                statusMessage = "Memvalidasi skin..."
                SkinUtils.importSkinFromUri(context, account.id, uri).fold(
                    onSuccess = { file ->
                        currentSkinFile = file
                        val isSlim = SkinUtils.isSlimModel(file)
                        selectedModel = if (isSlim) "slim" else "classic"
                        playerSkin.loadAccount(account, customSkinFile = file, customCapeFile = currentCapeFile)
                        playerSkin.loadSkin(file, selectedModel)
                        statusMessage = "Skin dimuat di preview (${if (isSlim) "Slim" else "Klasik"}) — Klik Simpan untuk menerapkan"
                    },
                    onFailure = { err ->
                        statusMessage = "Gagal: ${err.message}"
                        Toast.makeText(context, err.message ?: "File tidak valid", Toast.LENGTH_SHORT).show()
                    }
                )
                isProcessing = false
            }
        }
    }

    val capePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                statusMessage = "Memvalidasi cape..."
                SkinUtils.importCapeFromUri(context, account.id, uri).fold(
                    onSuccess = { file ->
                        currentCapeFile = file
                        selectedPresetCapeId = null
                        playerSkin.loadAccount(account, customSkinFile = currentSkinFile, customCapeFile = file)
                        playerSkin.loadCape(file)
                        statusMessage = "Cape kustom dimuat di preview — Klik Simpan untuk menerapkan"
                    },
                    onFailure = { err ->
                        statusMessage = "Gagal: ${err.message}"
                        Toast.makeText(context, err.message ?: "File cape tidak valid", Toast.LENGTH_SHORT).show()
                    }
                )
                isProcessing = false
            }
        }
    }

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "WARDROBE & CUSTOM SKIN",
                            color = NuxColors.DarkGray,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(NuxColors.ForestGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = account.username,
                                color = NuxColors.ForestGreen,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Kustomisasi tampilan skin dan jubah pemain Minecraft",
                        color = NuxColors.GrayNeutral,
                        fontSize = 9.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(NuxColors.SurfaceInput, CircleShape)
                        .border(1.dp, NuxColors.CardBorder, CircleShape)
                        .clickable {
                            onDismissRequest()
                        },
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

            // Main Content: Left 3D Viewport, Right Controls (Flexible Weight)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // LEFT: 3D SKIN VIEWER (Interactive)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(NuxColors.SurfaceInput, RoundedCornerShape(14.dp))
                        .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            playerSkin.loadWebView(ctx) {
                                isWebViewReady = true
                                playerSkin.loadAccount(account, currentSkinFile, currentCapeFile)
                                playerSkin.startAnim(selectedAnim, 0.8f)
                                playerSkin.setAutoRotate(isAutoRotating, 0.7f)
                                playerSkin.setCamera(0, 10, 52)
                            }
                        },
                        update = {
                            if (isWebViewReady) {
                                playerSkin.loadAccount(account, currentSkinFile, currentCapeFile)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top Quick Controls (Orbit Info & Camera/Rotate controls like Windows)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "3D (DRAG / PUTAR)",
                                color = NuxColors.ForestGreen,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Auto-rotate toggle
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        if (isAutoRotating) NuxColors.ForestGreen else Color.Black.copy(alpha = 0.65f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        isAutoRotating = !isAutoRotating
                                        playerSkin.setAutoRotate(isAutoRotating, 0.7f)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.RotateRight,
                                    contentDescription = "Putar Otomatis",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Recenter Camera
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        playerSkin.resetCamera()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CenterFocusStrong,
                                    contentDescription = "Reset Kamera",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    // Animation Switcher bar (Walking, Idle, Running, Wave)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Walking" to "Jalan", "DefaultIdle" to "Diam", "Running" to "Lari", "Wave" to "Sapa").forEach { (animKey, label) ->
                            val isSelected = selectedAnim == animKey
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) NuxColors.ForestGreen else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedAnim = animKey
                                        playerSkin.startAnim(animKey, 0.8f)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else NuxColors.GrayNeutral,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // RIGHT: TABS & CONTROLS
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Scrollable Controls Column
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Section Tabs: Skin vs Cape
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(8.dp))
                                .padding(2.5.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Tab Skin
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (activeTab == "skin") NuxColors.ForestGreen else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { activeTab = "skin" }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = if (activeTab == "skin") Color.White else NuxColors.GrayNeutral,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "KUSTOM SKIN",
                                        color = if (activeTab == "skin") Color.White else NuxColors.DarkGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Tab Cape
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (activeTab == "cape") Color(0xFFA855F7) else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { activeTab = "cape" }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Shield,
                                        contentDescription = null,
                                        tint = if (activeTab == "cape") Color.White else NuxColors.GrayNeutral,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "JUBAH / CAPE",
                                        color = if (activeTab == "cape") Color.White else NuxColors.DarkGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // TAB CONTENT: SKIN
                        if (activeTab == "skin") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Upload Button
                                NuxButton(
                                    onClick = { skinPickerLauncher.launch("image/png") },
                                    backgroundColor = NuxColors.ForestGreen.copy(alpha = 0.15f),
                                    contentColor = NuxColors.ForestGreen,
                                    cornerRadius = NuxSizes.CornerRadiusSmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.UploadFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "PILIH FILE SKIN (.PNG)",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp
                                    )
                                }

                                // Arm Model Selection (Steve vs Alex)
                                Text(
                                    text = "TIPE MODEL LENGAN:",
                                    color = NuxColors.GrayNeutral,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Classic (Klasik - 4px)
                                    val isClassic = selectedModel == "classic"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isClassic) NuxColors.ForestGreen.copy(alpha = 0.15f) else NuxColors.SurfaceInput,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isClassic) NuxColors.ForestGreen else NuxColors.CardBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedModel = "classic"
                                                playerSkin.loadSkin(currentSkinFile, "classic")
                                                statusMessage = "Model Klasik dipilih (Preview) — Klik Simpan"
                                            }
                                            .padding(vertical = 6.dp, horizontal = 8.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "Klasik",
                                                color = if (isClassic) NuxColors.ForestGreen else NuxColors.DarkGray,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Lengan tebal (4px)",
                                                color = NuxColors.GrayNeutral,
                                                fontSize = 8.sp
                                            )
                                        }
                                    }

                                    // Slim (Slim - 3px)
                                    val isSlim = selectedModel == "slim"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isSlim) NuxColors.ForestGreen.copy(alpha = 0.15f) else NuxColors.SurfaceInput,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSlim) NuxColors.ForestGreen else NuxColors.CardBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedModel = "slim"
                                                playerSkin.loadSkin(currentSkinFile, "slim")
                                                statusMessage = "Model Slim dipilih (Preview) — Klik Simpan"
                                            }
                                            .padding(vertical = 6.dp, horizontal = 8.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "Slim",
                                                color = if (isSlim) NuxColors.ForestGreen else NuxColors.DarkGray,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Lengan ramping (3px)",
                                                color = NuxColors.GrayNeutral,
                                                fontSize = 8.sp
                                            )
                                        }
                                    }
                                }

                                // Reset Skin Button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentSkinFile = null
                                            selectedModel = "classic"
                                            playerSkin.resetSkin()
                                            playerSkin.loadCape(currentCapeFile)
                                            statusMessage = "Skin direset ke default Steve (Preview) — Klik Simpan"
                                        }
                                        .padding(vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "↺ Reset ke Skin Default (Klasik)",
                                        color = NuxColors.GrayNeutral,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            // TAB CONTENT: CAPE
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                // Upload Custom Cape
                                NuxButton(
                                    onClick = { capePickerLauncher.launch("image/png") },
                                    backgroundColor = Color(0xFFA855F7).copy(alpha = 0.15f),
                                    contentColor = Color(0xFFA855F7),
                                    cornerRadius = NuxSizes.CornerRadiusSmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.UploadFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "PILIH FILE CAPE KUSTOM (.PNG)",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.5.sp
                                    )
                                }

                                Text(
                                    text = "ATAU PILIH PRESET JUBAH:",
                                    color = NuxColors.GrayNeutral,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // Preset Capes Grid (Chunked Rows for smooth vertical scrolling)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    SkinUtils.PRESET_CAPES.chunked(2).forEach { pair ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            pair.forEach { preset ->
                                                val isPresetSelected = selectedPresetCapeId == preset.id || (preset.id == "none" && currentCapeFile == null)
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .background(
                                                            if (isPresetSelected) Color(preset.badgeColor).copy(alpha = 0.18f) else NuxColors.SurfaceInput,
                                                            RoundedCornerShape(7.dp)
                                                        )
                                                        .border(
                                                            1.dp,
                                                            if (isPresetSelected) Color(preset.badgeColor) else NuxColors.CardBorder,
                                                            RoundedCornerShape(7.dp)
                                                        )
                                                        .clickable {
                                                            selectedPresetCapeId = preset.id
                                                            scope.launch {
                                                                val generatedCape = SkinUtils.applyPresetCape(context, account.id, preset.id)
                                                                currentCapeFile = generatedCape
                                                                playerSkin.loadCape(generatedCape)
                                                                statusMessage = if (preset.id == "none") "Cape dilepas (Preview) — Klik Simpan" else "Jubah ${preset.name} dipilih (Preview) — Klik Simpan"
                                                            }
                                                        }
                                                        .padding(horizontal = 7.dp, vertical = 5.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(7.dp)
                                                                .background(Color(preset.badgeColor), CircleShape)
                                                        )
                                                        Text(
                                                            text = preset.name,
                                                            color = if (isPresetSelected) Color(preset.badgeColor) else NuxColors.DarkGray,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                            if (pair.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bottom Action Bar (PINNED & ALWAYS VISIBLE)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (statusMessage != null) {
                            Text(
                                text = statusMessage!!,
                                color = NuxColors.ForestGreen,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            NuxButton(
                                onClick = onDismissRequest,
                                backgroundColor = Color(0xFF1A1D27),
                                borderColor = Color(0x33FFFFFF),
                                contentColor = Color.White,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                            ) {
                                Text("BATAL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }

                            NuxButton(
                                onClick = {
                                    persistAccountChanges(currentSkinFile, currentCapeFile, selectedModel)
                                    Toast.makeText(context, "Skin & Cape berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                    onDismissRequest()
                                },
                                backgroundColor = NuxColors.ForestGreen,
                                contentColor = Color.White,
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "SIMPAN PERUBAHAN",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.4.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
