package com.israadev.nuxlauncher.core.auth

import android.content.Context
import android.os.SystemClock

/**
 * Global OTP cooldown and anti-spam manager (5 Menit / 300 Detik).
 * Dilengkapi penyimpanan persisten (SharedPreferences) dan perlindungan monotonic clock
 * agar jeda 5 menit tetap aktif dan tidak bisa di-bypass dengan menutup aplikasi,
 * berpindah layar, atau memanipulasi jam di perangkat.
 */
object OtpCooldownManager {
    const val COOLDOWN_SECONDS: Int = 300 // 5 Menit (300 Detik)
    private const val PREF_NAME = "nux_otp_cooldown"
    private const val KEY_LAST_SENT_WALL = "last_sent_wall_ms"
    private const val KEY_LAST_SENT_BOOT = "last_sent_boot_ms"

    private var lastSentBootTimeMs: Long = 0L
    private var lastSentWallTimeMs: Long = 0L
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        lastSentWallTimeMs = prefs.getLong(KEY_LAST_SENT_WALL, 0L)
        lastSentBootTimeMs = prefs.getLong(KEY_LAST_SENT_BOOT, 0L)
        isInitialized = true
    }

    fun canSendOtp(): Boolean {
        return getRemainingSeconds() <= 0
    }

    fun getRemainingSeconds(): Int {
        val nowWall = System.currentTimeMillis()
        val nowBoot = SystemClock.elapsedRealtime()

        // 1. Hitungan berbasis Boot Time (Monotonic Clock - anti ubah jam HP saat app berjalan)
        var remBoot = 0
        if (lastSentBootTimeMs > 0L && nowBoot >= lastSentBootTimeMs) {
            val elapsedBoot = (nowBoot - lastSentBootTimeMs) / 1000
            val rem = COOLDOWN_SECONDS - elapsedBoot
            if (rem > 0) remBoot = rem.toInt()
        }

        // 2. Hitungan berbasis Wall Time (Persisten saat aplikasi dibuka ulang setelah ditutup)
        var remWall = 0
        if (lastSentWallTimeMs > 0L && nowWall >= lastSentWallTimeMs) {
            val elapsedWall = (nowWall - lastSentWallTimeMs) / 1000
            val rem = COOLDOWN_SECONDS - elapsedWall
            if (rem > 0) remWall = rem.toInt()
        }

        return maxOf(remBoot, remWall)
    }

    fun formatRemainingTime(): String {
        val rem = getRemainingSeconds()
        if (rem <= 0) return "0s"
        val m = rem / 60
        val s = rem % 60
        return if (m > 0) {
            "${m}m ${if (s < 10) "0$s" else "$s"}s"
        } else {
            "${s}s"
        }
    }

    fun markOtpSent(context: Context? = null, customRemainingSec: Int? = null) {
        if (customRemainingSec != null && customRemainingSec > 0) {
            val elapsedAlreadySec = (COOLDOWN_SECONDS - customRemainingSec).coerceAtLeast(0)
            lastSentBootTimeMs = SystemClock.elapsedRealtime() - (elapsedAlreadySec * 1000L)
            lastSentWallTimeMs = System.currentTimeMillis() - (elapsedAlreadySec * 1000L)
        } else {
            lastSentBootTimeMs = SystemClock.elapsedRealtime()
            lastSentWallTimeMs = System.currentTimeMillis()
        }

        context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)?.edit()?.apply {
            putLong(KEY_LAST_SENT_WALL, lastSentWallTimeMs)
            putLong(KEY_LAST_SENT_BOOT, lastSentBootTimeMs)
            apply()
        }
    }

    fun reset(context: Context? = null) {
        lastSentBootTimeMs = 0L
        lastSentWallTimeMs = 0L
        context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
    }
}
