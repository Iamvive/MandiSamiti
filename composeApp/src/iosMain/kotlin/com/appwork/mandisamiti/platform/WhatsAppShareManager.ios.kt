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

    actual fun shareText(
        text: String,
        phoneNumber: String?
    ) {
        val encodedText = text.replace(" ", "%20").replace("\n", "%0A")
        val phoneParam = if (!phoneNumber.isNullOrEmpty()) "&phone=$phoneNumber" else ""
        val urlString = "https://api.whatsapp.com/send?text=$encodedText$phoneParam"
        val url = NSURL.URLWithString(urlString)
        if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url)
        }
    }
}
