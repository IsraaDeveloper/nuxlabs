package com.israadev.nuxlauncher.core.utils

import android.os.Build
import com.israadev.nuxlauncher.core.renderer.NuxRendererInfo

object NuxVersionUtils {

    /**
     * Parse numeric version parts from string (e.g., "1.21.4" -> [1, 21, 4])
     */
    fun parseVersionParts(version: String): List<Int> {
        if (version.isBlank()) return emptyList()

        // Check snapshot format like "24w12a" or "25w01a" -> map to major version roughly (e.g. 24w -> 1.21)
        val snapshotMatch = Regex("""^(\d{2})w(\d{2})[a-z]?""", RegexOption.IGNORE_CASE).find(version.trim())
        if (snapshotMatch != null) {
            val year = snapshotMatch.groupValues[1].toIntOrNull() ?: 24
            val week = snapshotMatch.groupValues[2].toIntOrNull() ?: 1
            // 24w... is MC 1.21, 25w... is 1.22+, etc.
            val derivedMinor = when {
                year >= 25 -> 22
                year == 24 -> 21
                year == 23 -> 20
                year == 22 -> 19
                year == 21 -> 18
                else -> 17
            }
            return listOf(1, derivedMinor, 0, week)
        }

        // Clean version string: remove build metadata or suffix (e.g. "-rc1", "-pre2", "_Forge")
        val clean = version.trim().split("-", "_", " ").first()
        val parts = clean.split(".")
        val result = mutableListOf<Int>()
        for (part in parts) {
            val digits = part.filter { it.isDigit() }
            if (digits.isNotEmpty()) {
                result.add(digits.toInt())
            }
        }
        return result
    }

    /**
     * Returns:
     * < 0 if v1 < v2
     * 0 if v1 == v2
     * > 0 if v1 > v2
     */
    fun compare(v1: String, v2: String): Int {
        val s1 = v1.trim()
        val s2 = v2.trim()
        val p1 = parseVersionParts(s1)
        val p2 = parseVersionParts(s2)

        val maxLen = maxOf(p1.size, p2.size)
        for (i in 0 until maxLen) {
            val n1 = p1.getOrElse(i) { 0 }
            val n2 = p2.getOrElse(i) { 0 }
            if (n1 != n2) {
                return n1.compareTo(n2)
            }
        }

        // If numeric version is equal (e.g. "26.3" vs "26.3-snapshot-4"):
        // Release (no dash) is newer than pre-release/snapshot (with dash)
        val hasDash1 = s1.contains("-")
        val hasDash2 = s2.contains("-")
        if (!hasDash1 && hasDash2) return 1
        if (hasDash1 && !hasDash2) return -1

        // If both have snapshots (e.g. "26.3-snapshot-5" vs "26.3-snapshot-4")
        if (hasDash1 && hasDash2) {
            val snap1 = Regex("""snapshot-(\d+)""", RegexOption.IGNORE_CASE).find(s1)?.groupValues?.get(1)?.toIntOrNull()
            val snap2 = Regex("""snapshot-(\d+)""", RegexOption.IGNORE_CASE).find(s2)?.groupValues?.get(1)?.toIntOrNull()
            if (snap1 != null && snap2 != null && snap1 != snap2) {
                return snap1.compareTo(snap2)
            }
        }

        return 0
    }

    fun isLowerVer(v1: String, v2: String): Boolean {
        return compare(v1, v2) < 0
    }

    fun isBiggerVer(v1: String, v2: String): Boolean {
        return compare(v1, v2) > 0
    }

    /**
     * Checks whether a given renderer supports the specified Minecraft version.
     * Follows Zalith's rule:
     * - If minMCVersion is set and mcVer < minMCVersion -> unsupported
     * - If maxMCVersion is set and mcVer > maxMCVersion -> unsupported
     */
    fun isRendererSupported(renderer: NuxRendererInfo, mcVersion: String): Boolean {
        if (renderer.id.equals("auto", ignoreCase = true)) return true
        if (mcVersion.isBlank()) return true

        val minVer = renderer.minMCVersion
        if (!minVer.isNullOrBlank() && isLowerVer(mcVersion, minVer)) {
            return false
        }

        val maxVer = renderer.maxMCVersion
        if (!maxVer.isNullOrBlank() && isBiggerVer(mcVersion, maxVer)) {
            return false
        }

        return true
    }

    /**
     * Checks if device GPU is Qualcomm Adreno.
     * Qualcomm owns and exclusively uses Adreno GPUs on Qualcomm Snapdragon chipsets.
     */
    fun isAdrenoGPU(): Boolean {
        val hardware = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MANUFACTURER.lowercase()
        } else ""

        return hardware.contains("qcom") || hardware.contains("qualcomm") ||
                board.contains("qcom") || board.contains("msm") || board.contains("sm") ||
                soc.contains("qualcomm") || soc.contains("qcom")
    }
}
