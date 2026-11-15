package com.appwork.mandisamiti.platform

import platform.UIKit.UIPrintInfo
import platform.UIKit.UIPrintInfoOutputGeneral
import platform.UIKit.UIPrintInteractionController
import platform.UIKit.UIMarkupTextPrintFormatter

actual class ReceiptPrinter {
    actual fun printHtml(jobName: String, htmlContent: String) {
        val printController = UIPrintInteractionController.sharedPrintController()
        val printInfo = UIPrintInfo.printInfoWithDictionary(null)
        printInfo.outputType = UIPrintInfoOutputGeneral
        printInfo.jobName = jobName
        printController.printInfo = printInfo
        printController.printFormatter = UIMarkupTextPrintFormatter(htmlContent)
        printController.presentAnimated(true, completionHandler = null)
    }
}
