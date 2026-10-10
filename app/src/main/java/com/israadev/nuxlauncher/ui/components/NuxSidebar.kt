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
 * NUX Black Sidebar with right-side radius and flush left side, matching reference screenshot
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
    val sidebarShape = RoundedCornerShape(
        topStart = 0.dp,
        bottomStart = 0.dp,
        topEnd = (24.dp).resp(),
        bottomEnd = (24.dp).resp()
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(sidebarShape)
            .background(Color(0xFF101216), sidebarShape)
            .padding(top = (10.dp).resp(), bottom = (10.dp).resp(), start = (6.dp).resp(), end = (6.dp).resp()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP BRAND LOGO (Official NUX Icon)
        val logoShape = RoundedCornerShape((10.dp).resp())
        Box(
            modifier = Modifier
                .size((36.dp).resp())
                .clip(logoShape)
                .background(Color(0xFF181A20))
                .border(1.dp, Color(0x26FFFFFF), logoShape)
                .clickable { onTabSelected("home") },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.nux_icon),
                contentDescription = "NUX Logo",
                modifier = Modifier.size((24.dp).resp())
            )
        }

        Spacer(modifier = Modifier.height((6.dp).resp()))

        // CENTER: NAV ITEMS (Icon + Label)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy((8.dp).resp()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NUX_NAV_ITEMS.forEach { item ->
                val isSelected = activeTab == item.id
                val pillShape = RoundedCornerShape((14.dp).resp())

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape((10.dp).resp()))
                        .clickable { onTabSelected(item.id) }
                        .padding(horizontal = (2.dp).resp(), vertical = (2.dp).resp()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = (44.dp).resp(), height = (26.dp).resp())
                            .background(
                                color = if (isSelected) NuxColors.ForestGreen else Color.Transparent,
                                shape = pillShape
                            )
                            .clip(pillShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) Color(0xFF09090B) else Color(0xFFA1A1AA),
                            modifier = Modifier.size((17.dp).resp())
                        )
                    }

                    Spacer(modifier = Modifier.height((3.dp).resp()))

                    Text(
                        text = item.label,
                        color = if (isSelected) NuxColors.ForestGreen else Color(0xFFA1A1AA),
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                        fontSize = (8.5.sp).resp(),
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height((6.dp).resp()))

        // BOTTOM: INFO (i) ICON
        Box(
            modifier = Modifier
                .size((28.dp).resp())
                .clip(CircleShape)
                .background(Color(0xFF181A20))
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .clickable { onOpenAbout() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Tentang & Lisensi",
                tint = Color(0xFFA1A1AA),
                modifier = Modifier.size((15.dp).resp())
            )
        }
    }
}
