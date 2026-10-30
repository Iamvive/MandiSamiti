package com.appwork.mandisamiti.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.DuplicatePhoneException
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.ui.components.PartyCard
import com.appwork.mandisamiti.ui.home.HomeUiState
import com.appwork.mandisamiti.ui.home.PartyFilter
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Store
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiNavy
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary
import kotlinx.coroutines.launch

@Composable
fun KhataLedgerTabScreen(
    uiState: HomeUiState,
    onSearchQueryChanged: (String) -> Unit,
    onFilterSelected: (PartyFilter) -> Unit,
    onPartyClick: (Party) -> Unit,
    onCreateParty: suspend (name: String, village: String?, phone: String?, type: PartyType) -> Party,
    onUpdatePartyPhone: suspend (party: Party, phone: String) -> Party,
    onNavigateToStockSheet: () -> Unit = {},
    isEnglish: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showAddPartyDialog by remember { mutableStateOf(false) }
    var partyNameInput by remember { mutableStateOf("") }
    var partyVillageInput by remember { mutableStateOf("") }
    var partyPhoneInput by remember { mutableStateOf("") }
    var selectedPartyType by remember { mutableStateOf(PartyType.FARMER) }
    var duplicateParty by remember { mutableStateOf<Party?>(null) }
    var phoneEditParty by remember { mutableStateOf<Party?>(null) }
    var phoneEditInput by remember { mutableStateOf("") }
    var phoneEditDuplicate by remember { mutableStateOf<Party?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddPartyDialog = true },
                containerColor = MandiPrimaryAction,
                contentColor = MandiPrimaryActionText,
                shape = RoundedCornerShape(14.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = MandiPrimaryActionText,
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = {
                    Text(
                        text = if (isEnglish) "Add Party" else "नया खाता",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiPrimaryActionText
                    )
                },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        },
        containerColor = MandiBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Full-Width Search Input
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = {
                        Text(
                            text = if (isEnglish) "Search farmer, trader or village..." else "किसान, व्यापारी या गाँव का नाम खोजें...",
                            fontSize = 14.sp,
                            color = MandiTextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MandiTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "साफ करें",
                                    tint = MandiTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MandiSurface,
                        unfocusedContainerColor = MandiSurface,
                        focusedBorderColor = MandiTextPrimary,
                        unfocusedBorderColor = MandiBorder
                    )
                )
            }

            // 2. Godown Stock Tile (Warehouse Inventory Audit)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MandiSurfaceElevated)
                        .border(1.dp, MandiBorder, RoundedCornerShape(10.dp))
                        .clickable { onNavigateToStockSheet() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = MandiNavy,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (isEnglish) "Godown Stock (Inventory)" else "गोदाम स्टॉक (जिंस इन्वेंटरी)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiNavy
                            )
                            Text(
                                text = if (isEnglish) "Check bags and weight in warehouse" else "गोदाम में उपलब्ध बोरी एवं वजन देखें",
                                fontSize = 11.sp,
                                color = MandiTextSecondary
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "View Stock" else "स्टॉक देखें",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiAmberDark
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MandiAmberDark,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // 3. Segmented Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KhataFilterChip(
                        label = if (isEnglish) "All (${uiState.allParties.size})" else "सभी (${uiState.allParties.size})",
                        isSelected = uiState.activeFilter == PartyFilter.ALL,
                        onClick = { onFilterSelected(PartyFilter.ALL) },
                        modifier = Modifier.weight(1f)
                    )
                    KhataFilterChip(
                        label = if (isEnglish) "Receivable" else "लेना है",
                        isSelected = uiState.activeFilter == PartyFilter.RECEIVABLE,
                        onClick = { onFilterSelected(PartyFilter.RECEIVABLE) },
                        modifier = Modifier.weight(1f)
                    )
                    KhataFilterChip(
                        label = if (isEnglish) "Payable" else "देना है",
                        isSelected = uiState.activeFilter == PartyFilter.PAYABLE,
                        onClick = { onFilterSelected(PartyFilter.PAYABLE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Party Accounts Stream
            items(uiState.filteredParties, key = { it.party.id }) { partyWithBalance ->
                PartyCard(
                    party = partyWithBalance.party,
                    balancePaisa = partyWithBalance.balancePaisa,
                    isEnglish = isEnglish,
                    onPhoneNeededClick = {
                        phoneEditParty = partyWithBalance.party
                        phoneEditInput = ""
                        phoneEditDuplicate = null
                    },
                    onClick = { onPartyClick(partyWithBalance.party) }
                )
            }

            if (uiState.filteredParties.isEmpty() && !uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEnglish) "No matching accounts found" else "कोई खाता नहीं मिला",
                            color = MandiTextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp)) // Extra padding for FAB
            }
        }
    }

    if (showAddPartyDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddPartyDialog = false
                partyNameInput = ""
                partyVillageInput = ""
                partyPhoneInput = ""
            },
            title = {
                Text(
                    text = if (isEnglish) "Create New Account" else "नया खाता बनाएं",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Party Type selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KhataFilterChip(
                            label = if (isEnglish) "Farmer" else "किसान",
                            isSelected = selectedPartyType == PartyType.FARMER,
                            onClick = { selectedPartyType = PartyType.FARMER },
                            modifier = Modifier.weight(1f)
                        )
                        KhataFilterChip(
                            label = if (isEnglish) "Buyer" else "व्यापारी",
                            isSelected = selectedPartyType == PartyType.BUYER,
                            onClick = { selectedPartyType = PartyType.BUYER },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = partyNameInput,
                        onValueChange = { partyNameInput = it },
                        label = { Text(if (isEnglish) "Party Name *" else "नांव / नाम *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = partyVillageInput,
                        onValueChange = { partyVillageInput = it },
                        label = { Text(if (isEnglish) "Village / Location" else "गांव / पता (वैकल्पिक)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val phoneInvalid = !isValidPartyPhone(partyPhoneInput)
                    OutlinedTextField(
                        value = partyPhoneInput,
                        onValueChange = {
                            partyPhoneInput = sanitizePartyPhone(it)
                            duplicateParty = null
                        },
                        label = { Text(if (isEnglish) "Mobile Number *" else "मोबाइल नंबर *") },
                        isError = phoneInvalid,
                        supportingText = if (phoneInvalid) {
                            { Text(if (isEnglish) "Enter a 10-digit mobile number" else "10 अंकों का मोबाइल नंबर डालें (6–9 से शुरू)") }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    duplicateParty?.let { existing ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isEnglish) {
                                    "This number already belongs to ${existing.name}${existing.village?.let { ", $it" } ?: ""}"
                                } else {
                                    "यह नंबर पहले से ${existing.name}${existing.village?.let { ", $it" } ?: ""} के नाम पर है"
                                },
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                            )
                            TextButton(onClick = {
                                showAddPartyDialog = false
                                duplicateParty = null
                                onPartyClick(existing)
                            }) {
                                Text(if (isEnglish) "Open account" else "खाता खोलें")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = partyNameInput.trim()
                        if (trimmedName.isNotBlank() && isValidPartyPhone(partyPhoneInput)) {
                            coroutineScope.launch {
                                try {
                                    val created = onCreateParty(
                                        trimmedName,
                                        partyVillageInput.trim().ifBlank { null },
                                        partyPhoneInput,
                                        selectedPartyType
                                    )
                                    showAddPartyDialog = false
                                    partyNameInput = ""
                                    partyVillageInput = ""
                                    partyPhoneInput = ""
                                    onPartyClick(created)
                                } catch (duplicate: DuplicatePhoneException) {
                                    duplicateParty = duplicate.existing
                                }
                            }
                        }
                    },
                    enabled = partyNameInput.trim().isNotBlank() && isValidPartyPhone(partyPhoneInput),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MandiPrimaryAction,
                        contentColor = MandiPrimaryActionText
                    )
                ) {
                    Text(
                        text = if (isEnglish) "Save Account" else "खाता बनाएं",
                        fontWeight = FontWeight.Bold,
                        color = MandiPrimaryActionText
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddPartyDialog = false
                        partyNameInput = ""
                        partyVillageInput = ""
                        partyPhoneInput = ""
                    }
                ) {
                    Text(
                        text = if (isEnglish) "Cancel" else "रद्द करें",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }

    phoneEditParty?.let { party ->
        AlertDialog(
            onDismissRequest = { phoneEditParty = null },
            title = { Text(if (isEnglish) "Add phone number" else "फ़ोन नंबर जोड़ें") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(party.name, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = phoneEditInput,
                        onValueChange = {
                            phoneEditInput = sanitizePartyPhone(it)
                            phoneEditDuplicate = null
                        },
                        label = { Text(if (isEnglish) "Mobile Number" else "मोबाइल नंबर") },
                        isError = phoneEditInput.isNotEmpty() && !isValidPartyPhone(phoneEditInput),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                    )
                    phoneEditDuplicate?.let { existing ->
                        Text(
                            text = if (isEnglish) {
                                "This number already belongs to ${existing.name}"
                            } else {
                                "यह नंबर पहले से ${existing.name} के नाम पर है"
                            },
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = isValidPartyPhone(phoneEditInput),
                    onClick = {
                        coroutineScope.launch {
                            try {
                                onUpdatePartyPhone(party, phoneEditInput)
                                phoneEditParty = null
                                phoneEditInput = ""
                            } catch (duplicate: DuplicatePhoneException) {
                                phoneEditDuplicate = duplicate.existing
                            }
                        }
                    },
                ) {
                    Text(if (isEnglish) "Save" else "सहेजें")
                }
            },
            dismissButton = {
                TextButton(onClick = { phoneEditParty = null }) {
                    Text(if (isEnglish) "Cancel" else "रद्द करें")
                }
            },
        )
    }
}

@Composable
private fun KhataFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isSelected) MandiPrimaryAction else MandiSurface)
            .border(1.dp, if (isSelected) MandiPrimaryAction else MandiBorder, shape)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MandiPrimaryActionText else MandiTextSecondary
        )
    }
}
