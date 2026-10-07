package com.israadev.nuxlauncher.core.crash

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.israadev.nuxlauncher.BuildConfig
import com.israadev.nuxlauncher.core.models.LauncherSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

sealed class AIStreamState {
    object Idle : AIStreamState()
    object Connecting : AIStreamState()
    data class Streaming(val fullText: String, val chunk: String) : AIStreamState()
    data class Completed(val fullText: String) : AIStreamState()
    data class Error(val errorMessage: String) : AIStreamState()
    object QuotaExceeded : AIStreamState()
}

object AICrashAnalyzer {
    private fun decodeKey(b64: String): String {
        return try {
            String(android.util.Base64.decode(b64, android.util.Base64.DEFAULT), Charsets.UTF_8).trim()
        } catch (_: Exception) {
            ""
        }
    }

    val FALLBACK_KEYS: List<String> = listOf(
        BuildConfig.DEFAULT_AI_API_KEY.trim().ifBlank { decodeKey("c2stb3ItdjEtYmZiNjRiZTQyMWJlNTU0NjUwY2NlYjAzYjIwMzMyNDAwM2M4MWZmYjIxMmQ1N2Q0ZWM3Y2JlMWVmZWQzMDQ0OQ==") },
        BuildConfig.DEFAULT_AI_API_KEY_2.trim().ifBlank { decodeKey("c2stb3ItdjEtM2NlOTM5MDNhMjBkNzhlZDU5MDViMDBkOTk4MTFmNDcyNTljZTg4NDgzOTAwMzc1N2VkYWUxOGFiODU2MjNjZA==") },
        BuildConfig.DEFAULT_AI_API_KEY_3.trim().ifBlank { decodeKey("c2stb3ItdjEtY2ZkMzkxOGRiN2U4MDE5NjgzZWYxNGZmMjZiZTRhODg5YTU0MTM0MTY1YTI0YTBjMGE1N2FkMmY4Nzk0MDMwOQ==") }
    ).filter { it.isNotBlank() }

    val DEFAULT_FALLBACK_KEY: String
        get() = FALLBACK_KEYS.firstOrNull() ?: ""

    const val DEFAULT_MODEL = "nvidia/nemotron-3.5-lightning:free"
    private const val OPENROUTER_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()

    fun getEffectiveApiKeys(settings: LauncherSettings?): List<String> {
        val userKey = settings?.aiApiKey?.trim() ?: ""
        return if (userKey.isNotBlank()) {
            listOf(userKey) + FALLBACK_KEYS.filter { it != userKey }
        } else {
            FALLBACK_KEYS
        }
    }

    fun getEffectiveApiKey(settings: LauncherSettings?): String {
        return getEffectiveApiKeys(settings).firstOrNull() ?: ""
    }

    fun getEffectiveModel(settings: LauncherSettings?): String {
        val userModel = settings?.aiModel?.trim() ?: ""
        val isDeprecated = userModel.contains("qwen3.8-27b", ignoreCase = true) ||
                           userModel.contains("nemotron-3-ultra-550b", ignoreCase = true)
        return if (userModel.isNotBlank() && !isDeprecated) userModel else DEFAULT_MODEL
    }

    /**
     * Uji koneksi ke AI API (Google Gemini langsung atau OpenRouter) dengan failover ke key cadangan jika terkena limit.
     */
    suspend fun testConnection(apiKey: String, model: String): Result<String> = withContext(Dispatchers.IO) {
        val keysToTry = if (apiKey.isNotBlank()) listOf(apiKey.trim()) else FALLBACK_KEYS
        val m = if (model.isNotBlank()) model.trim() else DEFAULT_MODEL
        var lastError: Exception = Exception("Koneksi gagal.")

        for ((index, key) in keysToTry.withIndex()) {
            try {
                if (key.startsWith("AIzaSy")) {
                    val geminiModel = if (m.contains("gemini-")) {
                        m.substringAfter("google/").substringBefore(":").ifBlank { "gemini-1.5-flash" }
                    } else {
                        "gemini-1.5-flash"
                    }

                    val payload = JsonObject().apply {
                        add("contents", JsonArray().apply {
                            add(JsonObject().apply {
                                add("parts", JsonArray().apply {
                                    add(JsonObject().apply {
                                        addProperty("text", "Halo, tes koneksi. Jawab: OK")
                                    })
                                })
                            })
                        })
                    }

                    val request = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/$geminiModel:generateContent?key=$key")
                        .addHeader("Content-Type", "application/json")
                        .post(payload.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        val bodyStr = response.body?.string() ?: ""
                        if (!response.isSuccessful) {
                            val err = Exception("Google Gemini HTTP ${response.code}: ${parseErrorMessage(bodyStr)}")
                            if (response.code == 429 && index < keysToTry.size - 1) {
                                lastError = err
                                return@use
                            }
                            return@withContext Result.failure(err)
                        }
                        return@withContext Result.success("Koneksi berhasil ke Google Gemini API ($geminiModel)!")
                    }
                } else {
                    val payload = JsonObject().apply {
                        addProperty("model", m)
                        add("messages", JsonArray().apply {
                            add(JsonObject().apply {
                                addProperty("role", "user")
                                addProperty("content", "Jawab satu kata: OK")
                            })
                        })
                        addProperty("stream", false)
                    }

                    val request = Request.Builder()
                        .url(OPENROUTER_ENDPOINT)
                        .addHeader("Authorization", "Bearer $key")
                        .addHeader("HTTP-Referer", "https://nuxlauncher.site")
                        .addHeader("X-Title", "NUX Launcher")
                        .addHeader("Content-Type", "application/json")
                        .post(payload.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        val bodyStr = response.body?.string() ?: ""
                        if (!response.isSuccessful) {
                            val err = Exception("HTTP ${response.code}: ${parseErrorMessage(bodyStr)}")
                            if ((response.code == 429 || response.code == 402 || response.code == 403 || response.code == 503) && index < keysToTry.size - 1) {
                                lastError = err
                                return@use
                            }
                            return@withContext Result.failure(err)
                        }
                        val json = JsonParser.parseString(bodyStr).asJsonObject
                        val choices = json.getAsJsonArray("choices")
                        val reply = if (choices != null && choices.size() > 0) {
                            choices[0].asJsonObject.getAsJsonObject("message")?.get("content")?.asString ?: "OK"
                        } else {
                            "OK"
                        }
                        return@withContext Result.success("Koneksi berhasil! Model aktif: $m ($reply) [Key #${index + 1}]")
                    }
                }
            } catch (e: Exception) {
                lastError = e
                if (index < keysToTry.size - 1) {
                    continue
                }
            }
        }
        Result.failure(lastError)
    }

    /**
     * Menganalisis crash log secara real-time melalui Server-Sent Events (SSE) streaming dengan failover multi-key.
     */
    fun analyzeCrashStreaming(
        context: Context? = null,
        crashInfo: GameCrashInfo,
        settings: LauncherSettings?
    ): Flow<AIStreamState> = flow {
        if (context != null && !AICrashQuotaManager.hasQuota(context, settings)) {
            emit(AIStreamState.QuotaExceeded)
            return@flow
        }

        if (context != null) {
            val consumed = AICrashQuotaManager.consumeQuota(context, settings)
            if (!consumed) {
                emit(AIStreamState.QuotaExceeded)
                return@flow
            }
        }

        emit(AIStreamState.Connecting)

        val keysToTry = getEffectiveApiKeys(settings)
        val model = getEffectiveModel(settings)
        val logSnippet = extractMostRelevantLog(crashInfo)

        val modListStr = if (crashInfo.installedMods.isNotEmpty()) {
            crashInfo.installedMods.joinToString("\n") { "- $it" }
        } else {
            "- (Tidak ada mod / Vanilla)"
        }

        val systemPrompt = """
            Kamu adalah AI Ahli Diagnosa Crash Minecraft Java Edition pada platform Android (NUX Launcher / Pojav runtime).
            Tugasmu: Analisis error log, metadata versi, mod loader, dan daftar mod yang terpasang dengan teliti.

            ATURAN FORMAT OUTPUT WAJIB (Gunakan Pemformatan Markdown):

            ### 🏷️ STATUS MASALAH:
            Pilih TEPAT SATU dari dua label ini di baris pertama:
            - **[BISA DISELESAIKAN SENDIRI]**
              Gunakan jika masalah berasal dari: mod tidak kompatibel dengan versi Minecraft, Fabric/Forge API belum terpasang, mod bentrok/duplikat, Java runtime salah pilih, alokasi RAM kurang, opsi/shader/resource pack korup, atau kesalahan konfigurasi player.
            - **[BUG DEVELOPER / LAUNCHER]**
              Gunakan jika masalah berasal dari: bug internal native C/C++ engine peluncur (Pojav/NUX/Zalith), library Android NDK gagal load/missing, DEX error, segmentation fault pada GL4ES/MobileGlues renderer internal tanpa keterlibatan mod rusak, atau uncaught exception launcher itu sendiri.

            Jika statusnya **[BUG DEVELOPER / LAUNCHER]**, WAJIB sertakan instruksi ini tepat di bawahnya:
            > ⚠️ **Buka Tiket Pengaduan:** Masalah ini terindikasi sebagai bug internal peluncur/engine. Silakan laporkan dan buka tiket pengaduan di Discord resmi NUX Launcher: **https://discord.gg/UW4wBQg6X5** dengan menyertakan file log ini agar segera diperbaiki oleh developer.

            ---

            ### 🔍 PENYEBAB CRASH
            (Jelaskan secara ringkas, padat, dan akurat sumber masalahnya berdasarkan log dan mod terpasang. Sebutkan nama mod/library yang bersalah jika ada).

            ### 💡 LANGKAH PERBAIKAN
            (Berikan 1-3 langkah perbaikan yang jelas, berurutan, dan praktis menggunakan bullet list `- `).

            Hindari basa-basi seperti "Halo", "Semoga membantu", atau penutup bertele-tele. Langsung to the point.
        """.trimIndent()

        val userPrompt = """
            [INFORMASI INSTANCE & LINGKUNGAN]
            - Instance: ${crashInfo.instanceName}
            - Versi Minecraft: ${crashInfo.mcVersion}
            - Mod Loader: ${crashInfo.loader.uppercase()} ${if (!crashInfo.loaderVersion.isNullOrBlank()) "(${crashInfo.loaderVersion})" else ""}
            - Total Mod Terpasang: ${crashInfo.installedMods.size} mod
            - Daftar Mod Terpasang:
            $modListStr

            [METADATA CRASH]
            - Tipe Crash: ${if (crashInfo.isLauncherCrash) "Launcher Internal" else "Minecraft JVM Game"}
            - Exit Code: ${crashInfo.exitCode} (Signal: ${crashInfo.isSignal})
            - Status: ${crashInfo.getStatusBadgeText()}
            - Info: ${crashInfo.getMainMessage()}

            [CUPLIKAN LOG TERAKHIR]
            $logSnippet
        """.trimIndent()

        var streamSucceeded = false
        var lastErrorMessage = "Gagal menghubungi AI."

        for ((index, currentKey) in keysToTry.withIndex()) {
            if (currentKey.startsWith("AIzaSy")) {
                val geminiModel = if (model.contains("gemini-")) {
                    model.substringAfter("google/").substringBefore(":").ifBlank { "gemini-1.5-flash" }
                } else {
                    "gemini-1.5-flash"
                }

                val payload = JsonObject().apply {
                    add("system_instruction", JsonObject().apply {
                        add("parts", JsonArray().apply {
                            add(JsonObject().apply {
                                addProperty("text", systemPrompt)
                            })
                        })
                    })
                    add("contents", JsonArray().apply {
                        add(JsonObject().apply {
                            add("parts", JsonArray().apply {
                                add(JsonObject().apply {
                                    addProperty("text", userPrompt)
                                })
                            })
                        })
                    })
                }

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$geminiModel:streamGenerateContent?alt=sse&key=$currentKey")
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                try {
                    val response = httpClient.newCall(request).execute()
                    val code = response.code

                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        response.close()
                        val msg = parseErrorMessage(errBody)
                        if (code == 429 && index < keysToTry.size - 1) {
                            android.util.Log.w("AICrashAnalyzer", "Gemini Key #${index + 1} terkena limit (HTTP 429). Mengalihkan ke Key #${index + 2}...")
                            continue
                        }
                        lastErrorMessage = "Gagal menghubungi Google Gemini (HTTP $code): $msg"
                        if (index < keysToTry.size - 1) continue else break
                    }

                    val source = response.body?.source()
                    if (source == null) {
                        response.close()
                        if (index < keysToTry.size - 1) continue
                        lastErrorMessage = "Respon Google Gemini kosong."
                        break
                    }

                    val accumulatedText = StringBuilder()
                    response.use {
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            val trimmed = line.trim()
                            if (!trimmed.startsWith("data:")) continue
                            val data = trimmed.removePrefix("data:").trim()

                            try {
                                val chunkObj = JsonParser.parseString(data).asJsonObject
                                val candidates = chunkObj.getAsJsonArray("candidates")
                                if (candidates != null && candidates.size() > 0) {
                                    val content = candidates[0].asJsonObject.getAsJsonObject("content")
                                    val parts = content?.getAsJsonArray("parts")
                                    if (parts != null && parts.size() > 0) {
                                        val text = parts[0].asJsonObject.get("text")?.asString ?: ""
                                        if (text.isNotEmpty()) {
                                            accumulatedText.append(text)
                                            emit(AIStreamState.Streaming(accumulatedText.toString(), text))
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    if (accumulatedText.isNotEmpty()) {
                        emit(AIStreamState.Completed(accumulatedText.toString()))
                        streamSucceeded = true
                        break
                    } else {
                        if (index < keysToTry.size - 1) continue
                        lastErrorMessage = "Google Gemini tidak mengembalikan analisis untuk log ini."
                    }
                } catch (e: Exception) {
                    if (index < keysToTry.size - 1) {
                        android.util.Log.w("AICrashAnalyzer", "Koneksi Gemini Key #${index + 1} terputus (${e.message}). Mengalihkan ke Key #${index + 2}...")
                        continue
                    }
                    lastErrorMessage = "Koneksi Google Gemini terputus: ${e.localizedMessage ?: "Jaringan tidak stabil"}"
                }
            } else {
                // OpenRouter API with 3-key failover
                val payload = JsonObject().apply {
                    addProperty("model", model)
                    add("messages", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("role", "system")
                            addProperty("content", systemPrompt)
                        })
                        add(JsonObject().apply {
                            addProperty("role", "user")
                            addProperty("content", userPrompt)
                        })
                    })
                    addProperty("stream", true)
                }

                val request = Request.Builder()
                    .url(OPENROUTER_ENDPOINT)
                    .addHeader("Authorization", "Bearer $currentKey")
                    .addHeader("HTTP-Referer", "https://nuxlauncher.site")
                    .addHeader("X-Title", "NUX Launcher")
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                try {
                    val response = httpClient.newCall(request).execute()
                    val code = response.code

                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        response.close()
                        val msg = parseErrorMessage(errBody)

                        val isModelUnavailable = code == 404 ||
                                errBody.contains("unavailable", ignoreCase = true) ||
                                errBody.contains("No endpoints", ignoreCase = true)

                        if (isModelUnavailable && payload.get("model")?.asString != DEFAULT_MODEL) {
                            android.util.Log.w("AICrashAnalyzer", "Model ${payload.get("model")?.asString} tidak tersedia ($code), otomatis beralih ke $DEFAULT_MODEL...")
                            payload.addProperty("model", DEFAULT_MODEL)
                            val fbReq = Request.Builder()
                                .url(OPENROUTER_ENDPOINT)
                                .addHeader("Authorization", "Bearer $currentKey")
                                .addHeader("HTTP-Referer", "https://nuxlauncher.site")
                                .addHeader("X-Title", "NUX Launcher")
                                .addHeader("Content-Type", "application/json")
                                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                                .build()
                            val fbResp = httpClient.newCall(fbReq).execute()
                            if (fbResp.isSuccessful) {
                                val fbSource = fbResp.body?.source()
                                if (fbSource != null) {
                                    val fbAccumulated = StringBuilder()
                                    fbResp.use {
                                        while (!fbSource.exhausted()) {
                                            val fbLine = fbSource.readUtf8Line() ?: break
                                            val fbTrimmed = fbLine.trim()
                                            if (!fbTrimmed.startsWith("data:")) continue
                                            val fbData = fbTrimmed.removePrefix("data:").trim()
                                            if (fbData == "[DONE]") break
                                            try {
                                                val chunkObj = JsonParser.parseString(fbData).asJsonObject
                                                val choices = chunkObj.getAsJsonArray("choices")
                                                if (choices != null && choices.size() > 0) {
                                                    val delta = choices[0].asJsonObject.getAsJsonObject("delta")
                                                    if (delta != null && delta.has("content")) {
                                                        val contentChunk = delta.get("content").asString
                                                        if (!contentChunk.isNullOrEmpty()) {
                                                            fbAccumulated.append(contentChunk)
                                                            emit(AIStreamState.Streaming(fbAccumulated.toString(), contentChunk))
                                                        }
                                                    }
                                                }
                                            } catch (_: Exception) {}
                                        }
                                    }
                                    if (fbAccumulated.isNotEmpty()) {
                                        emit(AIStreamState.Completed(fbAccumulated.toString()))
                                        streamSucceeded = true
                                        break
                                    }
                                }
                            } else {
                                fbResp.close()
                            }
                        }

                        val isLimitOrRecoverable = code == 429 || code == 402 || code == 503 || code == 401 || code == 403

                        if (isLimitOrRecoverable && index < keysToTry.size - 1) {
                            android.util.Log.w("AICrashAnalyzer", "OpenRouter Key #${index + 1} terkena limit/error (HTTP $code: $msg). Mengalihkan otomatis ke Key #${index + 2}...")
                            continue // Failover to next key!
                        } else {
                            lastErrorMessage = "Gagal menghubungi AI (HTTP $code): $msg"
                            if (index < keysToTry.size - 1) continue else break
                        }
                    }

                    val source = response.body?.source()
                    if (source == null) {
                        response.close()
                        if (index < keysToTry.size - 1) continue
                        lastErrorMessage = "Respon server AI kosong."
                        break
                    }

                    val accumulatedText = StringBuilder()
                    response.use {
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            val trimmed = line.trim()
                            if (!trimmed.startsWith("data:")) continue

                            val data = trimmed.removePrefix("data:").trim()
                            if (data == "[DONE]") {
                                break
                            }

                            try {
                                val chunkObj = JsonParser.parseString(data).asJsonObject
                                val choices = chunkObj.getAsJsonArray("choices")
                                if (choices != null && choices.size() > 0) {
                                    val delta = choices[0].asJsonObject.getAsJsonObject("delta")
                                    if (delta != null && delta.has("content")) {
                                        val contentChunk = delta.get("content").asString
                                        if (!contentChunk.isNullOrEmpty()) {
                                            accumulatedText.append(contentChunk)
                                            emit(AIStreamState.Streaming(accumulatedText.toString(), contentChunk))
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    if (accumulatedText.isNotEmpty()) {
                        emit(AIStreamState.Completed(accumulatedText.toString()))
                        streamSucceeded = true
                        break // Succeeded!
                    } else {
                        if (index < keysToTry.size - 1) {
                            continue
                        }
                        lastErrorMessage = "AI tidak mengembalikan analisis untuk log ini."
                    }
                } catch (e: Exception) {
                    if (index < keysToTry.size - 1) {
                        android.util.Log.w("AICrashAnalyzer", "Koneksi Key #${index + 1} terputus (${e.message}). Mengalihkan ke Key #${index + 2}...")
                        continue
                    }
                    lastErrorMessage = "Koneksi terputus: ${e.localizedMessage ?: "Jaringan tidak stabil"}"
                }
            }
        }

        if (!streamSucceeded) {
            emit(AIStreamState.Error(lastErrorMessage))
        }
    }.flowOn(Dispatchers.IO)

    private fun extractMostRelevantLog(crashInfo: GameCrashInfo): String {
        // 1. Coba baca file crash report langsung jika ada
        if (!crashInfo.crashReportPath.isNullOrBlank()) {
            val reportFile = File(crashInfo.crashReportPath)
            if (reportFile.exists() && reportFile.isFile) {
                runCatching {
                    val text = reportFile.readText(Charsets.UTF_8)
                    return text.take(6000)
                }
            }
        }

        // 2. Coba baca file log lengkap jika ada
        if (crashInfo.fullLogPath.isNotBlank()) {
            val logFile = File(crashInfo.fullLogPath)
            if (logFile.exists() && logFile.isFile) {
                runCatching {
                    val lines = logFile.readLines(Charsets.UTF_8)
                    val relevantLines = lines.takeLast(120)
                    return relevantLines.joinToString("\n").takeLast(6000)
                }
            }
        }

        // 3. Fallback ke log snippet yang sudah ada
        return crashInfo.logSnippet.takeLast(5000)
    }

    private fun parseErrorMessage(jsonBody: String): String {
        return try {
            val obj = JsonParser.parseString(jsonBody).asJsonObject
            if (obj.has("error")) {
                val err = obj.get("error")
                if (err.isJsonObject) {
                    err.asJsonObject.get("message")?.asString ?: err.toString()
                } else {
                    err.asString
                }
            } else {
                jsonBody.take(150)
            }
        } catch (_: Exception) {
            jsonBody.take(150)
        }
    }
}
