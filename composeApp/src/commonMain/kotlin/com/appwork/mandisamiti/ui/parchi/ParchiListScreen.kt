package com.appwork.mandisamiti.ui.parchi

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.alpha
import com.appwork.mandisamiti.domain.model.Party
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.slipLabel
import com.appwork.mandisamiti.ui.theme.MandiAmberBorder
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiAmberLight
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenLight
import com.appwork.mandisamiti.ui.theme.MandiGreenText
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiRedBorder
import com.appwork.mandisamiti.ui.theme.MandiRedLight
import com.appwork.mandisamiti.ui.theme.MandiRedText
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class ParchiTab(
    val titleHi: String,
    val titleEn: String,
    val icon: ImageVector
) {
    KISAAN(
        titleHi = "किसान पर्ची",
        titleEn = "Farmer Slips",
        icon = Icons.Default.Agriculture
    ),
    VYAPARI(
        titleHi = "व्यापारी पर्ची",
        titleEn = "Buyer Slips",
        icon = Icons.Default.Store
    );

    fun getTitle(isEnglish: Boolean): String = if (isEnglish) titleEn else titleHi
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ParchiListScreen(
    buys: List<BuyEntry>,
    sells: List<SellEntry>,
    partyNamesById: Map<String, String>,
    parties: List<Party> = emptyList(),
    gallaCashPaisa: Long = 0L,
    onOpenBuyReceipt: (String) -> Unit,
    onOpenSellReceipt: (String) -> Unit,
    onNavigateToNewBuy: () -> Unit,
    onNavigateToNewSell: () -> Unit,
    onSettleBuySlipCash: ((buyId: String, amountPaisa: Long) -> Unit)? = null,
    onSettleSellSlipCash: ((sellId: String, amountPaisa: Long) -> Unit)? = null,
    isEnglish: Boolean = false,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    var query by rememberSaveable { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (pagerState.currentPage == 0) onNavigateToNewBuy() else onNavigateToNewSell()
                },
                containerColor = MandiPrimaryAction,
                contentColor = MandiPrimaryActionText,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (pagerState.currentPage == 0) {
                        if (isEnglish) "New Buy Slip" else "नई किसान पर्ची"
                    } else {
                        if (isEnglish) "New Buyer Slip" else "नई व्यापारी पर्ची"
                    }
                )
            }
        },
        containerColor = MandiBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ViewPager Tab Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MandiSurface,
                    contentColor = MandiPrimaryAction,
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                color = MandiPrimaryAction
                            )
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MandiBorder, RoundedCornerShape(12.dp))
                ) {
                    ParchiTab.entries.forEachIndexed { index, tab ->
                        val isSelected = pagerState.currentPage == index
                        Tab(
                            selected = isSelected,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isSelected) MandiPrimaryAction else MandiTextMuted
                                    )
                                    Text(
                                        text = tab.getTitle(isEnglish),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = if (isSelected) MandiTextPrimary else MandiTextMuted
                                    )
                                }
                            }
                        )
                    }
                }
            }

            ParchiSearchBox(query = query, onQueryChange = { query = it }, isEnglish = isEnglish)

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> KisaanParchiView(
                        buys = buys,
                        query = query,
                        parties = parties,
                        partyNamesById = partyNamesById,
                        gallaCashPaisa = gallaCashPaisa,
                        onOpenReceipt = onOpenBuyReceipt,
                        onSettleBuySlipCash = onSettleBuySlipCash,
                        isEnglish = isEnglish
                    )
                    1 -> VyapariParchiView(
                        sells = sells,
                        query = query,
                        parties = parties,
                        partyNamesById = partyNamesById,
                        gallaCashPaisa = gallaCashPaisa,
                        onOpenReceipt = onOpenSellReceipt,
                        onSettleSellSlipCash = onSettleSellSlipCash,
                        isEnglish = isEnglish
                    )
                }
            }
        }
    }
}

@Composable
private fun KisaanParchiView(
    buys: List<BuyEntry>,
    query: String,
    parties: List<Party>,
    partyNamesById: Map<String, String>,
    gallaCashPaisa: Long,
    onOpenReceipt: (String) -> Unit,
    onSettleBuySlipCash: ((buyId: String, amountPaisa: Long) -> Unit)? = null,
    isEnglish: Boolean
) {
    val nonVoidBuys = remember(buys) { buys.filter { !it.isVoid } }

    // Metrics calculation:
    // 1. Total Net Procured Value: sum of netPayablePaisa
    // 2. Gross: sum of grossPaisa
    // 3. Total Net Grain Procured: sum of totalWeightGrams in Quintals
    val totalNetProcuredPaisa = remember(nonVoidBuys) { nonVoidBuys.sumOf { it.netPayablePaisa } }
    val totalGrossPaisa = remember(nonVoidBuys) { nonVoidBuys.sumOf { it.grossPaisa } }
    val totalWeightGrams = remember(nonVoidBuys) { nonVoidBuys.sumOf { it.totalWeightGrams } }

    var settlingBuy by remember { mutableStateOf<BuyEntry?>(null) }

    val dateGroupedEntries = remember(nonVoidBuys) {
        nonVoidBuys
            .sortedByDescending { it.entryDate }
            .groupBy { epochToLocalDate(it.entryDate) }
    }
    val searchGroups = remember(query, nonVoidBuys, parties) {
        filterSlipsByParty(query, nonVoidBuys, parties, { it.farmerId }, ::buyDuePaisa, { it.entryDate })
    }
    val expandedPaid = remember(query) { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (query.isNotBlank()) {
            if (searchGroups.isEmpty()) {
                item(key = "kisaan_search_empty") { SearchEmptyState(isEnglish) }
            } else {
                searchGroups.forEach { g ->
                    item(key = "ks_sum_${g.party.id}") {
                        PartySearchSummary(
                            name = g.party.name,
                            village = g.party.village,
                            phone = g.party.phone,
                            unpaidCount = g.unpaid.size,
                            totalDuePaisa = g.totalDuePaisa,
                            isReceivable = false,
                            isEnglish = isEnglish
                        )
                    }
                    items(g.unpaid, key = { "ks_u_${it.id}" }) { buy ->
                        KisaanSlipCard(
                            buy = buy,
                            farmerName = g.party.name,
                            onOpenReceipt = { onOpenReceipt(buy.id) },
                            onSettleCash = { settlingBuy = buy },
                            isEnglish = isEnglish
                        )
                    }
                    if (g.paid.isNotEmpty()) {
                        val open = expandedPaid[g.party.id] == true
                        item(key = "ks_fold_${g.party.id}") {
                            PaidSlipsFold(g.paid.size, open, { expandedPaid[g.party.id] = !open }, isEnglish)
                        }
                        if (open) {
                            items(g.paid, key = { "ks_p_${it.id}" }) { buy ->
                                Box(Modifier.alpha(0.6f)) {
                                    KisaanSlipCard(
                                        buy = buy,
                                        farmerName = g.party.name,
                                        onOpenReceipt = { onOpenReceipt(buy.id) },
                                        onSettleCash = { settlingBuy = buy },
                                        isEnglish = isEnglish
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
        // Top 3 Cards
        item(key = "kisaan_kpi_cards") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ParchiKpiCard(
                    title = if (isEnglish) "Net Procured" else "कुल नेट खरीद",
                    value = "₹${MandiMathEngine.formatIndianGrouping(totalNetProcuredPaisa / 100)}",
                    subtitle = if (isEnglish) "Payable" else "शुद्ध देय",
                    modifier = Modifier.weight(1f)
                )
                ParchiKpiCard(
                    title = if (isEnglish) "Gross" else "सकल (Gross)",
                    value = "₹${MandiMathEngine.formatIndianGrouping(totalGrossPaisa / 100)}",
                    subtitle = if (isEnglish) "Base value" else "फसल मूल्य",
                    modifier = Modifier.weight(1f)
                )
                ParchiKpiCard(
                    title = if (isEnglish) "Grain Procured" else "कुल आवक",
                    value = "${MandiMathEngine.gramsToQuintals(totalWeightGrams)} Q",
                    subtitle = if (isEnglish) "Quintals" else "क्विंटल",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (dateGroupedEntries.isEmpty()) {
            item(key = "kisaan_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEnglish) "No farmer slips recorded" else "कोई किसान पर्ची दर्ज नहीं है",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MandiTextMuted
                    )
                }
            }
        } else {
            dateGroupedEntries.forEach { (date, slips) ->
                item(key = "header_buy_${date}") {
                    DateSectionHeader(date = date, isEnglish = isEnglish)
                }
                items(slips, key = { it.id }) { buy ->
                    KisaanSlipCard(
                        buy = buy,
                        farmerName = partyNamesById[buy.farmerId] ?: buy.farmerId,
                        onOpenReceipt = { onOpenReceipt(buy.id) },
                        onSettleCash = { settlingBuy = buy },
                        isEnglish = isEnglish
                    )
                }
            }
        }
        }
    }

    settlingBuy?.let { buy ->
        FarmerSlipCashSettlementSheet(
            buy = buy,
            farmerName = partyNamesById[buy.farmerId] ?: buy.farmerId,
            gallaCashPaisa = gallaCashPaisa,
            onDismiss = { settlingBuy = null },
            onConfirmPayment = { amountPaisa ->
                onSettleBuySlipCash?.invoke(buy.id, amountPaisa)
                settlingBuy = null
            },
            isEnglish = isEnglish
        )
    }
}

@Composable
private fun VyapariParchiView(
    sells: List<SellEntry>,
    query: String,
    parties: List<Party>,
    partyNamesById: Map<String, String>,
    gallaCashPaisa: Long,
    onOpenReceipt: (String) -> Unit,
    onSettleSellSlipCash: ((sellId: String, amountPaisa: Long) -> Unit)? = null,
    isEnglish: Boolean
) {
    var settlingSell by remember { mutableStateOf<SellEntry?>(null) }
    val nonVoidSells = remember(sells) { sells.filter { !it.isVoid } }

    // Metrics calculation:
    // 1. Total Mandi Turnover: sum of totalReceivablePaisa
    // 2. Total Volume: sum of totalWeightGrams in Quintals
    // 3. Base Value: sum of grossPaisa (amount without mandi fee, arat, tulak)
    val totalTurnoverPaisa = remember(nonVoidSells) { nonVoidSells.sumOf { it.totalReceivablePaisa } }
    val totalVolumeGrams = remember(nonVoidSells) { nonVoidSells.sumOf { it.totalWeightGrams } }
    val totalBaseValuePaisa = remember(nonVoidSells) { nonVoidSells.sumOf { it.grossPaisa } }

    val dateGroupedEntries = remember(nonVoidSells) {
        nonVoidSells
            .sortedByDescending { it.entryDate }
            .groupBy { epochToLocalDate(it.entryDate) }
    }
    val searchGroups = remember(query, nonVoidSells, parties) {
        filterSlipsByParty(query, nonVoidSells, parties, { it.buyerId }, ::sellDuePaisa, { it.entryDate })
    }
    val expandedPaid = remember(query) { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (query.isNotBlank()) {
            if (searchGroups.isEmpty()) {
                item(key = "vyapari_search_empty") { SearchEmptyState(isEnglish) }
            } else {
                searchGroups.forEach { g ->
                    item(key = "vs_sum_${g.party.id}") {
                        PartySearchSummary(
                            name = g.party.name,
                            village = g.party.village,
                            phone = g.party.phone,
                            unpaidCount = g.unpaid.size,
                            totalDuePaisa = g.totalDuePaisa,
                            isReceivable = true,
                            isEnglish = isEnglish
                        )
                    }
                    items(g.unpaid, key = { "vs_u_${it.id}" }) { sell ->
                        VyapariSlipCard(
                            sell = sell,
                            buyerName = g.party.name,
                            onOpenReceipt = { onOpenReceipt(sell.id) },
                            onSettleCash = if (onSettleSellSlipCash != null) { { settlingSell = sell } } else null,
                            isEnglish = isEnglish
                        )
                    }
                    if (g.paid.isNotEmpty()) {
                        val open = expandedPaid[g.party.id] == true
                        item(key = "vs_fold_${g.party.id}") {
                            PaidSlipsFold(g.paid.size, open, { expandedPaid[g.party.id] = !open }, isEnglish)
                        }
                        if (open) {
                            items(g.paid, key = { "vs_p_${it.id}" }) { sell ->
                                Box(Modifier.alpha(0.6f)) {
                                    VyapariSlipCard(
                                        sell = sell,
                                        buyerName = g.party.name,
                                        onOpenReceipt = { onOpenReceipt(sell.id) },
                                        onSettleCash = null,
                                        isEnglish = isEnglish
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
        // Top 3 Cards
        item(key = "vyapari_kpi_cards") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ParchiKpiCard(
                    title = if (isEnglish) "Mandi Turnover" else "कुल टर्नओवर",
                    value = "₹${MandiMathEngine.formatIndianGrouping(totalTurnoverPaisa / 100)}",
                    subtitle = if (isEnglish) "Total billed" else "कुल बिल राशि",
                    modifier = Modifier.weight(1f)
                )
                ParchiKpiCard(
                    title = if (isEnglish) "Total Volume" else "कुल मात्रा",
                    value = "${MandiMathEngine.gramsToQuintals(totalVolumeGrams)} Q",
                    subtitle = if (isEnglish) "Quintals" else "क्विंटल",
                    modifier = Modifier.weight(1f)
                )
                ParchiKpiCard(
                    title = if (isEnglish) "Base Value" else "बेस मूल्य",
                    value = "₹${MandiMathEngine.formatIndianGrouping(totalBaseValuePaisa / 100)}",
                    subtitle = if (isEnglish) "Excl. charges" else "बिना आढ़त/शुल्क",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (dateGroupedEntries.isEmpty()) {
            item(key = "vyapari_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEnglish) "No buyer slips recorded" else "कोई व्यापारी पर्ची दर्ज नहीं है",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MandiTextMuted
                    )
                }
            }
        } else {
            dateGroupedEntries.forEach { (date, slips) ->
                item(key = "header_sell_${date}") {
                    DateSectionHeader(date = date, isEnglish = isEnglish)
                }
                items(slips, key = { it.id }) { sell ->
                    VyapariSlipCard(
                        sell = sell,
                        buyerName = partyNamesById[sell.buyerId] ?: sell.buyerId,
                        onOpenReceipt = { onOpenReceipt(sell.id) },
                        onSettleCash = if (onSettleSellSlipCash != null) { { settlingSell = sell } } else null,
                        isEnglish = isEnglish
                    )
                }
            }
        }
        }
    }

    settlingSell?.let { sell ->
        TraderSlipCashSettlementSheet(
            sell = sell,
            traderName = partyNamesById[sell.buyerId] ?: sell.buyerId,
            gallaCashPaisa = gallaCashPaisa,
            onDismiss = { settlingSell = null },
            onConfirmReceipt = { amountPaisa ->
                onSettleSellSlipCash?.invoke(sell.id, amountPaisa)
                settlingSell = null
            },
            isEnglish = isEnglish
        )
    }
}

@Composable
private fun ParchiKpiCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MandiSurfaceElevated),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MandiBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MandiTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = MandiTextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DateSectionHeader(
    date: LocalDate,
    isEnglish: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatLocalDate(date, isEnglish),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MandiTextSecondary
        )
        Spacer(modifier = Modifier.size(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MandiBorder)
        )
    }
}

@Composable
private fun KisaanSlipCard(
    buy: BuyEntry,
    farmerName: String,
    onOpenReceipt: () -> Unit,
    onSettleCash: (() -> Unit)? = null,
    isEnglish: Boolean
) {
    val displayFarmerName = farmerName.ifBlank { if (isEnglish) "Farmer" else "किसान" }
    val cropLabel = buy.commodityId.ifBlank { if (isEnglish) "Crop" else "फसल" }
    val quintals = MandiMathEngine.gramsToQuintals(buy.totalWeightGrams)
    val rateRupees = buy.ratePaisaPerQuintal / 100
    val isFullyPaid = buy.paidPaisa >= buy.netPayablePaisa
    val remainingPaisa = (buy.netPayablePaisa - buy.paidPaisa).coerceAtLeast(0L)
    val remainingRupees = remainingPaisa / 100

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpenReceipt),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MandiBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Farmer Name & Slip # (Left), Net Amount & Status Badge (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = displayFarmerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MandiSurfaceElevated
                        ) {
                            Text(
                                text = "#${slipLabel(buy.slipNo, buy.id)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MandiTextSecondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$cropLabel • ${buy.bagsCount} बोरी ($quintals Q @ ₹${MandiMathEngine.formatIndianGrouping(rateRupees)}/Q)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MandiTextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${MandiMathEngine.formatIndianGrouping(buy.netPayablePaisa / 100)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MandiTextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            isFullyPaid -> MandiGreenLight
                            buy.paidPaisa > 0 -> MandiAmberLight
                            else -> MandiRedLight
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when {
                                isFullyPaid -> MandiGreenBorder
                                buy.paidPaisa > 0 -> MandiAmberBorder
                                else -> MandiRedBorder
                            }
                        )
                    ) {
                        Text(
                            text = when {
                                isFullyPaid -> if (isEnglish) "✓ Paid" else "✓ चुक्ता"
                                else -> if (isEnglish) "Due ₹${MandiMathEngine.formatIndianGrouping(remainingRupees)}" else "बाकी ₹${MandiMathEngine.formatIndianGrouping(remainingRupees)}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isFullyPaid -> MandiGreenText
                                buy.paidPaisa > 0 -> MandiAmberDark
                                else -> MandiRedText
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Bottom Action Row if unpaid / partial
            if (!isFullyPaid && onSettleCash != null) {
                HorizontalDivider(color = MandiBorder, thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (buy.paidPaisa > 0) {
                        Text(
                            text = if (isEnglish) "Paid: ₹${MandiMathEngine.formatIndianGrouping(buy.paidPaisa / 100)}" else "पूर्व भुगतान: ₹${MandiMathEngine.formatIndianGrouping(buy.paidPaisa / 100)}",
                            fontSize = 11.sp,
                            color = MandiTextSecondary
                        )
                    } else {
                        Text(
                            text = if (isEnglish) "Tap card for details" else "विवरण देखने के लिए पर्चा खोलें",
                            fontSize = 11.sp,
                            color = MandiTextMuted
                        )
                    }

                    Button(
                        onClick = onSettleCash,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MandiPrimaryAction,
                            contentColor = MandiPrimaryActionText
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Pay Cash →" else "नकद दें →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VyapariSlipCard(
    sell: SellEntry,
    buyerName: String,
    onOpenReceipt: () -> Unit,
    onSettleCash: (() -> Unit)? = null,
    isEnglish: Boolean
) {
    val isFullyReceived = sell.receivedPaisa >= sell.totalReceivablePaisa
    val remainingPaisa = (sell.totalReceivablePaisa - sell.receivedPaisa).coerceAtLeast(0L)
    val remainingRupees = remainingPaisa / 100
    // Slip amount WITHOUT majdoori and commission:
    // This is grossPaisa (base weight x rate), excluding arat, mandi shulk and tulak
    val baseValueWithoutChargesPaisa = sell.grossPaisa
    val displayBuyerName = buyerName.ifBlank { if (isEnglish) "Buyer" else "व्यापारी" }
    val cropLabel = sell.commodityId.ifBlank { if (isEnglish) "Crop" else "फसल" }
    val quintals = MandiMathEngine.gramsToQuintals(sell.totalWeightGrams)
    val rateRupees = sell.ratePaisaPerQuintal / 100

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpenReceipt),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MandiBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayBuyerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "पर्चा #${slipLabel(sell.slipNo, sell.id)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MandiTextMuted
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${MandiMathEngine.formatIndianGrouping(baseValueWithoutChargesPaisa / 100)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MandiPrimaryAction
                    )
                    Text(
                        text = if (isEnglish) "Base (excl. cuts)" else "बिना आढ़त/मजदूरी",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MandiTextMuted
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$cropLabel • ${sell.bagsCount} बोरी • $quintals क्विंटल",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MandiTextSecondary
                )
                Text(
                    text = "@ ₹${MandiMathEngine.formatIndianGrouping(rateRupees)}/Q",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MandiTextMuted
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (sell.receivedPaisa > 0L) {
                        if (isEnglish) "Received: ₹${MandiMathEngine.formatIndianGrouping(sell.receivedPaisa / 100)}" else "पूर्व प्राप्त: ₹${MandiMathEngine.formatIndianGrouping(sell.receivedPaisa / 100)}"
                    } else {
                        if (isEnglish) "Total ₹${MandiMathEngine.formatIndianGrouping(sell.totalReceivablePaisa / 100)}" else "कुल ₹${MandiMathEngine.formatIndianGrouping(sell.totalReceivablePaisa / 100)}"
                    },
                    fontSize = 11.sp,
                    color = MandiTextSecondary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        isFullyReceived -> MandiGreenLight
                        sell.receivedPaisa > 0 -> MandiAmberLight
                        else -> MandiRedLight
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when {
                            isFullyReceived -> MandiGreenBorder
                            sell.receivedPaisa > 0 -> MandiAmberBorder
                            else -> MandiRedBorder
                        }
                    )
                ) {
                    Text(
                        text = when {
                            isFullyReceived -> if (isEnglish) "✓ Received" else "✓ चुक्ता"
                            else -> if (isEnglish) "Due ₹${MandiMathEngine.formatIndianGrouping(remainingRupees)}" else "बाकी ₹${MandiMathEngine.formatIndianGrouping(remainingRupees)}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isFullyReceived -> MandiGreenText
                            sell.receivedPaisa > 0 -> MandiAmberDark
                            else -> MandiRedText
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (!isFullyReceived && onSettleCash != null) {
                HorizontalDivider(color = MandiBorder, thickness = 1.dp)
                Button(
                    onClick = onSettleCash,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MandiPrimaryAction,
                        contentColor = MandiPrimaryActionText
                    ),
                    modifier = Modifier
                        .align(Alignment.End)
                        .height(34.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Receive Cash →" else "नकद लें →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ParchiSearchBox(query: String, onQueryChange: (String) -> Unit, isEnglish: Boolean) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MandiTextMuted) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = if (isEnglish) "Clear search" else "खोज साफ़ करें",
                        tint = MandiTextMuted
                    )
                }
            }
        },
        placeholder = {
            Text(
                if (isEnglish) "Search name, village or phone" else "नाम, गाँव या फ़ोन खोजें",
                color = MandiTextMuted
            )
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun PartySearchSummary(
    name: String,
    village: String?,
    phone: String?,
    unpaidCount: Int,
    totalDuePaisa: Long,
    isReceivable: Boolean,
    isEnglish: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MandiTextPrimary)
                val sub = listOfNotNull(village, phone).joinToString(" · ")
                if (sub.isNotEmpty()) Text(sub, fontSize = 12.sp, color = MandiTextMuted)
                Text(
                    if (isEnglish) "$unpaidCount unpaid ${if (unpaidCount == 1) "slip" else "slips"}" else "$unpaidCount पर्ची बाकी",
                    fontSize = 12.sp,
                    color = MandiTextMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (isReceivable) {
                        if (isEnglish) "Total to receive" else "कुल लेना है"
                    } else {
                        if (isEnglish) "Total to pay" else "कुल देना है"
                    },
                    fontSize = 12.sp,
                    color = MandiTextMuted
                )
                Text(
                    "₹${MandiMathEngine.formatIndianGrouping(totalDuePaisa / 100)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MandiTextPrimary
                )
            }
        }
    }
}

@Composable
private fun PaidSlipsFold(count: Int, expanded: Boolean, onToggle: () -> Unit, isEnglish: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MandiBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (isEnglish) "Paid slips ($count)" else "चुक्ता पर्ची ($count)",
            fontSize = 13.sp,
            color = MandiTextMuted
        )
        Icon(
            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MandiTextMuted
        )
    }
}

@Composable
private fun SearchEmptyState(isEnglish: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (isEnglish) "No slips found for this name or phone" else "इस नाम/फ़ोन से कोई पर्ची नहीं मिली",
            style = MaterialTheme.typography.bodyLarge,
            color = MandiTextMuted
        )
    }
}

private fun epochToLocalDate(epochMillis: Long): LocalDate {
    return Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
}

private fun formatLocalDate(date: LocalDate, isEnglish: Boolean): String {
    val monthNamesEn = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val monthNamesHi = listOf("जनवरी", "फरवरी", "मार्च", "अप्रैल", "मई", "जून", "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर")
    val monthStr = if (isEnglish) {
        monthNamesEn.getOrElse(date.monthNumber - 1) { date.month.name.take(3) }
    } else {
        monthNamesHi.getOrElse(date.monthNumber - 1) { date.month.name }
    }
    return "${date.dayOfMonth} $monthStr ${date.year}"
}

