package com.appwork.mandisamiti.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder

actual class WhatsAppShareManager(private val context: Context) {

    actual fun shareReceiptImage(
        imageBytes: ByteArray,
        phoneNumber: String?,
        caption: String
    ) {
        try {
            val cachePath = File(context.cacheDir, "receipts")
            cachePath.mkdirs()
            val receiptFile = File(cachePath, "mandi_receipt_${System.currentTimeMillis()}.png")
            FileOutputStream(receiptFile).use { it.write(imageBytes) }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                receiptFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                shareIntent.setPackage("com.whatsapp")
                val chooser = Intent.createChooser(shareIntent, "पर्ची व्हाट्सएप पर भेजें").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (_: Exception) {
                shareIntent.setPackage(null)
                val chooser = Intent.createChooser(shareIntent, "पर्ची शेयर करें").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        } catch (_: Exception) {
            try {
                val genericIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, caption)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(genericIntent, "शेयर करें").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (_: Exception) {
                // Ignore any further OS-level intent launch issues
            }
        }
    }

    actual fun shareText(
        text: String,
        phoneNumber: String?
    ) {
        val cleanPhone = phoneNumber?.replace("[^0-9]".toRegex(), "")?.let {
            if (it.length == 10) "91$it" else it
        }

        try {
            if (!cleanPhone.isNullOrBlank()) {
                val encodedText = URLEncoder.encode(text, "UTF-8")
                val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedText")
                val directIntent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(directIntent)
                    return
                } catch (_: Exception) {
                    // WhatsApp app not found or direct view failed, fallback to generic send
                }
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(shareIntent)
            } catch (_: Exception) {
                shareIntent.setPackage(null)
                val chooser = Intent.createChooser(shareIntent, "पर्ची शेयर करें").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        } catch (_: Exception) {
            try {
                val genericIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(genericIntent, "पर्ची शेयर करें").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (_: Exception) {
                // Safely ignored
            }
        }
    }
}
