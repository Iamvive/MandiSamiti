package com.appwork.mandisamiti.ui.lend

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendStatus
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.repository.LendRepository
import com.appwork.mandisamiti.domain.validation.MandiValidators
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.ui.components.DiscardDraftDialog
import com.appwork.mandisamiti.ui.components.MandiFlatTextField
import com.appwork.mandisamiti.ui.components.MandiPartyLookupField
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiSurface
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.flow.emptyFlow
import androidx.compose.runtime.collectAsState

private const val MILLIS_PER_DAY = 86_400_000L

private class LoanContactDraft {
    var phone by mutableStateOf("")
    var name by mutableStateOf("")
    var village by mutableStateOf("")
    var selectedPartyId by mutableStateOf<String?>(null)

    fun updatePhone(input: String, parties: List<Party>) {
        val digits = input.filter { it in '0'..'9' }.take(10)
        phone = digits
        val match = parties.firstOrNull { !it.isDeleted && it.phone == digits }
        if (match == null || selectedPartyId != match.id) {
            selectedPartyId = null
            name = ""
            village = ""
        }
    }

    fun select(party: Party) {
        selectedPartyId = party.id
        phone = party.phone.orEmpty()
        name = party.name
        village = party.village.orEmpty()
    }

    val isValid: Boolean
        get() = MandiValidators.validatePhone(phone, isRequired = true).isValid &&
            name.isNotBlank() && village.trim().isNotBlank()

    val isDirty: Boolean
        get() = phone.isNotBlank() || name.isNotBlank() || village.isNotBlank()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LoanAdvanceEntryScreen(
    shopId: String,
    parties: List<Party>,
    lendRepository: LendRepository? = null,
    isEnglish: Boolean,
    onLoanGiven: (LendEntry, Party?) -> Unit,
    onLoanReceived: (CashTransaction, Party?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val recentLends by (lendRepository?.getLendEntriesStream(shopId) ?: emptyFlow()).collectAsState(initial = emptyList())
    val lender = remember { LoanContactDraft() }
    val cashRecipient = remember { LoanContactDraft() }
    val beneficiary = remember { LoanContactDraft() }
    val defaultDate = remember { todayMillis() }
    var useBeneficiary by remember { mutableStateOf(false) }
    var receivedAmount by remember { mutableStateOf("") }
    var givenAmount by remember { mutableStateOf("") }
    var receivedDate by remember { mutableStateOf(defaultDate) }
    var givenDate by remember { mutableStateOf(defaultDate) }
    var showReceivedDatePicker by remember { mutableStateOf(false) }
    var showGivenDatePicker by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isDirty = lender.isDirty || cashRecipient.isDirty || beneficiary.isDirty ||
        receivedAmount.isNotBlank() || givenAmount.isNotBlank() || useBeneficiary ||
        receivedDate != defaultDate || givenDate != defaultDate
    MandiBackHandler(enabled = true) {
        if (isDirty) showDiscardDialog = true else onBack()
    }

    val receivedValid = lender.isValid && wholeRupeePaisa(receivedAmount) != null
    val givenValid = cashRecipient.isValid && (!useBeneficiary || beneficiary.isValid) &&
        wholeRupeePaisa(givenAmount) != null

    val villageSuggestions = remember(parties, recentLends) {
        (parties.mapNotNull { it.village?.trim()?.takeIf(String::isNotEmpty) } +
            recentLends.mapNotNull { it.notes?.extractRecipientVillage() })
            .distinct().sorted()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(if (isEnglish) "Record Loan / Advance" else "उधार / पेशगी दर्ज करें") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = {
                        if (isDirty) showDiscardDialog = true else onBack()
                    }) {
                        androidx.compose.material3.Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEnglish) "Back" else "पीछे"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MandiSurface,
                border = BorderStroke(1.dp, MandiBorder),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().imePadding().navigationBarsPadding()
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Button(
                        onClick = {
                            val now = Clock.System.now().toEpochMilliseconds()
                            if (pagerState.currentPage == 0) {
                                val existing = findParty(parties, lender)
                                val savedParty = partyFor(lender, shopId, existing, now)
                                val partyId = existing?.id ?: savedParty?.id ?: return@Button
                                val transaction = CashTransaction(
                                    id = IdGenerator.newId(),
                                    shopId = shopId,
                                    partyId = partyId,
                                    transactionType = TransactionType.JAMA_RECEIVED,
                                    amountPaisa = wholeRupeePaisa(receivedAmount) ?: return@Button,
                                    paymentMode = PaymentMode.CASH,
                                    transactionDate = receivedDate,
                                    remarks = if (isEnglish) "Loan received by shop [LOAN_RECEIVED]" else "दुकान को उधार मिला [LOAN_RECEIVED]",
                                    createdAt = now,
                                    updatedAt = now,
                                )
                                onLoanReceived(transaction, savedParty)
                            } else {
                                val accountContact = if (useBeneficiary) beneficiary else cashRecipient
                                val existing = findParty(parties, accountContact)
                                val savedParty = partyFor(accountContact, shopId, existing, now)
                                val partyId = existing?.id ?: savedParty?.id ?: return@Button
                                val receiverSummary = if (isEnglish) {
                                    "Given to: ${cashRecipient.name.trim()} (${cashRecipient.phone}), Village: ${cashRecipient.village.trim()}"
                                } else {
                                    "पैसा दिया: ${cashRecipient.name.trim()} (${cashRecipient.phone}), गाँव: ${cashRecipient.village.trim()}"
                                }
                                val entry = LendEntry(
                                    id = IdGenerator.newId(),
                                    shopId = shopId,
                                    partyId = partyId,
                                    lendType = LendType.MONEY,
                                    entryDate = givenDate,
                                    amountPaisa = wholeRupeePaisa(givenAmount) ?: return@Button,
                                    notes = receiverSummary,
                                    status = LendStatus.OPEN,
                                    createdAt = now,
                                    updatedAt = now,
                                )
                                onLoanGiven(entry, savedParty)
                            }
                        },
                        enabled = if (pagerState.currentPage == 0) receivedValid else givenValid,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            when {
                                pagerState.currentPage == 0 && isEnglish -> "Save loan received"
                                pagerState.currentPage == 0 -> "दुकान को मिला उधार दर्ज करें"
                                isEnglish -> "Save loan given"
                                else -> "दिया गया उधार दर्ज करें"
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                listOf(
                    if (isEnglish) "Loan Received" else "उधार मिला",
                    if (isEnglish) "Loan Given" else "उधार दिया",
                ).forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title, fontWeight = if (pagerState.currentPage == index) FontWeight.SemiBold else FontWeight.Normal) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (page == 0) {
                        Text(
                            if (isEnglish) "Record money received by the shop from a lender." else "दुकान को उधार देने वाले व्यक्ति का विवरण दर्ज करें।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ContactFields(
                            contact = lender,
                            parties = parties,
                            villages = villageSuggestions,
                            phoneLabel = if (isEnglish) "Lender mobile number *" else "लेंडर का मोबाइल नंबर *",
                            nameLabel = if (isEnglish) "Lender name *" else "लेंडर का नाम *",
                            villageLabel = if (isEnglish) "Lender village *" else "लेंडर का गाँव *",
                            isEnglish = isEnglish,
                        )
                        WholeRupeeField(
                            value = receivedAmount,
                            onValueChange = { receivedAmount = acceptWholeRupeesInput(receivedAmount, it) },
                            label = if (isEnglish) "Amount (₹) *" else "राशि (₹) *",
                            isEnglish = isEnglish,
                        )
                        TransactionDateField(
                            dateMillis = receivedDate,
                            onPickDate = { showReceivedDatePicker = true },
                            isEnglish = isEnglish,
                        )
                    } else {
                        Text(
                            if (isEnglish) "Record who physically received the cash. Add a beneficiary if it belongs to someone else." else "पैसा किसने लिया दर्ज करें। किसी और के खाते में जोड़ना हो तो लाभार्थी चुनें।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ContactFields(
                            contact = cashRecipient,
                            parties = parties,
                            villages = villageSuggestions,
                            phoneLabel = if (isEnglish) "Given-to mobile number *" else "पैसा लेने वाले का मोबाइल नंबर *",
                            nameLabel = if (isEnglish) "Given to *" else "पैसा किसे दिया *",
                            villageLabel = if (isEnglish) "Recipient village *" else "पैसा लेने वाले का गाँव *",
                            isEnglish = isEnglish,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().toggleable(
                                value = useBeneficiary,
                                role = Role.Checkbox,
                                onValueChange = { useBeneficiary = it },
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = useBeneficiary, onCheckedChange = null)
                            Text(if (isEnglish) "On behalf of someone else" else "किसी और की ओर से")
                        }
                        if (useBeneficiary) {
                            ContactFields(
                                contact = beneficiary,
                                parties = parties,
                                villages = villageSuggestions,
                                phoneLabel = if (isEnglish) "Beneficiary mobile number *" else "लाभार्थी का मोबाइल नंबर *",
                                nameLabel = if (isEnglish) "Beneficiary name *" else "लाभार्थी का नाम *",
                                villageLabel = if (isEnglish) "Beneficiary village *" else "लाभार्थी का गाँव *",
                                isEnglish = isEnglish,
                            )
                            Text(
                                if (isEnglish) "A new beneficiary is saved. The physical recipient is saved only when they are also the beneficiary." else "नया लाभार्थी सेव होगा। पैसा लेने वाला तभी सेव होगा जब वही लाभार्थी हो।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                if (isEnglish) "Without another beneficiary, the recipient is the account holder for this advance." else "दूसरा लाभार्थी न चुनने पर पैसा लेने वाला ही इस पेशगी का खाता होगा।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        WholeRupeeField(
                            value = givenAmount,
                            onValueChange = { givenAmount = acceptWholeRupeesInput(givenAmount, it) },
                            label = if (isEnglish) "Amount (₹) *" else "राशि (₹) *",
                            isEnglish = isEnglish,
                        )
                        TransactionDateField(
                            dateMillis = givenDate,
                            onPickDate = { showGivenDatePicker = true },
                            isEnglish = isEnglish,
                        )
                    }
                }
            }
        }
    }

    if (showReceivedDatePicker) {
        LoanDatePickerDialog(
            initialDate = receivedDate,
            isEnglish = isEnglish,
            onDismiss = { showReceivedDatePicker = false },
            onConfirm = { selected -> receivedDate = selected; showReceivedDatePicker = false },
        )
    }
    if (showGivenDatePicker) {
        LoanDatePickerDialog(
            initialDate = givenDate,
            isEnglish = isEnglish,
            onDismiss = { showGivenDatePicker = false },
            onConfirm = { selected -> givenDate = selected; showGivenDatePicker = false },
        )
    }
    if (showDiscardDialog) {
        DiscardDraftDialog(
            title = if (isEnglish) "Discard changes?" else "बदलाव रद्द करें?",
            message = if (isEnglish) "Exit without saving the entered loan details?" else "क्या बिना सेव किए भरी हुई जानकारी छोड़नी है?",
            confirmText = if (isEnglish) "Discard" else "छोड़ें",
            dismissText = if (isEnglish) "Continue editing" else "भरना जारी रखें",
            onConfirmDiscard = { showDiscardDialog = false; onBack() },
            onDismiss = { showDiscardDialog = false },
        )
    }
}

@Composable
private fun ContactFields(
    contact: LoanContactDraft,
    parties: List<Party>,
    villages: List<String>,
    phoneLabel: String,
    nameLabel: String,
    villageLabel: String,
    isEnglish: Boolean,
) {
    MandiPartyLookupField(
        phoneValue = contact.phone,
        onPhoneChange = { contact.updatePhone(it, parties) },
        onPartySelected = contact::select,
        parties = parties,
        label = phoneLabel,
        placeholder = if (isEnglish) "Enter mobile number first" else "पहले मोबाइल नंबर दर्ज करें",
        isError = contact.phone.isNotBlank() && !MandiValidators.validatePhone(contact.phone, isRequired = true).isValid,
        errorMessage = if (contact.phone.isNotBlank() && !MandiValidators.validatePhone(contact.phone, isRequired = true).isValid) {
            if (isEnglish) "Enter a valid 10-digit Indian mobile number" else "10 अंकों का सही मोबाइल नंबर दर्ज करें"
        } else null,
    )
    Text(
        if (isEnglish) "A matching saved mobile number fills the name and village." else "सेव मोबाइल नंबर मिलने पर नाम और गाँव अपने आप भरेंगे।",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    MandiFlatTextField(
        value = contact.name,
        onValueChange = { contact.name = it },
        label = nameLabel,
        placeholder = if (isEnglish) "Enter name" else "नाम लिखें",
        modifier = Modifier.fillMaxWidth(),
        isTall = true,
    )
    VillageLookupField(
        value = contact.village,
        onValueChange = { contact.village = it },
        suggestions = villages,
        label = villageLabel,
        isEnglish = isEnglish,
    )
}

@Composable
private fun WholeRupeeField(value: String, onValueChange: (String) -> Unit, label: String, isEnglish: Boolean) {
    MandiFlatTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = if (isEnglish) "Whole rupees" else "पूरे रुपये",
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        isTall = true,
    )
}

@Composable
private fun TransactionDateField(dateMillis: Long, onPickDate: () -> Unit, isEnglish: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if (isEnglish) "Transaction date *" else "लेन-देन की तारीख *", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = dateLabel(dateMillis),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                TextButton(onClick = onPickDate) { Text(if (isEnglish) "Choose" else "तारीख चुनें") }
            },
        )
        Text(
            if (isEnglish) "Today is selected by default. Earlier dates are allowed; future dates are disabled." else "आज की तारीख पहले से चुनी है। पुरानी तारीख चुन सकते हैं, भविष्य की नहीं।",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanDatePickerDialog(
    initialDate: Long,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val today = todayMillis()
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= today
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { dateState.selectedDateMillis?.let(onConfirm) }) {
                Text(if (isEnglish) "Select date" else "तारीख चुनें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isEnglish) "Cancel" else "रद्द करें") }
        },
    ) {
        DatePicker(state = dateState)
    }
}

@Composable
private fun VillageLookupField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    label: String,
    isEnglish: Boolean,
) {
    val matches = remember(value, suggestions) {
        if (value.isBlank()) emptyList() else suggestions.filter { it.startsWith(value.trim(), ignoreCase = true) && !it.equals(value.trim(), ignoreCase = true) }.take(5)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MandiFlatTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = if (isEnglish) "Type village or choose a match" else "गाँव लिखें या सुझाव चुनें",
            modifier = Modifier.fillMaxWidth(),
            isTall = true,
        )
        matches.forEach { village ->
            TextButton(onClick = { onValueChange(village) }, modifier = Modifier.fillMaxWidth()) {
                Text(village, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun wholeRupeePaisa(value: String): Long? {
    val rupees = value.toLongOrNull()?.takeIf { it > 0L && it <= Long.MAX_VALUE / 100L } ?: return null
    return rupees * 100L
}

private fun acceptWholeRupeesInput(current: String, proposed: String): String =
    if (proposed.length <= 12 && proposed.all { it in '0'..'9' }) proposed else current

private fun findParty(parties: List<Party>, draft: LoanContactDraft): Party? =
    parties.firstOrNull { !it.isDeleted && (it.id == draft.selectedPartyId || it.phone == draft.phone) }

private fun partyFor(draft: LoanContactDraft, shopId: String, existing: Party?, now: Long): Party? {
    val normalizedVillage = MandiValidators.sanitizeVillage(draft.village).ifBlank { null }
    val name = draft.name.trim()
    if (existing == null) {
        return Party(
            id = IdGenerator.newId(),
            shopId = shopId,
            name = name,
            phone = draft.phone,
            village = normalizedVillage,
            partyType = PartyType.FARMER,
            createdAt = now,
            updatedAt = now,
        )
    }
    if (existing.name == name && existing.phone == draft.phone && existing.village == normalizedVillage) return null
    return existing.copy(
        name = name,
        phone = draft.phone,
        village = normalizedVillage,
        updatedAt = now,
        syncStatus = 0,
    )
}

private fun todayMillis(): Long {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return today.toEpochDays().toLong() * MILLIS_PER_DAY
}

private fun dateLabel(millis: Long): String {
    val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
    return "${date.dayOfMonth.toString().padStart(2, '0')}/${date.monthNumber.toString().padStart(2, '0')}/${date.year}"
}

private fun String.extractRecipientVillage(): String? {
    val village = when {
        contains("Village:") -> substringAfter("Village:")
        contains("गाँव:") -> substringAfter("गाँव:")
        else -> return null
    }.substringBefore('\n').trim()
    return village.takeIf(String::isNotEmpty)
}
