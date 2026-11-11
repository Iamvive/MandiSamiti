package com.appwork.mandisamiti.ui.register

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiCurrencyFormatter
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import com.appwork.mandisamiti.ui.components.SoundboxTopBar
import com.appwork.mandisamiti.ui.theme.MandiAccent
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiAmberLight
import com.appwork.mandisamiti.ui.theme.MandiAmberPrimary
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiBtnDangerBg
import com.appwork.mandisamiti.ui.theme.MandiBtnDangerFg
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessBg
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessFg
import com.appwork.mandisamiti.ui.theme.MandiGreenLight
import com.appwork.mandisamiti.ui.theme.MandiGreenReceivable
import com.appwork.mandisamiti.ui.theme.MandiNavy
import com.appwork.mandisamiti.ui.theme.MandiNeutralLight
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiRedLight
import com.appwork.mandisamiti.ui.theme.MandiRedPayable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DailyCashRegisterScreen(
    viewModel: DailyRegisterViewModel,
    onNavigateBack: () -> Unit = {},
    onShareWhatsApp: ((String) -> Unit)? = null,
    isEnglish: Boolean = false,
    showBackButton: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDirectCashDialog by remember { mutableStateOf<TransactionType?>(null) }
    var selectedPartyId by remember { mutableStateOf("") }
    var directAmountInput by remember { mutableStateOf("") }
    var directRemarksInput by remember { mutableStateOf("") }
    var directPaymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var physicalCashInput by remember { mutableStateOf("") }
    var physicalCashReason by remember(uiState.selectedDate) { mutableStateOf("") }
    var showOpeningBalanceDialog by remember { mutableStateOf(false) }
    var openingBalanceInput by remember { mutableStateOf("") }
    var openingBalanceRemarks by remember { mutableStateOf("") }
    var showCashHomeDialog by remember { mutableStateOf(false) }
    var cashHomeInput by remember { mutableStateOf("") }
    var cashHomeRemarks by remember { mutableStateOf("") }
    var useDenominations by remember(uiState.selectedDate, uiState.isDayClosingSummaryOpen) { mutableStateOf(false) }
    var denominationQuantities by remember(uiState.selectedDate, uiState.isDayClosingSummaryOpen) { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var showCloseConfirmation by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val visibleTransactions = uiState.todayTransactions.filter {
        it.transaction.transactionType in drawerMovementTypes &&
            it.transaction.paymentMode == PaymentMode.CASH
    }

    val jamaList = remember(visibleTransactions) {
        visibleTransactions.filter { it.transaction.transactionType == TransactionType.JAMA_RECEIVED || it.transaction.transactionType == TransactionType.INTEREST_RECEIVED }
    }
    val nameList = remember(visibleTransactions) {
        visibleTransactions.filter { it.transaction.transactionType == TransactionType.UDHAR_GIVEN }
    }
    val totalJamaSidePaisa = uiState.totalJamaSidePaisa

    Scaffold(
        contentWindowInsets = if (!showBackButton) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        topBar = {
            if (showBackButton) {
                SoundboxTopBar(
                    shopName = if (isEnglish) "Daily Cash Register" else "दैनिक गल्ला रोकड़ बही",
                    mandiLocation = if (isEnglish) "Shop: ${uiState.shopProfile?.shopName ?: "Cash Desk"}" else "दुकान: ${uiState.shopProfile?.shopName ?: "मंडी रोकड़"}",
                    isSoundEnabled = uiState.isSoundEnabled,
                    onToggleSound = {},
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isEnglish) "Go back" else "पीछे जाएं",
                                tint = MandiTextPrimary
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MandiSurface,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MandiBorder)
            ) {
                if (!uiState.isReadOnly) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (showBackButton) Modifier.navigationBarsPadding() else Modifier)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showDirectCashDialog = TransactionType.JAMA_RECEIVED },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MandiBtnSuccessBg,
                                contentColor = MandiBtnSuccessFg
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MandiBtnSuccessFg,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = if (isEnglish) "Cash In (Jama)" else "नकद जमा (In)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiBtnSuccessFg,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = { showDirectCashDialog = TransactionType.UDHAR_GIVEN },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MandiBtnDangerBg,
                                contentColor = MandiBtnDangerFg
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = null,
                                tint = MandiBtnDangerFg,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = if (isEnglish) "Cash Out (Naame)" else "नकद निकासी (Out)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiBtnDangerFg,
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (showBackButton) Modifier.navigationBarsPadding() else Modifier)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MandiTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) "Historical bahi is view-only. Cash entry disabled." else "पुराने दिन की बही सुरक्षित है — नया लेन-देन बंद है।",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MandiTextSecondary
                        )
                    }
                }
            }
        },
        containerColor = MandiBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            DrawerSummaryCard(
                state = uiState, isEnglish = isEnglish,
                onDateClick = { showDatePicker = true },
                onWhatsApp = {
                    onShareWhatsApp?.invoke(viewModel.generateDayClosingReportText(uiState.physicalCountPaisa?.let { it / 100L }))
                },
                onCount = {
                    physicalCashInput = uiState.physicalCountPaisa?.let { (it / 100L).toString() }.orEmpty()
                    physicalCashReason = uiState.physicalCountReason.orEmpty()
                    viewModel.openDayClosingSummary()
                },
                onOpening = {
                    openingBalanceInput = (uiState.openingCashPaisa / 100L).toString()
                    openingBalanceRemarks = ""
                    showOpeningBalanceDialog = true
                },
                onCashHome = {
                    cashHomeInput = ""
                    cashHomeRemarks = ""
                    showCashHomeDialog = true
                },
            )

            if (uiState.isReadOnly) {
                Text(if (isEnglish) "View only" else "सिर्फ देखने के लिए", color = MandiTextSecondary,
                    fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp))
            }

            // Authentic 2-Column Split Bahi (Left Jama, Right Name)
            Card(
                modifier = Modifier
                    .testTag("drawer-ledger")
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MandiSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MandiBorder)
                )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Column Headers (जमा बायां | नामे दायां)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MandiNeutralLight)
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Header (जमा)
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MandiGreenReceivable))
                                Text(
                                    text = if (isEnglish) "Receipts (Jama)" else "जमा पक्ष (आवक)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MandiTextPrimary
                                )
                            }
                        }

                        // Center fold separator
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .width(1.dp)
                                .height(18.dp)
                                .background(MandiBorder)
                        )

                        // Right Header (नामे)
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MandiRedPayable))
                                Text(
                                    text = if (isEnglish) "Payments (Naame)" else "नामे पक्ष (निकासी)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MandiTextPrimary
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MandiBorder))

                    // Scrollable Double Columns
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Left Side: Jama Entries
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            item {
                                BahiAnchorRow(
                                    title = if (isEnglish) "Opening" else "बाकी निकाला",
                                    subtitle = if (isEnglish) "Opening Cash" else "पिछला रोकड़ शेष",
                                    amountPaisa = uiState.openingCashPaisa,
                                    color = MandiNavy,
                                    actionLabel = if (isEnglish) "Edit" else "बदलें",
                                    onClick = {
                                        openingBalanceInput = if (uiState.openingCashPaisa > 0) (uiState.openingCashPaisa / 100L).toString() else ""
                                        openingBalanceRemarks = ""
                                        showOpeningBalanceDialog = true
                                    }
                                )
                            }

                            items(jamaList, key = { it.transaction.id }) { item ->
                                BahiTransactionItem(
                                    item = item,
                                    isDeposit = true,
                                    isEnglish = isEnglish,
                                    isCompact = true
                                )
                            }

                            if (jamaList.isEmpty()) {
                                item {
                                    Text(
                                        text = if (isEnglish) "No cash in" else "आज कोई जमा नहीं",
                                        fontSize = 11.sp,
                                        color = MandiTextMuted,
                                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }

                        // Center Fold Line
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(MandiBorder)
                        )

                        // Right Side: Name Entries
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            items(nameList, key = { it.transaction.id }) { item ->
                                BahiTransactionItem(
                                    item = item,
                                    isDeposit = false,
                                    isEnglish = isEnglish,
                                    isCompact = true
                                )
                            }

                            if (nameList.isEmpty()) {
                                item {
                                    Text(
                                        text = if (isEnglish) "No cash out" else "आज कोई निकासी नहीं",
                                        fontSize = 11.sp,
                                        color = MandiTextMuted,
                                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Tally Bar (दोनों पक्षों का जोड़)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MandiBorder))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MandiNeutralLight)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isEnglish) "Total in (cash)" else "कुल जमा (नकद)",
                                fontSize = 10.sp,
                                color = MandiTextSecondary
                            )
                            Text(
                                text = "+₹${MandiMathEngine.paisaToRupeesString(totalJamaSidePaisa)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiGreenReceivable
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isEnglish) "Total out (cash)" else "कुल नामे (नकद)",
                                fontSize = 10.sp,
                                color = MandiTextSecondary
                            )
                            Text(
                                text = "−₹${MandiMathEngine.paisaToRupeesString(uiState.todayCashOutPaisa)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiRedPayable
                            )
                        }
                    }
                }
            }
        }
    }


    if (showDatePicker) {
        DrawerDatePickerSheet(
            selected = uiState.selectedDate,
            earliest = viewModel.earliestSelectableDate,
            latest = viewModel.latestSelectableDate,
            isEnglish = isEnglish,
            onPick = { viewModel.selectDate(it) },
            onDismiss = { showDatePicker = false },
        )
    }

    // Day Closing bottom sheet
    if (uiState.isDayClosingSummaryOpen) {
        DayClosingSheet(
            state = uiState,
            isEnglish = isEnglish,
            cashInput = physicalCashInput,
            onCashInputChange = { physicalCashInput = it },
            reason = physicalCashReason,
            onReasonChange = { physicalCashReason = it },
            useDenominations = useDenominations,
            denominationQuantities = denominationQuantities,
            onToggleDenominations = {
                useDenominations = !useDenominations
                if (useDenominations) physicalCashInput = denominationTotal(denominationQuantities)?.toString().orEmpty()
            },
            onDenominationChange = { denomination, value ->
                denominationQuantities = denominationQuantities + (denomination to value)
                physicalCashInput = denominationTotal(denominationQuantities)?.toString().orEmpty()
            },
            onDismiss = { viewModel.closeDayClosingSummary() },
            onSaveCount = { rupees -> viewModel.setPhysicalCashCount(rupees, physicalCashReason) },
            onCloseDay = { showCloseConfirmation = true },
            onClearCount = {
                viewModel.setPhysicalCashCount(null)
                physicalCashInput = ""
                viewModel.closeDayClosingSummary()
            },
        )
    }

    if (showCloseConfirmation) {
        AlertDialog(
            onDismissRequest = { showCloseConfirmation = false },
            title = { Text(if (isEnglish) "Close this day?" else "यह दिन बंद करें?") },
            text = { Text(if (isEnglish)
                "The physical count and closing time will be saved. Any shortage or surplus stays visible. Later cash changes require another count."
                else "नकद गिनती और बंद करने का समय सहेजा जाएगा। कमी या अधिक रकम दिखती रहेगी। बाद में नकद प्रविष्टि बदलने पर फिर गिनना होगा।") },
            confirmButton = {
                TextButton(onClick = {
                    showCloseConfirmation = false
                    parseDrawerRupees(physicalCashInput)?.let { viewModel.closeDay(it, physicalCashReason) }
                }) { Text(if (isEnglish) "Close day" else "दिन बंद करें") }
            },
            dismissButton = { TextButton(onClick = { showCloseConfirmation = false }) {
                Text(if (isEnglish) "Keep open" else "खुला रखें")
            } },
        )
    }

    // Direct Cash Dialog
    if (showDirectCashDialog != null) {
        val isDeposit = showDirectCashDialog == TransactionType.JAMA_RECEIVED
        AlertDialog(
            onDismissRequest = { showDirectCashDialog = null },
            title = {
                Text(
                    text = if (isDeposit) (if (isEnglish) "Record Cash Deposit" else "नकद जमा प्रविष्टि") else (if (isEnglish) "Record Cash Withdrawal" else "नकद निकासी प्रविष्टि"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MandiTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (directPaymentMode == PaymentMode.CASH) {
                            if (isEnglish) "Cash payment: changes the physical drawer."
                            else "नकद भुगतान: गल्ले की नकदी बदलेगी।"
                        } else if (isEnglish) "Non-cash payment: recorded in the bahi; drawer cash stays unchanged."
                        else "गैर-नकद भुगतान: बही में दर्ज होगा; गल्ले की नकदी नहीं बदलेगी।",
                        color = MandiTextSecondary, fontSize = 12.sp,
                    )
                    // Party selector
                    if (uiState.availableParties.isNotEmpty()) {
                        Text(
                            text = if (isEnglish) "Select Account (Optional):" else "खाता चुनें (वैकल्पिक):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MandiTextSecondary
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 120.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MandiNeutralLight)
                                .padding(4.dp)
                        ) {
                            items(uiState.availableParties) { party ->
                                val isSelected = selectedPartyId == party.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) MandiAmberLight else Color.Transparent)
                                        .clickable { selectedPartyId = party.id }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${party.name} (${party.village ?: (if (isEnglish) "Village" else "गांव")})",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = MandiTextPrimary
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MandiAmberDark,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        value = directAmountInput,
                        onValueChange = { directAmountInput = it },
                        label = { Text(if (isEnglish) "Amount (₹)" else "राशि (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = if (isEnglish) "Payment mode" else "भुगतान का माध्यम",
                        fontSize = 12.sp,
                        color = MandiTextSecondary
                    )
                    listOf(
                        listOf(PaymentMode.CASH, PaymentMode.RTGS, PaymentMode.NEFT),
                        listOf(PaymentMode.CHEQUE, PaymentMode.UPI),
                    ).forEach { modes ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            modes.forEach { mode ->
                                FilterChip(
                                    selected = directPaymentMode == mode,
                                    onClick = { directPaymentMode = mode },
                                    label = { Text(if (mode == PaymentMode.CASH && !isEnglish) "नकद" else mode.name) }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        value = directRemarksInput,
                        onValueChange = { directRemarksInput = it },
                        label = { Text(if (isEnglish) "Remarks / Note (Optional)" else "विवरण / नोट (वैकल्पिक)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = directAmountInput.toLongOrNull() ?: 0L
                        if (amount > 0L) {
                            val partyId = selectedPartyId.ifBlank { uiState.availableParties.firstOrNull()?.id ?: "" }
                            viewModel.recordDailyEntry(
                                partyId = partyId,
                                transactionType = showDirectCashDialog!!,
                                amountRs = amount,
                                paymentMode = directPaymentMode,
                                remarks = directRemarksInput.ifBlank { null }
                            )
                            showDirectCashDialog = null
                            directAmountInput = ""
                            directRemarksInput = ""
                            selectedPartyId = ""
                            directPaymentMode = PaymentMode.CASH
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDeposit) MandiBtnSuccessBg else MandiBtnDangerBg,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isEnglish) "Save Entry" else "सुरक्षित करें", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectCashDialog = null; selectedPartyId = ""; directPaymentMode = PaymentMode.CASH }) {
                    Text(if (isEnglish) "Cancel" else "रद्द करें", color = MandiTextSecondary)
                }
            }
        )
    }

    // Opening Balance Setup Dialog
    if (showOpeningBalanceDialog) {
        AlertDialog(
            onDismissRequest = { showOpeningBalanceDialog = false },
            title = {
                Text(
                    text = if (isEnglish) "Set / Adjust Opening Cash" else "शुरुआती रोकड़ शेष (बाकी निकाला)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MandiTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isEnglish) {
                            "Set opening cash float in drawer. Current: ₹${MandiMathEngine.paisaToRupeesString(uiState.openingCashPaisa)}"
                        } else {
                            "दुकान खोलने के समय गल्ले में उपलब्ध नकदी निर्धारित करें। वर्तमान: ₹${MandiMathEngine.paisaToRupeesString(uiState.openingCashPaisa)}"
                        },
                        fontSize = 12.sp,
                        color = MandiTextSecondary
                    )
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        value = openingBalanceInput,
                        onValueChange = { openingBalanceInput = it },
                        label = { Text(if (isEnglish) "Opening Cash Float ₹" else "प्रारंभिक रोकड़ शेष ₹") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        value = openingBalanceRemarks,
                        onValueChange = { openingBalanceRemarks = it },
                        label = { Text(if (isEnglish) "Note / Reason (Optional)" else "कारण / नोट (वैकल्पिक)") },
                        placeholder = { Text(if (isEnglish) "e.g. Day 1 initial float" else "जैसे: पहला दिन या घर से लाई नकदी") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = openingBalanceInput.toLongOrNull() ?: 0L
                        viewModel.setOrAdjustOpeningBalance(
                            targetOpeningRs = amount,
                            remarks = openingBalanceRemarks.ifBlank { null }
                        )
                        showOpeningBalanceDialog = false
                        openingBalanceInput = ""
                        openingBalanceRemarks = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MandiPrimaryAction,
                        contentColor = MandiPrimaryActionText
                    )
                ) {
                    Text(if (isEnglish) "Save Opening Cash" else "रोकड़ शेष दर्ज करें", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpeningBalanceDialog = false }) {
                    Text(if (isEnglish) "Cancel" else "रद्द करें", color = MandiTextSecondary)
                }
            }
        )
    }

    // Cash to Home (Owner Drawings) Dialog
    if (showCashHomeDialog) {
        AlertDialog(
            onDismissRequest = { showCashHomeDialog = false },
            title = {
                Text(
                    text = if (isEnglish) "Cash to Home (Owner Drawings)" else "गल्ले से घर निकासी (मालिक का आहरण)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MandiTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isEnglish) {
                            "Record cash taken home from drawer. Current in drawer: ₹${MandiMathEngine.paisaToRupeesString(uiState.inHandCashDrawerPaisa)}"
                        } else {
                            "दुकानदार/मालिक द्वारा गल्ले से घर ले जाई गई नकदी दर्ज करें। वर्तमान गल्ला: ₹${MandiMathEngine.paisaToRupeesString(uiState.inHandCashDrawerPaisa)}"
                        },
                        fontSize = 12.sp,
                        color = MandiTextSecondary
                    )
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        value = cashHomeInput,
                        onValueChange = { cashHomeInput = it },
                        label = { Text(if (isEnglish) "Amount ₹" else "निकासी राशि ₹") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        value = cashHomeRemarks,
                        onValueChange = { cashHomeRemarks = it },
                        label = { Text(if (isEnglish) "Note (Optional)" else "विवरण / नोट (वैकल्पिक)") },
                        placeholder = { Text(if (isEnglish) "e.g. Daily night drawings" else "जैसे: शाम को घर ले गए") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = cashHomeInput.toLongOrNull() ?: 0L
                        if (amount > 0L) {
                            viewModel.recordCashWithdrawalHome(
                                amountRs = amount,
                                remarks = cashHomeRemarks.ifBlank { null }
                            )
                            showCashHomeDialog = false
                            cashHomeInput = ""
                            cashHomeRemarks = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MandiBtnDangerBg,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isEnglish) "Record Cash Out" else "निकासी दर्ज करें", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCashHomeDialog = false }) {
                    Text(if (isEnglish) "Cancel" else "रद्द करें", color = MandiTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun CashBreakdownItem(
    label: String,
    amountPaisa: Long,
    color: Color,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MandiNeutralLight)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MandiTextSecondary
                )
                if (actionLabel != null) {
                    Text(
                        text = actionLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiAmberDark,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MandiAmberLight)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                text = MandiCurrencyFormatter.formatRupees(amountPaisa),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun BahiTransactionItem(
    item: TransactionWithParty,
    isDeposit: Boolean,
    isEnglish: Boolean,
    isCompact: Boolean = false
) {
    val remarks = com.appwork.mandisamiti.domain.util.RemarksFormatter.cleanForDisplay(item.transaction.remarks)
    val isVoid = item.transaction.isVoid
    val amountStr = MandiMathEngine.paisaToRupeesString(item.transaction.amountPaisa)
    val displayName = when {
        item.partyName.isNotBlank() && item.partyName != "खाता" -> item.partyName
        !remarks.isNullOrBlank() -> remarks
        isDeposit -> if (isEnglish) "Jama (Cash In)" else "नकद जमा"
        else -> if (isEnglish) "Naame (Cash Out)" else "विविध निकासी (नामे)"
    }
    val secondaryText = buildString {
        if (!item.village.isNullOrBlank()) {
            append(item.village)
        }
        if (!remarks.isNullOrBlank() && remarks != displayName) {
            if (isNotEmpty()) append(" • ")
            append(remarks)
        }
        if (isEmpty()) {
            append(if (isDeposit) (if (isEnglish) "Jama" else "जमा") else (if (isEnglish) "Naame" else "नामे"))
        }
    }

    var showDetails by remember { mutableStateOf(false) }
    val mode = if (item.transaction.paymentMode == PaymentMode.CASH) {
        if (isEnglish) "Cash" else "नकद"
    } else item.transaction.paymentMode.name
    val time = kotlinx.datetime.Instant.fromEpochMilliseconds(item.transaction.transactionDate)
        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).time.toString().take(5)
    Column(
        Modifier.fillMaxWidth().clickable(onClickLabel = if (isEnglish) "Transaction details" else "प्रविष्टि विवरण") { showDetails = true }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(displayName, color = MandiTextPrimary, fontSize = 15.sp, lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold, maxLines = if (isCompact) 2 else 3, overflow = TextOverflow.Ellipsis)
        Text(
            text = "${if (isDeposit) "+" else "−"}₹$amountStr",
            fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold,
            color = if (isDeposit) MandiGreenReceivable else MandiRedPayable,
            textDecoration = if (isVoid) TextDecoration.LineThrough else null,
        )
        val meta = listOfNotNull(item.village?.takeIf { it.isNotBlank() },
            mode.takeIf { item.transaction.paymentMode != PaymentMode.CASH }).joinToString(" · ")
        if (meta.isNotBlank()) Text(meta, fontSize = 11.sp, lineHeight = 15.sp, color = MandiTextSecondary,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (isVoid) Text(if (isEnglish) "VOID" else "रद्द", color = MandiTextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
    }
    HorizontalDivider(color = MandiBorder)
    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text(displayName, color = MandiTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${if (isDeposit) "+" else "−"}₹$amountStr", color = MandiTextPrimary, fontWeight = FontWeight.Bold)
                    Text(secondaryText, color = MandiTextSecondary)
                    Text("$mode · $time", color = MandiTextSecondary)
                    if (isVoid) Text(if (isEnglish) "VOID · excluded from cash totals" else "रद्द · नकद जोड़ में शामिल नहीं",
                        color = MandiTextPrimary)
                }
            },
            confirmButton = {
                IconButton(onClick = { showDetails = false }) {
                    Icon(Icons.Default.Close, contentDescription = if (isEnglish) "Close details" else "विवरण बंद करें", tint = MandiTextPrimary)
                }
            },
        )
    }
}

@Composable
private fun BahiAnchorRow(
    title: String,
    subtitle: String,
    amountPaisa: Long,
    color: Color,
    actionLabel: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 12.sp, lineHeight = 16.sp, color = MandiTextSecondary)
            Text(MandiCurrencyFormatter.formatRupees(amountPaisa), fontSize = 16.sp, lineHeight = 20.sp,
                fontWeight = FontWeight.Bold, color = color)
        }
        if (actionLabel != null && onClick != null) {
            IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
                Icon(if (actionLabel == "Count" || actionLabel == "गिनती") Icons.Default.Calculate else Icons.Default.Edit,
                    contentDescription = actionLabel, tint = MandiTextPrimary)
            }
        }
    }
    HorizontalDivider(color = MandiBorder)
}

private fun formatBahiDate(date: LocalDate, isEnglish: Boolean, isToday: Boolean): String {
    val dayNameHindi = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "सोमवार"
        DayOfWeek.TUESDAY -> "मंगलवार"
        DayOfWeek.WEDNESDAY -> "बुधवार"
        DayOfWeek.THURSDAY -> "गुरुवार"
        DayOfWeek.FRIDAY -> "शुक्रवार"
        DayOfWeek.SATURDAY -> "शनिवार"
        DayOfWeek.SUNDAY -> "रविवार"
        else -> ""
    }
    val monthNameHindi = when (date.monthNumber) {
        1 -> "जनवरी"
        2 -> "फ़रवरी"
        3 -> "मार्च"
        4 -> "अप्रैल"
        5 -> "मई"
        6 -> "जून"
        7 -> "जुलाई"
        8 -> "अगस्त"
        9 -> "सितंबर"
        10 -> "अक्टूबर"
        11 -> "नवंबर"
        12 -> "दिसंबर"
        else -> ""
    }
    val monthNameEnglish = when (date.monthNumber) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> ""
    }
    val dayNameEnglish = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "Monday"
        DayOfWeek.TUESDAY -> "Tuesday"
        DayOfWeek.WEDNESDAY -> "Wednesday"
        DayOfWeek.THURSDAY -> "Thursday"
        DayOfWeek.FRIDAY -> "Friday"
        DayOfWeek.SATURDAY -> "Saturday"
        DayOfWeek.SUNDAY -> "Sunday"
        else -> ""
    }
    return if (isEnglish) {
        if (isToday) "Today, ${date.dayOfMonth} $monthNameEnglish ${date.year}"
        else "${date.dayOfMonth} $monthNameEnglish ${date.year}, $dayNameEnglish"
    } else {
        if (isToday) "आज, ${date.dayOfMonth} $monthNameHindi ${date.year}"
        else "${date.dayOfMonth} $monthNameHindi ${date.year}, $dayNameHindi"
    }
}

/**
 * Direction B Final तराजू (Taraju) balance scale.
 * Renders level when discrepancy is 0 (balanced), tilts when there is a cash surplus or shortage.
 */
@Composable
fun TarajuBalanceScale(
    diffPaisa: Long,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val isBalanced = diffPaisa == 0L
    val isSurplus = diffPaisa > 0L
    // Tilt angle: 0 for balanced, +6 deg if surplus (right pan down), -6 deg if shortage (left pan down)
    val tiltDegrees = when {
        isBalanced -> 0f
        isSurplus -> 6f
        else -> -6f
    }
    val diffAbs = kotlin.math.abs(diffPaisa)
    val diffStr = MandiMathEngine.paisaToRupeesString(diffAbs)

    val scaleColor = MandiTextPrimary
    val bgScaleColor = if (isBalanced) MandiGreenLight else MandiAmberLight
    val statusColor = if (isBalanced) MandiGreenReceivable else MandiAmberDark

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgScaleColor)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Physical Vector Drawing of Taraju (Scale)
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .width(160.dp)
                .height(44.dp)
        ) {
            val strokeWidth = 2.5.dp.toPx()
            val centerX = size.width / 2f
            val centerY = 14f

            // Central vertical stand / post
            drawLine(
                color = scaleColor,
                start = androidx.compose.ui.geometry.Offset(centerX, 2f),
                end = androidx.compose.ui.geometry.Offset(centerX, size.height - 4f),
                strokeWidth = strokeWidth
            )

            // Central base
            drawLine(
                color = scaleColor,
                start = androidx.compose.ui.geometry.Offset(centerX - 16f, size.height - 4f),
                end = androidx.compose.ui.geometry.Offset(centerX + 16f, size.height - 4f),
                strokeWidth = strokeWidth
            )

            // Rotating Crossbar / Beam
            val beamHalfLength = 54f
            val rad = tiltDegrees * (kotlin.math.PI / 180f).toFloat()
            val dx = beamHalfLength * kotlin.math.cos(rad)
            val dy = beamHalfLength * kotlin.math.sin(rad)

            val leftEnd = androidx.compose.ui.geometry.Offset(centerX - dx, centerY - dy)
            val rightEnd = androidx.compose.ui.geometry.Offset(centerX + dx, centerY + dy)

            drawLine(
                color = scaleColor,
                start = leftEnd,
                end = rightEnd,
                strokeWidth = strokeWidth
            )

            // Left Pan strings & bowl
            val stringLength = 14f
            val panWidth = 26f
            val leftPanBottom = androidx.compose.ui.geometry.Offset(leftEnd.x, leftEnd.y + stringLength)
            drawLine(
                color = scaleColor,
                start = leftEnd,
                end = androidx.compose.ui.geometry.Offset(leftEnd.x - panWidth / 2f, leftEnd.y + stringLength),
                strokeWidth = 1.2.dp.toPx()
            )
            drawLine(
                color = scaleColor,
                start = leftEnd,
                end = androidx.compose.ui.geometry.Offset(leftEnd.x + panWidth / 2f, leftEnd.y + stringLength),
                strokeWidth = 1.2.dp.toPx()
            )
            // Left pan bowl arc
            drawArc(
                color = scaleColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(leftEnd.x - panWidth / 2f, leftEnd.y + stringLength - 4f),
                size = androidx.compose.ui.geometry.Size(panWidth, 12f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )

            // Right Pan strings & bowl
            drawLine(
                color = scaleColor,
                start = rightEnd,
                end = androidx.compose.ui.geometry.Offset(rightEnd.x - panWidth / 2f, rightEnd.y + stringLength),
                strokeWidth = 1.2.dp.toPx()
            )
            drawLine(
                color = scaleColor,
                start = rightEnd,
                end = androidx.compose.ui.geometry.Offset(rightEnd.x + panWidth / 2f, rightEnd.y + stringLength),
                strokeWidth = 1.2.dp.toPx()
            )
            // Right pan bowl arc
            drawArc(
                color = scaleColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(rightEnd.x - panWidth / 2f, rightEnd.y + stringLength - 4f),
                size = androidx.compose.ui.geometry.Size(panWidth, 12f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
        }

        // Status description
        val statusText = when {
            isBalanced -> if (isEnglish) "Books balance exactly (₹0)" else "हिसाब बिल्कुल बराबर है (₹0)"
            isSurplus -> if (isEnglish) "Surplus cash: +₹$diffStr" else "फालतू नकदी: +₹$diffStr"
            else -> if (isEnglish) "Shortage: -₹$diffStr" else "कमी: -₹$diffStr"
        }
        Text(
            text = statusText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )
    }
}



/** Domain lines carry English defaults and raw mode names; show Hindi unless the app is in English. */
internal fun bahiLineLabel(line: com.appwork.mandisamiti.domain.bahi.BahiDayLine, isEnglish: Boolean): String {
    if (isEnglish) return line.label
    val fixed = mapOf(
        "Opening balance" to "खुला बाकी",
        "Closing cash" to "बंद नकद",
        "Day-close difference" to "दिन का अंतर",
        "Buyer payment" to "खरीदार से जमा",
        "Loan principal repaid" to "कर्ज मूलधन वापस",
        "Loan interest received" to "कर्ज ब्याज मिला",
        "Cash entry" to "नकद प्रविष्टि",
    )
    var label = fixed[line.label] ?: line.label
    val modeNames = mapOf(
        "RTGS" to "आरटीजीएस", "NEFT" to "एनईएफटी", "CHEQUE" to "चेक", "UPI" to "यूपीआई", "ONLINE" to "ऑनलाइन", "CASH" to "नकद",
    )
    modeNames.forEach { (raw, hi) -> if (label.endsWith(" · $raw")) label = label.removeSuffix(" · $raw") + " · $hi" }
    return label
}
