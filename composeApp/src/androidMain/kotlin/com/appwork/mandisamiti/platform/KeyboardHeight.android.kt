package com.appwork.mandisamiti.platform

import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private fun findActivity(context: android.content.Context): android.app.Activity? {
    var ctx = context
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
actual fun rememberImeOrKeyboardHeight(): Dp {
    val density = LocalDensity.current
    val windowInsetsImeDp = with(density) {
        WindowInsets.ime.getBottom(this).toDp()
    }
    val view = LocalView.current
    val activity = remember(view) { findActivity(view.context) }
    val targetView = activity?.window?.decorView ?: view.rootView
    var measuredKeyboardDp by remember { mutableStateOf(0.dp) }

    DisposableEffect(targetView) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            targetView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = targetView.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            val isKeyboardOpen = keypadHeight > screenHeight * 0.15
            measuredKeyboardDp = if (isKeyboardOpen) with(density) { keypadHeight.toDp() } else 0.dp
        }
        targetView.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            targetView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    return if (windowInsetsImeDp > 0.dp) windowInsetsImeDp else measuredKeyboardDp
}
