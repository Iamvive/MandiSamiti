package com.appwork.mandisamiti.ui.deal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import com.appwork.mandisamiti.platform.rememberCameraSlipPicker
import com.appwork.mandisamiti.ui.components.MandiCalculatorKeypad
import com.appwork.mandisamiti.ui.components.SoundboxTopBar
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiGold
import com.appwork.mandisamiti.ui.theme.MandiGreenPayable
import com.appwork.mandisamiti.ui.theme.MandiNavy
import com.appwork.mandisamiti.ui.theme.MandiRedReceivable
import com.appwork.mandisamiti.ui.theme.MandiSlate
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

@Composable
fun DealEntryScreen(
    viewModel: DealEntryViewModel,
    onNavigateBack: () -> Unit,
    onDealSavedSuccess: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPicker = rememberCameraSlipPicker()

    Scaffold(
        topBar = {
            SoundboxTopBar(
                shopName = if (uiState.isEditMode) "✏️ सौदा संशोधन" else "🌾 नई आवक व सौदा",
                mandiLocation = "मथुरा मंडी • पक्का हिसाब",
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
            // Scrollable upper content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // Stage Tabs
                TabRow(
                    selectedTabIndex = if (uiState.isSettledStage) 1 else 0,
                    containerColor = MandiSurface,
                    contentColor = MandiNavy
                ) {
                    Tab(
                        selected = !uiState.isSettledStage,
                        onClick = { viewModel.toggleSettlementStage(false) },
                        text = {
                            Text(
                                "⚖️ स्टेज 1: आवक / तौल",
                                fontWeight = if (!uiState.isSettledStage) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                    Tab(
                        selected = uiState.isSettledStage,
                        onClick = { viewModel.toggleSettlementStage(true) },
                        text = {
                            Text(
                                "💰 स्टेज 2: भाव व पक्का हिसाब",
                                fontWeight = if (uiState.isSettledStage) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Farmer Selection Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MandiSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "👨‍🌾 किसान (विक्रेता)",
                                fontSize = 13.sp,
                                color = MandiTextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                uiState.selectedFarmer?.name ?: "किसान चुनें",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiTextPrimary
                            )
                            if (uiState.selectedFarmer?.village != null) {
                                Text(
                                    "गांव: ${uiState.selectedFarmer?.village}",
                                    fontSize = 12.sp,
                                    color = MandiNavy
                                )
                            }
                        }
                    }
                }

                // If Stage 2, Buyer Selection Card
                if (uiState.isSettledStage) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MandiSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "🏭 खरीदार / व्यापारी (Buyer)",
                                    fontSize = 13.sp,
                                    color = MandiTextMuted
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    uiState.selectedBuyer?.name ?: "व्यापारी चुनें",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MandiTextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pinned Data Input Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Bags Count
                    InputFieldCard(
                        modifier = Modifier.weight(1f),
                        label = "📦 बोरी (Bags)",
                        value = if (uiState.bagsCountText.isEmpty()) "0" else uiState.bagsCountText,
                        isActive = uiState.activeField == ActiveInputField.BAGS_COUNT,
                        onClick = { viewModel.onFocusField(ActiveInputField.BAGS_COUNT) }
                    )
                    // Gross Weight
                    InputFieldCard(
                        modifier = Modifier.weight(1.5f),
                        label = "⚖️ कुल वजन (कुंतल)",
                        value = if (uiState.grossWeightText.isEmpty()) "0.00" else uiState.grossWeightText,
                        isActive = uiState.activeField == ActiveInputField.GROSS_WEIGHT,
                        onClick = { viewModel.onFocusField(ActiveInputField.GROSS_WEIGHT) }
                    )
                    // Tare Weight
                    InputFieldCard(
                        modifier = Modifier.weight(1f),
                        label = "कटौती (कुंतल)",
                        value = if (uiState.tareWeightText.isEmpty()) "0.00" else uiState.tareWeightText,
                        isActive = uiState.activeField == ActiveInputField.TARE_WEIGHT,
                        onClick = { viewModel.onFocusField(ActiveInputField.TARE_WEIGHT) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Rate and Deductions (Stage 2)
                if (uiState.isSettledStage) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InputFieldCard(
                            modifier = Modifier.weight(1.3f),
                            label = "₹ भाव / कुंतल",
                            value = if (uiState.ratePerQuintalText.isEmpty()) "₹ 0" else "₹ ${uiState.ratePerQuintalText}",
                            isActive = uiState.activeField == ActiveInputField.RATE_PER_QUINTAL,
                            onClick = { viewModel.onFocusField(ActiveInputField.RATE_PER_QUINTAL) }
                        )
                        InputFieldCard(
                            modifier = Modifier.weight(1f),
                            label = "पल्लेदारी (₹)",
                            value = if (uiState.labourChargesText.isEmpty()) "0" else "₹ ${uiState.labourChargesText}",
                            isActive = uiState.activeField == ActiveInputField.LABOUR_CHARGES,
                            onClick = { viewModel.onFocusField(ActiveInputField.LABOUR_CHARGES) }
                        )
                        InputFieldCard(
                            modifier = Modifier.weight(0.9f),
                            label = "आढ़त %",
                            value = "${uiState.commissionPercentText}%",
                            isActive = uiState.activeField == ActiveInputField.COMMISSION_PERCENT,
                            onClick = { viewModel.onFocusField(ActiveInputField.COMMISSION_PERCENT) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Calculation Summary Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MandiSlate),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("शुद्ध वजन (Net Weight):", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                            Text("${uiState.netWeightQuintals} कुंतल", color = MandiGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        if (uiState.isSettledStage && uiState.netFarmerPayablePaisa > 0L) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🟢 किसान को भुगतान:", color = MandiGreenPayable, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("₹ ${MandiMathEngine.paisaToRupeesString(uiState.netFarmerPayablePaisa)}", color = MandiGreenPayable, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🔴 व्यापारी से वसूली:", color = MandiRedReceivable, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("₹ ${MandiMathEngine.paisaToRupeesString(uiState.netBuyerReceivablePaisa)}", color = MandiRedReceivable, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Camera slip attachment button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            cameraPicker.launchCamera { uri ->
                                if (uri != null) viewModel.onReceiptPhotoCaptured(uri)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MandiSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MandiBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            if (uiState.receiptPhotoUri != null) "📷 पर्ची फोटो संलग्न ✅" else "📷 कांटा पर्ची फोटो खींचें",
                            color = MandiNavy,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom Calculator Keypad & Save Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MandiSurface)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                MandiCalculatorKeypad(
                    onKeyPressed = { action -> viewModel.onKeypadAction(action) },
                    showSubmitInsteadOfNext = uiState.isSettledStage
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.saveDeal() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MandiNavy)
                ) {
                    Text(
                        if (uiState.isSettledStage) "💾 सौदा पक्का करें [ 🔊 साउंडबॉक्स ]" else "💾 आवक तौल पर्ची दर्ज करें",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun InputFieldCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isActive) 2.5.dp else 1.dp,
                color = if (isActive) MandiNavy else MandiBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MandiNavy.copy(alpha = 0.06f) else MandiSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, color = MandiTextMuted, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) MandiNavy else MandiTextPrimary,
                maxLines = 1
            )
        }
    }
}
