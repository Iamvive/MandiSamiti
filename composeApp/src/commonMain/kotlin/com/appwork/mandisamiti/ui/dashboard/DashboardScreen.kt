package com.appwork.mandisamiti.ui.dashboard

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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.appwork.mandisamiti.ui.home.HomeUiState
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenPayable
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiRedReceivable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

@Composable
fun DashboardScreen(
    uiState: HomeUiState,
    onNavigateToBuy: () -> Unit = {},
    onNavigateToSell: () -> Unit = {},
    onNavigateToLend: () -> Unit = {},
    onNavigateToExpense: () -> Unit = {},
    onNavigateToBuyList: () -> Unit = {},
    onNavigateToSellList: () -> Unit = {},
    onNavigateToLendList: () -> Unit = {},
    onNavigateToStockSheet: () -> Unit = {},
    onNavigateToPartyKhata: (Party) -> Unit = {},
    onNavigateToAllKhata: () -> Unit = {},
    onNavigateToNewEntry: () -> Unit = onNavigateToBuy,
    isEnglish: Boolean = false,
    modifier: Modifier = Modifier
) {
    val receivableCount = uiState.allParties.count { it.balancePaisa > 0 }
    val payableCount = uiState.allParties.count { it.balancePaisa < 0 }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MandiBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Sleek Greeting Banner
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MandiSurfaceElevated)
                    .border(1.dp, MandiBorder, RoundedCornerShape(10.dp))
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
                        tint = MandiTextPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = if (isEnglish) "Welcome, ${uiState.shopProfile?.shopName ?: "Trader"}" else "नमस्ते, ${uiState.shopProfile?.shopName ?: "आढ़ती जी"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MandiTextPrimary
                    )
                }
                Text(
                    text = if (isEnglish) "Today's Mandi" else "आज का बहीखाता",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MandiTextSecondary
                )
            }
        }

        // 2. Modern 4-Way Quick Actions
        item {
            QuickActionsRow(
                onBuy = onNavigateToBuy,
                onSell = onNavigateToSell,
                onLend = onNavigateToLend,
                onExpense = onNavigateToExpense,
                isEnglish = isEnglish
            )
        }

        // 3. Core Financial KPIs Grid
        item {
            RevampKpiGrid(
                kpis = uiState.kpis,
                onBuyClick = onNavigateToBuyList,
                onSellClick = onNavigateToSellList,
                onLendClick = onNavigateToLendList,
                onExpenseClick = onNavigateToExpense
            )
        }

        // 4. High-Trust Financial Metric Tiles (Ledger Balances)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardMetricCard(
                    title = if (isEnglish) "Receivable (Market)" else "कुल लेना (बाजार बाकी)",
                    subtitle = if (isEnglish) "From $receivableCount accounts" else "$receivableCount खातों से",
                    amountPaisa = uiState.totalMarketReceivablePaisa,
                    textColor = MandiRedReceivable,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    iconTint = MandiRedReceivable,
                    modifier = Modifier.weight(1f)
                )

                DashboardMetricCard(
                    title = if (isEnglish) "Payable (Farmers)" else "कुल देना (किसान जमा)",
                    subtitle = if (isEnglish) "To $payableCount accounts" else "$payableCount खातों को",
                    amountPaisa = uiState.totalFarmerPayablePaisa,
                    textColor = MandiGreenPayable,
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    iconTint = MandiGreenPayable,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Recent Active Accounts Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEnglish) "Active Ledger Accounts" else "सक्रिय खाते (हाल का लेन-देन)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                )
                Row(
                    modifier = Modifier.clickable { onNavigateToAllKhata() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isEnglish) "View All" else "सभी देखें",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MandiTextSecondary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MandiTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // 5. Active Accounts Preview List (Top 5)
        val previewParties = uiState.allPartiesWithBalance.take(5)
        items(previewParties, key = { it.party.id }) { partyWithBalance ->
            PartyCard(
                party = partyWithBalance.party,
                balancePaisa = partyWithBalance.balancePaisa,
                onClick = { onNavigateToPartyKhata(partyWithBalance.party) }
            )
        }

        if (previewParties.isEmpty() && !uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEnglish) "No accounts added yet" else "अभी कोई खाता नहीं है",
                        color = MandiTextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    subtitle: String,
    amountPaisa: Long,
    textColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MandiSurface)
            .border(1.dp, MandiBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MandiTextSecondary
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
            }

            Text(
                text = "₹${MandiMathEngine.paisaToRupeesString(amountPaisa)}",
                fontSize = 22.sp,
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
fun QuickActionsRow(
    onBuy: () -> Unit,
    onSell: () -> Unit,
    onLend: () -> Unit,
    onExpense: () -> Unit,
    isEnglish: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionButton(
            label = if (isEnglish) "Buy" else "खरीद",
            subLabel = if (isEnglish) "Inward" else "आवक",
            icon = Icons.Default.ShoppingCart,
            containerColor = MandiGreenPayable.copy(alpha = 0.12f),
            contentColor = MandiGreenPayable,
            onClick = onBuy,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = if (isEnglish) "Sell" else "बिक्री",
            subLabel = if (isEnglish) "Outward" else "जावक",
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            containerColor = MandiPrimaryAction.copy(alpha = 0.12f),
            contentColor = MandiPrimaryAction,
            onClick = onSell,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = if (isEnglish) "Lend" else "उधार",
            subLabel = if (isEnglish) "Advance" else "पेशगी",
            icon = Icons.Default.AccountBalanceWallet,
            containerColor = Color(0xFFF57C00).copy(alpha = 0.12f),
            contentColor = Color(0xFFE65100),
            onClick = onLend,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = if (isEnglish) "Expense" else "खर्च",
            subLabel = if (isEnglish) "Mandi" else "मंडी व्यय",
            icon = Icons.Default.MonetizationOn,
            containerColor = Color(0xFFD81B60).copy(alpha = 0.12f),
            contentColor = Color(0xFFC2185B),
            onClick = onExpense,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    subLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = subLabel,
                fontSize = 10.sp,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

