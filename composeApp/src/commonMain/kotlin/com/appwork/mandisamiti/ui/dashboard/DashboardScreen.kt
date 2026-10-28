package com.appwork.mandisamiti.ui.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.appwork.mandisamiti.domain.math.MandiCurrencyFormatter
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.ui.components.PartyCard
import com.appwork.mandisamiti.ui.home.HomeUiState
import com.appwork.mandisamiti.ui.theme.MandiAmberBorder
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiAmberLight
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiBlueSell
import com.appwork.mandisamiti.ui.theme.MandiGreenBuy
import com.appwork.mandisamiti.ui.theme.MandiGreenReceivable
import com.appwork.mandisamiti.ui.theme.MandiOrangeLend
import com.appwork.mandisamiti.ui.theme.MandiPinkExpense
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiRedPayable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary
import com.appwork.mandisamiti.ui.theme.opaqueTint

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
    val receivableSubtitle = when {
        receivableCount > 0 && uiState.unlinkedLendCount > 0 ->
            if (isEnglish) "$receivableCount accounts + ${uiState.unlinkedLendCount} unlinked"
            else "$receivableCount खातों से + ${uiState.unlinkedLendCount} बिना खाते"
        receivableCount > 0 ->
            if (isEnglish) "From $receivableCount accounts" else "$receivableCount खातों से"
        uiState.unlinkedLendCount > 0 ->
            if (isEnglish) "${uiState.unlinkedLendCount} unlinked lend" else "${uiState.unlinkedLendCount} बिना खाते का उधार"
        else ->
            if (isEnglish) "From 0 accounts" else "0 खातों से"
    }

    if (uiState.isLoading) {
        DashboardSkeleton(modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MandiBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Modern 4-Way Quick Actions
        item {
            QuickActionsRow(
                onBuy = onNavigateToBuy,
                onSell = onNavigateToSell,
                onLend = onNavigateToLend,
                onExpense = onNavigateToExpense,
                isEnglish = isEnglish
            )
        }

        // Entries that exist only on this phone: a trader must know before clearing data or changing phones.
        if (uiState.pendingSyncCount > 0) {
            item {
                SyncPendingLine(count = uiState.pendingSyncCount, isEnglish = isEnglish)
            }
        }

        // 3. Core Financial KPIs Grid
        item {
            RevampKpiGrid(
                kpis = uiState.kpis,
                onBuyClick = onNavigateToBuyList,
                onSellClick = onNavigateToSellList,
                onLendClick = onNavigateToLendList,
                onExpenseClick = onNavigateToExpense,
                isEnglish = isEnglish
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
                    subtitle = receivableSubtitle,
                    amountPaisa = uiState.totalMarketReceivablePaisa,
                    textColor = MandiGreenReceivable,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    iconTint = MandiGreenReceivable,
                    isEnglish = isEnglish,
                    modifier = Modifier.weight(1f)
                )

                DashboardMetricCard(
                    title = if (isEnglish) "Payable (Farmers)" else "कुल देना (किसान जमा)",
                    subtitle = if (isEnglish) "To $payableCount accounts" else "$payableCount खातों को",
                    amountPaisa = uiState.totalFarmerPayablePaisa,
                    textColor = MandiRedPayable,
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    iconTint = MandiRedPayable,
                    isEnglish = isEnglish,
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
                    fontSize = 16.sp,
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MandiTextSecondary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MandiTextSecondary,
                        modifier = Modifier.size(16.dp)
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

        if (previewParties.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MandiSurfaceElevated)
                        .border(1.dp, MandiBorder, RoundedCornerShape(12.dp))
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isEnglish) "No accounts added yet" else "अभी कोई खाता नहीं है",
                        color = MandiTextMuted,
                        fontSize = 14.sp
                    )
                    OutlinedButton(
                        onClick = onNavigateToAllKhata,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) "Add First Account" else "पहला खाता जोड़ें",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
    isEnglish: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showExact by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MandiSurface)
            .border(1.dp, MandiBorder, RoundedCornerShape(12.dp))
            .clickable { showExact = !showExact }
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
                    fontSize = 14.sp,
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
                text = if (showExact) "₹${MandiMathEngine.paisaToRupeesString(amountPaisa)}"
                       else MandiCurrencyFormatter.formatCompactRupees(amountPaisa, isEnglish),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Text(
                text = subtitle,
                fontSize = 14.sp,
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
    val bg = MandiSurface
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionButton(
            label = DashboardStrings.quickActionBuy(isEnglish),
            subLabel = DashboardStrings.quickActionBuySub(isEnglish),
            icon = Icons.Default.ShoppingCart,
            containerColor = opaqueTint(MandiGreenBuy, bg, 0.12f),
            contentColor = MandiGreenBuy,
            onClick = onBuy,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = DashboardStrings.quickActionSell(isEnglish),
            subLabel = DashboardStrings.quickActionSellSub(isEnglish),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            containerColor = opaqueTint(MandiBlueSell, bg, 0.12f),
            contentColor = MandiBlueSell,
            onClick = onSell,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = DashboardStrings.quickActionLend(isEnglish),
            subLabel = DashboardStrings.quickActionLendSub(isEnglish),
            icon = Icons.Default.AccountBalanceWallet,
            containerColor = opaqueTint(MandiOrangeLend, bg, 0.12f),
            contentColor = MandiOrangeLend,
            onClick = onLend,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = DashboardStrings.quickActionExpense(isEnglish),
            subLabel = DashboardStrings.quickActionExpenseSub(isEnglish),
            icon = Icons.Default.MonetizationOn,
            containerColor = opaqueTint(MandiPinkExpense, bg, 0.12f),
            contentColor = MandiPinkExpense,
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
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = subLabel,
                fontSize = 14.sp,
                color = contentColor
            )
        }
    }
}

@Composable
private fun SyncPendingLine(count: Int, isEnglish: Boolean) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MandiAmberLight)
            .border(1.dp, MandiAmberBorder, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = null,
            tint = MandiAmberDark,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = if (isEnglish) "$count entries are saved on this phone only. They upload when the internet is on."
            else "$count एंट्रियाँ सिर्फ इस फोन में हैं। इंटरनेट मिलते ही सर्वर पर जाएँगी।",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MandiAmberDark,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun DashboardSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val shimmerColor = MandiSurfaceElevated.copy(alpha = alpha)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MandiBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick actions skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(shimmerColor)
                )
            }
        }

        // KPI grid skeleton (2x2)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(shimmerColor)
                        )
                    }
                }
            }
        }

        // Ledger metric cards skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(88.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(shimmerColor)
                )
            }
        }

        // Account list item skeleton
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(shimmerColor)
                )
            }
        }
    }
}


