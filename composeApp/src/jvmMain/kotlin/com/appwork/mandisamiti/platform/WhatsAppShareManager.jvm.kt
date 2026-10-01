package com.appwork.mandisamiti.platform

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.net.URLEncoder

actual class WhatsAppShareManager {
    actual fun shareReceiptImage(
        imageBytes: ByteArray,
        phoneNumber: String?,
        caption: String
    ) {
        shareText(caption, phoneNumber)
    }

    actual fun shareText(
        text: String,
        phoneNumber: String?
    ) {
        try {
            // 1. Copy formatted receipt text to system clipboard for desktop convenience
            val selection = StringSelection(text)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
        } catch (_: Exception) {
            // Headless or clipboard access restricted
        }

        try {
            // 2. Open WhatsApp Web / wa.me in default browser
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                val cleanPhone = phoneNumber?.replace("[^0-9]".toRegex(), "")?.let {
                    if (it.length == 10) "91$it" else it
                }
                val encodedText = URLEncoder.encode(text, "UTF-8")
                val url = if (!cleanPhone.isNullOrBlank()) {
                    "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedText"
                } else {
                    "https://api.whatsapp.com/send?text=$encodedText"
                }
                Desktop.getDesktop().browse(URI.create(url))
            }
        } catch (_: Exception) {
            // Browser open failed or unsupported desktop environment
        }
    }
}
