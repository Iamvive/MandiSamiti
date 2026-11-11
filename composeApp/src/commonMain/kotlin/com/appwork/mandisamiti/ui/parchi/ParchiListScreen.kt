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
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.slipLabel
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
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
    onOpenBuyReceipt: (String) -> Unit,
    onOpenSellReceipt: (String) -> Unit,
    onNavigateToNewBuy: () -> Unit,
    onNavigateToNewSell: () -> Unit,
    isEnglish: Boolean = false,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

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

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> KisaanParchiView(
                        buys = buys,
                        partyNamesById = partyNamesById,
                        onOpenReceipt = onOpenBuyReceipt,
                        isEnglish = isEnglish
                    )
                    1 -> VyapariParchiView(
                        sells = sells,
                        partyNamesById = partyNamesById,
                        onOpenReceipt = onOpenSellReceipt,
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
    partyNamesById: Map<String, String>,
    onOpenReceipt: (String) -> Unit,
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

    val dateGroupedEntries = remember(nonVoidBuys) {
        nonVoidBuys
            .sortedByDescending { it.entryDate }
            .groupBy { epochToLocalDate(it.entryDate) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
                        isEnglish = isEnglish
                    )
                }
            }
        }
    }
}

@Composable
private fun VyapariParchiView(
    sells: List<SellEntry>,
    partyNamesById: Map<String, String>,
    onOpenReceipt: (String) -> Unit,
    isEnglish: Boolean
) {
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
                        isEnglish = isEnglish
                    )
                }
            }
        }
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
    isEnglish: Boolean
) {
    // Slip amount WITHOUT majdoori and commission:
    // This is grossPaisa (base weight x rate)
    val baseValueWithoutChargesPaisa = buy.grossPaisa
    val displayFarmerName = farmerName.ifBlank { if (isEnglish) "Farmer" else "किसान" }
    val cropLabel = buy.commodityId.ifBlank { if (isEnglish) "Crop" else "फसल" }
    val quintals = MandiMathEngine.gramsToQuintals(buy.totalWeightGrams)
    val rateRupees = buy.ratePaisaPerQuintal / 100

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
                        text = displayFarmerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "पर्चा #${slipLabel(buy.slipNo, buy.id)}",
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
                        text = if (isEnglish) "Base (excl. cuts)" else "बिना मजदूरी/कमीशन",
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
                    text = "$cropLabel • ${buy.bagsCount} बोरी • $quintals क्विंटल",
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
        }
    }
}

@Composable
private fun VyapariSlipCard(
    sell: SellEntry,
    buyerName: String,
    onOpenReceipt: () -> Unit,
    isEnglish: Boolean
) {
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
        }
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

