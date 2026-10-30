package com.appwork.mandisamiti.ui.ledger

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.validation.MandiValidators
import kotlinx.datetime.Clock
import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.domain.model.slipLabel
import com.appwork.mandisamiti.ui.components.SoundboxTopBar
import com.appwork.mandisamiti.ui.components.PhoneNeededBadge
import com.appwork.mandisamiti.ui.theme.MandiAccent
import com.appwork.mandisamiti.ui.theme.MandiAmberPrimary
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiAmberLight
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiBtnDangerBg
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessBg
import com.appwork.mandisamiti.ui.theme.MandiGreenBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenLight
import com.appwork.mandisamiti.ui.theme.MandiGreenReceivable
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiGreenText
import com.appwork.mandisamiti.ui.theme.MandiNavy
import com.appwork.mandisamiti.ui.theme.MandiNeutralLight
import com.appwork.mandisamiti.ui.theme.MandiNeutralText
import com.appwork.mandisamiti.ui.theme.MandiRedBorder
import com.appwork.mandisamiti.ui.theme.MandiRedLight
import com.appwork.mandisamiti.ui.theme.MandiRedPayable
import com.appwork.mandisamiti.ui.theme.MandiRedText
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

@Composable
fun PartyLedgerScreen(
    viewModel: PartyLedgerViewModel,
    onNavigateBack: () -> Unit,
    onShareWhatsAppReceipt: (String) -> Unit,
    onEditDeal: (String) -> Unit = {},
    onOpenBuyReceipt: (String) -> Unit = {},
    onOpenSellReceipt: (String) -> Unit = {},
    isEnglish: Boolean = false,
) {
    val uiState by viewModel.uiState.collectAsState()
    val party = uiState.party

    var showCashEntryType by remember { mutableStateOf<TransactionType?>(null) }
    var cashAmountInput by remember { mutableStateOf("") }
    var cashRemarksInput by remember { mutableStateOf("") }

    var showApplyToLoanDialog by remember { mutableStateOf(false) }
    var pendingCashAmount by remember { mutableStateOf(0L) }
    var pendingCashRemarks by remember { mutableStateOf("") }
    var selectedLoanIdForRepay by remember { mutableStateOf<String?>(null) }
    var repayDialogLoanId by remember { mutableStateOf<String?>(null) }

    var voidTarget by remember { mutableStateOf<LedgerItem?>(null) }
    var lendEditTarget by remember { mutableStateOf<LedgerItem.LendItem?>(null) }
    var cashEditTarget by remember { mutableStateOf<LedgerItem.CashItem?>(null) }
    var cashEditAmount by remember { mutableStateOf("") }
    var cashEditRemarks by remember { mutableStateOf("") }

    // Leaving the apply-to-loan dialog without a choice must not lose the deposit: reopen the entry form as typed.
    val backToCashDialog = {
        showApplyToLoanDialog = false
        showCashEntryType = TransactionType.JAMA_RECEIVED
        cashAmountInput = pendingCashAmount.toString()
        cashRemarksInput = pendingCashRemarks
    }

    var interestRateInput by remember { mutableStateOf("1.5") }
    var interestDaysInput by remember { mutableStateOf("30") }

    var showEditPartyDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }
    var editVillageInput by remember { mutableStateOf("") }
    var editPhoneInput by remember { mutableStateOf("") }
    var editGstinInput by remember { mutableStateOf("") }
    var editMandiLicenseInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            SoundboxTopBar(
                shopName = party?.name ?: (if (isEnglish) "Account Details" else "खाता विवरण"),
                mandiLocation = if (party?.village != null) (if (isEnglish) "Village: ${party.village}" else "गांव: ${party.village}") else (if (isEnglish) "Mandi account" else "मंडी खाता"),
                isSoundEnabled = uiState.isSoundEnabled,
                onToggleSound = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEnglish) "Back" else "पीछे जाएं",
                            tint = MandiTextPrimary
                        )
                    }
                }
            )
        },
        containerColor = MandiBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (party?.phone == null) {
                PhoneNeededBadge(
                    isEnglish = isEnglish,
                    modifier = Modifier.padding(start = 14.dp, top = 10.dp),
                )
            }
            // Running Balance Hero Banner
            val isReceivable = uiState.balancePaisa > 0
            val isPayable = uiState.balancePaisa < 0
            val balanceAmount = if (uiState.balancePaisa < 0) -uiState.balancePaisa else uiState.balancePaisa

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isReceivable -> MandiRedLight
                        isPayable -> MandiGreenLight
                        else -> MandiSurface
                    }
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        when {
                            isReceivable -> MandiRedBorder
                            isPayable -> MandiGreenBorder
                            else -> MandiBorder
                        }
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isEnglish) "Net Balance" else "खाता शुद्ध शेष",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MandiTextSecondary
                        )
                        Text(
                            text = "₹${MandiMathEngine.paisaToRupeesString(balanceAmount)}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isReceivable -> MandiGreenReceivable
                                isPayable -> MandiRedPayable
                                else -> MandiTextPrimary
                            }
                        )
                    }

                    // Semantic Status Chip
                    val statusText = when {
                        isReceivable -> if (isEnglish) "To receive" else "लेना है"
                        isPayable -> if (isEnglish) "To pay" else "देना है"
                        else -> if (isEnglish) "Settled" else "हिसाब चुकता"
                    }
                    val statusColor = when {
                        isReceivable -> MandiGreenText
                        isPayable -> MandiRedText
                        else -> MandiNeutralText
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MandiSurface)
                            .border(1.dp, MandiBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            // Buyer Tax & Licence Details Card (hidden for farmers unless details exist)
            if (party?.partyType == PartyType.BUYER || party?.gstin != null || party?.mandiLicense != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MandiSurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MandiBorder)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEnglish) "Tax & Licence Details" else "कर एवं लाइसेंस विवरण",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MandiTextSecondary
                            )
                            Text(
                                text = if (isEnglish) "Edit" else "बदलें",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiPrimaryAction,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        editNameInput = party?.name ?: ""
                                        editVillageInput = party?.village ?: ""
                                        editPhoneInput = party?.phone ?: ""
                                        editGstinInput = party?.gstin ?: ""
                                        editMandiLicenseInput = party?.mandiLicense ?: ""
                                        showEditPartyDialog = true
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "GSTIN",
                                    fontSize = 11.sp,
                                    color = MandiTextMuted
                                )
                                Text(
                                    text = party?.gstin ?: (if (isEnglish) "Not provided" else "दर्ज नहीं"),
                                    fontSize = 13.sp,
                                    fontWeight = if (party?.gstin != null) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (party?.gstin != null) MandiTextPrimary else MandiTextMuted
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEnglish) "Mandi Licence" else "मंडी लाइसेंस",
                                    fontSize = 11.sp,
                                    color = MandiTextMuted
                                )
                                Text(
                                    text = party?.mandiLicense ?: (if (isEnglish) "Not provided" else "दर्ज नहीं"),
                                    fontSize = 13.sp,
                                    fontWeight = if (party?.mandiLicense != null) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (party?.mandiLicense != null) MandiTextPrimary else MandiTextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Quick Action Buttons (No emojis, vector icons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FintechActionButton(
                    label = if (isEnglish) "Received" else "जमा मिला",
                    icon = Icons.Default.ArrowDownward,
                    color = MandiGreenReceivable,
                    backgroundColor = MandiGreenLight,
                    borderColor = MandiGreenBorder,
                    textColor = MandiGreenText,
                    onClick = { showCashEntryType = TransactionType.JAMA_RECEIVED },
                    modifier = Modifier.weight(1f)
                )
                FintechActionButton(
                    label = if (isEnglish) "Paid" else "भुगतान दिया",
                    icon = Icons.Default.ArrowUpward,
                    color = MandiRedPayable,
                    backgroundColor = MandiRedLight,
                    borderColor = MandiRedBorder,
                    textColor = MandiRedText,
                    onClick = { showCashEntryType = TransactionType.UDHAR_GIVEN },
                    modifier = Modifier.weight(1f)
                )
                FintechActionButton(
                    label = if (isEnglish) "Interest" else "ब्याज हिसाब",
                    icon = Icons.Default.Calculate,
                    color = MandiTextPrimary,
                    backgroundColor = MandiSurfaceElevated,
                    borderColor = MandiBorder,
                    textColor = MandiTextPrimary,
                    onClick = {
                        val oldest = uiState.openLoans.minByOrNull { it.lend.entryDate }
                        if (oldest != null) {
                            val ms = Clock.System.now().toEpochMilliseconds() - oldest.lend.entryDate
                            interestDaysInput = (ms / 86_400_000L).coerceAtLeast(1L).toString()
                            interestRateInput = oldest.monthlyRatePercent.toString()
                        } else {
                            interestRateInput = (uiState.party?.monthlyInterestRate ?: uiState.defaultMonthlyRatePercent).toString()
                        }
                        viewModel.openInterestDialog()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transaction Stream Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (if (isEnglish) "Transactions" else "लेन-देन विवरण") + " (${uiState.ledgerItems.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                )
                Text(
                    text = if (isEnglish) "By date" else "तारीख अनुसार",
                    fontSize = 12.sp,
                    color = MandiTextMuted
                )
            }

            // Transaction List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.ledgerItems) { item ->
                    when (item) {
                        is LedgerItem.BuyItem -> {
                            FintechBuyCard(

                                isEnglish = isEnglish,
                                buy = item.buy,
                                onOpenReceipt = { onOpenBuyReceipt(item.buy.id) },
                                onLongPress = { voidTarget = item }
                            )
                        }
                        is LedgerItem.SellItem -> {
                            FintechSellCard(

                                isEnglish = isEnglish,
                                sell = item.sell,
                                onOpenReceipt = { onOpenSellReceipt(item.sell.id) },
                                onLongPress = { voidTarget = item }
                            )
                        }
                        is LedgerItem.LendItem -> {
                            FintechLendCard(

                                isEnglish = isEnglish,
                                lend = item.lend,
                                onLongPress = { voidTarget = item }
                            )
                        }
                        is LedgerItem.DealItem -> {
                            FintechDealCard(

                                isEnglish = isEnglish,
                                deal = item.deal,
                                isBuyerLedger = party?.partyType == PartyType.BUYER,
                                onShareSlip = { onShareWhatsAppReceipt(item.deal.id) },
                                onEditDeal = { onEditDeal(item.deal.id) },
                                onLongPress = { voidTarget = item }
                            )
                        }
                        is LedgerItem.CashItem -> {
                            FintechCashCard(

                                isEnglish = isEnglish,
                                transaction = item.transaction,
                                onLongPress = { voidTarget = item }
                            )
                        }
                    }
                }

                if (uiState.ledgerItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isEnglish) "No transactions in this account" else "इस खाते में कोई लेन-देन नहीं है",
                                color = MandiTextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditPartyDialog && party != null) {
        val isBuyer = party.partyType == PartyType.BUYER
        val gstinClean = editGstinInput.trim().uppercase()
        val gstinValid = !isBuyer || gstinClean.isEmpty() || MandiValidators.validateGstin(gstinClean, false).isValid
        val phoneValid = isValidPartyPhone(editPhoneInput)

        AlertDialog(
            onDismissRequest = { showEditPartyDialog = false },
            title = {
                Text(
                    text = if (isEnglish) "Edit Account Details" else "खाता विवरण बदलें",
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text(if (isEnglish) "Party Name *" else "नांव / नाम *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editVillageInput,
                        onValueChange = { editVillageInput = it },
                        label = { Text(if (isEnglish) "Village / Location" else "गांव / पता (वैकल्पिक)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhoneInput,
                        onValueChange = { editPhoneInput = sanitizePartyPhone(it) },
                        label = { Text(if (isEnglish) "Mobile Number *" else "मोबाइल नंबर *") },
                        isError = !phoneValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isBuyer) {
                        OutlinedTextField(
                            value = editGstinInput,
                            onValueChange = { if (it.length <= 15) editGstinInput = it.uppercase() },
                            label = { Text(if (isEnglish) "GSTIN (15 chars, Optional)" else "GSTIN (15 अक्षर, वैकल्पिक)") },
                            isError = !gstinValid,
                            supportingText = if (!gstinValid) {
                                { Text(if (isEnglish) "15-character GSTIN required" else "GSTIN 15 अक्षरों का होना चाहिए") }
                            } else null,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editMandiLicenseInput,
                            onValueChange = { if (it.length <= 50) editMandiLicenseInput = it },
                            label = { Text(if (isEnglish) "Mandi Licence No. (Optional)" else "मंडी लाइसेंस नंबर (वैकल्पिक)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNameInput.trim().isNotBlank() && phoneValid && gstinValid) {
                            viewModel.updatePartyDetails(
                                name = editNameInput,
                                village = editVillageInput,
                                phone = editPhoneInput,
                                gstin = if (isBuyer) gstinClean.ifBlank { null } else null,
                                mandiLicense = if (isBuyer) editMandiLicenseInput.trim().ifBlank { null } else null
                            )
                            showEditPartyDialog = false
                        }
                    },
                    enabled = editNameInput.trim().isNotBlank() && phoneValid && gstinValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MandiPrimaryAction,
                        contentColor = MandiPrimaryActionText
                    )
                ) {
                    Text(
                        text = if (isEnglish) "Save" else "सहेजें",
                        fontWeight = FontWeight.Bold,
                        color = MandiPrimaryActionText
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPartyDialog = false }) {
                    Text(if (isEnglish) "Cancel" else "रद्द करें", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    voidTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { voidTarget = null },
            title = { Text(if (isEnglish) "Cancel this entry?" else "प्रविष्टि रद्द करें?", fontWeight = FontWeight.Bold, color = MandiTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isEnglish) "The cancelled entry stays visible in the account but is removed from the totals." else "रद्द प्रविष्टि खाते में दिखेगी, पर हिसाब से हट जाएगी।", color = MandiTextSecondary, fontSize = 14.sp)
                    val remarksTag = (target as? LedgerItem.CashItem)?.transaction?.remarks.orEmpty()
                    val isLinkedCash = remarksTag.contains("[LEND:") || remarksTag.contains("[EXPENSE:") || remarksTag.contains("[SETTLE:")
                    if (target is LedgerItem.CashItem && !isLinkedCash) {
                        Button(
                            onClick = {
                                cashEditAmount = (target.transaction.amountPaisa / 100L).toString()
                                cashEditRemarks = target.transaction.remarks.orEmpty()
                                cashEditTarget = target
                                voidTarget = null
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) { Text(if (isEnglish) "Edit amount / note" else "रकम / नोट बदलें", fontSize = 16.sp) }
                    }
                    if (target is LedgerItem.LendItem) {
                        Button(
                            onClick = {
                                cashEditAmount = (target.lend.amountPaisa / 100L).toString()
                                cashEditRemarks = target.lend.notes.orEmpty()
                                lendEditTarget = target
                                voidTarget = null
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) { Text(if (isEnglish) "Edit amount / note" else "रकम / नोट बदलें", fontSize = 16.sp) }
                    }
                    val filteredReasons = if (target is LedgerItem.CashItem) {
                        VoidReason.entries.filter { it.appliesToCash }
                    } else {
                        VoidReason.entries.filter { it.appliesToDeal }
                    }
                    filteredReasons.forEach { reason ->
                        OutlinedButton(
                            onClick = { viewModel.voidEntry(target, reason); voidTarget = null },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) { Text(if (isEnglish) reason.labelEn else reason.labelHi, color = MandiTextPrimary, fontSize = 16.sp) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { voidTarget = null }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (isEnglish) "Back" else "वापस", color = MandiTextSecondary)
                }
            }
        )
    }

    lendEditTarget?.let { target ->
        val isMoney = target.lend.lendType == com.appwork.mandisamiti.domain.model.LendType.MONEY
        val newAmount = cashEditAmount.toLongOrNull() ?: 0L
        AlertDialog(
            onDismissRequest = { lendEditTarget = null },
            title = { Text(if (isEnglish) "Edit loan" else "उधार सुधारें", fontWeight = FontWeight.Bold, color = MandiTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isMoney) {
                        OutlinedTextField(
                            value = cashEditAmount,
                            onValueChange = { cashEditAmount = it.filter(Char::isDigit) },
                            label = { Text(if (isEnglish) "Amount (Rs)" else "रकम (Rs)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = cashEditRemarks,
                        onValueChange = { cashEditRemarks = it },
                        label = { Text(if (isEnglish) "Note" else "नोट") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isMoney || newAmount > 0,
                    onClick = {
                        viewModel.editLend(target, if (isMoney) newAmount * 100L else target.lend.amountPaisa, cashEditRemarks.trim().ifEmpty { null })
                        lendEditTarget = null
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(if (isEnglish) "Save" else "सेव करें") }
            },
            dismissButton = {
                TextButton(onClick = { lendEditTarget = null }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (isEnglish) "Back" else "वापस", color = MandiTextSecondary)
                }
            }
        )
    }

    cashEditTarget?.let { target ->
        val newAmount = cashEditAmount.toLongOrNull() ?: 0L
        AlertDialog(
            onDismissRequest = { cashEditTarget = null },
            title = { Text(if (isEnglish) "Edit entry" else "प्रविष्टि सुधारें", fontWeight = FontWeight.Bold, color = MandiTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cashEditAmount,
                        onValueChange = { cashEditAmount = it.filter(Char::isDigit) },
                        label = { Text(if (isEnglish) "Amount (Rs)" else "रकम (Rs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cashEditRemarks,
                        onValueChange = { cashEditRemarks = it },
                        label = { Text(if (isEnglish) "Note" else "नोट") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = newAmount > 0,
                    onClick = {
                        viewModel.editCash(target, newAmount * 100L, cashEditRemarks.trim().ifEmpty { null })
                        cashEditTarget = null
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(if (isEnglish) "Save" else "सेव करें") }
            },
            dismissButton = {
                TextButton(onClick = { cashEditTarget = null }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (isEnglish) "Back" else "वापस", color = MandiTextSecondary)
                }
            }
        )
    }

    // Cash Entry Dialog (Clean Fintech Modal)
    if (showCashEntryType != null) {
        val txnType = showCashEntryType!!
        val isDeposit = txnType == TransactionType.JAMA_RECEIVED
        AlertDialog(
            onDismissRequest = { showCashEntryType = null },
            title = {
                Text(
                    text = if (isDeposit) if (isEnglish) "Record cash received" else "नकद जमा प्राप्त दर्ज करें" else if (isEnglish) "Record cash paid" else "नकद भुगतान दिया दर्ज करें",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MandiTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = cashAmountInput,
                        onValueChange = { cashAmountInput = it },
                        label = { Text(if (isEnglish) "Amount (₹)" else "राशि (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cashRemarksInput,
                        onValueChange = { cashRemarksInput = it },
                        label = { Text(if (isEnglish) "Note (optional)" else "विवरण / नोट (वैकल्पिक)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = cashAmountInput.toLongOrNull() ?: 0L
                        if (amount > 0L) {
                            if (isDeposit && uiState.openLoans.isNotEmpty()) {
                                pendingCashAmount = amount
                                pendingCashRemarks = cashRemarksInput
                                selectedLoanIdForRepay = uiState.openLoans.first().lend.id
                                showApplyToLoanDialog = true
                                showCashEntryType = null
                                cashAmountInput = ""
                                cashRemarksInput = ""
                            } else {
                                viewModel.recordCashEntry(
                                    transactionType = txnType,
                                    amountRs = amount,
                                    paymentMode = PaymentMode.CASH,
                                    remarks = cashRemarksInput.ifBlank { null }
                                )
                                showCashEntryType = null
                                cashAmountInput = ""
                                cashRemarksInput = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDeposit) MandiBtnSuccessBg else MandiBtnDangerBg,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isEnglish) "Save" else "सुरक्षित करें", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCashEntryType = null }) {
                    Text(if (isEnglish) "Cancel" else "रद्द करें", color = MandiTextSecondary)
                }
            }
        )
    }

    if (showApplyToLoanDialog) {
        val totalOutstandingPaisa = uiState.openLoans.sumOf { it.outstandingPaisa }
        val totalOutstandingRs = totalOutstandingPaisa / 100L
        val selectedLoan = uiState.openLoans.find { it.lend.id == selectedLoanIdForRepay } ?: uiState.openLoans.firstOrNull()
        val selectedLoanOutstandingRs = (selectedLoan?.outstandingPaisa ?: 0L) / 100L
        val canApplyToLoan = selectedLoan != null && (pendingCashAmount * 100L <= selectedLoan.outstandingPaisa)

        AlertDialog(
            onDismissRequest = backToCashDialog,
            title = {
                Text(
                    text = if (isEnglish) "Deduct from loan?" else "उधार ऋण में काटें?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MandiTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isEnglish) "Total outstanding loan for this party is ₹${MandiMathEngine.formatIndianGrouping(totalOutstandingRs)}. Amount entered: ₹${MandiMathEngine.formatIndianGrouping(pendingCashAmount)}" else "इस खातेदार का कुल उधार बकाया ₹${MandiMathEngine.formatIndianGrouping(totalOutstandingRs)} है। दर्ज राशि: ₹${MandiMathEngine.formatIndianGrouping(pendingCashAmount)}",
                        fontSize = 14.sp,
                        color = MandiTextSecondary
                    )

                    if (uiState.openLoans.size > 1) {
                        Text(
                            text = if (isEnglish) "Choose the loan to deduct from:" else "ऋण चुनें जिसमें काटना है:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MandiTextPrimary
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            uiState.openLoans.forEach { loanInfo ->
                                val isSelected = loanInfo.lend.id == selectedLoanIdForRepay
                                val loanOutstandingRs = loanInfo.outstandingPaisa / 100L
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MandiGreenLight else MandiSurface)
                                        .border(1.dp, if (isSelected) MandiGreenBorder else MandiBorder, RoundedCornerShape(8.dp))
                                        .clickable { selectedLoanIdForRepay = loanInfo.lend.id }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = if (isEnglish) "Loan: ₹${MandiMathEngine.formatIndianGrouping(loanInfo.lend.amountPaisa / 100L)}" else "ऋण: ₹${MandiMathEngine.formatIndianGrouping(loanInfo.lend.amountPaisa / 100L)}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MandiTextPrimary
                                        )
                                        Text(
                                            text = if (isEnglish) "Outstanding: ₹${MandiMathEngine.formatIndianGrouping(loanOutstandingRs)}" else "बकाया: ₹${MandiMathEngine.formatIndianGrouping(loanOutstandingRs)}",
                                            fontSize = 12.sp,
                                            color = MandiGreenReceivable
                                        )
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedLoanIdForRepay = loanInfo.lend.id }
                                    )
                                }
                            }
                        }
                    } else if (selectedLoan != null) {
                        Text(
                            text = if (isEnglish) "Outstanding loan: ₹${MandiMathEngine.formatIndianGrouping(selectedLoanOutstandingRs)}" else "बकाया ऋण: ₹${MandiMathEngine.formatIndianGrouping(selectedLoanOutstandingRs)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MandiGreenReceivable
                        )
                    }

                    if (selectedLoan != null && pendingCashAmount * 100L > selectedLoan.outstandingPaisa) {
                        Text(
                            text = if (isEnglish) "Note: the amount entered (₹${MandiMathEngine.formatIndianGrouping(pendingCashAmount)}) is more than this loan's outstanding (₹${MandiMathEngine.formatIndianGrouping(selectedLoanOutstandingRs)}). To clear the loan, deduct only the outstanding amount, or record a normal deposit." else "ध्यान दें: दर्ज राशि (₹${MandiMathEngine.formatIndianGrouping(pendingCashAmount)}) इस ऋण के बकाया (₹${MandiMathEngine.formatIndianGrouping(selectedLoanOutstandingRs)}) से अधिक है। यदि पूरा ऋण चुकता करना है तो बकाया राशि जितनी ही काटें, या सामान्य जमा करें।",
                            fontSize = 12.sp,
                            color = MandiRedPayable
                        )
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (selectedLoan != null && canApplyToLoan) {
                                repayDialogLoanId = selectedLoan.lend.id
                                showApplyToLoanDialog = false
                            }
                        },
                        enabled = canApplyToLoan,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MandiBtnSuccessBg,
                            contentColor = Color.White
                        )
                    ) {
                        Text(if (isEnglish) "Deduct from loan" else "उधार में काटें", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (selectedLoan != null && !canApplyToLoan) {
                        val loanRs = selectedLoanOutstandingRs
                        Button(
                            onClick = {
                                repayDialogLoanId = selectedLoan.lend.id
                                showApplyToLoanDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MandiBtnSuccessBg,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                (if (isEnglish) "₹${MandiMathEngine.formatIndianGrouping(loanRs)} to loan, ₹${MandiMathEngine.formatIndianGrouping(pendingCashAmount - loanRs)} normal deposit" else "₹${MandiMathEngine.formatIndianGrouping(loanRs)} उधार में, ₹${MandiMathEngine.formatIndianGrouping(pendingCashAmount - loanRs)} सामान्य जमा"),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.recordCashEntry(
                                transactionType = TransactionType.JAMA_RECEIVED,
                                amountRs = pendingCashAmount,
                                paymentMode = PaymentMode.CASH,
                                remarks = pendingCashRemarks.ifBlank { null }
                            )
                            showApplyToLoanDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isEnglish) "Record as normal deposit" else "सामान्य जमा करें", color = MandiTextPrimary, fontWeight = FontWeight.Medium)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = backToCashDialog,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isEnglish) "Go back" else "वापस जाएँ", color = MandiTextSecondary)
                }
            }
        )
    }

    repayDialogLoanId?.let { loanId ->
        val loanInfo = uiState.openLoans.find { it.lend.id == loanId }
        if (loanInfo == null) {
            repayDialogLoanId = null
        } else {
            RepayLoanDialog(
                outstandingPrincipalPaisa = loanInfo.outstandingPaisa,
                calculatedInterestPaisa = loanInfo.interestDuePaisa,
                monthlyRatePercent = loanInfo.monthlyRatePercent,
                isEnglish = isEnglish,
                initialAmountRs = pendingCashAmount,
                onConfirm = { paymentPaisa, interestPaisa, calculatedPaisa ->
                    viewModel.repayLoan(
                        lendId = loanId,
                        paymentPaisa = paymentPaisa,
                        interestPaisa = interestPaisa,
                        calculatedInterestPaisa = calculatedPaisa,
                        note = pendingCashRemarks.ifBlank { null }
                    )
                    repayDialogLoanId = null
                },
                onDismiss = { repayDialogLoanId = null }
            )
        }
    }

    // Desi Interest Modal (Clean UI)
    if (uiState.isInterestDialogOpen) {
        val principal = if (uiState.balancePaisa > 0) uiState.balancePaisa else 0L
        val rate = interestRateInput.toDoubleOrNull() ?: 1.5
        val days = interestDaysInput.toIntOrNull() ?: 30
        val interestResult = RuralInterestEngine.calculateAccruedInterestByDays(principal, rate, days)

        AlertDialog(
            onDismissRequest = { viewModel.closeInterestDialog() },
            title = {
                Text(
                    text = if (isEnglish) "Interest calculation (rate per hundred)" else "देसी ब्याज हिसाब (सैकड़ा दर)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MandiTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isEnglish) "Principal: ₹${MandiMathEngine.paisaToRupeesString(principal)}" else "मूलधन: ₹${MandiMathEngine.paisaToRupeesString(principal)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MandiAmberDark
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = interestRateInput,
                            onValueChange = { interestRateInput = it },
                            label = { Text(if (isEnglish) "Rate (% per month)" else "दर (% प्रति माह)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = interestDaysInput,
                            onValueChange = { interestDaysInput = it },
                            label = { Text(if (isEnglish) "Days" else "दिन") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MandiNeutralLight)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (isEnglish) "Interest amount:" else "ब्याज राशि:", fontSize = 13.sp, color = MandiTextSecondary)
                            Text("₹${MandiMathEngine.paisaToRupeesString(interestResult.accruedInterestPaisa)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MandiGreenReceivable)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (isEnglish) "Total due:" else "कुल देय:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MandiTextPrimary)
                            Text("₹${MandiMathEngine.paisaToRupeesString(interestResult.totalPayablePaisa)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MandiAmberDark)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (interestResult.accruedInterestPaisa > 0) {
                            viewModel.recordCalculatedInterest(interestResult)
                        } else {
                            viewModel.closeInterestDialog()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MandiPrimaryAction,
                        contentColor = MandiPrimaryActionText
                    )
                ) {
                    Text(if (isEnglish) "Add interest to account" else "ब्याज खाते में जोड़ें", color = MandiPrimaryActionText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeInterestDialog() }) {
                    Text(if (isEnglish) "Close" else "बंद करें", color = MandiTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun FintechActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MandiSurface,
    borderColor: Color = MandiBorder,
    textColor: Color = color
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FintechDealCard(
    isEnglish: Boolean,
    deal: com.appwork.mandisamiti.domain.model.Deal,
    isBuyerLedger: Boolean = false,
    onShareSlip: () -> Unit,
    onEditDeal: () -> Unit = {},
    onLongPress: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val isVoid = deal.isVoid
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(onClick = { if (!isVoid) onEditDeal() }, onLongClick = { if (!isVoid) onLongPress() })
            .background(MandiSurface)
            .border(1.dp, MandiBorder, shape)
            .padding(12.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(
            modifier = Modifier.alpha(if (isVoid) 0.55f else 1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MandiAmberPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isEnglish) "Deal slip #${deal.id.takeLast(5)}" else "सौदा पर्ची #${deal.id.takeLast(5)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                    if (deal.syncStatus == 1) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Synced",
                            tint = MandiGreenReceivable,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Pending Local",
                            tint = MandiTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (!isVoid) {
                    IconButton(
                        onClick = onShareSlip,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = if (isEnglish) "Share slip" else "पर्ची शेयर करें",
                            tint = MandiAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isEnglish) "${deal.bagsCount} bags • ${MandiMathEngine.gramsToQuintals(deal.netWeightGrams)} Qtl" else "${deal.bagsCount} बोरी • ${MandiMathEngine.gramsToQuintals(deal.netWeightGrams)} Qtl",
                    fontSize = 13.sp,
                    color = MandiTextSecondary
                )
                if (isBuyerLedger) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isEnglish) "Due from buyer" else "व्यापारी से वसूली",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MandiTextSecondary
                        )
                        Text(
                            text = "₹${MandiMathEngine.paisaToRupeesString(deal.netBuyerReceivablePaisa)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary,
                            textDecoration = if (isVoid) TextDecoration.LineThrough else null
                        )
                    }
                } else {
                    // A negative payable means the farmer owes the shop: say so instead of printing "₹-…".
                    val farmerOwesShop = deal.netFarmerPayablePaisa < 0
                    Column(horizontalAlignment = Alignment.End) {
                        if (farmerOwesShop) {
                            Text(
                                text = if (isEnglish) "To receive from farmer" else "किसान से लेना है",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MandiGreenReceivable
                            )
                        }
                        Text(
                            text = "₹${MandiMathEngine.paisaToRupeesString(kotlin.math.abs(deal.netFarmerPayablePaisa))}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (farmerOwesShop) MandiGreenReceivable else MandiAmberDark,
                            textDecoration = if (isVoid) TextDecoration.LineThrough else null
                        )
                    }
                }
            }
        }

            if (isVoid) {
                Text(
                    text = if (isEnglish) "Cancelled · ${deal.voidReason?.labelEn ?: ""}" else "रद्द · ${deal.voidReason?.labelHi ?: ""}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MandiTextSecondary
                )
            }
      }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FintechCashCard(
    isEnglish: Boolean,
    transaction: com.appwork.mandisamiti.domain.model.CashTransaction,
    onLongPress: () -> Unit
) {
    val isVoid = transaction.isVoid
    val isDeposit = transaction.transactionType == TransactionType.JAMA_RECEIVED
    val shape = RoundedCornerShape(10.dp)
    val remarks = com.appwork.mandisamiti.domain.util.RemarksFormatter.cleanForDisplay(transaction.remarks)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(onClick = {}, onLongClick = { if (!isVoid) onLongPress() })
            .background(MandiSurface)
            .border(1.dp, MandiBorder, shape)
            .padding(12.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().alpha(if (isVoid) 0.55f else 1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isDeposit) MandiGreenLight else MandiRedLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDeposit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isDeposit) MandiGreenReceivable else MandiRedPayable,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isDeposit) if (isEnglish) "Cash received" else "नकद जमा प्राप्त" else "नकद भुगतान दिया",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                        if (transaction.syncStatus == 1) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Synced",
                                tint = MandiGreenReceivable,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Pending Local",
                                tint = MandiTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    if (!remarks.isNullOrBlank()) {
                        Text(
                            text = remarks,
                            fontSize = 12.sp,
                            color = MandiTextSecondary
                        )
                    }
                }
            }

            Text(
                text = "${if (isDeposit) "+" else "-"}₹${MandiMathEngine.paisaToRupeesString(transaction.amountPaisa)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDeposit) MandiGreenReceivable else MandiRedPayable,
                textDecoration = if (isVoid) TextDecoration.LineThrough else null
            )
        }

            if (isVoid) {
                Text(
                    text = if (isEnglish) "Cancelled · ${transaction.voidReason?.labelEn ?: ""}" else "रद्द · ${transaction.voidReason?.labelHi ?: ""}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MandiTextSecondary
                )
            }
      }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FintechBuyCard(
    isEnglish: Boolean,
    buy: com.appwork.mandisamiti.domain.model.BuyEntry,
    onOpenReceipt: () -> Unit,
    onLongPress: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val isVoid = buy.isVoid
    val cropName = buy.commodityId.ifBlank { if (isEnglish) "Crop" else "फसल" }
    val bagsText = if (isEnglish) "${buy.bagsCount} bags" else "${buy.bagsCount} बोरी"
    val weightText = "${MandiMathEngine.gramsToQuintals(buy.totalWeightGrams)} ${if (isEnglish) "Qtl" else "क्विंटल"}"
    val rateText = "₹${MandiMathEngine.paisaToRupeesString(buy.ratePaisaPerQuintal)}/${if (isEnglish) "Qtl" else "क्विंटल"}"
    val netRupees = MandiMathEngine.paisaToRupeesString(buy.netPayablePaisa)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                onClick = { if (!isVoid) onOpenReceipt() },
                onLongClick = { if (!isVoid) onLongPress() }
            )
            .background(MandiSurface)
            .border(1.dp, MandiBorder, shape)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.alpha(if (isVoid) 0.55f else 1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MandiAmberPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isEnglish) "Purchase slip #${slipLabel(buy.slipNo, buy.id)}" else "खरीद पर्ची #${slipLabel(buy.slipNo, buy.id)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                }

                Text(
                    text = formatEpochDate(buy.entryDate),
                    fontSize = 12.sp,
                    color = MandiTextMuted
                )
            }

            Text(
                text = "$cropName • $bagsText • $weightText (@ $rateText)",
                fontSize = 13.sp,
                color = MandiTextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEnglish) "To pay (Payable)" else "देना है (Payable)",
                    fontSize = 12.sp,
                    color = MandiRedPayable,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "₹$netRupees",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiRedPayable,
                    textDecoration = if (isVoid) TextDecoration.LineThrough else null
                )
            }

            if (isVoid) {
                Text(
                    text = if (isEnglish) "Cancelled · ${formatVoidReason(buy.voidReason, isEnglish)}" else "रद्द · ${formatVoidReason(buy.voidReason, false)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MandiTextSecondary
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FintechSellCard(
    isEnglish: Boolean,
    sell: com.appwork.mandisamiti.domain.model.SellEntry,
    onOpenReceipt: () -> Unit,
    onLongPress: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val isVoid = sell.isVoid
    val cropName = sell.commodityId.ifBlank { if (isEnglish) "Crop" else "फसल" }
    val bagsText = if (isEnglish) "${sell.bagsCount} bags" else "${sell.bagsCount} बोरी"
    val weightText = "${MandiMathEngine.gramsToQuintals(sell.totalWeightGrams)} ${if (isEnglish) "Qtl" else "क्विंटल"}"
    val rateText = "₹${MandiMathEngine.paisaToRupeesString(sell.ratePaisaPerQuintal)}/${if (isEnglish) "Qtl" else "क्विंटल"}"
    val grossRupees = MandiMathEngine.paisaToRupeesString(sell.totalReceivablePaisa)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                onClick = { if (!isVoid) onOpenReceipt() },
                onLongClick = { if (!isVoid) onLongPress() }
            )
            .background(MandiSurface)
            .border(1.dp, MandiBorder, shape)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.alpha(if (isVoid) 0.55f else 1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MandiGreenReceivable,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isEnglish) "Sale slip #${slipLabel(sell.slipNo, sell.id)}" else "बिक्री पर्ची #${slipLabel(sell.slipNo, sell.id)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                }

                Text(
                    text = formatEpochDate(sell.entryDate),
                    fontSize = 12.sp,
                    color = MandiTextMuted
                )
            }

            Text(
                text = "$cropName • $bagsText • $weightText (@ $rateText)",
                fontSize = 13.sp,
                color = MandiTextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEnglish) "To receive (Receivable)" else "लेना है (Receivable)",
                    fontSize = 12.sp,
                    color = MandiGreenReceivable,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "₹$grossRupees",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiGreenReceivable,
                    textDecoration = if (isVoid) TextDecoration.LineThrough else null
                )
            }

            if (isVoid) {
                Text(
                    text = if (isEnglish) "Cancelled · ${formatVoidReason(sell.voidReason, isEnglish)}" else "रद्द · ${formatVoidReason(sell.voidReason, false)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MandiTextSecondary
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FintechLendCard(
    isEnglish: Boolean,
    lend: com.appwork.mandisamiti.domain.model.LendEntry,
    onLongPress: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val isVoid = lend.isVoid
    val amountRupees = MandiMathEngine.paisaToRupeesString(lend.amountPaisa)
    val isAdvanceGiven = lend.lendType == com.appwork.mandisamiti.domain.model.LendType.MONEY

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                onClick = {},
                onLongClick = { if (!isVoid) onLongPress() }
            )
            .background(MandiSurface)
            .border(1.dp, MandiBorder, shape)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.alpha(if (isVoid) 0.55f else 1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isAdvanceGiven) MandiGreenReceivable else MandiRedPayable,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isAdvanceGiven) (if (isEnglish) "Loan #${slipLabel(lend.slipNo, lend.id)}" else "उधार #${slipLabel(lend.slipNo, lend.id)}") else (if (isEnglish) "Bag loan #${slipLabel(lend.slipNo, lend.id)}" else "बोरी उधार #${slipLabel(lend.slipNo, lend.id)}"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                }

                Text(
                    text = formatEpochDate(lend.createdAt),
                    fontSize = 12.sp,
                    color = MandiTextMuted
                )
            }

            val lendNote = com.appwork.mandisamiti.domain.util.RemarksFormatter.cleanForDisplay(lend.notes)
            if (lendNote != null) {
                Text(
                    text = lendNote,
                    fontSize = 13.sp,
                    color = MandiTextSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAdvanceGiven) if (isEnglish) "To receive (Receivable)" else "लेना है (Receivable)" else "बोरी वापस लेना है",
                    fontSize = 12.sp,
                    color = if (isAdvanceGiven) MandiGreenReceivable else MandiTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "₹$amountRupees",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAdvanceGiven) MandiGreenReceivable else MandiTextPrimary,
                    textDecoration = if (isVoid) TextDecoration.LineThrough else null
                )
            }

            if (isVoid) {
                Text(
                    text = if (isEnglish) "Cancelled · ${formatVoidReason(lend.voidReason, isEnglish)}" else "रद्द · ${formatVoidReason(lend.voidReason, false)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MandiTextSecondary
                )
            }
        }
    }
}

private fun formatVoidReason(reasonStr: String?, isEnglish: Boolean): String {
    if (reasonStr.isNullOrBlank()) return ""
    return VoidReason.entries.find { it.name == reasonStr }?.let { if (isEnglish) it.labelEn else it.labelHi } ?: reasonStr
}

private fun formatEpochDate(epochMs: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val monthStr = monthNames.getOrElse(dt.monthNumber - 1) { dt.month.name.take(3) }
    return "${dt.dayOfMonth} $monthStr ${dt.year}"
}
