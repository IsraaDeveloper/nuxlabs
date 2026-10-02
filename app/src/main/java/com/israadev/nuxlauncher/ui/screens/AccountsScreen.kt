package com.israadev.nuxlauncher.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.Bitmap
import com.israadev.nuxlauncher.core.skin.SkinUtils
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.account.elyby.ElyByAuthService
import com.israadev.nuxlauncher.core.account.elyby.ElyByDeviceCode
import com.israadev.nuxlauncher.core.account.elyby.ElyByLoginState
import com.israadev.nuxlauncher.core.account.microsoft.MicrosoftAuthService
import com.israadev.nuxlauncher.core.account.microsoft.MicrosoftDeviceCode
import com.israadev.nuxlauncher.core.models.UserAccount
import com.israadev.nuxlauncher.ui.components.*
import com.israadev.nuxlauncher.ui.dialogs.NuxMicrosoftAuthDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxWardrobeDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxPremiumDialog
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Creative Cyber-Obsidian Accounts Screen
 * - Left Panel: Active Profile Studio Showcase (fitted with zero clipping)
 * - Right Panel: Clean Account Deck & Roster (no redundant filter tabs)
 * - Add Account: High-end compact landscape modal with browser-only Ely.by & Microsoft
 */
@Composable
fun AccountsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val accounts by AccountManager.accounts.collectAsState()
    val currentAccount by AccountManager.currentAccount.collectAsState()
    val launcherUser by AccountManager.launcherUser.collectAsState()

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<UserAccount?>(null) }
    var accountForWardrobe by remember { mutableStateOf<UserAccount?>(null) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var premiumInitialPrompt by remember { mutableStateOf<String?>(null) }

    // 3D Player Model Preview (Interactive viewport like Zalith and Windows)
    val leftPlayerSkin = remember { PlayerSkin(context) }
    var leftPageFinished by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            leftPlayerSkin.destroy()
        }
    }

    // Active Account Signature Colors
    val activeType = currentAccount?.safeAccountType ?: "offline"
    val (activeTypeLabel, activeAccentColor, activeContainerBg) = when (activeType) {
        "microsoft" -> Triple("MICROSOFT", Color(0xFF0078D4), Color(0x260078D4))
        "elyby" -> Triple("ELY.BY", Color(0xFFA855F7), Color(0x26A855F7))
        else -> Triple("OFFLINE", NuxColors.ForestGreen, NuxColors.LightGreen)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NuxColors.Background)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // =========================================================
        // TOP APP HEADER (Minimalist, matching Home)
        // =========================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Back Button
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
                            imageVector = Icons.Outlined.ArrowBack,
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

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "PROFIL MINECRAFT",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 0.8.sp
                )
            }

            // Right Action Bar: Add Account Button (Matching + TAMBAH on Home)
            Box(
                modifier = Modifier
                    .height(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(NuxColors.ForestGreen.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .clickable {
                        if (accounts.size >= 2 && launcherUser?.isActivated != true) {
                            premiumInitialPrompt = "Pengguna Free dibatasi maksimal 2 akun tersimpan. Upgrade ke NUX Premium untuk menyimpan dan beralih antarakun tanpa batas!"
                            showPremiumDialog = true
                        } else {
                            showAddAccountDialog = true
                        }
                    }
                    .padding(horizontal = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "+ TAMBAH AKUN",
                        color = NuxColors.ForestGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // =========================================================
        // MAIN 2-COLUMN STUDIO & ROSTER LAYOUT
        // =========================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // -----------------------------------------------------
            // LEFT COLUMN: HERO PROFILE STUDIO SHOWCASE
            // -----------------------------------------------------
            NuxCard(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                backgroundColor = NuxColors.SurfaceWhite,
                borderColor = NuxColors.CardBorder,
                cornerRadius = NuxSizes.CornerRadiusLarge
            ) {
                if (currentAccount != null) {
                    val acc = currentAccount!!

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header row inside card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(NuxColors.ForestGreen, CircleShape)
                                )
                                Text(
                                    text = "PROFIL AKTIF",
                                    color = NuxColors.ForestGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(activeContainerBg, RoundedCornerShape(4.dp))
                                    .border(1.dp, activeAccentColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = activeTypeLabel,
                                    color = activeAccentColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 3D Interactive Player Character Showcase (Large full-body showcase)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    leftPlayerSkin.loadWebView(ctx) {
                                        leftPageFinished = true
                                        leftPlayerSkin.loadAccount(acc)
                                        leftPlayerSkin.startAnim("NewIdle", 0.7f)
                                        leftPlayerSkin.setAutoRotate(true, 0.6f)
                                        leftPlayerSkin.setCamera(20, 8, 44)
                                    }
                                },
                                update = {
                                    if (leftPageFinished) {
                                        leftPlayerSkin.loadAccount(acc)
                                    }
                                }
                            )
                        }

                        LaunchedEffect(acc.id, acc.customSkinPath, acc.customCapePath, acc.skinModel, leftPageFinished) {
                            if (leftPageFinished) {
                                val updated = SkinUtils.ensureSkinAndCapeCached(context, acc)
                                leftPlayerSkin.loadAccount(updated)
                            }
                        }

                        // Username & Subtitle (Cleanly placed at the bottom, zero clutter)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp)
                        ) {
                            Text(
                                text = acc.username,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = (-0.2).sp
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = when (acc.safeAccountType) {
                                    "microsoft" -> "Akun Resmi Microsoft"
                                    "elyby" -> "Akun Terverifikasi Ely.by"
                                    else -> "Akun Offline / Lokal"
                                },
                                color = NuxColors.GrayNeutral,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "Belum ada akun",
                            tint = NuxColors.GrayNeutral,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Belum Ada Profil",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tambahkan akun untuk mulai bermain.",
                            color = NuxColors.GrayNeutral,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // -----------------------------------------------------
            // RIGHT COLUMN: ACCOUNT ROSTER & DECK (MATCHING HOME INSTANCES)
            // -----------------------------------------------------
            NuxCard(
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxHeight(),
                backgroundColor = NuxColors.SurfaceWhite,
                borderColor = NuxColors.CardBorder,
                cornerRadius = NuxSizes.CornerRadiusLarge
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(11.dp)
                ) {
                    // Section Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DAFTAR AKUN (${accounts.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = "Pilih untuk berganti profil",
                            color = NuxColors.GrayNeutral,
                            fontSize = 9.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Accounts List
                    if (accounts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada akun tersimpan.",
                                color = NuxColors.GrayNeutral,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(accounts, key = { "${it.safeAccountType}_${it.id}_${it.username}" }) { acc ->
                                val isCurrent = acc.id == currentAccount?.id || (acc.username == currentAccount?.username && acc.safeAccountType == currentAccount?.safeAccountType)

                                val (badgeTxt, badgeBg, badgeColor) = when (acc.safeAccountType) {
                                    "microsoft" -> Triple("MS", Color(0x260078D4), Color(0xFF0078D4))
                                    "elyby" -> Triple("ELY", Color(0x26A855F7), Color(0xFFA855F7))
                                    else -> Triple("OFF", NuxColors.LightGreen, NuxColors.ForestGreen)
                                }

                                val itemShape = RoundedCornerShape(10.dp)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(itemShape)
                                        .background(
                                            if (isCurrent) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFF12141A),
                                            itemShape
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isCurrent) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0x12FFFFFF),
                                            shape = itemShape
                                        )
                                        .clickable {
                                            AccountManager.selectAccount(acc)
                                            coroutineScope.launch {
                                                val updated = SkinUtils.ensureSkinAndCapeCached(context, acc)
                                                leftPlayerSkin.loadAccount(updated)
                                            }
                                        }
                                        .padding(horizontal = 11.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Avatar
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .background(badgeBg, RoundedCornerShape(7.dp))
                                                .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(7.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            NuxAccountAvatar(
                                                account = acc,
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(RoundedCornerShape(6.dp)),
                                                fallbackInitials = acc.username.take(2).uppercase()
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(9.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = acc.username,
                                                    color = Color.White,
                                                    fontWeight = if (isCurrent) FontWeight.Black else FontWeight.SemiBold,
                                                    fontSize = 12.sp
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(badgeBg)
                                                        .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = badgeTxt,
                                                        color = badgeColor,
                                                        fontSize = 7.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(1.dp))
                                            val shortUuid = if (acc.uuid.length > 10) "${acc.uuid.take(6)}...${acc.uuid.takeLast(4)}" else acc.uuid
                                            Text(
                                                text = shortUuid,
                                                color = if (isCurrent) NuxColors.ForestGreen.copy(alpha = 0.85f) else Color(0xFF71717A),
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    // Actions
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .background(NuxColors.ForestGreen, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Aktif",
                                                    tint = Color(0xFF09090B),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .height(23.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(NuxColors.SurfaceElevated, RoundedCornerShape(6.dp))
                                                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        AccountManager.selectAccount(acc)
                                                        coroutineScope.launch {
                                                            val updated = SkinUtils.ensureSkinAndCapeCached(context, acc)
                                                            leftPlayerSkin.loadAccount(updated)
                                                        }
                                                    }
                                                    .padding(horizontal = 7.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "GUNAKAN",
                                                    color = Color.White,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Wardrobe icon (Hanya untuk Microsoft & Akun Offline / Local, persis seperti ZalithLauncher)
                                        val isSkinChangeAllowed = acc.safeAccountType == "microsoft" || acc.safeAccountType == "offline"
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    if (isSkinChangeAllowed) {
                                                        accountForWardrobe = acc
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "Skin & Jubah akun Ely.by dikelola langsung di https://account.ely.by",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Checkroom,
                                                contentDescription = "Skin & Cape",
                                                tint = if (isSkinChangeAllowed) Color(0xFF71717A) else Color(0xFF3F3F46),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        // Delete icon
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { accountToDelete = acc },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = "Hapus",
                                                tint = Color(0xFF71717A),
                                                modifier = Modifier.size(14.dp)
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

    // =============================================================
    // MODAL DIALOG: WARDROBE / CUSTOM SKIN & CAPE
    // =============================================================
    if (accountForWardrobe != null) {
        NuxWardrobeDialog(
            account = accountForWardrobe!!,
            onDismissRequest = { accountForWardrobe = null },
            onSaved = { updated ->
                accountForWardrobe = null
                leftPlayerSkin.loadAccount(updated)
            }
        )
    }

    // =============================================================
    // MODAL DIALOG: TAMBAH AKUN (High-End Space-Saving Landscape Popup)
    // =============================================================
    if (showAddAccountDialog) {
        AddAccountModal(
            onDismiss = { showAddAccountDialog = false },
            onAccountAdded = { newAcc ->
                AccountManager.addAccount(context, newAcc)
                Toast.makeText(context, "Akun ${newAcc.username} berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                showAddAccountDialog = false
            }
        )
    }

    // =============================================================
    // CONFIRM DELETE DIALOG
    // =============================================================
    if (accountToDelete != null) {
        val target = accountToDelete!!
        NuxDialog(
            onDismissRequest = { accountToDelete = null },
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(NuxColors.ErrorRed.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = NuxColors.ErrorRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Hapus Akun?",
                    color = NuxColors.DarkGray,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hapus profil '${target.username}' (${target.safeAccountType.uppercase()}) dari perangkat?",
                    color = NuxColors.GrayNeutral,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NuxButton(
                        onClick = { accountToDelete = null },
                        backgroundColor = NuxColors.SurfaceInput,
                        contentColor = NuxColors.DarkGray,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("BATAL", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    NuxButton(
                        onClick = {
                            AccountManager.removeAccount(context, target)
                            Toast.makeText(context, "Profil '${target.username}' dihapus", Toast.LENGTH_SHORT).show()
                            accountToDelete = null
                        },
                        backgroundColor = NuxColors.ErrorRed,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("HAPUS", fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }
        }
    }

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

/**
 * High-End Landscape Gaming Modal for Adding Accounts
 * Designed with authentic cyber-minimalist styling, compact proportions, and zero overflow
 */
@Composable
private fun AddAccountModal(
    onDismiss: () -> Unit,
    onAccountAdded: (UserAccount) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf("offline") } // "offline", "microsoft", "elyby"

    // Offline State
    var offlineName by remember { mutableStateOf("") }

    // Microsoft State
    var msDeviceCode by remember { mutableStateOf<MicrosoftDeviceCode?>(null) }
    var msErrorMsg by remember { mutableStateOf<String?>(null) }
    var isMsLoading by remember { mutableStateOf(false) }

    // Ely.by State (Browser Device Code Flow)
    val elyLoginState by ElyByAuthService.loginState.collectAsState()

    // Handle Ely.by login success
    LaunchedEffect(elyLoginState) {
        val state = elyLoginState
        if (state is ElyByLoginState.Success) {
            Toast.makeText(context, "Akun ${state.account.username} berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
            ElyByAuthService.resetState()
            onDismiss()
        }
    }

    NuxDialog(
        onDismissRequest = {
            onDismiss()
        },
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .wrapContentHeight(),
        fillMaxHeight = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Modal Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HUBUNGKAN AKUN",
                        color = NuxColors.DarkGray,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Pilih penyedia profil Minecraft yang ingin kamu gunakan",
                        color = NuxColors.GrayNeutral,
                        fontSize = 10.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(NuxColors.SurfaceInput, CircleShape)
                        .border(1.dp, NuxColors.CardBorder, CircleShape)
                        .clickable {
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = NuxColors.GrayNeutral,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Provider Selector Cards (Sleek Cyber Tiles)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: Offline
                ProviderCard(
                    title = "Offline",
                    badge = "Gratis",
                    description = "Main lokal tanpa login",
                    isSelected = selectedTab == "offline",
                    accentColor = NuxColors.ForestGreen,
                    icon = Icons.Outlined.Person,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = "offline" }
                )

                // Card 2: Microsoft
                ProviderCard(
                    title = "Microsoft",
                    badge = "Resmi",
                    description = "Akun resmi Mojang",
                    isSelected = selectedTab == "microsoft",
                    accentColor = Color(0xFF0078D4),
                    icon = Icons.Outlined.Language,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = "microsoft" }
                )

                // Card 3: Ely.by
                ProviderCard(
                    title = "Ely.by",
                    badge = "Browser",
                    description = "Skin & jubah online",
                    isSelected = selectedTab == "elyby",
                    accentColor = Color(0xFFA855F7),
                    icon = Icons.Outlined.Public,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = "elyby" }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Action Area for Selected Provider
            when (selectedTab) {
                // =============================================
                // TAB 1: OFFLINE
                // =============================================
                "offline" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Steve Avatar Head Box
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(12.dp))
                                .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (offlineName.trim().isNotBlank()) {
                                NuxNetworkImage(
                                    model = "https://mc-heads.net/avatar/${offlineName.trim()}/80",
                                    contentDescription = offlineName,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    fallbackInitials = offlineName.trim().take(2).uppercase(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = NuxColors.ForestGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Input Field
                        Box(modifier = Modifier.weight(1f)) {
                            NuxTextField(
                                value = offlineName,
                                onValueChange = { offlineName = it },
                                placeholder = "Masukkan nama pemain (Steve/Alex...)"
                            )
                        }

                        // Submit Button
                        NuxButton(
                            onClick = {
                                val cleanName = offlineName.trim()
                                if (cleanName.isBlank()) {
                                    Toast.makeText(context, "Username tidak boleh kosong", Toast.LENGTH_SHORT).show()
                                    return@NuxButton
                                }
                                val offlineUuid = UUID.nameUUIDFromBytes("OfflinePlayer:$cleanName".toByteArray(Charsets.UTF_8))
                                    .toString()
                                    .replace("-", "")
                                val newAcc = UserAccount(
                                    id = "offline_${System.currentTimeMillis()}",
                                    username = cleanName,
                                    uuid = offlineUuid,
                                    accessToken = "0",
                                    isOffline = true,
                                    accountType = "offline"
                                )
                                onAccountAdded(newAcc)
                            },
                            backgroundColor = NuxColors.ForestGreen,
                            contentColor = Color.White,
                            enabled = offlineName.trim().isNotBlank(),
                            cornerRadius = NuxSizes.CornerRadiusSmall,
                            modifier = Modifier.height(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SIMPAN",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // =============================================
                // TAB 2: MICROSOFT (In-App WebView & Device Code)
                // =============================================
                "microsoft" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Otorisasi Akun Microsoft / Xbox Live",
                                color = Color(0xFF0078D4),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Jendela otorisasi web dibuka langsung di launcher tanpa perlu keluar aplikasi.",
                                color = NuxColors.GrayNeutral,
                                fontSize = 10.sp
                            )
                            if (msErrorMsg != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msErrorMsg!!,
                                    color = NuxColors.ErrorRed,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        NuxButton(
                            onClick = {
                                isMsLoading = true
                                msErrorMsg = null
                                scope.launch {
                                    val res = MicrosoftAuthService.startDeviceCodeFlow()
                                    isMsLoading = false
                                    res.fold(
                                        onSuccess = { dc ->
                                            msDeviceCode = dc
                                        },
                                        onFailure = { err ->
                                            msErrorMsg = err.message ?: "Gagal memulai otorisasi Microsoft."
                                        }
                                    )
                                }
                            },
                            backgroundColor = Color(0xFF0078D4),
                            contentColor = Color.White,
                            enabled = !isMsLoading,
                            cornerRadius = NuxSizes.CornerRadiusSmall,
                            modifier = Modifier.height(40.dp)
                        ) {
                            if (isMsLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("MENGHUBUNGI...", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            } else {
                                Icon(imageVector = Icons.Outlined.Language, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("MASUK DENGAN MICROSOFT", fontWeight = FontWeight.Black, fontSize = 10.sp)
                            }
                        }
                    }
                }

                // =============================================
                // TAB 3: ELY.BY (Browser Device Code)
                // =============================================
                "elyby" -> {
                    when (val state = elyLoginState) {
                        is ElyByLoginState.WaitingForApproval -> {
                            val dc = state.deviceCode
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            color = Color(0xFFA855F7),
                                            strokeWidth = 1.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = state.statusMsg.ifBlank { "Menunggu otorisasi di browser Ely.by..." },
                                            color = NuxColors.DarkGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Kode: ${dc.userCode}",
                                            color = Color(0xFFA855F7),
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "(Salin)",
                                            color = NuxColors.GrayNeutral,
                                            fontSize = 10.sp,
                                            modifier = Modifier.clickable {
                                                clipboardManager.setText(AnnotatedString(dc.userCode))
                                                Toast.makeText(context, "Kode disalin", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    NuxButton(
                                        onClick = {
                                            ElyByAuthService.openBrowser(context, dc.verificationUrlWithCode)
                                        },
                                        backgroundColor = Color(0xFFA855F7),
                                        contentColor = Color.White,
                                        cornerRadius = NuxSizes.CornerRadiusSmall,
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("BUKA LAGI", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                    NuxButton(
                                        onClick = {
                                            ElyByAuthService.cancelLogin()
                                        },
                                        backgroundColor = NuxColors.SurfaceElevated,
                                        contentColor = NuxColors.DarkGray,
                                        cornerRadius = NuxSizes.CornerRadiusSmall,
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("BATAL", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                        else -> {
                            val isStarting = state is ElyByLoginState.Starting
                            val errMsg = (state as? ElyByLoginState.Error)?.message

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Otorisasi Akun Ely.by",
                                        color = Color(0xFFA855F7),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Browser dibuka otomatis untuk konfirmasi akun Ely.by tanpa mengetik password.",
                                        color = NuxColors.GrayNeutral,
                                        fontSize = 10.sp
                                    )
                                    if (errMsg != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = errMsg, color = NuxColors.ErrorRed, fontSize = 10.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                NuxButton(
                                    onClick = {
                                        ElyByAuthService.startLogin(context)
                                    },
                                    backgroundColor = Color(0xFFA855F7),
                                    contentColor = Color.White,
                                    enabled = !isStarting,
                                    cornerRadius = NuxSizes.CornerRadiusSmall,
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    if (isStarting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("MENGHUBUNGI...", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.OpenInBrowser,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("BUKA BROWSER ELY.BY", fontWeight = FontWeight.Black, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (msDeviceCode != null) {
        NuxMicrosoftAuthDialog(
            deviceCode = msDeviceCode!!,
            onDismiss = {
                msDeviceCode = null
            },
            onSuccess = { acc ->
                msDeviceCode = null
                onAccountAdded(acc)
            },
            onError = { err ->
                msDeviceCode = null
                msErrorMsg = err
            }
        )
    }
}

/**
 * Modern Interactive Provider Selector Card
 */
@Composable
private fun ProviderCard(
    title: String,
    badge: String,
    description: String,
    isSelected: Boolean,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .background(
                if (isSelected) accentColor.copy(alpha = 0.14f) else NuxColors.SurfaceInput,
                shape
            )
            .border(
                1.5.dp,
                if (isSelected) accentColor else NuxColors.CardBorder,
                shape
            )
            .clip(shape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) accentColor else NuxColors.GrayNeutral,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = title,
                        color = if (isSelected) accentColor else NuxColors.DarkGray,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.5.sp,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) accentColor.copy(alpha = 0.2f) else NuxColors.SurfaceElevated,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = if (isSelected) accentColor else NuxColors.GrayNeutral,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = description,
                color = NuxColors.GrayNeutral,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun NuxAccountAvatar(
    account: UserAccount,
    modifier: Modifier = Modifier,
    fallbackInitials: String = account.username.take(2).uppercase()
) {
    val context = LocalContext.current
    val headBitmapState = produceState<Bitmap?>(
        initialValue = null,
        account.id,
        account.customSkinPath,
        account.safeAccountType
    ) {
        value = SkinUtils.getAccountHeadBitmap(context, account, targetSize = 64)
    }

    val headBitmap = headBitmapState.value

    if (headBitmap != null) {
        Image(
            bitmap = headBitmap.asImageBitmap(),
            contentDescription = account.username,
            modifier = modifier,
            filterQuality = FilterQuality.None
        )
    } else {
        Box(
            modifier = modifier.background(NuxColors.SurfaceInput),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackInitials,
                color = NuxColors.DarkGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

