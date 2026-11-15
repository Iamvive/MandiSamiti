package com.appwork.mandisamiti.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient

actual class ReceiptPrinter(private val context: Context) {

    private fun findActivity(ctx: Context): Activity? {
        var cur: Context? = ctx
        while (cur is ContextWrapper) {
            if (cur is Activity) return cur
            cur = cur.baseContext
        }
        return null
    }

    actual fun printHtml(jobName: String, htmlContent: String) {
        Handler(Looper.getMainLooper()).post {
            try {
                val activity = findActivity(context) ?: (context as? Activity)
                val targetContext = activity ?: context
                val printManager = targetContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return@post
                val webView = WebView(targetContext)
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val printAdapter = webView.createPrintDocumentAdapter(jobName)
                        val printAttributes = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A5)
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()
                        printManager.print(jobName, printAdapter, printAttributes)
                    }
                }
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
