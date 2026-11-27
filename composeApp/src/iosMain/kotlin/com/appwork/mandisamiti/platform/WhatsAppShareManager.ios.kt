package com.appwork.mandisamiti.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual class WhatsAppShareManager {

    actual fun shareReceiptImage(
        imageBytes: ByteArray,
        phoneNumber: String?,
        caption: String
    ) {
        val url = NSURL.URLWithString("whatsapp://app")
        if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url)
        }
    }

    // iOS opens whichever WhatsApp app owns the link, so there is nothing to choose.
    actual fun installedApps(): List<WhatsAppApp> = emptyList()

    actual fun shareText(
        text: String,
        phoneNumber: String?,
        app: WhatsAppApp?
    ) {
        val url = NSURL.URLWithString(whatsAppSendUrl(phoneNumber, text)) ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
    }
}
