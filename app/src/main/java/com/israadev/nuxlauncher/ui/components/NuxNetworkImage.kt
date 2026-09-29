package com.israadev.nuxlauncher.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.collection.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.ui.theme.NuxColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private val imageCache = LruCache<String, ImageBitmap>(50)

private val imageHttpClient = OkHttpClient.Builder()
    .dns(com.israadev.nuxlauncher.core.network.NuxDns)
    .connectTimeout(6, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()

/**
 * Neo-Brutalist Network & Local Uri Image Loader with LRU Memory Caching and Fallback Initials
 */
@Composable
fun NuxNetworkImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallbackInitials: String = "N",
    shape: Shape = RoundedCornerShape(12.dp),
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    var imageBitmap by remember(model) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(model) { mutableStateOf(false) }

    LaunchedEffect(model) {
        if (model == null) {
            imageBitmap = null
            return@LaunchedEffect
        }

        val cacheKey = model.toString()
        if (cacheKey.isBlank()) {
            imageBitmap = null
            return@LaunchedEffect
        }

        val cached = imageCache.get(cacheKey)
        if (cached != null) {
            imageBitmap = cached
            return@LaunchedEffect
        }

        isLoading = true
        val loadedBitmap: ImageBitmap? = withContext(Dispatchers.IO) {
            try {
                when (model) {
                    is Uri -> {
                        context.contentResolver.openInputStream(model)?.use { stream ->
                            BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        }
                    }
                    is String -> {
                        if (model.startsWith("http://") || model.startsWith("https://")) {
                            val req = Request.Builder().url(model).build()
                            val resp = imageHttpClient.newCall(req).execute()
                            if (resp.isSuccessful) {
                                val bytes = resp.body?.bytes()
                                if (bytes != null && bytes.isNotEmpty()) {
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                } else null
                            } else null
                        } else if (model.startsWith("content://") || model.startsWith("file://")) {
                            val uri = Uri.parse(model)
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                BitmapFactory.decodeStream(stream)?.asImageBitmap()
                            }
                        } else null
                    }
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        }

        if (loadedBitmap != null) {
            imageCache.put(cacheKey, loadedBitmap)
            imageBitmap = loadedBitmap
        }
        isLoading = false
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(NuxColors.SurfaceWhite),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = imageBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NuxColors.LightGray.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = NuxColors.ForestGreen,
                    strokeWidth = 2.dp
                )
            }
        } else {
            // Fallback Initials Display with Neo-Brutalist gaming colors
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NuxColors.SoftLime),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fallbackInitials.take(2).uppercase(),
                    color = NuxColors.ForestGreen,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
