package com.appwork.mandisamiti.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class CameraSlipPicker {
    actual fun launchCamera(onPhotoCaptured: (String?) -> Unit) {
        // Camera capture via iOS UIImagePickerController
        onPhotoCaptured("file://mock/ios/slip_photo.jpg")
    }
}

@Composable
actual fun rememberCameraSlipPicker(): CameraSlipPicker {
    return remember { CameraSlipPicker() }
}
