package com.israadev.nuxlauncher.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.israadev.nuxlauncher.core.instance.InstanceManager
import com.israadev.nuxlauncher.core.models.Instance
import com.israadev.nuxlauncher.core.mods.NuxAddonImportManager
import com.israadev.nuxlauncher.core.mods.NuxModManager
import com.israadev.nuxlauncher.core.mods.PendingAddonImport
import com.israadev.nuxlauncher.ui.components.NuxButton
import com.israadev.nuxlauncher.ui.components.NuxCard
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes
import kotlinx.coroutines.launch

private data class AddonTypeOption(val id: String, val label: String, val icon: String)

private val ADDON_TYPES = listOf(
    AddonTypeOption("mods", "Mod (.jar)", "🧩"),
    AddonTypeOption("modpacks", "Modpack (.mrpack / .zip)", "📦"),
    AddonTypeOption("resourcepacks", "Resource Pack (.zip)", "🎨"),
    AddonTypeOption("shaderpacks", "Shaderpack (.zip)", "✨")
)

@Composable
fun NuxAddonImportDialog(
    pendingImport: PendingAddonImport,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val instances by InstanceManager.instances.collectAsState()
    val activeInstance by InstanceManager.selectedInstance.collectAsState()

    var selectedInstance by remember {
        mutableStateOf<Instance?>(activeInstance ?: instances.firstOrNull())
    }

    var selectedType by remember {
        mutableStateOf(
            if (ADDON_TYPES.any { it.id == pendingImport.suggestedType }) {
                pendingImport.suggestedType
            } else {
                "mods"
            }
        )
    }

    var isImporting by remember { mutableStateOf(false) }
    var importStatusText by remember { mutableStateOf("") }
    var importCurrent by remember { mutableStateOf(0) }
    var importTotal by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = {
            if (!isImporting) {
                NuxAddonImportManager.clearPendingImport()
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isImporting,
            dismissOnClickOutside = !isImporting,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            NuxCard(
                modifier = Modifier
                    .width(520.dp)
                    .fillMaxHeight(0.92f),
                backgroundColor = NuxColors.SurfaceElevated,
                borderColor = Color(0x3310B981),
                cornerRadius = NuxSizes.CornerRadiusLarge,
                fillMaxHeight = false
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NuxColors.ForestGreen.copy(alpha = 0.15f))
                                    .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "📥", fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "PASANG ADDON EKSTERNAL",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "NUX Addon Installer",
                                    color = NuxColors.MintGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (!isImporting) {
                            val closeShape = RoundedCornerShape(8.dp)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(closeShape)
                                    .background(NuxColors.SurfaceInput, closeShape)
                                    .border(1.dp, NuxColors.CardBorder, closeShape)
                                    .clickable {
                                        NuxAddonImportManager.clearPendingImport()
                                        onDismiss()
                                    },
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
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scrollable Dialog Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // File Info Box
                        val fileCardShape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(fileCardShape)
                                .background(Color(0xFF12141A), fileCardShape)
                                .border(1.dp, Color(0x22FFFFFF), fileCardShape)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E222D)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val icon = when (selectedType) {
                                        "mods" -> "🧩"
                                        "modpacks" -> "📦"
                                        "resourcepacks" -> "🎨"
                                        "shaderpacks" -> "✨"
                                        else -> "📄"
                                    }
                                    Text(text = icon, fontSize = 20.sp)
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pendingImport.fileName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Tipe file: ${selectedType.uppercase()} · Siap dipasang",
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 10.5.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Type Selector Chips
                        Text(
                            text = "KATEGORI ADDON",
                            color = Color(0xFF71717A),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(ADDON_TYPES) { option ->
                                val isSelected = selectedType == option.id
                                val chipShape = RoundedCornerShape(8.dp)
                                Box(
                                    modifier = Modifier
                                        .clip(chipShape)
                                        .background(
                                            if (isSelected) NuxColors.ForestGreen.copy(alpha = 0.2f) else Color(0xFF161922),
                                            chipShape
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) NuxColors.ForestGreen else Color(0x18FFFFFF),
                                            shape = chipShape
                                        )
                                        .clickable(enabled = !isImporting) {
                                            selectedType = option.id
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = option.icon, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = option.label,
                                            color = if (isSelected) Color.White else Color(0xFFA1A1AA),
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Instance Selector
                        Text(
                            text = "TARGET INSTANCE MINECRAFT",
                            color = Color(0xFF71717A),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (instances.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF271318))
                                    .border(1.dp, Color(0xFFF43F5E).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "⚠️ Belum ada instance Minecraft yang dibuat! Buat instance terlebih dahulu di Dashboard.",
                                    color = Color(0xFFFECDD3),
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(instances) { inst ->
                                    val isSelected = selectedInstance?.id == inst.id
                                    val cardShape = RoundedCornerShape(8.dp)
                                    Box(
                                        modifier = Modifier
                                            .clip(cardShape)
                                            .background(
                                            if (isSelected) NuxColors.ForestGreen.copy(alpha = 0.18f) else Color(0xFF161922),
                                                cardShape
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) NuxColors.ForestGreen else Color(0x18FFFFFF),
                                                shape = cardShape
                                            )
                                            .clickable(enabled = !isImporting) {
                                                selectedInstance = inst
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text(
                                                    text = inst.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "v${inst.mcVersion} · ${inst.loader.uppercase()}",
                                                    color = if (isSelected) NuxColors.MintGreen else Color(0xFF71717A),
                                                    fontSize = 10.sp
                                                )
                                            }
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .background(NuxColors.ForestGreen, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    androidx.compose.material3.Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Progress Section
                        if (isImporting) {
                            Spacer(modifier = Modifier.height(14.dp))
                            val progressShape = RoundedCornerShape(8.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(progressShape)
                                    .background(Color(0xFF101C19), progressShape)
                                    .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.3f), progressShape)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (importStatusText.isNotBlank()) importStatusText else "Memasang addon...",
                                            color = NuxColors.MintGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (importTotal > 0) {
                                            Text(
                                                text = "$importCurrent / $importTotal",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (importTotal > 0) {
                                        LinearProgressIndicator(
                                            progress = { (importCurrent.toFloat() / importTotal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = NuxColors.ForestGreen,
                                            trackColor = Color(0xFF1E2A27)
                                        )
                                    } else {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = NuxColors.ForestGreen,
                                            trackColor = Color(0xFF1E2A27)
                                        )
                                    }
                                }
                            }
                        }

                        // Error message
                        errorMessage?.let { err ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF271318))
                                    .border(1.dp, Color(0xFFF43F5E).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Gagal: $err",
                                    color = Color(0xFFFECDD3),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Buttons (Sticky at bottom, always visible)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isImporting) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        NuxAddonImportManager.clearPendingImport()
                                        onDismiss()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "BATAL",
                                    color = Color(0xFFA1A1AA),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        val canInstall = selectedInstance != null && !isImporting
                        NuxButton(
                            onClick = {
                                val target = selectedInstance ?: return@NuxButton
                                isImporting = true
                                errorMessage = null
                                importStatusText = "Mempersiapkan pemasangan..."
                                importCurrent = 0
                                importTotal = 0

                                scope.launch {
                                    val result = NuxModManager.importLocalFile(
                                        context = context,
                                        instance = target,
                                        itemType = selectedType,
                                        uri = pendingImport.uri,
                                        onProgress = { status, current, total ->
                                            importStatusText = status
                                            importCurrent = current
                                            importTotal = total
                                        }
                                    )

                                    isImporting = false
                                    if (result.isSuccess) {
                                        Toast.makeText(
                                            context,
                                            "✓ Berhasil memasang \"${pendingImport.fileName}\" ke ${target.name}!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        NuxAddonImportManager.clearPendingImport()
                                        onDismiss()
                                    } else {
                                        val err = result.exceptionOrNull()?.message ?: "Terjadi kesalahan yang tidak diketahui"
                                        errorMessage = err
                                        Toast.makeText(
                                            context,
                                            "Gagal memasang: $err",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            enabled = canInstall,
                            backgroundColor = NuxColors.ForestGreen,
                            contentColor = Color.White,
                            cornerRadius = 8.dp,
                            modifier = Modifier.height(36.dp)
                        ) {
                            if (isImporting) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "MEMASANG...",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = "PASANG SEKARANG",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
