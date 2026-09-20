package com.appwork.mandisamiti.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = MandiNavy,
    onPrimary = MandiSurface,
    primaryContainer = MandiSlate,
    onPrimaryContainer = MandiSurface,
    secondary = MandiGreenPayable,
    onSecondary = MandiSurface,
    error = MandiRedReceivable,
    onError = MandiSurface,
    background = MandiBackground,
    onBackground = MandiTextPrimary,
    surface = MandiSurface,
    onSurface = MandiTextPrimary,
    outline = MandiBorder
)

@Composable
fun MandiSamitiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = MandiTypography,
        content = content
    )
}
