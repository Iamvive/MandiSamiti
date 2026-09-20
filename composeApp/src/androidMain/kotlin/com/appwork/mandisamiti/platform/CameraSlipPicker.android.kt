package com.appwork.mandisamiti.platform

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

actual class CameraSlipPicker(
    private val onLaunch: ((String?) -> Unit) -> Unit
) {
    actual fun launchCamera(onPhotoCaptured: (String?) -> Unit) {
        onLaunch(onPhotoCaptured)
    }
}

@Composable
actual fun rememberCameraSlipPicker(): CameraSlipPicker {
    val context = LocalContext.current
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var currentCallback by remember { mutableStateOf<((String?) -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            currentCallback?.invoke(currentPhotoUri.toString())
        } else {
            currentCallback?.invoke(null)
        }
        currentCallback = null
    }

    return remember {
        CameraSlipPicker { callback ->
            currentCallback = callback
            val photoFile = File.createTempFile(
                "slip_${System.currentTimeMillis()}_",
                ".jpg",
                context.cacheDir
            )
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            currentPhotoUri = uri
            launcher.launch(uri)
        }
    }
}
