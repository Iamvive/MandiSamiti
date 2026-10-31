package com.appwork.mandisamiti.ui.register

import androidx.compose.foundation.BorderStroke
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

enum class BahiViewMode {
    LIST,
    SPLIT, // दो पल्ला (Left Jama, Right Name)
    JAMA,  // केवल जमा पक्ष
    NAME   // केवल नामे पक्ष
}

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
    val drawerChipColors = FilterChipDefaults.filterChipColors(
        containerColor = MandiSurface,
        labelColor = MandiTextSecondary,
        selectedContainerColor = MandiSurfaceElevated,
        selectedLabelColor = MandiTextPrimary,
    )

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
    var bahiViewMode by remember { mutableStateOf(BahiViewMode.SPLIT) }
    var showTransactionList by remember { mutableStateOf(false) }
    var cashOnly by remember { mutableStateOf(false) }
    var useDenominations by remember(uiState.selectedDate, uiState.isDayClosingSummaryOpen) { mutableStateOf(false) }
    var denominationQuantities by remember(uiState.selectedDate, uiState.isDayClosingSummaryOpen) { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var showCloseConfirmation by remember { mutableStateOf(false) }
    val visibleTransactions = uiState.todayTransactions.filter {
        it.transaction.transactionType in drawerMovementTypes &&
            (!cashOnly || it.transaction.paymentMode == PaymentMode.CASH)
    }

    val jamaList = remember(visibleTransactions) {
        visibleTransactions.filter { it.transaction.transactionType == TransactionType.JAMA_RECEIVED || it.transaction.transactionType == TransactionType.INTEREST_RECEIVED }
    }
    val nameList = remember(visibleTransactions) {
        visibleTransactions.filter { it.transaction.transactionType == TransactionType.UDHAR_GIVEN }
    }
    val totalJamaSidePaisa = uiState.totalJamaSidePaisa
    val totalNameSidePaisa = uiState.totalNameSidePaisa

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

            // Negative Cash Warning Banner (Opening float missing / cash below zero)
            if (uiState.isNegativeCash) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MandiAmberLight
                    ),
                    border = BorderStroke(1.dp, MandiAmberDark.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MandiAmberDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isEnglish) "Cash is below zero. Opening balance may be missing." else "गल्ले में रकम माइनस है। शुरुआती रोकड़ दर्ज करें?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MandiAmberDark,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                openingBalanceInput = if (uiState.openingCashPaisa > 0) (uiState.openingCashPaisa / 100L).toString() else ""
                                openingBalanceRemarks = ""
                                showOpeningBalanceDialog = true
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MandiPrimaryAction,
                                contentColor = MandiPrimaryActionText
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Set Float" else "रोकड़ दर्ज करें",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                color = MandiSurface,
            ) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("drawer-navigation"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { viewModel.navigateToPreviousDay() }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = if (isEnglish) "Previous Day" else "पिछला दिन", tint = MandiTextPrimary)
                    }
                    Text(
                        text = "${uiState.selectedDate.dayOfMonth.toString().padStart(2, '0')}/${uiState.selectedDate.monthNumber.toString().padStart(2, '0')}/${uiState.selectedDate.year}",
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MandiTextPrimary,
                    )
                    IconButton(onClick = { viewModel.navigateToNextDay() }, enabled = !uiState.isToday, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ChevronRight, contentDescription = if (isEnglish) "Next Day" else "अगला दिन", tint = MandiTextPrimary)
                    }
                    IconButton(
                        onClick = { showTransactionList = !showTransactionList },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(if (showTransactionList) Icons.Default.MenuBook else Icons.Default.ViewList,
                            contentDescription = if (showTransactionList) {
                                if (isEnglish) "Bahi" else "बही"
                            } else if (isEnglish) "List" else "सूची",
                            tint = MandiTextPrimary)
                    }
                    DrawerViewToggle(Icons.Default.LocalAtm, if (isEnglish) "Cash only" else "सिर्फ नकद", cashOnly) { cashOnly = !cashOnly }
                }
            }
            if (uiState.isReadOnly) {
                Text(if (isEnglish) "View only" else "सिर्फ देखने के लिए", color = MandiTextSecondary,
                    fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp))
            }

            // Main Ledger Book Body (पन्ना)
            when (if (showTransactionList) BahiViewMode.LIST else bahiViewMode) {
                BahiViewMode.LIST -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).testTag("drawer-ledger"),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            Text(if (isEnglish) "Latest first · ${visibleTransactions.size} entries" else "नई प्रविष्टि पहले · ${visibleTransactions.size} प्रविष्टियां",
                                color = MandiTextSecondary, fontSize = 12.sp)
                        }
                        items(visibleTransactions, key = { it.transaction.id }) { entry ->
                            BahiTransactionItem(entry,
                                entry.transaction.transactionType != TransactionType.UDHAR_GIVEN, isEnglish)
                        }
                        if (visibleTransactions.isEmpty()) item {
                            Text(if (isEnglish) "No entries for this date and payment filter."
                                else "इस दिन और भुगतान प्रकार में कोई प्रविष्टि नहीं है।",
                                color = MandiTextSecondary, modifier = Modifier.padding(vertical = 20.dp))
                        }
                    }
                }
                BahiViewMode.SPLIT -> {
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
                                    Text(
                                        text = "${jamaList.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MandiTextSecondary
                                    )
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
                                    Text(
                                        text = "${nameList.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MandiTextSecondary
                                    )
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

                                    item {
                                        BahiAnchorRow(
                                            title = if (isEnglish) "Closing cash" else "सिल्क बाकी",
                                            subtitle = if (uiState.physicalCountPaisa != null) {
                                                dayClosingCountLabel(
                                                    uiState.physicalCountPaisa ?: uiState.inHandCashDrawerPaisa,
                                                    uiState.inHandCashDrawerPaisa,
                                                    isEnglish
                                                )
                                            } else {
                                                if (isEnglish) "Book closing (uncounted)" else "बही शेष (गिनती बाकी)"
                                            },
                                            amountPaisa = uiState.effectiveClosingCashPaisa,
                                            color = MandiNavy,
                                            actionLabel = if (uiState.physicalCountPaisa == null) (if (isEnglish) "Count" else "गिनती") else (if (isEnglish) "Edit" else "बदलें"),
                                            onClick = {
                                                physicalCashInput = uiState.physicalCountPaisa?.let { (it / 100L).toString() } ?: ""
                                                viewModel.openDayClosingSummary()
                                            }
                                        )
                                    }
                                }
                            }

                            // Bottom Tally Bar (दोनों पक्षों का जोड़)
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MandiBorder))
                            androidx.compose.foundation.layout.FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MandiNeutralLight)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = if (isEnglish) "Opening + cash in" else "पिछला शेष + नकद आवक",
                                        fontSize = 10.sp,
                                        color = MandiTextSecondary
                                    )
                                    Text(
                                        text = "₹${MandiMathEngine.paisaToRupeesString(totalJamaSidePaisa)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiTextPrimary
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            physicalCashInput = uiState.physicalCountPaisa?.let { (it / 100L).toString() } ?: ""
                                            viewModel.openDayClosingSummary()
                                        }
                                        .background(
                                            when (uiState.reconciliationStatus) {
                                                BahiReconciliationStatus.NOT_COUNTED, BahiReconciliationStatus.RECOUNT_REQUIRED -> MandiSurfaceElevated
                                                BahiReconciliationStatus.BALANCED -> MandiGreenLight
                                                BahiReconciliationStatus.DISCREPANCY -> MandiAmberLight
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val badgeIcon = when (uiState.reconciliationStatus) {
                                        BahiReconciliationStatus.NOT_COUNTED, BahiReconciliationStatus.RECOUNT_REQUIRED -> Icons.Default.Info
                                        BahiReconciliationStatus.BALANCED -> Icons.Default.CheckCircle
                                        BahiReconciliationStatus.DISCREPANCY -> Icons.Default.Warning
                                    }
                                    val badgeColor = when (uiState.reconciliationStatus) {
                                        BahiReconciliationStatus.NOT_COUNTED, BahiReconciliationStatus.RECOUNT_REQUIRED -> MandiTextSecondary
                                        BahiReconciliationStatus.BALANCED -> MandiGreenReceivable
                                        BahiReconciliationStatus.DISCREPANCY -> MandiAmberDark
                                    }
                                    val badgeText = when (uiState.reconciliationStatus) {
                                        BahiReconciliationStatus.NOT_COUNTED -> if (isEnglish) "Not counted" else "मिलान बाकी"
                                        BahiReconciliationStatus.RECOUNT_REQUIRED -> if (isEnglish) "Recount" else "फिर गिनें"
                                        BahiReconciliationStatus.BALANCED -> if (isEnglish) "Cash matched" else "नकद मिला"
                                        BahiReconciliationStatus.DISCREPANCY -> {
                                            val diffAbs = if (uiState.discrepancyPaisa >= 0) uiState.discrepancyPaisa else -uiState.discrepancyPaisa
                                            val diffStr = MandiMathEngine.paisaToRupeesString(diffAbs)
                                            if (uiState.discrepancyPaisa > 0) {
                                                if (isEnglish) "Diff: +₹$diffStr" else "अंतर: +₹$diffStr"
                                            } else {
                                                if (isEnglish) "Diff: -₹$diffStr" else "अंतर: -₹$diffStr"
                                            }
                                        }
                                    }
                                    Icon(
                                        imageVector = badgeIcon,
                                        contentDescription = badgeText,
                                        tint = MandiTextPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isEnglish) "Cash out + closing" else "नकद निकासी + अंतिम शेष",
                                        fontSize = 11.sp,
                                        color = MandiTextSecondary
                                    )
                                    Text(
                                        text = "₹${MandiMathEngine.paisaToRupeesString(totalNameSidePaisa)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                BahiViewMode.JAMA -> {
                    // Full Width Jama Column
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            BahiAnchorRow(
                                title = if (isEnglish) "Opening Balance" else "बाकी निकाला (Opening Balance)",
                                subtitle = if (isEnglish) "Carried forward from yesterday" else "पिछला रोकड़ शेष",
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
                                isEnglish = isEnglish
                            )
                        }

                        if (jamaList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isEnglish) "No cash deposits today" else "आज कोई रोकड़ जमा प्रविष्टि नहीं है",
                                        color = MandiTextMuted,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MandiNeutralLight)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEnglish) "Total Jama (Opening + Receipts)" else "कुल जमा जोड़ (पिछला + आज की आवक)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiTextPrimary
                                    )
                                    Text(
                                        text = "₹${MandiMathEngine.paisaToRupeesString(totalJamaSidePaisa)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                BahiViewMode.NAME -> {
                    // Full Width Name Column
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(nameList, key = { it.transaction.id }) { item ->
                            BahiTransactionItem(
                                item = item,
                                isDeposit = false,
                                isEnglish = isEnglish
                            )
                        }

                        if (nameList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isEnglish) "No cash withdrawals today" else "आज कोई रोकड़ निकासी प्रविष्टि नहीं है",
                                        color = MandiTextMuted,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        item {
                            BahiAnchorRow(
                                title = if (isEnglish) "Closing cash" else "सिल्क बाकी",
                                subtitle = if (uiState.physicalCountPaisa != null) {
                                    if (isEnglish) "Counted physical cash in drawer" else "गल्ले में गिनी हुई शुद्ध नकदी"
                                } else {
                                    if (isEnglish) "Book closing cash in drawer" else "बही शेष (गिनती बाकी)"
                                },
                                amountPaisa = uiState.effectiveClosingCashPaisa,
                                color = MandiNavy,
                                actionLabel = if (uiState.physicalCountPaisa == null) (if (isEnglish) "Count" else "गिनती") else (if (isEnglish) "Edit" else "बदलें"),
                                onClick = {
                                    physicalCashInput = uiState.physicalCountPaisa?.let { (it / 100L).toString() } ?: ""
                                    viewModel.openDayClosingSummary()
                                }
                            )
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MandiNeutralLight)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEnglish) "Total Naame (Outflows + Silak)" else "कुल नामे जोड़ (खर्च + सिल्क बाकी)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiTextPrimary
                                    )
                                    Text(
                                        text = "₹${MandiMathEngine.paisaToRupeesString(totalNameSidePaisa)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    // Day Closing Summary & Reconciliation Modal
    if (uiState.isDayClosingSummaryOpen) {
        val expectedInHand = uiState.inHandCashDrawerPaisa
        val countedRs = parseDrawerRupees(physicalCashInput)
        val countedPaisa = if (countedRs != null) countedRs * 100L else null
        val diffPaisa = if (countedPaisa != null) countedPaisa - expectedInHand else null

        AlertDialog(
            onDismissRequest = { viewModel.closeDayClosingSummary() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MandiAmberDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isEnglish) "Day Closing Reconciliation" else "दैनिक रोज़नामा (Day Closing)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MandiTextPrimary
                    )
                }
            },
            text = {
                Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Summary Details Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MandiNeutralLight)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isEnglish) "Opening Balance:" else "पिछला शेष (Opening):", fontSize = 13.sp, color = MandiTextSecondary)
                            Text("₹${MandiMathEngine.paisaToRupeesString(uiState.openingCashPaisa)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MandiNavy)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isEnglish) "Total Cash In:" else "कुल नकद आवक (In):", fontSize = 13.sp, color = MandiTextSecondary)
                            Text("+₹${MandiMathEngine.paisaToRupeesString(uiState.todayCashInPaisa)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MandiTextPrimary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isEnglish) "Total Cash Out:" else "कुल नकद निकासी (Out):", fontSize = 13.sp, color = MandiTextSecondary)
                            Text("-₹${MandiMathEngine.paisaToRupeesString(uiState.todayCashOutPaisa)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MandiTextPrimary)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isEnglish) "Expected Cash In Hand:" else "गल्ले में शुद्ध नकदी (Expected):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MandiTextPrimary)
                            Text("₹${MandiMathEngine.paisaToRupeesString(expectedInHand)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MandiNavy)
                        }
                    }

                    uiState.bahiDayPage?.let { page ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                (if (isEnglish) "Left" else "जमा") to page.left,
                                (if (isEnglish) "Right" else "नामे") to page.right,
                            ).forEach { (heading, lines) ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MandiSurfaceElevated)
                                        .padding(7.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(heading, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MandiTextPrimary)
                                    lines.takeLast(5).forEach { line ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(
                                                line.label,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSize = 9.sp,
                                                color = MandiTextSecondary,
                                                textDecoration = if (line.isStruckThrough) TextDecoration.LineThrough else TextDecoration.None,
                                            )
                                            Text(
                                                "₹${MandiMathEngine.paisaToRupeesString(line.amountPaisa)}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MandiTextPrimary,
                                                textDecoration = if (line.isStruckThrough) TextDecoration.LineThrough else TextDecoration.None,
                                            )
                                        }
                                    }
                                    val total = if (heading == (if (isEnglish) "Left" else "जमा")) page.leftTotalPaisa else page.rightTotalPaisa
                                    Text("₹${MandiMathEngine.paisaToRupeesString(total)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MandiNavy)
                                }
                            }
                        }
                        if (!page.isBalanced) {
                            Text(
                                text = (if (isEnglish) "Difference: " else "अंतर: ") + "₹${MandiMathEngine.paisaToRupeesString(kotlin.math.abs(page.differencePaisa))}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiTextPrimary,
                            )
                            Text(if (isEnglish) "Likely causes" else "संभावित कारण", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MandiTextPrimary)
                            val causes = page.likelyCauses.ifEmpty {
                                listOf(
                                    com.appwork.mandisamiti.domain.bahi.BahiLikelyCause("online", if (isEnglish) "Online entry missing its other side" else "ऑनलाइन प्रविष्टि का दूसरा पक्ष छूटा"),
                                    com.appwork.mandisamiti.domain.bahi.BahiLikelyCause("amount", if (isEnglish) "Amount differs from the party's usual" else "राशि पार्टी के सामान्य हिसाब से अलग"),
                                    com.appwork.mandisamiti.domain.bahi.BahiLikelyCause("late", if (isEnglish) "Entry saved after closing" else "दिन बंद होने के बाद प्रविष्टि"),
                                )
                            }
                            causes.take(3).forEach { cause ->
                                Text("• ${cause.label}", fontSize = 10.sp, color = MandiTextSecondary)
                            }
                        }
                    }

                    Text(if (isEnglish) "Counts and closing are saved on this device. Differences remain visible; no cash adjustment is created."
                        else "गिनती और दिन बंद करने की जानकारी इस फोन पर सहेजी जाती है। अंतर दिखता रहेगा; नकद समायोजन अपने आप नहीं बनेगा।",
                        color = MandiTextSecondary, fontSize = 12.sp)
                    TextButton(onClick = {
                        useDenominations = !useDenominations
                        if (useDenominations) physicalCashInput = denominationTotal(denominationQuantities)?.toString().orEmpty()
                    }) {
                        Text(if (useDenominations) {
                            if (isEnglish) "Enter total instead" else "सीधे कुल रकम भरें"
                        } else if (isEnglish) "Count notes and coins" else "नोट और सिक्के गिनें", color = MandiTextPrimary)
                    }
                    if (useDenominations) {
                        drawerDenominations.forEach { denomination ->
                            OutlinedTextField(
                                value = denominationQuantities[denomination].orEmpty(),
                                onValueChange = { value ->
                                    denominationQuantities = denominationQuantities + (denomination to value)
                                    physicalCashInput = denominationTotal(denominationQuantities)?.toString().orEmpty()
                                },
                                label = { Text("₹$denomination × " + if (isEnglish) "quantity" else "संख्या") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = denominationQuantities[denomination]?.let { it.isNotBlank() && parseDrawerRupees(it) == null } ?: false,
                                singleLine = true, modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    // Physical Cash Verification Input
                    OutlinedTextField(
                        value = physicalCashInput,
                        onValueChange = { physicalCashInput = it },
                        readOnly = useDenominations,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = physicalCashInput.isNotEmpty() && countedRs == null,
                        label = { Text(if (isEnglish) "Counted cash ₹" else "गिनी हुई नकदी ₹") },
                        placeholder = { Text(if (isEnglish) "Enter counted cash in drawer" else "गल्ले में गिने हुए रुपये दर्ज करें") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = physicalCashReason,
                        onValueChange = { physicalCashReason = it },
                        label = { Text(if (isEnglish) "Reason (optional)" else "कारण (वैकल्पिक)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Difference / Discrepancy Indicator
                    if (diffPaisa != null) {
                        val diffColor = MandiTextPrimary
                        val diffLabel = when {
                            diffPaisa == 0L -> if (isEnglish) "Cash matches perfectly (₹0)" else "हिसाब बिल्कुल बराबर है (₹0)"
                            diffPaisa > 0L -> if (isEnglish) "Surplus Cash: +₹${MandiMathEngine.paisaToRupeesString(diffPaisa)}" else "फालतू नकदी: +₹${MandiMathEngine.paisaToRupeesString(diffPaisa)}"
                            else -> if (isEnglish) "Shortage: -₹${MandiMathEngine.paisaToRupeesString(-diffPaisa)}" else "कमी: -₹${MandiMathEngine.paisaToRupeesString(-diffPaisa)}"
                        }
                        Text(
                            text = diffLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = diffColor
                        )
                    }

                    uiState.countError?.let {
                        Text(if (isEnglish) "Could not save count. Please retry." else "गिनती सहेजी नहीं गई। फिर कोशिश करें।",
                            color = MandiTextPrimary)
                    }
                    OutlinedButton(
                        onClick = { onShareWhatsApp?.invoke(viewModel.generateDayClosingReportText(countedRs)) },
                        enabled = onShareWhatsApp != null && !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (isEnglish) "Share on WhatsApp" else "व्हाट्सएप शेयर", color = MandiTextPrimary)
                    }
                    // Physical Cash Actions (Save & Reset)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (countedRs != null) showCloseConfirmation = true
                            },
                            enabled = countedRs != null && !uiState.isSaving,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MandiBtnSuccessBg,
                                contentColor = MandiBtnSuccessFg
                            )
                        ) {
                            Text(
                                text = if (isEnglish) "Close day" else "दिन बंद करें",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        if (uiState.physicalCountPaisa != null) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.setPhysicalCashCount(null)
                                    physicalCashInput = ""
                                    viewModel.closeDayClosingSummary()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (isEnglish) "Reset" else "हटाएं",
                                    color = MandiBtnDangerBg,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Soundbox Speech Action
                    OutlinedButton(
                        onClick = { viewModel.announceDayClosingSummary() },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isEnglish) "Announce on Soundbox" else "साउंडबॉक्स पर सुनें", color = MandiTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { countedRs?.let { viewModel.setPhysicalCashCount(it, physicalCashReason) } },
                    enabled = countedRs != null && !uiState.isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MandiPrimaryAction,
                        contentColor = MandiPrimaryActionText
                    )
                ) {
                    Column {
                        Text(if (isEnglish) "Save count · keep day open" else "गिनती सहेजें · दिन खुला रखें", color = MandiPrimaryActionText, fontWeight = FontWeight.Bold)
                        countedRs?.let {
                            Text(MandiCurrencyFormatter.formatRupees(it * 100), color = MandiPrimaryActionText)
                        }
                    }
                }
            },
            dismissButton = {
                IconButton(onClick = { viewModel.closeDayClosingSummary() }) {
                    Icon(Icons.Default.Close, contentDescription = if (isEnglish) "Close" else "बंद करें", tint = MandiTextSecondary)
                }
            }
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
                        value = openingBalanceInput,
                        onValueChange = { openingBalanceInput = it },
                        label = { Text(if (isEnglish) "Opening Cash Float ₹" else "प्रारंभिक रोकड़ शेष ₹") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
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
                        value = cashHomeInput,
                        onValueChange = { cashHomeInput = it },
                        label = { Text(if (isEnglish) "Amount ₹" else "निकासी राशि ₹") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
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
            fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = MandiTextPrimary,
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
                fontWeight = FontWeight.Bold, color = MandiTextPrimary)
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

