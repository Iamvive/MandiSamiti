package com.appwork.mandisamiti.platform

expect class ReceiptPrinter {
    fun printHtml(jobName: String, htmlContent: String)
}
