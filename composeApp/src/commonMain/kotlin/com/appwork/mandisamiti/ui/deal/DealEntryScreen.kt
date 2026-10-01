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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.appwork.mandisamiti.ui.theme.MandiAccent
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenPayable
import com.appwork.mandisamiti.ui.theme.MandiNavy
import com.appwork.mandisamiti.ui.theme.MandiNeutralLight
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DealEntryScreen(
    viewModel: DealEntryViewModel,
    onNavigateBack: () -> Unit,
    onDealSavedSuccess: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPicker = rememberCameraSlipPicker()

    var showFarmerPicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showBuyerPicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showAddFarmerDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showAddBuyerDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showFarmerPicker) {
        com.appwork.mandisamiti.ui.components.PartySelectionDialog(
            title = "किसान (विक्रेता) चुनें",
            partyType = com.appwork.mandisamiti.domain.model.PartyType.FARMER,
            parties = uiState.availableFarmers,
            selectedParty = uiState.selectedFarmer,
            onSelectParty = { viewModel.onSelectFarmer(it) },
            onAddNewPartyClick = {
                showFarmerPicker = false
                showAddFarmerDialog = true
            },
            onDismiss = { showFarmerPicker = false }
        )
    }

    if (showBuyerPicker) {
        com.appwork.mandisamiti.ui.components.PartySelectionDialog(
            title = "व्यापारी (खरीदार) चुनें",
            partyType = com.appwork.mandisamiti.domain.model.PartyType.BUYER,
            parties = uiState.availableBuyers,
            selectedParty = uiState.selectedBuyer,
            onSelectParty = { viewModel.onSelectBuyer(it) },
            onAddNewPartyClick = {
                showBuyerPicker = false
                showAddBuyerDialog = true
            },
            onDismiss = { showBuyerPicker = false }
        )
    }

    if (showAddFarmerDialog) {
        com.appwork.mandisamiti.ui.components.AddPartyDialog(
            initialPartyType = com.appwork.mandisamiti.domain.model.PartyType.FARMER,
            onDismiss = { showAddFarmerDialog = false },
            onSaveParty = { name, phone, village, partyType, rate ->
                viewModel.addNewParty(name, phone, village, partyType, rate)
                showAddFarmerDialog = false
            }
        )
    }

    if (showAddBuyerDialog) {
        com.appwork.mandisamiti.ui.components.AddPartyDialog(
            initialPartyType = com.appwork.mandisamiti.domain.model.PartyType.BUYER,
            onDismiss = { showAddBuyerDialog = false },
            onSaveParty = { name, phone, village, partyType, rate ->
                viewModel.addNewParty(name, phone, village, partyType, rate)
                showAddBuyerDialog = false
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            if (event is DealEntryEvent.DealSavedSuccess) {
                onDealSavedSuccess(event.deal.id)
            }
        }
    }

    Scaffold(
        topBar = {
            SoundboxTopBar(
                shopName = if (uiState.isEditMode) "सौदा संशोधन" else "नई आवक व सौदा",
                mandiLocation = "मथुरा मंडी • पक्का हिसाब",
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
            // Upper Scrollable Section
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Sleek 2-Stage Breadcrumb Tabs (No Emojis)
                TabRow(
                    selectedTabIndex = if (uiState.isSettledStage) 1 else 0,
                    containerColor = MandiSurface,
                    contentColor = MandiNavy,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MandiBorder, RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = !uiState.isSettledStage,
                        onClick = { viewModel.toggleSettlementStage(false) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Scale,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "स्टेज 1: आवक व तौल",
                                    fontWeight = if (!uiState.isSettledStage) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    )
                    Tab(
                        selected = uiState.isSettledStage,
                        onClick = { viewModel.toggleSettlementStage(true) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Gavel,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "स्टेज 2: नीलामी व भाव",
                                    fontWeight = if (uiState.isSettledStage) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    )
                }

                // 2. Party Selector Cards
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MandiSurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MandiBorder)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "पार्टी विवरण",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )

                        // Farmer Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MandiBackground)
                                .border(1.dp, MandiBorder, RoundedCornerShape(8.dp))
                                .clickable { showFarmerPicker = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "किसान (विक्रेता):",
                                fontSize = 14.sp,
                                color = MandiTextSecondary
                            )
                            Text(
                                text = uiState.selectedFarmer?.let { "${it.name} ▾" } ?: "किसान चुनें ▾",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiNavy
                            )
                        }

                        // Commodity Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "जिंस (फसल):",
                                fontSize = 14.sp,
                                color = MandiTextSecondary
                            )
                            Text(
                                text = uiState.selectedCommodity?.nameHi ?: "गेहूं (Wheat)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiNavy
                            )
                        }

                        if (uiState.isSettledStage) {
                            // Buyer Selector
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MandiBackground)
                                    .border(1.dp, MandiBorder, RoundedCornerShape(8.dp))
                                    .clickable { showBuyerPicker = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "व्यापारी (खरीदार):",
                                    fontSize = 14.sp,
                                    color = MandiTextSecondary
                                )
                                Text(
                                    text = uiState.selectedBuyer?.let { "${it.name} ▾" } ?: "व्यापारी चुनें ▾",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MandiAccent
                                )
                            }
                        }
                    }
                }

                // 3. Stage 1 Inputs (Weighment & Cut)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MandiSurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MandiBorder)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "तौल व बारदाना विवरण",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FintechInputField(
                                label = "बोरी संख्या",
                                value = uiState.bagsCountText,
                                isFocused = uiState.activeField == ActiveInputField.BAGS_COUNT,
                                onClick = { viewModel.onFocusField(ActiveInputField.BAGS_COUNT) },
                                modifier = Modifier.weight(1f)
                            )
                            FintechInputField(
                                label = "सकल वजन (Qtl)",
                                value = uiState.grossWeightText,
                                isFocused = uiState.activeField == ActiveInputField.GROSS_WEIGHT,
                                onClick = { viewModel.onFocusField(ActiveInputField.GROSS_WEIGHT) },
                                modifier = Modifier.weight(1f)
                            )
                            FintechInputField(
                                label = "काट (Qtl)",
                                value = uiState.tareWeightText,
                                isFocused = uiState.activeField == ActiveInputField.TARE_WEIGHT,
                                onClick = { viewModel.onFocusField(ActiveInputField.TARE_WEIGHT) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Calculated Net Weight Banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MandiNeutralLight)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "शुद्ध वजन (Net Weight):",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MandiTextSecondary
                            )
                            Text(
                                text = "${uiState.netWeightQuintals} क्विंटल",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiNavy
                            )
                        }
                    }
                }

                // 4. Stage 2 Pricing & Deductions (If Settled Stage)
                if (uiState.isSettledStage) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MandiSurface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MandiBorder)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "नीलामी भाव व आढ़त कटौतियां",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiTextPrimary
                            )

                            FintechInputField(
                                label = "नीलामी भाव (₹ / क्विंटल)",
                                value = uiState.ratePerQuintalText,
                                isFocused = uiState.activeField == ActiveInputField.RATE_PER_QUINTAL,
                                onClick = { viewModel.onFocusField(ActiveInputField.RATE_PER_QUINTAL) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Itemized Breakdown Table
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MandiBackground)
                                    .border(1.dp, MandiBorder, RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                InvoiceRow(label = "सकल माल मूल्य (Gross)", amountPaisa = uiState.grossAmountPaisa, isBold = false)
                                InvoiceRow(
                                    label = "किसान को शुद्ध देय",
                                    amountPaisa = uiState.netFarmerPayablePaisa,
                                    isBold = true,
                                    color = MandiGreenPayable
                                )
                                InvoiceRow(
                                    label = "व्यापारी से देय वसूली",
                                    amountPaisa = uiState.netBuyerReceivablePaisa,
                                    isBold = false,
                                    color = MandiNavy
                                )
                            }
                        }
                    }
                }

                // 5. Slip Camera Attachment Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MandiSurface)
                        .border(1.dp, MandiBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            cameraPicker.launchCamera { uri ->
                                if (uri != null) viewModel.onReceiptPhotoCaptured(uri)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MandiTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (uiState.receiptPhotoUri != null) "कांटा पर्ची संलग्न (बदलें)" else "कांटा पर्ची फोटो संलग्न करें",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MandiTextSecondary
                    )
                }

                // 6. Action Save Button
                Button(
                    onClick = { viewModel.saveDeal() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isSettledStage) MandiNavy else MandiGreenPayable
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (uiState.isSettledStage) "पक्का सौदा दर्ज करें" else "तौल पर्ची सेव करें (भाव बाद में)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Lower Section: Integrated Mandi Keypad
            MandiCalculatorKeypad(
                onKeyPressed = { action ->
                    viewModel.onKeypadAction(action)
                },
                showSubmitInsteadOfNext = uiState.isSettledStage && uiState.activeField == ActiveInputField.RATE_PER_QUINTAL
            )
        }
    }
}

@Composable
private fun FintechInputField(
    label: String,
    value: String,
    isFocused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = modifier.clickable { onClick() },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isFocused) MandiNavy else MandiTextSecondary
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(shape)
                .background(if (isFocused) MandiNeutralLight else MandiSurface)
                .border(1.5.dp, if (isFocused) MandiNavy else MandiBorder, shape)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = if (value.isBlank()) "-" else value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (value.isBlank()) MandiTextMuted else MandiTextPrimary
            )
        }
    }
}

@Composable
private fun InvoiceRow(
    label: String,
    amountPaisa: Long,
    isBold: Boolean,
    color: Color = MandiTextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) MandiTextPrimary else MandiTextSecondary
        )
        val formatted = if (amountPaisa < 0) "-₹${MandiMathEngine.paisaToRupeesString(-amountPaisa)}" else "₹${MandiMathEngine.paisaToRupeesString(amountPaisa)}"
        Text(
            text = formatted,
            fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
    }
}
