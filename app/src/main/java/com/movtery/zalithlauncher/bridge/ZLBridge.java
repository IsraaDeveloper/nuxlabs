package com.movtery.zalithlauncher.bridge;

import android.content.Context;
import androidx.annotation.Keep;

@Keep
public final class ZLBridge {
    // AWT event types
    public static final int EVENT_TYPE_CHAR = 1000;
    public static final int EVENT_TYPE_CURSOR_POS = 1003;
    public static final int EVENT_TYPE_KEY = 1005;
    public static final int EVENT_TYPE_MOUSE_BUTTON = 1006;

    public static void sendKey(char keychar, int keycode) {
        sendInputData(EVENT_TYPE_KEY, (int) keychar, keycode, 1, 0);
        sendInputData(EVENT_TYPE_KEY, (int) keychar, keycode, 0, 0);
    }

    public static void sendKey(char keychar, int keycode, int state) {
        sendInputData(EVENT_TYPE_KEY, (int) keychar, keycode, state, 0);
    }

    public static void sendChar(char keychar) {
        sendInputData(EVENT_TYPE_CHAR, (int) keychar, 0, 0, 0);
    }

    public static void sendMousePress(int awtButtons, boolean isDown) {
        sendInputData(EVENT_TYPE_MOUSE_BUTTON, awtButtons, isDown ? 1 : 0, 0, 0);
    }

    public static void sendMousePress(int awtButtons) {
        sendMousePress(awtButtons, true);
        sendMousePress(awtButtons, false);
    }

    public static void sendMousePos(int x, int y) {
        sendInputData(EVENT_TYPE_CURSOR_POS, x, y, 0, 0);
    }

    // Game Lifecycle Hooks
    @Keep public static native void initializeGameExitHook();
    @Keep public static native void setupExitMethod(Context context);

    // Dynamic Linking & Library Paths
    @Keep public static native void setLdLibraryPath(String ldLibraryPath);
    @Keep public static native boolean dlopen(String libPath);

    // Surface & Window Rendering
    @Keep public static native void setupBridgeWindow(Object surface);
    @Keep public static native void releaseBridgeWindow();
    @Keep public static native void moveWindow(int xOffset, int yOffset);
    @Keep public static native int[] renderAWTScreenFrame();

    // Input & Clipboard
    @Keep public static native void sendInputData(int type, int i1, int i2, int i3, int i4);
    @Keep public static native void clipboardReceived(String data, String mimeTypeSub);

    // Directory navigation
    @Keep public static native int chdir(String path);

    static {
        NativeLibraryLoader.loadExitHookLib();
        NativeLibraryLoader.loadPojavLib();
        NativeLibraryLoader.loadPojavAWTLib();
    }
}
