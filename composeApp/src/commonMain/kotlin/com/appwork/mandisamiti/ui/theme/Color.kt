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
val MandiBorderActive = LightNgdlBorderActive
val MandiTextPrimary = LightNgdlTextPrimary
val MandiTextSecondary = LightNgdlTextSecondary
val MandiTextMuted = LightNgdlTextTertiary

// NGDL Refined Modernist Action Tokens (Deep Carbon & Emerald)
val MandiPrimaryAction = Color(0xFF090A0C)
val MandiPrimaryActionText = Color(0xFFFFFFFF)
val MandiSecondaryAction = Color(0xFFF1F3F5)
val MandiSecondaryActionText = Color(0xFF090A0C)

// Legacy Aliases mapped to Clean NGDL Neutrals (Eliminates Loud Amber/Brown)
val MandiAmberPrimary = Color(0xFF090A0C)       // Deep Carbon
val MandiAmberDark = Color(0xFF090A0C)          // Deep Carbon
val MandiAmberLight = Color(0xFFF1F3F5)         // Muted Sub-surface
val MandiAmber50 = Color(0xFFF8F9FA)            // Alabaster Canvas
val MandiAmberBorder = Color(0x14000000)        // 8% Border
val MandiNavy = Color(0xFF090A0C)
val MandiSlate = Color(0xFF585E6B)
val MandiAccent = Color(0xFF090A0C)
val MandiGold = Color(0xFF090A0C)
val MandiGoldLight = Color(0xFFF1F3F5)

// Neutral Tokens
val MandiNeutralLight = Color(0xFFF1F3F5)
val MandiNeutralBorder = Color(0x14000000)
val MandiNeutralText = Color(0xFF090A0C)

// High-Trust Financial Status Colors (Semantic Red / Green)
val MandiRedReceivable = Color(0xFFDC2626)       // Receivable (लेना है)
val MandiRedLight = Color(0xFFFEF2F2)
val MandiRedBorder = Color(0x33DC2626)          // 20% alpha
val MandiRedText = Color(0xFFB91C1C)            // Deep Crimson for high contrast (5.4:1)

val MandiGreenPayable = Color(0xFF16A34A)        // Payable (देना है)
val MandiGreenLight = Color(0xFFF0FDF4)
val MandiGreenBorder = Color(0x3316A34A)        // 20% alpha
val MandiGreenText = Color(0xFF15803D)          // Deep Emerald for high contrast (5.2:1)

// NGDL Solid Action Button Pairs (Strictly conforms to DESIGN-SYSTEM.md §2.1)
val MandiBtnSuccessBg = Color(0xFF15803D)       // Solid dark green fill
val MandiBtnSuccessFg = Color(0xFFFFFFFF)       // Crisp white text on success
val MandiBtnDangerBg = Color(0xFFDC2626)        // Solid deep red fill
val MandiBtnDangerFg = Color(0xFFFFFFFF)        // Crisp white text on danger


