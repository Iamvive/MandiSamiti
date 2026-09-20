package com.appwork.mandisamiti.platform

expect class WhatsAppShareManager {
    fun shareReceiptImage(
        imageBytes: ByteArray,
        phoneNumber: String?,
        caption: String
    )
}
