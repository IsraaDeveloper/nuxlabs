package com.israadev.nuxlauncher.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

/**
 * NUX Adaptive DPI & Responsive Layout System
 * 
 * Automatically scales UI dimensions, typography, cards, paddings, and touch targets
 * based on device screen DPI, display density, and physical landscape screen height/width.
 * 
 * Target baseline: 392dp landscape height (Standard Android phone in landscape mode).
 * Safely clamps scaling factors to prevent UI overflows on compact/high-DPI screens
 * while expanding gracefully on large displays, foldables, and tablets.
 */
data class NuxScaleMetrics(
    val scaleFactor: Float = 1.0f,
    val fontScaleFactor: Float = 1.0f,
    val isCompact: Boolean = false,
    val isTablet: Boolean = false,
    val screenWidthDp: Dp = 800.dp,
    val screenHeightDp: Dp = 392.dp
) {
    fun scaledDp(value: Dp): Dp = (value.value * scaleFactor).dp
    fun scaledSp(value: TextUnit): TextUnit = (value.value * fontScaleFactor).sp
}

val LocalNuxScale = staticCompositionLocalOf { NuxScaleMetrics() }

/**
 * Root Responsive Provider that calculates DPI-aware metrics and applies
 * clamped accessibility font scaling to avoid container clipping.
 */
@Composable
fun NuxResponsiveTheme(
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val systemDensity = LocalDensity.current

    val screenHeightDp = configuration.screenHeightDp.dp
    val screenWidthDp = configuration.screenWidthDp.dp

    // Calculate baseline ratio from standard 390dp landscape height
    val heightRatio = screenHeightDp.value / 390f
    
    // Determine device form-factor
    val isTablet = screenHeightDp >= 520.dp || configuration.smallestScreenWidthDp >= 600
    val isCompact = screenHeightDp < 350.dp

    // Smooth clamped scaling factor: 0.78f (ultra compact / high display zoom) .. 1.30f (tablets)
    val scaleFactor = min(max(heightRatio, 0.78f), if (isTablet) 1.28f else 1.15f)

    // Clamp system fontScale to a safe range [0.85f .. 1.15f] so extreme OS font accessibility
    // settings do not shatter compact gaming launcher layouts while preserving readability
    val clampedSystemFontScale = min(max(systemDensity.fontScale, 0.85f), 1.15f)
    val combinedFontScale = scaleFactor * (clampedSystemFontScale / max(systemDensity.fontScale, 0.01f))

    val metrics = NuxScaleMetrics(
        scaleFactor = scaleFactor,
        fontScaleFactor = scaleFactor * clampedSystemFontScale,
        isCompact = isCompact,
        isTablet = isTablet,
        screenWidthDp = screenWidthDp,
        screenHeightDp = screenHeightDp
    )

    // Provide modified Density with scaled density and clamped fontScale
    // This automatically scales ALL dp and sp values across EVERY SCREEN, COMPONENT,
    // CARD, BUTTON, and DIALOG in the application.
    val responsiveDensity = Density(
        density = systemDensity.density * scaleFactor,
        fontScale = clampedSystemFontScale
    )

    CompositionLocalProvider(
        LocalNuxScale provides metrics,
        LocalDensity provides responsiveDensity
    ) {
        content()
    }
}

// -------------------------------------------------------------------------
// Ergonomic Responsive Extensions for Dp, Int, Float, and TextUnit
// -------------------------------------------------------------------------

/**
 * Responsive extension for Dp. Since responsiveDensity already applies scaleFactor
 * globally to all DP conversions, this safely preserves the calibrated value.
 */
@Composable
fun Dp.resp(): Dp = this

/**
 * Responsive extension for TextUnit (sp). Since responsiveDensity already applies
 * font scaling globally to all SP conversions, this safely preserves the calibrated value.
 */
@Composable
fun TextUnit.resp(): TextUnit = this

@Composable
fun Int.respDp(): Dp = this.dp

@Composable
fun Float.respDp(): Dp = this.dp

@Composable
fun Int.respSp(): TextUnit = this.sp

@Composable
fun Float.respSp(): TextUnit = this.sp
