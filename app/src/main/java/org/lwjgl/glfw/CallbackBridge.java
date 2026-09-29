package org.lwjgl.glfw;

import static com.movtery.zalithlauncher.bridge.ZLBridgeStatesKt.CURSOR_DISABLED;
import static com.movtery.zalithlauncher.bridge.ZLBridgeStatesKt.CURSOR_ENABLED;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Choreographer;

import androidx.annotation.Keep;

import com.movtery.inputmap.keycodes.LwjglGlfwKeycode;
import com.movtery.zalithlauncher.bridge.CursorShape;
import com.movtery.zalithlauncher.bridge.LoggerBridge;
import com.movtery.zalithlauncher.bridge.NativeLibraryLoader;
import com.movtery.zalithlauncher.bridge.ZLBridgeStates;

import android.view.KeyEvent;
import android.view.MotionEvent;
import com.israadev.nuxlauncher.core.game.input.EfficientAndroidLWJGLKeycode;
import com.movtery.zalithlauncher.game.sdl.SdlBridge;
import org.libsdl.app.SDLActivity;
import org.libsdl.app.SDLSurface;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.function.Consumer;

import dalvik.annotation.optimization.CriticalNative;

@Keep
public class CallbackBridge {
    private static final int GLFW_IBEAM_CURSOR = 0x36002;
    private static final int GLFW_HAND_CURSOR = 0x36004;
    private static final int GLFW_CROSSHAIR_CURSOR = 0x36003;
    private static final int GLFW_RESIZE_NS_CURSOR = 0x36006;
    private static final int GLFW_RESIZE_EW_CURSOR = 0x36005;
    private static final int GLFW_RESIZE_ALL_CURSOR = 0x36009;
    private static final int GLFW_NOT_ALLOWED_CURSOR = 0x3600A;
    private static final int GLFW_ARROW_CURSOR = 0x36001;

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static volatile boolean isGrabbing = false;
    private static final Consumer<Boolean> grabListener = isGrabbing ->
            ZLBridgeStates.changeCursorMode(isGrabbing ? CURSOR_DISABLED : CURSOR_ENABLED);

    private static int cursorShape = GLFW_ARROW_CURSOR;
    private static final Consumer<CursorShape> cursorShapeListener = ZLBridgeStates::changeCursorShape;

    public static final int CLIPBOARD_COPY = 2000;
    public static final int CLIPBOARD_PASTE = 2001;
    public static final int CLIPBOARD_OPEN = 2002;

    public static final int NOTIF_TYPE_SDL = 0;
    public static final int ACTION_INIT_LAUNCHER_INTEGRATION = 0;
    public static final int ACTION_SEND_TEXTBOX_RECT = 1;

    public static final int SDL = NOTIF_TYPE_SDL;
    public static final int INIT = ACTION_INIT_LAUNCHER_INTEGRATION;

    public static boolean sGamepadDirectInput = false;

    public static float mouseX = 0f;
    public static float mouseY = 0f;
    public static float deltaX = 0f;
    public static float deltaY = 0f;
    public static int windowWidth = 1280;
    public static int windowHeight = 720;

    public static final ByteBuffer sGamepadButtonBuffer;
    public static final FloatBuffer sGamepadAxisBuffer;

    private static GraphicOutputListener sGraphicOutputListener;

    public static void setGraphicOutputListener(GraphicOutputListener listener) {
        sGraphicOutputListener = listener;
    }

    public static Context sContext = null;

    private static void postFrameCallbackDelayed(Choreographer.FrameCallback callback, long delayMillis) {
        MAIN_HANDLER.post(() -> Choreographer.getInstance().postFrameCallbackDelayed(callback, delayMillis));
    }

    @Keep
    public static boolean notifyLauncher(int type, int... action) {
        if (action == null || action.length == 0) {
            LoggerBridge.append("NuxLauncher: SDL notification has no action");
            return false;
        }
        switch (type) {
            case NOTIF_TYPE_SDL:
                if (action[0] == ACTION_INIT_LAUNCHER_INTEGRATION) {
                    if (!SdlBridge.markSdlInitialized()) {
                        return true;
                    }
                    try {
                        LoggerBridge.append("NuxLauncher: loading SDL3");
                        System.loadLibrary("SDL3");
                        LoggerBridge.append("NuxLauncher: loading SDL2");
                        System.loadLibrary("SDL2");
                        LoggerBridge.append("NuxLauncher: setting up SDL JNI");
                        SdlBridge.setupJNI();
                        LoggerBridge.append("NuxLauncher: binding SDL surface");
                        SdlBridge.setSdlEnabled(true);
                        SDLSurface surface = SDLActivity.getSDLSurface();
                        if (surface != null) {
                            surface.surfaceChanged();
                            if (windowWidth > 0 && windowHeight > 0) {
                                surface.nativeResize(windowWidth, windowHeight);
                            }
                        }
                        LoggerBridge.append("NuxLauncher: SDL support enabled!");
                        return true;
                    } catch (Throwable e) {
                        SdlBridge.setSdlEnabled(false);
                        SdlBridge.clearSdlInitialized();
                        StringWriter trace = new StringWriter();
                        e.printStackTrace(new PrintWriter(trace));
                        LoggerBridge.append("NuxLauncher: SDL launcher integration is unavailable:\n" + trace);
                    }
                }
                break;
        }
        return false;
    }

    @Keep
    public static void nativeNotifyLauncher(int type, int... action) {
        notifyLauncher(type, action);
    }

    public static void clearSdlBridgeState() {
        sGamepadDirectInput = false;
    }

    @Keep
    public static String accessAndroidClipboard(int type, String copyContent) {
        if (sContext == null) return "";
        try {
            ClipboardManager clipboard = (ClipboardManager) sContext.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) return "";
            switch (type) {
                case CLIPBOARD_COPY:
                    ClipData clip = ClipData.newPlainText("NUX_CLIP", copyContent);
                    clipboard.setPrimaryClip(clip);
                    return copyContent;
                case CLIPBOARD_PASTE:
                    if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClipDescription() != null && clipboard.getPrimaryClipDescription().hasMimeType(android.content.ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                        CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
                        return text != null ? text.toString() : "";
                    }
                    return "";
            }
        } catch (Throwable e) {
            Log.e("CallbackBridge", "accessAndroidClipboard error: " + e.getMessage());
        }
        return "";
    }

    @Keep
    public static void onGrabStateChanged(final boolean grabbing) {
        Log.i("NUX_INPUT", "Grab callback received: " + grabbing);
        isGrabbing = grabbing;
        deltaX = 0f;
        deltaY = 0f;
        postFrameCallbackDelayed((time) -> {
            if (isGrabbing != grabbing) return;
            Log.i("NUX_INPUT", "Grab changed : " + grabbing);
            synchronized (grabListener) {
                grabListener.accept(isGrabbing);
            }
        }, 16);
    }

    @Keep
    public static void onCursorShapeChanged(final int shape) {
        cursorShape = shape;
        postFrameCallbackDelayed((time) -> {
            if (cursorShape != shape) return;
            synchronized (cursorShapeListener) {
                CursorShape shape1;
                switch (cursorShape) {
                    case GLFW_IBEAM_CURSOR:
                        shape1 = CursorShape.IBeam;
                        break;
                    case GLFW_HAND_CURSOR:
                        shape1 = CursorShape.Hand;
                        break;
                    case GLFW_CROSSHAIR_CURSOR:
                        shape1 = CursorShape.CrossHair;
                        break;
                    case GLFW_RESIZE_NS_CURSOR:
                        shape1 = CursorShape.ResizeNS;
                        break;
                    case GLFW_RESIZE_EW_CURSOR:
                        shape1 = CursorShape.ResizeEW;
                        break;
                    case GLFW_RESIZE_ALL_CURSOR:
                        shape1 = CursorShape.ResizeAll;
                        break;
                    case GLFW_NOT_ALLOWED_CURSOR:
                        shape1 = CursorShape.NotAllowed;
                        break;
                    default:
                        shape1 = CursorShape.Arrow;
                }
                cursorShapeListener.accept(shape1);
            }
        }, 16);
    }

    @Keep
    public static void onGraphicOutput() {
        if (sGraphicOutputListener != null) {
            sGraphicOutputListener.onGraphicOutput();
        }
    }

    @Keep
    public static void onDirectInputEnable() {
        Log.i("NUX_INPUT", "Direct input enabled");
    }

    public static void sendCursorPos(float x, float y) {
        mouseX = x;
        mouseY = y;
        nativeSendCursorPos(mouseX, mouseY);
        if (!SdlBridge.getSdlEnabled()) return;
        SDLActivity.onNativeMouse(0, MotionEvent.ACTION_MOVE, x, y, false);
    }

    public static void sendCursorDelta(float dx, float dy) {
        deltaX = dx;
        deltaY = dy;
        mouseX += dx;
        mouseY += dy;
        nativeSendCursorPos(mouseX, mouseY);
        if (!SdlBridge.getSdlEnabled()) return;
        SDLActivity.onNativeMouse(0, MotionEvent.ACTION_MOVE, dx, dy, true);
    }

    public static void sendUpdateWindowSize(int w, int h) {
        windowWidth = w;
        windowHeight = h;
        try {
            nativeSendScreenSize(w, h);
        } catch (Throwable ignored) {}
    }

    public static volatile boolean holdingAlt = false;
    public static volatile boolean holdingCapslock = false;
    public static volatile boolean holdingCtrl = false;
    public static volatile boolean holdingNumlock = false;
    public static volatile boolean holdingShift = false;

    public static int getCurrentMods() {
        int v = 0;
        if (holdingAlt) v |= LwjglGlfwKeycode.GLFW_MOD_ALT;
        if (holdingCapslock) v |= LwjglGlfwKeycode.GLFW_MOD_CAPS_LOCK;
        if (holdingCtrl) v |= LwjglGlfwKeycode.GLFW_MOD_CONTROL;
        if (holdingNumlock) v |= LwjglGlfwKeycode.GLFW_MOD_NUM_LOCK;
        if (holdingShift) v |= LwjglGlfwKeycode.GLFW_MOD_SHIFT;
        return v;
    }

    private static int sMouseButtonState = 0;

    public static void sendMouseButton(int button, boolean status) {
        sendMouseKeycode(button, getCurrentMods(), status);
    }

    public static void putMouseEvent(int button, boolean isDown) {
        sendMouseKeycode(button, getCurrentMods(), isDown);
    }

    public static void sendMouseKeycode(int button, int modifiers, boolean isDown) {
        nativeSendMouseButton(button, isDown ? 1 : 0, modifiers);
        if (!SdlBridge.getSdlEnabled()) return;
        int aKey = -1;
        switch (button) {
            case LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT:
                aKey = MotionEvent.BUTTON_PRIMARY;
                break;
            case LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT:
                aKey = MotionEvent.BUTTON_SECONDARY;
                break;
            case LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE:
                aKey = MotionEvent.BUTTON_TERTIARY;
                break;
            case LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_5:
                aKey = MotionEvent.BUTTON_BACK;
                break;
            case LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_4:
                aKey = MotionEvent.BUTTON_FORWARD;
                break;
        }
        if (aKey != -1) {
            if (isDown) {
                sMouseButtonState |= aKey;
            } else {
                sMouseButtonState &= ~aKey;
            }
            SDLActivity.onNativeMouse(sMouseButtonState, isDown ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP, mouseX, mouseY, false);
        }
    }

    public static void sendMouseKeycode(int keycode) {
        sendMouseKeycode(keycode, getCurrentMods(), true);
        sendMouseKeycode(keycode, getCurrentMods(), false);
    }

    public static void putMouseEvent(int button) {
        putMouseEvent(button, true);
        postFrameCallbackDelayed(l -> putMouseEvent(button, false), 33);
    }

    public static void putMouseEventWithCoords(int button, float x, float y) {
        sendCursorPos(x, y);
        putMouseEvent(button);
    }

    public static void sendKeycode(int keycode, char keychar, int scancode, int modifiers, boolean isDown) {
        if (keycode > LwjglGlfwKeycode.GLFW_KEY_UNKNOWN && keycode <= LwjglGlfwKeycode.GLFW_KEY_LAST) {
            nativeSendKey(keycode, scancode, isDown ? 1 : 0, modifiers);
        }
        if (isDown && !Character.isISOControl(keychar)) {
            nativeSendCharMods(keychar, modifiers);
            nativeSendChar(keychar);
        }
        if (!SdlBridge.getSdlEnabled()) return;
        int androidKeycode = EfficientAndroidLWJGLKeycode.getSdlAndroidKeycode(keycode);
        if (androidKeycode == KeyEvent.KEYCODE_UNKNOWN) return;
        try {
            if (isDown) {
                SDLActivity.onNativeKeyDown(androidKeycode);
                if (isTextEventChar(keychar, modifiers) && SDLActivity.isSDLTextInputActive()) {
                    SDLActivity.onNativeTextInput(String.valueOf(keychar));
                }
            } else {
                SDLActivity.onNativeKeyUp(androidKeycode);
            }
        } catch (Throwable ignored) {}
    }

    private static boolean isTextEventChar(char keychar, int modifiers) {
        if (Character.isISOControl(keychar)) return false;
        return (modifiers & LwjglGlfwKeycode.GLFW_MOD_CONTROL) == 0;
    }

    public static void sendKeyPress(int keyCode, int modifiers, boolean status) {
        sendKeyPress(keyCode, 0, modifiers, status);
    }

    public static void sendKeyPress(int keyCode, int scancode, int modifiers, boolean status) {
        sendKeyPress(keyCode, '\u0000', scancode, modifiers, status);
    }

    public static void sendKeyPress(int keyCode, char keyChar, int scancode, int modifiers, boolean status) {
        sendKeycode(keyCode, keyChar, scancode, modifiers, status);
    }

    public static void sendKeyPress(int keyCode, boolean isDown) {
        sendKeycode(keyCode, '\u0000', 0, getCurrentMods(), isDown);
    }

    public static void sendKeyPress(int keyCode) {
        sendKeyPress(keyCode, getCurrentMods(), true);
        MAIN_HANDLER.postDelayed(() -> sendKeyPress(keyCode, getCurrentMods(), false), 33);
    }

    public static void sendChar(char keychar, int modifiers) {
        nativeSendCharMods(keychar, modifiers);
        nativeSendChar(keychar);
        if (!SdlBridge.getSdlEnabled()) return;
        try {
            int code = EfficientAndroidLWJGLKeycode.getAndroidKeycode(keychar);
            SDLActivity.onNativeKeyDown(code);
            SDLActivity.onNativeKeyUp(code);
        } catch (Throwable ignored) {}
    }

    public static void sendChar(char keychar) {
        sendChar(keychar, 0);
    }

    public static void sendScroll(double xoffset, double yoffset) {
        nativeSendScroll(xoffset, yoffset);
        if (!SdlBridge.getSdlEnabled()) return;
        SDLActivity.onNativeMouse(0, MotionEvent.ACTION_SCROLL, (float) xoffset, (float) yoffset, false);
    }

    public static void resetInputState() {
        nativeResetInputState();
        if (SdlBridge.getSdlEnabled() && sMouseButtonState != 0) {
            SDLActivity.onNativeMouse(0, MotionEvent.ACTION_UP, mouseX, mouseY, false);
        }
        deltaX = 0f;
        deltaY = 0f;
        sMouseButtonState = 0;
        holdingAlt = false;
        holdingCapslock = false;
        holdingCtrl = false;
        holdingNumlock = false;
        holdingShift = false;
    }

    // Native functions registered by libpojavexec.so
    @Keep @CriticalNative public static native void nativeSetUseInputStackQueue(boolean useInputStackQueue);
    @Keep @CriticalNative private static native boolean nativeSendChar(char codepoint);
    @Keep @CriticalNative private static native boolean nativeSendCharMods(char codepoint, int mods);
    @Keep @CriticalNative private static native void nativeSendKey(int key, int scancode, int action, int mods);
    @Keep @CriticalNative private static native void nativeSendCursorPos(float x, float y);
    @Keep @CriticalNative private static native void nativeSendMouseButton(int button, int action, int mods);
    @Keep @CriticalNative private static native void nativeResetInputState();
    @Keep @CriticalNative private static native void nativeSendScroll(double xoffset, double yoffset);
    @Keep @CriticalNative public static native void nativeSendScreenSize(int width, int height);
    @Keep public static native void nativeSetWindowAttrib(int attrib, int value);
    @Keep public static native void nativeSetGrabbing(boolean grab);
    @Keep public static native int getCurrentFps();

    private static native ByteBuffer nativeCreateGamepadButtonBuffer();
    private static native ByteBuffer nativeCreateGamepadAxisBuffer();

    static {
        NativeLibraryLoader.loadPojavLib();
        sGamepadButtonBuffer = nativeCreateGamepadButtonBuffer();
        sGamepadAxisBuffer = nativeCreateGamepadAxisBuffer() != null 
            ? nativeCreateGamepadAxisBuffer().order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
            : null;
    }
}
