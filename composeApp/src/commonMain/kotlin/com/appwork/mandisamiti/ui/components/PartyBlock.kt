package com.appwork.mandisamiti.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.validation.MandiValidators
import com.appwork.mandisamiti.ui.buy.BuyPartyResolver
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

/** What the party block of a slip shows. */
sealed interface PartyBlockState {
    data object Lookup : PartyBlockState
    data class Saved(val party: Party) : PartyBlockState
    data object New : PartyBlockState
}

fun partyBlockState(
    parties: List<Party>,
    selectedId: String?,
    phone: String,
    newRequested: Boolean,
): PartyBlockState {
    val saved = BuyPartyResolver.existing(parties, selectedId, phone)
    return when {
        saved != null -> PartyBlockState.Saved(saved)
        newRequested || phone.length == 10 -> PartyBlockState.New
        else -> PartyBlockState.Lookup
    }
}

/** True when the number no longer belongs to the picked account, so its details must be cleared. */
fun isSelectionStale(parties: List<Party>, selectedId: String?, newPhone: String): Boolean {
    val selected = parties.firstOrNull { it.id == selectedId } ?: return false
    return !selected.phone.isNullOrBlank() && selected.phone != newPhone
}

/** A saved account with this name and village while the slip has an unknown number. */
fun samePersonCandidate(
    parties: List<Party>,
    selectedId: String?,
    phone: String,
    name: String,
    village: String,
): Party? {
    if (BuyPartyResolver.existing(parties, selectedId, phone) != null) return null
    val n = name.trim()
    val v = village.trim()
    if (n.isEmpty() || v.isEmpty()) return null
    return parties.firstOrNull {
        it.name.trim().equals(n, ignoreCase = true) && it.village?.trim().equals(v, ignoreCase = true)
    }
}

@Composable
fun SavedPartyLine(name: String, village: String, isEnglish: Boolean, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = listOf(name, village).filter { it.isNotBlank() }.joinToString("  •  "),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MandiTextPrimary,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onEdit) { Text(if (isEnglish) "Edit" else "बदलें") }
    }
}

@Composable
fun EditPartyDialog(
    name: String,
    village: String,
    isEnglish: Boolean,
    onDone: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newName by remember { mutableStateOf(name) }
    var newVillage by remember { mutableStateOf(village) }
    val valid = newName.isNotBlank() && MandiValidators.sanitizeVillage(newVillage).isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEnglish) "Edit account" else "खाता बदलें") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MandiFlatTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = if (isEnglish) "Name" else "नाम",
                )
                MandiFlatTextField(
                    value = newVillage,
                    onValueChange = { newVillage = it },
                    label = if (isEnglish) "Village" else "गांव",
                )
                Text(
                    text = if (isEnglish) {
                        "Saved with this slip. Applies to this account's old slips too."
                    } else {
                        "पर्ची के साथ सेव होगा। इस खाते की पुरानी पर्चियों पर भी लागू होगा।"
                    },
                    fontSize = 12.sp,
                    color = MandiTextSecondary,
                )
            }
        },
        confirmButton = {
            Button(enabled = valid, onClick = { onDone(newName.trim(), newVillage) }) {
                Text(if (isEnglish) "Done" else "ठीक है")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (isEnglish) "Cancel" else "रद्द करें") } },
    )
}

@Composable
fun SamePersonDialog(
    candidate: Party,
    isEnglish: Boolean,
    onSamePerson: () -> Unit,
    onNewAccount: () -> Unit,
    onDismiss: () -> Unit,
) {
    val who = listOfNotNull(candidate.name, candidate.village, candidate.phone).joinToString("  •  ")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEnglish) "Same person with a new number?" else "वही व्यक्ति, नया नंबर?") },
        text = { Text(if (isEnglish) "Already saved: $who" else "पहले से सेव: $who") },
        confirmButton = {
            Button(onClick = onSamePerson) { Text(if (isEnglish) "Yes, same person" else "हाँ, वही है") }
        },
        dismissButton = {
            TextButton(onClick = onNewAccount) { Text(if (isEnglish) "No, new account" else "नहीं, नया खाता") }
        },
    )
}
