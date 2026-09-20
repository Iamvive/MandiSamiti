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
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.ui.components.SoundboxTopBar
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenLight
import com.appwork.mandisamiti.ui.theme.MandiGreenPayable
import com.appwork.mandisamiti.ui.theme.MandiNavy
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
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedParty by remember { mutableStateOf<Party?>(null) }
    var selectedType by remember { mutableStateOf(TransactionType.JAMA_RECEIVED) }
    var amountInput by remember { mutableStateOf("") }
    var remarksInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            SoundboxTopBar(
                shopName = "💼 गल्ला हिसाब (Daily Cash)",
                mandiLocation = "${uiState.shopProfile?.shopName ?: "मथुरा मंडी"} • रोकड़ बही",
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
            // Cash Drawer Summary Hero Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cash In
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MandiGreenLight)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🟢 आवक (In)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MandiGreenPayable)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "₹ ${MandiMathEngine.paisaToRupeesString(uiState.todayCashInPaisa)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiGreenPayable
                        )
                    }
                }

                // Cash Out
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MandiRedLight)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔴 जावक (Out)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MandiRedReceivable)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "₹ ${MandiMathEngine.paisaToRupeesString(uiState.todayCashOutPaisa)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiRedReceivable
                        )
                    }
                }

                // Drawer Balance
                Card(
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MandiNavy)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💼 गल्ला शेष", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "₹ ${MandiMathEngine.paisaToRupeesString(uiState.inHandCashDrawerPaisa)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Quick Add Transaction Button (Large 56dp CTA)
            Button(
                onClick = {
                    selectedParty = uiState.availableParties.firstOrNull()
                    showAddDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 14.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MandiNavy)
            ) {
                Text("➕ नया नकद लेनदेन दर्ज करें [ 🔊 ]", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                "📜 आज का रोकड़ हिसाब (Today's Cash Flow)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Feed of daily cash transactions
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.todayTransactions) { item ->
                    val tx = item.transaction
                    val isIn = tx.transactionType == TransactionType.JAMA_RECEIVED
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
                                Text(item.partyName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MandiNavy)
                                if (item.village != null) {
                                    Text("गांव: ${item.village}", fontSize = 12.sp, color = MandiTextSecondary)
                                }
                                val remarks = tx.remarks
                                if (!remarks.isNullOrEmpty()) {
                                    Text(remarks, fontSize = 12.sp, color = MandiTextMuted)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    (if (isIn) "+ ₹ " else "- ₹ ") + MandiMathEngine.paisaToRupeesString(tx.amountPaisa),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = if (isIn) MandiGreenPayable else MandiRedReceivable
                                )
                                Text(if (isIn) "🟢 जमा मिला" else "🔴 उधार दिया", fontSize = 11.sp, color = MandiTextMuted)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Cash Entry Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("➕ गल्ला हिसाब प्रविष्टि", fontWeight = FontWeight.Bold, color = MandiNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Type selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { selectedType = TransactionType.JAMA_RECEIVED },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedType == TransactionType.JAMA_RECEIVED) MandiGreenPayable else MandiBorder
                            )
                        ) {
                            Text("🟢 जमा मिला", fontSize = 13.sp)
                        }
                        Button(
                            onClick = { selectedType = TransactionType.UDHAR_GIVEN },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedType == TransactionType.UDHAR_GIVEN) MandiRedReceivable else MandiBorder
                            )
                        ) {
                            Text("🔴 उधार दिया", fontSize = 13.sp)
                        }
                    }

                    Text("खाताधारक: ${selectedParty?.name ?: "खाता चुनें"}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                    Text("रकम (₹):", fontSize = 13.sp, color = MandiTextSecondary)
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("उदा. 10000") }
                    )

                    Text("विवरण:", fontSize = 13.sp, color = MandiTextSecondary)
                    OutlinedTextField(
                        value = remarksInput,
                        onValueChange = { remarksInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("उदा. गल्ला रोकड़") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val party = selectedParty ?: uiState.availableParties.firstOrNull()
                        val amt = amountInput.toLongOrNull() ?: 0L
                        if (party != null && amt > 0L) {
                            viewModel.recordDailyEntry(
                                partyId = party.id,
                                transactionType = selectedType,
                                amountRs = amt,
                                remarks = remarksInput
                            )
                            showAddDialog = false
                            amountInput = ""
                            remarksInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MandiNavy)
                ) {
                    Text("💾 दर्ज करें [ 🔊 ]", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}
