package com.appwork.mandisamiti.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standardized Spacing Scale for MandiSamiti (NGDL v1.2)
 * Replaces ad-hoc paddings across screens with a cohesive geometric scale.
 */
data class MandiSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,

    // Semantic structural tokens
    val cardPadding: Dp = 16.dp,
    val screenPadding: Dp = 16.dp,
    val itemSpacing: Dp = 12.dp,
    val sectionSpacing: Dp = 20.dp,
    val dialogPadding: Dp = 20.dp,
    val buttonHeight: Dp = 48.dp,
    val iconSizeSm: Dp = 16.dp,
    val iconSizeMd: Dp = 20.dp,
    val iconSizeLg: Dp = 24.dp
)

val LocalMandiSpacing = staticCompositionLocalOf { MandiSpacing() }

val MandiSpacingTokens: MandiSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalMandiSpacing.current

