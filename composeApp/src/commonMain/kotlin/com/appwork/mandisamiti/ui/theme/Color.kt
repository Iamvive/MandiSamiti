package com.appwork.mandisamiti.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * MandiSamiti Theme Colors — Powered by NoGravity Design Language (NGDL v1.2)
 * Conforms strictly to docs/design-system/DESIGN-SYSTEM.md §2.1
 */

// NGDL Monochromatic Space Black & Graphite (Dark Theme)
val DarkNgdlBackground = Color(0xFF08090A)
val DarkNgdlSurface = Color(0xFF121316)
val DarkNgdlSurfaceElevated = Color(0xFF1A1C20)
val DarkNgdlBorder = Color(0x14FFFFFF) // 8% alpha
val DarkNgdlBorderActive = Color(0x38FFFFFF) // 22% alpha
val DarkNgdlTextPrimary = Color(0xFFFFFFFF)
val DarkNgdlTextSecondary = Color(0xFFA0A5AE)
val DarkNgdlTextTertiary = Color(0xFF5F6570)

// NGDL Pure Alabaster & Milk White (Light Theme)
val LightNgdlBackground = Color(0xFFF8F9FA)
val LightNgdlSurface = Color(0xFFFFFFFF)
val LightNgdlSurfaceElevated = Color(0xFFF1F3F5)
val LightNgdlBorder = Color(0x14000000) // 8% alpha
val LightNgdlBorderActive = Color(0x40000000) // 25% alpha
val LightNgdlTextPrimary = Color(0xFF090A0C)
val LightNgdlTextSecondary = Color(0xFF585E6B)
val LightNgdlTextTertiary = Color(0xFF8C929E)

// Mandi Domain Tokens (Dynamically Theme-Aware)
val MandiBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlBackground else LightNgdlBackground

val MandiSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlSurface else LightNgdlSurface

val MandiSurfaceElevated: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlSurfaceElevated else LightNgdlSurfaceElevated

val MandiBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlBorder else LightNgdlBorder

val MandiBorderActive: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlBorderActive else LightNgdlBorderActive

val MandiTextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlTextPrimary else LightNgdlTextPrimary

val MandiTextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlTextSecondary else LightNgdlTextSecondary

val MandiTextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlTextTertiary else LightNgdlTextTertiary

// NGDL Refined Modernist Action Tokens (Solid Action Button Pairs)
val MandiPrimaryAction: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFFFFFFFF) else Color(0xFF090A0C)

val MandiPrimaryActionText: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF08090A) else Color(0xFFFFFFFF)

val MandiSecondaryAction: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlSurfaceElevated else LightNgdlSurfaceElevated

val MandiSecondaryActionText: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlTextPrimary else LightNgdlTextPrimary

// Dynamic Carbon/Navy & Neutral Tokens
val MandiNavy: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlTextPrimary else Color(0xFF090A0C)

val MandiSlate: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlTextSecondary else LightNgdlTextSecondary

val MandiAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlTextPrimary else Color(0xFF090A0C)

val MandiGold: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlTextPrimary else Color(0xFF090A0C)

val MandiGoldLight: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlSurfaceElevated else LightNgdlSurfaceElevated

val MandiAmberPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlTextPrimary else Color(0xFF090A0C)

val MandiAmberDark: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlTextPrimary else Color(0xFF090A0C)

val MandiAmberLight: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlSurfaceElevated else Color(0xFFF1F3F5)

val MandiAmber50: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlBackground else Color(0xFFF8F9FA)

val MandiAmberBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlBorder else Color(0x14000000)

val MandiNeutralLight: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlSurfaceElevated else Color(0xFFF1F3F5)

val MandiNeutralBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlBorder else Color(0x14000000)

val MandiNeutralText: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) DarkNgdlTextPrimary else Color(0xFF090A0C)

// High-Trust Financial Status Colors (Semantic Red / Green)
// Accounting: Receivable (लेना है / Money coming in) = Green, Payable (देना है / Money going out) = Red
val MandiGreenReceivable = Color(0xFF16A34A)    // Receivable (लेना है)
val MandiRedPayable = Color(0xFFDC2626)         // Payable (देना है)

@Deprecated("Use MandiGreenReceivable or MandiRedPayable", ReplaceWith("MandiGreenReceivable"))
val MandiGreenPayable = MandiGreenReceivable

@Deprecated("Use MandiRedPayable or MandiGreenReceivable", ReplaceWith("MandiRedPayable"))
val MandiRedReceivable = MandiRedPayable

val MandiRedLight: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0x33DC2626) else Color(0xFFFEF2F2)

val MandiRedBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0x55DC2626) else Color(0x33DC2626)

val MandiRedText: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFFEF4444) else Color(0xFFB91C1C)

val MandiGreenLight: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0x3316A34A) else Color(0xFFF0FDF4)

val MandiGreenBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0x5516A34A) else Color(0x3316A34A)

val MandiGreenText: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF22C55E) else Color(0xFF15803D)

// NGDL Solid Action Button Pairs (Strictly conforms to DESIGN-SYSTEM.md §2.1)
val MandiBtnSuccessBg = Color(0xFF15803D)       // Solid dark green fill
val MandiBtnSuccessFg = Color(0xFFFFFFFF)       // Crisp white text on success
val MandiBtnDangerBg = Color(0xFFDC2626)        // Solid deep red fill
val MandiBtnDangerFg = Color(0xFFFFFFFF)        // Crisp white text on danger

// NGDL Action & Category Tokens (Opaque Tint Support)
val MandiBlueSell = Color(0xFF2563EB)
val MandiOrangeLend = Color(0xFFD97706)
val MandiPinkExpense = Color(0xFFDB2777)
val MandiGreenBuy = Color(0xFF16A34A)

/**
 * Composites [accent] over [background] with [alpha] to produce an opaque Color.
 * Prevents elevation shadow bleed-through on elevated surfaces.
 */
fun opaqueTint(accent: Color, background: Color, alpha: Float = 0.12f): Color {
    val r = accent.red * alpha + background.red * (1f - alpha)
    val g = accent.green * alpha + background.green * (1f - alpha)
    val b = accent.blue * alpha + background.blue * (1f - alpha)
    return Color(red = r, green = g, blue = b, alpha = 1f)
}


