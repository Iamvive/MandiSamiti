package com.appwork.mandisamiti.ui.billing

interface ContactSupportHandler {
    fun dialPhone(phoneNumber: String)
    fun openWhatsApp(phoneNumber: String, message: String)

    companion object {
        fun buildDialerUri(phoneNumber: String): String = "tel:$phoneNumber"

        fun buildWhatsAppUri(phoneNumber: String, message: String): String {
            val encodedMessage = message.replace(" ", "%20")
            return "https://wa.me/$phoneNumber?text=$encodedMessage"
        }
    }
}
