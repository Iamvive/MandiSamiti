package com.appwork.mandisamiti.ui.lend

import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.validation.MandiValidators
import com.appwork.mandisamiti.ui.buy.BuyPartyResolver

/**
 * Phone-first party resolution rules for the Lend form.
 * Reuses BuyPartyResolver for matching and existing lookup, and saves new borrower parties with PartyType.FARMER.
 */
object LendPartyResolver {

    fun matches(parties: List<Party>, phone: String, name: String): List<Party> =
        BuyPartyResolver.matches(parties, phone, name)

    fun existing(parties: List<Party>, selectedId: String?, phone: String): Party? =
        BuyPartyResolver.existing(parties, selectedId, phone)

    fun pick(form: LendEntryFormState, party: Party) {
        form.selectedPartyId = party.id
        form.partyName = party.name
        form.updatePhone(party.phone.orEmpty())
        form.village = party.village.orEmpty()
    }

    fun needsPhonePrompt(form: LendEntryFormState): Boolean =
        form.selectedPartyId != null && form.phone.isBlank()

    fun willRename(form: LendEntryFormState, existing: Party?): Boolean =
        existing != null && form.partyName.trim() != existing.name

    fun partyToSave(
        form: LendEntryFormState,
        shopId: String,
        existing: Party?,
        now: Long,
        newId: String = IdGenerator.newId(),
    ): Party? {
        val name = form.partyName.trim()
        val phone = form.phone.ifBlank { null }
        val village = MandiValidators.sanitizeVillage(form.village).ifBlank { null }
        if (existing == null) {
            return Party(
                id = newId,
                shopId = shopId,
                name = name,
                phone = phone,
                village = village,
                partyType = PartyType.FARMER,
                createdAt = now,
                updatedAt = now,
            )
        }
        if (existing.name == name && existing.phone == phone && existing.village == village) {
            return null
        }
        return existing.copy(
            name = name,
            phone = phone,
            village = village,
            updatedAt = now,
            syncStatus = 0
        )
    }
}

