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
        KeyOption("Mouse Tombol 4", 0, isMouseButton = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_4, category = "Mouse"),
        KeyOption("Mouse Tombol 5", 0, isMouseButton = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_5, category = "Mouse"),
        KeyOption("Scroll Wheel (Slide Naik/Turun)", 0, isScroll = true, category = "Mouse"),

        // Gerakan (Movement)
        KeyOption("W (Maju)", LwjglGlfwKeycode.GLFW_KEY_W, category = "Gerakan"),
        KeyOption("A (Kiri)", LwjglGlfwKeycode.GLFW_KEY_A, category = "Gerakan"),
        KeyOption("S (Mundur)", LwjglGlfwKeycode.GLFW_KEY_S, category = "Gerakan"),
        KeyOption("D (Kanan)", LwjglGlfwKeycode.GLFW_KEY_D, category = "Gerakan"),
        KeyOption("SPACE (Lompat)", LwjglGlfwKeycode.GLFW_KEY_SPACE, category = "Gerakan"),
        KeyOption("LEFT SHIFT (Sneak/Jongkok)", LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT, category = "Gerakan"),
        KeyOption("LEFT CTRL (Sprint/Lari)", LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL, category = "Gerakan"),

        // Aksi Game
        KeyOption("E (Inventory)", LwjglGlfwKeycode.GLFW_KEY_E, category = "Aksi"),
        KeyOption("Q (Drop Item)", LwjglGlfwKeycode.GLFW_KEY_Q, category = "Aksi"),
        KeyOption("F (Swap Hand)", LwjglGlfwKeycode.GLFW_KEY_F, category = "Aksi"),
        KeyOption("T (Buka Chat)", LwjglGlfwKeycode.GLFW_KEY_T, category = "Aksi"),
        KeyOption("/ (Buka Command)", LwjglGlfwKeycode.GLFW_KEY_SLASH, category = "Aksi"),
        KeyOption("ENTER (Kirim/Pilih)", LwjglGlfwKeycode.GLFW_KEY_ENTER, category = "Aksi"),
        KeyOption("ESCAPE (Pause/Menu)", LwjglGlfwKeycode.GLFW_KEY_ESCAPE, category = "Aksi"),
        KeyOption("TAB (Daftar Player)", LwjglGlfwKeycode.GLFW_KEY_TAB, category = "Aksi"),
        KeyOption("BACKSPACE (Hapus)", LwjglGlfwKeycode.GLFW_KEY_BACKSPACE, category = "Aksi"),

        // Modifier & Kontrol
        KeyOption("TAB", LwjglGlfwKeycode.GLFW_KEY_TAB, category = "Modifier & Kontrol"),
        KeyOption("LEFT ALT", LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT, category = "Modifier & Kontrol"),
        KeyOption("RIGHT ALT", LwjglGlfwKeycode.GLFW_KEY_RIGHT_ALT, category = "Modifier & Kontrol"),
        KeyOption("LEFT CONTROL", LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL, category = "Modifier & Kontrol"),
        KeyOption("RIGHT CONTROL", LwjglGlfwKeycode.GLFW_KEY_RIGHT_CONTROL, category = "Modifier & Kontrol"),
        KeyOption("LEFT SHIFT", LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT, category = "Modifier & Kontrol"),
        KeyOption("RIGHT SHIFT", LwjglGlfwKeycode.GLFW_KEY_RIGHT_SHIFT, category = "Modifier & Kontrol"),
        KeyOption("CAPS LOCK", LwjglGlfwKeycode.GLFW_KEY_CAPS_LOCK, category = "Modifier & Kontrol"),
        KeyOption("NUM LOCK", LwjglGlfwKeycode.GLFW_KEY_NUM_LOCK, category = "Modifier & Kontrol"),
        KeyOption("SCROLL LOCK", LwjglGlfwKeycode.GLFW_KEY_SCROLL_LOCK, category = "Modifier & Kontrol"),
        KeyOption("LEFT SUPER (Win)", LwjglGlfwKeycode.GLFW_KEY_LEFT_SUPER, category = "Modifier & Kontrol"),
        KeyOption("RIGHT SUPER", LwjglGlfwKeycode.GLFW_KEY_RIGHT_SUPER, category = "Modifier & Kontrol"),
        KeyOption("MENU", LwjglGlfwKeycode.GLFW_KEY_MENU, category = "Modifier & Kontrol"),
        KeyOption("PRINT SCREEN", LwjglGlfwKeycode.GLFW_KEY_PRINT_SCREEN, category = "Modifier & Kontrol"),
        KeyOption("PAUSE / BREAK", LwjglGlfwKeycode.GLFW_KEY_PAUSE, category = "Modifier & Kontrol"),

        // Fungsi (F1 - F12)
        KeyOption("F1 (Sembunyikan HUD)", LwjglGlfwKeycode.GLFW_KEY_F1, category = "Fungsi (F1-F12)"),
        KeyOption("F2 (Screenshot)", LwjglGlfwKeycode.GLFW_KEY_F2, category = "Fungsi (F1-F12)"),
        KeyOption("F3 (Debug Layar)", LwjglGlfwKeycode.GLFW_KEY_F3, category = "Fungsi (F1-F12)"),
        KeyOption("F4 (Shader / Spectator)", LwjglGlfwKeycode.GLFW_KEY_F4, category = "Fungsi (F1-F12)"),
        KeyOption("F5 (Ubah Perspektif)", LwjglGlfwKeycode.GLFW_KEY_F5, category = "Fungsi (F1-F12)"),
        KeyOption("F6", LwjglGlfwKeycode.GLFW_KEY_F6, category = "Fungsi (F1-F12)"),
        KeyOption("F7", LwjglGlfwKeycode.GLFW_KEY_F7, category = "Fungsi (F1-F12)"),
        KeyOption("F8 (Cinematic Camera)", LwjglGlfwKeycode.GLFW_KEY_F8, category = "Fungsi (F1-F12)"),
        KeyOption("F9", LwjglGlfwKeycode.GLFW_KEY_F9, category = "Fungsi (F1-F12)"),
        KeyOption("F10", LwjglGlfwKeycode.GLFW_KEY_F10, category = "Fungsi (F1-F12)"),
        KeyOption("F11 (Fullscreen)", LwjglGlfwKeycode.GLFW_KEY_F11, category = "Fungsi (F1-F12)"),
        KeyOption("F12", LwjglGlfwKeycode.GLFW_KEY_F12, category = "Fungsi (F1-F12)"),

        // Alfabet (A - Z)
        KeyOption("A", LwjglGlfwKeycode.GLFW_KEY_A, category = "Alfabet (A - Z)"),
        KeyOption("B", LwjglGlfwKeycode.GLFW_KEY_B, category = "Alfabet (A - Z)"),
        KeyOption("C (Zoom)", LwjglGlfwKeycode.GLFW_KEY_C, category = "Alfabet (A - Z)"),
        KeyOption("D", LwjglGlfwKeycode.GLFW_KEY_D, category = "Alfabet (A - Z)"),
        KeyOption("E", LwjglGlfwKeycode.GLFW_KEY_E, category = "Alfabet (A - Z)"),
        KeyOption("F", LwjglGlfwKeycode.GLFW_KEY_F, category = "Alfabet (A - Z)"),
        KeyOption("G", LwjglGlfwKeycode.GLFW_KEY_G, category = "Alfabet (A - Z)"),
        KeyOption("H", LwjglGlfwKeycode.GLFW_KEY_H, category = "Alfabet (A - Z)"),
        KeyOption("I", LwjglGlfwKeycode.GLFW_KEY_I, category = "Alfabet (A - Z)"),
        KeyOption("J", LwjglGlfwKeycode.GLFW_KEY_J, category = "Alfabet (A - Z)"),
        KeyOption("K", LwjglGlfwKeycode.GLFW_KEY_K, category = "Alfabet (A - Z)"),
        KeyOption("L", LwjglGlfwKeycode.GLFW_KEY_L, category = "Alfabet (A - Z)"),
        KeyOption("M", LwjglGlfwKeycode.GLFW_KEY_M, category = "Alfabet (A - Z)"),
        KeyOption("N", LwjglGlfwKeycode.GLFW_KEY_N, category = "Alfabet (A - Z)"),
        KeyOption("O", LwjglGlfwKeycode.GLFW_KEY_O, category = "Alfabet (A - Z)"),
        KeyOption("P", LwjglGlfwKeycode.GLFW_KEY_P, category = "Alfabet (A - Z)"),
        KeyOption("Q", LwjglGlfwKeycode.GLFW_KEY_Q, category = "Alfabet (A - Z)"),
        KeyOption("R", LwjglGlfwKeycode.GLFW_KEY_R, category = "Alfabet (A - Z)"),
        KeyOption("S", LwjglGlfwKeycode.GLFW_KEY_S, category = "Alfabet (A - Z)"),
        KeyOption("T", LwjglGlfwKeycode.GLFW_KEY_T, category = "Alfabet (A - Z)"),
        KeyOption("U", LwjglGlfwKeycode.GLFW_KEY_U, category = "Alfabet (A - Z)"),
        KeyOption("V", LwjglGlfwKeycode.GLFW_KEY_V, category = "Alfabet (A - Z)"),
        KeyOption("W", LwjglGlfwKeycode.GLFW_KEY_W, category = "Alfabet (A - Z)"),
        KeyOption("X", LwjglGlfwKeycode.GLFW_KEY_X, category = "Alfabet (A - Z)"),
        KeyOption("Y", LwjglGlfwKeycode.GLFW_KEY_Y, category = "Alfabet (A - Z)"),
        KeyOption("Z", LwjglGlfwKeycode.GLFW_KEY_Z, category = "Alfabet (A - Z)"),

        // Angka & Hotbar (0 - 9)
        KeyOption("1 (Slot 1)", LwjglGlfwKeycode.GLFW_KEY_1, category = "Angka & Hotbar"),
        KeyOption("2 (Slot 2)", LwjglGlfwKeycode.GLFW_KEY_2, category = "Angka & Hotbar"),
        KeyOption("3 (Slot 3)", LwjglGlfwKeycode.GLFW_KEY_3, category = "Angka & Hotbar"),
        KeyOption("4 (Slot 4)", LwjglGlfwKeycode.GLFW_KEY_4, category = "Angka & Hotbar"),
        KeyOption("5 (Slot 5)", LwjglGlfwKeycode.GLFW_KEY_5, category = "Angka & Hotbar"),
        KeyOption("6 (Slot 6)", LwjglGlfwKeycode.GLFW_KEY_6, category = "Angka & Hotbar"),
        KeyOption("7 (Slot 7)", LwjglGlfwKeycode.GLFW_KEY_7, category = "Angka & Hotbar"),
        KeyOption("8 (Slot 8)", LwjglGlfwKeycode.GLFW_KEY_8, category = "Angka & Hotbar"),
        KeyOption("9 (Slot 9)", LwjglGlfwKeycode.GLFW_KEY_9, category = "Angka & Hotbar"),
        KeyOption("0", LwjglGlfwKeycode.GLFW_KEY_0, category = "Angka & Hotbar"),

        // Navigasi
        KeyOption("Panah Atas (UP)", LwjglGlfwKeycode.GLFW_KEY_UP, category = "Navigasi"),
        KeyOption("Panah Bawah (DOWN)", LwjglGlfwKeycode.GLFW_KEY_DOWN, category = "Navigasi"),
        KeyOption("Panah Kiri (LEFT)", LwjglGlfwKeycode.GLFW_KEY_LEFT, category = "Navigasi"),
        KeyOption("Panah Kanan (RIGHT)", LwjglGlfwKeycode.GLFW_KEY_RIGHT, category = "Navigasi"),
        KeyOption("INSERT", LwjglGlfwKeycode.GLFW_KEY_INSERT, category = "Navigasi"),
        KeyOption("DELETE", LwjglGlfwKeycode.GLFW_KEY_DELETE, category = "Navigasi"),
        KeyOption("HOME", LwjglGlfwKeycode.GLFW_KEY_HOME, category = "Navigasi"),
        KeyOption("END", LwjglGlfwKeycode.GLFW_KEY_END, category = "Navigasi"),
        KeyOption("PAGE UP", LwjglGlfwKeycode.GLFW_KEY_PAGE_UP, category = "Navigasi"),
        KeyOption("PAGE DOWN", LwjglGlfwKeycode.GLFW_KEY_PAGE_DOWN, category = "Navigasi"),

        // Simbol & Tanda Baca
        KeyOption("GRAVE ( ` ~ )", LwjglGlfwKeycode.GLFW_KEY_GRAVE_ACCENT, category = "Simbol"),
        KeyOption("MINUS ( - _ )", LwjglGlfwKeycode.GLFW_KEY_MINUS, category = "Simbol"),
        KeyOption("EQUAL ( = + )", LwjglGlfwKeycode.GLFW_KEY_EQUAL, category = "Simbol"),
        KeyOption("KURUNG BUKA ( [ { )", LwjglGlfwKeycode.GLFW_KEY_LEFT_BRACKET, category = "Simbol"),
        KeyOption("KURUNG TUTUP ( ] } )", LwjglGlfwKeycode.GLFW_KEY_RIGHT_BRACKET, category = "Simbol"),
        KeyOption("BACKSLASH ( \\ | )", LwjglGlfwKeycode.GLFW_KEY_BACKSLASH, category = "Simbol"),
        KeyOption("SEMICOLON ( ; : )", LwjglGlfwKeycode.GLFW_KEY_SEMICOLON, category = "Simbol"),
        KeyOption("APOSTROPHE ( ' \" )", LwjglGlfwKeycode.GLFW_KEY_APOSTROPHE, category = "Simbol"),
        KeyOption("COMMA ( , < )", LwjglGlfwKeycode.GLFW_KEY_COMMA, category = "Simbol"),
        KeyOption("PERIOD ( . > )", LwjglGlfwKeycode.GLFW_KEY_PERIOD, category = "Simbol"),
        KeyOption("SLASH ( / ? )", LwjglGlfwKeycode.GLFW_KEY_SLASH, category = "Simbol"),

        // Numpad (Keypad)
        KeyOption("Numpad 0", LwjglGlfwKeycode.GLFW_KEY_KP_0, category = "Numpad"),
        KeyOption("Numpad 1", LwjglGlfwKeycode.GLFW_KEY_KP_1, category = "Numpad"),
        KeyOption("Numpad 2", LwjglGlfwKeycode.GLFW_KEY_KP_2, category = "Numpad"),
        KeyOption("Numpad 3", LwjglGlfwKeycode.GLFW_KEY_KP_3, category = "Numpad"),
        KeyOption("Numpad 4", LwjglGlfwKeycode.GLFW_KEY_KP_4, category = "Numpad"),
        KeyOption("Numpad 5", LwjglGlfwKeycode.GLFW_KEY_KP_5, category = "Numpad"),
        KeyOption("Numpad 6", LwjglGlfwKeycode.GLFW_KEY_KP_6, category = "Numpad"),
        KeyOption("Numpad 7", LwjglGlfwKeycode.GLFW_KEY_KP_7, category = "Numpad"),
        KeyOption("Numpad 8", LwjglGlfwKeycode.GLFW_KEY_KP_8, category = "Numpad"),
        KeyOption("Numpad 9", LwjglGlfwKeycode.GLFW_KEY_KP_9, category = "Numpad"),
        KeyOption("Numpad . (Titik)", LwjglGlfwKeycode.GLFW_KEY_KP_DECIMAL, category = "Numpad"),
        KeyOption("Numpad / (Bagi)", LwjglGlfwKeycode.GLFW_KEY_KP_DIVIDE, category = "Numpad"),
        KeyOption("Numpad * (Kali)", LwjglGlfwKeycode.GLFW_KEY_KP_MULTIPLY, category = "Numpad"),
        KeyOption("Numpad - (Kurang)", LwjglGlfwKeycode.GLFW_KEY_KP_SUBTRACT, category = "Numpad"),
        KeyOption("Numpad + (Tambah)", LwjglGlfwKeycode.GLFW_KEY_KP_ADD, category = "Numpad"),
        KeyOption("Numpad ENTER", LwjglGlfwKeycode.GLFW_KEY_KP_ENTER, category = "Numpad"),
        KeyOption("Numpad = (Sama Dengan)", LwjglGlfwKeycode.GLFW_KEY_KP_EQUAL, category = "Numpad")
    )

    fun getKeyName(keyCode: Int): String {
        return ALL_KEYS.firstOrNull { !it.isMouseButton && !it.isScroll && it.keyCode == keyCode }?.displayName
            ?: when (keyCode) {
                0 -> "None"
                else -> "Key $keyCode"
            }
    }
}
