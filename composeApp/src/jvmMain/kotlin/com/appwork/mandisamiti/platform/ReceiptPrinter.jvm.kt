package com.appwork.mandisamiti.platform

actual class ReceiptPrinter {
    var lastPrintedJobName: String? = null
        private set
    var lastPrintedHtml: String? = null
        private set

    actual fun printHtml(jobName: String, htmlContent: String) {
        lastPrintedJobName = jobName
        lastPrintedHtml = htmlContent
        println("[ReceiptPrinter JVM] printHtml: $jobName (length: ${htmlContent.length})")
    }
}
