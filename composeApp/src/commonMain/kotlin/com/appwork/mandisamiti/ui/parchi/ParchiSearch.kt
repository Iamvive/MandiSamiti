package com.appwork.mandisamiti.ui.parchi

import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.SellEntry

data class SlipPartyGroup<T>(
    val party: Party,
    val unpaid: List<T>,
    val paid: List<T>,
    val totalDuePaisa: Long
)

fun buyDuePaisa(buy: BuyEntry): Long = (buy.netPayablePaisa - buy.paidPaisa).coerceAtLeast(0L)

fun sellDuePaisa(sell: SellEntry): Long = sell.balanceDuePaisa.coerceAtLeast(0L)

/** Last 10 digits of a phone, dropping +91 / 91 / leading 0 and any spaces or dashes. */
private fun phoneDigits(raw: String?): String =
    raw.orEmpty().filter { it.isDigit() }.takeLast(10)

private fun queryPhoneDigits(query: String): String {
    var d = query.filter { it.isDigit() }
    if (d.length > 10 && d.startsWith("91")) d = d.drop(2)
    if (d.length == 11 && d.startsWith("0")) d = d.drop(1)
    return d
}

private fun partyMatches(party: Party, query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return false
    val hasLetters = q.any { it.isLetter() }
    return if (hasLetters) {
        party.name.contains(q, ignoreCase = true) || party.village.orEmpty().contains(q, ignoreCase = true)
    } else {
        val d = queryPhoneDigits(q)
        d.length >= 3 && phoneDigits(party.phone).contains(d)
    }
}

/**
 * Groups [slips] by matching party. Unpaid (due > 0) newest first, paid newest first.
 * Returns an empty list when [query] is blank or nothing matches.
 */
fun <T> filterSlipsByParty(
    query: String,
    slips: List<T>,
    parties: List<Party>,
    partyIdOf: (T) -> String,
    dueOf: (T) -> Long,
    dateOf: (T) -> Long
): List<SlipPartyGroup<T>> {
    if (query.isBlank()) return emptyList()
    val matched = parties.filter { !it.isDeleted && partyMatches(it, query) }
    return matched.mapNotNull { party ->
        val mine = slips.filter { partyIdOf(it) == party.id }.sortedByDescending(dateOf)
        if (mine.isEmpty()) return@mapNotNull null
        val (unpaid, paid) = mine.partition { dueOf(it) > 0L }
        SlipPartyGroup(party, unpaid, paid, unpaid.sumOf(dueOf))
    }.sortedBy { it.party.name.lowercase() }
}
