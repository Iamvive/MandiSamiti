package com.appwork.mandisamiti.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.appwork.mandisamiti.domain.model.Party

/** What a typed party name means at save time (P4: a partial name must never silently create a party). */
sealed interface TypedPartyCheck {
    data object Blank : TypedPartyCheck
    data class Selected(val id: String) : TypedPartyCheck
    data class ExactMatch(val party: Party) : TypedPartyCheck
    data class NeedsConfirm(val typed: String, val similar: List<Party>) : TypedPartyCheck
}

fun checkTypedParty(typed: String, selectedId: String?, parties: List<Party>): TypedPartyCheck {
    if (selectedId != null) return TypedPartyCheck.Selected(selectedId)
    val name = typed.trim()
    if (name.isEmpty()) return TypedPartyCheck.Blank
    val exact = parties.filter { it.name.trim().equals(name, ignoreCase = true) || (it.phone != null && it.phone == name) }
    if (exact.size == 1) return TypedPartyCheck.ExactMatch(exact.single())
    val similar = parties.filter { it.name.contains(name, ignoreCase = true) }.take(3)
    return TypedPartyCheck.NeedsConfirm(name, similar)
}

@Composable
fun NewPartyConfirmDialog(
    check: TypedPartyCheck.NeedsConfirm,
    isEnglish: Boolean,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEnglish) "Create new account?" else "नया खाता बनाएं?") },
        text = {
            val similar = check.similar.joinToString(", ") { it.name }
            val base = if (isEnglish) "\"${check.typed}\" is not in your list." else "\"${check.typed}\" आपकी सूची में नहीं है।"
            val hint = when {
                similar.isEmpty() -> ""
                isEnglish -> "\nDid you mean: $similar? Go back and pick from the list."
                else -> "\nक्या आपका मतलब: $similar? वापस जाकर सूची से चुनें।"
            }
            Text(base + hint)
        },
        confirmButton = { Button(onClick = onCreate) { Text(if (isEnglish) "Create new" else "नया बनाएं") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (isEnglish) "Go back" else "वापस जाएं") } },
    )
}
