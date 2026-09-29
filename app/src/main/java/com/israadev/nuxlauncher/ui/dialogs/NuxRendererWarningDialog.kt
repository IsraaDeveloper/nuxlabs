package com.israadev.nuxlauncher.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.israadev.nuxlauncher.core.renderer.NuxRendererInfo
import com.israadev.nuxlauncher.ui.components.NuxButton
import com.israadev.nuxlauncher.ui.components.NuxCard
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes

@Composable
fun NuxRendererWarningDialog(
    renderer: NuxRendererInfo,
    mcVersion: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 24.dp, vertical = 16.dp),
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
                        .padding(20.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PERINGATAN PERENDER",
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        val closeShape = RoundedCornerShape(8.dp)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
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
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Warning Content Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFBBF24).copy(alpha = 0.08f), NuxSizes.ShapeSmall)
                            .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.35f), NuxSizes.ShapeSmall)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Perender saat ini \"${renderer.displayName}\" tidak didukung untuk versi game ini ($mcVersion).",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                            Text(
                                text = "Terus menggunakannya dapat menyebabkan kesalahan rendering atau game crash! Apakah Anda yakin ingin mengabaikan risiko ini dan melanjutkan?",
                                color = Color(0xFFD4D4D8),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                            if (renderer.compatibility.isNotBlank()) {
                                Text(
                                    text = "Dukungan resmi perender ini: ${renderer.compatibility}",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            NuxButton(
                                onClick = onDismiss,
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
                                onClick = onConfirm,
                                backgroundColor = Color(0xFFF59E0B),
                                contentColor = Color.Black,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "TETAP JALANKAN",
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
