package com.appwork.mandisamiti.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Global Reduced Motion provider.
 * When true, all animations execute instantaneously (0ms duration).
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Standardized NGDL Motion Specifications.
 * Automatically adapts to user preference for reduced motion.
 */
object MandiMotion {
    const val DurationFast: Int = 150
    const val DurationNormal: Int = 250
    const val DurationSlow: Int = 350

    @Composable
    @ReadOnlyComposable
    fun <T> fast(): AnimationSpec<T> {
        return if (LocalReducedMotion.current) snap() else tween(durationMillis = DurationFast, easing = FastOutSlowInEasing)
    }

    @Composable
    @ReadOnlyComposable
    fun <T> normal(): AnimationSpec<T> {
        return if (LocalReducedMotion.current) snap() else tween(durationMillis = DurationNormal, easing = FastOutSlowInEasing)
    }

    @Composable
    @ReadOnlyComposable
    fun <T> slow(): AnimationSpec<T> {
        return if (LocalReducedMotion.current) snap() else tween(durationMillis = DurationSlow, easing = FastOutSlowInEasing)
    }

    @Composable
    @ReadOnlyComposable
    fun <T> fade(): AnimationSpec<T> {
        return if (LocalReducedMotion.current) snap() else tween(durationMillis = DurationFast, easing = LinearEasing)
    }
}

