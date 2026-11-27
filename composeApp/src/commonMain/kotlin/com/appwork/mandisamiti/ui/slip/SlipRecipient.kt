package com.appwork.mandisamiti.ui.slip

import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.ui.ledger.isValidPartyPhone

/** The 10-digit mobile in [raw], or null when it is not clearly one Indian mobile number. */
fun whatsAppPhone(raw: String?): String? {
    if (raw == null || raw.any { it !in '0'..'9' && it !in " +-()" }) return null
    val digits = raw.filter { it in '0'..'9' }
    val local = when {
        digits.length == 12 && digits.startsWith("91") -> digits.drop(2)
        digits.length == 11 && digits.startsWith("0") -> digits.drop(1)
        else -> digits
    }
    return local.takeIf(::isValidPartyPhone)
}

/** The party account number wins; the number stored on the slip is the fallback. */
fun slipRecipientPhone(accountPhone: String?, slipPhone: String?): String? =
    whatsAppPhone(accountPhone) ?: whatsAppPhone(slipPhone)

/** Opened from the buyer's ledger it goes to the buyer; in every other case to the farmer. */
fun receiptPreviewRecipient(farmer: Party, buyer: Party?, ledgerPartyId: String?): Party =
    if (buyer != null && buyer.id == ledgerPartyId) buyer else farmer
