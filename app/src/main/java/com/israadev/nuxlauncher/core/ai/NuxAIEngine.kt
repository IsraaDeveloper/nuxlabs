package com.israadev.nuxlauncher.core.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.israadev.nuxlauncher.core.crash.AICrashAnalyzer
import com.israadev.nuxlauncher.core.crash.AICrashQuotaManager
import com.israadev.nuxlauncher.core.crash.AIStreamState
import com.israadev.nuxlauncher.core.instance.InstanceManager
import com.israadev.nuxlauncher.core.models.Instance
import com.israadev.nuxlauncher.core.models.LauncherSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

data class AIChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" or "assistant" or "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

object NuxAIEngine {

    private val gson = Gson()
    private const val OPENROUTER_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Mengumpulkan ringkasan metadata instance aktif, mod, renderer, dan log terakhir
     * untuk diberikan sebagai konteks akurat bagi AI.
     */
    fun collectInstanceContext(
        context: Context,
        instance: Instance?,
        settings: LauncherSettings
    ): String {
        val sb = StringBuilder()

        // 1. Informasi Perangkat HP
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val totalRamGb = memInfo.totalMem / (1024 * 1024 * 1024)

        sb.appendLine("=== KONTEKS PERANGKAT PENGGUNA ===")
        sb.appendLine("- Perangkat: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
        sb.appendLine("- RAM Fisik HP: ~$totalRamGb GB | Alokasi RAM Game: ${settings.ramMb} MB")
        sb.appendLine("- Renderer Terpilih: ${settings.selectedRenderer.uppercase()}")
        sb.appendLine("- Vulkan Driver: ${settings.vulkanDriver}")
        sb.appendLine("- Graphics API: ${settings.graphicsApi}")
        sb.appendLine()

        // 2. Informasi Instance Minecraft
        if (instance != null) {
            val gameDir = InstanceManager.getInstanceGameDir(context, instance)
            val modsDir = File(gameDir, "mods")
            val installedModFiles = if (modsDir.exists() && modsDir.isDirectory) {
                modsDir.listFiles { f -> f.isFile && f.name.endsWith(".jar", ignoreCase = true) }?.map { it.name } ?: emptyList()
            } else {
                emptyList()
            }

            sb.appendLine("=== KONTEKS INSTANCE AKTIF ===")
            sb.appendLine("- Nama Instance: ${instance.name}")
            sb.appendLine("- Versi Minecraft: ${instance.mcVersion}")
            sb.appendLine("- Mod Loader: ${instance.loader.uppercase()} ${if (instance.loaderVersion.isNotBlank()) "(${instance.loaderVersion})" else ""}")
            sb.appendLine("- Jumlah Mod Terpasang: ${installedModFiles.size} mod")
            if (installedModFiles.isNotEmpty()) {
                val modPreview = installedModFiles.take(30).joinToString(", ")
                sb.appendLine("- Daftar Mod (sampel 30 mod): $modPreview")
            } else {
                sb.appendLine("- Daftar Mod: Tidak ada mod (Vanilla)")
            }
            sb.appendLine()

            // 3. Cuplikan Log Terakhir (latestlog.txt atau logs/latest.log)
            val logCandidates = listOf(
                File(context.filesDir, "latestlog.txt"),
                File(gameDir, "logs/latest.log")
            )
            val logFile = logCandidates.firstOrNull { it.exists() && it.length() > 0 }
            if (logFile != null) {
                runCatching {
                    val allLines = logFile.readLines(Charsets.UTF_8)
                    // Ambil 40 baris terakhir atau yang mengandung error/warning
                    val errorLines = allLines.filter { line ->
                        line.contains("ERROR", ignoreCase = true) ||
                        line.contains("WARN", ignoreCase = true) ||
                        line.contains("Exception", ignoreCase = true) ||
                        line.contains("Caused by:", ignoreCase = true) ||
                        line.contains("Crash", ignoreCase = true)
                    }.takeLast(20)

                    val tailLines = allLines.takeLast(25)
                    val combinedSnippet = (errorLines + tailLines).distinct().joinToString("\n")

                    if (combinedSnippet.isNotBlank()) {
                        sb.appendLine("=== CUPLIKAN LOG GAME TERAKHIR ===")
                        sb.appendLine(combinedSnippet.take(2500))
                        sb.appendLine()
                    }
                }
            }
        } else {
            sb.appendLine("=== KONTEKS INSTANCE ===")
            sb.appendLine("- Tidak ada instance aktif yang dipilih.")
            sb.appendLine()
        }

        return sb.toString().trim()
    }

    /**
     * Mengalirkan respon interaktif obrolan AI dengan multi-turn context dan OpenRouter SSE streaming.
     */
    fun streamChat(
        context: Context,
        chatHistory: List<AIChatMessage>,
        instanceContext: String,
        settings: LauncherSettings
    ): Flow<AIStreamState> = flow {
        // Cek kuota sebelum memproses
        if (!AICrashQuotaManager.hasQuota(context, settings)) {
            emit(AIStreamState.QuotaExceeded)
            return@flow
        }

        // Konsumsi 1 kuota
        val consumed = AICrashQuotaManager.consumeQuota(context, settings)
        if (!consumed) {
            emit(AIStreamState.QuotaExceeded)
            return@flow
        }

        emit(AIStreamState.Connecting)

        val keysToTry = AICrashAnalyzer.getEffectiveApiKeys(settings)
        val initialModel = AICrashAnalyzer.getEffectiveModel(settings)
        val candidateModels = if (initialModel != AICrashAnalyzer.DEFAULT_MODEL) {
            listOf(initialModel, AICrashAnalyzer.DEFAULT_MODEL)
        } else {
            listOf(initialModel)
        }

        val systemInstruction = """
            Kamu adalah NUX AI, Asisten Pintar & Diagnostic Engineer resmi NUX Launcher (Minecraft Java Edition di Android via Pojav runtime).
            
            Karakter dan Gaya Bicara:
            - Berpengetahuan luas tentang Minecraft Java, Modding (Fabric, Forge, NeoForge, Quilt), Shaders (Iris, Oculus), dan Optimasi Renderer Mobile (GL4ES, Holy GL4ES, Angle, Zink, Freedreno/Turnip).
            - Gaya bahasa profesional, ramah, to the point, dan solutif.
            - Gunakan pemformatan Markdown yang sangat rapi (gunakan bold `**`, bullet list `- `, heading `###`, dan code block ``` jika menyebutkan nama mod, command, path file, atau error log).
            - Jika user bertanya tentang crash atau masalah performa, manfaatkan konteks instance, daftar mod, renderer, dan cuplikan log terakhir yang diberikan di bawah ini untuk diagnosa yang akurat.
            - Bila user bertanya hal umum di luar Minecraft atau tentang game, jawablah dengan cerdas dan bersahabat.
            
            [DATA LINGKUNGAN INSTANCE PENGGUNA SAAT INI]:
            $instanceContext
        """.trimIndent()

        // Siapkan messages payload JSON
        val messagesArray = JsonArray().apply {
            // System message
            add(JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", systemInstruction)
            })
            // Riwayat obrolan (maksimal 10 percakapan terakhir agar efisien)
            chatHistory.takeLast(10).forEach { msg ->
                add(JsonObject().apply {
                    addProperty("role", msg.role)
                    addProperty("content", msg.content)
                })
            }
        }

        val payload = JsonObject().apply {
            addProperty("model", candidateModels.first())
            add("messages", messagesArray)
            addProperty("stream", true)
        }

        var streamSucceeded = false
        var lastErrorMessage = "Gagal menghubungi AI server."

        outer@ for (modelCandidate in candidateModels) {
            payload.addProperty("model", modelCandidate)

            for ((index, currentKey) in keysToTry.withIndex()) {
                val request = Request.Builder()
                    .url(OPENROUTER_ENDPOINT)
                    .addHeader("Authorization", "Bearer $currentKey")
                    .addHeader("HTTP-Referer", "https://nuxlauncher.site")
                    .addHeader("X-Title", "NUX Launcher Mobile")
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                try {
                    val response = httpClient.newCall(request).execute()
                    val code = response.code

                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        response.close()

                        val isModelUnavailable = code == 404 ||
                                errBody.contains("unavailable", ignoreCase = true) ||
                                errBody.contains("No endpoints", ignoreCase = true)

                        if (isModelUnavailable && modelCandidate != AICrashAnalyzer.DEFAULT_MODEL) {
                            android.util.Log.w("NuxAIEngine", "Model $modelCandidate tidak tersedia ($code), otomatis beralih ke model cadangan ${AICrashAnalyzer.DEFAULT_MODEL}...")
                            lastErrorMessage = parseErrorMessage(errBody)
                            break // Langsung beralih ke kandidat model berikutnya di outer loop
                        }

                        val isLimit = code == 429 || code == 402 || code == 503 || code == 401 || code == 403
                        if (isLimit && index < keysToTry.size - 1) {
                            android.util.Log.w("NuxAIEngine", "Key #${index + 1} terkena limit ($code). Mengalihkan ke Key cadangan #${index + 2}...")
                            continue
                        } else {
                            lastErrorMessage = "Gagal menghubungi server AI (HTTP $code): ${parseErrorMessage(errBody)}"
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
                            if (data == "[DONE]") break

                            try {
                                val chunkJson = JsonParser.parseString(data).asJsonObject
                                val choices = chunkJson.getAsJsonArray("choices")
                                if (choices != null && choices.size() > 0) {
                                    val delta = choices[0].asJsonObject.getAsJsonObject("delta")
                                    val textChunk = delta?.get("content")?.asString ?: ""
                                    if (textChunk.isNotEmpty()) {
                                        accumulatedText.append(textChunk)
                                        emit(AIStreamState.Streaming(accumulatedText.toString(), textChunk))
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    if (accumulatedText.isNotEmpty()) {
                        emit(AIStreamState.Completed(accumulatedText.toString()))
                        streamSucceeded = true
                        break@outer
                    } else {
                        if (index < keysToTry.size - 1) continue
                        lastErrorMessage = "AI tidak mengembalikan respon untuk pertanyaan ini."
                    }
                } catch (e: Exception) {
                    if (index < keysToTry.size - 1) {
                        android.util.Log.w("NuxAIEngine", "Koneksi Key #${index + 1} terputus (${e.message}). Mengalihkan ke Key #${index + 2}...")
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

    private fun parseErrorMessage(jsonStr: String): String {
        return try {
            val json = JsonParser.parseString(jsonStr).asJsonObject
            if (json.has("error")) {
                val err = json.get("error")
                if (err.isJsonObject) {
                    err.asJsonObject.get("message")?.asString ?: jsonStr
                } else {
                    err.asString
                }
            } else {
                jsonStr
            }
        } catch (_: Exception) {
            jsonStr.ifBlank { "Kesalahan jaringan" }
        }
    }
}
