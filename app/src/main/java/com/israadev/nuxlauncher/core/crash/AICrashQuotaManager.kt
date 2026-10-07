package com.israadev.nuxlauncher.core.crash

import android.content.Context
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.auth.AuthService
import com.israadev.nuxlauncher.core.models.LauncherSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Pengelola kuota harian untuk penggunaan fitur AI Crash Analyzer.
 * Default kuota: 5 kali per hari.
 * Otomatis di-reset setiap jam 00:00 WIB (Asia/Jakarta).
 * Disinkronkan dengan Database Firebase (Keystore Backend) dan dicache secara lokal.
 * Jika pengguna memiliki status VIP/Premium atau memasukkan API Key pribadi, kuota menjadi Unlimited (-1).
 */
object AICrashQuotaManager {
    const val DAILY_LIMIT = 5
    private const val PREF_NAME = "nux_ai_quota"
    private const val KEY_DATE = "quota_date"
    private const val KEY_USED_COUNT = "quota_used_count"
    private const val KEY_REMAINING_QUOTA = "quota_remaining"

    /**
     * Mendapatkan string tanggal hari ini dalam format YYYY-MM-DD
     * berdasarkan zona waktu Indonesia Barat (WIB, Asia/Jakarta) sehingga otomatis reset tepat jam 00:00 WIB.
     */
    fun getTodayJakartaDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        return sdf.format(Date())
    }

    /**
     * Memeriksa apakah user menggunakan custom API key sendiri.
     */
    fun isUsingCustomKey(settings: LauncherSettings?): Boolean {
        return !settings?.aiApiKey.isNullOrBlank()
    }

    /**
     * Memeriksa apakah akun pengguna saat ini memiliki hak akses VIP/Unlimited.
     */
    fun isVipUser(): Boolean {
        val user = AccountManager.getActiveUser() ?: AccountManager.launcherUser.value
        return user?.isActivated == true
    }

    /**
     * Sinkronisasi kuota dari database backend.
     * Mengembalikan sisa kuota setelah disinkronkan (-1 jika unlimited).
     */
    suspend fun syncQuotaFromDatabase(context: Context, settings: LauncherSettings?): Int = withContext(Dispatchers.IO) {
        if (isUsingCustomKey(settings) || isVipUser()) {
            return@withContext -1
        }

        val uid = AccountManager.getActiveUser()?.uid?.trim()
            ?: AccountManager.launcherUser.value?.uid?.trim()
            ?: ""

        val today = getTodayJakartaDate()
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        if (uid.isNotBlank()) {
            val result = AuthService.fetchAiQuota(uid)
            result.onSuccess { info ->
                prefs.edit()
                    .putString(KEY_DATE, info.date.ifBlank { today })
                    .putInt(KEY_USED_COUNT, info.usedCount)
                    .putInt(KEY_REMAINING_QUOTA, info.remainingQuota)
                    .apply()
                return@withContext info.remainingQuota
            }
        }

        // Fallback jika belum login atau offline: evaluasi cache lokal berdasarkan jam 00:00 WIB
        val savedDate = prefs.getString(KEY_DATE, "") ?: ""
        if (savedDate != today) {
            prefs.edit()
                .putString(KEY_DATE, today)
                .putInt(KEY_USED_COUNT, 0)
                .putInt(KEY_REMAINING_QUOTA, DAILY_LIMIT)
                .apply()
            return@withContext DAILY_LIMIT
        }

        val used = prefs.getInt(KEY_USED_COUNT, 0)
        return@withContext (DAILY_LIMIT - used).coerceAtLeast(0)
    }

    /**
     * Mendapatkan jumlah pemakaian AI hari ini (memeriksa reset jam 00:00 WIB).
     */
    fun getUsedCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedDate = prefs.getString(KEY_DATE, "") ?: ""
        val today = getTodayJakartaDate()

        return if (savedDate == today) {
            prefs.getInt(KEY_USED_COUNT, 0)
        } else {
            0
        }
    }

    /**
     * Mendapatkan sisa kuota hari ini.
     * Mengembalikan -1 jika Unlimited (VIP / custom key).
     */
    fun getRemainingQuota(context: Context, settings: LauncherSettings?): Int {
        if (isUsingCustomKey(settings) || isVipUser()) {
            return -1 // Unlimited
        }
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedDate = prefs.getString(KEY_DATE, "") ?: ""
        val today = getTodayJakartaDate()

        return if (savedDate == today) {
            val cachedRem = prefs.getInt(KEY_REMAINING_QUOTA, -99)
            if (cachedRem != -99) {
                cachedRem
            } else {
                val used = prefs.getInt(KEY_USED_COUNT, 0)
                (DAILY_LIMIT - used).coerceAtLeast(0)
            }
        } else {
            DAILY_LIMIT
        }
    }

    /**
     * Cek apakah user masih memiliki kuota untuk melakukan analisis AI.
     */
    fun hasQuota(context: Context, settings: LauncherSettings?): Boolean {
        if (isUsingCustomKey(settings) || isVipUser()) return true
        return getRemainingQuota(context, settings) > 0
    }

    /**
     * Mengurangi/mengonsumsi 1 kuota harian baik di database backend maupun cache lokal.
     * Mengembalikan true jika berhasil dikonsumsi, false jika kuota sudah habis.
     */
    suspend fun consumeQuota(context: Context, settings: LauncherSettings?): Boolean = withContext(Dispatchers.IO) {
        if (isUsingCustomKey(settings) || isVipUser()) {
            return@withContext true
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val today = getTodayJakartaDate()
        val savedDate = prefs.getString(KEY_DATE, "") ?: ""

        var currentUsed = if (savedDate == today) {
            prefs.getInt(KEY_USED_COUNT, 0)
        } else {
            0
        }

        // Cek limit lokal sebelum request
        if (currentUsed >= DAILY_LIMIT) {
            return@withContext false
        }

        val uid = AccountManager.getActiveUser()?.uid?.trim()
            ?: AccountManager.launcherUser.value?.uid?.trim()
            ?: ""

        if (uid.isNotBlank()) {
            val result = AuthService.consumeAiQuota(uid)
            if (result.isSuccess) {
                val info = result.getOrNull()!!
                prefs.edit()
                    .putString(KEY_DATE, info.date.ifBlank { today })
                    .putInt(KEY_USED_COUNT, info.usedCount)
                    .putInt(KEY_REMAINING_QUOTA, info.remainingQuota)
                    .apply()
                return@withContext true
            } else {
                val err = result.exceptionOrNull()?.message ?: ""
                if (err.contains("Batas", ignoreCase = true) || err.contains("429")) {
                    prefs.edit()
                        .putString(KEY_DATE, today)
                        .putInt(KEY_USED_COUNT, DAILY_LIMIT)
                        .putInt(KEY_REMAINING_QUOTA, 0)
                        .apply()
                    return@withContext false
                }
                // Jika kegagalan hanya karena jaringan tidak stabil, izinkan pemakaian secara lokal
            }
        }

        // Fallback / Guest local counter
        currentUsed += 1
        val newRemaining = (DAILY_LIMIT - currentUsed).coerceAtLeast(0)
        prefs.edit()
            .putString(KEY_DATE, today)
            .putInt(KEY_USED_COUNT, currentUsed)
            .putInt(KEY_REMAINING_QUOTA, newRemaining)
            .apply()

        return@withContext true
    }
}
