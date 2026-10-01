package com.israadev.nuxlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.israadev.nuxlauncher.R
import com.israadev.nuxlauncher.ui.theme.NuxColors

/**
 * Komponen Avatar Pengguna NUX Lintas Platform
 * - User Android melihat user Windows: Menampilkan icon PC Desktop di pojok kanan bawah gambar profile
 */
@Composable
fun NuxUserAvatar(
    photoUrl: String?,
    username: String,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 32.dp,
    shape: Shape = RoundedCornerShape(6.dp),
    platform: String = "android",
    isAndroid: Boolean = true,
    statusDotColor: Color? = null
) {
    val isWindowsUser = platform.equals("windows", ignoreCase = true) ||
            platform.equals("pc", ignoreCase = true) ||
            !isAndroid

    Box(
        modifier = modifier.size(avatarSize),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Gambar Profile Utama
        NuxNetworkImage(
            model = photoUrl?.takeIf { it.isNotBlank() },
            contentDescription = username,
            fallbackInitials = username,
            shape = shape,
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, NuxColors.CardBorder, shape)
        )

        // Status Dot (Online / In-game / Offline)
        if (statusDotColor != null) {
            val dotAlignment = if (isWindowsUser) Alignment.BottomStart else Alignment.BottomEnd
            val dotOffset = if (isWindowsUser) {
                Modifier.align(dotAlignment).offset(x = (-1.5).dp, y = 1.5.dp)
            } else {
                Modifier.align(dotAlignment).offset(x = 1.5.dp, y = 1.5.dp)
            }
            Box(
                modifier = dotOffset
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(statusDotColor)
                    .border(1.5.dp, Color(0xFF09090B), CircleShape)
            )
        }

        // Platform Windows PC Icon di Pojok Kanan Bawah
        if (isWindowsUser) {
            val badgeSize = (avatarSize.value * 0.42f).coerceIn(12f, 18f).dp
            val iconSize = (badgeSize.value * 0.72f).dp

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(Color(0xFF141722), CircleShape)
                    .border(1.dp, Color(0x66FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.nux_platform_windows),
                    contentDescription = "User Windows PC",
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}
