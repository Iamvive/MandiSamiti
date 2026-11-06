package com.appwork.mandisamiti.ui.sell

import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.ui.buy.BuyPartyResolver

/**
 * Phone-first party rules for the Sell form.
 * Reuses BuyPartyResolver for matching and existing lookup, and saves parties with PartyType.BUYER.
 */
object SellPartyResolver {

    fun matches(parties: List<Party>, phone: String, name: String): List<Party> =
        BuyPartyResolver.matches(parties, phone, name)

    fun existing(parties: List<Party>, selectedId: String?, phone: String): Party? =
        BuyPartyResolver.existing(parties, selectedId, phone)

    fun pick(form: SellEntryFormState, party: Party) {
        form.selectedBuyerId = party.id
        form.buyerName = party.name
        form.updatePhone(party.phone.orEmpty())
        form.village = party.village.orEmpty()
        party.gstin?.let { form.gstin = it }
    }

    fun needsPhonePrompt(form: SellEntryFormState): Boolean =
        form.selectedBuyerId != null && form.phone.isBlank()

    fun willRename(form: SellEntryFormState, existing: Party?): Boolean =
        existing != null && form.buyerName.trim() != existing.name

    fun partyToSave(
        form: SellEntryFormState,
        shopId: String,
        existing: Party?,
        now: Long,
        newId: String = IdGenerator.newId(),
    ): Party? {
        val name = form.buyerName.trim()
        val phone = form.phone.ifBlank { null }
        val village = form.village.ifBlank { null }
        val gstin = form.gstin.trim().ifBlank { null }
        if (existing == null) {
            return Party(
                id = newId,
                shopId = shopId,
                name = name,
                phone = phone,
                village = village,
                partyType = PartyType.BUYER,
                gstin = gstin,
                createdAt = now,
                updatedAt = now,
            )
        }
        if (existing.name == name && existing.phone == phone && existing.village == village && existing.gstin == gstin) {
            return null
        }
        return existing.copy(
            name = name,
            phone = phone,
            village = village,
            gstin = gstin ?: existing.gstin,
            updatedAt = now,
            syncStatus = 0
        )
    }
}

