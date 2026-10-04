package com.appwork.mandisamiti.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkNgdlTextPrimary,
    onPrimary = DarkNgdlBackground,
    primaryContainer = DarkNgdlSurfaceElevated,
    onPrimaryContainer = DarkNgdlTextPrimary,
    secondary = MandiGreenPayable,
    onSecondary = DarkNgdlTextPrimary,
    error = MandiRedReceivable,
    onError = DarkNgdlTextPrimary,
    background = DarkNgdlBackground,
    onBackground = DarkNgdlTextPrimary,
    surface = DarkNgdlSurface,
    onSurface = DarkNgdlTextPrimary,
    surfaceVariant = DarkNgdlSurfaceElevated,
    onSurfaceVariant = DarkNgdlTextSecondary,
    outline = DarkNgdlBorder
)

private val LightColorScheme = lightColorScheme(
    primary = LightNgdlTextPrimary,
    onPrimary = LightNgdlBackground,
    primaryContainer = LightNgdlSurfaceElevated,
    onPrimaryContainer = LightNgdlTextPrimary,
    secondary = MandiGreenPayable,
    onSecondary = LightNgdlBackground,
    error = MandiRedReceivable,
    onError = LightNgdlBackground,
    background = LightNgdlBackground,
    onBackground = LightNgdlTextPrimary,
    surface = LightNgdlSurface,
    onSurface = LightNgdlTextPrimary,
    surfaceVariant = LightNgdlSurfaceElevated,
    onSurfaceVariant = LightNgdlTextSecondary,
    outline = LightNgdlBorder
)

@Composable
fun MandiSamitiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MandiTypography,
        content = content
    )
}
