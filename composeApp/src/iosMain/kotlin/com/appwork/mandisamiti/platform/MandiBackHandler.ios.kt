package com.appwork.mandisamiti.platform

import androidx.compose.runtime.Composable

@Composable
actual fun MandiBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op on iOS
}
