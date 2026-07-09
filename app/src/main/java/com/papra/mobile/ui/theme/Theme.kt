package com.papra.mobile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = PapraBlue,
    onPrimary = SurfaceLight,
    primaryContainer = PapraBlueContainer,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    outline = OutlineLight,
)

private val DarkColors = darkColorScheme(
    primary = PapraBlueDark,
    onPrimary = SurfaceDark,
    primaryContainer = PapraBlue,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    outline = OutlineDark,
)

/**
 * Drive leans on Material You dynamic color on Android 12+, falling back to
 * its own brand palette otherwise. We mirror that: dynamic color when
 * available, PapraBlue palette as the fallback/branding.
 */
@Composable
fun PapraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PapraTypography,
        content = content,
    )
}
