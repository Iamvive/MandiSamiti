package com.appwork.mandisamiti.ui.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.slipLabel
import com.appwork.mandisamiti.ui.home.HomeUiState
import com.appwork.mandisamiti.ui.home.OutlierEntryInfo
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiGreenReceivable
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

enum class DashboardDateFilter {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    ALL_TIME
}

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
    onNavigateToGalla: () -> Unit = {},
    onNavigateToPartyKhata: (Party) -> Unit = {},
    onNavigateToAllKhata: () -> Unit = {},
    onNavigateToBuyReceipt: (String) -> Unit = {},
    onNavigateToSellReceipt: (String) -> Unit = {},
    onNavigateToNewEntry: () -> Unit = onNavigateToBuy,
    onPhoneNeeded: (onAdd: () -> Unit) -> Unit = {},
    needsLogin: Boolean = false,
    isEnglish: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(DashboardDateFilter.TODAY) }

    if (uiState.isLoading) {
        DashboardSkeleton(modifier = modifier)
        return
    }

    // Date Bounds Calculation
    val tz = TimeZone.currentSystemDefault()
    val todayDate = Clock.System.now().toLocalDateTime(tz).date
    val todayStartMs = todayDate.atStartOfDayIn(tz).toEpochMilliseconds()
    val yesterdayDate = todayDate.minus(1, DateTimeUnit.DAY)
    val yesterdayStartMs = yesterdayDate.atStartOfDayIn(tz).toEpochMilliseconds()
    val weekStartMs = todayDate.minus(7, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds()

    // Filtered Dated Entries
    val datedBuys = remember(uiState.buys, selectedFilter) {
        when (selectedFilter) {
            DashboardDateFilter.TODAY -> uiState.buys.filter { it.entryDate >= todayStartMs }
            DashboardDateFilter.YESTERDAY -> uiState.buys.filter { it.entryDate in yesterdayStartMs until todayStartMs }
            DashboardDateFilter.THIS_WEEK -> uiState.buys.filter { it.entryDate >= weekStartMs }
            DashboardDateFilter.ALL_TIME -> uiState.buys
        }
    }

    val datedSells = remember(uiState.sells, selectedFilter) {
        when (selectedFilter) {
            DashboardDateFilter.TODAY -> uiState.sells.filter { it.entryDate >= todayStartMs }
            DashboardDateFilter.YESTERDAY -> uiState.sells.filter { it.entryDate in yesterdayStartMs until todayStartMs }
            DashboardDateFilter.THIS_WEEK -> uiState.sells.filter { it.entryDate >= weekStartMs }
            DashboardDateFilter.ALL_TIME -> uiState.sells
        }
    }

    val datedExpenses = remember(uiState.expenses, selectedFilter) {
        when (selectedFilter) {
            DashboardDateFilter.TODAY -> uiState.expenses.filter { it.entryDate >= todayStartMs }
            DashboardDateFilter.YESTERDAY -> uiState.expenses.filter { it.entryDate in yesterdayStartMs until todayStartMs }
            DashboardDateFilter.THIS_WEEK -> uiState.expenses.filter { it.entryDate >= weekStartMs }
            DashboardDateFilter.ALL_TIME -> uiState.expenses
        }
    }

    // Dated Metrics (Direction B Formulas)
    // 1. Net Payable: Buy slip value only, without advance or subsequent payments
    val datedNetPayablePaisa = remember(datedBuys) {
        datedBuys.filter { !it.isVoid }.sumOf {
            (it.grossPaisa - it.labourPaisa - it.commissionPaisa).coerceAtLeast(0L)
        }
    }

    // 2. Vyapari Receivable: Unpaid amount on Sell slips in the selected dates (after received payments)
    val datedVyapariReceivablePaisa = remember(datedSells) {
        datedSells.filter { !it.isVoid }.sumOf {
            it.balanceDuePaisa.coerceAtLeast(0L)
        }
    }

    // 3. Crop Weights bought and sold
    val datedCropWeights = remember(datedBuys, datedSells) {
        val crops = (datedBuys.map { it.commodityId } + datedSells.map { it.commodityId }).toSet().filter { it.isNotBlank() }
        crops.associateWith { crop ->
            val bought = datedBuys.filter { !it.isVoid && it.commodityId == crop }.sumOf { it.totalWeightGrams } / 100_000.0
            val sold = datedSells.filter { !it.isVoid && it.commodityId == crop }.sumOf { it.totalWeightGrams } / 100_000.0
            Pair(bought, sold)
        }
    }

    // 4. Expenses Total
    val datedExpensesTotalPaisa = remember(datedExpenses) {
        datedExpenses.filter { !it.isVoid }.sumOf { it.amountPaisa }
    }

    // Top 5 Recent Buy Slips
    val recentBuys = remember(uiState.buys) {
        uiState.buys.filter { !it.isVoid }.sortedByDescending { it.entryDate }.take(5)
    }

    // All-Time Totals
    val allTimeBuysGross = remember(uiState.buys) { uiState.buys.filter { !it.isVoid }.sumOf { it.grossPaisa } }
    val allTimeSellsNet = remember(uiState.sells) { uiState.sells.filter { !it.isVoid }.sumOf { it.totalReceivablePaisa } }
    val allTimeLendsTotal = remember(uiState.lends) { uiState.lends.filter { !it.isVoid }.sumOf { it.amountPaisa } }
    val allTimeExpensesTotal = remember(uiState.expenses) { uiState.expenses.filter { !it.isVoid }.sumOf { it.amountPaisa } }

    Scaffold(
        bottomBar = {
            // Fixed 4-Way Action Dock (Direction B Ergonomics)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToBuy,
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isEnglish) "Buy" else "नई खरीद", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onNavigateToSell,
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.CallMade, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isEnglish) "Sell" else "नई बिक्री", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onNavigateToLend,
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.CallReceived, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isEnglish) "Lend" else "उधार", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onNavigateToExpense,
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isEnglish) "Expense" else "खर्च", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
        },
        containerColor = MandiBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Rokdi Balance Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToGalla),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEnglish) "Rokdi Balance" else "रोकड़ शेष",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MandiTextSecondary
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MandiTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = MandiMathEngine.formatRupees(uiState.rokdiBalancePaisa),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                        Text(
                            text = if (isEnglish) "By the book, in the galla now" else "बही के अनुसार, गल्ले में अभी",
                            style = MaterialTheme.typography.bodySmall,
                            color = MandiTextSecondary
                        )
                    }
                }
            }

            // 2. Date Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == DashboardDateFilter.TODAY,
                        onClick = { selectedFilter = DashboardDateFilter.TODAY },
                        label = { Text(if (isEnglish) "Today" else "आज") }
                    )
                    FilterChip(
                        selected = selectedFilter == DashboardDateFilter.YESTERDAY,
                        onClick = { selectedFilter = DashboardDateFilter.YESTERDAY },
                        label = { Text(if (isEnglish) "Yesterday" else "कल") }
                    )
                    FilterChip(
                        selected = selectedFilter == DashboardDateFilter.THIS_WEEK,
                        onClick = { selectedFilter = DashboardDateFilter.THIS_WEEK },
                        label = { Text(if (isEnglish) "This Week" else "इस हफ्ते") }
                    )
                    FilterChip(
                        selected = selectedFilter == DashboardDateFilter.ALL_TIME,
                        onClick = { selectedFilter = DashboardDateFilter.ALL_TIME },
                        label = { Text(if (isEnglish) "All Time" else "कस्टम तारीख") }
                    )
                }
            }

            // 3. Dated Metric: शुद्ध देय (Net Payable)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToBuyList),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isEnglish) "Net Payable" else "शुद्ध देय", style = MaterialTheme.typography.bodyMedium, color = MandiTextSecondary)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MandiTextSecondary, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = MandiMathEngine.formatRupees(datedNetPayablePaisa),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                        Text(
                            text = if (isEnglish) "Buy slip value only. Advance and payments not included." else "केवल खरीद पर्ची की राशि। अग्रिम और भुगतान शामिल नहीं।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MandiTextSecondary
                        )
                    }
                }
            }

            // 4. Dated Metric: व्यापारी से लेना (Vyapari Receivable)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToSellList),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isEnglish) "To Receive from Vyapari" else "व्यापारी से लेना", style = MaterialTheme.typography.bodyMedium, color = MandiTextSecondary)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MandiTextSecondary, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = MandiMathEngine.formatRupees(datedVyapariReceivablePaisa),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiGreenReceivable
                        )
                        Text(
                            text = if (isEnglish) "Unpaid on Sell slips in the selected dates, after money received." else "चुनी तारीख की बिक्री पर्चियों का बाकी। मिली रकम घटाकर।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MandiTextSecondary
                        )
                    }
                }
            }

            // 5. Dated Metric: गोदाम (Godown summary)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToStockSheet),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (isEnglish) "Crop Weight (qtl)" else "फसल का वजन (क्विंटल)", style = MaterialTheme.typography.bodySmall, color = MandiTextSecondary)
                            Text(if (isEnglish) "Bought / Sold" else "खरीद / बिक्री", style = MaterialTheme.typography.bodySmall, color = MandiTextSecondary)
                        }
                        if (datedCropWeights.isEmpty()) {
                            Text(
                                text = if (isEnglish) "No transactions for selected dates" else "चुनी तारीख में कोई सौदा नहीं",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MandiTextMuted
                            )
                        } else {
                            datedCropWeights.forEach { (crop, pair) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(crop, fontWeight = FontWeight.SemiBold, color = MandiTextPrimary)
                                    Text(
                                        text = "${MandiMathEngine.formatIndianGrouping(pair.first.toLong())} / ${MandiMathEngine.formatIndianGrouping(pair.second.toLong())}",
                                        fontWeight = FontWeight.Bold,
                                        color = MandiTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Dated Metric: खर्च (Expenses)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToExpense),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isEnglish) "Expenses" else "खर्च", style = MaterialTheme.typography.bodyMedium, color = MandiTextSecondary)
                        Text(
                            text = MandiMathEngine.formatRupees(datedExpensesTotalPaisa),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                    }
                }
            }

            // 7. Recent Buy Slips (हाल की खरीद पर्चियां)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Recent Buy Slips" else "हाल की खरीद पर्चियां",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                    TextButton(onClick = onNavigateToBuyList) {
                        Text(if (isEnglish) "See All" else "सभी देखें", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (recentBuys.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(if (isEnglish) "No buy slips yet" else "अभी कोई खरीद पर्ची नहीं है", color = MandiTextMuted)
                        }
                    }
                }
            } else {
                items(recentBuys, key = { it.id }) { slip ->
                    RecentSlipRow(
                        slip = slip,
                        farmerName = uiState.allParties.find { it.party.id == slip.farmerId }?.party?.name ?: slip.farmerId,
                        onClick = { onNavigateToBuyReceipt(slip.id) }
                    )
                }
            }

            // 8. All-Time Block (अब तक)
            item {
                Text(
                    text = if (isEnglish) "All Time" else "अब तक",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AllTimeCard(
                            label = if (isEnglish) "Purchases" else "कुल खरीद",
                            value = MandiMathEngine.formatRupees(allTimeBuysGross),
                            onClick = onNavigateToBuyList,
                            modifier = Modifier.weight(1f)
                        )
                        AllTimeCard(
                            label = if (isEnglish) "Sales" else "कुल बिक्री",
                            value = MandiMathEngine.formatRupees(allTimeSellsNet),
                            onClick = onNavigateToSellList,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AllTimeCard(
                            label = if (isEnglish) "Outstanding Loans" else "कुल उधार",
                            value = MandiMathEngine.formatRupees(allTimeLendsTotal),
                            onClick = onNavigateToLendList,
                            modifier = Modifier.weight(1f)
                        )
                        AllTimeCard(
                            label = if (isEnglish) "Expenses" else "कुल खर्च",
                            value = MandiMathEngine.formatRupees(allTimeExpensesTotal),
                            onClick = onNavigateToExpense,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RecentSlipRow(
    slip: BuyEntry,
    farmerName: String,
    onClick: () -> Unit
) {
    val slipNet = (slip.grossPaisa - slip.labourPaisa - slip.commissionPaisa).coerceAtLeast(0L)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(farmerName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MandiTextPrimary)
                Text(
                    text = "#${slipLabel(slip.slipNo, slip.id)} • ${slip.commodityId} • ${slip.bagsCount} बोरी",
                    style = MaterialTheme.typography.bodySmall,
                    color = MandiTextSecondary
                )
            }
            Text(
                text = "₹${MandiMathEngine.paisaToRupeesString(slipNet)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary
            )
        }
    }
}

@Composable
private fun AllTimeCard(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MandiTextSecondary)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MandiTextPrimary)
        }
    }
}

@Composable
fun DashboardSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
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
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmerColor)
            )
        }
    }
}
