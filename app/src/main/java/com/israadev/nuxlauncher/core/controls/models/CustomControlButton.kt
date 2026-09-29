package com.israadev.nuxlauncher.core.controls.models

import com.movtery.inputmap.keycodes.LwjglGlfwKeycode
import java.util.UUID

/**
 * Model konfigurasi sebuah tombol virtual touch kustom
 */
data class CustomControlButton(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "BTN",
    val keyCode: Int = LwjglGlfwKeycode.GLFW_KEY_SPACE,
    val isMouseButton: Boolean = false,
    val mouseButton: Int = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT,
    val xPercent: Float = 50f, // 0f - 100f
    val yPercent: Float = 50f, // 0f - 100f
    val widthDp: Int = 50,
    val heightDp: Int = 48,
    val opacity: Float = 0.85f, // 0.1f - 1.0f
    val cornerRadiusDp: Int = 8,
    val isToggle: Boolean = false,
    val isScroll: Boolean = false,
    val isSystem: Boolean = false,
    val systemAction: String = "" // "FPS", "KEYBOARD", "HIDE_GUI", "CLOSE"
)
