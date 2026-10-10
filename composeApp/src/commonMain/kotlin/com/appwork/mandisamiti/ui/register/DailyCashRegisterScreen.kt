package com.appwork.mandisamiti.ui.register

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
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
import com.appwork.mandisamiti.ui.theme.MandiGreenPayable
import com.appwork.mandisamiti.ui.theme.MandiNavy
import com.appwork.mandisamiti.ui.theme.MandiNeutralLight
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiRedLight
import com.appwork.mandisamiti.ui.theme.MandiRedReceivable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

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
    var physicalCashInput by remember { mutableStateOf("") }

    Scaffold(
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
        containerColor = MandiBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Executive Cash In Hand Hero Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MandiSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MandiBorder)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (isEnglish) "Cash In Hand (Total)" else "गल्ले में कुल नकदी (Cash In Hand)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MandiTextSecondary
                            )
                            Text(
                                text = "₹${MandiMathEngine.paisaToRupeesString(uiState.inHandCashDrawerPaisa)}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiNavy
                            )
                        }

                        // Day Closing CTA Pill Button
                        Button(
                            onClick = { viewModel.openDayClosingSummary() },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MandiAmberLight,
                                contentColor = MandiAmberDark
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = MandiAmberDark,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.size(5.dp))
                            Text(
                                text = if (isEnglish) "Day Closing" else "दैनिक रोज़नामा",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiAmberDark
                            )
                        }
                    }

                    CashBreakdownItem(
                        label = if (isEnglish) "Opening Balance" else "पिछला शेष (Opening)",
                        amountPaisa = uiState.openingCashPaisa,
                        color = MandiNavy,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2-Way Metrics Breakdown (In vs Out)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CashBreakdownItem(
                            label = if (isEnglish) "Total Cash In" else "कुल जमा (In)",
                            amountPaisa = uiState.todayCashInPaisa,
                            color = MandiGreenPayable,
                            modifier = Modifier.weight(1f)
                        )
                        CashBreakdownItem(
                            label = if (isEnglish) "Total Cash Out" else "कुल निकासी (Out)",
                            amountPaisa = uiState.todayCashOutPaisa,
                            color = MandiRedReceivable,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick Direct Cash Entry Actions (No emojis, vector icons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showDirectCashDialog = TransactionType.JAMA_RECEIVED },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
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
                        text = if (isEnglish) "Cash In" else "नकद जमा",
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
                        .height(48.dp),
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
                        text = if (isEnglish) "Cash Out" else "नकद निकासी",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiBtnDangerFg,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Daily Transactions Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEnglish) "Today's Cash Entries (${uiState.todayTransactions.size})" else "आज का रोकड़ लेन-देन (${uiState.todayTransactions.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                )
                Text(
                    text = if (isEnglish) "Cash Entries" else "रोकड़ प्रविष्टियाँ",
                    fontSize = 12.sp,
                    color = MandiTextMuted
                )
            }

            // Daily Transactions List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.todayTransactions, key = { it.transaction.id }) { item ->
                    val isDeposit = item.transaction.transactionType == TransactionType.JAMA_RECEIVED
                    val remarks = item.transaction.remarks
                    val isVoid = item.transaction.isVoid
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MandiSurface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MandiBorder)
                        )
                    ) {
                      Column(
                          modifier = Modifier.padding(12.dp),
                          verticalArrangement = Arrangement.spacedBy(6.dp)
                      ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isVoid) 0.55f else 1f),
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
                                        tint = if (isDeposit) MandiGreenPayable else MandiRedReceivable,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.partyName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MandiTextPrimary,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        // Non-cash rows do not move the drawer; the tag says why.
                                        val modeTag = when (item.transaction.paymentMode) {
                                            PaymentMode.CASH -> null
                                            PaymentMode.UPI -> "UPI"
                                            PaymentMode.BANK -> if (isEnglish) "Bank" else "बैंक"
                                            PaymentMode.BOOK_ENTRY -> if (isEnglish) "Khata" else "बही"
                                        }
                                        if (modeTag != null) {
                                            Text(text = modeTag, fontSize = 12.sp, color = MandiTextSecondary)
                                        }
                                    }
                                    Text(
                                        text = if (!remarks.isNullOrBlank()) remarks else if (isDeposit) (if (isEnglish) "Cash Deposit" else "नकद जमा") else (if (isEnglish) "Cash Expense" else "नकद निकासी"),
                                        fontSize = 12.sp,
                                        color = MandiTextSecondary
                                    )
                                }
                            }

                            Text(
                                text = "${if (isDeposit) "+" else "-"}₹${MandiMathEngine.paisaToRupeesString(item.transaction.amountPaisa)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDeposit) MandiGreenPayable else MandiRedReceivable,
                                textDecoration = if (isVoid) TextDecoration.LineThrough else null
                            )
                        }

                        if (isVoid) {
                            val voidReasonText = if (isEnglish) {
                                item.transaction.voidReason?.name ?: "VOID"
                            } else {
                                item.transaction.voidReason?.labelHi ?: ""
                            }
                            Text(
                                text = (if (isEnglish) "Voided" else "रद्द") + if (voidReasonText.isNotBlank()) " · $voidReasonText" else "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MandiTextSecondary
                            )
                        }
                      }
                    }
                }

                if (uiState.todayTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isEnglish) "No cash entries recorded today" else "आज कोई रोकड़ प्रविष्टि नहीं है",
                                color = MandiTextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Day Closing Summary & Reconciliation Modal
    if (uiState.isDayClosingSummaryOpen) {
        val expectedInHand = uiState.inHandCashDrawerPaisa
        val countedRs = physicalCashInput.toLongOrNull()
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            Text("+₹${MandiMathEngine.paisaToRupeesString(uiState.todayCashInPaisa)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MandiGreenPayable)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isEnglish) "Total Cash Out:" else "कुल नकद निकासी (Out):", fontSize = 13.sp, color = MandiTextSecondary)
                            Text("-₹${MandiMathEngine.paisaToRupeesString(uiState.todayCashOutPaisa)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MandiRedReceivable)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isEnglish) "Expected Cash In Hand:" else "गल्ले में शुद्ध नकदी (Expected):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MandiTextPrimary)
                            Text("₹${MandiMathEngine.paisaToRupeesString(expectedInHand)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MandiNavy)
                        }
                    }

                    // Physical Cash Verification Input
                    OutlinedTextField(
                        value = physicalCashInput,
                        onValueChange = { physicalCashInput = it },
                        label = { Text(if (isEnglish) "Physical Cash Count ₹ (Optional)" else "वास्तविक गल्ला गिनती ₹ (वैकल्पिक)") },
                        placeholder = { Text(if (isEnglish) "Enter counted cash in drawer" else "गल्ले में गिने हुए रुपये दर्ज करें") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Difference / Discrepancy Indicator
                    if (diffPaisa != null) {
                        val diffColor = when {
                            diffPaisa == 0L -> MandiGreenPayable
                            diffPaisa > 0L -> MandiAmberDark
                            else -> MandiRedReceivable
                        }
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
                    onClick = {
                        val reportText = viewModel.generateDayClosingReportText()
                        if (onShareWhatsApp != null) {
                            onShareWhatsApp(reportText)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MandiPrimaryAction,
                        contentColor = MandiPrimaryActionText
                    )
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = MandiPrimaryActionText, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(if (isEnglish) "Share on WhatsApp" else "व्हाट्सएप शेयर", color = MandiPrimaryActionText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDayClosingSummary() }) {
                    Text(if (isEnglish) "Close" else "बंद करें", color = MandiTextSecondary)
                }
            }
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
                                paymentMode = PaymentMode.CASH,
                                remarks = directRemarksInput.ifBlank { null }
                            )
                            showDirectCashDialog = null
                            directAmountInput = ""
                            directRemarksInput = ""
                            selectedPartyId = ""
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
                TextButton(onClick = { showDirectCashDialog = null; selectedPartyId = "" }) {
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
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MandiNeutralLight)
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MandiTextSecondary
            )
            Text(
                text = "₹${MandiMathEngine.paisaToRupeesString(amountPaisa)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
