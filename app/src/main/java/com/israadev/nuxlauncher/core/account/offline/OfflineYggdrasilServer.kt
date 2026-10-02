package com.israadev.nuxlauncher.core.account.offline

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local embedded Yggdrasil session server for offline Minecraft accounts.
 * Enables in-game custom skin & cape rendering via authlib-injector (matching Zalith Launcher & HMCL).
 */
class OfflineYggdrasilServer(
    private val preferredPort: Int = 0
) {
    companion object {
        private const val TAG = "OfflineYggdrasil"
        var activeInstance: OfflineYggdrasilServer? = null
            private set
    }

    private val gson = Gson()
    private val isRunning = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()

    private val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA").apply {
        initialize(2048)
    }.genKeyPair()

    private val charactersByUuid = ConcurrentHashMap<String, Character>()
    private val charactersByName = ConcurrentHashMap<String, Character>()

    data class Character(
        val uuid: String,
        val name: String,
        val skinHash: String?,
        val skinBytes: ByteArray?,
        val isSlim: Boolean,
        val capeHash: String?,
        val capeBytes: ByteArray?
    )

    fun addCharacter(
        username: String,
        uuid: String,
        skinFile: File?,
        capeFile: File?,
        isSlim: Boolean
    ) {
        val cleanUuid = uuid.replace("-", "").lowercase()
        val skinBytes = if (skinFile != null && skinFile.exists() && skinFile.length() > 0L) {
            runCatching { skinFile.readBytes() }.getOrNull()
        } else null
        val skinHash = skinBytes?.let { sha256(it) }

        val capeBytes = if (capeFile != null && capeFile.exists() && capeFile.length() > 0L) {
            runCatching { capeFile.readBytes() }.getOrNull()
        } else null
        val capeHash = capeBytes?.let { sha256(it) }

        val character = Character(
            uuid = cleanUuid,
            name = username,
            skinHash = skinHash,
            skinBytes = skinBytes,
            isSlim = isSlim,
            capeHash = capeHash,
            capeBytes = capeBytes
        )

        charactersByUuid[cleanUuid] = character
        charactersByName[username.lowercase()] = character
        Log.i(TAG, "Registered offline character: $username ($cleanUuid) with skin=${skinHash != null}, cape=${capeHash != null}")
    }

    @Synchronized
    fun start(): Int {
        if (isRunning.get() && serverSocket != null && !serverSocket!!.isClosed) {
            return serverSocket!!.localPort
        }

        try {
            val socket = ServerSocket(preferredPort)
            serverSocket = socket
            isRunning.set(true)
            activeInstance = this

            val port = socket.localPort
            Log.i(TAG, "Offline Yggdrasil server listening on port $port")

            executor.execute {
                while (isRunning.get() && !socket.isClosed) {
                    try {
                        val client = socket.accept()
                        executor.execute {
                            handleClient(client)
                        }
                    } catch (e: Exception) {
                        if (!isRunning.get()) break
                    }
                }
            }

            return port
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start offline Yggdrasil server: ${e.message}", e)
            return -1
        }
    }

    @Synchronized
    fun stop() {
        isRunning.set(false)
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        if (activeInstance == this) {
            activeInstance = null
        }
        Log.i(TAG, "Offline Yggdrasil server stopped")
    }

    fun getPort(): Int {
        return serverSocket?.localPort ?: -1
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 10000
            val input = socket.getInputStream()
            val output = socket.getOutputStream()
            val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0].uppercase()
            val uri = parts[1]

            // Read headers
            val headers = mutableMapOf<String, String>()
            var contentLength = 0
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    val key = line.substring(0, colonIdx).trim().lowercase()
                    val value = line.substring(colonIdx + 1).trim()
                    headers[key] = value
                    if (key == "content-length") {
                        contentLength = value.toIntOrNull() ?: 0
                    }
                }
            }

            // Read body if POST
            var body = ""
            if (contentLength > 0) {
                val bodyChars = CharArray(contentLength)
                var readTotal = 0
                while (readTotal < contentLength) {
                    val read = reader.read(bodyChars, readTotal, contentLength - readTotal)
                    if (read == -1) break
                    readTotal += read
                }
                body = String(bodyChars, 0, readTotal)
            }

            val path = if (uri.contains('?')) uri.substringBefore('?') else uri

            when {
                path == "/" || path.isEmpty() -> {
                    val pem = "-----BEGIN PUBLIC KEY-----\n" +
                            Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(keyPair.public.encoded) +
                            "\n-----END PUBLIC KEY-----\n"

                    val json = JsonObject().apply {
                        val skinDomains = JsonArray().apply {
                            add("127.0.0.1")
                            add("localhost")
                        }
                        add("skinDomains", skinDomains)
                        val meta = JsonObject().apply {
                          addProperty("serverName", "NuxLauncher_Offline")
                          addProperty("implementationName", "NUX")
                          addProperty("implementationVersion", "1.0")
                          addProperty("feature.non_email_login", true)
                        }
                        add("meta", meta)
                        addProperty("signaturePublickey", pem)
                    }
                    sendJsonResponse(output, 200, json.toString())
                }

                path == "/status" -> {
                    val json = JsonObject().apply {
                        addProperty("user.count", charactersByUuid.size)
                        addProperty("token.count", 0)
                    }
                    sendJsonResponse(output, 200, json.toString())
                }

                path == "/api/profiles/minecraft" && method == "POST" -> {
                    val requestedNames = runCatching {
                        gson.fromJson(body, Array<String>::class.java)
                    }.getOrNull() ?: emptyArray()

                    val profilesArray = JsonArray()
                    requestedNames.distinct().forEach { name ->
                        val char = charactersByName[name.lowercase()]
                        if (char != null) {
                            val obj = JsonObject().apply {
                                addProperty("id", char.uuid)
                                addProperty("name", char.name)
                            }
                            profilesArray.add(obj)
                        }
                    }
                    sendJsonResponse(output, 200, profilesArray.toString())
                }

                path == "/sessionserver/session/minecraft/join" && method == "POST" -> {
                    sendEmptyResponse(output, 204, "No Content")
                }

                path == "/sessionserver/session/minecraft/hasJoined" -> {
                    val query = if (uri.contains('?')) uri.substringAfter('?') else ""
                    val username = parseQueryParam(query, "username")
                    val char = if (username != null) charactersByName[username.lowercase()] else null
                    if (char != null) {
                        val port = getPort()
                        val profileJson = buildSignedProfileJson(char, port)
                        sendJsonResponse(output, 200, profileJson)
                    } else {
                        sendEmptyResponse(output, 204, "No Content")
                    }
                }

                path.startsWith("/sessionserver/session/minecraft/profile/") -> {
                    val uuid = path.substringAfter("/sessionserver/session/minecraft/profile/").replace("-", "").lowercase()
                    val char = charactersByUuid[uuid]
                    if (char != null) {
                        val port = getPort()
                        val profileJson = buildSignedProfileJson(char, port)
                        sendJsonResponse(output, 200, profileJson)
                    } else {
                        sendEmptyResponse(output, 204, "No Content")
                    }
                }

                path.startsWith("/textures/") -> {
                    val hash = path.substringAfter("/textures/")
                    val matchingBytes = charactersByUuid.values.firstNotNullOfOrNull { c ->
                        when (hash) {
                            c.skinHash -> c.skinBytes
                            c.capeHash -> c.capeBytes
                            else -> null
                        }
                    }

                    if (matchingBytes != null) {
                        sendBinaryResponse(output, 200, matchingBytes, "image/png", hash)
                    } else {
                        sendEmptyResponse(output, 404, "Not Found")
                    }
                }

                else -> {
                    sendEmptyResponse(output, 404, "Not Found")
                }
            }
        } catch (_: Exception) {
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun buildSignedProfileJson(character: Character, port: Int): String {
        val rootUrl = "http://127.0.0.1:$port"

        val texturesObj = JsonObject().apply {
            addProperty("timestamp", System.currentTimeMillis())
            addProperty("profileId", character.uuid)
            addProperty("profileName", character.name)

            val innerTextures = JsonObject()

            if (character.skinHash != null) {
                val skinObj = JsonObject().apply {
                    addProperty("url", "$rootUrl/textures/${character.skinHash}")
                    if (character.isSlim) {
                        val metaObj = JsonObject().apply {
                            addProperty("model", "slim")
                        }
                        add("metadata", metaObj)
                    }
                }
                innerTextures.add("SKIN", skinObj)
            }

            if (character.capeHash != null) {
                val capeObj = JsonObject().apply {
                    addProperty("url", "$rootUrl/textures/${character.capeHash}")
                }
                innerTextures.add("CAPE", capeObj)
            }

            add("textures", innerTextures)
        }

        val jsonString = texturesObj.toString()
        val base64Textures = Base64.getEncoder().encodeToString(jsonString.toByteArray(Charsets.UTF_8))
        val signature = signWithRsa(base64Textures)

        val root = JsonObject().apply {
            addProperty("id", character.uuid)
            addProperty("name", character.name)
            val properties = JsonArray().apply {
                val prop = JsonObject().apply {
                    addProperty("name", "textures")
                    addProperty("value", base64Textures)
                    addProperty("signature", signature)
                }
                add(prop)
            }
            add("properties", properties)
        }

        return root.toString()
    }

    private fun signWithRsa(data: String): String {
        val sig = Signature.getInstance("SHA1withRSA")
        sig.initSign(keyPair.private)
        sig.update(data.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(sig.sign())
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun parseQueryParam(query: String, param: String): String? {
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf('=')
            if (idx > 0 && pair.substring(0, idx) == param) {
                return pair.substring(idx + 1)
            }
        }
        return null
    }

    private fun sendJsonResponse(output: OutputStream, status: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val writer = PrintWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.print("HTTP/1.1 $status OK\r\n")
        writer.print("Content-Type: application/json; charset=utf-8\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()
        output.write(bytes)
        output.flush()
    }

    private fun sendBinaryResponse(output: OutputStream, status: Int, bytes: ByteArray, mimeType: String, etag: String) {
        val writer = PrintWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.print("HTTP/1.1 $status OK\r\n")
        writer.print("Content-Type: $mimeType\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Cache-Control: max-age=2592000, public\r\n")
        writer.print("ETag: \"$etag\"\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()
        output.write(bytes)
        output.flush()
    }

    private fun sendEmptyResponse(output: OutputStream, status: Int, statusText: String) {
        val writer = PrintWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.print("HTTP/1.1 $status $statusText\r\n")
        writer.print("Content-Length: 0\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()
    }
}
