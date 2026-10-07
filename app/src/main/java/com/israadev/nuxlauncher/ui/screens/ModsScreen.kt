package com.israadev.nuxlauncher.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.core.instance.InstanceManager
import com.israadev.nuxlauncher.core.models.*
import com.israadev.nuxlauncher.core.mods.NuxModManager
import com.israadev.nuxlauncher.core.mods.WorldInfo
import com.israadev.nuxlauncher.ui.components.*
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat

private fun getMimeTypesForType(type: String): Array<String> {
    return when (type) {
        "mods" -> arrayOf(
            "application/java-archive",
            "application/x-java-archive",
            "application/x-jar",
            "application/jar",
            "application/octet-stream"
        )
        "modpacks" -> arrayOf(
            "application/zip",
            "application/x-zip-compressed",
            "application/octet-stream"
        )
        else -> arrayOf(
            "application/zip",
            "application/x-zip-compressed"
        )
    }
}

private fun isValidExtensionForType(itemType: String, filename: String): Boolean {
    val lower = filename.lowercase()
    return when (itemType) {
        "mods" -> lower.endsWith(".jar")
        "modpacks" -> lower.endsWith(".mrpack") || lower.endsWith(".zip")
        "shaderpacks", "resourcepacks", "datapacks" -> lower.endsWith(".zip")
        else -> false
    }
}

private fun getAllowedExtensionLabel(itemType: String): String {
    return when (itemType) {
        "mods" -> ".jar"
        "modpacks" -> ".mrpack, .zip"
        "shaderpacks" -> ".zip (Shaderpack)"
        "resourcepacks" -> ".zip (Resource Pack)"
        "datapacks" -> ".zip (Datapack)"
        else -> "format yang didukung"
    }
}

private fun getFileNameFromUri(context: android.content.Context, uri: Uri): String {
    var name: String? = null
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {}
    }
    if (name.isNullOrBlank()) {
        name = uri.lastPathSegment?.substringAfterLast('/')
    }
    return name ?: "unknown_file"
}

private fun isModInstalled(inst: InstalledModItem, hit: ModrinthSearchHit): Boolean {
    // 1. Exact Modrinth Project ID (dari metadata manifest atau id)
    if (inst.id.isNotBlank() && inst.id.equals(hit.projectId, ignoreCase = true)) {
        return true
    }
    // 2. Exact Title Match
    if (inst.name.equals(hit.title, ignoreCase = true)) {
        return true
    }
    // 3. Exact slug match pada nama file mod (bukan substring ceroboh yang mencocokkan dependensi seperti sodium vs sodium-extra)
    val slug = hit.slug?.trim()?.lowercase() ?: ""
    if (slug.isNotBlank()) {
        val clean = inst.filename.removeSuffix(".disabled").removeSuffix(".jar").lowercase()
        val namePrefix = clean.takeWhile { !it.isDigit() }.trimEnd('-', '_', '+')
        val base = namePrefix
            .removeSuffix("-fabric").removeSuffix("-forge").removeSuffix("-neoforge").removeSuffix("-quilt")
            .removeSuffix("_fabric").removeSuffix("_forge").removeSuffix("_neoforge").removeSuffix("_quilt")
            .trimEnd('-', '_')
        if (base.equals(slug, ignoreCase = true)) {
            return true
        }
    }
    return false
}

data class ModCategoryItem(val id: String, val label: String)

private val CATEGORIES_MAP = mapOf(
    "mods" to listOf(
        ModCategoryItem("Semua", "Semua"),
        ModCategoryItem("optimization", "⚡ Optimization"),
        ModCategoryItem("technology", "⚙️ Technology"),
        ModCategoryItem("magic", "🪄 Magic"),
        ModCategoryItem("adventure", "🗺️ Adventure"),
        ModCategoryItem("utility", "🛠️ Utility"),
        ModCategoryItem("decoration", "🎨 Decoration")
    ),
    "modpacks" to listOf(
        ModCategoryItem("Semua", "Semua"),
        ModCategoryItem("adventure", "🗺️ Adventure"),
        ModCategoryItem("optimization", "⚡ Optimization"),
        ModCategoryItem("technology", "⚙️ Technology"),
        ModCategoryItem("magic", "🪄 Magic"),
        ModCategoryItem("quests", "📜 Quests")
    ),
    "resourcepacks" to listOf(
        ModCategoryItem("Semua", "Semua"),
        ModCategoryItem("16x", "16x"),
        ModCategoryItem("32x", "32x"),
        ModCategoryItem("simplistic", "✨ Simplistic"),
        ModCategoryItem("traditional", "🏛️ Traditional"),
        ModCategoryItem("medieval", "🏰 Medieval")
    ),
    "shaderpacks" to listOf(
        ModCategoryItem("Semua", "Semua"),
        ModCategoryItem("realistic", "🌅 Realistic"),
        ModCategoryItem("stylized", "🎨 Stylized"),
        ModCategoryItem("fantasy", "🔮 Fantasy")
    ),
    "datapacks" to listOf(
        ModCategoryItem("Semua", "Semua"),
        ModCategoryItem("adventure", "🗺️ Adventure"),
        ModCategoryItem("utility", "🛠️ Utility"),
        ModCategoryItem("magic", "🪄 Magic"),
        ModCategoryItem("worldgen", "🌍 Worldgen")
    )
)

@Composable
fun ModsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val instances by InstanceManager.instances.collectAsState()
    val selectedInstance by InstanceManager.selectedInstance.collectAsState()

    // Mode: "installed" | "browse"
    var mainMode by remember { mutableStateOf("browse") }

    // Item Type: "mods" | "modpacks" | "resourcepacks" | "shaderpacks" | "datapacks"
    var activeType by remember { mutableStateOf("mods") }

    // World state for Datapacks
    var worlds by remember { mutableStateOf<List<WorldInfo>>(emptyList()) }
    var selectedWorld by remember { mutableStateOf("") }
    var showWorldMenu by remember { mutableStateOf(false) }

    // Installed list state
    var installedItems by remember { mutableStateOf<List<InstalledModItem>>(emptyList()) }
    var isInstalledLoading by remember { mutableStateOf(false) }
    var installedSearch by remember { mutableStateOf("") }
    var itemToDelete by remember { mutableStateOf<InstalledModItem?>(null) }

    // Browse Modrinth state
    var browseQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var searchHits by remember { mutableStateOf<List<ModrinthSearchHit>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchErrorMessage by remember { mutableStateOf<String?>(null) }
    var filterByGameVersion by remember { mutableStateOf(true) }
    var currentPage by remember { mutableStateOf(1) }
    var totalHits by remember { mutableStateOf(0) }
    var downloadingProjectIds by remember { mutableStateOf<Map<String, String>>(emptyMap()) } // projectId -> status

    // Detail Modal state
    var detailModalHit by remember { mutableStateOf<ModrinthSearchHit?>(null) }
    var detailProject by remember { mutableStateOf<ModrinthProject?>(null) }
    var compatibleVersions by remember { mutableStateOf<List<ModrinthVersion>>(emptyList()) }
    var selectedVersion by remember { mutableStateOf<ModrinthVersion?>(null) }
    var isDetailLoading by remember { mutableStateOf(false) }
    var isDetailInstalling by remember { mutableStateOf(false) }
    var detailProgressText by remember { mutableStateOf("") }
    var detailTab by remember { mutableStateOf("overview") } // "overview" | "versions"
    var previewGalleryUrl by remember { mutableStateOf<String?>(null) }

    // Instance switch dropdown
    var showInstanceMenu by remember { mutableStateOf(false) }

    // Load available worlds for selected instance
    fun reloadWorlds() {
        val inst = selectedInstance ?: return
        val list = NuxModManager.getInstanceWorlds(context, inst)
        worlds = list
        if (list.isNotEmpty()) {
            if (selectedWorld.isBlank() || list.none { it.folderName == selectedWorld }) {
                selectedWorld = list.first().folderName
            }
        } else {
            selectedWorld = ""
        }
    }

    // Reload installed items
    fun reloadInstalled() {
        val inst = selectedInstance ?: return
        isInstalledLoading = true
        scope.launch {
            val targetWorld = if (activeType == "datapacks") selectedWorld else null
            installedItems = NuxModManager.getInstalledItems(context, inst, activeType, targetWorld)
            isInstalledLoading = false
        }
    }

    // Search Modrinth
    fun performSearch(page: Int = 1) {
        val inst = selectedInstance ?: return
        isSearching = true
        searchErrorMessage = null
        scope.launch {
            val cat = if (selectedCategory == "Semua") null else selectedCategory
            val res = NuxModManager.searchProjects(
                query = browseQuery,
                itemType = activeType,
                instance = inst,
                category = cat,
                filterByGameVersion = filterByGameVersion,
                page = page
            )
            if (res.isSuccess) {
                val data = res.getOrNull()
                searchHits = data?.hits ?: emptyList()
                totalHits = data?.totalHits ?: 0
                currentPage = page
                searchErrorMessage = null
            } else {
                searchHits = emptyList()
                totalHits = 0
                searchErrorMessage = res.exceptionOrNull()?.message ?: "Gagal terhubung ke Modrinth"
            }
            isSearching = false
        }
    }

    // Trigger reload when tab, type, world or instance changes
    LaunchedEffect(selectedInstance, activeType, mainMode, selectedWorld, filterByGameVersion) {
        if (selectedInstance != null) {
            reloadWorlds()
            reloadInstalled()
            if (mainMode == "browse") {
                performSearch(1)
            }
        }
    }

    // Debounce search query
    var searchJob by remember { mutableStateOf<Job?>(null) }
    fun onQueryChanged(newQuery: String) {
        browseQuery = newQuery
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(300)
            currentPage = 1
            performSearch(1)
        }
    }

    var isImportingModpack by remember { mutableStateOf(false) }
    var modpackImportStatus by remember { mutableStateOf("") }
    var modpackImportCurrent by remember { mutableStateOf(0) }
    var modpackImportTotal by remember { mutableStateOf(0) }
    var modpackImportName by remember { mutableStateOf("") }

    // File Picker for local addon import with strict extension filtering and modpack extraction
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        if (!uris.isNullOrEmpty() && selectedInstance != null) {
            val inst = selectedInstance!!
            scope.launch {
                val targetWorld = if (activeType == "datapacks") selectedWorld else null

                // 1. Filter file berdasarkan ekstensi yang di-support
                val validUris = mutableListOf<Pair<Uri, String>>()
                val rejectedNames = mutableListOf<String>()

                for (uri in uris) {
                    val fileName = getFileNameFromUri(context, uri)
                    if (isValidExtensionForType(activeType, fileName)) {
                        validUris.add(Pair(uri, fileName))
                    } else {
                        rejectedNames.add(fileName)
                    }
                }

                if (rejectedNames.isNotEmpty()) {
                    val hint = getAllowedExtensionLabel(activeType)
                    val msg = if (rejectedNames.size == 1) {
                        "File \"${rejectedNames.first()}\" diabaikan! Format yang didukung untuk $activeType hanya $hint"
                    } else {
                        "${rejectedNames.size} file diabaikan karena format tidak didukung ($hint)!"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }

                if (validUris.isEmpty()) return@launch

                // 2. Import file yang valid
                if (activeType == "modpacks") {
                    isImportingModpack = true
                    var successCount = 0
                    var lastError: String? = null

                    for ((uri, fileName) in validUris) {
                        modpackImportName = fileName
                        modpackImportStatus = "Membaca arsip modpack..."
                        modpackImportCurrent = 0
                        modpackImportTotal = 0

                        val importRes = NuxModManager.importLocalFile(
                            context = context,
                            instance = inst,
                            itemType = activeType,
                            uri = uri,
                            worldName = targetWorld,
                            onProgress = { statusText, cur, tot ->
                                modpackImportStatus = statusText
                                modpackImportCurrent = cur
                                modpackImportTotal = tot
                            }
                        )

                        if (importRes.isSuccess) {
                            successCount++
                        } else {
                            lastError = importRes.exceptionOrNull()?.message
                        }
                    }

                    isImportingModpack = false
                    reloadInstalled()

                    if (successCount > 0) {
                        Toast.makeText(
                            context,
                            "Berhasil mengimpor dan memasang $successCount modpack!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (lastError != null) {
                        Toast.makeText(
                            context,
                            "Gagal mengimpor modpack: $lastError",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    var successCount = 0
                    var lastError: String? = null

                    for ((uri, _) in validUris) {
                        val importRes = NuxModManager.importLocalFile(
                            context = context,
                            instance = inst,
                            itemType = activeType,
                            uri = uri,
                            worldName = targetWorld
                        )
                        if (importRes.isSuccess) {
                            successCount++
                        } else {
                            lastError = importRes.exceptionOrNull()?.message
                        }
                    }

                    reloadInstalled()

                    if (successCount > 0) {
                        Toast.makeText(
                            context,
                            "Berhasil mengimpor $successCount file $activeType!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (lastError != null) {
                        Toast.makeText(
                            context,
                            "Gagal mengimpor file: $lastError",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NuxColors.Background)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
    ) {
        if (selectedInstance == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                NuxCard(
                    modifier = Modifier.width(320.dp),
                    backgroundColor = NuxColors.SurfaceElevated,
                    borderColor = NuxColors.CardBorder,
                    cornerRadius = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ExtensionOff,
                            contentDescription = null,
                            tint = NuxColors.ForestGreen,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum Ada Instance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pilih atau buat instance di Dashboard untuk mulai memasang mod.",
                            color = NuxColors.GrayNeutral,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        NuxButton(
                            onClick = { onNavigateBack() },
                            backgroundColor = NuxColors.ForestGreen,
                            cornerRadius = 6.dp
                        ) {
                            Text("KEMBALI KE DASHBOARD", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }
            return
        }

        val currentInst = selectedInstance!!

        // =========================================================================
        // ROW 1: MINIMALIST CYBER-GLASS TOP NAVIGATION BAR
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group: Back Button + Segmented Switcher (Terpasang vs Jelajah)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Back Button (Matching Dashboard & Accounts)
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NuxColors.SurfaceElevated, RoundedCornerShape(6.dp))
                        .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                        .clickable { onNavigateBack() }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Kembali",
                            tint = NuxColors.ForestGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "DASHBOARD",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Segmented Mode Tabs: [ Terpasang (X) | Jelajah Modrinth ]
                Row(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NuxColors.SurfaceElevated, RoundedCornerShape(6.dp))
                        .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isInstalled = mainMode == "installed"
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isInstalled) NuxColors.ForestGreen.copy(alpha = 0.2f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (isInstalled) NuxColors.ForestGreen.copy(alpha = 0.45f) else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { mainMode = "installed" }
                            .padding(horizontal = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TERPASANG (${installedItems.size})",
                            color = if (isInstalled) NuxColors.MintGreen else NuxColors.GrayNeutral,
                            fontWeight = if (isInstalled) FontWeight.Black else FontWeight.SemiBold,
                            fontSize = 9.5.sp
                        )
                    }

                    val isBrowse = mainMode == "browse"
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isBrowse) NuxColors.ForestGreen.copy(alpha = 0.2f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (isBrowse) NuxColors.ForestGreen.copy(alpha = 0.45f) else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { mainMode = "browse" }
                            .padding(horizontal = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "JELAJAH MODRINTH",
                            color = if (isBrowse) NuxColors.MintGreen else NuxColors.GrayNeutral,
                            fontWeight = if (isBrowse) FontWeight.Black else FontWeight.SemiBold,
                            fontSize = 9.5.sp
                        )
                    }
                }
            }

            // Right Group: Instance Selector Dropdown Pill
            Box {
                Row(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NuxColors.SurfaceElevated, RoundedCornerShape(6.dp))
                        .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                        .clickable { if (instances.size > 1) showInstanceMenu = true }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = NuxColors.ForestGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${currentInst.name} (${currentInst.mcVersion} · ${currentInst.loader.uppercase()})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (instances.size > 1) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Outlined.ArrowDropDown,
                            contentDescription = null,
                            tint = NuxColors.GrayNeutral,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showInstanceMenu,
                    onDismissRequest = { showInstanceMenu = false }
                ) {
                    instances.forEach { inst ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${inst.name} (${inst.mcVersion} · ${inst.loader.uppercase()})",
                                    fontWeight = if (inst.id == selectedInstance?.id) FontWeight.Black else FontWeight.Normal,
                                    fontSize = 11.5.sp
                                )
                            },
                            onClick = {
                                InstanceManager.selectInstance(inst)
                                showInstanceMenu = false
                            }
                        )
                    }
                }
            }
        }

        // =========================================================================
        // ROW 2: ADDON TYPES + SEARCH & ACTIONS TOOLBAR
        // =========================================================================
        val addonTypes = listOf(
            "mods" to "⚡ Mods",
            "modpacks" to "📦 Modpacks",
            "resourcepacks" to "🎨 Resource Packs",
            "shaderpacks" to "✨ Shaders",
            "datapacks" to "🗺️ Datapacks"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Addon Type Pills
            LazyRow(
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(addonTypes) { (typeId, typeLabel) ->
                    val isSelected = activeType == typeId
                    Box(
                        modifier = Modifier
                            .height(25.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (isSelected) NuxColors.ForestGreen.copy(alpha = 0.18f) else NuxColors.SurfaceElevated.copy(alpha = 0.7f),
                                RoundedCornerShape(5.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) NuxColors.ForestGreen.copy(alpha = 0.5f) else NuxColors.CardBorder,
                                shape = RoundedCornerShape(5.dp)
                            )
                            .clickable {
                                activeType = typeId
                                selectedCategory = "Semua"
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = typeLabel,
                            color = if (isSelected) NuxColors.MintGreen else NuxColors.GrayNeutral,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 9.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Search Input + Mode-specific controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (mainMode == "browse") {
                    // Search Bar
                    CompactSearchBar(
                        value = browseQuery,
                        onValueChange = { onQueryChanged(it) },
                        placeholder = "Cari di Modrinth...",
                        modifier = Modifier.width(160.dp)
                    )

                    // World Selector (Datapacks only)
                    if (activeType == "datapacks") {
                        Box {
                            Row(
                                modifier = Modifier
                                    .height(25.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(NuxColors.SurfaceElevated, RoundedCornerShape(5.dp))
                                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(5.dp))
                                    .clickable { if (worlds.isNotEmpty()) showWorldMenu = true }
                                    .padding(horizontal = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Public,
                                    contentDescription = null,
                                    tint = NuxColors.ForestGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (worlds.isNotEmpty()) "World: $selectedWorld" else "Tidak Ada World",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (worlds.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowDropDown,
                                        contentDescription = null,
                                        tint = NuxColors.GrayNeutral,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showWorldMenu,
                                onDismissRequest = { showWorldMenu = false }
                            ) {
                                worlds.forEach { w ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = w.displayName,
                                                fontWeight = if (w.folderName == selectedWorld) FontWeight.Black else FontWeight.Normal,
                                                fontSize = 11.sp
                                            )
                                        },
                                        onClick = {
                                            selectedWorld = w.folderName
                                            showWorldMenu = false
                                            reloadInstalled()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Version Filter Toggle Pill
                    Box(
                        modifier = Modifier
                            .height(25.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (filterByGameVersion) NuxColors.ForestGreen.copy(alpha = 0.15f) else NuxColors.SurfaceElevated,
                                RoundedCornerShape(5.dp)
                            )
                            .border(
                                1.dp,
                                if (filterByGameVersion) NuxColors.ForestGreen.copy(alpha = 0.4f) else NuxColors.CardBorder,
                                RoundedCornerShape(5.dp)
                            )
                            .clickable {
                                filterByGameVersion = !filterByGameVersion
                                currentPage = 1
                                performSearch(1)
                            }
                            .padding(horizontal = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (filterByGameVersion) Icons.Outlined.Check else Icons.Outlined.Close,
                                contentDescription = null,
                                tint = if (filterByGameVersion) NuxColors.ForestGreen else NuxColors.GrayNeutral,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (filterByGameVersion) currentInst.mcVersion else "Semua Versi",
                                color = if (filterByGameVersion) NuxColors.MintGreen else NuxColors.GrayNeutral,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                } else {
                    // Installed mode: Search + World Selector + Import
                    CompactSearchBar(
                        value = installedSearch,
                        onValueChange = { installedSearch = it },
                        placeholder = "Cari di ${installedItems.size} item...",
                        modifier = Modifier.width(160.dp)
                    )

                    if (activeType == "datapacks") {
                        Box {
                            Row(
                                modifier = Modifier
                                    .height(25.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(NuxColors.SurfaceElevated, RoundedCornerShape(5.dp))
                                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(5.dp))
                                    .clickable { if (worlds.isNotEmpty()) showWorldMenu = true }
                                    .padding(horizontal = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Public,
                                    contentDescription = null,
                                    tint = NuxColors.ForestGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (worlds.isNotEmpty()) "World: $selectedWorld" else "Tidak Ada World",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (worlds.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowDropDown,
                                        contentDescription = null,
                                        tint = NuxColors.GrayNeutral,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showWorldMenu,
                                onDismissRequest = { showWorldMenu = false }
                            ) {
                                worlds.forEach { w ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = w.displayName,
                                                fontWeight = if (w.folderName == selectedWorld) FontWeight.Black else FontWeight.Normal,
                                                fontSize = 11.sp
                                            )
                                        },
                                        onClick = {
                                            selectedWorld = w.folderName
                                            showWorldMenu = false
                                            reloadInstalled()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Import Button
                    Box(
                        modifier = Modifier
                            .height(25.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(NuxColors.ForestGreen.copy(alpha = 0.12f), RoundedCornerShape(5.dp))
                            .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.35f), RoundedCornerShape(5.dp))
                            .clickable {
                                if (selectedInstance == null) {
                                    Toast.makeText(context, "Pilih instance terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                val mimeTypes = getMimeTypesForType(activeType)
                                filePicker.launch(mimeTypes)
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.FileUpload,
                                contentDescription = "Impor",
                                tint = NuxColors.ForestGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "+ IMPOR",
                                color = NuxColors.ForestGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // ROW 3: CATEGORY SUB-FILTERS (BROWSE MODE ONLY, COMPACT 22DP)
        // =========================================================================
        if (mainMode == "browse") {
            val categories = CATEGORIES_MAP[activeType] ?: listOf(ModCategoryItem("Semua", "Semua"))
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat.id
                    Box(
                        modifier = Modifier
                            .height(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) NuxColors.ForestGreen.copy(alpha = 0.2f) else Color(0x08FFFFFF),
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) NuxColors.ForestGreen.copy(alpha = 0.5f) else NuxColors.CardBorder.copy(alpha = 0.4f),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                selectedCategory = cat.id
                                currentPage = 1
                                performSearch(1)
                            }
                            .padding(horizontal = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat.label,
                            color = if (isSelected) NuxColors.MintGreen else NuxColors.GrayNeutral,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 8.5.sp
                        )
                    }
                }
            }
        }

        // =========================================================================
        // MOD LIST / INSTALLED CARDS
        // =========================================================================
        if (mainMode == "installed") {
            val filteredInstalled = remember(installedItems, installedSearch) {
                if (installedSearch.isBlank()) installedItems
                else installedItems.filter {
                    it.name.contains(installedSearch, ignoreCase = true) ||
                            it.filename.contains(installedSearch, ignoreCase = true)
                }
            }

            if (isInstalledLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NuxColors.ForestGreen, modifier = Modifier.size(24.dp))
                }
            } else if (filteredInstalled.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = NuxColors.GrayNeutral,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (activeType == "datapacks" && worlds.isEmpty())
                                "Belum ada world di instance ini. Buka game untuk membuat world terlebih dahulu."
                            else if (installedSearch.isBlank())
                                "Belum ada ${activeType} terpasang di instance ini"
                            else "Tidak ada item yang cocok",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        NuxButton(
                            onClick = { mainMode = "browse" },
                            backgroundColor = NuxColors.ForestGreen,
                            cornerRadius = 6.dp
                        ) {
                            Text("JELAJAHI MODRINTH", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredInstalled, key = { it.id }) { item ->
                        NuxCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = if (item.isEnabled) NuxColors.SurfaceElevated else NuxColors.SurfaceElevated.copy(alpha = 0.6f),
                            borderColor = NuxColors.CardBorder,
                            shadowOffset = 0.dp,
                            cornerRadius = 8.dp,
                            borderWidth = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (item.isEnabled) NuxColors.ForestGreen.copy(alpha = 0.15f) else Color(0x0DFFFFFF),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.name.take(2).uppercase(),
                                            fontWeight = FontWeight.Black,
                                            color = if (item.isEnabled) NuxColors.MintGreen else NuxColors.GrayNeutral,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (item.isEnabled) Color.White else NuxColors.GrayNeutral,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!item.isEnabled) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0x1AFFFFFF), RoundedCornerShape(3.dp))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = "NONAKTIF",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = NuxColors.GrayNeutral
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${item.filename} · ${formatBytes(item.sizeBytes)}",
                                            fontSize = 9.5.sp,
                                            color = NuxColors.GrayNeutral,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = item.isEnabled,
                                        onCheckedChange = {
                                            scope.launch {
                                                val targetWorld = if (activeType == "datapacks") selectedWorld else null
                                                NuxModManager.toggleItemEnabled(context, currentInst, activeType, item, targetWorld)
                                                reloadInstalled()
                                            }
                                        },
                                        modifier = Modifier.scale(0.75f),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = NuxColors.ForestGreen,
                                            uncheckedThumbColor = NuxColors.GrayNeutral,
                                            uncheckedTrackColor = Color(0x26FFFFFF)
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(NuxColors.ErrorRed.copy(alpha = 0.12f), RoundedCornerShape(5.dp))
                                            .border(1.dp, NuxColors.ErrorRed.copy(alpha = 0.35f), RoundedCornerShape(5.dp))
                                            .clickable { itemToDelete = item },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Hapus",
                                            tint = NuxColors.ErrorRed,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // BROWSE MODRINTH
            // ==========================================
            if (isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NuxColors.ForestGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Memuat data dari Modrinth...", color = NuxColors.GrayNeutral, fontSize = 11.sp)
                    }
                }
            } else if (searchErrorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudOff,
                            contentDescription = null,
                            tint = NuxColors.ErrorRed,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Gagal Memuat dari Modrinth",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = searchErrorMessage ?: "Terjadi kesalahan jaringan",
                            color = NuxColors.GrayNeutral,
                            fontSize = 10.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        NuxButton(
                            onClick = { performSearch(currentPage) },
                            backgroundColor = NuxColors.ForestGreen,
                            cornerRadius = 6.dp
                        ) {
                            Text("COBA LAGI", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            } else if (searchHits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.SearchOff,
                            contentDescription = null,
                            tint = NuxColors.GrayNeutral,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (filterByGameVersion) {
                                "Tidak ada hasil cocok untuk Minecraft ${currentInst.mcVersion} (${currentInst.loader.uppercase()})"
                            } else {
                                "Tidak ada hasil ditemukan untuk kata kunci ini"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Coba ubah kata kunci atau nonaktifkan filter versi",
                            color = NuxColors.GrayNeutral,
                            fontSize = 10.sp
                        )
                        if (filterByGameVersion) {
                            Spacer(modifier = Modifier.height(10.dp))
                            NuxButton(
                                onClick = {
                                    filterByGameVersion = false
                                    currentPage = 1
                                    performSearch(1)
                                },
                                backgroundColor = NuxColors.ForestGreen,
                                cornerRadius = 6.dp
                            ) {
                                Text("CARI DI SEMUA VERSI MINECRAFT", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(searchHits, key = { it.projectId }) { hit ->
                        val isInstalled = installedItems.any { isModInstalled(it, hit) }

                        val isDownloading = downloadingProjectIds.containsKey(hit.projectId)
                        val downloadStatus = downloadingProjectIds[hit.projectId] ?: ""

                        NuxCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    detailModalHit = hit
                                    detailProject = null
                                    detailTab = "overview"
                                    compatibleVersions = emptyList()
                                    selectedVersion = null
                                    isDetailLoading = true
                                    scope.launch {
                                        val pRes = NuxModManager.getProjectDetails(hit.projectId)
                                        val vRes = NuxModManager.getCompatibleVersions(hit.projectId, currentInst, activeType)
                                        detailProject = pRes.getOrNull()
                                        val vers = vRes.getOrNull() ?: emptyList()
                                        compatibleVersions = vers
                                        selectedVersion = vers.firstOrNull()
                                        isDetailLoading = false
                                    }
                                },
                            backgroundColor = NuxColors.SurfaceElevated,
                            borderColor = NuxColors.CardBorder,
                            shadowOffset = 0.dp,
                            cornerRadius = 8.dp,
                            borderWidth = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Project Icon (38dp)
                                NuxNetworkImage(
                                    model = hit.iconUrl,
                                    contentDescription = hit.title,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp)),
                                    fallbackInitials = hit.title.take(2).uppercase(),
                                    shape = RoundedCornerShape(6.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                // Info Column
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = hit.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!hit.author.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "by ${hit.author}",
                                                fontSize = 9.5.sp,
                                                color = NuxColors.GrayNeutral,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (hit.projectType == "modpack") {
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(NuxColors.ForestGreen.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                                                    .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text("PACK", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NuxColors.MintGreen)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(1.dp))

                                    Text(
                                        text = hit.description ?: "Tidak ada deskripsi",
                                        fontSize = 10.sp,
                                        color = NuxColors.GrayNeutral.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0x0DFFFFFF), RoundedCornerShape(3.dp))
                                                .border(1.dp, NuxColors.CardBorder.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "📥 ${formatCompactNumber(hit.downloads)}",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = NuxColors.GrayNeutral
                                            )
                                        }

                                        hit.categories?.take(2)?.forEach { cat ->
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0x08FFFFFF), RoundedCornerShape(3.dp))
                                                    .border(1.dp, NuxColors.CardBorder.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = cat,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = NuxColors.GrayNeutral.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Action Button
                                if (isDownloading) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(
                                            color = NuxColors.ForestGreen,
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = downloadStatus.ifBlank { "Unduh..." },
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NuxColors.ForestGreen
                                        )
                                    }
                                } else if (isInstalled) {
                                    Box(
                                        modifier = Modifier
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(NuxColors.ForestGreen.copy(alpha = 0.12f), RoundedCornerShape(5.dp))
                                            .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.35f), RoundedCornerShape(5.dp))
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = NuxColors.ForestGreen,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "TERPASANG",
                                                color = NuxColors.ForestGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                } else {
                                    val buttonLabel = if (hit.projectType == "modpack") "+ PASANG PACK" else "+ PASANG"
                                    Box(
                                        modifier = Modifier
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(NuxColors.ForestGreen, RoundedCornerShape(5.dp))
                                            .border(1.dp, NuxColors.MintGreen.copy(alpha = 0.4f), RoundedCornerShape(5.dp))
                                            .clickable {
                                                scope.launch {
                                                    downloadingProjectIds = downloadingProjectIds + (hit.projectId to "Mencari versi...")
                                                    val vRes = NuxModManager.getCompatibleVersions(hit.projectId, currentInst, activeType)
                                                    val compList = vRes.getOrNull()
                                                    if (compList.isNullOrEmpty()) {
                                                        Toast.makeText(context, "Tidak ada versi cocok untuk ${currentInst.mcVersion}!", Toast.LENGTH_SHORT).show()
                                                        downloadingProjectIds = downloadingProjectIds - hit.projectId
                                                        return@launch
                                                    }

                                                    val targetVer = compList.first()
                                                    downloadingProjectIds = downloadingProjectIds + (hit.projectId to "Mengunduh...")

                                                    val targetWorld = if (activeType == "datapacks") selectedWorld else null
                                                    val downRes = NuxModManager.downloadVersionAndDependencies(
                                                        context = context,
                                                        instance = currentInst,
                                                        version = targetVer,
                                                        itemType = activeType,
                                                        worldName = targetWorld,
                                                        onProgress = { status ->
                                                            downloadingProjectIds = downloadingProjectIds + (hit.projectId to status)
                                                        }
                                                    )

                                                    downloadingProjectIds = downloadingProjectIds - hit.projectId

                                                    if (downRes.isSuccess) {
                                                        val downloaded = downRes.getOrNull() ?: emptyList()
                                                        val depCount = (downloaded.size - 1).coerceAtLeast(0)
                                                        val msg = if (activeType == "modpacks") {
                                                            "Modpack ${hit.title} berhasil dipasang!"
                                                        } else if (depCount > 0) {
                                                            "Berhasil memasang ${hit.title} beserta $depCount dependensi!"
                                                        } else {
                                                            "Berhasil memasang ${hit.title}!"
                                                        }
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                        reloadInstalled()
                                                    } else {
                                                        Toast.makeText(context, "Gagal mengunduh: ${downRes.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = buttonLabel,
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Pagination
                    item {
                        val totalPages = ((totalHits + 17) / 18).coerceAtLeast(1)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (currentPage > 1) NuxColors.SurfaceElevated else Color(0x08FFFFFF), RoundedCornerShape(5.dp))
                                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(5.dp))
                                    .clickable(enabled = currentPage > 1) { performSearch(currentPage - 1) }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text("« SEBELUMNYA", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = if (currentPage > 1) Color.White else NuxColors.GrayNeutral.copy(alpha = 0.35f))
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "$currentPage / $totalPages",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                color = NuxColors.GrayNeutral
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (currentPage < totalPages) NuxColors.SurfaceElevated else Color(0x08FFFFFF), RoundedCornerShape(5.dp))
                                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(5.dp))
                                    .clickable(enabled = currentPage < totalPages) { performSearch(currentPage + 1) }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text("SELANJUTNYA »", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = if (currentPage < totalPages) Color.White else NuxColors.GrayNeutral.copy(alpha = 0.35f))
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 4. DELETE CONFIRMATION DIALOG ---
    itemToDelete?.let { item ->
        NuxDialog(onDismissRequest = { itemToDelete = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = NuxColors.ErrorRed,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (activeType == "modpacks") "Hapus Modpack?" else "Hapus ${item.category}?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (activeType == "modpacks") {
                        "Yakin ingin menghapus modpack \"${item.name}\"? Semua mod yang terpasang dari modpack ini juga akan dihapus dari instance."
                    } else {
                        "Yakin ingin menghapus \"${item.name}\" dari instance ini?"
                    },
                    fontSize = 11.sp,
                    color = NuxColors.GrayNeutral,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NuxButton(
                        onClick = { itemToDelete = null },
                        backgroundColor = NuxColors.SurfaceElevated,
                        borderColor = NuxColors.CardBorder,
                        cornerRadius = 6.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("BATAL", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = Color.White)
                    }
                    NuxButton(
                        onClick = {
                            if (selectedInstance != null) {
                                scope.launch {
                                    val targetWorld = if (activeType == "datapacks") selectedWorld else null
                                    NuxModManager.deleteItem(context, selectedInstance!!, activeType, item, targetWorld)
                                    itemToDelete = null
                                    reloadInstalled()
                                    val toastMsg = if (activeType == "modpacks") {
                                        "Modpack dan semua mod berhasil dihapus!"
                                    } else {
                                        "${item.name} berhasil dihapus!"
                                    }
                                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        backgroundColor = NuxColors.ErrorRed,
                        borderColor = NuxColors.CardBorder,
                        contentColor = Color.White,
                        cornerRadius = 6.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("HAPUS", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = Color.White)
                    }
                }
            }
        }
    }

    // --- 4.5 MODPACK IMPORT PROGRESS DIALOG ---
    if (isImportingModpack) {
        NuxDialog(onDismissRequest = { /* Tidak bisa ditutup saat sedang mengunduh */ }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(NuxColors.ForestGreen.copy(alpha = 0.15f))
                        .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = NuxColors.ForestGreen,
                        strokeWidth = 2.5.dp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Mengimpor & Memasang Modpack",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )

                if (modpackImportName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = modpackImportName,
                        fontSize = 11.sp,
                        color = NuxColors.ForestGreen,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (modpackImportStatus.isNotBlank()) modpackImportStatus else "Membaca daftar mod dan mengunduh file...",
                    fontSize = 11.sp,
                    color = NuxColors.GrayNeutral,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (modpackImportTotal > 0) {
                    val progressRatio = (modpackImportCurrent.toFloat() / modpackImportTotal.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NuxColors.ForestGreen,
                        trackColor = NuxColors.SurfaceElevated
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$modpackImportCurrent / $modpackImportTotal file mod",
                        fontSize = 10.sp,
                        color = NuxColors.GrayNeutral
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NuxColors.ForestGreen,
                        trackColor = NuxColors.SurfaceElevated
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Launcher sedang membaca daftar mod dan mengunduh modpack seperti di versi Windows. Mohon tunggu.",
                    fontSize = 9.5.sp,
                    color = NuxColors.GrayNeutral.copy(alpha = 0.6f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    // --- 5. MODRINTH PROJECT DETAIL MODAL ---
    detailModalHit?.let { hit ->
        val isHitInstalled = installedItems.any { isModInstalled(it, hit) }

        NuxDialog(
            onDismissRequest = {
                if (!isDetailInstalling) detailModalHit = null
            },
            modifier = Modifier.fillMaxWidth(0.92f),
            fillMaxHeight = true
        ) {
            val modalOuterShape = RoundedCornerShape(20.dp)
            val modalInnerShape = RoundedCornerShape(14.dp)
            val subCardShape = RoundedCornerShape(10.dp)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(modalOuterShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color(0xFF151922), Color(0xFF0E1017))
                        )
                    )
                    .border(1.dp, Color(0x2EFFFFFF), modalOuterShape)
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(subCardShape)
                                    .background(Color(0xFF181C26))
                                    .border(1.dp, Color(0x33FFFFFF), subCardShape),
                                contentAlignment = Alignment.Center
                            ) {
                                NuxNetworkImage(
                                    model = hit.iconUrl,
                                    contentDescription = hit.title,
                                    modifier = Modifier.fillMaxSize(),
                                    fallbackInitials = hit.title.take(2).uppercase(),
                                    shape = subCardShape
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = hit.title,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                when (hit.projectType) {
                                                    "mod" -> Color(0xFF10B981).copy(alpha = 0.18f)
                                                    "modpack" -> Color(0xFF8B5CF6).copy(alpha = 0.18f)
                                                    "resourcepack" -> Color(0xFF38BDF8).copy(alpha = 0.18f)
                                                    else -> Color(0xFFF59E0B).copy(alpha = 0.18f)
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                when (hit.projectType) {
                                                    "mod" -> NuxColors.ForestGreen.copy(alpha = 0.5f)
                                                    "modpack" -> Color(0xFF8B5CF6).copy(alpha = 0.5f)
                                                    "resourcepack" -> Color(0xFF38BDF8).copy(alpha = 0.5f)
                                                    else -> Color(0xFFF59E0B).copy(alpha = 0.5f)
                                                },
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = hit.projectType.uppercase(),
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (hit.projectType) {
                                                "mod" -> NuxColors.MintGreen
                                                "modpack" -> Color(0xFFA78BFA)
                                                "resourcepack" -> Color(0xFF7DD3FC)
                                                else -> Color(0xFFFCD34D)
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "oleh ${hit.author ?: "Komunitas"}",
                                        fontSize = 10.sp,
                                        color = Color(0xFFA1A1AA),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "·",
                                        color = Color(0xFF52525B),
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "📥 ${formatCompactNumber(hit.downloads)}",
                                        fontSize = 10.sp,
                                        color = Color(0xFFA1A1AA),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Close Pill Button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E222D))
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                .clickable(enabled = !isDetailInstalling) { detailModalHit = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Tutup",
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Segmented Tab Switcher (Double Bezel)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0C0E14))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
                            .padding(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isTabOverview = detailTab == "overview"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isTabOverview) Color(0xFF181C26) else Color.Transparent)
                                    .border(
                                        width = if (isTabOverview) 1.dp else 0.dp,
                                        color = if (isTabOverview) Color(0x33FFFFFF) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { detailTab = "overview" },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Description,
                                        contentDescription = null,
                                        tint = if (isTabOverview) NuxColors.MintGreen else Color(0xFF71717A),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "RINGKASAN & GALERI",
                                        fontSize = 10.sp,
                                        fontWeight = if (isTabOverview) FontWeight.Black else FontWeight.SemiBold,
                                        color = if (isTabOverview) Color.White else Color(0xFF71717A)
                                    )
                                }
                            }

                            val isTabVersions = detailTab == "versions"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isTabVersions) Color(0xFF181C26) else Color.Transparent)
                                    .border(
                                        width = if (isTabVersions) 1.dp else 0.dp,
                                        color = if (isTabVersions) Color(0x33FFFFFF) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { detailTab = "versions" },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Layers,
                                        contentDescription = null,
                                        tint = if (isTabVersions) NuxColors.MintGreen else Color(0xFF71717A),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "VERSI KOMPATIBEL (${compatibleVersions.size})",
                                        fontSize = 10.sp,
                                        fontWeight = if (isTabVersions) FontWeight.Black else FontWeight.SemiBold,
                                        color = if (isTabVersions) Color.White else Color(0xFF71717A)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Detail Content Scrollable (Inner Core)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(modalInnerShape)
                            .background(Color(0xFF0F1218))
                            .border(1.dp, Color(0x1AFFFFFF), modalInnerShape)
                            .padding(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (isDetailLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = NuxColors.ForestGreen, modifier = Modifier.size(26.dp))
                                }
                            } else if (detailTab == "overview") {
                                // TAB 1: OVERVIEW & GALLERY
                                val galleryList = detailProject?.gallery ?: emptyList()
                                if (galleryList.isNotEmpty()) {
                                    Text(
                                        text = "TANGKAPAN LAYAR",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = Color(0xFF71717A),
                                        letterSpacing = 0.8.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(galleryList) { gal ->
                                            Box(
                                                modifier = Modifier
                                                    .width(170.dp)
                                                    .height(96.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF141720))
                                                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(8.dp))
                                                    .clickable { previewGalleryUrl = gal.url }
                                            ) {
                                                NuxNetworkImage(
                                                    model = gal.url,
                                                    contentDescription = gal.title ?: "Screenshot",
                                                    modifier = Modifier.fillMaxSize(),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                Text(
                                    text = "DESKRIPSI LENGKAP",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = Color(0xFF71717A),
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = detailProject?.description ?: hit.description ?: "Tidak ada ringkasan deskripsi.",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFE2E8F0),
                                    lineHeight = 17.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Category & Loader Tags
                                val allLoaders = (detailProject?.loaders ?: hit.categories ?: emptyList()).distinct()
                                if (allLoaders.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        allLoaders.take(5).forEach { tag ->
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFF181C26), RoundedCornerShape(6.dp))
                                                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = tag.uppercase(),
                                                    fontSize = 8.5.sp,
                                                    color = Color(0xFFA1A1AA),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // TAB 2: COMPATIBLE VERSIONS ONLY
                                if (compatibleVersions.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF43F5E).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFFF43F5E).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "⚠️ Tidak Ada Versi yang Didukung",
                                                color = Color(0xFFFDA4AF),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "Mod ini tidak memiliki rilis yang cocok dengan Minecraft ${selectedInstance?.mcVersion ?: ""} (${(selectedInstance?.loader ?: "Vanilla").uppercase()}).",
                                                color = Color(0xFFE2E8F0),
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        compatibleVersions.forEach { ver ->
                                            val isPicked = selectedVersion?.id == ver.id
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isPicked) Color(0xFF10B981).copy(alpha = 0.14f) else Color(0xFF141720),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isPicked) NuxColors.ForestGreen else Color(0x14FFFFFF),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { selectedVersion = ver }
                                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(14.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isPicked) NuxColors.ForestGreen else Color.Transparent)
                                                                .border(1.5.dp, if (isPicked) NuxColors.MintGreen else Color(0xFF71717A), CircleShape)
                                                        )
                                                        Spacer(modifier = Modifier.width(9.dp))
                                                        Column {
                                                            Text(
                                                                text = ver.name.ifBlank { ver.versionNumber },
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.5.sp,
                                                                color = Color.White,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "Versi: ${ver.versionNumber} · MC: ${ver.gameVersions.joinToString(", ")}",
                                                                fontSize = 9.sp,
                                                                color = Color(0xFFA1A1AA)
                                                            )
                                                        }
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                when (ver.versionType) {
                                                                    "release" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                                    "beta" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                                                    else -> Color(0xFFF43F5E).copy(alpha = 0.15f)
                                                                },
                                                                RoundedCornerShape(4.dp)
                                                            )
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = ver.versionType.uppercase(),
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = when (ver.versionType) {
                                                                "release" -> NuxColors.MintGreen
                                                                "beta" -> Color(0xFFFBBF24)
                                                                else -> Color(0xFFFDA4AF)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF141720), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoFixHigh,
                                        contentDescription = null,
                                        tint = NuxColors.ForestGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (hit.projectType == "modpack")
                                            "Modpack akan diekstrak langsung ke instance (overrides, konfigurasi, dan modpack manifest)."
                                        else
                                            "Auto-Download Dependensi aktif: dependensi wajib akan otomatis ikut terpasang.",
                                        fontSize = 9.sp,
                                        color = Color(0xFFA1A1AA)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. Bottom Action Area
                    if (isDetailInstalling) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = NuxColors.ForestGreen, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = detailProgressText.ifBlank { "Sedang mengunduh dan memasang..." },
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                color = NuxColors.MintGreen
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NuxButton(
                                onClick = { detailModalHit = null },
                                backgroundColor = Color(0xFF1E222D),
                                borderColor = Color(0x33FFFFFF),
                                cornerRadius = 10.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("TUTUP", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = Color(0xFFA1A1AA))
                            }

                            if (isHitInstalled) {
                                Box(
                                    modifier = Modifier
                                        .weight(2f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                        .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = NuxColors.MintGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "SUDAH TERPASANG",
                                            color = NuxColors.MintGreen,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            } else {
                                val canInstall = compatibleVersions.isNotEmpty()
                                Box(
                                    modifier = Modifier
                                        .weight(2f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (canInstall) androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                listOf(Color(0xFF059669), Color(0xFF10B981))
                                            ) else androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                listOf(Color(0xFF27272A), Color(0xFF18181B))
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            if (canInstall) Color(0x6634D399) else Color(0x1AFFFFFF),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable(enabled = canInstall) {
                                            val ver = selectedVersion ?: compatibleVersions.firstOrNull()
                                            val inst = selectedInstance
                                            if (ver != null && inst != null) {
                                                scope.launch {
                                                    isDetailInstalling = true
                                                    detailProgressText = "Memulai pengunduhan..."

                                                    val targetWorld = if (activeType == "datapacks") selectedWorld else null
                                                    val res = NuxModManager.downloadVersionAndDependencies(
                                                        context = context,
                                                        instance = inst,
                                                        version = ver,
                                                        itemType = activeType,
                                                        worldName = targetWorld,
                                                        onProgress = { status ->
                                                            detailProgressText = status
                                                        }
                                                    )

                                                    isDetailInstalling = false

                                                    if (res.isSuccess) {
                                                        val files = res.getOrNull() ?: emptyList()
                                                        val depCount = (files.size - 1).coerceAtLeast(0)
                                                        val msg = if (activeType == "modpacks") {
                                                            "Modpack ${hit.title} berhasil dipasang!"
                                                        } else if (depCount > 0) {
                                                            "Berhasil memasang ${hit.title} beserta $depCount dependensi!"
                                                        } else {
                                                            "Berhasil memasang ${hit.title}!"
                                                        }
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                        reloadInstalled()
                                                    } else {
                                                        Toast.makeText(context, "Gagal mengunduh: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Download,
                                            contentDescription = null,
                                            tint = if (canInstall) Color.White else Color(0xFF71717A),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (hit.projectType == "modpack") "PASANG MODPACK" else "PASANG KE INSTANCE",
                                            color = if (canInstall) Color.White else Color(0xFF71717A),
                                            fontWeight = FontWeight.Black,
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
    }

    // --- 5.5 MODAL PREVIEW GALERI GAMBAR FULLSCREEN ---
    previewGalleryUrl?.let { url ->
        NuxDialog(onDismissRequest = { previewGalleryUrl = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tangkapan Layar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(NuxColors.SurfaceElevated, CircleShape)
                            .border(1.dp, NuxColors.CardBorder, CircleShape)
                            .clickable { previewGalleryUrl = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "Tutup", tint = Color.White, modifier = Modifier.size(13.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                NuxNetworkImage(
                    model = url,
                    contentDescription = "Preview Gambar",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

/**
 * Ultra-Compact Cyber-Glass Search Bar for Mobile Landscape layout
 */
@Composable
private fun CompactSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(25.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(NuxColors.SurfaceInput, RoundedCornerShape(5.dp))
            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = NuxColors.GrayNeutral,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = NuxColors.GrayNeutral.copy(alpha = 0.5f),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(NuxColors.ForestGreen),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Hapus",
                tint = NuxColors.GrayNeutral,
                modifier = Modifier
                    .size(12.dp)
                    .clickable { onValueChange("") }
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
    return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

private fun formatCompactNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> DecimalFormat("#,##0.#").format(number / 1_000_000.0) + "M"
        number >= 1_000 -> DecimalFormat("#,##0.#").format(number / 1_000.0) + "K"
        else -> number.toString()
    }
}
