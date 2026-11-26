package com.appwork.mandisamiti.platform

/** Masks Indian mobile numbers in text sent to crash reporting. Mirrors backend `observability._PHONE_RE`. */
object PiiScrubber {
    private val phone = Regex("""(?:\+?91[\s-]?)?[6-9]\d{4}[\s-]?\d{5}""")

    fun scrub(text: String): String = phone.replace(text, "[phone]")
}
