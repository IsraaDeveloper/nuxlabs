package com.israadev.nuxlauncher.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.auth.AuthService
import com.israadev.nuxlauncher.core.auth.SubscriptionInfo
import com.israadev.nuxlauncher.ui.components.NuxBadge
import com.israadev.nuxlauncher.ui.components.NuxButton
import com.israadev.nuxlauncher.ui.components.NuxCard
import com.israadev.nuxlauncher.ui.components.NuxDialog
import com.israadev.nuxlauncher.ui.theme.NuxColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private data class PremiumFeatureItem(
    val icon: String,
    val title: String,
    val desc: String
)

private val PREMIUM_FEATURES = listOf(
    PremiumFeatureItem("🎙️", "Voice Rooms Real-Time", "Masuk dan buat ruang obrolan suara mabar berlatensi rendah (LiveKit WebRTC)."),
    PremiumFeatureItem("📦", "Unlimited Game Instances", "Buat profil Minecraft dan modpack tanpa batas (Pengguna Free dibatasi maks 3)."),
    PremiumFeatureItem("🎬", "Custom Video Hero Banner", "Pasang video MP4 kustom sebagai latar belakang bergerak di dashboard launcher."),
    PremiumFeatureItem("👑", "Lencana Profil VIP Eksklusif", "Tampilan lencana Cyber Golden / Emerald VIP di profil, header, dan chat mabar."),
    PremiumFeatureItem("🧪", "Akses Versi Snapshot & Beta", "Bebas unduh dan mainkan build Snapshot terbaru, Old Beta, dan Old Alpha."),
    PremiumFeatureItem("👥", "Multi-Account Switcher Bebas", "Simpan dan beralih antarakun tanpa batas (Pengguna Free dibatasi maks 2)."),
    PremiumFeatureItem("🚀", "Turbo Download Multi-Thread", "Akselerasi unduhan aset dan pustaka dengan 48 parallel worker threads.")
)

private fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null || timestamp <= 0) return "Telah Teraktivasi"
    return try {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", Locale("id", "ID"))
        sdf.format(Date(timestamp))
    } catch (_: Exception) {
        "Telah Teraktivasi"
    }
}

private fun formatExpiry(expiresAt: String?): Pair<String, String> {
    if (expiresAt.isNullOrBlank() || expiresAt.equals("lifetime", ignoreCase = true)) {
        return Pair("Permanen (Selamanya)", "Tanpa Batas Waktu")
    }
    return try {
        val epoch = expiresAt.toLongOrNull()
        if (epoch != null) {
            val diffMs = epoch - System.currentTimeMillis()
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            val dateStr = sdf.format(Date(epoch))
            if (diffMs > 0) {
                val days = TimeUnit.MILLISECONDS.toDays(diffMs)
                val hours = TimeUnit.MILLISECONDS.toHours(diffMs) % 24
                val remainingStr = if (days > 0) "$days Hari lagi" else "$hours Jam lagi"
                Pair(dateStr, remainingStr)
            } else {
                Pair(dateStr, "Masa Aktif Berakhir")
            }
        } else {
            Pair(expiresAt, "Aktif")
        }
    } catch (_: Exception) {
        Pair(expiresAt ?: "Aktif", "Aktif")
    }
}

private fun maskKey(key: String?): String {
    if (key.isNullOrBlank()) return "VIP-AKTIF-SERVER"
    val clean = key.trim().uppercase()
    if (clean.length < 8) return clean
    return if (clean.contains("-")) {
        val parts = clean.split("-")
        if (parts.size >= 4) {
            "${parts[0]}-••••-••••-${parts.last()}"
        } else {
            "${clean.take(4)}-••••-${clean.takeLast(4)}"
        }
    } else {
        "${clean.take(4)}••••${clean.takeLast(4)}"
    }
}

@Composable
fun NuxPremiumDialog(
    initialPrompt: String? = null,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val launcherUser by AccountManager.launcherUser.collectAsState()

    var licenseKeyInput by remember { mutableStateOf("") }
    var isActivating by remember { mutableStateOf(false) }
    var activationError by remember { mutableStateOf<String?>(null) }
    var activationSuccess by remember { mutableStateOf<String?>(null) }
    var showRedeemBoxForVip by remember { mutableStateOf(false) }

    // Live subscription info fetched from server
    var subscriptionInfo by remember { mutableStateOf<SubscriptionInfo?>(null) }
    var isRefreshingInfo by remember { mutableStateOf(false) }

    val isUserPremium = launcherUser?.isActivated == true || subscriptionInfo?.isActivated == true
    val activeTier = (subscriptionInfo?.tier ?: launcherUser?.tier ?: "unactivated").lowercase()

    // Tier-based theme styling
    val isLifetime = activeTier == "lifetime"
    val isYearly = activeTier == "yearly"
    val isMonthly = activeTier == "monthly"

    val themeAccentColor = when {
        isLifetime -> Color(0xFFF59E0B) // Cyber Gold / Amber
        isYearly -> Color(0xFF10B981)   // Emerald Green
        isMonthly -> Color(0xFF0EA5E9)  // Electric Cyan
        else -> Color(0xFFF59E0B)
    }

    val themeGradient = when {
        isLifetime -> listOf(Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFF78350F))
        isYearly -> listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF064E3B))
        isMonthly -> listOf(Color(0xFF0EA5E9), Color(0xFF0284C7), Color(0xFF082F49))
        else -> listOf(Color(0xFFF59E0B), Color(0xFFB45309), Color(0xFF451A03))
    }

    // Auto-fetch fresh subscription details on open
    LaunchedEffect(launcherUser?.uid) {
        val uid = launcherUser?.uid
        if (!uid.isNullOrBlank()) {
            isRefreshingInfo = true
            val info = AuthService.fetchSubscriptionInfo(uid, launcherUser?.idToken)
            if (info != null) {
                subscriptionInfo = info
                if (launcherUser != null && (launcherUser!!.activatedAt == null || launcherUser!!.expiresAt == null)) {
                    val updated = launcherUser!!.copy(
                        isActivated = info.isActivated,
                        tier = if (info.isActivated) info.tier else launcherUser!!.tier,
                        activatedAt = info.activatedAt ?: launcherUser!!.activatedAt,
                        expiresAt = info.expiresAt ?: launcherUser!!.expiresAt,
                        redeemedKey = info.redeemedKey ?: launcherUser!!.redeemedKey
                    )
                    AccountManager.saveAuthUser(context, updated)
                }
            }
            isRefreshingInfo = false
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
            // ==========================================
            // 1. TOP HEADER BAR (Double-Bezel Aura)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Outer bezel icon ring
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(themeAccentColor.copy(alpha = 0.28f), Color.Transparent)
                                ),
                                CircleShape
                            )
                            .border(1.5.dp, themeAccentColor.copy(alpha = 0.7f), CircleShape)
                            .padding(2.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF161922), CircleShape)
                                .border(1.dp, Color(0x33FFFFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isUserPremium) Icons.Default.Star else Icons.Outlined.WorkspacePremium,
                                contentDescription = null,
                                tint = themeAccentColor,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NUX LAUNCHER PREMIUM",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.5.sp,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            NuxBadge(
                                text = if (isUserPremium) "VIP AKTIF" else "UPGRADE",
                                backgroundColor = if (isUserPremium) themeAccentColor.copy(alpha = 0.25f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                                textColor = if (isUserPremium) themeAccentColor else Color(0xFFFBBF24)
                            )
                        }
                        Text(
                            text = if (isUserPremium)
                                "Keanggotaan VIP aktif • Buka 7 fitur eksklusif, Voice Room, dan slot tanpa batas"
                            else
                                "Buka 7 fitur eksklusif, Voice Room mabar, dan slot tanpa batas",
                            color = NuxColors.GrayNeutral,
                            fontSize = 8.5.sp
                        )
                    }
                }

                // Close Button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(NuxColors.SurfaceInput, CircleShape)
                        .border(1.dp, NuxColors.CardBorder, CircleShape)
                        .clickable { onDismissRequest() },
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

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // 2. MAIN BODY (Left Features + Right Details/Upgrade)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ------------------------------------------
                // LEFT COLUMN: 7 Showcase Cards (Double-Bezel)
                // ------------------------------------------
                Column(
                    modifier = Modifier
                        .weight(1.08f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!initialPrompt.isNullOrBlank()) {
                        NuxCard(
                            backgroundColor = Color(0xFF1E1710),
                            borderColor = Color(0xFFF59E0B).copy(alpha = 0.5f),
                            cornerRadius = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = initialPrompt,
                                color = Color(0xFFFBBF24),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "7 FITUR EKSKLUSIF NUX PREMIUM:",
                            color = NuxColors.GrayNeutral,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        if (isUserPremium) {
                            Text(
                                text = "SEMUA AKTIF ✓",
                                color = NuxColors.MintGreen,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    PREMIUM_FEATURES.forEach { item ->
                        // Double-bezel Outer Shell
                        val outerBorderColor = if (isUserPremium)
                            themeAccentColor.copy(alpha = 0.35f)
                        else
                            NuxColors.CardBorder

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F1219), RoundedCornerShape(9.dp))
                                .border(1.dp, outerBorderColor, RoundedCornerShape(9.dp))
                                .padding(1.5.dp)
                        ) {
                            // Inner Core
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF141822), RoundedCornerShape(7.5.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color(0xFF1A202C), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(item.icon, fontSize = 14.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = item.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                        if (isUserPremium) {
                                            Box(
                                                modifier = Modifier
                                                    .background(NuxColors.ForestGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                    .border(0.5.dp, NuxColors.ForestGreen.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "AKTIF",
                                                    color = NuxColors.MintGreen,
                                                    fontSize = 6.5.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = item.desc,
                                        color = NuxColors.GrayNeutral,
                                        fontSize = 7.5.sp,
                                        lineHeight = 10.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // ------------------------------------------
                // RIGHT COLUMN:
                // If Premium -> VIP Detail Passport Card
                // If Free -> Upgrade Plans & Key Redemption
                // ------------------------------------------
                Column(
                    modifier = Modifier
                        .weight(1.12f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isUserPremium) {
                        // ==========================================
                        // STATE A: DETAIL PREMIUM AKTIF (VIP PASSPORT)
                        // ==========================================
                        val effectiveExpiresAt = subscriptionInfo?.expiresAt ?: launcherUser?.expiresAt
                        val effectiveActivatedAt = subscriptionInfo?.activatedAt ?: launcherUser?.activatedAt
                        val effectiveKey = subscriptionInfo?.redeemedKey ?: launcherUser?.redeemedKey
                        val (expiryDateStr, expiryRemainingStr) = formatExpiry(effectiveExpiresAt)
                        val activatedDateStr = formatTimestamp(effectiveActivatedAt)

                        val tierDisplayName = when (activeTier) {
                            "monthly" -> "VIP 1 BULAN (BULANAN)"
                            "yearly" -> "VIP 1 TAHUN (TAHUNAN)"
                            else -> "VIP LIFETIME (PERMANEN)"
                        }

                        // Outer Double-Bezel Frame
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(themeAccentColor.copy(alpha = 0.15f), Color(0xFF0B0E14))
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.5.dp,
                                    Brush.linearGradient(themeGradient),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(3.dp)
                        ) {
                            // Inner Core Card
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F131C), RoundedCornerShape(9.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Passport Badge Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(themeAccentColor.copy(alpha = 0.2f), CircleShape)
                                                .border(1.dp, themeAccentColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isLifetime) "👑" else if (isYearly) "💎" else "⚡",
                                                fontSize = 11.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = tierDisplayName,
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                text = "Status: Terverifikasi di Server NUX",
                                                color = NuxColors.GrayNeutral,
                                                fontSize = 7.sp
                                            )
                                        }
                                    }

                                    // Live Pulsing Dot Badge
                                    Box(
                                        modifier = Modifier
                                            .background(NuxColors.ForestGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                            .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(NuxColors.MintGreen, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "AKTIF",
                                                color = NuxColors.MintGreen,
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }

                                // Bento Matrix Grid (2x2)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Bento 1: Masa Berlaku / Expire
                                    BentoDetailItem(
                                        icon = Icons.Outlined.Schedule,
                                        label = "MASA BERLAKU",
                                        primaryValue = expiryDateStr,
                                        secondaryValue = expiryRemainingStr,
                                        accentColor = themeAccentColor,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Bento 2: Tanggal Aktivasi
                                    BentoDetailItem(
                                        icon = Icons.Outlined.Verified,
                                        label = "TERAKTIVASI PADA",
                                        primaryValue = activatedDateStr,
                                        secondaryValue = "Server Verified",
                                        accentColor = NuxColors.MintGreen,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Bento 3: Akun Pemilik
                                    BentoDetailItem(
                                        icon = Icons.Outlined.AccountCircle,
                                        label = "AKUN TERTAUT",
                                        primaryValue = "@${launcherUser?.username ?: "User"}",
                                        secondaryValue = "UID: ${launcherUser?.uid?.take(10) ?: "-"}...",
                                        accentColor = Color(0xFF38BDF8),
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Bento 4: License Key Tertaut
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(Color(0xFF141822), RoundedCornerShape(7.dp))
                                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(7.dp))
                                            .padding(6.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Key,
                                                        contentDescription = null,
                                                        tint = themeAccentColor,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "LICENSE KEY",
                                                        color = NuxColors.GrayNeutral,
                                                        fontSize = 6.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                if (!effectiveKey.isNullOrBlank()) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.ContentCopy,
                                                        contentDescription = "Salin",
                                                        tint = NuxColors.GrayNeutral,
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clickable {
                                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                                cm.setPrimaryClip(ClipData.newPlainText("License Key", effectiveKey))
                                                                Toast.makeText(context, "Key disalin ke clipboard", Toast.LENGTH_SHORT).show()
                                                            }
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = maskKey(effectiveKey),
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 8.sp
                                            )
                                            Text(
                                                text = "Platform: Android Client",
                                                color = NuxColors.GrayNeutral,
                                                fontSize = 6.5.sp
                                            )
                                        }
                                    }
                                }

                                // Fasilitas Aktif Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF161C28), RoundedCornerShape(6.dp))
                                        .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = NuxColors.MintGreen,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "7 Fitur Eksklusif VIP Aktif Sepenuhnya",
                                                color = Color.White,
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "100% UNLOCKED",
                                            color = themeAccentColor,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                // Tombol Toggle Perbarui / Ganti Key
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(26.dp)
                                        .background(Color(0xFF1F2432), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                                        .clickable { showRedeemBoxForVip = !showRedeemBoxForVip },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Key,
                                            contentDescription = null,
                                            tint = themeAccentColor,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (showRedeemBoxForVip) "SEMBUNYIKAN FORM TUKAR KEY" else "TUKAR / PERPANJANG LICENSE KEY",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }

                        // Form input redeem opsional untuk VIP yang ingin upgrade ke Lifetime atau tukar key
                        AnimatedVisibility(
                            visible = showRedeemBoxForVip,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            RedeemKeyCard(
                                licenseKeyInput = licenseKeyInput,
                                onKeyChange = { licenseKeyInput = it },
                                isActivating = isActivating,
                                activationError = activationError,
                                activationSuccess = activationSuccess,
                                onActivateClick = {
                                    val user = launcherUser
                                    if (user == null) {
                                        activationError = "Silakan login ke akun NUX terlebih dahulu."
                                        return@RedeemKeyCard
                                    }
                                    val cleanKey = licenseKeyInput.trim().replace("-", "").uppercase()
                                    if (cleanKey.length < 8) {
                                        activationError = "Format key tidak valid."
                                        return@RedeemKeyCard
                                    }

                                    isActivating = true
                                    activationError = null
                                    coroutineScope.launch {
                                        val res = AuthService.activateLicense(user.uid, cleanKey, user.idToken)
                                        isActivating = false
                                        res.fold(
                                            onSuccess = { actResult ->
                                                if (!actResult.success || !actResult.isActivated) {
                                                    activationError = actResult.message.takeIf { it.isNotBlank() } ?: "Key lisensi tidak valid atau sudah dipakai."
                                                } else {
                                                    val updatedUser = user.copy(
                                                        isActivated = true,
                                                        tier = actResult.tier,
                                                        activatedAt = System.currentTimeMillis(),
                                                        expiresAt = actResult.expiresAt,
                                                        redeemedKey = cleanKey
                                                    )
                                                    AccountManager.saveAuthUser(context, updatedUser)
                                                    subscriptionInfo = SubscriptionInfo(
                                                        isActivated = true,
                                                        tier = actResult.tier,
                                                        activatedAt = System.currentTimeMillis(),
                                                        expiresAt = actResult.expiresAt,
                                                        redeemedKey = cleanKey
                                                    )
                                                    activationSuccess = "Selamat! Paket diperbarui ke ${actResult.tier.uppercase()} Member!"
                                                    Toast.makeText(context, "Aktivasi Berhasil! Paket VIP Anda Diperbarui!", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            onFailure = { err ->
                                                activationError = err.message ?: "Aktivasi gagal. Periksa koneksi internet."
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    } else {
                        // ==========================================
                        // STATE B: PENGGUNA FREE (UPGRADE PLANS + KEY)
                        // ==========================================
                        Text(
                            text = "PILIHAN PAKET UPGRADE:",
                            color = NuxColors.GrayNeutral,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 1 Bulan
                            PlanCard(
                                title = "1 BULAN",
                                price = "Rp 5.000",
                                subtext = "Coba fitur",
                                modifier = Modifier.weight(1f),
                                onSelect = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.nuxlauncher.site/upgrade")))
                                    } catch (_: Exception) {}
                                }
                            )

                            // 1 Tahun (Best Value)
                            PlanCard(
                                title = "1 TAHUN",
                                price = "Rp 50.000",
                                subtext = "Hemat 16%",
                                isBestValue = true,
                                modifier = Modifier.weight(1.15f),
                                onSelect = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.nuxlauncher.site/upgrade")))
                                    } catch (_: Exception) {}
                                }
                            )

                            // Lifetime
                            PlanCard(
                                title = "LIFETIME",
                                price = "Rp 125.000",
                                subtext = "Sekali beli",
                                modifier = Modifier.weight(1f),
                                onSelect = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.nuxlauncher.site/upgrade")))
                                    } catch (_: Exception) {}
                                }
                            )
                        }

                        // Key Redemption Box
                        RedeemKeyCard(
                            licenseKeyInput = licenseKeyInput,
                            onKeyChange = { licenseKeyInput = it },
                            isActivating = isActivating,
                            activationError = activationError,
                            activationSuccess = activationSuccess,
                            onActivateClick = {
                                val user = launcherUser
                                if (user == null) {
                                    activationError = "Silakan login ke akun NUX terlebih dahulu."
                                    return@RedeemKeyCard
                                }
                                val cleanKey = licenseKeyInput.trim().replace("-", "").uppercase()
                                if (cleanKey.length < 8) {
                                    activationError = "Format key tidak valid."
                                    return@RedeemKeyCard
                                }

                                isActivating = true
                                activationError = null
                                coroutineScope.launch {
                                    val res = AuthService.activateLicense(user.uid, cleanKey, user.idToken)
                                    isActivating = false
                                    res.fold(
                                        onSuccess = { actResult ->
                                            if (!actResult.success || !actResult.isActivated) {
                                                activationError = actResult.message.takeIf { it.isNotBlank() } ?: "Key lisensi tidak valid atau sudah dipakai."
                                            } else {
                                                val updatedUser = user.copy(
                                                    isActivated = true,
                                                    tier = actResult.tier,
                                                    activatedAt = System.currentTimeMillis(),
                                                    expiresAt = actResult.expiresAt,
                                                    redeemedKey = cleanKey
                                                )
                                                AccountManager.saveAuthUser(context, updatedUser)
                                                subscriptionInfo = SubscriptionInfo(
                                                    isActivated = true,
                                                    tier = actResult.tier,
                                                    activatedAt = System.currentTimeMillis(),
                                                    expiresAt = actResult.expiresAt,
                                                    redeemedKey = cleanKey
                                                )
                                                activationSuccess = "Selamat! Akun Anda kini aktif sebagai ${actResult.tier.uppercase()} Member!"
                                                Toast.makeText(context, "Aktivasi Berhasil! Fitur Premium Terbuka!", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        onFailure = { err ->
                                            activationError = err.message ?: "Aktivasi gagal. Periksa koneksi internet."
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ==========================================
            // 3. FOOTER CLOSE BUTTON
            // ==========================================
            NuxButton(
                onClick = onDismissRequest,
                backgroundColor = Color(0xFF1A1D27),
                borderColor = Color(0x33FFFFFF),
                contentColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
            ) {
                Text("TUTUP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
            }
        }
    }
}

@Composable
private fun BentoDetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    primaryValue: String,
    secondaryValue: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF141822), RoundedCornerShape(7.dp))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(7.dp))
            .padding(6.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = label,
                    color = NuxColors.GrayNeutral,
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = primaryValue,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 8.sp,
                maxLines = 1
            )
            Text(
                text = secondaryValue,
                color = accentColor.copy(alpha = 0.9f),
                fontSize = 6.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RedeemKeyCard(
    licenseKeyInput: String,
    onKeyChange: (String) -> Unit,
    isActivating: Boolean,
    activationError: String?,
    activationSuccess: String?,
    onActivateClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1219), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
            .padding(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131722), RoundedCornerShape(8.dp))
                .padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Key,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AKTIVASI / MASUKKAN LICENSE KEY",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }

            // Key Input Field
            val shape = RoundedCornerShape(7.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(Color(0xFF1A1F2C), shape)
                    .border(1.dp, Color(0x33FFFFFF), shape)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = licenseKeyInput,
                    onValueChange = { raw ->
                        var clean = raw.filter { it.isLetterOrDigit() }.uppercase()
                        if (clean.length > 16) clean = clean.substring(0, 16)
                        val formatted = buildString {
                            for (i in clean.indices) {
                                if (i > 0 && i % 4 == 0) append('-')
                                append(clean[i])
                            }
                        }
                        onKeyChange(formatted)
                    },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                )
                if (licenseKeyInput.isEmpty()) {
                    Text(
                        text = "Contoh: XXXX-XXXX-XXXX-XXXX",
                        color = Color(0xFF52525B),
                        fontSize = 9.5.sp
                    )
                }
            }

            if (!activationError.isNullOrBlank()) {
                Text(
                    text = activationError,
                    color = NuxColors.ErrorRed,
                    fontSize = 8.sp,
                    lineHeight = 11.sp
                )
            }

            if (!activationSuccess.isNullOrBlank()) {
                Text(
                    text = activationSuccess,
                    color = NuxColors.ForestGreen,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            NuxButton(
                onClick = onActivateClick,
                backgroundColor = Color(0xFFF59E0B),
                contentColor = Color.Black,
                enabled = !isActivating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
            ) {
                Text(
                    text = if (isActivating) "MEMVERIFIKASI..." else "AKTIFKAN KEY SEKARANG",
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    subtext: String,
    isBestValue: Boolean = false,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    // Outer Doppelrand
    Box(
        modifier = modifier
            .background(
                if (isBestValue) Color(0xFF241B12) else Color(0xFF10131B),
                RoundedCornerShape(9.dp)
            )
            .border(
                1.dp,
                if (isBestValue) Color(0xFFF59E0B).copy(alpha = 0.6f) else Color(0x22FFFFFF),
                RoundedCornerShape(9.dp)
            )
            .padding(1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isBestValue) Color(0xFF191410) else Color(0xFF141822),
                    RoundedCornerShape(7.5.dp)
                )
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isBestValue) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF59E0B), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text("POPULER", color = Color.Black, fontSize = 6.5.sp, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(title, color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
            Text(price, color = if (isBestValue) Color(0xFFFBBF24) else Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(subtext, color = NuxColors.GrayNeutral, fontSize = 7.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(if (isBestValue) Color(0xFFF59E0B) else Color(0xFF27272A), RoundedCornerShape(5.dp))
                .clickable { onSelect() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Beli", color = if (isBestValue) Color.Black else Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = if (isBestValue) Color.Black else Color.White,
                        modifier = Modifier.size(9.dp)
                    )
                }
            }
        }
    }
}
