package com.israadev.nuxlauncher.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.israadev.nuxlauncher.core.controls.ControlLayoutManager
import com.israadev.nuxlauncher.core.controls.KeycodeCatalog
import com.israadev.nuxlauncher.core.controls.models.CustomControlButton
import com.israadev.nuxlauncher.ui.components.NuxBadge
import com.israadev.nuxlauncher.ui.components.NuxButton
import com.israadev.nuxlauncher.ui.components.NuxCard
import com.israadev.nuxlauncher.ui.components.NuxTextField
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes
import com.movtery.inputmap.keycodes.LwjglGlfwKeycode
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun CustomGuiEditorScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedButtons by ControlLayoutManager.buttons.collectAsState()

    // Working local copy of buttons for live editing
    var buttonsList by remember { mutableStateOf(savedButtons.ifEmpty { ControlLayoutManager.getDefaultButtons() }) }
    var selectedButtonId by remember { mutableStateOf<String?>(null) }
    var showKeyPickerForButtonId by remember { mutableStateOf<String?>(null) }
    var keyPickerTargetMode by remember { mutableStateOf("MAIN") } // "MAIN" or "COMBO"
    var keyPickerSearchQuery by remember { mutableStateOf("") }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var manualFlipSide by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        ControlLayoutManager.init(context)
        if (ControlLayoutManager.buttons.value.isNotEmpty()) {
            buttonsList = ControlLayoutManager.buttons.value
        }
    }

    LaunchedEffect(savedButtons) {
        if (savedButtons.isNotEmpty() && buttonsList.isEmpty()) {
            buttonsList = savedButtons
        }
    }

    val selectedButton = remember(buttonsList, selectedButtonId) {
        buttonsList.firstOrNull { it.id == selectedButtonId }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0E1411))
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        // 1. Dark Blueprint Dot Grid Canvas (High-Contrast, Sleek Tactical Look)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 32.dp.toPx()
            val dotColor = Color(0xFF22362C)
            var x = step
            while (x < size.width) {
                var y = step
                while (y < size.height) {
                    drawCircle(dotColor, radius = 1.5.dp.toPx(), center = Offset(x, y))
                    y += step
                }
                x += step
            }

            // Center guide lines
            drawLine(
                color = Color(0xFF22362C).copy(alpha = 0.6f),
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color(0xFF22362C).copy(alpha = 0.6f),
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 2. Interactive Draggable Buttons on Screen
        buttonsList.forEach { btn ->
            val isSelected = btn.id == selectedButtonId
            val btnWidthPx = with(density) { btn.widthDp.dp.toPx() }
            val btnHeightPx = with(density) { btn.heightDp.dp.toPx() }

            // Convert percentage coordinates to pixel center
            val centerX = screenWidthPx * (btn.xPercent / 100f)
            val centerY = screenHeightPx * (btn.yPercent / 100f)

            val leftPx = (centerX - btnWidthPx / 2f).coerceIn(0f, (screenWidthPx - btnWidthPx).coerceAtLeast(0f))
            val topPx = (centerY - btnHeightPx / 2f).coerceIn(0f, (screenHeightPx - btnHeightPx).coerceAtLeast(0f))

            val leftDp = with(density) { leftPx.toDp() }
            val topDp = with(density) { topPx.toDp() }

            val btnBg = if (isSelected) {
                Color(0xCC10B981)
            } else if (btn.isSystem) {
                when (btn.systemAction) {
                    "FPS" -> Color(0x800A0E17)
                    "HIDE_GUI" -> Color(0x730A0E17)
                    "CLOSE" -> Color(0x80EF4444).copy(alpha = 0.35f)
                    "KEYBOARD" -> Color(0x730A0E17)
                    else -> Color(0x730A0E17)
                }
            } else {
                Color(0x730A0E17)
            }

            val btnBorderColor = if (isSelected) {
                Color(0xFFFFD166)
            } else if (btn.isSystem) {
                when (btn.systemAction) {
                    "HIDE_GUI" -> Color(0x3834D399)
                    "CLOSE" -> Color(0x80EF4444)
                    "FPS" -> Color(0x3834D399)
                    "KEYBOARD" -> Color(0x3834D399)
                    else -> Color(0x3834D399)
                }
            } else {
                Color(0x3834D399)
            }

            Box(
                modifier = Modifier
                    .offset(x = leftDp, y = topDp)
                    .size(width = btn.widthDp.dp, height = btn.heightDp.dp)
                    .alpha(btn.opacity)
                    .background(btnBg, RoundedCornerShape(btn.cornerRadiusDp.dp))
                    .border(
                        if (isSelected) 2.2.dp else 1.5.dp,
                        btnBorderColor,
                        RoundedCornerShape(btn.cornerRadiusDp.dp)
                    )
                    .pointerInput(btn.id) {
                        detectTapGestures {
                            manualFlipSide = null
                            selectedButtonId = if (selectedButtonId == btn.id) null else btn.id
                        }
                    }
                    .pointerInput(btn.id, screenWidthPx, screenHeightPx) {
                        detectDragGestures(
                            onDragStart = {
                                manualFlipSide = null
                                selectedButtonId = btn.id
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val currentBtn = buttonsList.firstOrNull { it.id == btn.id } ?: return@detectDragGestures
                                val curCenterX = screenWidthPx * (currentBtn.xPercent / 100f)
                                val curCenterY = screenHeightPx * (currentBtn.yPercent / 100f)

                                val newCenterX = (curCenterX + dragAmount.x).coerceIn(btnWidthPx / 2f, screenWidthPx - btnWidthPx / 2f)
                                val newCenterY = (curCenterY + dragAmount.y).coerceIn(btnHeightPx / 2f, screenHeightPx - btnHeightPx / 2f)

                                val newXPercent = (newCenterX / screenWidthPx) * 100f
                                val newYPercent = (newCenterY / screenHeightPx) * 100f

                                buttonsList = buttonsList.map {
                                    if (it.id == btn.id) it.copy(xPercent = newXPercent, yPercent = newYPercent) else it
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (btn.isScroll) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "▲",
                            color = if (isSelected) Color(0xFFFFD166) else Color(0xFF34D399),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = btn.name,
                            color = if (isSelected) Color(0xFF022C22) else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "▼",
                            color = if (isSelected) Color(0xFFFFD166) else Color(0xFF34D399),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else if (btn.isSystem) {
                    when (btn.systemAction) {
                        "FPS" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                            ) {
                                Box(modifier = Modifier.size(7.dp).background(Color(0xFF10B981), CircleShape))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("FPS: --", color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                        "KEYBOARD" -> {
                            Text(
                                text = "KEYBOARD",
                                color = if (isSelected) Color(0xFF022C22) else Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                        "HIDE_GUI" -> {
                            Text(
                                text = "HIDE GUI",
                                color = if (isSelected) Color(0xFF022C22) else Color(0xFF34D399),
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                        "CLOSE" -> {
                            Text(
                                text = "✕",
                                color = Color(0xFFFCA5A5),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                        else -> {
                            Text(
                                text = btn.name,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = btn.name,
                            color = if (isSelected) Color(0xFF022C22) else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = if (btn.name.length > 5) 10.sp else 12.sp,
                            maxLines = 1
                        )
                        if (btn.isToggle) {
                            Text(
                                text = "TOGGLE",
                                color = if (isSelected) Color(0xFF022C22) else Color(0xFF34D399),
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (btn.isMacro) {
                            Text(
                                text = when (btn.macroType) {
                                    "COMMAND" -> "⚡CMD"
                                    "COMBO" -> "⚡CMB"
                                    "TURBO" -> "⚡TRB"
                                    else -> "⚡MAC"
                                },
                                color = if (isSelected) Color(0xFF022C22) else Color(0xFFFFD166),
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        // 3. TOP ACTION BAR (Clean & Un-clipped Floating Header)
        // Top-Left: Kembali Button
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color(0xFF161F1A), RoundedCornerShape(8.dp))
                .border(1.2.dp, Color(0xFF2C3E34), RoundedCornerShape(8.dp))
                .clickable { onNavigateBack() }
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color(0xFFE8F5E9),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text("KEMBALI", color = Color(0xFFE8F5E9), fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }

        // Top-Center: Compact Floating Action Pill (+ Tambah, Reset, Simpan)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .background(Color(0xFF161F1A), RoundedCornerShape(10.dp))
                .border(1.5.dp, Color(0xFF2C3E34), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // + TAMBAH
            Box(
                modifier = Modifier
                    .background(Color(0xFF2E7D5B), RoundedCornerShape(6.dp))
                    .clickable {
                        val newBtn = CustomControlButton(
                            id = "btn_" + UUID.randomUUID().toString().take(6),
                            name = "NEW",
                            keyCode = LwjglGlfwKeycode.GLFW_KEY_SPACE,
                            xPercent = 50f,
                            yPercent = 50f,
                            widthDp = 52,
                            heightDp = 48
                        )
                        buttonsList = buttonsList + newBtn
                        selectedButtonId = newBtn.id
                        manualFlipSide = null
                        Toast.makeText(context, "Tombol baru ditambahkan di tengah layar!", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("+ TAMBAH", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White)
            }

            // RESET
            Box(
                modifier = Modifier
                    .background(Color(0xFF381C1C), RoundedCornerShape(6.dp))
                    .clickable { showResetConfirmDialog = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("RESET", color = Color(0xFFFF8A80), fontWeight = FontWeight.Black, fontSize = 11.sp)
            }

            // SIMPAN
            Box(
                modifier = Modifier
                    .background(Color(0xFF43A047), RoundedCornerShape(6.dp))
                    .clickable {
                        ControlLayoutManager.saveButtons(context, buttonsList)
                        Toast.makeText(context, "✓ Layout GUI berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("SIMPAN", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }

        // 4. SMART AUTO-DOCKING INSPECTOR PANEL (NEVER COVERS THE SELECTED BUTTON!)
        if (selectedButton != null) {
            // If the selected button is on the right half, auto-dock panel on the LEFT side.
            // If the button is on the left half, auto-dock panel on the RIGHT side.
            val shouldDockLeft = manualFlipSide ?: (selectedButton.xPercent > 48f)

            val panelAlignment = if (shouldDockLeft) Alignment.CenterStart else Alignment.CenterEnd
            val panelPadding = if (shouldDockLeft) PaddingValues(start = 16.dp) else PaddingValues(end = 16.dp)

            Box(
                modifier = Modifier
                    .align(panelAlignment)
                    .padding(panelPadding)
            ) {
                NuxCard(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(0.85f),
                    backgroundColor = Color(0xFF16201B),
                    shadowOffset = 4.dp,
                    cornerRadius = 14.dp,
                    fillMaxHeight = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Inspector Header with Flip-Side & Close
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PENGATURAN TOMBOL",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Posisi: ${selectedButton.xPercent.roundToInt()}% x ${selectedButton.yPercent.roundToInt()}%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF81C784)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Flip Side Button (allows user to move panel to other side manually)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color(0xFF223129), CircleShape)
                                        .border(1.dp, Color(0xFF3B5244), CircleShape)
                                        .clickable { manualFlipSide = !shouldDockLeft },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⇄", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                                }

                                // Close Button
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color(0xFF3B1E22), CircleShape)
                                        .border(1.dp, Color(0xFF5E2D32), CircleShape)
                                        .clickable { selectedButtonId = null },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✕", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF8A80))
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF2C3E34), thickness = 1.dp)

                        // 1. Label Name Field
                        Text("Label Teks Tombol:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5D6A7))
                        if (selectedButton.isSystem) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1F2D25).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF2C3E34), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedButton.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("🔒 Tetap", color = Color(0xFF81C784), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1F2D25), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF3B5244), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                if (selectedButton.name.isEmpty()) {
                                    Text("Label tombol...", color = Color(0xFF6B8074), fontSize = 13.sp)
                                }
                                BasicTextField(
                                    value = selectedButton.name,
                                    onValueChange = { newName ->
                                        buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(name = newName) else it }
                                    },
                                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                    cursorBrush = SolidColor(Color(0xFF4CAF50)),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // 2. Mapped Keycode / Input
                        Text("Tombol / Aksi Input:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5D6A7))
                        if (selectedButton.isSystem) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1F2D25).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF2C3E34), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                val actionDesc = when (selectedButton.systemAction) {
                                    "FPS" -> "Indikator FPS In-Game"
                                    "KEYBOARD" -> "Toggle Keyboard Layar"
                                    "HIDE_GUI" -> "Sembunyikan/Tampilkan GUI"
                                    "CLOSE" -> "Keluar dari Permainan"
                                    else -> "Aksi Sistem Launcher"
                                }
                                Text(actionDesc, color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        } else {
                            val mappedKeyName = remember(selectedButton) {
                                if (selectedButton.isScroll) {
                                    "Scroll Wheel (Slide Naik/Turun)"
                                } else if (selectedButton.isMouseButton) {
                                    when (selectedButton.mouseButton) {
                                        LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT -> "Mouse Kiri (Attack)"
                                        LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT -> "Mouse Kanan (Use)"
                                        LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE -> "Mouse Tengah (Pick)"
                                        else -> "Mouse Btn ${selectedButton.mouseButton}"
                                    }
                                } else {
                                    KeycodeCatalog.ALL_KEYS.firstOrNull { it.keyCode == selectedButton.keyCode }?.displayName
                                        ?: "Key Code ${selectedButton.keyCode}"
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1F2D25), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF3B5244), RoundedCornerShape(8.dp))
                                    .clickable {
                                        keyPickerTargetMode = "MAIN"
                                        keyPickerSearchQuery = ""
                                        showKeyPickerForButtonId = selectedButton.id
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(mappedKeyName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ubah >", color = Color(0xFF69F0AE), fontWeight = FontWeight.Black, fontSize = 10.sp)
                                }
                            }
                        }

                        // 3. Width Slider
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Lebar (Width):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5D6A7))
                            Text("${selectedButton.widthDp} dp", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF69F0AE))
                        }
                        Slider(
                            value = selectedButton.widthDp.toFloat(),
                            onValueChange = { newW ->
                                buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(widthDp = newW.roundToInt()) else it }
                            },
                            valueRange = 24f..160f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF4CAF50),
                                activeTrackColor = Color(0xFF4CAF50),
                                inactiveTrackColor = Color(0xFF2C3E34)
                            )
                        )

                        // 4. Height Slider
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tinggi (Height):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5D6A7))
                            Text("${selectedButton.heightDp} dp", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF69F0AE))
                        }
                        Slider(
                            value = selectedButton.heightDp.toFloat(),
                            onValueChange = { newH ->
                                buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(heightDp = newH.roundToInt()) else it }
                            },
                            valueRange = 24f..160f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF4CAF50),
                                activeTrackColor = Color(0xFF4CAF50),
                                inactiveTrackColor = Color(0xFF2C3E34)
                            )
                        )

                        // 5. Opacity Slider
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Transparansi (Opacity):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5D6A7))
                            Text("${(selectedButton.opacity * 100).roundToInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF69F0AE))
                        }
                        Slider(
                            value = selectedButton.opacity,
                            onValueChange = { newOp ->
                                buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(opacity = (newOp * 100).roundToInt() / 100f) else it }
                            },
                            valueRange = 0.15f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF4CAF50),
                                activeTrackColor = Color(0xFF4CAF50),
                                inactiveTrackColor = Color(0xFF2C3E34)
                            )
                        )

                        // 6. Corner Radius Slider
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Kebulatan Sudut:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5D6A7))
                            Text("${selectedButton.cornerRadiusDp} dp", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF69F0AE))
                        }
                        Slider(
                            value = selectedButton.cornerRadiusDp.toFloat(),
                            onValueChange = { newR ->
                                buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(cornerRadiusDp = newR.roundToInt()) else it }
                            },
                            valueRange = 0f..28f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF4CAF50),
                                activeTrackColor = Color(0xFF4CAF50),
                                inactiveTrackColor = Color(0xFF2C3E34)
                            )
                        )

                        // 7. Toggle Mode Switch (Only for normal key buttons)
                        if (!selectedButton.isSystem) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Mode Toggle:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Sekali tap untuk kunci", fontSize = 8.sp, color = Color(0xFFA5D6A7))
                                }
                                Switch(
                                    checked = selectedButton.isToggle,
                                    onCheckedChange = { isToggled ->
                                        buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(isToggle = isToggled) else it }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF69F0AE),
                                        checkedTrackColor = Color(0xFF1B5E20),
                                        uncheckedThumbColor = Color(0xFF757575),
                                        uncheckedTrackColor = Color(0xFF2C3E34)
                                    )
                                )
                            }
                        }

                        // 8. Fitur Makro (Macro Settings)
                        if (!selectedButton.isSystem && !selectedButton.isScroll) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF16241D), RoundedCornerShape(10.dp))
                                    .border(1.dp, if (selectedButton.isMacro) Color(0xFF10B981) else Color(0xFF2C3E34), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Macro Header & Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("⚡ FITUR MAKRO", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (selectedButton.isMacro) Color(0xFF69F0AE) else Color.White)
                                            if (selectedButton.isMacro) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0x3310B981), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Text("AKTIF", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF69F0AE))
                                                }
                                            }
                                        }
                                        Text("Jadikan tombol ini sebagai makro otomatis", fontSize = 8.sp, color = Color(0xFFA5D6A7))
                                    }
                                    Switch(
                                        checked = selectedButton.isMacro,
                                        onCheckedChange = { isMacroEnabled ->
                                            buttonsList = buttonsList.map {
                                                if (it.id == selectedButton.id) it.copy(isMacro = isMacroEnabled) else it
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF69F0AE),
                                            checkedTrackColor = Color(0xFF1B5E20),
                                            uncheckedThumbColor = Color(0xFF757575),
                                            uncheckedTrackColor = Color(0xFF2C3E34)
                                        )
                                    )
                                }

                                if (selectedButton.isMacro) {
                                    HorizontalDivider(color = Color(0xFF23362A), thickness = 1.dp)

                                    // Macro Mode Selector
                                    Text("Pilih Mode Makro:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5D6A7))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val types = listOf(
                                            Triple("COMMAND", "⌨ Chat/Perintah", "Ketik otomatis"),
                                            Triple("COMBO", "🔗 Kombinasi", "2 Tombol"),
                                            Triple("TURBO", "⚡ Turbo Click", "Auto-clicker")
                                        )
                                        types.forEach { (typeKey, typeLabel, _) ->
                                            val isChosen = selectedButton.macroType == typeKey
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(
                                                        if (isChosen) Color(0xFF10B981) else Color(0xFF1F2D25),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isChosen) Color(0xFF69F0AE) else Color(0xFF2F4237),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable {
                                                        buttonsList = buttonsList.map {
                                                            if (it.id == selectedButton.id) it.copy(macroType = typeKey) else it
                                                        }
                                                    }
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = typeLabel,
                                                    fontSize = 8.sp,
                                                    fontWeight = if (isChosen) FontWeight.Black else FontWeight.Bold,
                                                    color = if (isChosen) Color(0xFF022C22) else Color.White,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    // Mode 1: COMMAND
                                    if (selectedButton.macroType == "COMMAND") {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Teks / Perintah yang diketik otomatis:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            BasicTextField(
                                                value = selectedButton.macroCommand,
                                                onValueChange = { newCmd ->
                                                    buttonsList = buttonsList.map {
                                                        if (it.id == selectedButton.id) it.copy(macroCommand = newCmd) else it
                                                    }
                                                },
                                                textStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                                cursorBrush = SolidColor(Color(0xFF69F0AE)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF111A15), RoundedCornerShape(6.dp))
                                                    .border(1.dp, Color(0xFF2F4237), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                decorationBox = { innerTextField ->
                                                    if (selectedButton.macroCommand.isEmpty()) {
                                                        Text(
                                                            text = "Misal: /gamemode creative atau /home",
                                                            color = Color(0xFF6B8A78),
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            )

                                            // Quick Chips
                                            Text("Template Cepat:", fontSize = 8.sp, color = Color(0xFFA5D6A7))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                listOf("/gamemode c", "/gamemode s", "/spawn", "/home").forEach { sample ->
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFF1B2921), RoundedCornerShape(4.dp))
                                                            .border(0.5.dp, Color(0xFF2C4234), RoundedCornerShape(4.dp))
                                                            .clickable {
                                                                buttonsList = buttonsList.map {
                                                                    if (it.id == selectedButton.id) it.copy(macroCommand = sample) else it
                                                                }
                                                            }
                                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(sample, fontSize = 8.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Mode 2: COMBO
                                    if (selectedButton.macroType == "COMBO") {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Tombol Kedua (Modifier / Ditahan Bersamaan):", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            val comboKeyName = remember(selectedButton.macroComboKey) {
                                                KeycodeCatalog.getKeyName(selectedButton.macroComboKey)
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF111A15), RoundedCornerShape(6.dp))
                                                    .border(1.dp, Color(0xFF3B5244), RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        keyPickerTargetMode = "COMBO"
                                                        keyPickerSearchQuery = ""
                                                        showKeyPickerForButtonId = selectedButton.id
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = if (selectedButton.macroComboKey == 0) "Belum Dipilih (Klik Disini)" else comboKeyName,
                                                        color = if (selectedButton.macroComboKey == 0) Color(0xFFE57373) else Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    Text("Pilih >", color = Color(0xFF69F0AE), fontWeight = FontWeight.Black, fontSize = 10.sp)
                                                }
                                            }

                                            // Quick Combo Chips
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                listOf(
                                                    "F3" to LwjglGlfwKeycode.GLFW_KEY_F3,
                                                    "SHIFT" to LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT,
                                                    "CTRL" to LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL,
                                                    "ALT" to LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT
                                                ).forEach { (label, code) ->
                                                    Box(
                                                        modifier = Modifier
                                                            .background(if (selectedButton.macroComboKey == code) Color(0x3310B981) else Color(0xFF1B2921), RoundedCornerShape(4.dp))
                                                            .border(0.5.dp, if (selectedButton.macroComboKey == code) Color(0xFF10B981) else Color(0xFF2C4234), RoundedCornerShape(4.dp))
                                                            .clickable {
                                                                buttonsList = buttonsList.map {
                                                                    if (it.id == selectedButton.id) it.copy(macroComboKey = code) else it
                                                                }
                                                            }
                                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(label, fontSize = 8.sp, color = if (selectedButton.macroComboKey == code) Color(0xFF69F0AE) else Color(0xFF81C784), fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Text("Contoh: F3 + B (Hitbox), Shift + Klik, Ctrl + Q", fontSize = 8.sp, color = Color(0xFFA5D6A7))
                                        }
                                    }

                                    // Mode 3: TURBO
                                    if (selectedButton.macroType == "TURBO") {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Interval Auto-Click:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                val cps = (1000f / selectedButton.macroTurboIntervalMs.coerceAtLeast(1L)).roundToInt()
                                                Text("${selectedButton.macroTurboIntervalMs} ms (~$cps CPS)", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF69F0AE))
                                            }
                                            Slider(
                                                value = selectedButton.macroTurboIntervalMs.toFloat(),
                                                onValueChange = { newInterval ->
                                                    buttonsList = buttonsList.map {
                                                        if (it.id == selectedButton.id) it.copy(macroTurboIntervalMs = newInterval.toLong()) else it
                                                    }
                                                },
                                                valueRange = 40f..500f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color(0xFF4CAF50),
                                                    activeTrackColor = Color(0xFF4CAF50),
                                                    inactiveTrackColor = Color(0xFF2C3E34)
                                                )
                                            )
                                            Text("Tombol akan menekan & melepas berulang-ulang sangat cepat secara otomatis saat ditekan.", fontSize = 8.sp, color = Color(0xFFA5D6A7))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // 8. Delete Button (or System locked notice)
                        if (!selectedButton.isSystem) {
                            NuxButton(
                                onClick = {
                                    buttonsList = buttonsList.filter { it.id != selectedButton.id }
                                    selectedButtonId = null
                                    Toast.makeText(context, "Tombol dihapus", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth().height(38.dp),
                                backgroundColor = Color(0xFF3E181B),
                                contentColor = Color(0xFFFF8A80),
                                shadowOffset = 2.dp,
                                cornerRadius = 8.dp
                            ) {
                                Text("HAPUS TOMBOL", color = Color(0xFFFF8A80), fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF222F27), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF2C3E34), RoundedCornerShape(8.dp))
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🔒 Tombol sistem tidak dapat dihapus",
                                    color = Color(0xFFA5D6A7),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Keycode Picker Dialog
    if (showKeyPickerForButtonId != null) {
        val targetId = showKeyPickerForButtonId!!
        val isComboMode = keyPickerTargetMode == "COMBO"

        Dialog(onDismissRequest = {
            showKeyPickerForButtonId = null
            keyPickerSearchQuery = ""
        }) {
            NuxCard(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.88f),
                backgroundColor = Color(0xFF16201B),
                shadowOffset = 6.dp,
                cornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isComboMode) "Pilih Tombol Kedua (Kombinasi)" else "Pilih Input Key Minecraft",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (isComboMode) "Tombol ini akan ditekan bersamaan dengan tombol utama" else "Tersedia semua keyboard A-Z, F1-F12, angka, modifier & simbol",
                                fontSize = 9.sp,
                                color = Color(0xFFA5D6A7)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF3B1E22), CircleShape)
                                .border(1.dp, Color(0xFF5E2D32), CircleShape)
                                .clickable {
                                    showKeyPickerForButtonId = null
                                    keyPickerSearchQuery = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF8A80))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Input Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1F2D25), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF3B5244), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔍", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = keyPickerSearchQuery,
                            onValueChange = { keyPickerSearchQuery = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                            cursorBrush = SolidColor(Color(0xFF69F0AE)),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (keyPickerSearchQuery.isEmpty()) {
                                    Text("Cari key... (misal: W, Alt, F3, Tab, Shift, Numpad)", color = Color(0xFF6B8A78), fontSize = 11.sp)
                                }
                                innerTextField()
                            }
                        )
                        if (keyPickerSearchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Color(0xFF2E3E34), CircleShape)
                                    .clickable { keyPickerSearchQuery = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✕", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Key List
                    val availableKeys = remember(isComboMode) {
                        if (isComboMode) {
                            KeycodeCatalog.ALL_KEYS.filter { !it.isScroll && !it.isMouseButton }
                        } else {
                            KeycodeCatalog.ALL_KEYS
                        }
                    }

                    val filteredKeys = remember(availableKeys, keyPickerSearchQuery) {
                        if (keyPickerSearchQuery.isBlank()) {
                            availableKeys
                        } else {
                            val q = keyPickerSearchQuery.trim().lowercase()
                            availableKeys.filter {
                                it.displayName.lowercase().contains(q) || it.category.lowercase().contains(q)
                            }
                        }
                    }

                    if (filteredKeys.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Tidak ada tombol yang cocok dengan \"$keyPickerSearchQuery\"",
                                color = Color(0xFF81C784),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val grouped = filteredKeys.groupBy { it.category }
                            grouped.forEach { (category, keys) ->
                                item {
                                    Text(
                                        text = category.uppercase(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = Color(0xFF69F0AE),
                                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                    )
                                }
                                items(keys) { keyOpt ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF1F2D25), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFF2F4237), RoundedCornerShape(8.dp))
                                            .clickable {
                                                buttonsList = buttonsList.map {
                                                    if (it.id == targetId) {
                                                        if (isComboMode) {
                                                            it.copy(macroComboKey = keyOpt.keyCode)
                                                        } else {
                                                            it.copy(
                                                                keyCode = keyOpt.keyCode,
                                                                isMouseButton = keyOpt.isMouseButton,
                                                                mouseButton = keyOpt.mouseButton,
                                                                isScroll = keyOpt.isScroll,
                                                                name = if (keyOpt.isScroll) "SCROLL"
                                                                else if (it.name == "NEW" || it.name == "BTN" || it.name == "SCROLL") {
                                                                    keyOpt.displayName.substringBefore(" ").take(6)
                                                                } else it.name
                                                            )
                                                        }
                                                    } else it
                                                }
                                                showKeyPickerForButtonId = null
                                                keyPickerSearchQuery = ""
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = keyOpt.displayName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = keyOpt.category,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 9.sp,
                                                color = Color(0xFFA5D6A7)
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
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        Dialog(onDismissRequest = { showResetConfirmDialog = false }) {
            NuxCard(
                modifier = Modifier.width(320.dp),
                backgroundColor = Color(0xFF16201B),
                shadowOffset = 6.dp,
                cornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Reset Layout GUI?", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                    Text("Semua posisi tombol dan kustomisasi akan dikembalikan ke tata letak awal standar NUX.", fontSize = 12.sp, color = Color(0xFFA5D6A7))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NuxButton(
                            onClick = { showResetConfirmDialog = false },
                            modifier = Modifier.weight(1f).height(44.dp),
                            backgroundColor = Color(0xFF223129),
                            contentColor = Color.White,
                            shadowOffset = 2.dp
                        ) {
                            Text("BATAL", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White)
                        }

                        NuxButton(
                            onClick = {
                                buttonsList = ControlLayoutManager.getDefaultButtons()
                                selectedButtonId = null
                                showResetConfirmDialog = false
                                Toast.makeText(context, "Layout direset ke standar", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            backgroundColor = Color(0xFFC62828),
                            contentColor = Color.White,
                            shadowOffset = 2.dp
                        ) {
                            Text("RESET", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
