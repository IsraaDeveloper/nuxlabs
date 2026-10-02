package com.israadev.nuxlauncher.ui.screens

import android.content.Intent
import android.net.Uri
import com.israadev.nuxlauncher.core.network.NuxConfig
import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.R
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.auth.AuthService
import com.israadev.nuxlauncher.core.auth.AuthUser
import com.israadev.nuxlauncher.core.auth.OtpCooldownManager
import com.israadev.nuxlauncher.ui.components.NuxButton
import com.israadev.nuxlauncher.ui.components.NuxCard
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes
import com.israadev.nuxlauncher.ui.theme.resp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN,
    REGISTER,
    VERIFY_REGISTER_OTP,
    ACTIVATE_KEY,
    FORGOT_PASSWORD_EMAIL,
    FORGOT_PASSWORD_RESET
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(AuthMode.LOGIN) }

    // Form states
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var licenseKey by remember { mutableStateOf("") }

    // Activation pending user
    var pendingUser by remember { mutableStateOf<AuthUser?>(null) }

    // UI feedback
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Monotonic Anti-Bypass Countdown timer for OTP
    var countdown by remember { mutableIntStateOf(OtpCooldownManager.getRemainingSeconds()) }

    LaunchedEffect(Unit) {
        OtpCooldownManager.init(context)
        while (isActive) {
            countdown = OtpCooldownManager.getRemainingSeconds()
            delay(500)
        }
    }

    // Two-Column Landscape Console Layout
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(NuxColors.Background)
            .padding((10.dp).resp()),
        horizontalArrangement = Arrangement.spacedBy((10.dp).resp())
    ) {
        // =========================================================================
        // LEFT PANE: BRANDING & GAMING SHOWCASE (38% Width)
        // =========================================================================
        val leftPanelShape = RoundedCornerShape((16.dp).resp())
        Box(
            modifier = Modifier
                .weight(0.38f)
                .fillMaxHeight()
                .clip(leftPanelShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF141722), Color(0xFF0C0E14))
                    )
                )
                .border(1.dp, Color(0x2EFFFFFF), leftPanelShape)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Wallpaper with gradient overlay
                Image(
                    painter = painterResource(id = R.drawable.mc_hero_bg),
                    contentDescription = "Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.30f
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF0C0E14).copy(alpha = 0.8f),
                                    Color(0xFF0C0E14)
                                )
                            )
                        )
                )

                // Left Content Column
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar: App Badge & Online Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.16f))
                                .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "NUX CLIENT",
                                color = NuxColors.MintGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(NuxColors.ForestGreen, CircleShape)
                            )
                            Text(
                                text = "ONLINE GATEWAY",
                                color = Color(0xFFA1A1AA),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Middle: Title & Gaming Highlights
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "MINECRAFT JAVA",
                            color = NuxColors.MintGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )

                        Text(
                            text = "NUX LAUNCHER",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.3).sp,
                            lineHeight = 28.sp
                        )

                        Text(
                            text = "Mainkan Minecraft Java Edition di perangkat Android kamu dengan performa maksimal, kontrol kustom, dan room suara.",
                            color = Color(0xFFD4D4D8),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // Bottom: Feature Badges
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GamingFeaturePill(text = "FABRIC / FORGE", icon = Icons.Default.Extension, modifier = Modifier.weight(1f))
                            GamingFeaturePill(text = "LIVEKIT VOICE", icon = Icons.Default.Mic, modifier = Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GamingFeaturePill(text = "CUSTOM GUI", icon = Icons.Default.TouchApp, modifier = Modifier.weight(1f))
                            GamingFeaturePill(text = "RESMI & AMAN", icon = Icons.Default.Shield, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // =========================================================================
        // RIGHT PANE: COMPACT INTERACTIVE AUTH CONSOLE (62% Width)
        // =========================================================================
        val rightPanelShape = RoundedCornerShape((16.dp).resp())
        Box(
            modifier = Modifier
                .weight(0.62f)
                .fillMaxHeight()
                .clip(rightPanelShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF141722), Color(0xFF0C0E14))
                    )
                )
                .border(1.dp, Color(0x2EFFFFFF), rightPanelShape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = (13.dp).resp(), vertical = (10.dp).resp()),
                verticalArrangement = Arrangement.spacedBy((7.dp).resp())
            ) {
                // Top Segmented Switcher (Only in Login & Register)
                if (mode == AuthMode.LOGIN || mode == AuthMode.REGISTER) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((36.dp).resp())
                            .clip(RoundedCornerShape((9.dp).resp()))
                            .background(Color(0xFF090B10))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape((9.dp).resp()))
                            .padding((3.dp).resp())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isLogin = mode == AuthMode.LOGIN
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isLogin) Color(0xFF181C26) else Color.Transparent)
                                    .border(
                                        width = if (isLogin) 1.dp else 0.dp,
                                        color = if (isLogin) Color(0x33FFFFFF) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        errorMessage = ""
                                        successMessage = ""
                                        mode = AuthMode.LOGIN
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "MASUK KE AKUN",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isLogin) FontWeight.Black else FontWeight.SemiBold,
                                    color = if (isLogin) Color.White else Color(0xFF71717A)
                                )
                            }

                            val isRegister = mode == AuthMode.REGISTER
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isRegister) Color(0xFF181C26) else Color.Transparent)
                                    .border(
                                        width = if (isRegister) 1.dp else 0.dp,
                                        color = if (isRegister) Color(0x33FFFFFF) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        errorMessage = ""
                                        successMessage = ""
                                        mode = AuthMode.REGISTER
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "DAFTAR BARU",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isRegister) FontWeight.Black else FontWeight.SemiBold,
                                    color = if (isRegister) Color.White else Color(0xFF71717A)
                                )
                            }
                        }
                    }
                }

                // Error Banner
                if (errorMessage.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF261017))
                            .border(1.dp, Color(0xFFF43F5E).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(16.dp))
                            Text(text = errorMessage, color = Color(0xFFFDA4AF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Success Banner
                if (successMessage.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F221B))
                            .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = NuxColors.ForestGreen, modifier = Modifier.size(16.dp))
                            Text(text = successMessage, color = NuxColors.MintGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Important Reminder to check SPAM folder when entering OTP
                if (mode == AuthMode.VERIFY_REGISTER_OTP || mode == AuthMode.FORGOT_PASSWORD_RESET) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF261D0C))
                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                            Text(
                                text = "Kode belum masuk ke Inbox? Pastikan untuk memeriksa folder SPAM / JUNK email Anda. Harap tunggu cooldown sebelum meminta ulang.",
                                color = Color(0xFFFCD34D),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // SUB-SCREENS
                when (mode) {
                    // ==========================================
                    // 1. LOGIN
                    // ==========================================
                    AuthMode.LOGIN -> {
                        AuthInputField(
                            value = email,
                            onValueChange = { email = it; errorMessage = "" },
                            label = "Alamat Email",
                            placeholder = "nama@gmail.com",
                            leadingIcon = Icons.Default.Email
                        )

                        AuthInputField(
                            value = password,
                            onValueChange = { password = it; errorMessage = "" },
                            label = "Kata Sandi",
                            placeholder = "Minimal 6 karakter",
                            leadingIcon = Icons.Default.Lock,
                            isPassword = true,
                            isPasswordVisible = isPasswordVisible,
                            onPasswordToggle = { isPasswordVisible = !isPasswordVisible }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Lupa Kata Sandi?",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NuxColors.ForestGreen,
                                modifier = Modifier.clickable {
                                    errorMessage = ""
                                    successMessage = ""
                                    mode = AuthMode.FORGOT_PASSWORD_EMAIL
                                }
                            )
                        }

                        NuxButton(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Email dan kata sandi wajib diisi."
                                    return@NuxButton
                                }
                                isLoading = true
                                errorMessage = ""
                                coroutineScope.launch {
                                    val res = AuthService.login(email, password)
                                    isLoading = false
                                    res.fold(
                                        onSuccess = { loginResult ->
                                            AccountManager.saveAuthUser(context, loginResult.user)
                                            if (AccountManager.accounts.value.isEmpty()) {
                                                AccountManager.addAccount(
                                                    context,
                                                    AccountManager.createGuestAccount(loginResult.user.username)
                                                )
                                            }
                                            onAuthSuccess()
                                        },
                                        onFailure = { err ->
                                            errorMessage = err.message ?: "Gagal masuk. Periksa kembali akun Anda."
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = NuxColors.ForestGreen,
                            contentColor = Color.White,
                            enabled = !isLoading
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.SportsEsports, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (isLoading) "MEMVERIFIKASI..." else "MASUK & MAINKAN",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Tombol Masuk Langsung Sebagai Guest (Free)
                        NuxButton(
                            onClick = {
                                val guestUser = AuthUser(
                                    uid = "guest_" + java.util.UUID.randomUUID().toString().take(8),
                                    email = "",
                                    username = "NuxPlayer",
                                    photoURL = "",
                                    isActivated = false,
                                    tier = "free"
                                )
                                AccountManager.saveAuthUser(context, guestUser)
                                if (AccountManager.accounts.value.isEmpty()) {
                                    AccountManager.addAccount(
                                        context,
                                        AccountManager.createGuestAccount("NuxPlayer")
                                    )
                                }
                                onAuthSuccess()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0xFF181C26),
                            contentColor = NuxColors.MintGreen,
                            enabled = !isLoading
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NuxColors.MintGreen, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "MAIN LANGSUNG SEBAGAI GUEST (FREE)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = NuxColors.MintGreen
                                )
                            }
                        }
                    }

                    // ==========================================
                    // 2. REGISTER
                    // ==========================================
                    AuthMode.REGISTER -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                AuthInputField(
                                    value = username,
                                    onValueChange = { username = it; errorMessage = "" },
                                    label = "Username Player",
                                    placeholder = "NuxGamer",
                                    leadingIcon = Icons.Default.Person
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                AuthInputField(
                                    value = email,
                                    onValueChange = { email = it; errorMessage = "" },
                                    label = "Alamat Email",
                                    placeholder = "nama@gmail.com",
                                    leadingIcon = Icons.Default.Email
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                AuthInputField(
                                    value = password,
                                    onValueChange = { password = it; errorMessage = "" },
                                    label = "Kata Sandi",
                                    placeholder = "Min. 6 karakter",
                                    leadingIcon = Icons.Default.Lock,
                                    isPassword = true,
                                    isPasswordVisible = isPasswordVisible,
                                    onPasswordToggle = { isPasswordVisible = !isPasswordVisible }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                AuthInputField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it; errorMessage = "" },
                                    label = "Konfirmasi Sandi",
                                    placeholder = "Ulangi sandi",
                                    leadingIcon = Icons.Default.Lock,
                                    isPassword = true,
                                    isPasswordVisible = isPasswordVisible,
                                    onPasswordToggle = { isPasswordVisible = !isPasswordVisible }
                                )
                            }
                        }

                        NuxButton(
                            onClick = {
                                val cleanUser = username.trim()
                                val cleanEmail = email.trim()
                                if (cleanUser.isEmpty()) {
                                    errorMessage = "Username wajib diisi."
                                    return@NuxButton
                                }
                                if (!cleanEmail.contains("@")) {
                                    errorMessage = "Format email tidak valid."
                                    return@NuxButton
                                }
                                if (password.length < 6) {
                                    errorMessage = "Kata sandi minimal 6 karakter."
                                    return@NuxButton
                                }
                                if (password != confirmPassword) {
                                    errorMessage = "Konfirmasi kata sandi tidak cocok."
                                    return@NuxButton
                                }

                                isLoading = true
                                errorMessage = ""
                                coroutineScope.launch {
                                    val res = AuthService.register(cleanEmail, cleanUser, password)
                                    isLoading = false
                                    res.fold(
                                        onSuccess = { regResult ->
                                            AccountManager.saveAuthUser(context, regResult.user)
                                            if (AccountManager.accounts.value.isEmpty()) {
                                                AccountManager.addAccount(
                                                    context,
                                                    AccountManager.createGuestAccount(regResult.user.username)
                                                )
                                            }
                                            onAuthSuccess()
                                        },
                                        onFailure = { err ->
                                            errorMessage = err.message ?: "Gagal mendaftarkan akun."
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = NuxColors.ForestGreen,
                            contentColor = Color.White,
                            enabled = !isLoading
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (isLoading) "MENDAFTARKAN AKUN..." else "DAFTAR AKUN BARU",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // ==========================================
                    // 3. VERIFY REGISTER OTP (DEPRECATED - BYPASS)
                    // ==========================================
                    AuthMode.VERIFY_REGISTER_OTP -> {
                        LaunchedEffect(Unit) {
                            mode = AuthMode.REGISTER
                        }
                    }

                    // ==========================================
                    // 4. MANDATORY LICENSE ACTIVATION GATE
                    // ==========================================
                    AuthMode.ACTIVATE_KEY -> {
                        val user = pendingUser ?: AccountManager.launcherUser.value

                        // Compact Account Pill
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF141926), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0x334ADE80), RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "AKUN: ${user?.username ?: "User"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF4ADE80)
                                )
                                Text(
                                    text = user?.email ?: email,
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Text(
                                text = "Ganti Akun",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.clickable {
                                    AccountManager.logout(context)
                                    pendingUser = null
                                    mode = AuthMode.LOGIN
                                }
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Key Lisensi NUX Android",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Format: XXXX-XXXX-XXXX-XXXX (16 karakter)",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        // Formatted Monospace Key Input
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F131D), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            if (licenseKey.isEmpty()) {
                                Text(
                                    text = "PASTE KEY LISENSI KAMU DI SINI",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            BasicTextField(
                                value = licenseKey,
                                onValueChange = { input ->
                                    val cleaned = input.filter { it.isLetterOrDigit() || it == '-' }.uppercase()
                                    licenseKey = cleaned
                                    errorMessage = ""
                                },
                                textStyle = TextStyle(
                                    color = Color(0xFF4ADE80),
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                cursorBrush = SolidColor(Color(0xFF4ADE80)),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Buy Key Button
                            NuxButton(
                                onClick = {
                                    try {
                                        val buyUrl = NuxConfig.BUY_KEY_URL
                                        if (buyUrl.isNotBlank()) {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(buyUrl))
                                            context.startActivity(intent)
                                        }
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.weight(0.42f),
                                backgroundColor = Color(0xFF161B26)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                                    Text("Beli Key", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                }
                            }

                            // Activate Button
                            NuxButton(
                                onClick = {
                                    val uid = user?.uid ?: ""
                                    val rawKey = licenseKey.trim().uppercase()
                                    val key = if (!rawKey.contains("-") && rawKey.length == 16) {
                                        "${rawKey.substring(0, 4)}-${rawKey.substring(4, 8)}-${rawKey.substring(8, 12)}-${rawKey.substring(12, 16)}"
                                    } else {
                                        rawKey
                                    }
                                    if (uid.isEmpty()) {
                                        errorMessage = "Sesi akun tidak valid. Silakan login kembali."
                                        mode = AuthMode.LOGIN
                                        return@NuxButton
                                    }
                                    if (key.length < 8) {
                                        errorMessage = "Format key tidak valid."
                                        return@NuxButton
                                    }

                                    val targetUser = user ?: AuthUser(uid = uid, email = email, username = username)

                                    isLoading = true
                                    errorMessage = ""
                                    coroutineScope.launch {
                                        val token = user?.idToken?.takeIf { it.isNotBlank() }
                                            ?: AccountManager.getActiveUser()?.idToken?.takeIf { it.isNotBlank() }
                                        val res = AuthService.activateLicense(uid, key, token)
                                        isLoading = false
                                        res.fold(
                                            onSuccess = { actResult ->
                                                if (!actResult.success || !actResult.isActivated) {
                                                    errorMessage = if (actResult.message.isNotBlank()) {
                                                        actResult.message
                                                    } else {
                                                        "Key lisensi tidak valid atau sudah digunakan oleh akun lain."
                                                    }
                                                    return@fold
                                                }
                                                val sessionPair = AuthService.requestSessionToken(uid)
                                                val activatedUser = targetUser.copy(
                                                    isActivated = true,
                                                    tier = actResult.tier,
                                                    idToken = sessionPair?.first ?: targetUser.idToken,
                                                    refreshToken = sessionPair?.second ?: targetUser.refreshToken
                                                )
                                                AccountManager.saveAuthUser(context, activatedUser)
                                                onAuthSuccess()
                                            },
                                            onFailure = { err ->
                                                errorMessage = err.message ?: "Aktivasi gagal. Periksa kembali key kamu."
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.weight(0.58f),
                                backgroundColor = NuxColors.ForestGreen,
                                enabled = !isLoading
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    Text(
                                        text = if (isLoading) "AKTIVASI..." else "AKTIFKAN SEKARANG",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // ==========================================
                    // 5. FORGOT PASSWORD (EMAIL)
                    // ==========================================
                    // 5. FORGOT PASSWORD (EMAIL RESET VIA FIREBASE)
                    // ==========================================
                    AuthMode.FORGOT_PASSWORD_EMAIL -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clickable { mode = AuthMode.LOGIN }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF94A3B8), modifier = Modifier.size(15.dp))
                            Text("Kembali ke Login", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        }

                        Text(
                            text = "Reset Kata Sandi Akun",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        Text(
                            text = "Masukkan alamat email yang terdaftar. Firebase akan mengirimkan tautan (link) resmi ke email kamu untuk membuat kata sandi baru tanpa perlu kode OTP.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 16.sp
                        )

                        AuthInputField(
                            value = email,
                            onValueChange = { email = it; errorMessage = "" },
                            label = "Email Akun Terdaftar",
                            placeholder = "email_kamu@gmail.com",
                            leadingIcon = Icons.Default.Email
                        )

                        NuxButton(
                            onClick = {
                                val cleanEmail = email.trim()
                                if (!cleanEmail.contains("@")) {
                                    errorMessage = "Format email tidak valid."
                                    return@NuxButton
                                }

                                isLoading = true
                                errorMessage = ""
                                coroutineScope.launch {
                                    val res = AuthService.sendPasswordResetEmail(cleanEmail)
                                    isLoading = false
                                    res.fold(
                                        onSuccess = { msg ->
                                            successMessage = msg
                                            mode = AuthMode.LOGIN
                                        },
                                        onFailure = { err ->
                                            errorMessage = err.message ?: "Gagal mengirim link reset password."
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = NuxColors.ForestGreen,
                            enabled = !isLoading
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (isLoading) "MENGIRIM LINK..." else "KIRIM LINK RESET PASSWORD",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // ==========================================
                    // 6. FORGOT PASSWORD (DEPRECATED - BYPASS)
                    // ==========================================
                    AuthMode.FORGOT_PASSWORD_RESET -> {
                        LaunchedEffect(Unit) {
                            mode = AuthMode.LOGIN
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// HELPER COMPOSABLES
// --------------------------------------------------------------------------

@Composable
private fun GamingFeaturePill(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141824))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NuxColors.MintGreen,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = text,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AuthInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFA1A1AA),
            letterSpacing = 0.8.sp
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0C0E14))
                .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = NuxColors.MintGreen,
                    modifier = Modifier.size(16.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color(0xFF52525B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        cursorBrush = SolidColor(NuxColors.ForestGreen),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isPassword && onPasswordToggle != null) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password",
                        tint = Color(0xFFA1A1AA),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onPasswordToggle() }
                    )
                }
            }
        }
    }
}

@Composable
private fun OtpBoxes(
    otp: String,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF090B10))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 6) {
                val digit = otp.getOrNull(i)?.toString() ?: ""
                val isFilled = digit.isNotEmpty()

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isFilled) Color(0xFF10B981).copy(alpha = 0.16f) else Color(0xFF141720)
                        )
                        .border(
                            1.dp,
                            if (isFilled) NuxColors.ForestGreen else Color(0x22FFFFFF),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isFilled) NuxColors.MintGreen else Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Invisible input layer capturing keyboard
        BasicTextField(
            value = otp,
            onValueChange = onOtpChange,
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            singleLine = true,
            modifier = Modifier.matchParentSize()
        )
    }
}
