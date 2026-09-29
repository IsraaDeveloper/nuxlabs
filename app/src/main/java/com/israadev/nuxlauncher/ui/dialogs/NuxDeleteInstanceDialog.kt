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
import com.israadev.nuxlauncher.core.models.Instance
import com.israadev.nuxlauncher.ui.components.NuxButton
import com.israadev.nuxlauncher.ui.components.NuxCard
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes

@Composable
fun NuxDeleteInstanceDialog(
    instance: Instance,
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
                    .width(440.dp)
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
                        Text(
                            text = "HAPUS INSTANCE",
                            color = NuxColors.Coral,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 0.5.sp
                        )

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

                    // Content Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NuxColors.SurfaceInput, NuxSizes.ShapeSmall)
                            .border(1.dp, NuxColors.CardBorder, NuxSizes.ShapeSmall)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = instance.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Versi: ${instance.mcVersion} · Loader: ${instance.loader.uppercase()}",
                                color = Color(0xFFA1A1AA),
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Apakah Anda yakin ingin menghapus instance ini? Semua dunia, savegame, dan modifikasi pada instance ini akan dihapus secara permanen.",
                        color = Color(0xFFA1A1AA),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NuxButton(
                            onClick = onDismiss,
                            backgroundColor = NuxColors.SurfaceInput,
                            contentColor = Color.White,
                            cornerRadius = 10.dp,
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text(
                                text = "BATAL",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        NuxButton(
                            onClick = onConfirm,
                            backgroundColor = NuxColors.Coral,
                            contentColor = Color.White,
                            cornerRadius = 10.dp,
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text(
                                text = "HAPUS 🗑",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
