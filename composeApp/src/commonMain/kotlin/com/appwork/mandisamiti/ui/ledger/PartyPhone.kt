package com.appwork.mandisamiti.ui.ledger

/** What the party phone field keeps of typed/pasted text: digits only, at most 10. */
fun sanitizePartyPhone(raw: String): String = raw.filter { it in '0'..'9' }.take(10)

/** Phone is optional; when given it must be a 10-digit Indian mobile starting 6–9. */
fun isValidPartyPhone(phone: String): Boolean =
    phone.isEmpty() || (phone.length == 10 && phone.all { it in '0'..'9' } && phone[0] in '6'..'9')
