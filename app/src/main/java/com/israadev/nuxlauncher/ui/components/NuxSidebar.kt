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
    val contentDescription: String
)

val NUX_NAV_ITEMS = listOf(
    NuxNavItem("home", Icons.Outlined.Home, "Home"),
    NuxNavItem("accounts", Icons.Outlined.Person, "Accounts"),
    NuxNavItem("mods", Icons.Outlined.Extension, "Mods"),
    NuxNavItem("ai", Icons.Outlined.Psychology, "NUX AI"),
    NuxNavItem("friends", Icons.Outlined.Group, "Friends"),
    NuxNavItem("settings", Icons.Outlined.Settings, "Settings")
)

/**
 * Compact Dark Obsidian Cyber-Glass Sidebar
 */
@Composable
fun NuxSidebar(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    currentAccount: UserAccount? = null,
    launcherUser: AuthUser? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .width((52.dp).resp())
                .fillMaxHeight()
                .background(NuxColors.Background)
                .padding(top = (6.dp).resp(), bottom = (8.dp).resp(), start = (3.dp).resp(), end = (3.dp).resp()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BRAND LOGO (Official NUX Icon)
            val logoShape = RoundedCornerShape((8.dp).resp())
            Box(
                modifier = Modifier
                    .size((32.dp).resp())
                    .clip(logoShape)
                    .background(NuxColors.SurfaceElevated)
                    .border(1.dp, NuxColors.CardBorder, logoShape)
                    .clickable { onTabSelected("home") },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.nux_icon),
                    contentDescription = "NUX Logo",
                    modifier = Modifier.size((20.dp).resp())
                )
            }

            Spacer(modifier = Modifier.height((5.dp).resp()))

            // CENTER: 9 VECTOR ICON BUTTONS
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy((4.dp).resp()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NUX_NAV_ITEMS.forEach { item ->
                    val isSelected = activeTab == item.id
                    val shape = RoundedCornerShape((9.dp).resp())

                    Box(
                        modifier = Modifier
                            .size((34.dp).resp())
                            .background(
                                color = if (isSelected) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Transparent,
                                shape = shape
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF10B981).copy(alpha = 0.55f) else Color.Transparent,
                                shape = shape
                            )
                            .clip(shape)
                            .clickable { onTabSelected(item.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.contentDescription,
                            tint = if (isSelected) NuxColors.ForestGreen else Color(0xFF71717A),
                            modifier = Modifier.size((17.dp).resp())
                        )

                        // Notification badge dot for friends tab
                        if (item.id == "friends") {
                            val friendsList by NuxSocialManager.friends.collectAsState()
                            val hasUnread = friendsList.any { it.unreadCount > 0 || it.isPendingReceived }
                            if (hasUnread) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding((2.5.dp).resp())
                                        .size((5.5.dp).resp())
                                        .clip(CircleShape)
                                        .background(Color(0xFFF43F5E))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height((5.dp).resp()))

            // BOTTOM: USER PROFILE AVATAR SHORTCUT
            val userPhoto = launcherUser?.photoURL ?: currentAccount?.photoUrl
            val userName = launcherUser?.username ?: currentAccount?.username
            val shape = RoundedCornerShape((8.dp).resp())

            Box(
                modifier = Modifier
                    .size((30.dp).resp())
                    .clip(shape)
                    .background(NuxColors.SurfaceElevated, shape)
                    .border(1.dp, NuxColors.CardBorder, shape)
                    .clickable { onTabSelected("settings") },
                contentAlignment = Alignment.Center
            ) {
                if (!userName.isNullOrBlank()) {
                    if (!userPhoto.isNullOrBlank()) {
                        NuxNetworkImage(
                            model = userPhoto,
                            contentDescription = "Avatar",
                            fallbackInitials = userName,
                            modifier = Modifier.fillMaxSize(),
                            shape = shape
                        )
                    } else {
                        Text(
                            text = userName.take(2).uppercase(),
                            color = NuxColors.ForestGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = (10.5.sp).resp()
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Settings",
                        tint = NuxColors.ForestGreen,
                        modifier = Modifier.size((17.dp).resp())
                    )
                }

                // Online status indicator dot
                Box(
                    modifier = Modifier
                        .size((6.dp).resp())
                        .align(Alignment.BottomEnd)
                        .offset(x = (-1).dp, y = (-1).dp)
                        .background(
                            if (launcherUser?.isActivated == true || currentAccount != null) NuxColors.ForestGreen else NuxColors.Amber,
                            CircleShape
                        )
                        .border(1.dp, Color(0xFF09090B), CircleShape)
                )
            }
        }

        // Right hairline border divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0x14FFFFFF))
        )
    }
}
