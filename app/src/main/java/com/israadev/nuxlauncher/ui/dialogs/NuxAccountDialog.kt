package com.israadev.nuxlauncher.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.models.UserAccount
import com.israadev.nuxlauncher.ui.components.*
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes
import java.util.UUID

@Composable
fun NuxAccountDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accounts by AccountManager.accounts.collectAsState()
    val currentAccount by AccountManager.currentAccount.collectAsState()
    var newUsername by remember { mutableStateOf("") }

    NuxDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MANAJEMEN AKUN",
                        color = NuxColors.DarkGray,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    NuxBadge(text = "OFFLINE", backgroundColor = NuxColors.SoftLime, textColor = NuxColors.ForestGreen)
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(NuxColors.SurfaceWhite, NuxSizes.ShapeSmall)
                        .border(2.dp, NuxColors.DarkGray, NuxSizes.ShapeSmall)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = NuxColors.DarkGray,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Two-Column Content (Landscape Optimized)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LEFT COLUMN: Accounts List
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "AKUN TERSIMPAN (${accounts.size})",
                        color = NuxColors.GrayNeutral,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(NuxColors.SurfaceWhite, NuxSizes.ShapeSmall)
                            .border(2.dp, NuxColors.DarkGray, NuxSizes.ShapeSmall)
                            .padding(4.dp)
                    ) {
                        if (accounts.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Belum Ada Akun",
                                        color = NuxColors.DarkGray,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Tambah akun di samping untuk mulai bermain.",
                                        color = NuxColors.GrayNeutral,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(accounts) { acc ->
                                    val isSelected = acc.username == currentAccount?.username
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isSelected) NuxColors.SoftLime else Color.Transparent,
                                                NuxSizes.ShapeSmall
                                            )
                                            .clickable {
                                                AccountManager.selectAccount(acc)
                                                onDismiss()
                                            }
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(
                                                        if (isSelected) NuxColors.ForestGreen else NuxColors.DarkGray,
                                                        NuxSizes.ShapeSmall
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = acc.username.take(2).uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = acc.username,
                                                color = NuxColors.DarkGray,
                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            val (bText, bBg, bColor) = when (acc.safeAccountType) {
                                                "microsoft" -> Triple("MS", Color(0xFF0078D4).copy(alpha = 0.15f), Color(0xFF0078D4))
                                                "elyby" -> Triple("ELY", Color(0xFF8E24AA).copy(alpha = 0.15f), Color(0xFF8E24AA))
                                                else -> Triple("OFF", NuxColors.LightGreen, NuxColors.ForestGreen)
                                            }
                                            NuxBadge(text = bText, backgroundColor = bBg, textColor = bColor)
                                        }

                                        if (isSelected) {
                                            NuxBadge(text = "AKTIF", backgroundColor = NuxColors.ForestGreen, textColor = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // RIGHT COLUMN: Add New Account
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "TAMBAH AKUN OFFLINE",
                            color = NuxColors.GrayNeutral,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        NuxTextField(
                            value = newUsername,
                            onValueChange = { newUsername = it },
                            placeholder = "Username Minecraft..."
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Mode offline memungkinkan Anda bermain tanpa akun Microsoft resmi.",
                            color = NuxColors.GrayNeutral,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }

                    NuxButton(
                        onClick = {
                            if (newUsername.isNotBlank()) {
                                val account = UserAccount(
                                    id = UUID.randomUUID().toString(),
                                    username = newUsername.trim(),
                                    uuid = UUID.nameUUIDFromBytes("OfflinePlayer:${newUsername.trim()}".toByteArray(Charsets.UTF_8)).toString().replace("-", ""),
                                    isOffline = true,
                                    accountType = "offline"
                                )
                                AccountManager.addAccount(context, account)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = NuxColors.ForestGreen,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = "SIMPAN AKUN >",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
