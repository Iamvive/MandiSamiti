package com.appwork.mandisamiti.platform

actual class WhatsAppShareManager {
    actual fun shareReceiptImage(
        imageBytes: ByteArray,
        phoneNumber: String?,
        caption: String
    ) {
        // No-op on JVM desktop testing
    }

    actual fun shareText(
        text: String,
        phoneNumber: String?
    ) {
        // No-op on JVM desktop testing
    }
}
