package com.israadev.nuxlauncher.ui.dialogs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.core.fabric.FabricService
import com.israadev.nuxlauncher.core.manifest.MojangManifestService
import com.israadev.nuxlauncher.core.models.Instance
import com.israadev.nuxlauncher.core.models.VersionItem
import com.israadev.nuxlauncher.ui.components.*
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes
import java.util.UUID

/**
 * Filter kategori versi Minecraft (seperti Zalith Launcher)
 */
enum class VersionCategory(
    val label: String,
    val badgeBg: Color,
    val badgeText: Color
) {
    RELEASE("Release", Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF10B981)),
    SNAPSHOT("Snapshot / Beta", Color(0xFF8B5CF6).copy(alpha = 0.15f), Color(0xFFA78BFA)),
    OLD_BETA("Old Beta", Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFFBBF24)),
    OLD_ALPHA("Old Alpha", Color(0xFFEC4899).copy(alpha = 0.15f), Color(0xFFF472B6)),
    ALL("Semua", Color(0xFF3B82F6).copy(alpha = 0.15f), Color(0xFF60A5FA))
}

@Composable
fun NuxAddInstanceDialog(
    onDismiss: () -> Unit,
    onInstanceCreated: (Instance) -> Unit
) {
    var instanceName by remember { mutableStateOf("") }
    var selectedVersion by remember { mutableStateOf("1.21.1") }
    var selectedLoader by remember { mutableStateOf("vanilla") } // "vanilla" or "fabric"
    var selectedFabricVersion by remember { mutableStateOf("") }

    var versionsList by remember { mutableStateOf<List<VersionItem>>(emptyList()) }
    var fabricVersionsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingVersions by remember { mutableStateOf(true) }
    var isLoadingFabric by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(VersionCategory.RELEASE) }

    LaunchedEffect(Unit) {
        val result = MojangManifestService.getVersionManifest()
        val manifest = result.getOrNull()
        if (manifest != null) {
            versionsList = manifest.versions
            val releases = manifest.versions.filter { it.type == "release" }
            val defaultChoice = releases.find { it.id == "1.21.1" }?.id
                ?: releases.find { it.id == "1.20.1" }?.id
                ?: releases.firstOrNull()?.id
                ?: "1.21.1"
            selectedVersion = defaultChoice
        }
        isLoadingVersions = false
    }

    LaunchedEffect(selectedVersion, selectedLoader) {
        if (selectedLoader == "fabric") {
            isLoadingFabric = true
            fabricVersionsList = emptyList()
            selectedFabricVersion = ""
            val result = FabricService.getFabricLoaders(selectedVersion)
            val loaders = result.getOrNull() ?: emptyList()
            fabricVersionsList = loaders
            if (loaders.isNotEmpty()) {
                selectedFabricVersion = loaders.first()
            }
            isLoadingFabric = false
        }
    }

    val filteredVersions = remember(versionsList, searchQuery, selectedCategory) {
        versionsList.filter { item ->
            val matchesCategory = when (selectedCategory) {
                VersionCategory.ALL -> true
                VersionCategory.RELEASE -> item.type == "release"
                VersionCategory.SNAPSHOT -> item.type == "snapshot" || item.type == "pending" || item.type == "unobfuscated"
                VersionCategory.OLD_BETA -> item.type == "old_beta" || item.id.startsWith("b1.") || item.id.startsWith("b")
                VersionCategory.OLD_ALPHA -> item.type == "old_alpha" || item.id.startsWith("a1.") || item.id.startsWith("a")
            }
            val matchesQuery = if (searchQuery.isBlank()) true else item.id.contains(searchQuery.trim(), ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    NuxDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        fillMaxHeight = true
    ) {
        // Outer Shell Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF141720),
                            Color(0xFF0F1117)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(NuxColors.ForestGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "INSTANCE ARCHITECT",
                                color = Color(0xFF71717A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Buat Instance Baru",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = (-0.3).sp
                            )
                        }
                    }

                    // Close Button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E222D))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Two-Column Landscape Architecture
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ==========================================
                    // LEFT COLUMN: Instance Configurations
                    // ==========================================
                    val leftBezelShape = RoundedCornerShape(16.dp)
                    val innerBezelShape = RoundedCornerShape(12.dp)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(leftBezelShape)
                            .background(Color(0xFF11141C))
                            .border(1.dp, Color(0x1FFFFFFF), leftBezelShape)
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. Nama Instance Box (Double-Bezel Inner Core)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(innerBezelShape)
                                        .background(Color(0xFF181C26))
                                        .border(1.dp, Color(0x1AFFFFFF), innerBezelShape)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "NAMA INSTANCE",
                                        color = Color(0xFFA1A1AA),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp,
                                        letterSpacing = 0.8.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0D0F15))
                                            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (instanceName.isEmpty()) {
                                            Text(
                                                text = "Default: $selectedVersion ${selectedLoader.replaceFirstChar { it.uppercase() }}",
                                                color = Color(0xFF52525B),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                        BasicTextField(
                                            value = instanceName,
                                            onValueChange = { instanceName = it },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            cursorBrush = SolidColor(NuxColors.ForestGreen),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                // 2. Mod Loader Selection
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(innerBezelShape)
                                        .background(Color(0xFF181C26))
                                        .border(1.dp, Color(0x1AFFFFFF), innerBezelShape)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "MOD LOADER ENGINE",
                                        color = Color(0xFFA1A1AA),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp,
                                        letterSpacing = 0.8.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("vanilla" to "Vanilla", "fabric" to "Fabric").forEach { (type, label) ->
                                            val isSelected = selectedLoader == type
                                            val btnBg by animateColorAsState(
                                                targetValue = if (isSelected) Color(0xFF10B981).copy(alpha = 0.16f) else Color(0xFF0D0F15),
                                                animationSpec = tween(200)
                                            )
                                            val btnBorder by animateColorAsState(
                                                targetValue = if (isSelected) NuxColors.ForestGreen else Color(0x22FFFFFF),
                                                animationSpec = tween(200)
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(btnBg, RoundedCornerShape(8.dp))
                                                    .border(1.dp, btnBorder, RoundedCornerShape(8.dp))
                                                    .clickable { selectedLoader = type },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (isSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .background(NuxColors.MintGreen, CircleShape)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                    }
                                                    Text(
                                                        text = label,
                                                        color = if (isSelected) NuxColors.MintGreen else Color(0xFFA1A1AA),
                                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                                        fontSize = 11.5.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Fabric Loader Version Picker
                                    if (selectedLoader == "fabric") {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        var showFabricMenu by remember { mutableStateOf(false) }

                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF0D0F15), RoundedCornerShape(8.dp))
                                                    .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        if (!isLoadingFabric && fabricVersionsList.isNotEmpty()) {
                                                            showFabricMenu = true
                                                        }
                                                    }
                                                    .padding(horizontal = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (isLoadingFabric) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(12.dp),
                                                            strokeWidth = 1.5.dp,
                                                            color = NuxColors.ForestGreen
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "Memuat loader Fabric...",
                                                            color = Color(0xFFA1A1AA),
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                } else if (fabricVersionsList.isEmpty()) {
                                                    Text(
                                                        text = "⚠️ Fabric belum tersedia untuk $selectedVersion",
                                                        color = Color(0xFFFB7185),
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                } else {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "Fabric $selectedFabricVersion",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.5.sp
                                                        )
                                                    }
                                                    Text(
                                                        text = "▼",
                                                        color = Color(0xFF71717A),
                                                        fontSize = 9.sp
                                                    )
                                                }
                                            }

                                            DropdownMenu(
                                                expanded = showFabricMenu,
                                                onDismissRequest = { showFabricMenu = false },
                                                modifier = Modifier
                                                    .background(Color(0xFF181C26))
                                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                                    .heightIn(max = 200.dp)
                                            ) {
                                                fabricVersionsList.take(30).forEach { ver ->
                                                    val isPicked = ver == selectedFabricVersion
                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = ver,
                                                                    fontWeight = if (isPicked) FontWeight.Black else FontWeight.Normal,
                                                                    fontSize = 11.5.sp,
                                                                    color = if (isPicked) NuxColors.MintGreen else Color.White
                                                                )
                                                                if (isPicked) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Check,
                                                                        contentDescription = null,
                                                                        tint = NuxColors.MintGreen,
                                                                        modifier = Modifier.size(14.dp)
                                                                    )
                                                                }
                                                            }
                                                        },
                                                        onClick = {
                                                            selectedFabricVersion = ver
                                                            showFabricMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 3. Summary Card with Subtle Ambient Glow
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(innerBezelShape)
                                        .background(Color(0xFF0F221B))
                                        .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.4f), innerBezelShape)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "SPESIFIKASI TARGET",
                                            color = NuxColors.MintGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.5.sp,
                                            letterSpacing = 0.8.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Minecraft $selectedVersion",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (selectedLoader == "fabric") Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                                else Color(0xFF10B981).copy(alpha = 0.2f)
                                            )
                                            .border(
                                                1.dp,
                                                if (selectedLoader == "fabric") Color(0xFF8B5CF6).copy(alpha = 0.5f)
                                                else Color(0xFF10B981).copy(alpha = 0.5f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (selectedLoader == "fabric") "FABRIC" else "VANILLA",
                                            color = if (selectedLoader == "fabric") Color(0xFFA78BFA) else NuxColors.MintGreen,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }

                            // 4. Island CTA Button (Bottom Anchored)
                            val canCreate = !(selectedLoader == "fabric" && (isLoadingFabric || selectedFabricVersion.isBlank()))
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (canCreate) Brush.horizontalGradient(
                                            listOf(Color(0xFF059669), Color(0xFF10B981))
                                        ) else Brush.horizontalGradient(
                                            listOf(Color(0xFF27272A), Color(0xFF18181B))
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        if (canCreate) Color(0x6634D399) else Color(0x1AFFFFFF),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = canCreate) {
                                        val defaultName = "$selectedVersion ${selectedLoader.replaceFirstChar { it.uppercase() }}"
                                        val finalName = if (instanceName.isNotBlank()) instanceName else defaultName
                                        val newInstance = Instance(
                                            id = "inst_${UUID.randomUUID()}",
                                            name = finalName,
                                            mcVersion = selectedVersion,
                                            loader = selectedLoader,
                                            loaderVersion = if (selectedLoader == "fabric") selectedFabricVersion else "",
                                            icon = if (selectedLoader == "fabric") "fabric" else "crafting_table"
                                        )
                                        onInstanceCreated(newInstance)
                                    }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when {
                                            isLoadingFabric && selectedLoader == "fabric" -> "Memuat Fabric..."
                                            selectedLoader == "fabric" && selectedFabricVersion.isBlank() -> "Fabric Tidak Tersedia"
                                            else -> "Buat Instance Sekarang"
                                        },
                                        color = if (canCreate) Color.White else Color(0xFF71717A),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.2.sp
                                    )

                                    // Nested Icon Pill
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (canCreate) Color.White.copy(alpha = 0.2f) else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            tint = if (canCreate) Color.White else Color(0xFF71717A),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // RIGHT COLUMN: Version Picker with Beta Options
                    // ==========================================
                    Box(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight()
                            .clip(leftBezelShape)
                            .background(Color(0xFF11141C))
                            .border(1.dp, Color(0x1FFFFFFF), leftBezelShape)
                            .padding(8.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Category Filter Bar (Pill Tabs like Zalith)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VersionCategory.values().forEach { category ->
                                    val isCatSelected = selectedCategory == category
                                    val pillBg by animateColorAsState(
                                        targetValue = if (isCatSelected) category.badgeBg else Color(0xFF181C26),
                                        animationSpec = tween(150)
                                    )
                                    val pillBorder by animateColorAsState(
                                        targetValue = if (isCatSelected) category.badgeText.copy(alpha = 0.7f) else Color(0x1AFFFFFF),
                                        animationSpec = tween(150)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(pillBg)
                                            .border(1.dp, pillBorder, RoundedCornerShape(8.dp))
                                            .clickable { selectedCategory = category }
                                            .padding(horizontal = 9.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = category.label,
                                            color = if (isCatSelected) category.badgeText else Color(0xFFA1A1AA),
                                            fontWeight = if (isCatSelected) FontWeight.Black else FontWeight.SemiBold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Search Field
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0D0F15))
                                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 9.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = Color(0xFF71717A),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        cursorBrush = SolidColor(NuxColors.ForestGreen),
                                        modifier = Modifier.weight(1f),
                                        decorationBox = { innerTextField ->
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    text = "Cari versi (${selectedCategory.label})...",
                                                    color = Color(0xFF52525B),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = Color(0xFFA1A1AA),
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { searchQuery = "" }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Version Count Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DAFTAR VERSI",
                                    color = Color(0xFF71717A),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "${filteredVersions.size} versi ditemukan",
                                    color = Color(0xFF52525B),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Version List Inner Core
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(innerBezelShape)
                                    .background(Color(0xFF0D0F15))
                                    .border(1.dp, Color(0x1AFFFFFF), innerBezelShape)
                                    .padding(4.dp)
                            ) {
                                if (isLoadingVersions) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(
                                                color = NuxColors.ForestGreen,
                                                modifier = Modifier.size(22.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Memuat manifest Mojang...",
                                                color = Color(0xFF71717A),
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }
                                } else if (filteredVersions.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Tidak ada versi yang cocok",
                                            color = Color(0xFF71717A),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(filteredVersions, key = { it.id }) { item ->
                                            val isSelected = item.id == selectedVersion
                                            val itemShape = RoundedCornerShape(8.dp)

                                            // Determine Badge Color & Label based on item type
                                            val (badgeText, badgeBg, badgeTextColor) = when {
                                                item.type == "release" -> Triple("RELEASE", Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF10B981))
                                                item.type == "snapshot" -> Triple("SNAPSHOT", Color(0xFF8B5CF6).copy(alpha = 0.15f), Color(0xFFA78BFA))
                                                item.type == "old_beta" || item.id.startsWith("b") -> Triple("BETA", Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFFBBF24))
                                                item.type == "old_alpha" || item.id.startsWith("a") -> Triple("ALPHA", Color(0xFFEC4899).copy(alpha = 0.15f), Color(0xFFF472B6))
                                                else -> Triple(item.type.uppercase(), Color(0xFF3B82F6).copy(alpha = 0.15f), Color(0xFF60A5FA))
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(itemShape)
                                                    .background(
                                                        if (isSelected) Color(0xFF10B981).copy(alpha = 0.14f)
                                                        else Color(0xFF141721),
                                                        itemShape
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSelected) NuxColors.ForestGreen else Color(0x14FFFFFF),
                                                        shape = itemShape
                                                    )
                                                    .clickable { selectedVersion = item.id }
                                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = item.id,
                                                        color = if (isSelected) Color.White else Color(0xFFE4E4E7),
                                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(badgeBg)
                                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = badgeText,
                                                            color = badgeTextColor,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 8.5.sp
                                                        )
                                                    }
                                                }

                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(18.dp)
                                                            .background(NuxColors.ForestGreen, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = Color(0xFF09090B),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
