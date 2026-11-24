package com.appwork.mandisamiti.ui.register

/** A kisan khata to create while saving a Galla-Rokad cash entry. */
data class NewKisan(val name: String, val phone: String?, val village: String?)

/** Where a Galla-Rokad cash entry is booked: a khata, a new kisan, or no khata (name kept in remarks). */
data class CashEntryTarget(val partyId: String, val remarks: String?, val newKisan: NewKisan?)

/**
 * Decides the party for the cash popup. Never falls back to another party:
 * with no khata picked the entry has no party, and a typed name is kept in remarks.
 */
fun cashEntryTarget(
    selectedPartyId: String?,
    typedName: String,
    addingNewKisan: Boolean,
    phone: String,
    village: String,
    note: String,
): CashEntryTarget {
    val name = typedName.trim()
    val cleanNote = note.trim().ifBlank { null }
    if (!selectedPartyId.isNullOrBlank()) return CashEntryTarget(selectedPartyId, cleanNote, null)
    if (addingNewKisan && name.isNotBlank()) {
        val kisan = NewKisan(
            name = name,
            phone = phone.filter { it.isDigit() }.ifBlank { null },
            village = village.trim().ifBlank { null },
        )
        return CashEntryTarget("", cleanNote, kisan)
    }
    val remarks = listOfNotNull(name.ifBlank { null }, cleanNote).joinToString(" · ").ifBlank { null }
    return CashEntryTarget("", remarks, null)
}
