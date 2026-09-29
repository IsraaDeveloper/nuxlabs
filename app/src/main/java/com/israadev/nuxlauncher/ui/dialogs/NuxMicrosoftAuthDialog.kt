package com.israadev.nuxlauncher.ui.dialogs

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.israadev.nuxlauncher.core.account.microsoft.MicrosoftAuthService
import com.israadev.nuxlauncher.core.account.microsoft.MicrosoftDeviceCode
import com.israadev.nuxlauncher.core.models.UserAccount
import com.israadev.nuxlauncher.ui.components.NuxDialog
import com.israadev.nuxlauncher.ui.theme.NuxColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * In-App Dialog WebView untuk Login Microsoft / Xbox Live.
 * Mengikuti sistem arsitektur Zalith Launcher: User tidak perlu beralih ke browser eksternal,
 * sehingga koneksi polling tetap stabil di foreground tanpa risiko process suspension oleh OS Android.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NuxMicrosoftAuthDialog(
    deviceCode: MicrosoftDeviceCode,
    onDismiss: () -> Unit,
    onSuccess: (UserAccount) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isCancelled by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Menunggu otorisasi akun di halaman web Microsoft...") }
    var isWebLoading by remember { mutableStateOf(true) }
    var currentWebUrl by remember { mutableStateOf("https://microsoft.com/link?otc=${deviceCode.userCode}") }

    val webViewHolder = remember { mutableStateOf<WebView?>(null) }

    // Salin kode otomatis saat dialog pertama kali muncul
    LaunchedEffect(deviceCode.userCode) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Microsoft User Code", deviceCode.userCode))
        Toast.makeText(context, "Kode '${deviceCode.userCode}' telah disalin!", Toast.LENGTH_SHORT).show()
    }

    // Polling Coroutine yang aktif selama dialog terbuka
    LaunchedEffect(deviceCode) {
        withContext(Dispatchers.IO) {
            val tokenResult = MicrosoftAuthService.pollForToken(
                deviceCode = deviceCode,
                isCancelled = { isCancelled },
                onStatusUpdate = { newStatus ->
                    scope.launch { statusText = newStatus }
                }
            )

            tokenResult.fold(
                onSuccess = { (msAccessToken, msRefreshToken) ->
                    scope.launch { statusText = "Token diterima! Memproses autentikasi Minecraft & Xbox..." }
                    val mcAuthResult = MicrosoftAuthService.completeMinecraftAuth(
                        msAccessToken = msAccessToken,
                        msRefreshToken = msRefreshToken,
                        onStatusUpdate = { stepStatus ->
                            scope.launch { statusText = stepStatus }
                        }
                    )

                    mcAuthResult.fold(
                        onSuccess = { account ->
                            withContext(Dispatchers.Main) {
                                onSuccess(account)
                            }
                        },
                        onFailure = { err ->
                            if (err !is CancellationException && !isCancelled) {
                                withContext(Dispatchers.Main) {
                                    onError(err.message ?: "Gagal memproses autentikasi Minecraft.")
                                }
                            }
                        }
                    )
                },
                onFailure = { err ->
                    if (err !is CancellationException && !isCancelled) {
                        withContext(Dispatchers.Main) {
                            onError(err.message ?: "Otorisasi Microsoft gagal.")
                        }
                    }
                }
            )
        }
    }

    // Bersihkan resource saat dialog ditutup
    DisposableEffect(Unit) {
        onDispose {
            isCancelled = true
            webViewHolder.value?.apply {
                stopLoading()
                loadUrl("about:blank")
                clearHistory()
                removeAllViews()
                destroy()
            }
            webViewHolder.value = null
        }
    }

    NuxDialog(
        onDismissRequest = {
            isCancelled = true
            onDismiss()
        },
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .fillMaxHeight(0.94f),
        fillMaxHeight = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // TOP HEADER BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF0078D4).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF0078D4).copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = "Microsoft",
                            tint = Color(0xFF0078D4),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "LOGIN AKUN MICROSOFT",
                            color = NuxColors.DarkGray,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Masuk dan konfirmasi langsung di jendela aman ini",
                            color = NuxColors.GrayNeutral,
                            fontSize = 9.sp
                        )
                    }
                }

                // Interactive Code Badge & Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Badge Kode User
                    Row(
                        modifier = Modifier
                            .background(NuxColors.SurfaceInput, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF0078D4).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("Microsoft User Code", deviceCode.userCode))
                                Toast.makeText(context, "Kode '${deviceCode.userCode}' disalin!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "KODE:",
                            color = NuxColors.GrayNeutral,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = deviceCode.userCode,
                            color = Color(0xFF0078D4),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        )
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Salin Kode",
                            tint = Color(0xFF0078D4),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // Tombol Buka di Browser Eksternal (Cadangan jika user butuh)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(NuxColors.SurfaceInput, RoundedCornerShape(8.dp))
                            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                runCatching {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentWebUrl))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = "Buka di Browser Eksternal",
                            tint = NuxColors.GrayNeutral,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Tombol Tutup
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(NuxColors.SurfaceInput, CircleShape)
                            .border(1.dp, NuxColors.CardBorder, CircleShape)
                            .clickable {
                                isCancelled = true
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Tutup",
                            tint = NuxColors.GrayNeutral,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Web Loading Progress Bar
            AnimatedVisibility(visible = isWebLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = Color(0xFF0078D4),
                    trackColor = NuxColors.SurfaceInput
                )
            }

            // WEBVIEW CONTAINER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(10.dp))
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                cacheMode = WebSettings.LOAD_NO_CACHE
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isWebLoading = true
                                    url?.let { currentWebUrl = it }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isWebLoading = false
                                    url?.let { currentWebUrl = it }
                                }
                            }

                            loadUrl("https://microsoft.com/link?otc=${deviceCode.userCode}")
                            webViewHolder.value = this
                        }
                    }
                )
            }

            // BOTTOM STATUS FOOTER
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NuxColors.SurfaceInput, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(13.dp),
                    color = Color(0xFF0078D4),
                    strokeWidth = 1.8.dp
                )

                Text(
                    text = statusText,
                    color = NuxColors.DarkGray,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "Klik 'Lanjutkan' di halaman atas setelah memasukkan kode",
                    color = NuxColors.GrayNeutral,
                    fontSize = 9.sp
                )
            }
        }
    }
}
