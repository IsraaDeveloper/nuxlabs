package com.israadev.nuxlauncher.core.social

import com.google.gson.annotations.SerializedName

/**
 * Model Profil Publik Pengguna (Lintas Platform Android & Windows)
 * Sinkron dengan node shared_social/public_profiles/{uid}
 */
data class NuxUserProfile(
    val uid: String = "",
    val username: String = "",
    @SerializedName("photoURL") val photoURL: String = "",
    val email: String = "",
    val status: String = "offline", // "online", "in_game", "offline"
    val lastOnline: Long? = null,
    val platform: String = "android",
    val isAndroid: Boolean = true,
    val isVerified: Boolean = false,
    val isPremium: Boolean = false
)

/**
 * Model Teman & Status Relasi Pertemanan (shared_social/friends/{uid})
 */
data class NuxFriend(
    val uid: String,
    val username: String = "",
    val photoUrl: String = "",
    val status: String = "", // "accepted", "pending_sent", "pending_received"
    val lastMessageTime: Long? = null,
    val lastMessageText: String? = null,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isInGame: Boolean = false,
    val lastOnline: Long? = null,
    val presenceStatus: String = "offline",
    val platform: String = "windows",
    val isAndroid: Boolean = false,
    val isTyping: Boolean = false,
    val isVerified: Boolean = false,
    val isPremium: Boolean = false
) {
    val isAccepted: Boolean get() = status == "accepted"
    val isPendingSent: Boolean get() = status == "pending_sent"
    val isPendingReceived: Boolean get() = status == "pending_received"
    val isBlocked: Boolean get() = status == "blocked"
}

/**
 * Model Kutipan Balasan Pesan (Reply)
 */
data class NuxChatReply(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val isVerified: Boolean = false
)

/**
 * Model Pesan Direct Chat 1-on-1 (shared_social/chats/{chatId})
 */
data class NuxChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhotoURL: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val imageUrl: String? = null,
    val replyTo: NuxChatReply? = null,
    val isVerified: Boolean = false,
    val isPremium: Boolean = false
)

/**
 * Model Peserta Voice Room (shared_social/voice_rooms/{roomId}/participants)
 */
data class NuxParticipant(
    val uid: String = "",
    val username: String = "",
    @SerializedName("photoURL") val photoURL: String = "",
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
    val isVerified: Boolean = false,
    val isPremium: Boolean = false,
    val platform: String = "android",
    val isAndroid: Boolean = true
)

/**
 * Model Voice Room LiveKit (shared_social/voice_rooms/{roomId})
 */
data class NuxVoiceRoom(
    val id: String = "",
    val name: String = "",
    val hostUid: String = "",
    val hostName: String = "",
    val password: String = "",
    val maxUsers: Int = 5,
    val createdAt: Long = 0L,
    val participants: Map<String, NuxParticipant> = emptyMap()
) {
    val isLocked: Boolean get() = password.isNotEmpty()
    val participantCount: Int get() = participants.size
    val isFull: Boolean get() = participantCount >= maxUsers
}

/**
 * Model Pesan Chat di Dalam Voice Room
 */
data class NuxVoiceMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    @SerializedName("senderPhotoURL") val senderPhotoURL: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val isVerified: Boolean = false,
    val isPremium: Boolean = false
)
