package com.appwork.mandisamiti.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiRedPayable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

/**
 * Standard flat grey-filled box text field conforming strictly to B final & NGDL §5:
 * - Label sits above the box (12sp, muted).
 * - Box is flat grey-filled (MandiSurfaceElevated, 8dp squircle, 1dp MandiBorder).
 * - 56dp tall for tall fields (default), with 20sp text (FontWeight.Medium).
 * - Single-line with cursor and placeholder.
 */
@Composable
fun MandiFlatTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    supportingText: String? = null,
    isTall: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MandiTextSecondary
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = if (isTall) 56.dp else 48.dp)
                .background(MandiSurfaceElevated, RoundedCornerShape(8.dp))
                .border(
                    BorderStroke(
                        1.dp,
                        if (isError) MandiRedPayable else MandiBorder
                    ),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                readOnly = readOnly,
                textStyle = TextStyle(
                    fontSize = if (isTall) 20.sp else 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MandiTextPrimary
                ),
                cursorBrush = SolidColor(MandiTextPrimary),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = true,
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty() && placeholder != null) {
                                Text(
                                    text = placeholder,
                                    fontSize = if (isTall) 20.sp else 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MandiTextMuted
                                )
                            }
                            innerTextField()
                        }
                        if (trailingIcon != null) {
                            trailingIcon()
                        }
                    }
                }
            )
        }
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                fontSize = 11.sp,
                color = MandiRedPayable
            )
        } else if (supportingText != null) {
            Text(
                text = supportingText,
                fontSize = 11.sp,
                color = MandiTextSecondary
            )
        }
    }
}

/**
 * Direction B final Party Lookup Field ("मोबाइल नंबर या नाम"):
 * - Renders a flat grey-filled box with 56dp height and 20sp text.
 * - Searches parties by phone prefix (if digits) or name (if text).
 * - Shows dropdown overlay of matching parties when typing.
 * - Selecting a party fills phone, name, and village.
 */
@Composable
fun MandiPartyLookupField(
    phoneValue: String,
    onPhoneChange: (String) -> Unit,
    onPartySelected: (Party) -> Unit,
    parties: List<Party>,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // When phoneValue changes externally, synchronize searchQuery if not actively typing query
    val effectiveQuery = if (searchQuery.isNotEmpty()) searchQuery else phoneValue
    val trimmed = effectiveQuery.trim()

    val matches = remember(parties, trimmed) {
        if (trimmed.isEmpty()) {
            emptyList()
        } else {
            val digits = trimmed.filter { it.isDigit() }
            if (digits.length >= 2) {
                parties.filter { it.phone?.startsWith(digits) == true || it.name.contains(trimmed, ignoreCase = true) }.take(5)
            } else if (trimmed.length >= 2) {
                parties.filter { it.name.contains(trimmed, ignoreCase = true) || it.village?.contains(trimmed, ignoreCase = true) == true }.take(5)
            } else {
                emptyList()
            }
        }
    }

    val showDropdown = expanded && matches.isNotEmpty() &&
        !matches.any { (it.phone == trimmed || it.name.equals(trimmed, ignoreCase = true)) && matches.size == 1 }

    Column(modifier = modifier.fillMaxWidth()) {
        MandiFlatTextField(
            value = if (searchQuery.isNotEmpty()) searchQuery else phoneValue,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            onValueChange = { raw ->
                val input = raw.filter { it.isDigit() }.take(10)
                searchQuery = input
                expanded = true
                onPhoneChange(input)
                val digits = input
                if (digits.length == 10) {
                    val exact = parties.firstOrNull { it.phone == digits }
                    if (exact != null) {
                        onPartySelected(exact)
                    }
                }
            },
            label = label,
            placeholder = placeholder,
            isError = isError,
            errorMessage = errorMessage,
            isTall = true
        )

        if (showDropdown) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MandiBorder),
                colors = CardDefaults.cardColors(containerColor = MandiSurface)
            ) {
                Column {
                    matches.forEachIndexed { index, party ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expanded = false
                                    searchQuery = ""
                                    onPartySelected(party)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = party.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MandiTextPrimary
                                )
                                val subDetails = buildList {
                                    if (!party.phone.isNullOrBlank()) add("फ़ोन: ${party.phone}")
                                    if (!party.village.isNullOrBlank()) add("गाँव: ${party.village}")
                                }.joinToString("  •  ")
                                if (subDetails.isNotBlank()) {
                                    Text(
                                        text = subDetails,
                                        fontSize = 12.sp,
                                        color = MandiTextSecondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "चुनें",
                                tint = MandiTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (index < matches.size - 1) {
                            HorizontalDivider(
                                color = MandiBorder,
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        }
    }
}
