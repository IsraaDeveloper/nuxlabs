package com.israadev.nuxlauncher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.israadev.nuxlauncher.R

/**
 * Komponen Badge Centang Pengguna Lintas Platform
 * - Menampilkan Centang Biru (Verified) bagi user terverifikasi
 * - Menampilkan Centang Emas/Oranye (Premium) bagi user Windows Premium
 * Sesuai ketentuan: Pada Android tidak perlu menambahkan ikon Windows untuk user PC.
 */
@Composable
fun NuxUserBadge(
    isVerified: Boolean,
    isPremium: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp
) {
    if (!isVerified && !isPremium) return

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cukup satu centang: jika ada verified dan premium, munculkan verified saja
        if (isVerified) {
            Image(
                painter = painterResource(id = R.drawable.nux_badge_verified),
                contentDescription = "Verified User",
                modifier = Modifier.size(size)
            )
        } else if (isPremium) {
            Image(
                painter = painterResource(id = R.drawable.nux_badge_premium),
                contentDescription = "Premium Windows",
                modifier = Modifier.size(size)
            )
        }
    }
}
