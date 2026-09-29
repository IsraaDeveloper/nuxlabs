package com.israadev.nuxlauncher.core.skin

import android.util.Base64
import android.util.Log
import com.israadev.nuxlauncher.core.models.UserAccount
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "OfflineSkinServer"

class OfflineSkinServer {
    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private var serverThread: Thread? = null

    private val keyPair: KeyPair by lazy {
        KeyPairGenerator.getInstance("RSA").apply {
            initialize(2048)
        }.genKeyPair()
    }

    private val charactersByUuid = ConcurrentHashMap<String, CharacterData>()
    private val charactersByName = ConcurrentHashMap<String, CharacterData>()
    private val texturesByHash = ConcurrentHashMap<String, ByteArray>()

    var port: Int = 0
        private set

    data class CharacterData(
        val uuid: String,
        val name: String,
        val isSlim: Boolean,
        val skinHash: String?,
        val capeHash: String?
    )

    fun start(): Int {
        if (isRunning.get()) return port

        val socket = ServerSocket(0)
        port = socket.localPort
        serverSocket = socket
        isRunning.set(true)

        serverThread = Thread({
            Log.d(TAG, "OfflineSkinServer started on port $port")
            while (isRunning.get()) {
                try {
                    val client = socket.accept()
                    Thread({ handleClient(client) }, "OfflineSkinClient-${client.port}").start()
                } catch (e: Exception) {
                    if (!isRunning.get()) break
                    Log.e(TAG, "Accept error: ${e.message}")
                }
            }
        }, "OfflineSkinServerThread").apply {
            isDaemon = true
            start()
        }

        return port
    }

    fun stop() {
        isRunning.set(false)
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        charactersByUuid.clear()
        charactersByName.clear()
        texturesByHash.clear()
        Log.d(TAG, "OfflineSkinServer stopped")
    }

    fun registerAccount(account: UserAccount) {
        val cleanUuid = account.uuid.replace("-", "").lowercase()
        val cleanName = account.username.trim()

        var skinHash: String? = null
        if (!account.customSkinPath.isNullOrBlank()) {
            val skinFile = File(account.customSkinPath)
            if (skinFile.exists()) {
                val bytes = skinFile.readBytes()
                val hash = sha256(bytes)
                texturesByHash[hash] = bytes
                skinHash = hash
            }
        }

        var capeHash: String? = null
        if (!account.customCapePath.isNullOrBlank()) {
            val capeFile = File(account.customCapePath)
            if (capeFile.exists()) {
                val bytes = capeFile.readBytes()
                val hash = sha256(bytes)
                texturesByHash[hash] = bytes
                capeHash = hash
            }
        }

        val charData = CharacterData(
            uuid = cleanUuid,
            name = cleanName,
            isSlim = account.safeSkinModel.lowercase() == "slim",
            skinHash = skinHash,
            capeHash = capeHash
        )

        charactersByUuid[cleanUuid] = charData
        charactersByName[cleanName.lowercase()] = charData
        Log.d(TAG, "Registered character for offline server: ${charData.name} (UUID: ${charData.uuid}, skin: $skinHash, cape: $capeHash)")
    }

    private fun handleClient(socket: Socket) {
        try {
            val input = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            val output = BufferedOutputStream(socket.getOutputStream())

            val requestLine = input.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val path = parts[1]

            var contentLength = 0
            var headerLine: String?
            while (input.readLine().also { headerLine = it } != null) {
                if (headerLine.isNullOrBlank()) break
                val lower = headerLine!!.lowercase()
                if (lower.startsWith("content-length:")) {
                    contentLength = lower.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            var requestBody = ""
            if (contentLength > 0) {
                val chars = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val r = input.read(chars, read, contentLength - read)
                    if (r == -1) break
                    read += r
                }
                requestBody = String(chars, 0, read)
            }

            when {
                path == "/" || path == "/api" -> {
                    // Yggdrasil metadata conforming to authlib-injector specification
                    val pem = exportPublicKeyToPEM()
                    val json = """{"skinDomains":["127.0.0.1","localhost"],"meta":{"serverName":"NUX-Launcher_Offline","implementationName":"NUX-Launcher","implementationVersion":"1.0","feature.non_email_login":true},"signaturePublickey":"$pem"}"""
                    sendResponse(output, 200, "application/json", json.toByteArray(Charsets.UTF_8))
                }
                path == "/status" -> {
                    val json = """{"user.count":${charactersByUuid.size},"token.count":0}"""
                    sendResponse(output, 200, "application/json", json.toByteArray(Charsets.UTF_8))
                }
                path.startsWith("/api/profiles/minecraft") -> {
                    val results = mutableListOf<String>()
                    for ((nameLower, charData) in charactersByName) {
                        if (requestBody.contains("\"$nameLower\"", ignoreCase = true) || requestBody.contains("\"${charData.name}\"", ignoreCase = true) || requestBody.isEmpty()) {
                            results.add("""{"id":"${charData.uuid}","name":"${charData.name}"}""")
                        }
                    }
                    if (results.isEmpty() && charactersByName.isNotEmpty()) {
                        val first = charactersByName.values.first()
                        results.add("""{"id":"${first.uuid}","name":"${first.name}"}""")
                    }
                    val resp = "[${results.joinToString(",")}]"
                    sendResponse(output, 200, "application/json", resp.toByteArray(Charsets.UTF_8))
                }
                path.startsWith("/sessionserver/session/minecraft/join") -> {
                    sendResponse(output, 204, "application/json", ByteArray(0))
                }
                path.startsWith("/sessionserver/session/minecraft/hasJoined") -> {
                    val query = path.substringAfter("?", "")
                    val rawUsername = query.split("&")
                        .firstOrNull { it.startsWith("username=") }
                        ?.substringAfter("username=")
                    val username = if (!rawUsername.isNullOrBlank()) {
                        runCatching { java.net.URLDecoder.decode(rawUsername, "UTF-8") }.getOrDefault(rawUsername)
                    } else null
                    val charData = if (!username.isNullOrBlank()) charactersByName[username.lowercase()] else null
                    if (charData != null) {
                        val resp = buildProfileResponse(charData)
                        sendResponse(output, 200, "application/json", resp.toByteArray(Charsets.UTF_8))
                    } else {
                        sendResponse(output, 204, "application/json", ByteArray(0))
                    }
                }
                path.startsWith("/sessionserver/session/minecraft/profile/") -> {
                    val rawUuid = path.substringAfter("/sessionserver/session/minecraft/profile/").substringBefore("?").replace("-", "").lowercase()
                    val charData = charactersByUuid[rawUuid]
                    if (charData != null) {
                        val resp = buildProfileResponse(charData)
                        sendResponse(output, 200, "application/json", resp.toByteArray(Charsets.UTF_8))
                    } else {
                        sendResponse(output, 204, "application/json", ByteArray(0))
                    }
                }
                path.startsWith("/textures/") -> {
                    val hash = path.substringAfter("/textures/").substringBefore("?")
                    val textureBytes = texturesByHash[hash]
                    if (textureBytes != null) {
                        sendResponse(output, 200, "image/png", textureBytes)
                    } else {
                        sendResponse(output, 404, "text/plain", "Not found".toByteArray(Charsets.UTF_8))
                    }
                }
                else -> {
                    sendResponse(output, 204, "application/json", ByteArray(0))
                }
            }
        } catch (_: Exception) {
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun buildProfileResponse(charData: CharacterData): String {
        val rootUrl = "http://127.0.0.1:$port"
        val skinBlock = if (charData.skinHash != null) {
            val modelAttr = if (charData.isSlim) ""","metadata":{"model":"slim"}""" else ""
            """"SKIN":{"url":"$rootUrl/textures/${charData.skinHash}"$modelAttr}"""
        } else null

        val capeBlock = if (charData.capeHash != null) {
            """"CAPE":{"url":"$rootUrl/textures/${charData.capeHash}"}"""
        } else null

        val texturesInner = listOfNotNull(skinBlock, capeBlock).joinToString(",")
        val texturesJson = """{"timestamp":${System.currentTimeMillis()},"profileId":"${charData.uuid}","profileName":"${charData.name}","textures":{$texturesInner}}"""
        val base64Value = Base64.encodeToString(texturesJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val signature = signData(base64Value)

        return """{"id":"${charData.uuid}","name":"${charData.name}","properties":[{"name":"textures","value":"$base64Value","signature":"$signature"}]}"""
    }

    private fun signData(data: String): String {
        val sig = Signature.getInstance("SHA1withRSA")
        sig.initSign(keyPair.private)
        sig.update(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(sig.sign(), Base64.NO_WRAP)
    }

    private fun exportPublicKeyToPEM(): String {
        val base64Key = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
        return "-----BEGIN PUBLIC KEY-----\\n$base64Key\\n-----END PUBLIC KEY-----"
    }

    private fun sha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun sendResponse(output: BufferedOutputStream, status: Int, contentType: String, body: ByteArray) {
        val statusText = when (status) {
            200 -> "OK"
            204 -> "No Content"
            404 -> "Not Found"
            else -> "OK"
        }
        val header = "HTTP/1.1 $status $statusText\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        if (body.isNotEmpty()) {
            output.write(body)
        }
        output.flush()
    }
}
