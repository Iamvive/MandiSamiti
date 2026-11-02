package com.appwork.mandisamiti.ui.buy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.LendStatus
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TradeSettings
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.model.receiptDateTime
import com.appwork.mandisamiti.domain.model.slipLabel
import com.appwork.mandisamiti.domain.repository.BuyRepository
import com.appwork.mandisamiti.domain.repository.CashTransactionRepository
import com.appwork.mandisamiti.domain.repository.LendRepository
import com.appwork.mandisamiti.domain.repository.PartyRepository
import com.appwork.mandisamiti.domain.validation.MandiValidators
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.ui.components.DiscardDraftDialog
import com.appwork.mandisamiti.ui.components.DuplicateEntryDialog
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

enum class BuyStep {
    FORM,
    PAY,
    BILL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyEntryScreen(
    shopId: String = "",
    existingId: String? = null,
    buyRepository: BuyRepository? = null,
    partyRepository: PartyRepository? = null,
    cashRepository: CashTransactionRepository? = null,
    lendRepository: LendRepository? = null,
    tradeSettings: TradeSettings? = null,
    formState: BuyEntryFormState = remember(tradeSettings) { BuyEntryFormState(tradeSettings) },
    hasActiveLoan: Boolean = false,
    activeLoanChecker: ((String) -> Boolean)? = null,
    parties: List<Party> = emptyList(),
    isEnglish: Boolean = false,
    onSaved: ((BuyEntry) -> Unit)? = null,
    onBack: () -> Unit,
    onShareWhatsApp: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var buyStep by remember { mutableStateOf(BuyStep.FORM) }
    var existingEntry by remember { mutableStateOf<BuyEntry?>(null) }
    var savedEntry by remember { mutableStateOf<BuyEntry?>(null) }
    var showRateOutlierConfirmation by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var pendingEntryToSave by remember { mutableStateOf<BuyEntry?>(null) }

    // Payment state for step PAY
    var payMode by remember { mutableStateOf("full") } // "full" or "part"
    var payAmountText by remember { mutableStateOf("") }
    var isRevisedBill by remember { mutableStateOf(false) }

    // Live Streams
    val recentBuys by (buyRepository?.getBuyEntriesStream(shopId) ?: emptyFlow()).collectAsState(initial = emptyList())
    val lends by (lendRepository?.getLendEntriesStream(shopId) ?: emptyFlow()).collectAsState(initial = emptyList())
    val cashTransactions by (cashRepository?.getTransactionsByShopStream(shopId) ?: emptyFlow()).collectAsState(initial = emptyList())

    // Rokdi Cash In Hand calculation
    val rokdiBalancePaisa = remember(cashTransactions) {
        cashTransactions.filter { !it.isVoid && it.paymentMode == PaymentMode.CASH }.sumOf {
            if (it.transactionType == TransactionType.JAMA_RECEIVED || it.transactionType == TransactionType.INTEREST_RECEIVED) {
                it.amountPaisa
            } else {
                -it.amountPaisa
            }
        }
    }

    // Party loans
    val activePartyKey = formState.selectedFarmerId ?: formState.farmerName.trim()
    val partyOpenLends = remember(lends, activePartyKey) {
        if (activePartyKey.isBlank()) emptyList()
        else lends.filter {
            !it.isVoid && it.status != LendStatus.SETTLED &&
                (it.partyId == activePartyKey || it.partyId.equals(formState.farmerName, ignoreCase = true))
        }
    }
    val totalOpenPrincipalPaisa = remember(partyOpenLends) {
        partyOpenLends.sumOf { it.amountPaisa }
    }
    val totalOpenInterestPaisa = remember(partyOpenLends) {
        // Approximate monthly interest: 2% default or recorded
        partyOpenLends.sumOf { 200_00L }
    }

    val isDirty = formState.hasUnsavedChanges(existingEntry)

    MandiBackHandler(enabled = true) {
        when (buyStep) {
            BuyStep.FORM -> {
                if (isDirty) showDiscardDialog = true else onBack()
            }
            BuyStep.PAY -> {
                buyStep = BuyStep.FORM
            }
            BuyStep.BILL -> {
                onBack()
            }
        }
    }

    LaunchedEffect(existingId) {
        if (existingId != null && buyRepository != null) {
            val entry = buyRepository.getBuyEntryById(existingId)
            if (entry != null) {
                existingEntry = entry
                formState.populateFrom(entry)
            }
        }
    }

    LaunchedEffect(existingEntry, parties) {
        val entry = existingEntry ?: return@LaunchedEffect
        val party = parties.firstOrNull { it.id == entry.farmerId } ?: return@LaunchedEffect
        if (formState.farmerName == entry.farmerId) formState.farmerName = party.name
        if (formState.phone.isBlank()) party.phone?.let(formState::updatePhone)
        if (formState.village.isBlank()) formState.village = party.village.orEmpty()
    }

    val preview = formState.aggregatePreview()
    val canSave = formState.canSave()
    val validationReason = formState.validationReason()

    when (buyStep) {
        BuyStep.FORM -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                if (isEnglish) {
                                    if (existingId == null) "New Purchase" else "Edit Purchase"
                                } else {
                                    if (existingId == null) "नई खरीद एंट्री" else "खरीद एंट्री बदलें"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    if (isDirty) showDiscardDialog = true else onBack()
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (isEnglish) "Back" else "पीछे")
                            }
                        }
                    )
                },
                bottomBar = {
                    // Fixed Bottom Dock (Direction B Ergonomics)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 8.dp,
                        tonalElevation = 2.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEnglish) "Final Payable" else "अंतिम देय राशि",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MandiTextSecondary
                                )
                                Text(
                                    text = MandiMathEngine.formatRupees(preview.netPayablePaisa),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Button(
                                onClick = {
                                    val entry = formState.toBuyEntry(
                                        shopId = shopId,
                                        existingId = existingId,
                                        existingEntry = existingEntry
                                    )
                                    val now = Clock.System.now().toEpochMilliseconds()
                                    val isDuplicate = existingId == null && recentBuys.any { prev ->
                                        !prev.isVoid &&
                                            prev.farmerId.trim().equals(entry.farmerId.trim(), ignoreCase = true) &&
                                            prev.netPayablePaisa == entry.netPayablePaisa &&
                                            prev.bagsCount == entry.bagsCount &&
                                            prev.commodityId.trim().equals(entry.commodityId.trim(), ignoreCase = true) &&
                                            kotlin.math.abs(now - prev.createdAt) <= 2 * 60 * 1000L
                                    }

                                    if (isDuplicate) {
                                        pendingEntryToSave = entry
                                        showDuplicateDialog = true
                                    } else if (formState.hasSanityWarning()) {
                                        pendingEntryToSave = entry
                                        showRateOutlierConfirmation = true
                                    } else {
                                        coroutineScope.launch {
                                            executeSave(
                                                entry = entry,
                                                shopId = shopId,
                                                buyRepository = buyRepository,
                                                partyRepository = partyRepository,
                                                formState = formState,
                                                onSavedCallback = onSaved,
                                                onProceedToPay = { saved ->
                                                    savedEntry = saved
                                                    payAmountText = (saved.netPayablePaisa / 100L).toString()
                                                    payMode = "full"
                                                    buyStep = BuyStep.PAY
                                                }
                                            )
                                        }
                                    }
                                },
                                enabled = canSave,
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .height(50.dp)
                            ) {
                                val buttonLabel = when {
                                    formState.isOldPartyWithoutPhone -> if (isEnglish) "Add number to save" else "नंबर डालने पर सुरक्षित होगा"
                                    formState.phone.length != 10 -> if (isEnglish) "Enter 10-digit phone" else "10 अंकों का नंबर डालें"
                                    formState.farmerName.isBlank() || formState.village.isBlank() -> if (isEnglish) "Add name & village" else "नाम और गांव डालने पर सुरक्षित होगा"
                                    else -> if (isEnglish) "Save" else "सुरक्षित करें"
                                }
                                Text(buttonLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Section 1: Farmer Lookup & Phone-First Entry
                    FarmerLookupSection(
                        formState = formState,
                        parties = parties,
                        isEnglish = isEnglish
                    )

                    // Section 2: Crop Lots (Bags, Weight/Bag, Extra on ONE line, Rate on next)
                    CropLotsSection(
                        formState = formState,
                        isEnglish = isEnglish
                    )

                    // Commission % & Labour (2-column grid)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = formState.commissionPercent,
                            onValueChange = { formState.commissionPercent = it },
                            label = { Text(if (isEnglish) "Commission %" else "कमीशन %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = formState.labourPerBag,
                            onValueChange = { formState.labourPerBag = it },
                            label = { Text(if (isEnglish) "Labour (₹/bag)" else "मजदूरी (रु/बोरी)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Section 3: हिसाब पर्ची (Live Commodity Calculations)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Slip Calculations" else "हिसाब पर्ची",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            preview.lotsCalculations.forEachIndexed { idx, pair ->
                                val lot = pair.first
                                val res = pair.second
                                if (res.grossPaisa > 0) {
                                    SummaryLine(
                                        label = if (isEnglish) "Lot #${idx + 1}: ${lot.cropName}, ${res.bags} bags" else "फसल #${idx + 1}: ${lot.cropName}, ${res.bags} बोरी",
                                        value = "₹${MandiMathEngine.paisaToRupeesString(res.grossPaisa)}"
                                    )
                                }
                            }
                            SummaryLine(
                                label = if (isEnglish) "Labour Deduction" else "मजदूरी कटौती",
                                value = "− ₹${MandiMathEngine.paisaToRupeesString(preview.labourPaisa)}"
                            )
                            SummaryLine(
                                label = if (isEnglish) "Commission Deduction" else "कमीशन कटौती",
                                value = "− ₹${MandiMathEngine.paisaToRupeesString(preview.commissionPaisa)}"
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            SummaryLine(
                                label = if (isEnglish) "Slip Value" else "पर्ची राशि",
                                value = "₹${MandiMathEngine.paisaToRupeesString(preview.slipNetPaisa)}",
                                isBold = true
                            )
                        }
                    }

                    // Section 4: पर्ची के बाद का हिसाब (Advance, Loan cut, Deposit)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "After the Slip" else "पर्ची के बाद का हिसाब",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = formState.advancePaid,
                                onValueChange = { formState.advancePaid = it },
                                label = { Text(if (isEnglish) "Advance Paid (₹)" else "अग्रिम / पेशगी (रु)") },
                                placeholder = { Text(if (isEnglish) "Optional" else "वैकल्पिक") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Open loan deduction card
                            if (partyOpenLends.isNotEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(if (isEnglish) "Open Loan" else "पुराना उधार", fontWeight = FontWeight.SemiBold)
                                            Text("₹${MandiMathEngine.paisaToRupeesString(totalOpenPrincipalPaisa + totalOpenInterestPaisa)}", fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            text = "मूल ₹${MandiMathEngine.paisaToRupeesString(totalOpenPrincipalPaisa)} + ब्याज ₹${MandiMathEngine.paisaToRupeesString(totalOpenInterestPaisa)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MandiTextSecondary
                                        )
                                        OutlinedTextField(
                                            value = formState.loanCutPaid,
                                            onValueChange = { formState.loanCutPaid = it },
                                            label = { Text(if (isEnglish) "Cut from loan (₹)" else "उधार से काटें (रु)") },
                                            placeholder = { Text(if (isEnglish) "Optional" else "वैकल्पिक") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = if (isEnglish) "Interest deducted first, then principal." else "पहले ब्याज, फिर मूल राशि में कटौती होगी।",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MandiTextSecondary
                                        )
                                    }
                                }
                            }

                            // Summary breakdown
                            SummaryLine(
                                label = if (isEnglish) "Slip Value" else "पर्ची राशि",
                                value = "₹${MandiMathEngine.paisaToRupeesString(preview.slipNetPaisa)}"
                            )
                            if (preview.advancePaidPaisa > 0) {
                                SummaryLine(
                                    label = if (isEnglish) "Advance" else "अग्रिम / पेशगी",
                                    value = "− ₹${MandiMathEngine.paisaToRupeesString(preview.advancePaidPaisa)}"
                                )
                            }
                            if (preview.loanCutPaisa > 0) {
                                SummaryLine(
                                    label = if (isEnglish) "Loan Cut" else "उधार कटौती",
                                    value = "− ₹${MandiMathEngine.paisaToRupeesString(preview.loanCutPaisa)}"
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            SummaryLine(
                                label = if (isEnglish) "Final Payable" else "अंतिम देय राशि",
                                value = MandiMathEngine.formatRupees(preview.netPayablePaisa),
                                isBold = true
                            )
                        }
                    }

                    if (!canSave && validationReason != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Text(validationReason, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(Modifier.height(100.dp))
                }
            }

            // Dialogs
            if (showDuplicateDialog && pendingEntryToSave != null) {
                val pending = pendingEntryToSave!!
                DuplicateEntryDialog(
                    message = if (isEnglish) "Duplicate entry detected. Still want to save?" else "यह एंट्री पहले से है, फिर भी सुरक्षित करें?",
                    details = "${pending.farmerId} • ₹${MandiMathEngine.formatIndianGrouping(pending.netPayablePaisa / 100)} • ${pending.bagsCount} बोरी",
                    onConfirm = {
                        showDuplicateDialog = false
                        coroutineScope.launch {
                            executeSave(
                                entry = pending,
                                shopId = shopId,
                                buyRepository = buyRepository,
                                partyRepository = partyRepository,
                                formState = formState,
                                onSavedCallback = onSaved,
                                onProceedToPay = { saved ->
                                    savedEntry = saved
                                    payAmountText = (saved.netPayablePaisa / 100L).toString()
                                    payMode = "full"
                                    buyStep = BuyStep.PAY
                                }
                            )
                            pendingEntryToSave = null
                        }
                    },
                    onDismiss = {
                        showDuplicateDialog = false
                        pendingEntryToSave = null
                    }
                )
            }

            if (showRateOutlierConfirmation && pendingEntryToSave != null) {
                AlertDialog(
                    onDismissRequest = {
                        showRateOutlierConfirmation = false
                        pendingEntryToSave = null
                    },
                    icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MandiAmberDark, modifier = Modifier.size(32.dp)) },
                    title = { Text(if (isEnglish) "Unusual Value Confirmation" else "असामान्य प्रविष्टि पुष्टि", fontWeight = FontWeight.Bold) },
                    text = { Text(formState.sanityWarningMessage() ?: "प्रविष्टि में असामान्य मान दर्ज हैं।") },
                    confirmButton = {
                        Button(
                            onClick = {
                                val entry = pendingEntryToSave!!
                                showRateOutlierConfirmation = false
                                pendingEntryToSave = null
                                coroutineScope.launch {
                                    executeSave(
                                        entry = entry,
                                        shopId = shopId,
                                        buyRepository = buyRepository,
                                        partyRepository = partyRepository,
                                        formState = formState,
                                        onSavedCallback = onSaved,
                                        onProceedToPay = { saved ->
                                            savedEntry = saved
                                            payAmountText = (saved.netPayablePaisa / 100L).toString()
                                            payMode = "full"
                                            buyStep = BuyStep.PAY
                                        }
                                    )
                                }
                            }
                        ) {
                            Text(if (isEnglish) "Yes, Correct" else "हाँ, विवरण सही है")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showRateOutlierConfirmation = false
                                pendingEntryToSave = null
                            }
                        ) {
                            Text(if (isEnglish) "Edit" else "सुधारें")
                        }
                    }
                )
            }

            if (showDiscardDialog) {
                DiscardDraftDialog(
                    title = if (isEnglish) "Discard Changes?" else "बदलाव रद्द करें?",
                    message = if (isEnglish) "Are you sure you want to exit without saving?" else "क्या आप बिना सुरक्षित किए बाहर जाना चाहते हैं?",
                    confirmText = if (isEnglish) "Yes, Exit" else "हाँ, बाहर निकलें",
                    dismissText = if (isEnglish) "Continue Editing" else "भरना जारी रखें",
                    onConfirmDiscard = {
                        showDiscardDialog = false
                        onBack()
                    },
                    onDismiss = { showDiscardDialog = false }
                )
            }
        }

        BuyStep.PAY -> {
            // STEP 2: किसान को भुगतान (Direction B final Payment Screen)
            val entry = savedEntry ?: return
            val finalPayablePaisa = entry.netPayablePaisa
            val payAmtEntered = (payAmountText.toLongOrNull() ?: 0L) * 100L
            val payAmtPaisa = if (payMode == "full") finalPayablePaisa else payAmtEntered.coerceIn(0L, finalPayablePaisa)
            val leftPaisa = (finalPayablePaisa - payAmtPaisa).coerceAtLeast(0L)
            val isNegativeRokdi = payAmtPaisa > rokdiBalancePaisa

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(if (isEnglish) "Pay the Farmer" else "किसान को भुगतान", fontWeight = FontWeight.Bold)
                                Text(
                                    text = "पर्ची #${slipLabel(entry.slipNo, entry.id)} • ${formState.farmerName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MandiTextSecondary
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { buyStep = BuyStep.FORM }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Success banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                text = "पर्ची #${slipLabel(entry.slipNo, entry.id)} सुरक्षित हुई",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Hero Card: Final Payable
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (isEnglish) "Final Payable" else "अंतिम देय राशि", style = MaterialTheme.typography.bodySmall, color = MandiTextSecondary)
                            Text(MandiMathEngine.formatRupees(finalPayablePaisa), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Payment choice card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = payMode == "part",
                                    onClick = {
                                        payMode = "part"
                                        if (payAmountText.isBlank() || payAmountText == (finalPayablePaisa / 100L).toString()) {
                                            payAmountText = "0"
                                        }
                                    },
                                    label = { Text(if (isEnglish) "Partial" else "आंशिक") }
                                )
                                FilterChip(
                                    selected = payMode == "full",
                                    onClick = {
                                        payMode = "full"
                                        payAmountText = (finalPayablePaisa / 100L).toString()
                                    },
                                    label = { Text(if (isEnglish) "Full" else "पूरा") }
                                )
                            }

                            OutlinedTextField(
                                value = if (payMode == "full") (finalPayablePaisa / 100L).toString() else payAmountText,
                                onValueChange = {
                                    payAmountText = it.filter { ch -> ch.isDigit() }
                                },
                                enabled = payMode == "part",
                                label = { Text(if (isEnglish) "Pay Cash Now (₹)" else "अभी नकद दें (रु)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "पर्ची बाकी: ₹${MandiMathEngine.paisaToRupeesString(leftPaisa)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "गल्ले से ₹${MandiMathEngine.paisaToRupeesString(payAmtPaisa)} नकद निकलेगा",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MandiTextSecondary
                                )
                            }

                            // Warning when payment exceeds Rokdi cash balance (Allowed per Vivek's decision!)
                            if (isNegativeRokdi) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                        Text(
                                            text = "गल्ले में ₹${MandiMathEngine.paisaToRupeesString(rokdiBalancePaisa)} शेष है। भुगतान के बाद गल्ला ऋणात्मक (-₹${MandiMathEngine.paisaToRupeesString(payAmtPaisa - rokdiBalancePaisa)}) हो जाएगा।",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Buttons
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                // Record cash payout in Galla
                                if (cashRepository != null && payAmtPaisa > 0L) {
                                    val now = Clock.System.now().toEpochMilliseconds()
                                    cashRepository.recordTransaction(
                                        CashTransaction(
                                            id = IdGenerator.newId(),
                                            shopId = shopId,
                                            partyId = entry.farmerId,
                                            dealId = entry.id,
                                            transactionType = TransactionType.UDHAR_GIVEN,
                                            amountPaisa = payAmtPaisa,
                                            paymentMode = PaymentMode.CASH,
                                            transactionDate = now,
                                            remarks = "खरीद पर्ची #${slipLabel(entry.slipNo, entry.id)} भुगतान",
                                            createdAt = now,
                                            updatedAt = now
                                        )
                                    )
                                }
                                // Update paidPaisa on BuyEntry
                                if (buyRepository != null && payAmtPaisa > 0L) {
                                    val updatedEntry = entry.copy(
                                        paidPaisa = payAmtPaisa,
                                        updatedAt = Clock.System.now().toEpochMilliseconds()
                                    )
                                    buyRepository.editBuyEntry(updatedEntry)
                                    savedEntry = updatedEntry
                                }
                                isRevisedBill = payAmtPaisa > 0L
                                buyStep = BuyStep.BILL
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(if (isEnglish) "Save Payment" else "भुगतान सुरक्षित करें", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            isRevisedBill = false
                            buyStep = BuyStep.BILL
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(if (isEnglish) "Not Now" else "अभी नहीं", fontSize = 14.sp)
                    }
                }
            }
        }

        BuyStep.BILL -> {
            // STEP 3: खरीद पर्ची Bill View with WhatsApp Share and New Buy
            val entry = savedEntry ?: return
            val displayName = formState.farmerName.ifBlank { "किसान" }
            val now = Clock.System.now().toEpochMilliseconds()

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(if (isEnglish) "Purchase Slip #${slipLabel(entry.slipNo, entry.id)}" else "खरीद पर्ची #${slipLabel(entry.slipNo, entry.id)}", fontWeight = FontWeight.Bold)
                                Text(displayName, style = MaterialTheme.typography.bodySmall, color = MandiTextSecondary)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isRevisedBill) "संशोधित • ${receiptDateTime(now)}" else "मूल पर्ची • ${receiptDateTime(now)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            preview.lotsCalculations.forEachIndexed { idx, pair ->
                                val lot = pair.first
                                val res = pair.second
                                SummaryLine(
                                    label = "फसल #${idx + 1}: ${lot.cropName}, ${res.bags} बोरी",
                                    value = "₹${MandiMathEngine.paisaToRupeesString(res.grossPaisa)}"
                                )
                            }
                            SummaryLine(label = "मजदूरी कटौती", value = "− ₹${MandiMathEngine.paisaToRupeesString(preview.labourPaisa)}")
                            SummaryLine(label = "कमीशन कटौती", value = "− ₹${MandiMathEngine.paisaToRupeesString(preview.commissionPaisa)}")
                            SummaryLine(label = "पर्ची राशि", value = "₹${MandiMathEngine.paisaToRupeesString(preview.slipNetPaisa)}", isBold = true)
                            if (preview.advancePaidPaisa > 0) {
                                SummaryLine(label = "अग्रिम / पेशगी", value = "− ₹${MandiMathEngine.paisaToRupeesString(preview.advancePaidPaisa)}")
                            }
                            if (preview.loanCutPaisa > 0) {
                                SummaryLine(label = "उधार कटौती", value = "− ₹${MandiMathEngine.paisaToRupeesString(preview.loanCutPaisa)}")
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            SummaryLine(label = "अंतिम देय राशि", value = MandiMathEngine.formatRupees(entry.netPayablePaisa), isBold = true)
                            if (entry.paidPaisa > 0) {
                                SummaryLine(label = "नकद भुगतान", value = "− ₹${MandiMathEngine.paisaToRupeesString(entry.paidPaisa)}")
                                val left = (entry.netPayablePaisa - entry.paidPaisa).coerceAtLeast(0L)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                SummaryLine(label = "बाकी", value = MandiMathEngine.formatRupees(left), isBold = true)
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {
                            val slipText = buildFarmerSlipText(entry, displayName, preview.lotsCalculations.map { it.second })
                            onShareWhatsApp?.invoke(slipText)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(if (isEnglish) "Share Slip" else "पर्ची भेजें", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            formState.farmerName = ""
                            formState.phone = ""
                            formState.village = ""
                            formState.selectedFarmerId = null
                            formState.advancePaid = ""
                            formState.loanCutPaid = ""
                            formState.depositUsed = ""
                            formState.isOldPartyWithoutPhone = false
                            savedEntry = null
                            buyStep = BuyStep.FORM
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(if (isEnglish) "New Purchase" else "नई खरीद", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

private suspend fun executeSave(
    entry: BuyEntry,
    shopId: String,
    buyRepository: BuyRepository?,
    partyRepository: PartyRepository?,
    formState: BuyEntryFormState,
    onSavedCallback: ((BuyEntry) -> Unit)?,
    onProceedToPay: (BuyEntry) -> Unit
) {
    // 1. Provision or update party
    if (partyRepository != null && formState.phone.isNotBlank()) {
        val existingParty = formState.selectedFarmerId?.let { partyRepository.getPartyById(it) }
            ?: partyRepository.getPartyByPhone(shopId, formState.phone)
        val now = Clock.System.now().toEpochMilliseconds()
        if (existingParty != null) {
            val updated = existingParty.copy(
                name = formState.farmerName.ifBlank { existingParty.name },
                phone = formState.phone,
                village = formState.village.ifBlank { existingParty.village },
                updatedAt = now
            )
            partyRepository.saveParty(updated)
        } else {
            val newParty = Party(
                id = formState.selectedFarmerId ?: IdGenerator.newId(),
                shopId = shopId,
                name = formState.farmerName,
                phone = formState.phone,
                village = formState.village.ifBlank { null },
                partyType = PartyType.FARMER,
                createdAt = now,
                updatedAt = now
            )
            partyRepository.saveParty(newParty)
        }
    }

    // 2. Save Buy Entry
    if (buyRepository != null) {
        buyRepository.saveBuyEntry(entry)
    }
    onSavedCallback?.invoke(entry)
    onProceedToPay(entry)
}

@Composable
private fun FarmerLookupSection(
    formState: BuyEntryFormState,
    parties: List<Party>,
    isEnglish: Boolean
) {
    var searchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(formState.phone.ifBlank { formState.farmerName }) }

    val matches = remember(parties, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) emptyList()
        else parties.filter {
            it.name.contains(q, ignoreCase = true) ||
                (it.village?.contains(q, ignoreCase = true) == true) ||
                (it.phone?.contains(q) == true)
        }.take(5)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isEnglish) "Farmer Details" else "किसान विवरण",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Primary Phone / Name Search Field
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { input ->
                        searchQuery = input
                        searchExpanded = true
                        if (input.all { it.isDigit() }) {
                            formState.updatePhone(input)
                            val partyWithPhone = parties.firstOrNull { it.phone == input }
                            if (partyWithPhone != null) {
                                formState.farmerName = partyWithPhone.name
                                formState.village = partyWithPhone.village ?: ""
                                formState.selectedFarmerId = partyWithPhone.id
                                formState.isOldPartyWithoutPhone = false
                            } else if (input.length == 10) {
                                formState.selectedFarmerId = null
                                formState.isOldPartyWithoutPhone = false
                            }
                        } else {
                            formState.farmerName = input
                            formState.selectedFarmerId = null
                        }
                    },
                    label = { Text(if (isEnglish) "Mobile Number or Name *" else "मोबाइल नंबर या नाम *") },
                    leadingIcon = {
                        Icon(
                            if (searchQuery.any { it.isLetter() }) Icons.Default.Person else Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                if (searchExpanded && matches.isNotEmpty() && !matches.any { it.phone == searchQuery && matches.size == 1 }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            matches.forEachIndexed { index, party ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            searchExpanded = false
                                            formState.farmerName = party.name
                                            formState.village = party.village ?: ""
                                            formState.selectedFarmerId = party.id
                                            val partyPhone = party.phone
                                            if (!partyPhone.isNullOrBlank()) {
                                                formState.updatePhone(partyPhone)
                                                searchQuery = partyPhone
                                                formState.isOldPartyWithoutPhone = false
                                            } else {
                                                formState.phone = ""
                                                searchQuery = party.name
                                                formState.isOldPartyWithoutPhone = true
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(party.name, fontWeight = FontWeight.SemiBold, color = MandiTextPrimary)
                                        val subDetails = buildList {
                                            if (!party.village.isNullOrBlank()) add("गाँव: ${party.village}")
                                            if (!party.phone.isNullOrBlank()) add("फ़ोन: ${party.phone}")
                                            else add("फ़ोन नहीं है")
                                        }.joinToString("  •  ")
                                        Text(subDetails, style = MaterialTheme.typography.bodySmall, color = MandiTextSecondary)
                                    }
                                    Icon(Icons.Default.Check, contentDescription = "चुनें", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                                if (index < matches.size - 1) {
                                    HorizontalDivider(thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }

            // Old party missing phone warning
            if (formState.isOldPartyWithoutPhone) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Text(
                            text = if (isEnglish) "This account has no phone number. Add 10-digit number before saving." else "इस खाते में नंबर नहीं है। सेव से पहले 10 अंकों का नंबर डालें।",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Farmer Name Field
            OutlinedTextField(
                value = formState.farmerName,
                onValueChange = { formState.farmerName = it },
                label = { Text(if (isEnglish) "Farmer Name *" else "किसान का नाम *") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            // Village Field
            OutlinedTextField(
                value = formState.village,
                onValueChange = { formState.village = it },
                label = { Text(if (isEnglish) "Village *" else "गांव *") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            // Helper text
            val helperText = when {
                formState.isOldPartyWithoutPhone -> ""
                formState.selectedFarmerId != null -> if (isEnglish) "Filled from saved number. Changing updates this account." else "सेव नंबर से नाम और गांव भरे। बदलने पर यह खाते में हर जगह बदलेगा।"
                formState.phone.length == 10 -> if (isEnglish) "New number. Account created upon save." else "नया नंबर। एंट्री सेव होते ही खाता बन जाएगा।"
                else -> ""
            }
            if (helperText.isNotBlank()) {
                Text(helperText, style = MaterialTheme.typography.bodySmall, color = MandiTextSecondary)
            }
        }
    }
}

@Composable
private fun CropLotsSection(
    formState: BuyEntryFormState,
    isEnglish: Boolean
) {
    val commonCrops = if (isEnglish) {
        listOf("Wheat", "Wheat Sharbati", "Gram", "Mustard")
    } else {
        listOf("गेहूं", "गेहूं शरबती", "चना", "सरसों")
    }

    formState.lots.forEachIndexed { index, lot ->
        val lotResult = lot.calculateLot()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Lot #${index + 1}: ${lot.cropName}" else "फसल #${index + 1}: ${lot.cropName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (formState.lots.size > 1) {
                        IconButton(onClick = { formState.removeLot(index) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Quick Crop Selection Chips
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    commonCrops.forEach { crop ->
                        FilterChip(
                            selected = lot.cropName == crop,
                            onClick = { lot.cropName = crop },
                            label = { Text(crop, fontSize = 12.sp) }
                        )
                    }
                }

                // Single-Row Inputs: Bags (max 4 digits) | Kg/Bag (max 3 digits) | Extra (kg)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = lot.bags,
                        onValueChange = { lot.bags = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text(if (isEnglish) "Bags *" else "बोरी संख्या *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lot.weightPerBagKg,
                        onValueChange = { lot.weightPerBagKg = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text(if (isEnglish) "Kg/Bag *" else "वजन/बोरी (किग्रा) *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1.1f)
                    )
                    OutlinedTextField(
                        value = lot.extraWeightKg,
                        onValueChange = { lot.extraWeightKg = it },
                        label = { Text(if (isEnglish) "Extra (kg)" else "अतिरिक्त (किग्रा)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Rate row (max 100,000)
                OutlinedTextField(
                    value = lot.ratePerQuintal,
                    onValueChange = {
                        val clean = it.filter { ch -> ch.isDigit() }
                        val num = clean.toLongOrNull() ?: 0L
                        if (num <= 100_000L) lot.ratePerQuintal = clean
                    },
                    label = { Text(if (isEnglish) "Rate (₹/qtl) *" else "भाव (रु/क्विंटल) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                // Subtotal summary strip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "वजन: ${MandiMathEngine.gramsToQuintals(lotResult.totalWeightGrams)} क्विंटल",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "सकल: ₹${MandiMathEngine.paisaToRupeesString(lotResult.grossPaisa)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    OutlinedButton(
        onClick = { formState.addLot() },
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text(if (isEnglish) "(+) Add another crop lot" else "(+) अन्य फसल / वैरायटी जोड़ें")
    }
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) MandiTextPrimary else MandiTextSecondary
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isBold) MaterialTheme.colorScheme.primary else MandiTextPrimary
        )
    }
}
