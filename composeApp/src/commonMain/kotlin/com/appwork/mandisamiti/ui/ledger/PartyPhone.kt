package com.appwork.mandisamiti.ui.ledger

/** Last ten digits of typed or pasted text, matching the domain normalizer. */
fun sanitizePartyPhone(raw: String): String = raw.filter { it in '0'..'9' }.takeLast(10)

/** A manually created party requires a 10-digit Indian mobile starting 6–9. */
fun isValidPartyPhone(phone: String): Boolean =
    phone.length == 10 && phone.all { it in '0'..'9' } && phone[0] in '6'..'9'
