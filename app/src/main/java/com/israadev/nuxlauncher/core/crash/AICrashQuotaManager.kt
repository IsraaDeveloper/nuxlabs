package com.israadev.nuxlauncher.core.crash

import android.content.Context
import com.israadev.nuxlauncher.core.models.LauncherSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pengelola kuota harian untuk penggunaan fitur AI Crash Analyzer.
 * Default kuota: 5 kali per hari.
 * Jika pengguna menggunakan API Key pribadi di Pengaturan, kuota menjadi Unlimited.
 */
object AICrashQuotaManager {
    const val DAILY_LIMIT = 5
    private const val PREF_NAME = "nux_ai_quota"
    private const val KEY_DATE = "quota_date"
    private const val KEY_USED_COUNT = "quota_used_count"

    private fun getTodayDateStr(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    /**
     * Memeriksa apakah user menggunakan custom API key sendiri.
     */
    fun isUsingCustomKey(settings: LauncherSettings?): Boolean {
        return !settings?.aiApiKey.isNullOrBlank()
    }

    /**
     * Mendapatkan jumlah pemakaian AI hari ini.
     */
    fun getUsedCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedDate = prefs.getString(KEY_DATE, "") ?: ""
        val today = getTodayDateStr()

        return if (savedDate == today) {
            prefs.getInt(KEY_USED_COUNT, 0)
        } else {
            0
        }
    }

    /**
     * Mendapatkan sisa kuota hari ini.
     * Mengembalikan -1 jika Unlimited (menggunakan API key pribadi).
     */
    fun getRemainingQuota(context: Context, settings: LauncherSettings?): Int {
        if (isUsingCustomKey(settings)) {
            return -1 // Unlimited
        }
        val used = getUsedCount(context)
        return (DAILY_LIMIT - used).coerceAtLeast(0)
    }

    /**
     * Cek apakah user masih memiliki kuota untuk melakukan analisis AI.
     */
    fun hasQuota(context: Context, settings: LauncherSettings?): Boolean {
        if (isUsingCustomKey(settings)) return true
        return getRemainingQuota(context, settings) > 0
    }

    /**
     * Mengurangi/mengonsumsi 1 kuota harian (jika bukan custom key).
     * Mengembalikan true jika berhasil dikonsumsi, false jika kuota sudah habis.
     */
    @Synchronized
    fun consumeQuota(context: Context, settings: LauncherSettings?): Boolean {
        if (isUsingCustomKey(settings)) {
            return true
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val today = getTodayDateStr()
        val savedDate = prefs.getString(KEY_DATE, "") ?: ""

        val currentUsed = if (savedDate == today) {
            prefs.getInt(KEY_USED_COUNT, 0)
        } else {
            0
        }

        if (currentUsed >= DAILY_LIMIT) {
            return false
        }

        prefs.edit()
            .putString(KEY_DATE, today)
            .putInt(KEY_USED_COUNT, currentUsed + 1)
            .apply()

        return true
    }
}
