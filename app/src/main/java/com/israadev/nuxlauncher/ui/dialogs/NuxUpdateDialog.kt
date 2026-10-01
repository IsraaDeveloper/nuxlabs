package com.israadev.nuxlauncher.ui.dialogs

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.israadev.nuxlauncher.core.update.AndroidUpdateInfo
import com.israadev.nuxlauncher.core.update.UpdateManager
import com.israadev.nuxlauncher.ui.components.NuxButton
import com.israadev.nuxlauncher.ui.components.NuxCard
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes

@Composable
fun NuxUpdateDialog(
    updateInfo: AndroidUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val formattedSize = if (updateInfo.fileSize > 0) {
        String.format("%.1f MB", updateInfo.fileSize / (1024.0 * 1024.0))
    } else {
        "~380 MB"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .padding(horizontal = 24.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            NuxCard(
                modifier = Modifier
                    .width(460.dp)
                    .wrapContentHeight(),
                backgroundColor = NuxColors.SurfaceElevated,
                borderColor = Color(0x33FFFFFF),
                cornerRadius = NuxSizes.CornerRadiusLarge,
                fillMaxHeight = false
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(
                                        if (updateInfo.isUpdateAvailable) NuxColors.LightGreen else Color(0x2210B981)
                                    )
                                    .border(
                                        1.dp,
                                        if (updateInfo.isUpdateAvailable) NuxColors.ForestGreen else Color(0x4410B981),
                                        RoundedCornerShape(7.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (updateInfo.isUpdateAvailable) Icons.Outlined.SystemUpdate else Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = NuxColors.ForestGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = if (updateInfo.isUpdateAvailable) "PEMBARUAN TERSEDIA" else "LAUNCHER TERBARU",
                                    color = if (updateInfo.isUpdateAvailable) NuxColors.ForestGreen else Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (updateInfo.isUpdateAvailable) "Versi baru siap diunduh" else "Aplikasi sudah menggunakan versi terkini",
                                    color = NuxColors.GrayNeutral,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }

                        // Close button
                        val closeShape = RoundedCornerShape(7.dp)
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(closeShape)
                                .background(NuxColors.SurfaceInput, closeShape)
                                .border(1.dp, NuxColors.CardBorder, closeShape)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Version Info Grid Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NuxColors.SurfaceInput, RoundedCornerShape(8.dp))
                            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "VERSI SERVER",
                                    color = NuxColors.GrayNeutral,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = "v${updateInfo.version}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (updateInfo.isUpdateAvailable) NuxColors.ForestGreen.copy(alpha = 0.2f) else Color(0x3310B981))
                                            .border(1.dp, if (updateInfo.isUpdateAvailable) NuxColors.ForestGreen else Color(0x5510B981), RoundedCornerShape(3.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (updateInfo.isUpdateAvailable) "NEW" else "ACTIVE",
                                            color = NuxColors.ForestGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.5.sp
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "VERSI TERPASANG",
                                    color = NuxColors.GrayNeutral,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = "v${updateInfo.localVersion}",
                                    color = Color(0xFFE4E4E7),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "UKURAN APK",
                                    color = NuxColors.GrayNeutral,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = formattedSize,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Changelog Section
                    Text(
                        text = "CATATAN PEMBARUAN",
                        color = NuxColors.GrayNeutral,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val scrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 55.dp, max = 80.dp)
                            .background(Color(0xFF0A0C0F), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = updateInfo.changelog,
                            color = Color(0xFFD4D4D8),
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NuxButton(
                            onClick = onDismiss,
                            backgroundColor = Color(0xFF1A1D27),
                            borderColor = Color(0x33FFFFFF),
                            contentColor = Color.White,
                            cornerRadius = 8.dp,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (updateInfo.isUpdateAvailable) "NANTI" else "TUTUP",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        NuxButton(
                            onClick = {
                                UpdateManager.openDownloadUrl(context, updateInfo.downloadUrl)
                                onDismiss()
                            },
                            backgroundColor = NuxColors.ForestGreen,
                            contentColor = Color.White,
                            cornerRadius = 8.dp,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (updateInfo.isUpdateAvailable) "UNDUH PEMBARUAN" else "UNDUH ULANG APK",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
