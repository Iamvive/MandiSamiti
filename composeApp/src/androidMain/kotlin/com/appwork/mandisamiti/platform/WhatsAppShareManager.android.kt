package com.appwork.mandisamiti.platform

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

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
                setPackage("com.whatsapp")
            }

            startChooser(shareIntent, "पर्ची व्हाट्सएप पर भेजें")
        } catch (e: Exception) {
            // Fallback to generic share sheet
            val genericIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, caption)
            }
            startChooser(genericIntent, "शेयर करें")
        }
    }

    actual fun shareText(
        text: String,
        phoneNumber: String?
    ) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            val genericIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startChooser(genericIntent, "पर्ची शेयर करें")
        }
    }

    // createChooser() does not carry NEW_TASK over from the wrapped intent, and this class holds the
    // application context, so the flag must be on the chooser itself or startActivity throws.
    private fun startChooser(target: Intent, title: String) {
        context.startActivity(Intent.createChooser(target, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
