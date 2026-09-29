package com.israadev.nuxlauncher.core.controls

import com.movtery.inputmap.keycodes.LwjglGlfwKeycode

data class KeyOption(
    val displayName: String,
    val keyCode: Int,
    val isMouseButton: Boolean = false,
    val mouseButton: Int = 0,
    val isScroll: Boolean = false,
    val category: String = "Umum"
)

object KeycodeCatalog {
    val ALL_KEYS: List<KeyOption> = listOf(
        // Mouse Controls
        KeyOption("Mouse Kiri (Attack/Break)", 0, isMouseButton = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, category = "Mouse"),
        KeyOption("Mouse Kanan (Use/Place)", 0, isMouseButton = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT, category = "Mouse"),
        KeyOption("Mouse Tengah (Pick Block)", 0, isMouseButton = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE, category = "Mouse"),
        KeyOption("Scroll Wheel (Slide Naik/Turun)", 0, isScroll = true, category = "Mouse"),

        // Gerakan (Movement)
        KeyOption("W (Maju)", LwjglGlfwKeycode.GLFW_KEY_W, category = "Gerakan"),
        KeyOption("A (Kiri)", LwjglGlfwKeycode.GLFW_KEY_A, category = "Gerakan"),
        KeyOption("S (Mundur)", LwjglGlfwKeycode.GLFW_KEY_S, category = "Gerakan"),
        KeyOption("D (Kanan)", LwjglGlfwKeycode.GLFW_KEY_D, category = "Gerakan"),
        KeyOption("SPACE (Lompat)", LwjglGlfwKeycode.GLFW_KEY_SPACE, category = "Gerakan"),
        KeyOption("LEFT SHIFT (Sneak/Jongkok)", LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT, category = "Gerakan"),
        KeyOption("LEFT CTRL (Sprint/Lari)", LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL, category = "Gerakan"),

        // Interaksi Game
        KeyOption("E (Inventory)", LwjglGlfwKeycode.GLFW_KEY_E, category = "Aksi"),
        KeyOption("Q (Drop Item)", LwjglGlfwKeycode.GLFW_KEY_Q, category = "Aksi"),
        KeyOption("F (Swap Hand)", LwjglGlfwKeycode.GLFW_KEY_F, category = "Aksi"),
        KeyOption("T (Buka Chat)", LwjglGlfwKeycode.GLFW_KEY_T, category = "Aksi"),
        KeyOption("/ (Buka Command)", LwjglGlfwKeycode.GLFW_KEY_SLASH, category = "Aksi"),
        KeyOption("ENTER (Kirim/Chat)", LwjglGlfwKeycode.GLFW_KEY_ENTER, category = "Aksi"),
        KeyOption("ESCAPE (Pause/Menu)", LwjglGlfwKeycode.GLFW_KEY_ESCAPE, category = "Aksi"),
        KeyOption("TAB (Daftar Player)", LwjglGlfwKeycode.GLFW_KEY_TAB, category = "Aksi"),

        // Fungsi Debug & Tampilan
        KeyOption("F1 (Sembunyikan HUD MC)", LwjglGlfwKeycode.GLFW_KEY_F1, category = "Fungsi"),
        KeyOption("F2 (Screenshot)", LwjglGlfwKeycode.GLFW_KEY_F2, category = "Fungsi"),
        KeyOption("F3 (Debug Layar)", LwjglGlfwKeycode.GLFW_KEY_F3, category = "Fungsi"),
        KeyOption("F5 (Ubah Perspektif)", LwjglGlfwKeycode.GLFW_KEY_F5, category = "Fungsi"),
        KeyOption("F11 (Fullscreen)", LwjglGlfwKeycode.GLFW_KEY_F11, category = "Fungsi"),

        // Hotbar Angka (1-9)
        KeyOption("Slot 1", LwjglGlfwKeycode.GLFW_KEY_1, category = "Hotbar"),
        KeyOption("Slot 2", LwjglGlfwKeycode.GLFW_KEY_2, category = "Hotbar"),
        KeyOption("Slot 3", LwjglGlfwKeycode.GLFW_KEY_3, category = "Hotbar"),
        KeyOption("Slot 4", LwjglGlfwKeycode.GLFW_KEY_4, category = "Hotbar"),
        KeyOption("Slot 5", LwjglGlfwKeycode.GLFW_KEY_5, category = "Hotbar"),
        KeyOption("Slot 6", LwjglGlfwKeycode.GLFW_KEY_6, category = "Hotbar"),
        KeyOption("Slot 7", LwjglGlfwKeycode.GLFW_KEY_7, category = "Hotbar"),
        KeyOption("Slot 8", LwjglGlfwKeycode.GLFW_KEY_8, category = "Hotbar"),
        KeyOption("Slot 9", LwjglGlfwKeycode.GLFW_KEY_9, category = "Hotbar"),

        // Huruf Tambahan
        KeyOption("B", LwjglGlfwKeycode.GLFW_KEY_B, category = "Lainnya"),
        KeyOption("C (Zoom)", LwjglGlfwKeycode.GLFW_KEY_C, category = "Lainnya"),
        KeyOption("G", LwjglGlfwKeycode.GLFW_KEY_G, category = "Lainnya"),
        KeyOption("H", LwjglGlfwKeycode.GLFW_KEY_H, category = "Lainnya"),
        KeyOption("M", LwjglGlfwKeycode.GLFW_KEY_M, category = "Lainnya"),
        KeyOption("R", LwjglGlfwKeycode.GLFW_KEY_R, category = "Lainnya"),
        KeyOption("V", LwjglGlfwKeycode.GLFW_KEY_V, category = "Lainnya"),
        KeyOption("X", LwjglGlfwKeycode.GLFW_KEY_X, category = "Lainnya"),
        KeyOption("Z", LwjglGlfwKeycode.GLFW_KEY_Z, category = "Lainnya")
    )
}
