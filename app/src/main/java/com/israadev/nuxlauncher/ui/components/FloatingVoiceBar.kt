package com.israadev.nuxlauncher.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.core.social.NuxSocialManager
import com.israadev.nuxlauncher.core.social.NuxVoiceManager
import com.israadev.nuxlauncher.ui.theme.NuxColors
import kotlin.math.roundToInt

/**
 * Floating Dark Obsidian Cyber-Glass Voice Bar for Android Landscape
 * Mengambang di atas layar ketika user terhubung dalam Voice Room.
 * Mendukung drag bebas, toggle mute, buka room, dan disconnect.
 */
@Composable
fun FloatingVoiceBar(
    onOpenVoiceRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected by NuxVoiceManager.isConnected.collectAsState()
    val isConnecting by NuxVoiceManager.isConnecting.collectAsState()
    val isMuted by NuxVoiceManager.isMuted.collectAsState()
    val activeRoom by NuxSocialManager.activeVoiceRoom.collectAsState()
    val activeSpeakers by NuxVoiceManager.activeSpeakers.collectAsState()

    if (!isConnected && !isConnecting && activeRoom == null) return

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Pulsing green wave animation when speaking
    val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val roomName = activeRoom?.name?.ifBlank { "Voice Room" } ?: "Voice Room"
    val participantCount = activeRoom?.participantCount ?: 1
    val isSomeoneSpeaking = activeSpeakers.isNotEmpty()
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        // Main Bar Content
        Row(
            modifier = Modifier
                .clip(shape)
                .background(NuxColors.SurfaceElevated, shape)
                .border(1.dp, NuxColors.CardBorder, shape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Audio Pulsing Dot
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(24.dp)
            ) {
                if (isSomeoneSpeaking) {
                    Box(
                        modifier = Modifier
                            .size((16 * pulseScale).dp)
                            .clip(CircleShape)
                            .background(NuxColors.ForestGreen.copy(alpha = 0.35f))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) Color(0xFFF43F5E) else NuxColors.ForestGreen)
                )
            }

            // 2. Room Title & Info
            Column(
                modifier = Modifier
                    .widthIn(max = 180.dp)
                    .clickable { onOpenVoiceRoom() }
            ) {
                Text(
                    text = roomName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = NuxColors.ForestGreen,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isConnecting) "Menyambungkan..." else "$participantCount online",
                        color = Color(0xFFA1A1AA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 3. Mute / Unmute Button
            val btnShape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(btnShape)
                    .background(if (isMuted) Color(0xFFF43F5E) else NuxColors.ForestGreen, btnShape)
                    .border(1.dp, Color(0x33FFFFFF), btnShape)
                    .clickable { NuxVoiceManager.toggleMute() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = if (isMuted) Color.White else Color(0xFF09090B),
                    modifier = Modifier.size(16.dp)
                )
            }

            // 4. Expand / Open Room Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(btnShape)
                    .background(Color(0xFF222733), btnShape)
                    .border(1.dp, Color(0x26FFFFFF), btnShape)
                    .clickable { onOpenVoiceRoom() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = "Buka Room",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }

            // 5. Leave Voice Call Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(btnShape)
                    .background(Color(0xFF3F1923), btnShape)
                    .border(1.dp, Color(0xFFF43F5E).copy(alpha = 0.4f), btnShape)
                    .clickable { NuxSocialManager.leaveVoiceRoom() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Keluar Room",
                    tint = Color(0xFFF43F5E),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
