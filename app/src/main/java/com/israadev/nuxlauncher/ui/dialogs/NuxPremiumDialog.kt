package com.israadev.nuxlauncher.ui.dialogs

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.OpenInNew
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
import com.israadev.nuxlauncher.ui.components.NuxBadge
import com.israadev.nuxlauncher.ui.components.NuxButton
import com.israadev.nuxlauncher.ui.components.NuxCard
import com.israadev.nuxlauncher.ui.components.NuxDialog
import com.israadev.nuxlauncher.ui.theme.NuxColors
import kotlinx.coroutines.launch

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

    val isUserPremium = launcherUser?.isActivated == true

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
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFF59E0B).copy(alpha = 0.3f), Color.Transparent)
                                ),
                                CircleShape
                            )
                            .border(1.dp, Color(0xFFF59E0B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NUX LAUNCHER PREMIUM",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            NuxBadge(
                                text = if (isUserPremium) "VIP ACTIVE" else "UPGRADE",
                                backgroundColor = if (isUserPremium) NuxColors.ForestGreen.copy(alpha = 0.25f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                                textColor = if (isUserPremium) NuxColors.MintGreen else Color(0xFFFBBF24)
                            )
                        }
                        Text(
                            text = "Buka 7 fitur eksklusif, Voice Room mabar, dan slot tanpa batas",
                            color = NuxColors.GrayNeutral,
                            fontSize = 8.5.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(NuxColors.SurfaceInput, CircleShape)
                        .border(1.dp, NuxColors.CardBorder, CircleShape)
                        .clickable { onDismissRequest() },
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

            // Body Content (Two column layout for landscape screens)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // LEFT COLUMN: 7 Premium Features Showcase
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!initialPrompt.isNullOrBlank()) {
                        NuxCard(
                            backgroundColor = Color(0xFF1E1710),
                            borderColor = Color(0xFFF59E0B).copy(alpha = 0.4f),
                            cornerRadius = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = initialPrompt,
                                color = Color(0xFFFBBF24),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Text(
                        text = "7 FITUR EKSKLUSIF NUX PREMIUM:",
                        color = NuxColors.GrayNeutral,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )

                    PREMIUM_FEATURES.forEach { item ->
                        NuxCard(
                            backgroundColor = NuxColors.SurfaceInput,
                            borderColor = NuxColors.CardBorder,
                            cornerRadius = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.icon, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
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

                // RIGHT COLUMN: Pricing Plans & Key Redemption
                Column(
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pricing Options
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
                    NuxCard(
                        backgroundColor = Color(0xFF11141C),
                        borderColor = Color(0x26FFFFFF),
                        cornerRadius = 10.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
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
                                    text = "SUDAH PUNYA LICENSE KEY?",
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
                                    .background(Color(0xFF161B26), shape)
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
                                        licenseKeyInput = formatted
                                        activationError = null
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
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            if (!activationError.isNullOrBlank()) {
                                Text(
                                    text = activationError!!,
                                    color = NuxColors.ErrorRed,
                                    fontSize = 8.sp,
                                    lineHeight = 11.sp
                                )
                            }

                            if (!activationSuccess.isNullOrBlank()) {
                                Text(
                                    text = activationSuccess!!,
                                    color = NuxColors.ForestGreen,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            NuxButton(
                                onClick = {
                                    val user = launcherUser
                                    if (user == null) {
                                        activationError = "Silakan login ke akun NUX terlebih dahulu."
                                        return@NuxButton
                                    }
                                    val cleanKey = licenseKeyInput.trim().replace("-", "").uppercase()
                                    if (cleanKey.length < 8) {
                                        activationError = "Format key tidak valid."
                                        return@NuxButton
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
                                                        tier = actResult.tier
                                                    )
                                                    AccountManager.saveAuthUser(context, updatedUser)
                                                    activationSuccess = "Selamat! Akun Anda kini aktif sebagai ${actResult.tier.uppercase()} Member!"
                                                    Toast.makeText(context, "Aktivasi Berhasil! Fitur Premium Terbuka!", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            onFailure = { err ->
                                                activationError = err.message ?: "Aktivasi gagal. Periksa koneksi internet."
                                            }
                                        )
                                    }
                                },
                                backgroundColor = Color(0xFFF59E0B),
                                contentColor = Color.Black,
                                enabled = !isActivating,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                            ) {
                                Text(
                                    text = if (isActivating) "MEMVERIFIKASI..." else "AKTIFKAN KEY SEKARANG",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.5.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Footer Close Button
            NuxButton(
                onClick = onDismissRequest,
                backgroundColor = NuxColors.SurfaceInput,
                contentColor = NuxColors.DarkGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
            ) {
                Text("TUTUP", fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
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
    NuxCard(
        backgroundColor = if (isBestValue) Color(0xFF1E1710) else NuxColors.SurfaceInput,
        borderColor = if (isBestValue) Color(0xFFF59E0B).copy(alpha = 0.5f) else NuxColors.CardBorder,
        cornerRadius = 8.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
