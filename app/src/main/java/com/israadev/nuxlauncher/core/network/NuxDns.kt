package com.israadev.nuxlauncher.core.network

import android.util.Log
import com.google.gson.JsonParser
import okhttp3.Dns
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException

object NuxDns : Dns {
    private const val TAG = "NuxDns"

    // Cloudflare edge Anycast IPs for Modrinth API and CDN
    private val MODRINTH_IPS = listOf(
        byteArrayOf(104.toByte(), 18.toByte(), 23.toByte(), 35.toByte()),
        byteArrayOf(104.toByte(), 18.toByte(), 22.toByte(), 35.toByte()),
        byteArrayOf(172.toByte(), 67.toByte(), 182.toByte(), 196.toByte())
    )

    // Cloudflare edge Anycast IPs for Fabric Meta and Maven
    private val FABRIC_IPS = listOf(
        byteArrayOf(104.toByte(), 21.toByte(), 33.toByte(), 240.toByte()),
        byteArrayOf(172.toByte(), 67.toByte(), 151.toByte(), 177.toByte())
    )

    override fun lookup(hostname: String): List<InetAddress> {
        // 1. Try standard system DNS first
        try {
            val systemAddresses = Dns.SYSTEM.lookup(hostname)
            if (systemAddresses.isNotEmpty()) {
                return systemAddresses
            }
        } catch (e: Exception) {
            Log.w(TAG, "System DNS lookup failed for $hostname: ${e.message}, falling back")
        }

        // 2. Direct Cloudflare IP fallback for Modrinth domains
        if (hostname.equals("api.modrinth.com", ignoreCase = true) ||
            hostname.equals("cdn.modrinth.com", ignoreCase = true) ||
            hostname.endsWith(".modrinth.com", ignoreCase = true)
        ) {
            Log.i(TAG, "Using direct Cloudflare anycast IPs for Modrinth host: $hostname")
            return MODRINTH_IPS.map { ipBytes ->
                InetAddress.getByAddress(hostname, ipBytes)
            }
        }

        // Direct Cloudflare IP fallback for Fabric domains
        if (hostname.endsWith("fabricmc.net", ignoreCase = true)) {
            Log.i(TAG, "Using direct Cloudflare anycast IPs for Fabric host: $hostname")
            return FABRIC_IPS.map { ipBytes ->
                InetAddress.getByAddress(hostname, ipBytes)
            }
        }

        // 3. Fallback to Cloudflare DoH (DNS over HTTPS) using direct IP 1.1.1.1
        try {
            val dohAddresses = queryCloudflareDoh(hostname)
            if (dohAddresses.isNotEmpty()) {
                Log.i(TAG, "Resolved $hostname via Cloudflare DoH: $dohAddresses")
                return dohAddresses
            }
        } catch (e: Exception) {
            Log.w(TAG, "DoH lookup failed for $hostname: ${e.message}")
        }

        // If all fallbacks fail, throw UnknownHostException as per OkHttp Dns contract
        throw UnknownHostException("Unable to resolve host \"$hostname\": System DNS and Fallback DoH failed")
    }

    private fun queryCloudflareDoh(hostname: String): List<InetAddress> {
        val url = URL("https://1.1.1.1/dns-query?name=$hostname&type=A")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 4000
        conn.readTimeout = 4000
        conn.setRequestProperty("Accept", "application/dns-json")
        conn.setRequestProperty("User-Agent", "NuxLauncher/1.4.0")

        if (conn.responseCode == 200) {
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JsonParser.parseString(responseText).asJsonObject
            if (json.has("Answer")) {
                val answers = json.getAsJsonArray("Answer")
                val result = mutableListOf<InetAddress>()
                for (i in 0 until answers.size()) {
                    val ansObj = answers[i].asJsonObject
                    val type = ansObj.get("type")?.asInt ?: 0
                    if (type == 1) { // Type A (IPv4)
                        val ipStr = ansObj.get("data")?.asString ?: continue
                        val parts = ipStr.split(".")
                        if (parts.size == 4) {
                            val bytes = ByteArray(4) { idx -> parts[idx].toInt().toByte() }
                            result.add(InetAddress.getByAddress(hostname, bytes))
                        }
                    }
                }
                if (result.isNotEmpty()) return result
            }
        }
        return emptyList()
    }
}
