package com.appwork.mandisamiti.platform

expect class WhatsAppShareManager {
    fun shareReceiptImage(
        imageBytes: ByteArray,
        phoneNumber: String?,
        caption: String
    )

    /** WhatsApp apps the user can choose between. Empty when the platform picks for itself or none is installed. */
    fun installedApps(): List<WhatsAppApp>

    /** [phoneNumber] is a 10-digit mobile, or null to choose the contact in WhatsApp. */
    fun shareText(
        text: String,
        phoneNumber: String?,
        app: WhatsAppApp? = null
    )
}
