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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToNewEntry: () -> Unit,
    onNavigateToPartyKhata: (Party) -> Unit,
    onNavigateToDayClosing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

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
            // Persistent Giant 64dp Bottom Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MandiSurface)
                    .border(1.dp, MandiBorder)
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MandiGreenPayable)
                        .clickable { onNavigateToNewEntry() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚡ नया पर्चा / बिक्री दर्ज करें",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
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
            // 1. Two Master Metrics (लाल बाकी vs हरा जमा)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Market Receivable (लेना है)
                    BalanceStatCard(
                        title = "बाजार में बाकी (लेना)",
                        amountPaisa = uiState.totalMarketReceivablePaisa,
                        textColor = MandiRedReceivable,
                        bgColor = MandiRedLight,
                        borderColor = MandiRedReceivable.copy(alpha = 0.3f),
                        modifier = Modifier.weight(1f)
                    )

                    // Farmer Payable (देना है)
                    BalanceStatCard(
                        title = "किसान जमा (देना)",
                        amountPaisa = uiState.totalFarmerPayablePaisa,
                        textColor = MandiGreenPayable,
                        bgColor = MandiGreenLight,
                        borderColor = MandiGreenPayable.copy(alpha = 0.3f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Daily Cash Summary Shortcut Button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MandiNavy.copy(alpha = 0.06f))
                        .border(1.dp, MandiBorder, RoundedCornerShape(10.dp))
                        .clickable { onNavigateToDayClosing() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📊 आज का दैनिक गल्ला रोकड़ हिसाब",
                            color = MandiNavy,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "देखें ➔",
                            color = MandiNavy,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. Search Bar with Voice hint
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = {
                        Text(
                            text = "🔍 किसान या गाँव का नाम खोजें...",
                            fontSize = 16.sp,
                            color = MandiTextMuted
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

            // 4. Quick Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        label = "सभी (${uiState.allParties.size})",
                        isSelected = uiState.activeFilter == PartyFilter.ALL,
                        onClick = { viewModel.onFilterSelected(PartyFilter.ALL) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        label = "🔴 लेना है",
                        isSelected = uiState.activeFilter == PartyFilter.RECEIVABLE,
                        onClick = { viewModel.onFilterSelected(PartyFilter.RECEIVABLE) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        label = "🟢 देना है",
                        isSelected = uiState.activeFilter == PartyFilter.PAYABLE,
                        onClick = { viewModel.onFilterSelected(PartyFilter.PAYABLE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Party Cards List
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
                            fontSize = 16.sp
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
private fun BalanceStatCard(
    title: String,
    amountPaisa: Long,
    textColor: Color,
    bgColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(1.5.dp, borderColor, shape)
            .padding(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MandiTextSecondary
            )
            Text(
                text = "₹ ${MandiMathEngine.paisaToRupeesString(amountPaisa)}",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FilterChip(
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
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else MandiTextPrimary
        )
    }
}
