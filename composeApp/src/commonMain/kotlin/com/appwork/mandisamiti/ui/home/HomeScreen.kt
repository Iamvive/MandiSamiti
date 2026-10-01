package com.appwork.mandisamiti.ui.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.ui.components.PartyCard
import com.appwork.mandisamiti.ui.components.SoundboxTopBar
import com.appwork.mandisamiti.ui.theme.MandiAccent
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenLight
import com.appwork.mandisamiti.ui.theme.MandiGreenPayable
import com.appwork.mandisamiti.ui.theme.MandiGreenText
import com.appwork.mandisamiti.ui.theme.MandiNavy
import com.appwork.mandisamiti.ui.theme.MandiNeutralBorder
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
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToNewEntry: () -> Unit,
    onNavigateToPartyKhata: (Party) -> Unit,
    onNavigateToDayClosing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddPartyDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showAddPartyDialog) {
        com.appwork.mandisamiti.ui.components.AddPartyDialog(
            onDismiss = { showAddPartyDialog = false },
            onSaveParty = { name, phone, village, partyType, interestRate ->
                viewModel.addNewParty(name, phone, village, partyType, interestRate)
                showAddPartyDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            SoundboxTopBar(
                shopName = uiState.shopProfile?.shopName ?: "श्री गणेश ट्रेडिंग",
                mandiLocation = uiState.shopProfile?.mandiName ?: "नवीन अनाज मंडी, मथुरा",
                isSoundEnabled = uiState.shopProfile?.isSoundEnabled ?: true,
                onToggleSound = { viewModel.toggleSoundSetting() }
            )
        },
        bottomBar = {
            // High-Trust Fintech Action Bar (Split 2-Action: Add Party & Add Deal)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MandiSurface)
                    .border(1.dp, MandiBorder)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Add Party (किसान / व्यापारी जोड़ें)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MandiBackground)
                            .border(1.dp, MandiNavy, RoundedCornerShape(10.dp))
                            .clickable { showAddPartyDialog = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MandiNavy,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "नया खाता",
                            color = MandiNavy,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 2. New Deal Entry (नया सौदा दर्ज करें)
                    Row(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MandiNavy)
                            .clickable { onNavigateToNewEntry() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "नया सौदा दर्ज करें",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MandiBackground)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Executive Balance Overview Cards (Receivables & Payables)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Market Receivable (लेना है)
                    FintechMetricCard(
                        title = "बाजार में बाकी",
                        subtitle = "कुल लेना",
                        amountPaisa = uiState.totalMarketReceivablePaisa,
                        textColor = MandiRedReceivable,
                        bgColor = MandiSurface,
                        borderColor = MandiRedBorder,
                        icon = Icons.Default.TrendingDown,
                        iconTint = MandiRedReceivable,
                        modifier = Modifier.weight(1f)
                    )

                    // Farmer Payable (देना है)
                    FintechMetricCard(
                        title = "किसान जमा",
                        subtitle = "कुल देय",
                        amountPaisa = uiState.totalFarmerPayablePaisa,
                        textColor = MandiGreenPayable,
                        bgColor = MandiSurface,
                        borderColor = MandiGreenBorder,
                        icon = Icons.Default.TrendingUp,
                        iconTint = MandiGreenPayable,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Daily Cash Drawer Banner (Stripe / Google Pay Style)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MandiSurface)
                        .border(1.dp, MandiBorder, RoundedCornerShape(12.dp))
                        .clickable { onNavigateToDayClosing() }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MandiNeutralLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MandiNavy,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "दैनिक गल्ला रोकड़ बही",
                                    color = MandiTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "आज की नकद आवक, निकासी व मिलान",
                                    color = MandiTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MandiTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 3. Search Bar with Vector Search Icon
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = {
                        Text(
                            text = "किसान, व्यापारी या गाँव का नाम खोजें...",
                            fontSize = 15.sp,
                            color = MandiTextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MandiTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MandiSurface,
                        unfocusedContainerColor = MandiSurface,
                        focusedBorderColor = MandiNavy,
                        unfocusedBorderColor = MandiBorder
                    )
                )
            }

            // 4. Clean Filter Segmented Control (No Emojis)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentFilterChip(
                        label = "सभी (${uiState.allParties.size})",
                        isSelected = uiState.activeFilter == PartyFilter.ALL,
                        onClick = { viewModel.onFilterSelected(PartyFilter.ALL) },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentFilterChip(
                        label = "लेना है",
                        isSelected = uiState.activeFilter == PartyFilter.RECEIVABLE,
                        onClick = { viewModel.onFilterSelected(PartyFilter.RECEIVABLE) },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentFilterChip(
                        label = "देना है",
                        isSelected = uiState.activeFilter == PartyFilter.PAYABLE,
                        onClick = { viewModel.onFilterSelected(PartyFilter.PAYABLE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Party Ledger Stream
            items(uiState.filteredParties, key = { it.party.id }) { partyWithBalance ->
                PartyCard(
                    party = partyWithBalance.party,
                    balancePaisa = partyWithBalance.balancePaisa,
                    onClick = { onNavigateToPartyKhata(partyWithBalance.party) }
                )
            }

            if (uiState.filteredParties.isEmpty() && !uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "कोई खाता नहीं मिला",
                            color = MandiTextMuted,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun FintechMetricCard(
    title: String,
    subtitle: String,
    amountPaisa: Long,
    textColor: Color,
    bgColor: Color,
    borderColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MandiTextSecondary
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = "₹${MandiMathEngine.paisaToRupeesString(amountPaisa)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = MandiTextMuted
            )
        }
    }
}

@Composable
private fun SegmentFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isSelected) MandiNavy else MandiSurface)
            .border(1.dp, if (isSelected) MandiNavy else MandiBorder, shape)
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MandiTextSecondary
        )
    }
}
