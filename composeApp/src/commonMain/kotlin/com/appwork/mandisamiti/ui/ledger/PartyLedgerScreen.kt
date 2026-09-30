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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.ui.components.SoundboxTopBar
import com.appwork.mandisamiti.ui.theme.MandiAccent
import com.appwork.mandisamiti.ui.theme.MandiAmberPrimary
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiAmberLight
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenLight
import com.appwork.mandisamiti.ui.theme.MandiGreenPayable
import com.appwork.mandisamiti.ui.theme.MandiGreenText
import com.appwork.mandisamiti.ui.theme.MandiNavy
import com.appwork.mandisamiti.ui.theme.MandiNeutralLight
import com.appwork.mandisamiti.ui.theme.MandiNeutralText
import com.appwork.mandisamiti.ui.theme.MandiRedBorder
import com.appwork.mandisamiti.ui.theme.MandiRedLight
import com.appwork.mandisamiti.ui.theme.MandiRedReceivable
import com.appwork.mandisamiti.ui.theme.MandiRedText
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

@Composable
fun PartyLedgerScreen(
    viewModel: PartyLedgerViewModel,
    onNavigateBack: () -> Unit,
    onShareWhatsAppReceipt: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val party = uiState.party

    var showCashEntryType by remember { mutableStateOf<TransactionType?>(null) }
    var cashAmountInput by remember { mutableStateOf("") }
    var cashRemarksInput by remember { mutableStateOf("") }

    var interestRateInput by remember { mutableStateOf("1.5") }
    var interestDaysInput by remember { mutableStateOf("30") }

    Scaffold(
        topBar = {
            SoundboxTopBar(
                shopName = party?.name ?: "खाता विवरण",
                mandiLocation = if (party?.village != null) "गांव: ${party.village}" else "मंडी खाता",
                isSoundEnabled = uiState.isSoundEnabled,
                onToggleSound = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "पीछे जाएं",
                            tint = Color.White
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
                            text = "खाता शुद्ध शेष (Balance)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MandiTextSecondary
                        )
                        Text(
                            text = "₹${MandiMathEngine.paisaToRupeesString(balanceAmount)}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isReceivable -> MandiRedReceivable
                                isPayable -> MandiGreenPayable
                                else -> MandiTextPrimary
                            }
                        )
                    }

                    // Semantic Status Chip
                    val statusText = when {
                        isReceivable -> "लेना है"
                        isPayable -> "देना है"
                        else -> "हिसाब चुकता"
                    }
                    val statusColor = when {
                        isReceivable -> MandiRedText
                        isPayable -> MandiGreenText
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

            // Quick Action Buttons (No emojis, vector icons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FintechActionButton(
                    label = "जमा मिला",
                    icon = Icons.Default.ArrowDownward,
                    color = MandiGreenPayable,
                    onClick = { showCashEntryType = TransactionType.JAMA_RECEIVED },
                    modifier = Modifier.weight(1f)
                )
                FintechActionButton(
                    label = "भुगतान दिया",
                    icon = Icons.Default.ArrowUpward,
                    color = MandiRedReceivable,
                    onClick = { showCashEntryType = TransactionType.UDHAR_GIVEN },
                    modifier = Modifier.weight(1f)
                )
                FintechActionButton(
                    label = "ब्याज हिसाब",
                    icon = Icons.Default.Calculate,
                    color = MandiAmberDark,
                    onClick = { viewModel.openInterestDialog() },
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
                    text = "लेन-देन विवरण (${uiState.ledgerItems.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                )
                Text(
                    text = "तारीख अनुसार",
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
                        is LedgerItem.DealItem -> {
                            FintechDealCard(
                                deal = item.deal,
                                onShareSlip = { onShareWhatsAppReceipt(item.deal.id) }
                            )
                        }
                        is LedgerItem.CashItem -> {
                            FintechCashCard(transaction = item.transaction)
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
                                text = "इस खाते में कोई लेन-देन नहीं है",
                                color = MandiTextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Cash Entry Dialog (Clean Fintech Modal)
    if (showCashEntryType != null) {
        val txnType = showCashEntryType!!
        val isDeposit = txnType == TransactionType.JAMA_RECEIVED
        AlertDialog(
            onDismissRequest = { showCashEntryType = null },
            title = {
                Text(
                    text = if (isDeposit) "नकद जमा प्राप्त दर्ज करें" else "नकद भुगतान दिया दर्ज करें",
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
                        label = { Text("राशि (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cashRemarksInput,
                        onValueChange = { cashRemarksInput = it },
                        label = { Text("विवरण / नोट (वैकल्पिक)") },
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
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDeposit) MandiGreenPayable else MandiRedReceivable
                    )
                ) {
                    Text("सुरक्षित करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCashEntryType = null }) {
                    Text("रद्द करें")
                }
            }
        )
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
                    text = "देसी ब्याज हिसाब (सैकड़ा दर)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MandiTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "मूलधन: ₹${MandiMathEngine.paisaToRupeesString(principal)}",
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
                            label = { Text("दर (% प्रति माह)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = interestDaysInput,
                            onValueChange = { interestDaysInput = it },
                            label = { Text("दिन") },
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
                            Text("ब्याज राशि:", fontSize = 13.sp, color = MandiTextSecondary)
                            Text("₹${MandiMathEngine.paisaToRupeesString(interestResult.accruedInterestPaisa)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MandiRedReceivable)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("कुल देय:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MandiTextPrimary)
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
                    colors = ButtonDefaults.buttonColors(containerColor = MandiAmberPrimary)
                ) {
                    Text("ब्याज खाते में जोड़ें")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeInterestDialog() }) {
                    Text("बंद करें")
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
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(MandiSurface)
            .border(1.dp, MandiBorder, shape)
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
            color = MandiTextPrimary
        )
    }
}

@Composable
private fun FintechDealCard(
    deal: com.appwork.mandisamiti.domain.model.Deal,
    onShareSlip: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MandiSurface)
            .border(1.dp, MandiBorder, shape)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MandiAmberPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "सौदा पर्ची #${deal.id.takeLast(5)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                }

                IconButton(
                    onClick = onShareSlip,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "पर्ची शेयर करें",
                        tint = MandiAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${deal.bagsCount} बोरी • ${MandiMathEngine.gramsToQuintals(deal.netWeightGrams)} Qtl",
                    fontSize = 13.sp,
                    color = MandiTextSecondary
                )
                Text(
                    text = "₹${MandiMathEngine.paisaToRupeesString(deal.netFarmerPayablePaisa)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiAmberDark
                )
            }
        }
    }
}

@Composable
private fun FintechCashCard(
    transaction: com.appwork.mandisamiti.domain.model.CashTransaction
) {
    val isDeposit = transaction.transactionType == TransactionType.JAMA_RECEIVED
    val shape = RoundedCornerShape(10.dp)
    val remarks = transaction.remarks

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MandiSurface)
            .border(1.dp, MandiBorder, shape)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isDeposit) "नकद जमा प्राप्त" else "नकद भुगतान दिया",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
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
                color = if (isDeposit) MandiGreenPayable else MandiRedReceivable
            )
        }
    }
}
