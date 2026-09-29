package com.israadev.nuxlauncher.core.skin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

import android.util.LruCache
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.models.UserAccount
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class CapePreset(
    val id: String,
    val name: String,
    val description: String,
    val badgeColor: Long
)

object SkinUtils {

    private val headCache = LruCache<String, Bitmap>(60)

    val PRESET_CAPES = listOf(
        CapePreset("none", "Tanpa Cape", "Reset / Lepas cape", 0xFF6B7280),
        CapePreset("mojang_red", "Mojang Studios", "Klasik merah resmi Mojang", 0xFFEF4444),
        CapePreset("anniversary_15", "15th Anniversary", "Creeper emas peringatan 15 tahun", 0xFF10B981),
        CapePreset("migrator", "Migrator Cape", "Jubah biru kehormatan migrasi", 0xFF3B82F6),
        CapePreset("cherry_blossom", "Cherry Blossom", "Motif sakura edisi 1.20", 0xFFEC4899),
        CapePreset("founders", "Founder's Cape", "Edisi emas pencipta", 0xFFF59E0B),
        CapePreset("vanilla", "Vanilla Cape", "Edisi Java & Bedrock bundle", 0xFF8B5CF6)
    )

    fun getSkinsDir(context: Context): File {
        val dir = File(context.filesDir, "skins")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getCapesDir(context: Context): File {
        val dir = File(context.filesDir, "capes")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getSkinFileForAccount(context: Context, accountId: String): File {
        return File(getSkinsDir(context), "${accountId}_skin.png")
    }

    fun getCapeFileForAccount(context: Context, accountId: String): File {
        return File(getCapesDir(context), "${accountId}_cape.png")
    }

    fun resolveSkinFile(context: Context, account: UserAccount): File? {
        account.getSkinFile()?.let { if (it.exists() && it.length() > 0) return it }
        val defaultFile = getSkinFileForAccount(context, account.id)
        return if (defaultFile.exists() && defaultFile.length() > 0) defaultFile else null
    }

    fun resolveCapeFile(context: Context, account: UserAccount): File? {
        account.getCapeFile()?.let { if (it.exists() && it.length() > 0) return it }
        val defaultFile = getCapeFileForAccount(context, account.id)
        return if (defaultFile.exists() && defaultFile.length() > 0) defaultFile else null
    }

    fun invalidateHeadCache(accountId: String? = null) {
        headCache.evictAll()
    }

    /**
     * Extracts a 2D Minecraft face avatar by compositing:
     * - Base Head layer: Rect(8, 8, 16, 16)
     * - Hat / Helmet outer layer: Rect(40, 8, 48, 16)
     * Scales up with nearest-neighbor interpolation to preserve crisp Minecraft pixel art.
     */
    fun extractHeadFromSkin(skinBitmap: Bitmap, targetSize: Int = 128): Bitmap {
        val headBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(headBitmap)
        val paint = Paint().apply {
            isFilterBitmap = false // Nearest-neighbor: essential for sharp pixel art
            isAntiAlias = false
            isDither = false
        }

        val scale = (skinBitmap.width / 64).coerceAtLeast(1)
        val dst = Rect(0, 0, targetSize, targetSize)

        // 1. Base head front face
        val srcHead = Rect(8 * scale, 8 * scale, 16 * scale, 16 * scale)
        canvas.drawBitmap(skinBitmap, srcHead, dst, paint)

        // 2. Outer hat/helmet layer
        if (skinBitmap.width >= 48 * scale && skinBitmap.height >= 16 * scale) {
            val srcHat = Rect(40 * scale, 8 * scale, 48 * scale, 16 * scale)
            canvas.drawBitmap(skinBitmap, srcHat, dst, paint)
        }

        return headBitmap
    }

    fun getSteveHeadBitmap(context: Context, targetSize: Int = 128): Bitmap {
        val cacheKey = "steve_head_$targetSize"
        headCache.get(cacheKey)?.let { return it }

        val bitmap = runCatching {
            context.assets.open("steve.png").use { stream ->
                BitmapFactory.decodeStream(stream)
            }?.let { steveSkin ->
                try {
                    extractHeadFromSkin(steveSkin, targetSize)
                } finally {
                    steveSkin.recycle()
                }
            }
        }.getOrNull()

        val finalBitmap = bitmap ?: Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888).also { b ->
            Canvas(b).drawColor(Color.parseColor("#805335"))
        }
        headCache.put(cacheKey, finalBitmap)
        return finalBitmap
    }

    suspend fun getAccountHeadBitmap(
        context: Context,
        account: UserAccount,
        targetSize: Int = 128
    ): Bitmap = withContext(Dispatchers.IO) {
        val skinFile = resolveSkinFile(context, account)
        val fileModified = skinFile?.takeIf { it.exists() }?.lastModified() ?: 0L
        val cacheKey = "${account.safeAccountType}_${account.id}_${fileModified}_$targetSize"

        headCache.get(cacheKey)?.let { return@withContext it }

        // A. If local skin file exists, decode and extract head
        if (skinFile != null && skinFile.exists() && skinFile.length() > 0) {
            val skinBitmap = BitmapFactory.decodeFile(skinFile.absolutePath)
            if (skinBitmap != null) {
                val head = extractHeadFromSkin(skinBitmap, targetSize)
                skinBitmap.recycle()
                headCache.put(cacheKey, head)
                return@withContext head
            }
        }

        // B. If Ely.by account, ensure skin file is downloaded and cached
        if (account.safeAccountType == "elyby") {
            val cachedAcc = ensureSkinAndCapeCached(context, account)
            val downloadedSkinFile = resolveSkinFile(context, cachedAcc)
            if (downloadedSkinFile != null && downloadedSkinFile.exists() && downloadedSkinFile.length() > 0) {
                val skinBitmap = BitmapFactory.decodeFile(downloadedSkinFile.absolutePath)
                if (skinBitmap != null) {
                    val head = extractHeadFromSkin(skinBitmap, targetSize)
                    skinBitmap.recycle()
                    val newKey = "${account.safeAccountType}_${account.id}_${downloadedSkinFile.lastModified()}_$targetSize"
                    headCache.put(newKey, head)
                    return@withContext head
                }
            }
        }

        // C. If Microsoft account, fetch head from mc-heads.net
        if (account.safeAccountType == "microsoft" && account.uuid.isNotBlank()) {
            runCatching {
                val client = OkHttpClient()
                val req = Request.Builder().url("https://mc-heads.net/avatar/${account.uuid}/$targetSize").build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        resp.body?.byteStream()?.use { stream ->
                            BitmapFactory.decodeStream(stream)?.let { bmp ->
                                headCache.put(cacheKey, bmp)
                                return@withContext bmp
                            }
                        }
                    }
                }
            }
        }

        // D. Fallback to Steve head
        getSteveHeadBitmap(context, targetSize)
    }

    suspend fun ensureSkinAndCapeCached(context: Context, account: UserAccount): UserAccount = withContext(Dispatchers.IO) {
        var updatedAccount = account
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        if (account.safeAccountType == "elyby") {
            val skinFile = getSkinFileForAccount(context, account.id)
            // Download skin if missing or empty
            if (!skinFile.exists() || skinFile.length() <= 0) {
                runCatching {
                    val skinUrl = "https://skinsystem.ely.by/skins/${account.username}.png"
                    val req = Request.Builder().url(skinUrl).build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful && resp.body != null) {
                            resp.body!!.byteStream().use { input ->
                                FileOutputStream(skinFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
            }

            // Check cape / cloak
            val capeFile = getCapeFileForAccount(context, account.id)
            runCatching {
                val capeUrl = "https://skinsystem.ely.by/cloaks/${account.username}.png"
                val req = Request.Builder().url(capeUrl).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful && resp.body != null) {
                        val bytes = resp.body!!.bytes()
                        if (bytes.isNotEmpty()) {
                            FileOutputStream(capeFile).use { out ->
                                out.write(bytes)
                            }
                        } else {
                            if (capeFile.exists()) capeFile.delete()
                        }
                    } else {
                        if (capeFile.exists()) capeFile.delete()
                    }
                }
            }.onFailure {
                if (capeFile.exists()) capeFile.delete()
            }

            // Detect model (classic vs slim)
            val isSlim = if (skinFile.exists()) isSlimModel(skinFile) else false
            val finalSkinPath = if (skinFile.exists() && skinFile.length() > 0) skinFile.absolutePath else null
            val finalCapePath = if (capeFile.exists() && capeFile.length() > 0) capeFile.absolutePath else null

            updatedAccount = updatedAccount.copy(
                customSkinPath = finalSkinPath,
                customCapePath = finalCapePath,
                skinModel = if (isSlim) "slim" else "classic",
                skinUrl = "https://skinsystem.ely.by/skins/${account.username}.png"
            )

            if (updatedAccount != account) {
                AccountManager.updateAccount(context, updatedAccount)
            }
        } else if (account.isOffline) {
            val skinFile = getSkinFileForAccount(context, account.id)
            val capeFile = getCapeFileForAccount(context, account.id)

            var changed = false
            var newSkinPath = account.customSkinPath
            var newCapePath = account.customCapePath

            if (skinFile.exists() && skinFile.length() > 0 && account.customSkinPath.isNullOrBlank()) {
                newSkinPath = skinFile.absolutePath
                changed = true
            }
            if (capeFile.exists() && capeFile.length() > 0 && account.customCapePath.isNullOrBlank()) {
                newCapePath = capeFile.absolutePath
                changed = true
            }

            if (changed) {
                updatedAccount = updatedAccount.copy(
                    customSkinPath = newSkinPath,
                    customCapePath = newCapePath
                )
                AccountManager.updateAccount(context, updatedAccount)
            }
        }

        updatedAccount
    }

    suspend fun validateSkinFile(file: File): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() <= 0) return@withContext false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        (options.outWidth == 64 && options.outHeight == 64) || (options.outWidth == 64 && options.outHeight == 32)
    }

    suspend fun validateCapeFile(file: File): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() <= 0) return@withContext false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        (options.outWidth == 64 && options.outHeight == 32) || (options.outWidth == 128 && options.outHeight == 64)
    }

    suspend fun isSlimModel(file: File): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth != 64 || options.outHeight != 64) {
            return@withContext false
        }

        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext false
        try {
            var isTransparent = true
            for (x in 54..55) {
                for (y in 20..31) {
                    if (bitmap.getPixel(x, y) ushr 24 != 0) {
                        isTransparent = false
                        break
                    }
                }
                if (!isTransparent) break
            }
            isTransparent
        } catch (_: Exception) {
            false
        } finally {
            bitmap.recycle()
        }
    }

    suspend fun importSkinFromUri(context: Context, accountId: String, uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val targetFile = getSkinFileForAccount(context, accountId)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Gagal membaca file dari penyimpanan.")

            if (!validateSkinFile(targetFile)) {
                targetFile.delete()
                throw Exception("Format skin tidak valid. Ukuran harus 64x64 atau 64x32 piksel.")
            }
            invalidateHeadCache(accountId)
            targetFile
        }
    }

    suspend fun importCapeFromUri(context: Context, accountId: String, uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val targetFile = getCapeFileForAccount(context, accountId)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Gagal membaca file dari penyimpanan.")

            if (!validateCapeFile(targetFile)) {
                targetFile.delete()
                throw Exception("Format cape tidak valid. Ukuran harus 64x32 atau 128x64 piksel.")
            }
            targetFile
        }
    }

    fun createPresetCape(presetId: String): Bitmap? {
        if (presetId == "none") return null

        val bitmap = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (presetId) {
            "mojang_red" -> {
                paint.color = Color.parseColor("#B31217")
                canvas.drawRect(Rect(0, 0, 22, 17), paint)
                paint.color = Color.WHITE
                canvas.drawRect(Rect(3, 5, 8, 7), paint)
                canvas.drawRect(Rect(3, 7, 5, 12), paint)
                canvas.drawRect(Rect(6, 7, 8, 12), paint)
                canvas.drawRect(Rect(3, 12, 8, 14), paint)
                canvas.drawRect(Rect(14, 5, 19, 7), paint)
                canvas.drawRect(Rect(14, 7, 16, 12), paint)
                canvas.drawRect(Rect(17, 7, 19, 12), paint)
                canvas.drawRect(Rect(14, 12, 19, 14), paint)
            }
            "anniversary_15" -> {
                paint.color = Color.parseColor("#15803D")
                canvas.drawRect(Rect(0, 0, 22, 17), paint)
                paint.color = Color.parseColor("#FBBF24")
                canvas.drawRect(Rect(3, 4, 5, 6), paint)
                canvas.drawRect(Rect(7, 4, 9, 6), paint)
                canvas.drawRect(Rect(5, 6, 7, 9), paint)
                canvas.drawRect(Rect(4, 8, 8, 10), paint)
                canvas.drawRect(Rect(3, 9, 5, 12), paint)
                canvas.drawRect(Rect(7, 9, 9, 12), paint)
                canvas.drawRect(Rect(14, 4, 16, 6), paint)
                canvas.drawRect(Rect(18, 4, 20, 6), paint)
                canvas.drawRect(Rect(16, 6, 18, 9), paint)
                canvas.drawRect(Rect(15, 8, 19, 10), paint)
                canvas.drawRect(Rect(14, 9, 16, 12), paint)
                canvas.drawRect(Rect(18, 9, 20, 12), paint)
            }
            "migrator" -> {
                paint.color = Color.parseColor("#1E3A8A")
                canvas.drawRect(Rect(0, 0, 22, 17), paint)
                paint.color = Color.parseColor("#DC2626")
                canvas.drawRect(Rect(3, 4, 9, 12), paint)
                canvas.drawRect(Rect(14, 4, 20, 12), paint)
                paint.color = Color.parseColor("#F59E0B")
                canvas.drawRect(Rect(5, 6, 7, 10), paint)
                canvas.drawRect(Rect(16, 6, 18, 10), paint)
            }
            "cherry_blossom" -> {
                paint.color = Color.parseColor("#F472B6")
                canvas.drawRect(Rect(0, 0, 22, 17), paint)
                paint.color = Color.WHITE
                canvas.drawRect(Rect(4, 4, 6, 6), paint)
                canvas.drawRect(Rect(7, 8, 9, 10), paint)
                canvas.drawRect(Rect(3, 11, 5, 13), paint)
                canvas.drawRect(Rect(15, 4, 17, 6), paint)
                canvas.drawRect(Rect(18, 8, 20, 10), paint)
                canvas.drawRect(Rect(14, 11, 16, 13), paint)
            }
            "founders" -> {
                paint.color = Color.parseColor("#D97706")
                canvas.drawRect(Rect(0, 0, 22, 17), paint)
                paint.color = Color.parseColor("#FEF08A")
                canvas.drawRect(Rect(3, 5, 9, 7), paint)
                canvas.drawRect(Rect(5, 7, 7, 13), paint)
                canvas.drawRect(Rect(14, 5, 20, 7), paint)
                canvas.drawRect(Rect(16, 7, 18, 13), paint)
            }
            "vanilla" -> {
                paint.color = Color.parseColor("#4A3728")
                canvas.drawRect(Rect(0, 0, 22, 17), paint)
                paint.color = Color.parseColor("#FDE047")
                canvas.drawRect(Rect(5, 6, 7, 10), paint)
                canvas.drawRect(Rect(3, 8, 9, 9), paint)
                canvas.drawRect(Rect(16, 6, 18, 10), paint)
                canvas.drawRect(Rect(14, 8, 20, 9), paint)
            }
            else -> return null
        }

        return bitmap
    }

    suspend fun applyPresetCape(context: Context, accountId: String, presetId: String): File? = withContext(Dispatchers.IO) {
        val targetFile = getCapeFileForAccount(context, accountId)
        if (presetId == "none") {
            if (targetFile.exists()) targetFile.delete()
            return@withContext null
        }

        val bitmap = createPresetCape(presetId) ?: return@withContext null
        try {
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            targetFile
        } finally {
            bitmap.recycle()
        }
    }
}
