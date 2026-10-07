package com.israadev.nuxlauncher

import android.app.Application
import android.util.Log
import com.israadev.nuxlauncher.core.crash.CrashManager
import com.israadev.nuxlauncher.ui.activities.ErrorActivity

class NuxApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        org.lwjgl.glfw.CallbackBridge.sContext = this
        com.movtery.zalithlauncher.bridge.ZLNativeInvoker.appContext = this

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("NuxApplication", "Uncaught exception on thread ${thread.name}", throwable)
                // Catat ke sistem crash manager
                CrashManager.recordLauncherCrash(this, throwable)
                // Tampilkan ErrorActivity mandiri
                ErrorActivity.showLauncherCrash(this, throwable)
            } catch (e: Exception) {
                Log.e("NuxApplication", "Failed to dispatch crash activity", e)
                defaultHandler?.uncaughtException(thread, throwable)
            }

            // Hentikan proses yang mengalami crash agar tidak memicu ANR / loop
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(10)
        }
    }
}
