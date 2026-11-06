package com.appwork.mandisamiti.ui.buy

import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.validation.MandiValidators

/**
 * Phone-first party rules for the Buy form. Slips point at the party id and show the party's
 * current name, so renaming the party renames it on every slip, old ones included.
 */
object BuyPartyResolver {
    private const val MIN_PHONE_DIGITS = 3
    private const val MIN_NAME_CHARS = 2

    /** Matches by phone prefix, or by name so old parties with no number can be found. */
    fun matches(parties: List<Party>, phone: String, name: String): List<Party> {
        val digits = phone.filter { it.isDigit() }
        val q = name.trim()
        return when {
            digits.length >= MIN_PHONE_DIGITS -> parties.filter { it.phone?.startsWith(digits) == true }
            q.length >= MIN_NAME_CHARS -> parties.filter { it.name.contains(q, ignoreCase = true) }
            else -> emptyList()
        }.take(5)
    }

    /** The saved party this slip belongs to: the picked one, else an exact phone match (same rule as Save). */
    fun existing(parties: List<Party>, selectedId: String?, phone: String): Party? =
        parties.firstOrNull { it.id == selectedId }
            ?: phone.takeIf { it.isNotBlank() }?.let { ph -> parties.firstOrNull { it.phone == ph } }

    fun pick(form: BuyEntryFormState, party: Party) {
        form.selectedFarmerId = party.id
        form.farmerName = party.name
        form.updatePhone(party.phone.orEmpty())
        form.village = party.village.orEmpty()
    }

    /** A picked party that has no number: the screen asks for it before Save. */
    fun needsPhonePrompt(form: BuyEntryFormState): Boolean =
        form.selectedFarmerId != null && form.phone.isBlank()

    fun willRename(form: BuyEntryFormState, existing: Party?): Boolean =
        existing != null && form.farmerName.trim() != existing.name

    /**
     * The party row to persist with the slip, or null when nothing changed.
     * No [existing] party means a new number: create it once.
     */
    fun partyToSave(
        form: BuyEntryFormState,
        shopId: String,
        existing: Party?,
        now: Long,
        newId: String = IdGenerator.newId(),
    ): Party? {
        val name = form.farmerName.trim()
        val phone = form.phone.ifBlank { null }
        val village = MandiValidators.sanitizeVillage(form.village).ifBlank { null }
        if (existing == null) {
            return Party(
                id = newId, shopId = shopId, name = name, phone = phone, village = village,
                partyType = PartyType.FARMER, createdAt = now, updatedAt = now,
            )
        }
        if (existing.name == name && existing.phone == phone && existing.village == village) return null
        return existing.copy(name = name, phone = phone, village = village, updatedAt = now, syncStatus = 0)
    }
}
