package com.appwork.mandisamiti.ui.components

import com.appwork.mandisamiti.domain.model.Party

/** Pure logic behind [MandiPartyLookupField], kept separate so it is unit-testable. */
object PartyLookup {
    data class Input(val query: String, val phoneToEmit: String?)

    /** Number mode keeps up to 10 digits and reports them as a phone. Text mode is search-only. */
    fun onInput(raw: String, textMode: Boolean): Input =
        if (textMode) {
            Input(query = raw, phoneToEmit = null)
        } else {
            val digits = raw.filter { it.isDigit() }.take(10)
            Input(query = digits, phoneToEmit = digits)
        }

    fun matches(parties: List<Party>, query: String): List<Party> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val digits = trimmed.filter { it.isDigit() }
        return when {
            digits.length >= 2 -> parties.filter {
                it.phone?.startsWith(digits) == true || it.name.contains(trimmed, ignoreCase = true)
            }
            trimmed.length >= 2 -> parties.filter {
                it.name.contains(trimmed, ignoreCase = true) ||
                    it.village?.contains(trimmed, ignoreCase = true) == true
            }
            else -> emptyList()
        }.take(5)
    }
}
