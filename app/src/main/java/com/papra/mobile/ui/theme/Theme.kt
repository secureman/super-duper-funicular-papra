package com.papra.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = PaperLight,
    primaryContainer = InkContainer,
    onPrimaryContainer = OnInkContainer,
    secondary = InkLight,
    tertiary = Seal,
    onTertiary = PaperLight,
    tertiaryContainer = SealContainer,
    onTertiaryContainer = OnSealContainer,
    surface = PaperLight,
    onSurface = OnPaperLight,
    surfaceVariant = PaperVariantLight,
    onSurfaceVariant = OnPaperLight,
    outline = ParchmentLight,
    background = PaperLight,
    onBackground = OnPaperLight,
)

private val DarkColors = darkColorScheme(
    primary = InkLight,
    onPrimary = PaperDark,
    primaryContainer = Ink,
    onPrimaryContainer = InkContainer,
    secondary = InkLight,
    tertiary = SealLight,
    onTertiary = OnSealContainer,
    tertiaryContainer = Seal,
    onTertiaryContainer = SealContainer,
    surface = PaperDark,
    onSurface = OnPaperDark,
    surfaceVariant = PaperVariantDark,
    onSurfaceVariant = OnPaperDark,
    outline = ParchmentDark,
    background = PaperDark,
    onBackground = OnPaperDark,
)

/**
 * Dynamic color (Material You) is deliberately OFF by default. On most
 * Android 12+ devices it derives a muted palette from the wallpaper, which
 * silently overrides any deliberate brand palette and is a common reason an
 * app ends up looking generic/flat regardless of what colors are defined
 * here. Papra has its own intentional ink/paper/seal identity instead.
 */
@Composable
fun PapraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PapraTypography,
        content = content,
    )
}
