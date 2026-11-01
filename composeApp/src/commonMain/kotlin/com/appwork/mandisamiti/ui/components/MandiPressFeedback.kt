package com.appwork.mandisamiti.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Subtle tactile press feedback (scale to 0.98 on touch)
 * with optional silent haptic pulse and strict reduced motion support.
 */
fun Modifier.mandiPressEffect(
    enabled: Boolean = true,
    haptics: MandiHapticHelper? = null,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isReduced = LocalReducedMotion.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isReduced) 0.98f else 1f,
        animationSpec = MandiMotion.fast(),
        label = "mandiPressScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled
                ) {
                    haptics?.click()
                    onClick()
                }
            } else Modifier
        )
}

