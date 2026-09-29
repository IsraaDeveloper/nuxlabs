/*
 * Zalith Launcher 2 / Nux Launcher Input Bridge
 */

package com.movtery.zalithlauncher.game.input;

import androidx.annotation.Keep;
import dalvik.annotation.optimization.CriticalNative;

@Keep
public class CriticalNativeTest {
    @Keep
    @CriticalNative
    public static native void testCriticalNative(int arg0, int arg1);

    @Keep
    public static void invokeTest() {
        testCriticalNative(0, 0);
    }
}
