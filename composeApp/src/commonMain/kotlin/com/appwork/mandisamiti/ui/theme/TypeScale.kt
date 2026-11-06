package com.appwork.mandisamiti.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Standardized Typography Scale for MandiSamiti (NGDL v1.2)
 * Conforms strictly to docs/design-system/DESIGN-SYSTEM.md §3.
 * Eliminates ad-hoc font sizes across screens.
 */
data class MandiTypeScale(
    // Display / Hero: 32sp Bold
    val hero: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp
    ),
    // H1 / Screen Title: 24sp SemiBold
    val title: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    // H2 / Section Header: 18sp SemiBold
    val section: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    // H3 / Card Title: 15sp Medium
    val cardTitle: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    // Body Primary: 14sp Regular
    val body: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Body Strong / Highlighted: 14sp SemiBold
    val bodyStrong: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Caption / Secondary: 12sp Regular
    val caption: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    // Mono / Numeric Metrics: 13sp Medium
    val monoMetric: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    // Micro-Label / Tag: 10sp SemiBold
    val micro: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)

val LocalMandiTypeScale = staticCompositionLocalOf { MandiTypeScale() }

val MandiTypeTokens: MandiTypeScale
    @Composable
    @ReadOnlyComposable
    get() = LocalMandiTypeScale.current

