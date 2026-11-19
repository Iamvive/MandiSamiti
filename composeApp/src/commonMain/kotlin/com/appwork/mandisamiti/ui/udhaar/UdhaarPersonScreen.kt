package com.appwork.mandisamiti.ui.udhaar

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiCurrencyFormatter
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.LendDirection
import com.appwork.mandisamiti.domain.model.LendKind
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.usecase.UdhaarOverpaymentException
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.ui.components.MandiOutlinedButton
import com.appwork.mandisamiti.ui.ledger.EditRepaymentDialog
import com.appwork.mandisamiti.ui.ledger.RepaymentHistoryDialog
import com.appwork.mandisamiti.ui.ledger.formatEpochDate
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenReceivable
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiRedPayable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UdhaarPersonScreen(
    viewModel: UdhaarPersonViewModel,
    isEnglish: Boolean,
    onBack: () -> Unit,
    onRepaymentSaved: () -> Unit = {},
    onOpenKhata: (Party) -> Unit = {},
) {
    MandiBackHandler(enabled = true) { onBack() }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showRepayDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MandiBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEnglish) "Udhaar" else "उधार",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEnglish) "Back" else "पीछे जाएँ",
                            tint = MandiTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MandiSurface,
                    titleContentColor = MandiTextPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val state = uiState
            if (state != null && !state.isSettled) {
                val btnLabel = if (state.direction == LendDirection.GIVEN) {
                    if (isEnglish) "Received" else "पैसे मिले"
                } else {
                    if (isEnglish) "Paid" else "पैसे दिए"
                }
                ExtendedFloatingActionButton(
                    onClick = { showRepayDialog = true },
                    containerColor = MandiPrimaryAction,
                    contentColor = MandiPrimaryActionText,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(btnLabel, fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        val state = uiState
        if (state == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MandiPrimaryAction)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header card
                item(key = "header_card") {
                    PersonHeaderCard(
                        partyName = state.partyName,
                        village = state.village,
                        phone = state.phone,
                        isEnglish = isEnglish
                    )
                }

                // View Khata button
                item(key = "khata_button") {
                    var lastClickMs by remember { mutableStateOf(0L) }
                    MandiOutlinedButton(
                        text = if (isEnglish) "View Khata" else "खाता देखें",
                        onClick = {
                            val p = state.party
                            if (p != null) {
                                val now = Clock.System.now().toEpochMilliseconds()
                                if (now - lastClickMs > 800L) {
                                    lastClickMs = now
                                    onOpenKhata(p)
                                }
                            }
                        },
                        leadingIcon = Icons.AutoMirrored.Filled.MenuBook,
                        fullWidth = true
                    )
                }

                // Balance card
                item(key = "balance_card") {
                    PersonBalanceCard(
                        state = state,
                        isEnglish = isEnglish
                    )
                }

                // Loans section title
                item(key = "loans_title") {
                    Text(
                        text = if (isEnglish) "Loans & Repayments" else "उधार एवं वापसी",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MandiTextPrimary
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Loans list
                if (state.loans.isEmpty()) {
                    item(key = "empty_loans") {
                        Text(
                            text = if (isEnglish) "No loans found" else "कोई उधार दर्ज नहीं है",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MandiTextSecondary),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    items(
                        items = state.loans,
                        key = { it.loan.id }
                    ) { loanItem ->
                        LoanCardItem(
                            item = loanItem,
                            isEnglish = isEnglish,
                            isGiven = state.direction == LendDirection.GIVEN,
                            onEditRepayment = { settlement ->
                                viewModel.startRepaymentEdit(settlement)
                            },
                            onViewHistory = { settlementId ->
                                viewModel.loadRepaymentHistory(settlementId)
                            }
                        )
                    }
                }
            }

            // Repay Dialog
            if (showRepayDialog) {
                UdhaarRepayDialog(
                    uiState = state,
                    isEnglish = isEnglish,
                    viewModel = viewModel,
                    onDismiss = { showRepayDialog = false },
                    onSaved = {
                        coroutineScope.launch {
                            val msg = if (isEnglish) "Repayment recorded" else "वापसी दर्ज की गई"
                            snackbarHostState.showSnackbar(msg)
                        }
                        onRepaymentSaved()
                    }
                )
            }

            // Edit Repayment Dialog
            val editSettlement = state.repaymentEditSettlement
            if (editSettlement != null) {
                EditRepaymentDialog(
                    settlement = editSettlement,
                    isEnglish = isEnglish,
                    preview = state.repaymentEditPreview,
                    errorCode = state.repaymentEditError,
                    onPreview = { amountPaisa, settledAt ->
                        viewModel.previewRepaymentEdit(editSettlement, amountPaisa, settledAt)
                    },
                    onConfirm = { amountPaisa, settledAt ->
                        coroutineScope.launch {
                            val res = viewModel.editRepayment(editSettlement, amountPaisa, settledAt)
                            res.onSuccess {
                                val msg = if (isEnglish) "Repayment updated" else "वापसी सुधारी गई"
                                snackbarHostState.showSnackbar(msg)
                                onRepaymentSaved()
                            }
                        }
                    },
                    onDismiss = { viewModel.clearRepaymentEdit() }
                )
            }

            // History Dialog
            val history = state.repaymentHistory
            if (history != null) {
                RepaymentHistoryDialog(
                    history = history,
                    isEnglish = isEnglish,
                    onDismiss = { viewModel.clearRepaymentHistory() }
                )
            }
        }
    }
}

@Composable
private fun PersonHeaderCard(
    partyName: String?,
    village: String?,
    phone: String?,
    isEnglish: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        border = BorderStroke(1.dp, MandiBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val displayName = partyName ?: if (isEnglish) "Unknown" else "अज्ञात"
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (!village.isNullOrBlank()) {
                Text(
                    text = village,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MandiTextSecondary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (!phone.isNullOrBlank()) {
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MandiTextSecondary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PersonBalanceCard(
    state: UdhaarPersonUiState,
    isEnglish: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        border = BorderStroke(1.dp, MandiBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (state.isSettled) {
                Text(
                    text = if (isEnglish) "Settled" else "चुकता",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MandiGreenReceivable
                    )
                )
                Text(
                    text = if (isEnglish) "All loans in this direction are settled" else "इस दिशा के सभी उधार चुकता हैं",
                    style = MaterialTheme.typography.bodySmall.copy(color = MandiTextSecondary)
                )
            } else {
                val label = if (state.direction == LendDirection.GIVEN) {
                    if (isEnglish) "Outstanding Balance (Receivable)" else "कुल बाकी (लेना)"
                } else {
                    if (isEnglish) "Outstanding Balance (Payable)" else "कुल बाकी (देना)"
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(color = MandiTextSecondary)
                )

                val dueText = MandiCurrencyFormatter.formatRupees(state.principalDuePaisa)
                Text(
                    text = "बाकी $dueText",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                )

                if (state.interestOn && state.interestDuePaisa > 0L) {
                    val interestText = MandiCurrencyFormatter.formatRupees(state.interestDuePaisa)
                    val totalDueText = MandiCurrencyFormatter.formatRupees(state.totalDuePaisa)
                    Text(
                        text = "+ ब्याज $interestText (कुल $totalDueText)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MandiTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
internal fun LoanCardItem(
    item: UdhaarLoanWithSettlements,
    isEnglish: Boolean,
    isGiven: Boolean,
    onEditRepayment: (LendSettlement) -> Unit,
    onViewHistory: (settlementId: String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        border = BorderStroke(1.dp, MandiBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Loan Top Row: Date and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatEpochDate(item.loan.entryDate),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MandiTextSecondary)
                )
                Text(
                    text = MandiCurrencyFormatter.formatRupees(item.loan.amountPaisa),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                )
            }

            // Chips & Receiver Row
            val hasChips = item.loan.kind == LendKind.PESGI ||
                (isGiven && !item.loan.receiverName.isNullOrBlank()) ||
                item.loan.monthlyRatePercent > 0.0

            if (hasChips) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.loan.kind == LendKind.PESGI) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MandiSurfaceElevated,
                            border = BorderStroke(1.dp, MandiBorder)
                        ) {
                            Text(
                                text = if (isEnglish) "Pesgi" else "पेशगी",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MandiTextPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (isGiven && !item.loan.receiverName.isNullOrBlank()) {
                        Text(
                            text = "${if (isEnglish) "Receiver: " else "प्राप्तकर्ता: "}${item.loan.receiverName}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MandiTextSecondary),
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    if (item.loan.monthlyRatePercent > 0.0) {
                        Text(
                            text = "${item.loan.monthlyRatePercent}% ${if (isEnglish) "monthly" else "मासिक"}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MandiTextMuted)
                        )
                    }
                }
            }

            // Nested Settlements List
            if (item.settlements.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MandiSurfaceElevated,
                    border = BorderStroke(1.dp, MandiBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Repayments" else "वापसी",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MandiTextSecondary
                            )
                        )

                        item.settlements.forEach { settlement ->
                            SettlementRowItem(
                                settlement = settlement,
                                isEnglish = isEnglish,
                                onEdit = { onEditRepayment(settlement) },
                                onViewHistory = { onViewHistory(settlement.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettlementRowItem(
    settlement: LendSettlement,
    isEnglish: Boolean,
    onEdit: () -> Unit,
    onViewHistory: () -> Unit,
) {
    val isVoid = settlement.isVoid
    val textColor = if (isVoid) MandiTextMuted else MandiTextPrimary
    val textDecoration = if (isVoid) TextDecoration.LineThrough else TextDecoration.None

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formatEpochDate(settlement.settledAt),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isVoid) MandiTextMuted else MandiTextSecondary,
                        textDecoration = textDecoration
                    )
                )
                Text(
                    text = MandiCurrencyFormatter.formatRupees(settlement.amountPaisa),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        textDecoration = textDecoration
                    )
                )
                if (isVoid) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MandiSurface,
                        border = BorderStroke(1.dp, MandiBorder)
                    ) {
                        Text(
                            text = if (isEnglish) "Cancelled" else "रद्द",
                            fontSize = 10.sp,
                            color = MandiTextMuted,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            if (!isVoid && (settlement.principalPaisa > 0L || settlement.interestPaisa > 0L)) {
                val principalStr = MandiCurrencyFormatter.formatRupees(settlement.principalPaisa)
                val interestStr = MandiCurrencyFormatter.formatRupees(settlement.interestPaisa)
                val breakdown = if (isEnglish) {
                    "Principal: $principalStr · Interest: $interestStr"
                } else {
                    "मूल: $principalStr · ब्याज: $interestStr"
                }
                Text(
                    text = breakdown,
                    style = MaterialTheme.typography.labelSmall.copy(color = MandiTextSecondary)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isVoid) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = if (isEnglish) "Edit repayment" else "वापसी सुधारें",
                        tint = MandiTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (settlement.revision > 1L) {
                IconButton(
                    onClick = onViewHistory,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = if (isEnglish) "History" else "इतिहास",
                        tint = MandiTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UdhaarRepayDialog(
    uiState: UdhaarPersonUiState,
    isEnglish: Boolean,
    viewModel: UdhaarPersonViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val todayMs = remember { Clock.System.now().toEpochMilliseconds() }
    val formState = remember {
        UdhaarRepayFormState(
            totalDuePaisa = uiState.totalDuePaisa,
            todayMs = todayMs,
            minDateMs = uiState.minDateMs,
        )
    }
    val coroutineScope = rememberCoroutineScope()
    var computeJob by remember { mutableStateOf<Job?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.totalDuePaisa, uiState.minDateMs) {
        formState.minDateMs = uiState.minDateMs
        val isToday = isSameDay(formState.dateMs, formState.todayMs)
        if (isToday) {
            formState.updateTotalDue(uiState.totalDuePaisa)
        } else {
            computeJob?.cancel()
            computeJob = coroutineScope.launch {
                val due = viewModel.computeDueAt(formState.dateMs)
                formState.updateTotalDue(due.totalDuePaisa)
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localInstantToPickerUtc(formState.dateMs)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMs ->
                        val localMs = normalisePickerDate(utcMs)
                        formState.dateMs = localMs
                        computeJob?.cancel()
                        computeJob = coroutineScope.launch {
                            val due = viewModel.computeDueAt(localMs)
                            formState.updateTotalDue(due.totalDuePaisa)
                        }
                    }
                    showDatePicker = false
                }) {
                    Text(if (isEnglish) "Select Date" else "तारीख चुनें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(if (isEnglish) "Cancel" else "रद्द करें")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = MandiSurface,
        title = {
            val titleText = if (uiState.direction == LendDirection.GIVEN) {
                if (isEnglish) "Receive Payment" else "पैसे मिले"
            } else {
                if (isEnglish) "Pay Money" else "पैसे दिए"
            }
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Banner with Total Due and "Pay full" chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MandiSurfaceElevated,
                    border = BorderStroke(1.dp, MandiBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isEnglish) "Total Due" else "कुल बाकी",
                                style = MaterialTheme.typography.bodySmall.copy(color = MandiTextSecondary)
                            )
                            Text(
                                text = MandiCurrencyFormatter.formatRupees(formState.totalDuePaisa),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MandiTextPrimary
                                )
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MandiPrimaryAction,
                            modifier = Modifier.clickable {
                                formState.amountRupees = MandiMathEngine.paisaToInputString(formState.totalDuePaisa)
                            }
                        ) {
                            Text(
                                text = if (isEnglish) "Pay full" else "पूरा चुकाएँ",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MandiPrimaryActionText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }

                // Amount OutlinedTextField
                OutlinedTextField(
                    value = formState.amountRupees,
                    onValueChange = {
                        formState.amountRupees = it
                        saveError = null
                    },
                    label = { Text(if (isEnglish) "Amount" else "रकम") },
                    prefix = { Text("₹ ", color = MandiTextPrimary, fontWeight = FontWeight.SemiBold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = formState.validationReason(isEnglish) != null,
                    supportingText = formState.validationReason(isEnglish)?.let { reason ->
                        { Text(reason, color = MandiRedPayable) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Date Picker trigger
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MandiBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isEnglish) "Date" else "तारीख",
                                style = MaterialTheme.typography.bodySmall.copy(color = MandiTextSecondary)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatEpochDate(formState.dateMs),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MandiTextPrimary
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = if (isEnglish) "Select date" else "तारीख चुनें",
                            tint = MandiTextSecondary
                        )
                    }
                }

                // Generic error banner
                if (saveError != null) {
                    Text(
                        text = saveError!!,
                        style = MaterialTheme.typography.bodySmall.copy(color = MandiRedPayable)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (formState.isValid && !isSaving) {
                        isSaving = true
                        coroutineScope.launch {
                            val res = viewModel.repay(formState.paymentPaisa, formState.dateMs)
                            isSaving = false
                            res.onSuccess {
                                onSaved()
                                onDismiss()
                            }.onFailure { err ->
                                if (err is UdhaarOverpaymentException) {
                                    formState.onOverpayment(err)
                                } else {
                                    saveError = if (isEnglish) "Failed to record repayment" else "वापसी दर्ज नहीं हो सकी"
                                }
                            }
                        }
                    }
                },
                enabled = formState.isValid && !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MandiPrimaryAction,
                    contentColor = MandiPrimaryActionText
                )
            ) {
                val btnText = if (uiState.direction == LendDirection.GIVEN) {
                    if (isEnglish) "Received" else "पैसे मिले"
                } else {
                    if (isEnglish) "Paid" else "पैसे दिए"
                }
                Text(btnText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text(
                    text = if (isEnglish) "Cancel" else "रद्द करें",
                    color = MandiTextSecondary
                )
            }
        }
    )
}
