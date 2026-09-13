package com.porter.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Light Color Scheme ────────────────────────────────────────────────────────
private val PorterLightColors = lightColorScheme(
    primary = ActionBlue,
    onPrimary = CanvasWhite,
    primaryContainer = Parchment,
    onPrimaryContainer = InkNearBlack,
    secondary = InkNearBlack,
    onSecondary = CanvasWhite,
    secondaryContainer = Parchment,
    onSecondaryContainer = InkNearBlack,
    tertiary = SkyLinkBlue,
    onTertiary = CanvasWhite,
    background = CanvasWhite,
    onBackground = InkNearBlack,
    surface = CanvasWhite,
    onSurface = InkNearBlack,
    surfaceVariant = Parchment,
    onSurfaceVariant = InkMuted80,
    outline = Hairline,
    outlineVariant = DividerSoft,
    error = StatusError,
    onError = CanvasWhite,
    scrim = Color(0x99000000),
)

// ── Dark Color Scheme ─────────────────────────────────────────────────────────
private val PorterDarkColors = darkColorScheme(
    primary = SkyLinkBlue,             // Sky Link Blue on dark surfaces
    onPrimary = CanvasWhite,
    primaryContainer = DarkTile2,
    onPrimaryContainer = BodyOnDark,
    secondary = BodyMuted,
    onSecondary = DarkTile1,
    secondaryContainer = DarkTile2,
    onSecondaryContainer = BodyOnDark,
    tertiary = SkyLinkBlue,
    onTertiary = DarkTile1,
    background = DarkTile1,
    onBackground = BodyOnDark,
    surface = DarkTile1,
    onSurface = BodyOnDark,
    surfaceVariant = DarkTile2,
    onSurfaceVariant = BodyMuted,
    outline = Color(0xFF3A3A3C),
    outlineVariant = Color(0xFF2C2C2E),
    error = StatusError,
    onError = CanvasWhite,
    scrim = Color(0xCC000000),
)

// ── PorterTheme ───────────────────────────────────────────────────────────────
@Composable
fun PorterTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PorterDarkColors else PorterLightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PorterTypography,
        shapes = PorterShapes,
        content = content
    )
}
