package com.appwork.mandisamiti.platform

import androidx.compose.runtime.Composable

expect class CameraSlipPicker {
    fun launchCamera(onPhotoCaptured: (String?) -> Unit)
}

@Composable
expect fun rememberCameraSlipPicker(): CameraSlipPicker
