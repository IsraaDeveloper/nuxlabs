package com.israadev.nuxlauncher.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.core.ai.AIChatMessage
import com.israadev.nuxlauncher.core.ai.NuxAIEngine
import com.israadev.nuxlauncher.core.crash.AICrashAnalyzer
import com.israadev.nuxlauncher.core.crash.AICrashQuotaManager
import com.israadev.nuxlauncher.core.crash.AIStreamState
import com.israadev.nuxlauncher.core.instance.InstanceManager
import com.israadev.nuxlauncher.core.settings.SettingsManager
import com.israadev.nuxlauncher.ui.components.NuxButton
import com.israadev.nuxlauncher.ui.components.NuxMarkdownView
import com.israadev.nuxlauncher.ui.theme.NuxColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val instances by InstanceManager.instances.collectAsState()
    val settings by SettingsManager.settings.collectAsState()

    // Instance aktif yang dipilih untuk konteks AI
    var selectedInstanceIndex by remember { mutableIntStateOf(0) }
    val currentInstance = remember(instances, selectedInstanceIndex) {
        if (instances.isNotEmpty() && selectedInstanceIndex in instances.indices) {
            instances[selectedInstanceIndex]
        } else instances.firstOrNull()
    }
    var showInstanceSelectorMenu by remember { mutableStateOf(false) }

    // Quota State
    var remainingQuota by remember {
        mutableIntStateOf(AICrashQuotaManager.getRemainingQuota(context, settings))
    }

    LaunchedEffect(Unit) {
        val synced = AICrashQuotaManager.syncQuotaFromDatabase(context, settings)
        remainingQuota = synced
    }

    // Chat History State
    val chatMessages = remember { mutableStateListOf<AIChatMessage>() }
    var currentInputText by remember { mutableStateOf("") }
    var streamState by remember { mutableStateOf<AIStreamState>(AIStreamState.Idle) }

    // Auto-scroll list state
    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size, streamState) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // Cursor Blink for real-time typewriter effect
    var isCursorBlinkVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(450)
            isCursorBlinkVisible = !isCursorBlinkVisible
        }
    }

    val effectiveModel = remember(settings.aiModel) {
        AICrashAnalyzer.getEffectiveModel(settings)
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || streamState is AIStreamState.Connecting || streamState is AIStreamState.Streaming) {
            return
        }

        if (!AICrashQuotaManager.hasQuota(context, settings)) {
            Toast.makeText(context, "Batas kuota gratis AI hari ini telah habis (reset 00:00 WIB).", Toast.LENGTH_LONG).show()
            streamState = AIStreamState.QuotaExceeded
            return
        }

        // Tambahkan pesan user ke list
        val userMsg = AIChatMessage(role = "user", content = trimmed)
        chatMessages.add(userMsg)
        currentInputText = ""

        // Buat placeholder pesan AI
        val aiMsgId = java.util.UUID.randomUUID().toString()
        val aiMsg = AIChatMessage(id = aiMsgId, role = "assistant", content = "")
        chatMessages.add(aiMsg)

        // Kumpulkan konteks instance aktif
        val instanceContext = NuxAIEngine.collectInstanceContext(context, currentInstance, settings)

        coroutineScope.launch {
            NuxAIEngine.streamChat(
                context = context,
                chatHistory = chatMessages.filter { it.id != aiMsgId },
                instanceContext = instanceContext,
                settings = settings
            ).collect { state ->
                streamState = state
                when (state) {
                    is AIStreamState.Streaming -> {
                        val index = chatMessages.indexOfFirst { it.id == aiMsgId }
                        if (index != -1) {
                            chatMessages[index] = chatMessages[index].copy(content = state.fullText)
                        }
                    }
                    is AIStreamState.Completed -> {
                        val index = chatMessages.indexOfFirst { it.id == aiMsgId }
                        if (index != -1) {
                            chatMessages[index] = chatMessages[index].copy(content = state.fullText)
                        }
                        remainingQuota = AICrashQuotaManager.getRemainingQuota(context, settings)
                    }
                    is AIStreamState.Error -> {
                        val index = chatMessages.indexOfFirst { it.id == aiMsgId }
                        if (index != -1) {
                            chatMessages[index] = chatMessages[index].copy(
                                content = "⚠️ **Terjadi Kesalahan:** ${state.errorMessage}\n\nSilakan coba tanyakan kembali atau periksa koneksi internet."
                            )
                        }
                    }
                    is AIStreamState.QuotaExceeded -> {
                        val index = chatMessages.indexOfFirst { it.id == aiMsgId }
                        if (index != -1) {
                            chatMessages[index] = chatMessages[index].copy(
                                content = "⏱️ **Kuota Harian AI Telah Habis (5/5)**\n\nBatas penggunaan gratis harian telah tercapai. Kuota akan otomatis di-reset pada jam 00:00 WIB besok. Anda juga dapat menggunakan Custom API Key pribadi di Pengaturan untuk akses Unlimited."
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    val outerShape = RoundedCornerShape(16.dp)
    val cardShape = RoundedCornerShape(12.dp)
    val pillShape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A0F))
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(outerShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B1018),
                            Color(0xFF080C12)
                        )
                    )
                )
                .border(1.dp, Color(0x1F10B981), outerShape)
                .padding(10.dp)
        ) {
            // ==========================================
            // 1. TOP HEADER BAR
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Tombol Back
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF141A24))
                            .border(1.dp, Color(0x26FFFFFF), CircleShape)
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Glowing AI Icon
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0x3310B981), Color(0x11059669))
                                )
                            )
                            .border(1.dp, Color(0x4D10B981), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Psychology,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NUX AI ASSISTANT",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Dynamic Model Tag
                            val modelTag = when {
                                effectiveModel.contains("lightning", ignoreCase = true) -> "Nemotron Lightning"
                                effectiveModel.contains("120b", ignoreCase = true) -> "Nemotron 120B"
                                effectiveModel.contains("550b", ignoreCase = true) -> "Nemotron 550B"
                                effectiveModel.contains("gemini", ignoreCase = true) -> "Gemini Flash"
                                effectiveModel.contains("llama", ignoreCase = true) -> "Llama 3.3"
                                effectiveModel.contains("qwen", ignoreCase = true) -> "Qwen 3.8"
                                effectiveModel.contains("deepseek", ignoreCase = true) -> "DeepSeek R1"
                                effectiveModel.contains("mistral", ignoreCase = true) -> "Mistral"
                                else -> "Nemotron AI"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x2238BDF8))
                                    .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = modelTag,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                        Text(
                            text = "Diagnosa Mod, Log Crash, Analisis Renderer & Konsultasi Minecraft",
                            fontSize = 8.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Controls Kanan: Quota Pill & Clear Chat
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Badge Kuota
                    val isUnlimited = remainingQuota < 0
                    val quotaText = if (isUnlimited) "UNLIMITED" else "KUOTA: $remainingQuota/5"
                    val quotaColor = when {
                        isUnlimited -> Color(0xFF38BDF8)
                        remainingQuota > 1 -> Color(0xFF10B981)
                        remainingQuota == 1 -> Color(0xFFFBBF24)
                        else -> Color(0xFFF43F5E)
                    }

                    Box(
                        modifier = Modifier
                            .clip(pillShape)
                            .background(quotaColor.copy(alpha = 0.12f))
                            .border(1.dp, quotaColor.copy(alpha = 0.35f), pillShape)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = quotaText,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = quotaColor
                        )
                    }

                    // Clear Chat Button
                    if (chatMessages.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .height(26.dp)
                                .clip(pillShape)
                                .background(Color(0xFF1A1F2C))
                                .border(1.dp, Color(0x26FFFFFF), pillShape)
                                .clickable {
                                    chatMessages.clear()
                                    streamState = AIStreamState.Idle
                                }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteSweep,
                                    contentDescription = null,
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "BERSIHKAN",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 2. CONTEXT RIBBON & INSTANCE SELECTOR
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(pillShape)
                    .background(Color(0xFF0F1521))
                    .border(1.dp, Color(0x1F38BDF8), pillShape)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
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
                        Icon(
                            imageVector = Icons.Outlined.Layers,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "INSTANCE AKTIF: ",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = currentInstance?.name ?: "(Tidak ada)",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentInstance != null) {
                            Text(
                                text = " • ${currentInstance.mcVersion} (${currentInstance.loader.uppercase()}) • RENDERER: ${settings.selectedRenderer.uppercase()}",
                                fontSize = 8.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Dropdown ganti instance (jika lebih dari 1)
                    if (instances.size > 1) {
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1E293B))
                                    .clickable { showInstanceSelectorMenu = true }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("GANTI", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                            }

                            DropdownMenu(
                                expanded = showInstanceSelectorMenu,
                                onDismissRequest = { showInstanceSelectorMenu = false }
                            ) {
                                instances.forEachIndexed { idx, inst ->
                                    DropdownMenuItem(
                                        text = { Text("${inst.name} (${inst.mcVersion})", fontSize = 10.sp) },
                                        onClick = {
                                            selectedInstanceIndex = idx
                                            showInstanceSelectorMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ==========================================
            // 3. QUICK ACTIONS CHIPS (AKSI CEPAT)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val quickActions = listOf(
                    Triple("📊 Analisis Instance", "Tolong analisis instance ${currentInstance?.name ?: ""} (${currentInstance?.mcVersion ?: ""}), apakah mod dan spesifikasinya sudah optimal?", Color(0xFF10B981)),
                    Triple("🔍 Diagnosa Log", "Tolong periksa cuplikan log game terakhir saya dan analisis apakah ada error atau warning yang perlu diperbaiki.", Color(0xFF38BDF8)),
                    Triple("⚡ Rekomendasi Renderer", "Renderer apa dan pengaturan grafik apa yang paling bagus untuk Minecraft ${currentInstance?.mcVersion ?: ""} di HP saya agar FPS stabil?", Color(0xFFF59E0B)),
                    Triple("💡 Mod FPS Terbaik", "Apa saja rekomendasi mod peningkat FPS dan optimasi memori terbaik untuk versi ${currentInstance?.mcVersion ?: ""} (${currentInstance?.loader ?: "Fabric"})?", Color(0xFFA855F7))
                )

                quickActions.forEach { (label, prompt, chipColor) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(chipColor.copy(alpha = 0.08f))
                            .border(1.dp, chipColor.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .clickable(enabled = streamState !is AIStreamState.Connecting && streamState !is AIStreamState.Streaming) {
                                sendMessage(prompt)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = chipColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ==========================================
            // 4. CHAT HISTORY CONVERSATION
            // ==========================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(Color(0xFF090D14))
                    .border(1.dp, Color(0x1F22D3EE), cardShape)
                    .padding(8.dp)
            ) {
                if (chatMessages.isEmpty()) {
                    // WELCOME HERO CARD
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0x1F10B981))
                                .border(1.dp, Color(0x4D10B981), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✨", fontSize = 22.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Selamat Datang di NUX AI Assistant",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "AI ini otomatis membaca metadata versi Minecraft, daftar mod, konfigurasi renderer grafis, dan log game terakhir Anda untuk memberikan analisa paling akurat.",
                            fontSize = 9.5.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 13.sp,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Suggestion Pills
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(pillShape)
                                    .background(Color(0xFF131B26))
                                    .border(1.dp, Color(0x2638BDF8), pillShape)
                                    .clickable { sendMessage("Halo NUX AI! Berikan gambaran singkat kondisi instance Minecraft saya sekarang.") }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("🚀 Cek Kondisi Instance Saya", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            }
                            Box(
                                modifier = Modifier
                                    .clip(pillShape)
                                    .background(Color(0xFF131B26))
                                    .border(1.dp, Color(0x2610B981), pillShape)
                                    .clickable { sendMessage("Tolong rekomendasikan shader pack dan mod pendukung yang ringan untuk HP saya.") }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("✨ Tanya Shader Ringan", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        items(chatMessages, key = { it.id }) { msg ->
                            val isUser = msg.role == "user"
                            val isStreamingThis = !isUser && streamState is AIStreamState.Streaming && msg.id == chatMessages.lastOrNull()?.id

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                if (!isUser) {
                                    // AI Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("AI", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF34D399))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                // Message Bubble (Double-bezel / nested container)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(if (isUser) 0.8f else 0.92f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isUser) Color(0xFF16202E) else Color(0xFF0F1520)
                                        )
                                        .border(
                                            1.dp,
                                            if (isUser) Color(0x4038BDF8) else Color(0x2E10B981),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        if (!isUser) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "NUX AI DIAGNOSTICIAN",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 7.5.sp,
                                                    color = Color(0xFF34D399),
                                                    letterSpacing = 0.5.sp
                                                )

                                                // Copy Button
                                                if (msg.content.isNotBlank() && !isStreamingThis) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.ContentCopy,
                                                        contentDescription = "Salin",
                                                        tint = Color(0xFF64748B),
                                                        modifier = Modifier
                                                            .size(13.dp)
                                                            .clickable {
                                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                                clipboard.setPrimaryClip(ClipData.newPlainText("AI Answer", msg.content))
                                                                Toast.makeText(context, "Respon AI disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                                                            }
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }

                                        if (isUser) {
                                            Text(
                                                text = msg.content,
                                                color = Color.White,
                                                fontSize = 9.5.sp,
                                                lineHeight = 13.5.sp
                                            )
                                        } else {
                                            if (msg.content.isBlank() && (streamState is AIStreamState.Connecting || streamState is AIStreamState.Idle)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(11.dp),
                                                        color = Color(0xFF38BDF8),
                                                        strokeWidth = 1.5.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "AI sedang membaca konteks & berpikir...",
                                                        fontSize = 8.5.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }
                                            } else {
                                                NuxMarkdownView(
                                                    markdownText = msg.content,
                                                    isStreaming = isStreamingThis,
                                                    showCursor = isCursorBlinkVisible && isStreamingThis
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

            Spacer(modifier = Modifier.height(6.dp))

            // ==========================================
            // 5. INPUT FIELD BAR
            // ==========================================
            val isBusy = streamState is AIStreamState.Connecting || streamState is AIStreamState.Streaming

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(pillShape)
                    .background(Color(0xFF0F1521))
                    .border(1.dp, Color(0x33FFFFFF), pillShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = currentInputText,
                    onValueChange = { currentInputText = it },
                    placeholder = {
                        Text(
                            text = "Tanyakan seputar mod, error log, renderer, atau masalah game...",
                            fontSize = 9.sp,
                            color = Color(0xFF64748B)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(0.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Send Button
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isBusy || currentInputText.isBlank()) {
                                Brush.horizontalGradient(listOf(Color(0xFF1E293B), Color(0xFF1E293B)))
                            } else {
                                Brush.horizontalGradient(listOf(Color(0xFF059669), Color(0xFF10B981)))
                            }
                        )
                        .clickable(enabled = !isBusy && currentInputText.isNotBlank()) {
                            sendMessage(currentInputText)
                        }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            color = Color(0xFF38BDF8),
                            strokeWidth = 1.5.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "KIRIM",
                                fontWeight = FontWeight.Black,
                                fontSize = 8.5.sp,
                                color = if (currentInputText.isNotBlank()) Color.White else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Outlined.Send,
                                contentDescription = "Kirim",
                                tint = if (currentInputText.isNotBlank()) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
