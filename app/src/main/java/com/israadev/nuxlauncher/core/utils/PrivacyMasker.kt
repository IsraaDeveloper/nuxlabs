package com.israadev.nuxlauncher.core.utils

object PrivacyMasker {

    /**
     * Menyensor email: hanya menampilkan 3 karakter di depan, lalu ***, lalu @ dan kata/domain setelahnya.
     * Contoh: israadev@gmail.com -> isr***@gmail.com
     */
    fun maskEmail(email: String?): String {
        if (email.isNullOrBlank()) return "-"
        val trimmed = email.trim()
        if (trimmed.equals("offline", ignoreCase = true) || trimmed == "-") return trimmed

        val atIndex = trimmed.indexOf('@')
        if (atIndex <= 0) {
            return if (trimmed.length <= 3) trimmed else "${trimmed.take(3)}***"
        }

        val username = trimmed.substring(0, atIndex)
        val domainPart = trimmed.substring(atIndex) // Termasuk '@' dan karakter setelahnya

        val prefix = when {
            username.length >= 3 -> username.take(3)
            username.isNotEmpty() -> username
            else -> "***"
        }

        return "$prefix***$domainPart"
    }

    /**
     * Menyensor license key: hanya menampilkan 3 karakter di depan, sisanya disensor.
     * Contoh: NUX-1234-5678-ABCD -> NUX-••••-••••-••••
     */
    fun maskKey(key: String?): String {
        if (key.isNullOrBlank()) return "VIP-AKTIF-SERVER"
        val clean = key.trim().uppercase()
        val prefix = if (clean.length >= 3) clean.take(3) else clean
        return "$prefix-••••-••••-••••"
    }
}
