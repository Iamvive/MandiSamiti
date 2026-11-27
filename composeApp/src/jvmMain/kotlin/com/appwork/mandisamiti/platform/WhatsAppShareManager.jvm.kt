package com.appwork.mandisamiti.platform

actual class WhatsAppShareManager {
    actual fun shareReceiptImage(
        imageBytes: ByteArray,
        phoneNumber: String?,
        caption: String
    ) {
        // No-op on JVM desktop testing
    }

    actual fun installedApps(): List<WhatsAppApp> = emptyList()

    actual fun shareText(
        text: String,
        phoneNumber: String?,
        app: WhatsAppApp?
    ) {
        // No-op on JVM desktop testing
    }
}
