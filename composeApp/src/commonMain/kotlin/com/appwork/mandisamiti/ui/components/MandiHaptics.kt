package com.appwork.mandisamiti.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * MandiSamiti Silent Haptic Helper.
 * Mandated: strictly zero audio emission.
 * Channels subtle tactile confirmation for key presses, card taps, and saves.
 */
class MandiHapticHelper(private val haptics: HapticFeedback) {
    /**
     * Standard click tactile pulse (silent).
     */
    fun click() {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /**
     * Success tactile confirmation on transaction save or validation (silent).
     */
    fun success() {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /**
     * Long-press tactile pulse (silent).
     */
    fun longPress() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

@Composable
fun rememberMandiHaptics(): MandiHapticHelper {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback) { MandiHapticHelper(hapticFeedback) }
}

