package com.israadev.nuxlauncher.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.social.*
import com.israadev.nuxlauncher.ui.components.NuxBadge
import com.israadev.nuxlauncher.ui.components.NuxNetworkImage
import com.israadev.nuxlauncher.ui.components.NuxUserBadge
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * NUX Launcher Social & Voice Hub (Android Landscape Optimized)
 * Clean Neo-Brutalist Soft Green palette matching Windows & Android Dashboard
 */
@Composable
fun FriendsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        NuxSocialManager.startSync()
        onDispose {}
    }

    val friends by NuxSocialManager.friends.collectAsState()
    val activeFriend by NuxSocialManager.activeChatFriend.collectAsState()
    val chatMessages by NuxSocialManager.chatMessages.collectAsState()
    val isFriendTyping by NuxSocialManager.isFriendTyping.collectAsState()
    val voiceRooms by NuxSocialManager.voiceRooms.collectAsState()
    val activeVoiceRoom by NuxSocialManager.activeVoiceRoom.collectAsState()
    val voiceMessages by NuxSocialManager.voiceMessages.collectAsState()
    val searchResults by NuxSocialManager.searchResults.collectAsState()
    val isSearching by NuxSocialManager.isSearching.collectAsState()

    var selectedTab by remember { mutableStateOf("friends") } // "room_chat", "friends", "rooms", "requests"
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(activeVoiceRoom?.id) {
        if (activeVoiceRoom != null) {
            NuxSocialManager.closeChat()
            selectedTab = "room_chat"
        } else if (selectedTab == "room_chat") {
            selectedTab = "friends"
        }
    }

    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var roomToJoinWithPassword by remember { mutableStateOf<NuxVoiceRoom?>(null) }
    var replyingToMessage by remember { mutableStateOf<NuxChatMessage?>(null) }

    val acceptedFriends = remember(friends) { friends.filter { it.isAccepted } }
    val pendingReceived = remember(friends) { friends.filter { it.isPendingReceived } }
    val pendingSent = remember(friends) { friends.filter { it.isPendingSent } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NuxColors.Background)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
    ) {
        // =====================================================================
        // 1. TOP HEADER BAR (Minimalist Cyber-Glass matching Home, Accounts & Mods)
        // =====================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Back Button
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

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "SOCIAL",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 0.8.sp
                )
            }

            // Top Action Buttons: Cari Teman & Buat Room
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Button: Cari Teman
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NuxColors.SurfaceElevated, RoundedCornerShape(6.dp))
                        .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                        .clickable { showAddFriendDialog = true }
                        .padding(horizontal = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = NuxColors.ForestGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("+ CARI TEMAN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                    }
                }

                // Button: Buat Voice Room
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NuxColors.ForestGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .clickable { showCreateRoomDialog = true }
                        .padding(horizontal = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = NuxColors.MintGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("+ BUAT ROOM", color = NuxColors.MintGreen, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                    }
                }
            }
        }

        // =====================================================================
        // 2. MAIN 2-COLUMN STAGE (Left Panel ~300dp + Right Interactive Stage)
        // =====================================================================
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // -------------------------------------------------------------
            // A. LEFT PANEL: DIRECTORY CARD
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NuxColors.SurfaceElevated, RoundedCornerShape(8.dp))
                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                // Segmented Tabs: CHAT (if in active room) | TEMAN | VOICE | REQUEST
                if (activeVoiceRoom != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PillTab(
                            title = "CHAT",
                            count = voiceMessages.size,
                            isSelected = selectedTab == "room_chat",
                            badgeColor = NuxColors.ForestGreen,
                            onClick = { selectedTab = "room_chat" },
                            modifier = Modifier.weight(1.1f)
                        )
                        PillTab(
                            title = "TEMAN",
                            count = acceptedFriends.size,
                            isSelected = selectedTab == "friends",
                            onClick = { selectedTab = "friends" },
                            modifier = Modifier.weight(1f)
                        )
                        PillTab(
                            title = "VOICE",
                            count = voiceRooms.size,
                            isSelected = selectedTab == "rooms",
                            onClick = { selectedTab = "rooms" },
                            modifier = Modifier.weight(1f)
                        )
                        PillTab(
                            title = "REQ",
                            count = pendingReceived.size,
                            isSelected = selectedTab == "requests",
                            badgeColor = if (pendingReceived.isNotEmpty()) Color(0xFFEF4444) else null,
                            onClick = { selectedTab = "requests" },
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        PillTab(
                            title = "TEMAN",
                            count = acceptedFriends.size,
                            isSelected = selectedTab == "friends",
                            onClick = { selectedTab = "friends" },
                            modifier = Modifier.weight(1f)
                        )
                        PillTab(
                            title = "VOICE",
                            count = voiceRooms.size,
                            isSelected = selectedTab == "rooms",
                            onClick = { selectedTab = "rooms" },
                            modifier = Modifier.weight(1f)
                        )
                        PillTab(
                            title = "REQUEST",
                            count = pendingReceived.size,
                            isSelected = selectedTab == "requests",
                            badgeColor = if (pendingReceived.isNotEmpty()) Color(0xFFEF4444) else null,
                            onClick = { selectedTab = "requests" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedTab == "room_chat" && activeVoiceRoom != null) {
                    VoiceRoomChatPanel(
                        room = activeVoiceRoom!!,
                        messages = voiceMessages,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    // Compact Search Bar
                    NuxCompactSearchBar(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (selectedTab == "friends" && it.isNotBlank()) {
                                scope.launch { NuxSocialManager.searchUser(it) }
                            }
                        },
                        placeholder = if (selectedTab == "rooms") "Cari nama voice room..." else "Cari teman..."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Content List inside Left Card
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            "friends" -> {
                                val filtered = remember(acceptedFriends, searchQuery) {
                                    if (searchQuery.isBlank()) acceptedFriends
                                    else acceptedFriends.filter { it.username.contains(searchQuery, ignoreCase = true) }
                                }

                                if (filtered.isEmpty()) {
                                    EmptyIndicator(
                                        icon = Icons.Outlined.Group,
                                        title = if (searchQuery.isNotBlank()) "Tidak ada hasil" else "Belum Ada Teman",
                                        subtitle = if (searchQuery.isNotBlank()) "Tidak menemukan teman dengan nama \"$searchQuery\""
                                        else "Gunakan tombol \"+ CARI TEMAN\" di atas untuk menambah teman!"
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(filtered, key = { it.uid }) { friend ->
                                            FriendItem(
                                                friend = friend,
                                                isSelected = activeFriend?.uid == friend.uid,
                                                onClick = { NuxSocialManager.openChat(friend) }
                                            )
                                        }
                                    }
                                }
                            }

                            "rooms" -> {
                                val filtered = remember(voiceRooms, searchQuery) {
                                    if (searchQuery.isBlank()) voiceRooms
                                    else voiceRooms.filter { it.name.contains(searchQuery, ignoreCase = true) }
                                }

                                if (filtered.isEmpty()) {
                                    EmptyIndicator(
                                        icon = Icons.Outlined.RecordVoiceOver,
                                        title = "Belum Ada Room",
                                        subtitle = "Klik \"+ BUAT ROOM\" di kanan atas untuk membuat voice room baru!"
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(filtered, key = { it.id }) { room ->
                                            VoiceRoomItem(
                                                room = room,
                                                isJoined = activeVoiceRoom?.id == room.id,
                                                onJoin = {
                                                    if (room.isLocked) {
                                                        roomToJoinWithPassword = room
                                                    } else {
                                                        scope.launch {
                                                            val res = NuxSocialManager.joinVoiceRoom(room)
                                                            if (res.isFailure) {
                                                                Toast.makeText(context, res.exceptionOrNull()?.message ?: "Gagal gabung", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            "requests" -> {
                                if (pendingReceived.isEmpty() && pendingSent.isEmpty()) {
                                    EmptyIndicator(
                                        icon = Icons.Outlined.MarkEmailRead,
                                        title = "Tidak Ada Permintaan",
                                        subtitle = "Permintaan pertemanan masuk atau menunggu konfirmasi akan muncul di sini."
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (pendingReceived.isNotEmpty()) {
                                            item {
                                                Text(
                                                    text = "PERMINTAAN MASUK (${pendingReceived.size})",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NuxColors.ForestGreen,
                                                    modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)
                                                )
                                            }
                                            items(pendingReceived, key = { it.uid }) { req ->
                                                RequestReceivedItem(
                                                    friend = req,
                                                    onAccept = { scope.launch { NuxSocialManager.acceptFriendRequest(req.uid) } },
                                                    onReject = { scope.launch { NuxSocialManager.removeFriend(req.uid) } }
                                                )
                                            }
                                        }

                                        if (pendingSent.isNotEmpty()) {
                                            item {
                                                Text(
                                                    text = "MENUNGGU KONFIRMASI (${pendingSent.size})",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NuxColors.GrayNeutral,
                                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp)
                                                )
                                            }
                                            items(pendingSent, key = { it.uid }) { req ->
                                                 RequestSentItem(
                                                    friend = req,
                                                    onCancel = { scope.launch { NuxSocialManager.removeFriend(req.uid) } }
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

            // -------------------------------------------------------------
            // B. RIGHT PANEL: INTERACTIVE STAGE (Voice Room / Chat / Empty)
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NuxColors.SurfaceElevated, RoundedCornerShape(8.dp))
                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(8.dp))
            ) {
                when {
                    // 1. Voice Room Active
                    activeVoiceRoom != null -> {
                        VoiceRoomView(
                            room = activeVoiceRoom!!,
                            onLeave = { NuxSocialManager.leaveVoiceRoom() }
                        )
                    }

                    // 2. Direct Chat Active
                    activeFriend != null -> {
                        DirectChatView(
                            friend = activeFriend!!,
                            messages = chatMessages,
                            isFriendTyping = isFriendTyping,
                            replyingTo = replyingToMessage,
                            onCancelReply = { replyingToMessage = null },
                            onReplyMessage = { msg -> replyingToMessage = msg },
                            onCloseChat = { NuxSocialManager.closeChat() },
                            onStartVoiceWithFriend = {
                                scope.launch {
                                    val res = NuxSocialManager.createVoiceRoom(
                                        name = "Room: ${activeFriend!!.username}",
                                        password = "",
                                        maxUsers = 5
                                    )
                                    if (res.isSuccess) {
                                        selectedTab = "rooms"
                                    }
                                }
                            }
                        )
                    }

                    // 3. Empty Social Stage
                    else -> {
                        EmptySocialHubView(
                            friendsCount = acceptedFriends.size,
                            roomsCount = voiceRooms.size,
                            onOpenSearch = { showAddFriendDialog = true },
                            onOpenCreateRoom = { showCreateRoomDialog = true }
                        )
                    }
                }
            }
        }
    }

    // =====================================================================
    // DIALOGS (Landscape-First Compact Design, Never Cut Off!)
    // =====================================================================

    // 1. Dialog Tambah / Cari Teman
    if (showAddFriendDialog) {
        AddFriendDialog(
            searchResults = searchResults,
            isSearching = isSearching,
            friends = friends,
            onSearch = { q -> scope.launch { NuxSocialManager.searchUser(q) } },
            onSendRequest = { targetUid ->
                scope.launch {
                    val res = NuxSocialManager.sendFriendRequest(targetUid)
                    if (res.isSuccess) {
                        Toast.makeText(context, "Permintaan pertemanan terkirim!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Gagal mengirim", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onAcceptRequest = { targetUid ->
                scope.launch {
                    val res = NuxSocialManager.acceptFriendRequest(targetUid)
                    if (res.isSuccess) {
                        Toast.makeText(context, "Permintaan pertemanan diterima!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Gagal menerima", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onBlockUser = { targetUid ->
                scope.launch {
                    val res = NuxSocialManager.blockUser(targetUid)
                    if (res.isSuccess) {
                        Toast.makeText(context, "Pengguna berhasil diblokir", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Gagal memblokir", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onUnblockUser = { targetUid ->
                scope.launch {
                    val res = NuxSocialManager.unblockUser(targetUid)
                    if (res.isSuccess) {
                        Toast.makeText(context, "Blokir dibuka", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Gagal membuka blokir", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onOpenChat = { friend ->
                NuxSocialManager.openChat(friend)
            },
            onDismiss = { showAddFriendDialog = false }
        )
    }

    // 2. Dialog Buat Voice Room Baru (Landscape 2-Column Compact)
    if (showCreateRoomDialog) {
        CreateVoiceRoomDialog(
            onCreate = { name, pass, max ->
                scope.launch {
                    val res = NuxSocialManager.createVoiceRoom(name, pass, max)
                    if (res.isSuccess) {
                        selectedTab = "rooms"
                        showCreateRoomDialog = false
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Gagal membuat room", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showCreateRoomDialog = false }
        )
    }

    // 3. Dialog Input Password Room
    if (roomToJoinWithPassword != null) {
        JoinPasswordDialog(
            room = roomToJoinWithPassword!!,
            onJoin = { pass ->
                scope.launch {
                    val res = NuxSocialManager.joinVoiceRoom(roomToJoinWithPassword!!, pass)
                    if (res.isSuccess) {
                        roomToJoinWithPassword = null
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Password salah", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { roomToJoinWithPassword = null }
        )
    }
}

// =====================================================================
// COMPONENT: PILL TAB
// =====================================================================
@Composable
private fun PillTab(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeColor: Color? = null
) {
    val bg = if (isSelected) NuxColors.ForestGreen else NuxColors.SurfaceInput
    val textColor = if (isSelected) Color.White else NuxColors.GrayNeutral
    val border = if (isSelected) NuxColors.ForestGreen else NuxColors.CardBorder

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(vertical = 5.dp, horizontal = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 9.5.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                color = textColor,
                letterSpacing = 0.4.sp
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(3.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeColor ?: if (isSelected) Color(0xFF064E3B) else NuxColors.SurfaceElevated)
                        .border(0.8.dp, if (isSelected) NuxColors.MintGreen.copy(alpha = 0.4f) else NuxColors.CardBorder, CircleShape)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "$count",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// =====================================================================
// COMPONENT: COMPACT SEARCH BAR
// =====================================================================
@Composable
private fun NuxCompactSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = NuxColors.GrayNeutral,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 11.sp,
                    color = NuxColors.GrayNeutral,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear",
                tint = NuxColors.GrayNeutral,
                modifier = Modifier
                    .size(13.dp)
                    .clickable { onValueChange("") }
            )
        }
    }
}

// =====================================================================
// COMPONENT: FRIEND ITEM IN LIST
// =====================================================================
@Composable
private fun FriendItem(
    friend: NuxFriend,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) NuxColors.ForestGreen.copy(alpha = 0.15f) else NuxColors.SurfaceInput
    val border = if (isSelected) NuxColors.ForestGreen.copy(alpha = 0.5f) else NuxColors.CardBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar + Status Dot
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            NuxNetworkImage(
                model = friend.photoUrl.takeIf { it.isNotBlank() },
                contentDescription = friend.username,
                fallbackInitials = friend.username,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
            )

            val dotColor = when {
                friend.isInGame -> Color(0xFF06B6D4) // Cyan (In Game)
                friend.isOnline -> Color(0xFF10B981) // Green (Online)
                else -> Color(0xFF71717A)            // Gray (Offline)
            }
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(1.2.dp, NuxColors.SurfaceElevated, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Username + Badges + Status text
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = friend.username,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(3.dp))
                NuxUserBadge(
                    isVerified = friend.isVerified,
                    isPremium = friend.isPremium,
                    size = 11.dp
                )
            }

            val statusText = when {
                friend.isInGame -> "Sedang bermain"
                friend.isOnline -> "Online"
                friend.lastOnline != null -> {
                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(friend.lastOnline))
                    "Terlihat $timeStr"
                }
                else -> "Offline"
            }

            Text(
                text = statusText,
                fontSize = 9.5.sp,
                color = if (friend.isOnline) NuxColors.MintGreen else NuxColors.GrayNeutral,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Unread Badge
        if (friend.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "${friend.unreadCount}",
                    color = Color.White,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// =====================================================================
// COMPONENT: VOICE ROOM ITEM IN LIST
// =====================================================================
@Composable
private fun VoiceRoomItem(
    room: NuxVoiceRoom,
    isJoined: Boolean,
    onJoin: () -> Unit
) {
    val bg = if (isJoined) NuxColors.ForestGreen.copy(alpha = 0.15f) else NuxColors.SurfaceInput
    val border = if (isJoined) NuxColors.ForestGreen.copy(alpha = 0.5f) else NuxColors.CardBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isJoined) NuxColors.ForestGreen else NuxColors.ForestGreen.copy(alpha = 0.12f))
                .border(1.dp, if (isJoined) NuxColors.MintGreen.copy(alpha = 0.5f) else NuxColors.CardBorder, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.RecordVoiceOver,
                contentDescription = null,
                tint = if (isJoined) Color.White else NuxColors.MintGreen,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = room.name,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (room.isLocked) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Private",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            Text(
                text = "${room.participantCount}/${room.maxUsers} Peserta",
                fontSize = 9.5.sp,
                color = NuxColors.GrayNeutral
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isJoined) NuxColors.ForestGreen else NuxColors.ForestGreen.copy(alpha = 0.15f))
                .border(1.dp, if (isJoined) NuxColors.ForestGreen else NuxColors.ForestGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .clickable { onJoin() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isJoined) "AKTIF" else "MASUK",
                color = if (isJoined) Color.White else NuxColors.MintGreen,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// =====================================================================
// COMPONENT: REQUEST ITEMS
// =====================================================================
@Composable
private fun RequestReceivedItem(
    friend: NuxFriend,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(NuxColors.SurfaceInput)
            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NuxNetworkImage(
            model = friend.photoUrl.takeIf { it.isNotBlank() },
            contentDescription = friend.username,
            fallbackInitials = friend.username,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .size(30.dp)
                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = friend.username, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = "Ingin berteman", fontSize = 9.5.sp, color = NuxColors.GrayNeutral)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(NuxColors.ForestGreen)
                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                .clickable { onAccept() }
                .padding(4.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = "Terima", tint = Color.White, modifier = Modifier.size(12.dp))
        }
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .clickable { onReject() }
                .padding(4.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Tolak", tint = Color(0xFFFCA5A5), modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun RequestSentItem(
    friend: NuxFriend,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(NuxColors.SurfaceInput)
            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NuxNetworkImage(
            model = friend.photoUrl.takeIf { it.isNotBlank() },
            contentDescription = friend.username,
            fallbackInitials = friend.username,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .size(30.dp)
                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = friend.username, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = "Menunggu konfirmasi...", fontSize = 9.5.sp, color = NuxColors.GrayNeutral)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(NuxColors.SurfaceElevated)
                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                .clickable { onCancel() }
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text("Batal", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = NuxColors.GrayNeutral)
        }
    }
}

// =====================================================================
// STAGE 1: DIRECT CHAT VIEW
// =====================================================================
@Composable
private fun DirectChatView(
    friend: NuxFriend,
    messages: List<NuxChatMessage>,
    isFriendTyping: Boolean,
    replyingTo: NuxChatMessage?,
    onCancelReply: () -> Unit,
    onReplyMessage: (NuxChatMessage) -> Unit,
    onCloseChat: () -> Unit,
    onStartVoiceWithFriend: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var inputMessage by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSending by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NuxColors.SurfaceElevated)
                .border(BorderStroke(1.dp, NuxColors.CardBorder))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(28.dp)) {
                    NuxNetworkImage(
                        model = friend.photoUrl.takeIf { it.isNotBlank() },
                        contentDescription = friend.username,
                        fallbackInitials = friend.username,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = friend.username,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        NuxUserBadge(isVerified = friend.isVerified, isPremium = friend.isPremium, size = 11.dp)
                    }

                    Text(
                        text = if (friend.isOnline) "🟢 Online" else "Offline",
                        color = if (friend.isOnline) NuxColors.MintGreen else NuxColors.GrayNeutral,
                        fontSize = 9.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Voice Call Button
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NuxColors.ForestGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .clickable { onStartVoiceWithFriend() }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = NuxColors.MintGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Voice", color = NuxColors.MintGreen, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Close Button
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                        .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                        .clickable { onCloseChat() }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Messages Feed
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(NuxColors.Background)
        ) {
            if (messages.isEmpty()) {
                EmptyIndicator(
                    icon = Icons.Outlined.Forum,
                    title = "Belum Ada Pesan",
                    subtitle = "Sapa ${friend.username} sekarang!"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val myUid = AccountManager.launcherUser.value?.uid ?: ""
                    items(messages, key = { it.id }) { msg ->
                        ChatBubble(
                            message = msg,
                            isMine = msg.senderId == myUid,
                            onReply = { onReplyMessage(msg) }
                        )
                    }
                }
            }

            // Typing indicator banner
            if (isFriendTyping) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NuxColors.SurfaceElevated)
                        .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "✍️ ${friend.username} sedang mengetik...",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = NuxColors.MintGreen
                    )
                }
            }
        }

        // Replying snippet preview (if any)
        if (replyingTo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NuxColors.SurfaceElevated)
                    .border(BorderStroke(1.dp, NuxColors.CardBorder))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Reply, contentDescription = null, tint = NuxColors.MintGreen, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Membalas: ${replyingTo.text.take(60)}", fontSize = 9.5.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Batal",
                    tint = NuxColors.GrayNeutral,
                    modifier = Modifier
                        .size(13.dp)
                        .clickable { onCancelReply() }
                )
            }
        }

        // Image Attachment Preview
        if (selectedImageUri != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NuxColors.ForestGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("📷 Foto siap dikirim", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NuxColors.MintGreen)
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Hapus",
                    tint = NuxColors.GrayNeutral,
                    modifier = Modifier
                        .size(13.dp)
                        .clickable { selectedImageUri = null }
                )
            }
        }

        // Bottom Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NuxColors.SurfaceElevated)
                .border(BorderStroke(1.dp, NuxColors.CardBorder))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Attach image button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, contentDescription = "Foto", tint = NuxColors.GrayNeutral, modifier = Modifier.size(15.dp))
            }

            // Message text input box
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (inputMessage.isEmpty()) {
                        Text("Ketik pesan...", fontSize = 11.sp, color = NuxColors.GrayNeutral)
                    }
                    BasicTextField(
                        value = inputMessage,
                        onValueChange = {
                            inputMessage = it
                            NuxSocialManager.setMyTyping(it.isNotBlank())
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Send Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (inputMessage.isNotBlank() || selectedImageUri != null) NuxColors.ForestGreen else NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                    .border(1.dp, if (inputMessage.isNotBlank() || selectedImageUri != null) NuxColors.ForestGreen else NuxColors.CardBorder, RoundedCornerShape(6.dp))
                    .clickable(enabled = !isSending && (inputMessage.isNotBlank() || selectedImageUri != null)) {
                        val textToSend = inputMessage.trim()
                        val uriToSend = selectedImageUri
                        val replyObj = replyingTo?.let {
                            NuxChatReply(
                                id = it.id,
                                senderId = it.senderId,
                                senderName = it.senderName,
                                text = it.text
                            )
                        }

                        isSending = true
                        scope.launch {
                            var imgBytes: ByteArray? = null
                            if (uriToSend != null) {
                                try {
                                    imgBytes = context.contentResolver.openInputStream(uriToSend)?.use { it.readBytes() }
                                } catch (_: Exception) {}
                            }

                            val res = NuxSocialManager.sendChatMessage(
                                text = textToSend,
                                imageBytes = imgBytes,
                                replyTo = replyObj
                            )

                            isSending = false
                            if (res.isSuccess) {
                                inputMessage = ""
                                selectedImageUri = null
                                onCancelReply()
                            } else {
                                Toast.makeText(context, res.exceptionOrNull()?.message ?: "Gagal kirim pesan", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 1.8.dp)
                } else {
                    Icon(Icons.Default.Send, contentDescription = "Kirim", tint = if (inputMessage.isNotBlank() || selectedImageUri != null) Color.White else NuxColors.GrayNeutral, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// =====================================================================
// COMPONENT: CHAT BUBBLE
// =====================================================================
@Composable
private fun ChatBubble(
    message: NuxChatMessage,
    isMine: Boolean,
    onReply: () -> Unit
) {
    val align = if (isMine) Alignment.End else Alignment.Start
    val bg = if (isMine) NuxColors.ForestGreen.copy(alpha = 0.22f) else NuxColors.SurfaceElevated
    val border = if (isMine) NuxColors.ForestGreen.copy(alpha = 0.45f) else NuxColors.CardBorder
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = align
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 60.dp, max = 250.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 8.dp,
                        topEnd = 8.dp,
                        bottomStart = if (isMine) 8.dp else 2.dp,
                        bottomEnd = if (isMine) 2.dp else 8.dp
                    )
                )
                .background(bg)
                .border(
                    width = 1.dp,
                    color = border,
                    shape = RoundedCornerShape(
                        topStart = 8.dp,
                        topEnd = 8.dp,
                        bottomStart = if (isMine) 8.dp else 2.dp,
                        bottomEnd = if (isMine) 2.dp else 8.dp
                    )
                )
                .clickable { onReply() }
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Column {
                // Quoted reply
                if (message.replyTo != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(NuxColors.SurfaceInput)
                            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Column {
                            Text(
                                text = message.replyTo.senderName.ifBlank { "Pengguna" },
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = NuxColors.MintGreen
                            )
                            Text(
                                text = message.replyTo.text,
                                fontSize = 8.5.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Image attachment
                if (!message.imageUrl.isNullOrEmpty()) {
                    NuxNetworkImage(
                        model = message.imageUrl,
                        contentDescription = "Foto",
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .border(1.dp, border, RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Text & Timestamp side by side
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (message.text.isNotEmpty()) {
                        Text(
                            text = message.text,
                            fontSize = 11.5.sp,
                            color = Color.White,
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Text(
                        text = timeStr,
                        fontSize = 7.5.sp,
                        color = NuxColors.GrayNeutral,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }
        }
    }
}

// =====================================================================
// =====================================================================
// COMPONENT: VOICE ROOM CHAT PANEL (Placed in Left Column, Windows Style!)
// =====================================================================
@Composable
private fun VoiceRoomChatPanel(
    room: NuxVoiceRoom,
    messages: List<NuxVoiceMessage>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var textInput by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Room Chat Info Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NuxColors.SurfaceElevated, RoundedCornerShape(6.dp))
                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(NuxColors.MintGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CHAT: ${room.name}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NuxColors.ForestGreen.copy(alpha = 0.15f))
                    .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text("LIVE", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = NuxColors.MintGreen)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Message Feed
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(NuxColors.Background, RoundedCornerShape(6.dp))
                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                .padding(6.dp)
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Belum ada obrolan di room.\nKetik pesan di bawah!",
                        fontSize = 10.sp,
                        color = NuxColors.GrayNeutral,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val myUid = AccountManager.launcherUser.value?.uid ?: ""
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isMine = msg.senderId == myUid
                        val senderDisplayName = if (msg.senderName.length > 8) "${msg.senderName.take(8)}..." else msg.senderName
                        val timeStr = if (msg.timestamp > 0) {
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))
                        } else ""

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                        ) {
                            if (!isMine) {
                                Text(
                                    text = senderDisplayName,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NuxColors.MintGreen,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 1.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 6.dp,
                                            topEnd = 6.dp,
                                            bottomStart = if (isMine) 6.dp else 2.dp,
                                            bottomEnd = if (isMine) 2.dp else 6.dp
                                        )
                                    )
                                    .background(if (isMine) NuxColors.ForestGreen.copy(alpha = 0.22f) else NuxColors.SurfaceElevated)
                                    .border(1.dp, if (isMine) NuxColors.ForestGreen.copy(alpha = 0.45f) else NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    lineHeight = 13.sp
                                )
                            }
                            if (timeStr.isNotEmpty()) {
                                Text(
                                    text = timeStr,
                                    fontSize = 7.5.sp,
                                    color = NuxColors.GrayNeutral,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Input Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (textInput.isEmpty()) {
                    Text("Kirim pesan di room...", fontSize = 10.sp, color = NuxColors.GrayNeutral)
                }
                BasicTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Kirim",
                tint = if (textInput.isNotBlank()) NuxColors.ForestGreen else NuxColors.GrayNeutral,
                modifier = Modifier
                    .size(15.dp)
                    .clickable {
                        if (textInput.isNotBlank()) {
                            val txt = textInput.trim()
                            textInput = ""
                            scope.launch { NuxSocialManager.sendVoiceRoomMessage(txt) }
                        }
                    }
            )
        }
    }
}

// =====================================================================
// STAGE 2: VOICE ROOM VIEW (Dedicated Clean Stage)
// =====================================================================
@Composable
private fun VoiceRoomView(
    room: NuxVoiceRoom,
    onLeave: () -> Unit
) {
    val isMuted by NuxVoiceManager.isMuted.collectAsState()
    val activeSpeakers by NuxVoiceManager.activeSpeakers.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Voice Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NuxColors.SurfaceElevated)
                .border(BorderStroke(1.dp, NuxColors.CardBorder))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NuxColors.ForestGreen.copy(alpha = 0.15f))
                        .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = NuxColors.MintGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = room.name,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NuxColors.ForestGreen.copy(alpha = 0.15f))
                                .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("LIVEKIT SFU", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = NuxColors.MintGreen)
                        }
                    }
                    Text(
                        text = "${room.participantCount}/${room.maxUsers} Peserta Aktif",
                        color = NuxColors.GrayNeutral,
                        fontSize = 9.sp
                    )
                }
            }

            // Tombol Keluar
            Box(
                modifier = Modifier
                    .height(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                    .clickable { onLeave() }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("KELUAR", color = Color(0xFFFCA5A5), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Voice Arena (Dedicated Participant Stage, Minimalist & Spacious!)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            val participants = room.participants.values.toList()
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 78.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(participants, key = { it.uid }) { p ->
                    val isSpeaking = activeSpeakers.contains(p.uid)
                    ParticipantBadge(
                        participant = p,
                        isSpeaking = isSpeaking
                    )
                }
            }
        }

        // Bottom Voice Control Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NuxColors.SurfaceElevated)
                .border(BorderStroke(1.dp, NuxColors.CardBorder))
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isMuted) Color(0xFFEF4444).copy(alpha = 0.2f) else NuxColors.ForestGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .border(1.dp, if (isMuted) Color(0xFFEF4444).copy(alpha = 0.6f) else NuxColors.ForestGreen.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .clickable { NuxVoiceManager.toggleMute() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isMuted) Color(0xFFFCA5A5) else NuxColors.MintGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMuted) "MIC BISU (MUTED)" else "MIC AKTIF (BERBICARA)",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }
    }
}

// =====================================================================
// COMPONENT: PARTICIPANT BADGE IN VOICE ROOM (Minimalist, Max 4 Chars)
// =====================================================================
@Composable
private fun ParticipantBadge(
    participant: NuxParticipant,
    isSpeaking: Boolean
) {
    val displayName = if (participant.username.length > 4) {
        "${participant.username.take(4)}..."
    } else {
        participant.username
    }

    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSpeaking) NuxColors.ForestGreen.copy(alpha = 0.2f) else NuxColors.SurfaceElevated)
            .border(
                width = 1.dp,
                color = if (isSpeaking) NuxColors.MintGreen else NuxColors.CardBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar with ring if speaking
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .border(1.5.dp, if (isSpeaking) NuxColors.MintGreen else NuxColors.CardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            NuxNetworkImage(
                model = participant.photoURL.takeIf { it.isNotBlank() },
                contentDescription = participant.username,
                fallbackInitials = participant.username,
                shape = CircleShape,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Truncated Username
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = displayName,
                color = Color.White,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(2.dp))
            NuxUserBadge(isVerified = participant.isVerified, isPremium = participant.isPremium, size = 9.dp)
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Status Label: Bicara or Platform (Setara seperti versi Windows)
        if (isSpeaking) {
            Text(
                text = "Bicara",
                color = NuxColors.MintGreen,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = participant.platform.take(3).uppercase(),
                color = NuxColors.GrayNeutral,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// =====================================================================
// =====================================================================
// STAGE 3: EMPTY SOCIAL HUB VIEW (Clean & Creative)
// =====================================================================
@Composable
private fun EmptySocialHubView(
    friendsCount: Int,
    roomsCount: Int,
    onOpenSearch: () -> Unit,
    onOpenCreateRoom: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon Badge Box
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NuxColors.ForestGreen.copy(alpha = 0.15f))
                .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = NuxColors.MintGreen,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "PUSAT KOMUNIKASI & VOICE",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Terhubung dengan teman lintas platform PC Windows & Android.\nObrolan langsung dan suara jernih berkecepatan tinggi via LiveKit WebRTC.",
            fontSize = 10.5.sp,
            color = NuxColors.GrayNeutral,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Stats Row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(NuxColors.SurfaceInput)
                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("👥 $friendsCount Teman Terdaftar", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(NuxColors.SurfaceInput)
                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("🎙️ $roomsCount Voice Room Aktif", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NuxColors.MintGreen)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons Row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(NuxColors.SurfaceInput)
                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                    .clickable { onOpenSearch() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = NuxColors.ForestGreen, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("CARI TEMAN BARU", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                }
            }

            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(NuxColors.ForestGreen.copy(alpha = 0.15f))
                    .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .clickable { onOpenCreateRoom() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = NuxColors.MintGreen, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("BUAT VOICE ROOM", color = NuxColors.MintGreen, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                }
            }
        }
    }
}

// =====================================================================
// COMPONENT: EMPTY STATE HELPER
// =====================================================================
@Composable
private fun EmptyIndicator(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = NuxColors.ForestGreen.copy(alpha = 0.6f), modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.height(5.dp))
        Text(text = title, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = subtitle, fontSize = 9.5.sp, color = NuxColors.GrayNeutral, textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}

// =====================================================================
// DIALOG 1: CARI & TAMBAH TEMAN (Landscape-First Compact)
// =====================================================================
@Composable
private fun AddFriendDialog(
    searchResults: List<NuxUserProfile>,
    isSearching: Boolean,
    friends: List<NuxFriend>,
    onSearch: (String) -> Unit,
    onSendRequest: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onBlockUser: (String) -> Unit,
    onUnblockUser: (String) -> Unit,
    onOpenChat: (NuxFriend) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var sentRequestUids by remember { mutableStateOf(setOf<String>()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .width(440.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(NuxColors.SurfaceElevated, RoundedCornerShape(8.dp))
                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "CARI & TAMBAH TEMAN", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NuxColors.SurfaceInput)
                        .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            NuxCompactSearchBar(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it)
                },
                placeholder = "Ketik username teman untuk mencari..."
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 160.dp)
            ) {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = NuxColors.ForestGreen, strokeWidth = 2.dp)
                } else if (searchResults.isEmpty()) {
                    Text(
                        text = if (query.isBlank()) "Ketik nama teman di atas." else "Tidak ada pengguna ditemukan.",
                        fontSize = 10.5.sp,
                        color = NuxColors.GrayNeutral,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    val currentMyUid = AccountManager.launcherUser.value?.uid ?: ""
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        items(searchResults, key = { it.uid }) { user ->
                            val isMe = user.uid == currentMyUid
                            val friendEntry = friends.find { it.uid == user.uid }
                            val isPendingSent = (friendEntry?.isPendingSent == true || sentRequestUids.contains(user.uid)) && friendEntry?.isAccepted != true
                            val isAccepted = friendEntry?.isAccepted == true
                            val isPendingReceived = friendEntry?.isPendingReceived == true
                            val isBlocked = friendEntry?.isBlocked == true

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NuxColors.SurfaceInput)
                                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NuxNetworkImage(
                                    model = user.photoURL.takeIf { it.isNotBlank() },
                                    contentDescription = user.username,
                                    fallbackInitials = user.username,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(user.username, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        NuxUserBadge(isVerified = user.isVerified, isPremium = user.isPremium, size = 10.dp)
                                    }
                                    Text(user.platform.uppercase(), fontSize = 8.5.sp, color = NuxColors.GrayNeutral)
                                }

                                // Action Area
                                if (isMe) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(NuxColors.SurfaceElevated)
                                            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("Kamu", color = NuxColors.GrayNeutral, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (isAccepted) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(NuxColors.ForestGreen.copy(alpha = 0.15f))
                                                .border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    if (friendEntry != null) {
                                                        onOpenChat(friendEntry)
                                                        onDismiss()
                                                    }
                                                }
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Text("✓ Teman", color = NuxColors.MintGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFEF4444).copy(alpha = 0.18f))
                                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                                .clickable { onBlockUser(user.uid) }
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Text("Block", color = Color(0xFFFCA5A5), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else if (isPendingSent) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(NuxColors.SurfaceElevated)
                                            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Schedule, contentDescription = null, tint = NuxColors.GrayNeutral, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Menunggu", color = NuxColors.GrayNeutral, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else if (isPendingReceived) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(NuxColors.ForestGreen)
                                            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                            .clickable { onAcceptRequest(user.uid) }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("Terima", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (isBlocked) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFEF4444).copy(alpha = 0.18f))
                                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                            .clickable { onUnblockUser(user.uid) }
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Text("Buka Block", color = Color(0xFFFCA5A5), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(NuxColors.ForestGreen)
                                            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                            .clickable {
                                                sentRequestUids = sentRequestUids + user.uid
                                                onSendRequest(user.uid)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("+ Tambah", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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

// =====================================================================
// DIALOG 2: BUAT VOICE ROOM (2-Kolom Landscape, Tombol SELALU Terlihat!)
// =====================================================================
@Composable
private fun CreateVoiceRoomDialog(
    onCreate: (name: String, pass: String, max: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var roomName by remember { mutableStateOf("") }
    var roomPassword by remember { mutableStateOf("") }
    var maxUsers by remember { mutableIntStateOf(5) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .width(450.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(NuxColors.SurfaceElevated, RoundedCornerShape(8.dp))
                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "BUAT VOICE ROOM BARU", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NuxColors.SurfaceInput)
                        .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2-Kolom Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Kolom Kiri: Input Nama & Password
                Column(modifier = Modifier.weight(1.1f)) {
                    Text("NAMA ROOM:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NuxColors.GrayNeutral)
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (roomName.isEmpty()) {
                                Text("Contoh: Mabar Survival", fontSize = 10.5.sp, color = NuxColors.GrayNeutral)
                            }
                            BasicTextField(
                                value = roomName,
                                onValueChange = { roomName = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("PASSWORD (OPSIONAL):", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NuxColors.GrayNeutral)
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (roomPassword.isEmpty()) {
                                Text("Kosongkan jika publik", fontSize = 10.5.sp, color = NuxColors.GrayNeutral)
                            }
                            BasicTextField(
                                value = roomPassword,
                                onValueChange = { roomPassword = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Kolom Kanan: Kapasitas Slider & Tombol Aksi
                Column(
                    modifier = Modifier.weight(0.9f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("KAPASITAS PESERTA:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NuxColors.GrayNeutral)
                            Text("$maxUsers Pemain", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = NuxColors.MintGreen)
                        }
                        Slider(
                            value = maxUsers.toFloat(),
                            onValueChange = { maxUsers = it.toInt() },
                            valueRange = 2f..10f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                thumbColor = NuxColors.ForestGreen,
                                activeTrackColor = NuxColors.ForestGreen,
                                inactiveTrackColor = NuxColors.SurfaceInput
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tombol Aksi: Batal & Buat Sekarang
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("BATAL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(NuxColors.ForestGreen, RoundedCornerShape(6.dp))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    if (roomName.isNotBlank()) {
                                        onCreate(roomName.trim(), roomPassword.trim(), maxUsers)
                                    } else {
                                        onCreate("Voice Room", roomPassword.trim(), maxUsers)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("BUAT ROOM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// DIALOG 3: INPUT PASSWORD ROOM
// =====================================================================
@Composable
private fun JoinPasswordDialog(
    room: NuxVoiceRoom,
    onJoin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var passInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(NuxColors.SurfaceElevated, RoundedCornerShape(8.dp))
                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Text(text = "ROOM DILINDUNGI PASSWORD", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Masukkan kata sandi untuk masuk ke room \"${room.name}\".", fontSize = 9.5.sp, color = NuxColors.GrayNeutral)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (passInput.isEmpty()) {
                        Text("Password...", fontSize = 10.5.sp, color = NuxColors.GrayNeutral)
                    }
                    BasicTextField(
                        value = passInput,
                        onValueChange = { passInput = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NuxColors.SurfaceInput, RoundedCornerShape(6.dp))
                        .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("BATAL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NuxColors.ForestGreen, RoundedCornerShape(6.dp))
                        .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                        .clickable { onJoin(passInput) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("GABUNG", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                }
            }
        }
    }
}
