package com.movtery.zalithlauncher.bridge;

import androidx.annotation.Keep;

@Keep
public final class LoggerBridge {
    /** Reset the log file, effectively erasing any previous logs */
    @Keep
    public static native void start(String filePath);

    /** Print the text to the log file if not censored */
    @Keep
    public static native void append(String log);

    /** Link a log listener to the logger */
    @Keep
    public static native void setListener(EventLogListener listener);

    /** Small listener for anything listening to the log */
    @Keep
    public interface EventLogListener {
        @Keep
        void onEventLogged(String text);
    }

    public static void appendTitle(String title) {
        String logText = "==================== " + title + " ====================";
        append(logText);
    }

    static {
        NativeLibraryLoader.loadPojavLib();
    }
}
