package com.israadev.nuxlauncher.ui.components

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hardware-accelerated, muted looping video player for NUX Launcher Hero Banner.
 * Supports smooth animated rotation (0°, 90°, 180°, 270°) with exact minimal
 * aspect-cover fitting (fit height when wide, fit width when tall, zero overzoom).
 */
@Composable
fun HeroBannerVideoPlayer(
    videoPath: String,
    rotationDegrees: Int = 0,
    modifier: Modifier = Modifier
) {
    if (videoPath.isBlank() || !File(videoPath).exists()) {
        return
    }

    var videoNaturalW by remember { mutableIntStateOf(0) }
    var videoNaturalH by remember { mutableIntStateOf(0) }
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }

    val animatedRotation by animateFloatAsState(
        targetValue = rotationDegrees.toFloat(),
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "heroBannerRotation"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val viewW = constraints.maxWidth.toFloat()
        val viewH = constraints.maxHeight.toFloat()

        fun applyTransform(rot: Float) {
            val tv = textureViewRef ?: return
            val vw = if (videoNaturalW > 0) videoNaturalW.toFloat() else 1920f
            val vh = if (videoNaturalH > 0) videoNaturalH.toFloat() else 1080f
            if (viewW <= 0f || viewH <= 0f) return

            val rad = Math.toRadians(rot.toDouble())
            val cosA = abs(cos(rad)).toFloat()
            val sinA = abs(sin(rad)).toFloat()

            // Exact bounding box of the rotated content
            val effW = vw * cosA + vh * sinA
            val effH = vw * sinA + vh * cosA

            // Minimal cover scale: fit width (crop top/bottom) or fit height (crop left/right)
            val scale = maxOf(viewW / effW, viewH / effH)
            val sx = (vw * scale) / viewW
            val sy = (vh * scale) / viewH

            val matrix = Matrix()
            matrix.setScale(sx, sy, viewW / 2f, viewH / 2f)
            matrix.postRotate(rot, viewW / 2f, viewH / 2f)
            tv.setTransform(matrix)
        }

        LaunchedEffect(animatedRotation, videoNaturalW, videoNaturalH, viewW, viewH) {
            applyTransform(animatedRotation)
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                TextureView(ctx).apply {
                    textureViewRef = this
                    var mediaPlayer: MediaPlayer? = null

                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, w: Int, h: Int) {
                            try {
                                val surface = Surface(surfaceTexture)
                                mediaPlayer = MediaPlayer().apply {
                                    setSurface(surface)
                                    setDataSource(videoPath)
                                    isLooping = true
                                    setVolume(0f, 0f)
                                    setOnVideoSizeChangedListener { _, width, height ->
                                        if (width > 0 && height > 0) {
                                            videoNaturalW = width
                                            videoNaturalH = height
                                            applyTransform(animatedRotation)
                                        }
                                    }
                                    setOnPreparedListener { mp ->
                                        try {
                                            if (mp.videoWidth > 0 && mp.videoHeight > 0) {
                                                videoNaturalW = mp.videoWidth
                                                videoNaturalH = mp.videoHeight
                                                applyTransform(animatedRotation)
                                            }
                                            mp.start()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    prepareAsync()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, w: Int, h: Int) {
                            applyTransform(animatedRotation)
                        }

                        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                            try {
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                                mediaPlayer = null
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
                    }
                }
            },
            update = { tv ->
                textureViewRef = tv
                applyTransform(animatedRotation)
            }
        )
    }
}
