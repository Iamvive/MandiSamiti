package com.appwork.mandisamiti.platform

actual class WhatsAppShareManager {
    actual fun shareReceiptImage(
        imageBytes: ByteArray,
        phoneNumber: String?,
        caption: String
    ) {
        println("[WhatsAppShare JVM]: sharing receipt (${imageBytes.size} bytes) - $caption")
    }
}
