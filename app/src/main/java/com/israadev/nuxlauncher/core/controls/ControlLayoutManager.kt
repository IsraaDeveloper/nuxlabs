package com.israadev.nuxlauncher.core.controls

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.israadev.nuxlauncher.core.controls.models.CustomControlButton
import com.movtery.inputmap.keycodes.LwjglGlfwKeycode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object ControlLayoutManager {
    private val gson = Gson()
    private val _buttons = MutableStateFlow<List<CustomControlButton>>(emptyList())
    val buttons: StateFlow<List<CustomControlButton>> = _buttons.asStateFlow()

    private fun getLayoutFile(context: Context): File {
        val dir = File(context.filesDir, "controls")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "default_layout.json")
    }

    fun init(context: Context) {
        val file = getLayoutFile(context)
        if (file.exists()) {
            try {
                val json = file.readText()
                val type = object : TypeToken<List<CustomControlButton>>() {}.type
                val loaded: List<CustomControlButton>? = gson.fromJson(json, type)
                if (!loaded.isNullOrEmpty()) {
                    _buttons.value = loaded
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Initialize default layout
        resetToDefaults(context)
    }

    fun getDefaultButtons(): List<CustomControlButton> {
        return listOf(
            // Top-Left FPS Indicator
            CustomControlButton(
                id = "sys_fps",
                name = "FPS",
                xPercent = 6f,
                yPercent = 7f,
                widthDp = 64,
                heightDp = 28,
                opacity = 0.85f,
                cornerRadiusDp = 8,
                isSystem = true,
                systemAction = "FPS"
            ),

            // Top-Left Utility
            CustomControlButton(
                id = "key_esc",
                name = "ESC",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_ESCAPE,
                xPercent = 6f,
                yPercent = 20f,
                widthDp = 46,
                heightDp = 38,
                opacity = 0.85f,
                cornerRadiusDp = 8
            ),

            // Left Movement & Modifier Cluster (Clean D-Pad with Center SHIFT and Top-Left SPRINT)
            CustomControlButton(
                id = "key_ctrl",
                name = "SPRINT",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL,
                xPercent = 6f,
                yPercent = 52f,
                widthDp = 48,
                heightDp = 48,
                opacity = 0.85f,
                cornerRadiusDp = 10,
                isToggle = true
            ),
            CustomControlButton(
                id = "key_w",
                name = "W",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_W,
                xPercent = 14f,
                yPercent = 52f,
                widthDp = 48,
                heightDp = 48,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),
            CustomControlButton(
                id = "key_a",
                name = "A",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_A,
                xPercent = 6f,
                yPercent = 70f,
                widthDp = 48,
                heightDp = 48,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),
            CustomControlButton(
                id = "key_shift",
                name = "SHIFT",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT,
                xPercent = 14f,
                yPercent = 70f,
                widthDp = 48,
                heightDp = 48,
                opacity = 0.85f,
                cornerRadiusDp = 10,
                isToggle = true
            ),
            CustomControlButton(
                id = "key_d",
                name = "D",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_D,
                xPercent = 22f,
                yPercent = 70f,
                widthDp = 48,
                heightDp = 48,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),
            CustomControlButton(
                id = "key_s",
                name = "S",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_S,
                xPercent = 14f,
                yPercent = 88f,
                widthDp = 48,
                heightDp = 48,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),

            // Top-Center Quick Actions (Chat, Enter, Zoom)
            CustomControlButton(
                id = "key_t",
                name = "T",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_T,
                xPercent = 46f,
                yPercent = 22f,
                widthDp = 40,
                heightDp = 40,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),
            CustomControlButton(
                id = "key_enter",
                name = "ENTER",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_ENTER,
                xPercent = 52f,
                yPercent = 22f,
                widthDp = 50,
                heightDp = 40,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),
            CustomControlButton(
                id = "key_c",
                name = "C",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_C,
                xPercent = 58f,
                yPercent = 22f,
                widthDp = 40,
                heightDp = 40,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),

            // Middle-Right Utility & Scroll (F5 / F3 stacked next to vertical scroll)
            CustomControlButton(
                id = "key_f5",
                name = "F5",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_F5,
                xPercent = 56f,
                yPercent = 67f,
                widthDp = 44,
                heightDp = 34,
                opacity = 0.85f,
                cornerRadiusDp = 8
            ),
            CustomControlButton(
                id = "key_f3",
                name = "F3",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_F3,
                xPercent = 56f,
                yPercent = 79f,
                widthDp = 44,
                heightDp = 34,
                opacity = 0.85f,
                cornerRadiusDp = 8
            ),
            CustomControlButton(
                id = "widget_scroll",
                name = "SCROLL",
                xPercent = 62f,
                yPercent = 73f,
                widthDp = 42,
                heightDp = 76,
                opacity = 0.85f,
                cornerRadiusDp = 12,
                isScroll = true
            ),

            // Top-Right System Controls
            CustomControlButton(
                id = "sys_keyboard",
                name = "KEYBOARD",
                xPercent = 78f,
                yPercent = 7f,
                widthDp = 76,
                heightDp = 28,
                opacity = 0.85f,
                cornerRadiusDp = 8,
                isSystem = true,
                systemAction = "KEYBOARD"
            ),
            CustomControlButton(
                id = "sys_hide_gui",
                name = "HIDE GUI",
                xPercent = 89f,
                yPercent = 7f,
                widthDp = 72,
                heightDp = 28,
                opacity = 0.85f,
                cornerRadiusDp = 8,
                isSystem = true,
                systemAction = "HIDE_GUI"
            ),
            CustomControlButton(
                id = "sys_close",
                name = "✕",
                xPercent = 96.5f,
                yPercent = 7f,
                widthDp = 30,
                heightDp = 28,
                opacity = 0.85f,
                cornerRadiusDp = 8,
                isSystem = true,
                systemAction = "CLOSE"
            ),

            // Right-Side Mouse Clicks (Underneath KEYBOARD and HIDE GUI)
            CustomControlButton(
                id = "mouse_left",
                name = "L-CLICK",
                isMouseButton = true,
                mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT,
                xPercent = 78f,
                yPercent = 22f,
                widthDp = 56,
                heightDp = 48,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),
            CustomControlButton(
                id = "mouse_right",
                name = "R-CLICK",
                isMouseButton = true,
                mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT,
                xPercent = 87f,
                yPercent = 22f,
                widthDp = 56,
                heightDp = 48,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),

            // Bottom-Right In-Game Actions
            CustomControlButton(
                id = "key_inv",
                name = "INV",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_E,
                xPercent = 77f,
                yPercent = 74f,
                widthDp = 54,
                heightDp = 48,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),
            CustomControlButton(
                id = "key_jump",
                name = "JUMP",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_SPACE,
                xPercent = 87f,
                yPercent = 74f,
                widthDp = 64,
                heightDp = 50,
                opacity = 0.85f,
                cornerRadiusDp = 10
            )
        )
    }

    fun resetToDefaults(context: Context) {
        val defaults = getDefaultButtons()
        _buttons.value = defaults
        save(context, defaults)
    }

    fun saveButtons(context: Context, newButtons: List<CustomControlButton>) {
        _buttons.value = newButtons
        save(context, newButtons)
    }

    fun addButton(context: Context, button: CustomControlButton) {
        val current = _buttons.value.toMutableList()
        current.add(button)
        saveButtons(context, current)
    }

    fun updateButton(context: Context, updated: CustomControlButton) {
        val current = _buttons.value.toMutableList()
        val index = current.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            current[index] = updated
            saveButtons(context, current)
        }
    }

    fun deleteButton(context: Context, buttonId: String) {
        val current = _buttons.value.filter { it.id != buttonId }
        saveButtons(context, current)
    }

    private fun save(context: Context, list: List<CustomControlButton>) {
        try {
            val file = getLayoutFile(context)
            file.writeText(gson.toJson(list))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
