package com.israadev.nuxlauncher.core.crash

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.israadev.nuxlauncher.core.models.LauncherSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
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
}

object AICrashAnalyzer {
    private val DEFAULT_FALLBACK_KEY: String by lazy {
        try {
            String(android.util.Base64.decode("c2stb3ItdjEtYmZiNjRiZTQyMWJlNTU0NjUwY2NlYjAzYjIwMzMyNDAwM2M4MWZmYjIxMmQ1N2Q0ZWM3Y2JlMWVmZWQzMDQ0OQ==", android.util.Base64.NO_WRAP), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }
    const val DEFAULT_MODEL = "qwen/qwen3.8-27b:free"
    private const val OPENROUTER_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()

    fun getEffectiveApiKey(settings: LauncherSettings?): String {
        val userKey = settings?.aiApiKey?.trim() ?: ""
        return if (userKey.isNotBlank()) userKey else DEFAULT_FALLBACK_KEY
    }

    fun getEffectiveModel(settings: LauncherSettings?): String {
        val userModel = settings?.aiModel?.trim() ?: ""
        return if (userModel.isNotBlank()) userModel else DEFAULT_MODEL
    }

    /**
     * Uji koneksi ke AI API (Google Gemini langsung atau OpenRouter) dengan API key dan model yang diberikan.
     */
    suspend fun testConnection(apiKey: String, model: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val key = if (apiKey.isNotBlank()) apiKey.trim() else DEFAULT_FALLBACK_KEY
            val m = if (model.isNotBlank()) model.trim() else DEFAULT_MODEL

            if (key.startsWith("AIzaSy")) {
                // Direct Google Gemini API
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
                        return@withContext Result.failure(
                            Exception("Google Gemini HTTP ${response.code}: ${parseErrorMessage(bodyStr)}")
                        )
                    }
                    Result.success("Koneksi berhasil ke Google Gemini API ($geminiModel)!")
                }
            } else {
                // OpenRouter API
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
                        return@withContext Result.failure(
                            Exception("HTTP ${response.code}: ${parseErrorMessage(bodyStr)}")
                        )
                    }
                    val json = JsonParser.parseString(bodyStr).asJsonObject
                    val choices = json.getAsJsonArray("choices")
                    if (choices != null && choices.size() > 0) {
                        val reply = choices[0].asJsonObject.getAsJsonObject("message")?.get("content")?.asString ?: "OK"
                        Result.success("Koneksi berhasil! Model aktif: $m ($reply)")
                    } else {
                        Result.success("Koneksi terhubung ke OpenRouter ($m).")
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Menganalisis crash log secara real-time melalui Server-Sent Events (SSE) streaming.
     */
    fun analyzeCrashStreaming(
        crashInfo: GameCrashInfo,
        settings: LauncherSettings?
    ): Flow<AIStreamState> = flow {
        emit(AIStreamState.Connecting)

        val apiKey = getEffectiveApiKey(settings)
        val model = getEffectiveModel(settings)

        val logSnippet = extractMostRelevantLog(crashInfo)

        val systemPrompt = """
            Kamu adalah AI Ahli Analisis Crash Minecraft (Android/Pojav/NUX Launcher).
            Tugasmu: Analisis log error berikut, berikan penjelasan singkat, padat, dan solutif dalam Bahasa Indonesia.

            Format Output Wajib:
            🔍 PENYEBAB CRASH:
            (1-2 kalimat ringkas menjelaskan sumber masalah, misal: mod tidak kompatibel, memori/RAM kurang, renderer Vulkan/Zink error, Java version tidak cocok, atau class not found)

            💡 SOLUSI REKOMENDASI:
            • (Langkah perbaikan praktis 1)
            • (Langkah perbaikan praktis 2)
            • (Langkah perbaikan praktis 3 jika relevan)

            Jangan gunakan kalimat basa-basi seperti "Halo" atau "Semoga membantu". Langsung ke poin.
        """.trimIndent()

        val userPrompt = """
            [METADATA CRASH]
            - Exit Code: ${crashInfo.exitCode} (Signal: ${crashInfo.isSignal})
            - Status: ${crashInfo.getStatusBadgeText()}
            - Info: ${crashInfo.getMainMessage()}

            [CUPLIKAN LOG TERAKHIR]
            $logSnippet
        """.trimIndent()

        if (apiKey.startsWith("AIzaSy")) {
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
                .url("https://generativelanguage.googleapis.com/v1beta/models/$geminiModel:streamGenerateContent?alt=sse&key=$apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val accumulatedText = StringBuilder()
            try {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        val msg = parseErrorMessage(errBody)
                        emit(AIStreamState.Error("Gagal menghubungi Google Gemini (HTTP ${response.code}): $msg"))
                        return@use
                    }

                    val source = response.body?.source() ?: run {
                        emit(AIStreamState.Error("Respon Google Gemini kosong."))
                        return@use
                    }

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

                    if (accumulatedText.isNotEmpty()) {
                        emit(AIStreamState.Completed(accumulatedText.toString()))
                    } else {
                        emit(AIStreamState.Error("Google Gemini tidak mengembalikan analisis untuk log ini."))
                    }
                }
            } catch (e: Exception) {
                emit(AIStreamState.Error("Koneksi Google Gemini terputus: ${e.localizedMessage ?: "Jaringan tidak stabil"}"))
            }
            return@flow
        }

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
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://nuxlauncher.site")
            .addHeader("X-Title", "NUX Launcher")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val accumulatedText = StringBuilder()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    val msg = parseErrorMessage(errBody)
                    emit(AIStreamState.Error("Gagal menghubungi AI (HTTP ${response.code}): $msg"))
                    return@use
                }

                val source = response.body?.source() ?: run {
                    emit(AIStreamState.Error("Respon server AI kosong."))
                    return@use
                }

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
                    } catch (_: Exception) {
                        // Skip malformed chunk
                    }
                }

                if (accumulatedText.isNotEmpty()) {
                    emit(AIStreamState.Completed(accumulatedText.toString()))
                } else {
                    emit(AIStreamState.Error("AI tidak mengembalikan analisis untuk log ini."))
                }
            }
        } catch (e: Exception) {
            emit(AIStreamState.Error("Koneksi terputus: ${e.localizedMessage ?: "Jaringan tidak stabil"}"))
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
