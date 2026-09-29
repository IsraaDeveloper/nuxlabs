package com.israadev.nuxlauncher.core.game.input;

import android.view.KeyEvent;
import com.movtery.inputmap.keycodes.LwjglGlfwKeycode;
import org.lwjgl.glfw.CallbackBridge;

import java.util.Arrays;

/**
 * High-performance binary search keycode mapping from Android KeyEvent keycodes to GLFW/LWJGL keycodes.
 */
public class EfficientAndroidLWJGLKeycode {

    private static final int KEYCODE_COUNT = 112;
    private static final int[] sAndroidKeycodes = new int[KEYCODE_COUNT];
    private static final short[] sLwjglKeycodes = new short[KEYCODE_COUNT];
    private static int mTmpCount = 0;

    static {
        /*  BINARY SEARCH IS PERFORMED ON THE sAndroidKeycodes ARRAY !
            WHEN ADDING A MAPPING, ADD IT SO THE sAndroidKeycodes ARRAY STAYS SORTED ASCENDING ! */
        add(KeyEvent.KEYCODE_UNKNOWN, (short) LwjglGlfwKeycode.GLFW_KEY_UNKNOWN);
        add(KeyEvent.KEYCODE_HOME, (short) LwjglGlfwKeycode.GLFW_KEY_HOME);
        add(KeyEvent.KEYCODE_BACK, (short) LwjglGlfwKeycode.GLFW_KEY_ESCAPE);

        // 0-9 keys (7 to 16)
        add(KeyEvent.KEYCODE_0, (short) LwjglGlfwKeycode.GLFW_KEY_0);
        add(KeyEvent.KEYCODE_1, (short) LwjglGlfwKeycode.GLFW_KEY_1);
        add(KeyEvent.KEYCODE_2, (short) LwjglGlfwKeycode.GLFW_KEY_2);
        add(KeyEvent.KEYCODE_3, (short) LwjglGlfwKeycode.GLFW_KEY_3);
        add(KeyEvent.KEYCODE_4, (short) LwjglGlfwKeycode.GLFW_KEY_4);
        add(KeyEvent.KEYCODE_5, (short) LwjglGlfwKeycode.GLFW_KEY_5);
        add(KeyEvent.KEYCODE_6, (short) LwjglGlfwKeycode.GLFW_KEY_6);
        add(KeyEvent.KEYCODE_7, (short) LwjglGlfwKeycode.GLFW_KEY_7);
        add(KeyEvent.KEYCODE_8, (short) LwjglGlfwKeycode.GLFW_KEY_8);
        add(KeyEvent.KEYCODE_9, (short) LwjglGlfwKeycode.GLFW_KEY_9);

        add(KeyEvent.KEYCODE_POUND, (short) LwjglGlfwKeycode.GLFW_KEY_3);

        // Arrow keys (19 to 22)
        add(KeyEvent.KEYCODE_DPAD_UP, (short) LwjglGlfwKeycode.GLFW_KEY_UP);
        add(KeyEvent.KEYCODE_DPAD_DOWN, (short) LwjglGlfwKeycode.GLFW_KEY_DOWN);
        add(KeyEvent.KEYCODE_DPAD_LEFT, (short) LwjglGlfwKeycode.GLFW_KEY_LEFT);
        add(KeyEvent.KEYCODE_DPAD_RIGHT, (short) LwjglGlfwKeycode.GLFW_KEY_RIGHT);

        // A-Z keys (29 to 54)
        add(KeyEvent.KEYCODE_A, (short) LwjglGlfwKeycode.GLFW_KEY_A);
        add(KeyEvent.KEYCODE_B, (short) LwjglGlfwKeycode.GLFW_KEY_B);
        add(KeyEvent.KEYCODE_C, (short) LwjglGlfwKeycode.GLFW_KEY_C);
        add(KeyEvent.KEYCODE_D, (short) LwjglGlfwKeycode.GLFW_KEY_D);
        add(KeyEvent.KEYCODE_E, (short) LwjglGlfwKeycode.GLFW_KEY_E);
        add(KeyEvent.KEYCODE_F, (short) LwjglGlfwKeycode.GLFW_KEY_F);
        add(KeyEvent.KEYCODE_G, (short) LwjglGlfwKeycode.GLFW_KEY_G);
        add(KeyEvent.KEYCODE_H, (short) LwjglGlfwKeycode.GLFW_KEY_H);
        add(KeyEvent.KEYCODE_I, (short) LwjglGlfwKeycode.GLFW_KEY_I);
        add(KeyEvent.KEYCODE_J, (short) LwjglGlfwKeycode.GLFW_KEY_J);
        add(KeyEvent.KEYCODE_K, (short) LwjglGlfwKeycode.GLFW_KEY_K);
        add(KeyEvent.KEYCODE_L, (short) LwjglGlfwKeycode.GLFW_KEY_L);
        add(KeyEvent.KEYCODE_M, (short) LwjglGlfwKeycode.GLFW_KEY_M);
        add(KeyEvent.KEYCODE_N, (short) LwjglGlfwKeycode.GLFW_KEY_N);
        add(KeyEvent.KEYCODE_O, (short) LwjglGlfwKeycode.GLFW_KEY_O);
        add(KeyEvent.KEYCODE_P, (short) LwjglGlfwKeycode.GLFW_KEY_P);
        add(KeyEvent.KEYCODE_Q, (short) LwjglGlfwKeycode.GLFW_KEY_Q);
        add(KeyEvent.KEYCODE_R, (short) LwjglGlfwKeycode.GLFW_KEY_R);
        add(KeyEvent.KEYCODE_S, (short) LwjglGlfwKeycode.GLFW_KEY_S);
        add(KeyEvent.KEYCODE_T, (short) LwjglGlfwKeycode.GLFW_KEY_T);
        add(KeyEvent.KEYCODE_U, (short) LwjglGlfwKeycode.GLFW_KEY_U);
        add(KeyEvent.KEYCODE_V, (short) LwjglGlfwKeycode.GLFW_KEY_V);
        add(KeyEvent.KEYCODE_W, (short) LwjglGlfwKeycode.GLFW_KEY_W);
        add(KeyEvent.KEYCODE_X, (short) LwjglGlfwKeycode.GLFW_KEY_X);
        add(KeyEvent.KEYCODE_Y, (short) LwjglGlfwKeycode.GLFW_KEY_Y);
        add(KeyEvent.KEYCODE_Z, (short) LwjglGlfwKeycode.GLFW_KEY_Z);

        add(KeyEvent.KEYCODE_COMMA, (short) LwjglGlfwKeycode.GLFW_KEY_COMMA);
        add(KeyEvent.KEYCODE_PERIOD, (short) LwjglGlfwKeycode.GLFW_KEY_PERIOD);

        // Alt keys
        add(KeyEvent.KEYCODE_ALT_LEFT, (short) LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT);
        add(KeyEvent.KEYCODE_ALT_RIGHT, (short) LwjglGlfwKeycode.GLFW_KEY_RIGHT_ALT);

        // Shift keys
        add(KeyEvent.KEYCODE_SHIFT_LEFT, (short) LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT);
        add(KeyEvent.KEYCODE_SHIFT_RIGHT, (short) LwjglGlfwKeycode.GLFW_KEY_RIGHT_SHIFT);

        add(KeyEvent.KEYCODE_TAB, (short) LwjglGlfwKeycode.GLFW_KEY_TAB);
        add(KeyEvent.KEYCODE_SPACE, (short) LwjglGlfwKeycode.GLFW_KEY_SPACE);
        add(KeyEvent.KEYCODE_ENTER, (short) LwjglGlfwKeycode.GLFW_KEY_ENTER);
        add(KeyEvent.KEYCODE_DEL, (short) LwjglGlfwKeycode.GLFW_KEY_BACKSPACE);
        add(KeyEvent.KEYCODE_GRAVE, (short) LwjglGlfwKeycode.GLFW_KEY_GRAVE_ACCENT);
        add(KeyEvent.KEYCODE_MINUS, (short) LwjglGlfwKeycode.GLFW_KEY_MINUS);
        add(KeyEvent.KEYCODE_EQUALS, (short) LwjglGlfwKeycode.GLFW_KEY_EQUAL);
        add(KeyEvent.KEYCODE_LEFT_BRACKET, (short) LwjglGlfwKeycode.GLFW_KEY_LEFT_BRACKET);
        add(KeyEvent.KEYCODE_RIGHT_BRACKET, (short) LwjglGlfwKeycode.GLFW_KEY_RIGHT_BRACKET);
        add(KeyEvent.KEYCODE_BACKSLASH, (short) LwjglGlfwKeycode.GLFW_KEY_BACKSLASH);
        add(KeyEvent.KEYCODE_SEMICOLON, (short) LwjglGlfwKeycode.GLFW_KEY_SEMICOLON);
        add(KeyEvent.KEYCODE_APOSTROPHE, (short) LwjglGlfwKeycode.GLFW_KEY_APOSTROPHE);
        add(KeyEvent.KEYCODE_SLASH, (short) LwjglGlfwKeycode.GLFW_KEY_SLASH);
        add(KeyEvent.KEYCODE_AT, (short) LwjglGlfwKeycode.GLFW_KEY_2);
        add(KeyEvent.KEYCODE_PLUS, (short) LwjglGlfwKeycode.GLFW_KEY_KP_ADD);
        add(KeyEvent.KEYCODE_MENU, (short) LwjglGlfwKeycode.GLFW_KEY_MENU);

        // Page keys
        add(KeyEvent.KEYCODE_PAGE_UP, (short) LwjglGlfwKeycode.GLFW_KEY_PAGE_UP);
        add(KeyEvent.KEYCODE_PAGE_DOWN, (short) LwjglGlfwKeycode.GLFW_KEY_PAGE_DOWN);

        add(KeyEvent.KEYCODE_ESCAPE, (short) LwjglGlfwKeycode.GLFW_KEY_ESCAPE);
        add(KeyEvent.KEYCODE_FORWARD_DEL, (short) LwjglGlfwKeycode.GLFW_KEY_DELETE);

        // Control keys
        add(KeyEvent.KEYCODE_CTRL_LEFT, (short) LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL);
        add(KeyEvent.KEYCODE_CTRL_RIGHT, (short) LwjglGlfwKeycode.GLFW_KEY_RIGHT_CONTROL);

        add(KeyEvent.KEYCODE_CAPS_LOCK, (short) LwjglGlfwKeycode.GLFW_KEY_CAPS_LOCK);
        add(KeyEvent.KEYCODE_SCROLL_LOCK, (short) LwjglGlfwKeycode.GLFW_KEY_SCROLL_LOCK);
        add(KeyEvent.KEYCODE_META_LEFT, (short) LwjglGlfwKeycode.GLFW_KEY_LEFT_SUPER);
        add(KeyEvent.KEYCODE_META_RIGHT, (short) LwjglGlfwKeycode.GLFW_KEY_RIGHT_SUPER);
        add(KeyEvent.KEYCODE_SYSRQ, (short) LwjglGlfwKeycode.GLFW_KEY_PRINT_SCREEN);
        add(KeyEvent.KEYCODE_BREAK, (short) LwjglGlfwKeycode.GLFW_KEY_PAUSE);
        add(KeyEvent.KEYCODE_MOVE_HOME, (short) LwjglGlfwKeycode.GLFW_KEY_HOME);
        add(KeyEvent.KEYCODE_MOVE_END, (short) LwjglGlfwKeycode.GLFW_KEY_END);
        add(KeyEvent.KEYCODE_INSERT, (short) LwjglGlfwKeycode.GLFW_KEY_INSERT);

        // Fn keys
        add(KeyEvent.KEYCODE_F1, (short) LwjglGlfwKeycode.GLFW_KEY_F1);
        add(KeyEvent.KEYCODE_F2, (short) LwjglGlfwKeycode.GLFW_KEY_F2);
        add(KeyEvent.KEYCODE_F3, (short) LwjglGlfwKeycode.GLFW_KEY_F3);
        add(KeyEvent.KEYCODE_F4, (short) LwjglGlfwKeycode.GLFW_KEY_F4);
        add(KeyEvent.KEYCODE_F5, (short) LwjglGlfwKeycode.GLFW_KEY_F5);
        add(KeyEvent.KEYCODE_F6, (short) LwjglGlfwKeycode.GLFW_KEY_F6);
        add(KeyEvent.KEYCODE_F7, (short) LwjglGlfwKeycode.GLFW_KEY_F7);
        add(KeyEvent.KEYCODE_F8, (short) LwjglGlfwKeycode.GLFW_KEY_F8);
        add(KeyEvent.KEYCODE_F9, (short) LwjglGlfwKeycode.GLFW_KEY_F9);
        add(KeyEvent.KEYCODE_F10, (short) LwjglGlfwKeycode.GLFW_KEY_F10);
        add(KeyEvent.KEYCODE_F11, (short) LwjglGlfwKeycode.GLFW_KEY_F11);
        add(KeyEvent.KEYCODE_F12, (short) LwjglGlfwKeycode.GLFW_KEY_F12);

        // Num keys
        add(KeyEvent.KEYCODE_NUM_LOCK, (short) LwjglGlfwKeycode.GLFW_KEY_NUM_LOCK);
        add(KeyEvent.KEYCODE_NUMPAD_0, (short) LwjglGlfwKeycode.GLFW_KEY_KP_0);
        add(KeyEvent.KEYCODE_NUMPAD_1, (short) LwjglGlfwKeycode.GLFW_KEY_KP_1);
        add(KeyEvent.KEYCODE_NUMPAD_2, (short) LwjglGlfwKeycode.GLFW_KEY_KP_2);
        add(KeyEvent.KEYCODE_NUMPAD_3, (short) LwjglGlfwKeycode.GLFW_KEY_KP_3);
        add(KeyEvent.KEYCODE_NUMPAD_4, (short) LwjglGlfwKeycode.GLFW_KEY_KP_4);
        add(KeyEvent.KEYCODE_NUMPAD_5, (short) LwjglGlfwKeycode.GLFW_KEY_KP_5);
        add(KeyEvent.KEYCODE_NUMPAD_6, (short) LwjglGlfwKeycode.GLFW_KEY_KP_6);
        add(KeyEvent.KEYCODE_NUMPAD_7, (short) LwjglGlfwKeycode.GLFW_KEY_KP_7);
        add(KeyEvent.KEYCODE_NUMPAD_8, (short) LwjglGlfwKeycode.GLFW_KEY_KP_8);
        add(KeyEvent.KEYCODE_NUMPAD_9, (short) LwjglGlfwKeycode.GLFW_KEY_KP_9);
        add(KeyEvent.KEYCODE_NUMPAD_DIVIDE, (short) LwjglGlfwKeycode.GLFW_KEY_KP_DIVIDE);
        add(KeyEvent.KEYCODE_NUMPAD_MULTIPLY, (short) LwjglGlfwKeycode.GLFW_KEY_KP_MULTIPLY);
        add(KeyEvent.KEYCODE_NUMPAD_SUBTRACT, (short) LwjglGlfwKeycode.GLFW_KEY_KP_SUBTRACT);
        add(KeyEvent.KEYCODE_NUMPAD_ADD, (short) LwjglGlfwKeycode.GLFW_KEY_KP_ADD);
        add(KeyEvent.KEYCODE_NUMPAD_DOT, (short) LwjglGlfwKeycode.GLFW_KEY_KP_DECIMAL);
        add(KeyEvent.KEYCODE_NUMPAD_COMMA, (short) LwjglGlfwKeycode.GLFW_KEY_COMMA);
        add(KeyEvent.KEYCODE_NUMPAD_ENTER, (short) LwjglGlfwKeycode.GLFW_KEY_KP_ENTER);
        add(KeyEvent.KEYCODE_NUMPAD_EQUALS, (short) LwjglGlfwKeycode.GLFW_KEY_KP_EQUAL);
    }

    public static boolean containsIndex(int index) {
        return index >= 0;
    }

    public static void execKey(KeyEvent keyEvent, int valueIndex) {
        CallbackBridge.holdingAlt = keyEvent.isAltPressed();
        CallbackBridge.holdingCapslock = keyEvent.isCapsLockOn();
        CallbackBridge.holdingCtrl = keyEvent.isCtrlPressed();
        CallbackBridge.holdingNumlock = keyEvent.isNumLockOn();
        CallbackBridge.holdingShift = keyEvent.isShiftPressed();

        char key = (char) (keyEvent.getUnicodeChar() != 0 ? keyEvent.getUnicodeChar() : '\u0000');
        CallbackBridge.sendKeyPress(
                getValueByIndex(valueIndex),
                key,
                keyEvent.getScanCode(),
                CallbackBridge.getCurrentMods(),
                keyEvent.getAction() == KeyEvent.ACTION_DOWN);
    }

    public static void execKeyIndex(int index) {
        CallbackBridge.sendKeyPress(getValueByIndex(index));
    }

    public static short getValueByIndex(int index) {
        return sLwjglKeycodes[index];
    }

    public static int getIndexByKey(int key) {
        return Arrays.binarySearch(sAndroidKeycodes, key);
    }

    public static int getIndexByValue(int lwjglKey) {
        for (int i = 0; i < sLwjglKeycodes.length; i++) {
            if (sLwjglKeycodes[i] == lwjglKey) return i;
        }
        return -1;
    }

    public static int getAndroidKeycode(int lwjglGlfwKeycode) {
        if (lwjglGlfwKeycode == LwjglGlfwKeycode.GLFW_KEY_2) return KeyEvent.KEYCODE_2;
        if (lwjglGlfwKeycode == LwjglGlfwKeycode.GLFW_KEY_3) return KeyEvent.KEYCODE_3;
        int index = getIndexByValue(lwjglGlfwKeycode);
        return index >= 0 && index < sAndroidKeycodes.length
                ? sAndroidKeycodes[index]
                : KeyEvent.KEYCODE_UNKNOWN;
    }

    public static int getSdlAndroidKeycode(int lwjglGlfwKeycode) {
        switch (lwjglGlfwKeycode) {
            case LwjglGlfwKeycode.GLFW_KEY_ESCAPE: return KeyEvent.KEYCODE_ESCAPE;
            case LwjglGlfwKeycode.GLFW_KEY_HOME: return KeyEvent.KEYCODE_MOVE_HOME;
            case LwjglGlfwKeycode.GLFW_KEY_END: return KeyEvent.KEYCODE_MOVE_END;
            case LwjglGlfwKeycode.GLFW_KEY_KP_ADD: return KeyEvent.KEYCODE_NUMPAD_ADD;
            case LwjglGlfwKeycode.GLFW_KEY_KP_DECIMAL: return KeyEvent.KEYCODE_NUMPAD_DOT;
            case LwjglGlfwKeycode.GLFW_KEY_KP_ENTER: return KeyEvent.KEYCODE_NUMPAD_ENTER;
            case LwjglGlfwKeycode.GLFW_KEY_DELETE: return KeyEvent.KEYCODE_FORWARD_DEL;
            case LwjglGlfwKeycode.GLFW_KEY_KP_EQUAL: return KeyEvent.KEYCODE_NUMPAD_EQUALS;
            case LwjglGlfwKeycode.GLFW_KEY_LEFT_SUPER: return KeyEvent.KEYCODE_META_LEFT;
            case LwjglGlfwKeycode.GLFW_KEY_RIGHT_SUPER: return KeyEvent.KEYCODE_META_RIGHT;
            case LwjglGlfwKeycode.GLFW_KEY_MENU: return KeyEvent.KEYCODE_MENU;
            default: return getAndroidKeycode(lwjglGlfwKeycode);
        }
    }

    private static void add(int androidKeycode, short lwjglKeycode) {
        sAndroidKeycodes[mTmpCount] = androidKeycode;
        sLwjglKeycodes[mTmpCount] = lwjglKeycode;
        mTmpCount++;
    }
}
