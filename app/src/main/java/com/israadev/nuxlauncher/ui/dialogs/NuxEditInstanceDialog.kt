package com.israadev.nuxlauncher.ui.dialogs

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.israadev.nuxlauncher.core.instance.InstanceManager
import com.israadev.nuxlauncher.core.models.Instance
import com.israadev.nuxlauncher.core.runtime.JavaRuntimeManager
import com.israadev.nuxlauncher.ui.components.NuxButton
import com.israadev.nuxlauncher.ui.components.NuxCard
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes

@Composable
fun NuxEditInstanceDialog(
    instance: Instance,
    onDismiss: () -> Unit,
    onInstanceUpdated: (Instance) -> Unit,
    onInstanceDeleted: (Instance) -> Unit
) {
    val context = LocalContext.current
    var editName by remember { mutableStateOf(instance.name) }
    var selectedJavaRuntime by remember { mutableStateOf(instance.javaRuntime) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val autoRecommendedRuntime = remember(instance.mcVersion) {
        JavaRuntimeManager.getRecommendedRuntime(instance.mcVersion)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            NuxCard(
                modifier = Modifier
                    .width(480.dp)
                    .wrapContentHeight(),
                backgroundColor = NuxColors.SurfaceElevated,
                borderColor = Color(0x33FFFFFF),
                cornerRadius = NuxSizes.CornerRadiusLarge,
                fillMaxHeight = false
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Header Bar (No green dot indicator, tight bottom padding)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "INSTANCE CONFIGURATION",
                                color = Color(0xFF71717A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Edit Instance: ${instance.name}",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.5.sp,
                                letterSpacing = (-0.3).sp,
                                maxLines = 1
                            )
                        }

                        // Close Button
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(NuxColors.SurfaceInput)
                                .border(1.dp, Color(0x2EFFFFFF), CircleShape)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Tutup",
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Content Scrollable Column (Compact spacing)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Section 1: Nama Instance
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF141721))
                                .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "NAMA INSTANCE",
                                color = Color(0xFFA1A1AA),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFF0D0F15))
                                    .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(5.dp))
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    cursorBrush = SolidColor(NuxColors.MintGreen),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Section 2: Custom Java Runtime Selection
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF141721))
                                .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "JAVA RUNTIME",
                                    color = Color(0xFFA1A1AA),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "Rekomendasi: ${JavaRuntimeManager.getRuntimeDisplayName(autoRecommendedRuntime)}",
                                    color = NuxColors.MintGreen,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Runtimes Pills
                            val runtimeOptions = listOf(
                                "auto" to "Auto (${autoRecommendedRuntime.replace("jre-", "Java ")})",
                                "jre-8" to "Java 8",
                                "jre-17" to "Java 17",
                                "jre-21" to "Java 21",
                                "jre-25" to "Java 25"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                runtimeOptions.forEach { (key, label) ->
                                    val isSelected = selectedJavaRuntime == key
                                    val pillBg by animateColorAsState(
                                        targetValue = if (isSelected) NuxColors.ForestGreen.copy(alpha = 0.2f) else Color(0xFF0D0F15),
                                        animationSpec = tween(150)
                                    )
                                    val pillBorder by animateColorAsState(
                                        targetValue = if (isSelected) NuxColors.MintGreen else Color(0x22FFFFFF),
                                        animationSpec = tween(150)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(pillBg)
                                            .border(1.dp, pillBorder, RoundedCornerShape(5.dp))
                                            .clickable { selectedJavaRuntime = key }
                                            .padding(horizontal = 7.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .background(NuxColors.MintGreen, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            Text(
                                                text = label,
                                                color = if (isSelected) NuxColors.MintGreen else Color(0xFFA1A1AA),
                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Section 3: Instance Details & File Explorer Action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF141721))
                                .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TARGET ENGINE",
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = "MC ${instance.mcVersion} · ${instance.loader.uppercase()}${if (instance.loaderVersion.isNotBlank()) " (${instance.loaderVersion})" else ""}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.5.sp
                                )
                            }

                            // Button Open Folder
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFF1E222D))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(5.dp))
                                    .clickable {
                                        Toast.makeText(context, "Membuka folder game...", Toast.LENGTH_SHORT).show()
                                        InstanceManager.openInstanceFolder(context, instance)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Folder,
                                        contentDescription = null,
                                        tint = NuxColors.MintGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Buka Folder",
                                        color = Color.White,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Section 4: Danger Zone (Hapus Instance)
                        if (!showDeleteConfirm) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.08f))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f), RoundedCornerShape(7.dp))
                                    .clickable { showDeleteConfirm = true }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = null,
                                            tint = Color(0xFFF87171),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Hapus Instance Ini",
                                            color = Color(0xFFF87171),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "Hapus game & data",
                                        color = Color(0xFF71717A),
                                        fontSize = 8.5.sp
                                    )
                                }
                            }
                        } else {
                            // Confirm Delete Box
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color(0xFF450A0A))
                                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(7.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Yakin ingin menghapus '${instance.name}'?",
                                    color = Color.White,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Semua world, mod, config, dan file instance akan dihapus permanen.",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 8.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    NuxButton(
                                        onClick = { showDeleteConfirm = false },
                                        backgroundColor = Color(0xFF1E222D),
                                        contentColor = Color.White,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(26.dp)
                                    ) {
                                        Text("BATAL", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    NuxButton(
                                        onClick = {
                                            onInstanceDeleted(instance)
                                            onDismiss()
                                        },
                                        backgroundColor = Color(0xFFDC2626),
                                        contentColor = Color.White,
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .height(26.dp)
                                    ) {
                                        Text("YA, HAPUS SEKARANG", fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bottom Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NuxButton(
                            onClick = onDismiss,
                            backgroundColor = NuxColors.SurfaceInput,
                            contentColor = NuxColors.DarkGray,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Text("BATAL", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }

                        NuxButton(
                            onClick = {
                                val trimmed = editName.trim()
                                if (trimmed.isBlank()) {
                                    Toast.makeText(context, "Nama instance tidak boleh kosong", Toast.LENGTH_SHORT).show()
                                    return@NuxButton
                                }
                                val updated = instance.copy(
                                    name = trimmed,
                                    javaRuntime = selectedJavaRuntime
                                )
                                onInstanceUpdated(updated)
                                Toast.makeText(context, "Instance berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                onDismiss()
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
