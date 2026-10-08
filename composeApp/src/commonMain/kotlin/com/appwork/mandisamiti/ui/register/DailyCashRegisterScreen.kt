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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
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
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.ui.components.SoundboxTopBar
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
import com.appwork.mandisamiti.ui.theme.MandiRedLight
import com.appwork.mandisamiti.ui.theme.MandiRedReceivable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

@Composable
fun DailyCashRegisterScreen(
    viewModel: DailyRegisterViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDirectCashDialog by remember { mutableStateOf<TransactionType?>(null) }
    var selectedPartyId by remember { mutableStateOf("") }
    var directAmountInput by remember { mutableStateOf("") }
    var directRemarksInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            SoundboxTopBar(
                shopName = "दैनिक गल्ला रोकड़ बही",
                mandiLocation = "दुकान: ${uiState.shopProfile?.shopName ?: "मंडी रोकड़"}",
                isSoundEnabled = uiState.isSoundEnabled,
                onToggleSound = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "पीछे जाएं",
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
                    Text(
                        text = "गल्ले में कुल नकदी (Cash In Hand)",
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

                    // 2-Way Metrics Breakdown (In vs Out)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CashBreakdownItem(
                            label = "कुल जमा (In)",
                            amountPaisa = uiState.todayCashInPaisa,
                            color = MandiGreenPayable,
                            modifier = Modifier.weight(1f)
                        )
                        CashBreakdownItem(
                            label = "कुल निकासी (Out)",
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
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MandiBtnSuccessFg,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "नकद आवक (जमा)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiBtnSuccessFg
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
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = null,
                        tint = MandiBtnDangerFg,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "नकद निकासी (खर्च)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiBtnDangerFg
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
                    text = "आज का रोकड़ लेन-देन (${uiState.todayTransactions.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                )
                Text(
                    text = "रोकड़ प्रविष्टियाँ",
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MandiSurface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MandiBorder)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
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
                                        text = item.partyName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiTextPrimary
                                    )
                                    Text(
                                        text = if (!remarks.isNullOrBlank()) remarks else if (isDeposit) "नकद जमा" else "नकद निकासी",
                                        fontSize = 12.sp,
                                        color = MandiTextSecondary
                                    )
                                }
                            }

                            Text(
                                text = "${if (isDeposit) "+" else "-"}₹${MandiMathEngine.paisaToRupeesString(item.transaction.amountPaisa)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDeposit) MandiGreenPayable else MandiRedReceivable
                            )
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
                                text = "आज कोई रोकड़ प्रविष्टि नहीं है",
                                color = MandiTextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Direct Cash Dialog
    if (showDirectCashDialog != null) {
        val isDeposit = showDirectCashDialog == TransactionType.JAMA_RECEIVED
        AlertDialog(
            onDismissRequest = { showDirectCashDialog = null },
            title = {
                Text(
                    text = if (isDeposit) "नकद जमा प्रविष्टि" else "नकद निकासी प्रविष्टि",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MandiTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = directAmountInput,
                        onValueChange = { directAmountInput = it },
                        label = { Text("राशि (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = directRemarksInput,
                        onValueChange = { directRemarksInput = it },
                        label = { Text("विवरण / नोट (वैकल्पिक)") },
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
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDeposit) MandiBtnSuccessBg else MandiBtnDangerBg,
                        contentColor = Color.White
                    )
                ) {
                    Text("सुरक्षित करें", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectCashDialog = null }) {
                    Text("रद्द करें", color = MandiTextSecondary)
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
