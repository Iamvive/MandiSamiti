package com.appwork.mandisamiti.platform

enum class WhatsAppApp(val packageName: String, val label: String) {
    PERSONAL("com.whatsapp", "WhatsApp"),
    BUSINESS("com.whatsapp.w4b", "WhatsApp Business")
}

/** Click-to-chat link. [phone] is a 10-digit Indian mobile, or null to pick the contact inside WhatsApp. */
fun whatsAppSendUrl(phone: String?, text: String): String =
    "https://wa.me/${phone?.let { "91$it" }.orEmpty()}?text=${percentEncode(text)}"

private fun percentEncode(text: String): String = buildString {
    for (byte in text.encodeToByteArray()) {
        val code = byte.toInt() and 0xFF
        val char = code.toChar()
        if (char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char in "-_.~") {
            append(char)
        } else {
            append('%').append(code.toString(16).uppercase().padStart(2, '0'))
        }
    }
}
