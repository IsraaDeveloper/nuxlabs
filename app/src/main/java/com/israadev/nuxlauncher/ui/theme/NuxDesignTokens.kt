package com.israadev.nuxlauncher.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * NUX Dark Obsidian Cyber-Glass Color Palette
 * Direct match with Windows launcher and Keystore
 */
object NuxColors {
    // Primary Cyber Emerald Palette
    val ForestGreen = Color(0xFF10B981) // Cyber Emerald Primary (#10b981)
    val MintGreen = Color(0xFF34D399)   // Bright Mint Accent / Glow (#34d399)
    val SageGreen = Color(0xFF059669)   // Darker Emerald (#059669)
    val SoftLime = Color(0xFF102A1F)    // Dark Emerald Tint Container
    val LightGreen = Color(0x2610B981)  // 15% Emerald Glow Container

    // Dark Obsidian Neutrals (Frosted Glass Translucent)
    val Background = Color(0xFF09090B)  // Deep Obsidian background (#09090b)
    val SurfaceWhite = Color(0x9912141C)// Obsidian Glass surface (60% frosted)
    val SurfaceElevated = Color(0xB3161A24) // Elevated card surface (70% frosted)
    val SurfaceInput = Color(0x8C0D0F16) // Dark input surface (55% frosted)
    val DarkGray = Color(0xFFF4F4F5)    // High-contrast text (#F4F4F5)
    val GrayNeutral = Color(0xFFA1A1AA) // Zinc-400 Muted text & subtitles
    val LightGray = Color(0x26FFFFFF)   // Hairline dividers (15% white)
    val CardBorder = Color(0x33FFFFFF)  // Hairline borders (20% white)
    val ErrorRed = Color(0xFFF43F5E)    // Rose error (#f43f5e)
    val Amber = Color(0xFFF59E0B)       // Amber warning (#f59e0b)
    val TextPrimary = Color(0xFFFFFFFF) // Pure white text

    // Legacy aliases for backward compatibility
    val Mint = SoftLime
    val MintDark = MintGreen
    val ForestDark = DarkGray
    val ForestLight = GrayNeutral
    val ForestMuted = GrayNeutral
    val Cream = Background
    val Coral = ErrorRed
    val TextMuted = GrayNeutral
    val SuccessGreen = ForestGreen
    val SkyBlue = Color(0xFF38BDF8)
}

object NuxSizes {
    val BorderWidth = 1.dp
    val CornerRadius = 18.dp
    val CornerRadiusLarge = 22.dp
    val CornerRadiusSmall = 12.dp
    val ShadowOffset = 0.dp
    val BorderDefault = BorderStroke(BorderWidth, NuxColors.CardBorder)
    val ShapeDefault = RoundedCornerShape(CornerRadius)
    val ShapeLarge = RoundedCornerShape(CornerRadiusLarge)
    val ShapeSmall = RoundedCornerShape(CornerRadiusSmall)
}
