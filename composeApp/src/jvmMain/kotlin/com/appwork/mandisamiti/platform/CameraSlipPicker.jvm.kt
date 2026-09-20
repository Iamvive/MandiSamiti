package com.appwork.mandisamiti.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class CameraSlipPicker {
    actual fun launchCamera(onPhotoCaptured: (String?) -> Unit) {
        onPhotoCaptured(null)
    }
}

@Composable
actual fun rememberCameraSlipPicker(): CameraSlipPicker {
    return remember { CameraSlipPicker() }
}
