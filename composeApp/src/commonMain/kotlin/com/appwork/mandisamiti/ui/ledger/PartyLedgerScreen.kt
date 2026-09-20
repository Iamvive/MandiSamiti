package com.appwork.mandisamiti.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.ui.components.SoundboxTopBar
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiGold
import com.appwork.mandisamiti.ui.theme.MandiGreenLight
import com.appwork.mandisamiti.ui.theme.MandiGreenPayable
import com.appwork.mandisamiti.ui.theme.MandiNavy
import com.appwork.mandisamiti.ui.theme.MandiRedLight
import com.appwork.mandisamiti.ui.theme.MandiRedReceivable
import com.appwork.mandisamiti.ui.theme.MandiSlate
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
                shopName = party?.name ?: "खाता विवरण (Khata)",
                mandiLocation = if (party?.village != null) "गांव: ${party.village}" else "मंडी खाता",
                isSoundEnabled = uiState.isSoundEnabled,
                onToggleSound = {}
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
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isReceivable -> MandiRedLight
                        isPayable -> MandiGreenLight
                        else -> MandiSurface
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when {
                            isReceivable -> "🔴 कुल बाकी रकम (लेना है)"
                            isPayable -> "🟢 कुल जमा रकम (देना है)"
                            else -> "⚪ हिसाब चुकता (शून्य बकाया)"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isReceivable -> MandiRedReceivable
                            isPayable -> MandiGreenPayable
                            else -> MandiTextSecondary
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "₹ ${MandiMathEngine.paisaToRupeesString(balanceAmount)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isReceivable -> MandiRedReceivable
                            isPayable -> MandiGreenPayable
                            else -> MandiNavy
                        }
                    )
                }
            }

            // Quick Action Grid (Large 56dp+ touch targets)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showCashEntryType = TransactionType.UDHAR_GIVEN },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MandiRedReceivable)
                ) {
                    Text("🔴 उधार दिया", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { showCashEntryType = TransactionType.JAMA_RECEIVED },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MandiGreenPayable)
                ) {
                    Text("🟢 जमा मिला", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.openInterestDialog() },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MandiGold)
                ) {
                    Text("➕ देसी ब्याज", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { showCashEntryType = TransactionType.DISCOUNT_GIVEN },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MandiSlate)
                ) {
                    Text("🤝 छूट / समझौता", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Khata Timeline Title
            Text(
                "📜 लेनदेन व सौदा बहीखाता (Timeline)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Feed of transactions and crop deals
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.ledgerItems) { item ->
                    when (item) {
                        is LedgerItem.DealItem -> {
                            val deal = item.deal
                            val isFarmer = deal.farmerId == party?.id
                            val amount = if (isFarmer) deal.netFarmerPayablePaisa else deal.netBuyerReceivablePaisa
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MandiSurface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("🌾 फसल सौदा • ${deal.bagsCount} बोरी", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MandiNavy)
                                        Text("${MandiMathEngine.gramsToQuintals(deal.netWeightGrams)} कुंतल", fontSize = 13.sp, color = MandiTextSecondary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "₹ ${MandiMathEngine.paisaToRupeesString(amount)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (isFarmer) MandiGreenPayable else MandiRedReceivable
                                        )
                                        Text(if (isFarmer) "जमा (देना है)" else "बकाया (लेना है)", fontSize = 11.sp, color = MandiTextMuted)
                                    }
                                }
                            }
                        }
                        is LedgerItem.CashItem -> {
                            val tx = item.transaction
                            val isNegative = tx.transactionType == TransactionType.JAMA_RECEIVED || tx.transactionType == TransactionType.DISCOUNT_GIVEN
                            val typeLabel = when (tx.transactionType) {
                                TransactionType.UDHAR_GIVEN -> "🔴 नकद उधार दिया"
                                TransactionType.JAMA_RECEIVED -> "🟢 नकद जमा मिला"
                                TransactionType.INTEREST_ADDED -> "➕ ब्याज जोड़ा गया"
                                TransactionType.DISCOUNT_GIVEN -> "🤝 छूट / समझौता"
                            }
                            val remarksText = tx.remarks
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MandiSurface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(typeLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MandiNavy)
                                        if (remarksText != null && remarksText.isNotEmpty()) {
                                            Text(remarksText, fontSize = 12.sp, color = MandiTextSecondary)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "₹ ${MandiMathEngine.paisaToRupeesString(tx.amountPaisa)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (isNegative) MandiGreenPayable else MandiRedReceivable
                                        )
                                        Text(tx.paymentMode.name, fontSize = 11.sp, color = MandiTextMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Direct Cash Entry Dialog
    if (showCashEntryType != null) {
        val type = showCashEntryType!!
        val title = when (type) {
            TransactionType.UDHAR_GIVEN -> "🔴 उधार प्रविष्टि (Cash Advance)"
            TransactionType.JAMA_RECEIVED -> "🟢 जमा प्रविष्टि (Cash Received)"
            TransactionType.DISCOUNT_GIVEN -> "🤝 छूट / समझौता दर्ज करें"
            TransactionType.INTEREST_ADDED -> "➕ ब्याज प्रविष्टि"
        }
        AlertDialog(
            onDismissRequest = { showCashEntryType = null },
            title = { Text(title, fontWeight = FontWeight.Bold, color = MandiNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("रकम दर्ज करें (₹):", fontSize = 14.sp, color = MandiTextSecondary)
                    OutlinedTextField(
                        value = cashAmountInput,
                        onValueChange = { cashAmountInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("उदा. 5000") }
                    )
                    Text("विवरण / टिप्पणी:", fontSize = 14.sp, color = MandiTextSecondary)
                    OutlinedTextField(
                        value = cashRemarksInput,
                        onValueChange = { cashRemarksInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("उदा. खाद/बीज हेतु नकद") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = cashAmountInput.toLongOrNull() ?: 0L
                        if (amt > 0L) {
                            viewModel.recordCashEntry(type, amt, remarks = cashRemarksInput)
                            showCashEntryType = null
                            cashAmountInput = ""
                            cashRemarksInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MandiNavy)
                ) {
                    Text("💾 दर्ज करें [ 🔊 ]", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCashEntryType = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Rural Simple Interest Dialog
    if (uiState.isInterestDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.closeInterestDialog() },
            title = { Text("➕ देसी ब्याज हिसाब (सैकड़ा दर)", fontWeight = FontWeight.Bold, color = MandiNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val principal = (uiState.balancePaisa.toDouble() / 100.0).toLong().coerceAtLeast(0L)
                    Text("मूलधन (बाकी रकम): ₹ $principal", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MandiRedReceivable)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("मासिक दर (%):", fontSize = 12.sp, color = MandiTextSecondary)
                            OutlinedTextField(
                                value = interestRateInput,
                                onValueChange = { interestRateInput = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("दिन:", fontSize = 12.sp, color = MandiTextSecondary)
                            OutlinedTextField(
                                value = interestDaysInput,
                                onValueChange = { interestDaysInput = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    val rate = interestRateInput.toDoubleOrNull() ?: 1.5
                    val days = interestDaysInput.toIntOrNull() ?: 30
                    val calculated = RuralInterestEngine.calculateAccruedInterestByDays(
                        principalPaisa = uiState.balancePaisa.coerceAtLeast(0L),
                        monthlyRatePercent = rate,
                        elapsedDays = days
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MandiGold.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("कुल ब्याज: ₹ ${MandiMathEngine.paisaToRupeesString(calculated.accruedInterestPaisa)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MandiGold)
                            Text("कुल देय: ₹ ${MandiMathEngine.paisaToRupeesString(calculated.totalPayablePaisa)}", fontSize = 13.sp, color = MandiNavy)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rate = interestRateInput.toDoubleOrNull() ?: 1.5
                        val days = interestDaysInput.toIntOrNull() ?: 30
                        val calculated = RuralInterestEngine.calculateAccruedInterestByDays(
                            principalPaisa = uiState.balancePaisa.coerceAtLeast(0L),
                            monthlyRatePercent = rate,
                            elapsedDays = days
                        )
                        viewModel.recordCalculatedInterest(calculated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MandiNavy)
                ) {
                    Text("➕ खाते में जोड़ें [ 🔊 ]", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeInterestDialog() }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}
