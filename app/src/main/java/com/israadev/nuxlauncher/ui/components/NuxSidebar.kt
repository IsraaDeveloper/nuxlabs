package com.israadev.nuxlauncher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.R
import com.israadev.nuxlauncher.core.auth.AuthUser
import com.israadev.nuxlauncher.core.models.UserAccount
import com.israadev.nuxlauncher.core.social.NuxSocialManager
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.resp

data class NuxNavItem(
    val id: String,
    val icon: ImageVector,
    val label: String
)

val NUX_NAV_ITEMS = listOf(
    NuxNavItem("home", Icons.Outlined.Home, "Home"),
    NuxNavItem("accounts", Icons.Outlined.Person, "Akun"),
    NuxNavItem("mods", Icons.Outlined.Extension, "Mod"),
    NuxNavItem("ai", Icons.Outlined.Psychology, "AI"),
    NuxNavItem("settings", Icons.Outlined.Settings, "Pengaturan")
)

/**
 * NUX Cyber-Emerald Sidebar matching reference screenshot
 */
@Composable
fun NuxSidebar(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    onOpenAbout: () -> Unit = {},
    currentAccount: UserAccount? = null,
    launcherUser: AuthUser? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = (6.dp).resp()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP BRAND LOGO (Official NUX Icon)
        val logoShape = RoundedCornerShape((10.dp).resp())
        Box(
            modifier = Modifier
                .size((34.dp).resp())
                .clip(logoShape)
                .background(Color(0xFF09090B))
                .border(1.dp, Color(0x33000000), logoShape)
                .clickable { onTabSelected("home") },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.nux_icon),
                contentDescription = "NUX Logo",
                modifier = Modifier.size((22.dp).resp())
            )
        }

        Spacer(modifier = Modifier.height((6.dp).resp()))

        // CENTER: NAV ITEMS (Icon + Label)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy((6.dp).resp()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NUX_NAV_ITEMS.forEach { item ->
                val isSelected = activeTab == item.id
                val shape = RoundedCornerShape((12.dp).resp())

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape((8.dp).resp()))
                        .clickable { onTabSelected(item.id) }
                        .padding(horizontal = (2.dp).resp(), vertical = (2.dp).resp()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = (40.dp).resp(), height = (25.dp).resp())
                            .background(
                                color = if (isSelected) Color(0xFF09090B) else Color.Transparent,
                                shape = shape
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0x33000000) else Color.Transparent,
                                shape = shape
                            )
                            .clip(shape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) NuxColors.ForestGreen else Color(0xFF09090B).copy(alpha = 0.85f),
                            modifier = Modifier.size((16.dp).resp())
                        )
                    }

                    Spacer(modifier = Modifier.height((2.dp).resp()))

                    Text(
                        text = item.label,
                        color = Color(0xFF09090B),
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        fontSize = (8.5.sp).resp(),
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height((4.dp).resp()))

        // BOTTOM: INFO (i) ICON
        Box(
            modifier = Modifier
                .size((26.dp).resp())
                .clip(CircleShape)
                .background(Color(0xFF09090B).copy(alpha = 0.14f))
                .border(1.dp, Color(0xFF09090B).copy(alpha = 0.35f), CircleShape)
                .clickable { onOpenAbout() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Tentang & Lisensi",
                tint = Color(0xFF09090B),
                modifier = Modifier.size((14.dp).resp())
            )
        }
    }
}
