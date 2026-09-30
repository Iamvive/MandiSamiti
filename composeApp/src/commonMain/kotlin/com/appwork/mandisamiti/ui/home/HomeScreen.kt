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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.ui.components.PartyCard
import com.appwork.mandisamiti.ui.components.SoundboxTopBar
import com.appwork.mandisamiti.ui.theme.MandiAmber50
import com.appwork.mandisamiti.ui.theme.MandiAmberBorder
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiAmberLight
import com.appwork.mandisamiti.ui.theme.MandiAmberPrimary
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenLight
import com.appwork.mandisamiti.ui.theme.MandiGreenPayable
import com.appwork.mandisamiti.ui.theme.MandiGreenText
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

    val receivableCount = uiState.allParties.count { it.balancePaisa > 0 }
    val payableCount = uiState.allParties.count { it.balancePaisa < 0 }

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
            // Mandi Saffron Action Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MandiSurface)
                    .border(1.dp, MandiBorder)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(MandiAmberDark, MandiAmberPrimary)
                            )
                        )
                        .clickable { onNavigateToNewEntry() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "नया सौदा / आवक दर्ज करें",
                        color = Color.White,
                        fontSize = 17.sp,
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Warm Greeting & Mandi Date Banner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MandiAmber50)
                        .border(1.dp, MandiAmberBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = MandiAmberDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "नमस्ते, ${uiState.shopProfile?.shopName ?: "आढ़ती जी"}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiAmberDark
                        )
                    }
                    Text(
                        text = "आज का बहीखाता",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MandiAmberPrimary
                    )
                }
            }

            // 2. High-Trust Metric Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FintechMetricCard(
                        title = "कुल लेना (Receivable)",
                        subtitle = "$receivableCount खातों से",
                        amountPaisa = uiState.totalMarketReceivablePaisa,
                        textColor = MandiRedReceivable,
                        bgColor = MandiRedLight,
                        borderColor = MandiRedBorder,
                        icon = Icons.Default.TrendingUp,
                        iconTint = MandiRedReceivable,
                        modifier = Modifier.weight(1f)
                    )

                    FintechMetricCard(
                        title = "कुल देना (Payable)",
                        subtitle = "$payableCount खातों को",
                        amountPaisa = uiState.totalFarmerPayablePaisa,
                        textColor = MandiGreenPayable,
                        bgColor = MandiGreenLight,
                        borderColor = MandiGreenBorder,
                        icon = Icons.Default.TrendingDown,
                        iconTint = MandiGreenPayable,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Quick Cash Register (गल्ला हिसाब) Strip
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MandiAmberLight)
                                    .border(1.dp, MandiAmberBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MandiAmberDark,
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

            // 4. Search Bar
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
                        focusedBorderColor = MandiAmberPrimary,
                        unfocusedBorderColor = MandiBorder
                    )
                )
            }

            // 5. Filter Segmented Control
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

            // 6. Party Ledger Stream
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
                fontSize = 24.sp,
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
            .background(if (isSelected) MandiAmberPrimary else MandiSurface)
            .border(1.dp, if (isSelected) MandiAmberPrimary else MandiBorder, shape)
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
