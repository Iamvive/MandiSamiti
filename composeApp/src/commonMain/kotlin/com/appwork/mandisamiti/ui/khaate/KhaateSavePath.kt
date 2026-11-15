package com.appwork.mandisamiti.ui.khaate

import com.appwork.mandisamiti.domain.repository.PartyRepository

/** Every khaate entry points at the hidden khaate party, so the party row must exist before the entry is written. */
suspend fun saveKhaateAware(
    isKhaate: Boolean,
    shopId: String,
    partyType: String,
    partyRepo: PartyRepository,
    save: suspend () -> Unit,
) {
    if (isKhaate) partyRepo.ensureKhaateParty(shopId, partyType)
    save()
}
