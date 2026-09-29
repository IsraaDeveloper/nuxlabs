package com.israadev.nuxlauncher.core.crash

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object CrashUtils {

    fun getSignalName(signal: Int): String {
        return when (signal) {
            4 -> "SIGILL (Instruksi CPU Tidak Didukung)"
            6 -> "SIGABRT (Native Assertion / Abort)"
            7 -> "SIGBUS (Kesalahan Bus Memori)"
            8 -> "SIGFPE (Kesalahan Perhitungan Float)"
            9 -> "SIGKILL (Dihentikan OS / Kehabisan RAM)"
            11 -> "SIGSEGV (Akses Memori Ilegal / Segfault)"
            15 -> "SIGTERM (Permintaan Terminasi)"
            else -> "Signal $signal"
        }
    }

    fun getExitCodeDescription(exitCode: Int): String {
        return when (exitCode) {
            0 -> "Selesai Normal (0)"
            1 -> "Kesalahan Inisialisasi Mod / Java Exception (1)"
            130 -> "Dihentikan Pengguna (SIGINT 130)"
            134 -> "Native Driver Abort (SIGABRT 134)"
            137 -> "Kehabisan Memori RAM / OOM (SIGKILL 137)"
            139 -> "Crash Driver Grafis / Segfault (SIGSEGV 139)"
            -1 -> "Kegagalan Bootstrap Mesin Virtual (-1)"
            else -> "Kode Keluar $exitCode"
        }
    }

    const val CRASH_LOG_NOTE = "Untuk detail penyebabnya, silahkan periksa file log. Jika Anda tidak bisa menyelesaikan masalah sendiri, silakan kirim file log kepada seseorang atau tim yang dapat menganalisisnya. Jangan hanya mengambil screenshot dari layar saat ini, karena screenshot tidak bisa memberikan informasi teknis yang cukup untuk menyelesaikan masalah."

    fun getExitMessage(exitCode: Int, isSignal: Boolean): String {
        return if (isSignal) {
            "JVM digugurkan karena sinyal fatal $exitCode (${getSignalName(exitCode)})."
        } else {
            "JVM keluar dengan kode $exitCode (${getExitCodeDescription(exitCode)})."
        }
    }

    fun shareLogFile(context: Context, file: File) {
        try {
            if (!file.exists()) {
                Toast.makeText(context, "File log tidak ditemukan.", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Log Crash NUX Launcher - ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Bagikan Log Crash: ${file.name}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Tidak ada aplikasi untuk membagikan file ini.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
