package com.appwork.mandisamiti.platform

import androidx.compose.runtime.Composable

@Composable
expect fun MandiBackHandler(enabled: Boolean = true, onBack: () -> Unit)
