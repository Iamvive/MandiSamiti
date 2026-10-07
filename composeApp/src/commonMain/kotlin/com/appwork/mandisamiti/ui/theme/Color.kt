package com.appwork.mandisamiti.ui.theme

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

// Mandi Domain Aliases (Mapped to NGDL Light Defaults)
val MandiBackground = LightNgdlBackground
val MandiSurface = LightNgdlSurface
val MandiSurfaceElevated = LightNgdlSurfaceElevated
val MandiBorder = LightNgdlBorder
val MandiTextPrimary = LightNgdlTextPrimary
val MandiTextSecondary = LightNgdlTextSecondary
val MandiTextMuted = LightNgdlTextTertiary

// Mandi Saffron / Gold Accents
val MandiAmberPrimary = Color(0xFFB45309)
val MandiAmberDark = Color(0xFF92400E)
val MandiAmberLight = Color(0xFFFEF3C7)
val MandiAmber50 = Color(0xFFFFFBEB)
val MandiAmberBorder = Color(0xFFFDE68A)
val MandiNavy = MandiAmberPrimary
val MandiSlate = MandiAmberDark
val MandiAccent = Color(0xFFD97706)
val MandiGold = Color(0xFFD97706)
val MandiGoldLight = Color(0xFFFFFBEB)

// Neutral Tokens
val MandiNeutralLight = Color(0xFFF1F3F5)
val MandiNeutralBorder = Color(0x14000000)
val MandiNeutralText = Color(0xFF090A0C)

// High-Trust Financial Status Colors (Semantic Red / Green)
val MandiRedReceivable = Color(0xFFDC2626)  // Receivable (लेना है)
val MandiRedLight = Color(0xFFFEF2F2)
val MandiRedBorder = Color(0xFFFECACA)
val MandiRedText = Color(0xFF991B1B)

val MandiGreenPayable = Color(0xFF16A34A)   // Payable (देना है)
val MandiGreenLight = Color(0xFFF0FDF4)
val MandiGreenBorder = Color(0xFFBBF7D0)
val MandiGreenText = Color(0xFF166534)
