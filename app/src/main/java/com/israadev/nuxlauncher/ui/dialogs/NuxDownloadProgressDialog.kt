package com.israadev.nuxlauncher.ui.dialogs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.ui.components.NuxDialog
import com.israadev.nuxlauncher.ui.theme.NuxColors

@Composable
fun NuxDownloadProgressDialog(
    instanceName: String,
    progress: Float,
    message: String,
    onDismissRequest: () -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 250),
        label = "dlProgress"
    )

    val outerShape = RoundedCornerShape(22.dp)
    val cardShape = RoundedCornerShape(14.dp)

    NuxDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(0.62f)
    ) {
        // Outer Shell Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(outerShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF141923),
                            Color(0xFF0F121A)
                        )
                    )
                )
                .border(1.dp, Color(0x3310B981), outerShape)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = NuxColors.MintGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CORE ASSET PIPELINE",
                                color = Color(0xFF71717A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Mengunduh Game",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = (-0.3).sp
                            )
                        }
                    }

                    // Percentage Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.18f))
                            .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            color = NuxColors.MintGreen,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Instance Name Badge Core
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(Color(0xFF0B0D13))
                        .border(1.dp, Color(0x1AFFFFFF), cardShape)
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(NuxColors.ForestGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = instanceName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "MOJANG ASSETS",
                            color = Color(0xFF71717A),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // High-End Cyber Gradient Progress Track (Double Bezel)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF090B0F), CircleShape)
                        .border(1.dp, Color(0x2EFFFFFF), CircleShape)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = animatedProgress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF059669),
                                        Color(0xFF10B981),
                                        Color(0xFF34D399),
                                        Color(0xFF38BDF8)
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Current Action / File Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = NuxColors.ForestGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message.ifBlank { "Menyiapkan dependensi game & checksum..." },
                        color = Color(0xFFA1A1AA),
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
