package com.movtery.zalithlauncher.bridge;

public class NativeLibraryLoader {
    public static void loadPojavLib() {
        System.loadLibrary("pojavexec");
    }

    public static void loadExitHookLib() {
        System.loadLibrary("bytehook");
        System.loadLibrary("exithook");
    }

    public static void loadPojavAWTLib() {
        System.loadLibrary("pojavexec_awt");
    }
}
