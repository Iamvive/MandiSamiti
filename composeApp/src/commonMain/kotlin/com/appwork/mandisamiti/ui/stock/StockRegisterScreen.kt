package com.appwork.mandisamiti.ui.stock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BuyCropLot
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.CommodityParser
import com.appwork.mandisamiti.domain.model.SellCropLot
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.StockLine
import com.appwork.mandisamiti.domain.model.StockVarietyLine
import com.appwork.mandisamiti.domain.model.slipLabel
import com.appwork.mandisamiti.ui.theme.MandiBlueSell
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenLight
import com.appwork.mandisamiti.ui.theme.MandiGreenReceivable
import com.appwork.mandisamiti.ui.theme.MandiGreenText
import com.appwork.mandisamiti.ui.theme.MandiNavy
import com.appwork.mandisamiti.ui.theme.MandiRedBorder
import com.appwork.mandisamiti.ui.theme.MandiRedLight
import com.appwork.mandisamiti.ui.theme.MandiRedPayable
import com.appwork.mandisamiti.ui.theme.MandiRedText
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.math.abs

private enum class StockSort { ON_HAND_DESC, NAME }

/**
 * Dedicated Godown Stock Register Tab (गोदाम स्टॉक रजिस्टर).
 * Conforms strictly to NGDL v1.2 Design Tokens and Warehouse Ledger Ergonomics.
 */
@Composable
fun StockRegisterScreen(
    stockLines: List<StockLine>,
    isEnglish: Boolean,
    buys: List<BuyEntry> = emptyList(),
    sells: List<SellEntry> = emptyList(),
    partyNames: Map<String, String> = emptyMap(),
    onOpenBuy: (String) -> Unit = {},
    onOpenSell: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bills = remember(buys, sells, partyNames) { buildBills(buys, sells, partyNames) }
    var query by remember { mutableStateOf("") }
    var grain by remember { mutableStateOf<String?>(null) }
    var sort by remember { mutableStateOf(StockSort.ON_HAND_DESC) }

    val visible = stockLines
        .filter { grain == null || it.commodityId == grain }
        .filter {
            query.isBlank() ||
                it.commodityId.contains(query.trim(), ignoreCase = true) ||
                it.varieties.any { v -> v.varietyName.contains(query.trim(), ignoreCase = true) }
        }
        .let { list ->
            when (sort) {
                StockSort.ON_HAND_DESC -> list.sortedByDescending { it.onHandGrams }
                StockSort.NAME -> list.sortedBy { it.commodityId.lowercase() }
            }
        }

    val totalBought = stockLines.sumOf { it.boughtGrams }
    val totalSold = stockLines.sumOf { it.soldGrams }
    val totalOnHand = totalBought - totalSold
    val oversoldGrains = stockLines.count { it.onHandGrams < 0 }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Warehouse KPI Summary Card
        item(key = "godown_kpi_hero") {
            WarehouseKpiCard(
                totalOnHand = totalOnHand,
                totalBought = totalBought,
                totalSold = totalSold,
                commodityCount = stockLines.size,
                oversoldGrains = oversoldGrains,
                isEnglish = isEnglish
            )
        }

        // 2. Search Field
        item(key = "godown_search_bar") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MandiTextSecondary
                    )
                },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = if (isEnglish) "Clear search" else "खोज हटाएं",
                                tint = MandiTextSecondary
                            )
                        }
                    }
                },
                placeholder = {
                    Text(
                        if (isEnglish) "Search commodity or variety..." else "जिंस या किस्म खोजें...",
                        color = MandiTextMuted
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MandiSurface,
                    unfocusedContainerColor = MandiSurface,
                    focusedBorderColor = MandiNavy,
                    unfocusedBorderColor = MandiBorder,
                    focusedTextColor = MandiTextPrimary,
                    unfocusedTextColor = MandiTextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 3. Horizontal Commodity Quick Filter
        item(key = "godown_quick_filter") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = grain == null,
                        onClick = { grain = null },
                        label = {
                            Text(
                                if (isEnglish) "All (${stockLines.size})" else "सभी (${stockLines.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (grain == null) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MandiNavy,
                            selectedLabelColor = MandiSurface,
                            containerColor = MandiSurfaceElevated,
                            labelColor = MandiTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = grain == null,
                            borderColor = MandiBorder,
                            selectedBorderColor = MandiNavy
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                items(stockLines, key = { it.commodityId }) { line ->
                    val isSelected = grain == line.commodityId
                    FilterChip(
                        selected = isSelected,
                        onClick = { grain = if (isSelected) null else line.commodityId },
                        label = {
                            Text(
                                "${line.commodityId} (${formatQuintals(line.onHandGrams)} Q)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MandiNavy,
                            selectedLabelColor = MandiSurface,
                            containerColor = MandiSurfaceElevated,
                            labelColor = MandiTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MandiBorder,
                            selectedBorderColor = MandiNavy
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        // 4. Sort and Results Count Header
        item(key = "godown_sort_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = null,
                    tint = MandiTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    if (isEnglish) "Sort:" else "क्रम:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MandiTextSecondary
                )
                FilterChip(
                    selected = sort == StockSort.ON_HAND_DESC,
                    onClick = { sort = StockSort.ON_HAND_DESC },
                    label = {
                        Text(
                            if (isEnglish) "Stock (High-Low)" else "ज्यादा स्टॉक",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MandiSurfaceElevated,
                        selectedLabelColor = MandiTextPrimary,
                        containerColor = MandiSurface,
                        labelColor = MandiTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = sort == StockSort.ON_HAND_DESC,
                        borderColor = MandiBorder,
                        selectedBorderColor = MandiNavy
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = sort == StockSort.NAME,
                    onClick = { sort = StockSort.NAME },
                    label = {
                        Text(
                            if (isEnglish) "Name (A-Z)" else "नाम (A-Z)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MandiSurfaceElevated,
                        selectedLabelColor = MandiTextPrimary,
                        containerColor = MandiSurface,
                        labelColor = MandiTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = sort == StockSort.NAME,
                        borderColor = MandiBorder,
                        selectedBorderColor = MandiNavy
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                if (visible.size < stockLines.size) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (isEnglish) "${visible.size} of ${stockLines.size}" else "${visible.size}/${stockLines.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MandiTextMuted
                    )
                }
            }
        }

        // 5. Commodity Cards or Empty State
        if (visible.isEmpty()) {
            item(key = "godown_empty_state") {
                StockEmptyState(
                    hasStockInStore = stockLines.isNotEmpty(),
                    query = query,
                    isEnglish = isEnglish,
                    onResetFilter = {
                        query = ""
                        grain = null
                    }
                )
            }
        } else {
            items(visible, key = { it.commodityId }) { line ->
                StockRegisterCard(
                    line = line,
                    isEnglish = isEnglish,
                    bills = bills[line.commodityId].orEmpty(),
                    onOpenBuy = onOpenBuy,
                    onOpenSell = onOpenSell
                )
            }
        }
    }
}

/**
 * Top Warehouse Inventory KPI card with high contrast, split bought/sold tiles, and bag estimation.
 */
@Composable
private fun WarehouseKpiCard(
    totalOnHand: Long,
    totalBought: Long,
    totalSold: Long,
    commodityCount: Int,
    oversoldGrains: Int,
    isEnglish: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        border = BorderStroke(1.dp, MandiBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Warehouse Icon + Title + Tracked Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MandiSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MandiNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            if (isEnglish) "Godown Stock Register" else "गोदाम स्टॉक रजिस्टर",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                        Text(
                            if (isEnglish) "Live stock & inventory balance" else "लाइव स्टॉक एवं आवक-बिक्री ऑडिट",
                            style = MaterialTheme.typography.bodySmall,
                            color = MandiTextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MandiSurfaceElevated
                ) {
                    Text(
                        if (isEnglish) "$commodityCount Commodities" else "$commodityCount जिंस दर्ज",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MandiTextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Total On-Hand Metric Banner
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (isEnglish) "TOTAL ON-HAND STOCK" else "कुल उपलब्ध स्टॉक (शेष)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextSecondary,
                    letterSpacing = 0.5.sp
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "${formatQuintals(totalOnHand)} Qtl",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (totalOnHand < 0) MandiRedPayable else MandiNavy
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (totalOnHand < 0) MandiRedLight else MandiSurfaceElevated,
                        border = if (totalOnHand < 0) BorderStroke(1.dp, MandiRedBorder) else null,
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Text(
                            text = formatBags(totalOnHand, isEnglish),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (totalOnHand < 0) MandiRedText else MandiTextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Split Inflow vs Outflow Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Bought (In)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MandiSurfaceElevated)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MandiGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = MandiGreenReceivable,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                if (isEnglish) "Total Bought" else "कुल आवक",
                                style = MaterialTheme.typography.labelSmall,
                                color = MandiTextSecondary
                            )
                        }
                        Text(
                            "${formatQuintals(totalBought)} Qtl",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MandiGreenText
                        )
                        Text(
                            formatBags(totalBought, isEnglish),
                            style = MaterialTheme.typography.labelSmall,
                            color = MandiTextMuted
                        )
                    }
                }

                // Total Sold (Out)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MandiSurfaceElevated)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MandiRedLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = MandiRedPayable,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                if (isEnglish) "Total Sold" else "कुल बिक्री",
                                style = MaterialTheme.typography.labelSmall,
                                color = MandiTextSecondary
                            )
                        }
                        Text(
                            "${formatQuintals(totalSold)} Qtl",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MandiRedText
                        )
                        Text(
                            formatBags(totalSold, isEnglish),
                            style = MaterialTheme.typography.labelSmall,
                            color = MandiTextMuted
                        )
                    }
                }
            }

            // Oversold Warning Banner (if any grain sold > bought)
            if (oversoldGrains > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MandiRedLight,
                    border = BorderStroke(1.dp, MandiRedBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MandiRedPayable,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isEnglish) {
                                "$oversoldGrains commodity has negative stock (sales exceed bought)"
                            } else {
                                "$oversoldGrains जिंस में स्टॉक से ज्यादा बिक्री दर्ज है (पर्चे जांचें)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MandiRedText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Commodity Stock Card with in/out progress, variety drill-down, and expandable recent bills drawer.
 */
@Composable
private fun StockRegisterCard(
    line: StockLine,
    isEnglish: Boolean,
    bills: List<StockBill>,
    onOpenBuy: (String) -> Unit,
    onOpenSell: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showAllBills by remember { mutableStateOf(false) }
    val negative = line.onHandGrams < 0

    val soldRatio = if (line.boughtGrams > 0) {
        (line.soldGrams.toFloat() / line.boughtGrams.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        border = BorderStroke(1.dp, if (negative) MandiRedBorder else MandiBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Commodity icon, Name + Variety Count, On-Hand Badge + Expand Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (negative) MandiRedLight else MandiSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = if (negative) MandiRedPayable else MandiNavy,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = line.commodityId,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (line.varieties.size > 1) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MandiSurfaceElevated
                            ) {
                                Text(
                                    text = if (isEnglish) "${line.varieties.size} varieties" else "${line.varieties.size} किस्में",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MandiTextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = (if (isEnglish) "In: " else "आवक: ") + "${formatQuintals(line.boughtGrams)} Q  •  " +
                            (if (isEnglish) "Out: " else "बिक्री: ") + "${formatQuintals(line.soldGrams)} Q",
                        style = MaterialTheme.typography.bodySmall,
                        color = MandiTextSecondary
                    )
                }

                Spacer(Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (negative) MandiRedLight else MandiGreenLight,
                        border = BorderStroke(1.dp, if (negative) MandiRedBorder else MandiGreenBorder)
                    ) {
                        Text(
                            text = "${if (line.onHandGrams > 0) "+" else ""}${formatQuintals(line.onHandGrams)} Q",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (negative) MandiRedText else MandiGreenText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = formatBags(line.onHandGrams, isEnglish),
                        style = MaterialTheme.typography.labelSmall,
                        color = MandiTextMuted
                    )
                }

                Spacer(Modifier.width(6.dp))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MandiTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Inflow vs Outflow Mini Progress Bar (if bought > 0)
            if (line.boughtGrams > 0 && !negative) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MandiSurfaceElevated)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = soldRatio)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(MandiNavy)
                        )
                    }

                    val soldPct = (soldRatio * 100).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (isEnglish) "$soldPct% sold" else "$soldPct% बिक्री हुई",
                            style = MaterialTheme.typography.labelSmall,
                            color = MandiTextMuted
                        )
                        Text(
                            if (isEnglish) "${100 - soldPct}% on hand" else "${100 - soldPct}% शेष",
                            style = MaterialTheme.typography.labelSmall,
                            color = MandiTextMuted
                        )
                    }
                }
            }

            // Oversold alert within the commodity card
            if (negative) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MandiRedLight,
                    border = BorderStroke(1.dp, MandiRedBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MandiRedPayable,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isEnglish) {
                                "Oversold by ${formatQuintals(-line.onHandGrams)} Qtl! Check sales entries."
                            } else {
                                "स्टॉक से ${formatQuintals(-line.onHandGrams)} क्विंटल अधिक बिक्री! बिक्री पर्चे जांचें।"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MandiRedText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Expanded Section: Varieties & Bills
            if (expanded) {
                HorizontalDivider(color = MandiBorder, thickness = 1.dp)

                // 1. Variety Drill-down
                if (line.varieties.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isEnglish) "VARIETY BREAKDOWN" else "किस्मवार स्टॉक विवरण",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextSecondary,
                            letterSpacing = 0.5.sp
                        )

                        line.varieties.forEach { v ->
                            VarietyItemRow(v = v, isEnglish = isEnglish)
                        }
                    }
                }

                // 2. Recent Bills Drawer
                if (bills.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = MandiNavy,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isEnglish) "Recent Bills (${bills.size})" else "हाल के सौदे (${bills.size} पर्चे)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MandiTextPrimary
                                )
                            }

                            if (bills.size > 5) {
                                Text(
                                    text = if (showAllBills) {
                                        if (isEnglish) "Show less" else "कम दिखाएं"
                                    } else {
                                        if (isEnglish) "View all (${bills.size})" else "सभी देखें (${bills.size})"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MandiBlueSell,
                                    modifier = Modifier
                                        .clickable { showAllBills = !showAllBills }
                                        .padding(4.dp)
                                )
                            }
                        }

                        val displayedBills = if (showAllBills) bills else bills.take(5)
                        displayedBills.forEach { b ->
                            StockBillItemRow(
                                bill = b,
                                isEnglish = isEnglish,
                                onClick = {
                                    if (b.isBuy) onOpenBuy(b.id) else onOpenSell(b.id)
                                }
                            )
                        }
                    }
                }

                // 3. Fallback if no varieties and no bills recorded yet
                if (line.varieties.isEmpty() && bills.isEmpty()) {
                    Text(
                        text = if (isEnglish) "No detailed bills recorded for this commodity." else "इस जिंस के लिए कोई पर्चा दर्ज नहीं है।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MandiTextMuted,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Individual variety row with in/out summary and remaining balance pill.
 */
@Composable
private fun VarietyItemRow(v: StockVarietyLine, isEnglish: Boolean) {
    val isNeg = v.onHandGrams < 0
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MandiSurfaceElevated,
        border = BorderStroke(1.dp, MandiBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = v.varietyName.ifBlank { if (isEnglish) "General / Mixed" else "सामान्य / मिक्स" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MandiTextPrimary
                )
                Text(
                    text = (if (isEnglish) "In: " else "आवक: ") + "${formatQuintals(v.boughtGrams)} Q  |  " +
                        (if (isEnglish) "Out: " else "बिक्री: ") + "${formatQuintals(v.soldGrams)} Q",
                    style = MaterialTheme.typography.labelSmall,
                    color = MandiTextSecondary
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isNeg) MandiRedLight else MandiSurface,
                border = BorderStroke(1.dp, if (isNeg) MandiRedBorder else MandiBorder)
            ) {
                Text(
                    text = "${if (v.onHandGrams > 0) "+" else ""}${formatQuintals(v.onHandGrams)} Q",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isNeg) MandiRedText else MandiNavy,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Individual bill row with direction badge, party name, slip #, date, and tap action.
 */
@Composable
private fun StockBillItemRow(
    bill: StockBill,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MandiSurfaceElevated,
        border = BorderStroke(1.dp, MandiBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (bill.isBuy) MandiGreenLight else MandiRedLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (bill.isBuy) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (bill.isBuy) MandiGreenReceivable else MandiRedPayable,
                    modifier = Modifier.size(15.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = bill.partyName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MandiTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MandiSurface
                    ) {
                        Text(
                            text = "#${bill.slip}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MandiTextSecondary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "${if (bill.isBuy) (if (isEnglish) "Purchase" else "खरीद") else (if (isEnglish) "Sale" else "बिक्री")} • ${bill.date}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MandiTextMuted
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatQuintals(bill.grams)} Q",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                )
                Text(
                    text = formatBags(bill.grams, isEnglish),
                    style = MaterialTheme.typography.labelSmall,
                    color = MandiTextSecondary
                )
            }

            Spacer(Modifier.width(6.dp))

            Icon(
                Icons.Default.Edit,
                contentDescription = if (isEnglish) "Open slip to edit" else "पर्चा खोलें",
                tint = MandiTextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Empty state when no inventory is recorded or search produces 0 results.
 */
@Composable
private fun StockEmptyState(
    hasStockInStore: Boolean,
    query: String,
    isEnglish: Boolean,
    onResetFilter: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        border = BorderStroke(1.dp, MandiBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MandiSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = MandiTextSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = if (!hasStockInStore) {
                    if (isEnglish) "No Stock Recorded Yet" else "गोदाम में अभी कोई स्टॉक नहीं है"
                } else {
                    if (isEnglish) "No commodity matches \"$query\"" else "\"$query\" से कोई जिंस नहीं मिली"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (!hasStockInStore) {
                    if (isEnglish) {
                        "Enter new purchase entries in the Purchase tab. Godown inventory updates automatically."
                    } else {
                        "खरीद टैब में नई खरीद (आवक) दर्ज करें, गोदाम स्टॉक अपने आप अपडेट हो जाएगा।"
                    }
                } else {
                    if (isEnglish) {
                        "Try searching with a different commodity name or clear your filters."
                    } else {
                        "अलग नाम से खोजें या फ़िल्टर हटाकर पूरा स्टॉक देखें।"
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MandiTextSecondary,
                textAlign = TextAlign.Center
            )

            if (hasStockInStore) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onResetFilter,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MandiNavy)
                ) {
                    Text(
                        if (isEnglish) "Clear Filters" else "फ़िल्टर हटाएं",
                        color = MandiNavy,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Exact integer-based formatting for quintals (2 decimal places) without float precision drift.
 */
private fun formatQuintals(grams: Long): String {
    val isNeg = grams < 0
    val absGrams = abs(grams)
    val whole = absGrams / MandiMathEngine.GRAMS_PER_QUINTAL
    val fracTwo = ((absGrams % MandiMathEngine.GRAMS_PER_QUINTAL) / 1_000L)
        .toString()
        .padStart(2, '0')
        .take(2)
    val prefix = if (isNeg) "-" else ""
    return "$prefix$whole.$fracTwo"
}

/**
 * Standard mandi bag estimation (~50 kg / 0.50 Qtl per bag).
 */
private fun formatBags(grams: Long, isEnglish: Boolean): String {
    if (grams == 0L) return if (isEnglish) "~0 bags" else "~0 बोरी"
    val isNeg = grams < 0
    val absGrams = abs(grams)
    val bags = (absGrams + 25_000L) / 50_000L
    return if (isNeg) {
        if (isEnglish) "-$bags bags (Deficit)" else "-$bags बोरी (कमी)"
    } else {
        if (isEnglish) "~$bags bags" else "~$bags बोरी"
    }
}

private data class StockBill(
    val id: String,
    val isBuy: Boolean,
    val partyName: String,
    val slip: String,
    val date: String,
    val grams: Long,
    val at: Long
)

private fun buildBills(
    buys: List<BuyEntry>,
    sells: List<SellEntry>,
    names: Map<String, String>
): Map<String, List<StockBill>> {
    val json = Json { ignoreUnknownKeys = true }
    fun day(ms: Long) = Instant.fromEpochMilliseconds(ms)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()

    val out = mutableMapOf<String, MutableList<StockBill>>()

    buys.filter { !it.isVoid }.forEach { e ->
        val raw = e.remarks?.takeIf { it.contains("DATA: ") }?.substringAfter("DATA: ")?.trim()
        val lots = raw?.let { runCatching { json.decodeFromString<List<BuyCropLot>>(it) }.getOrNull() }
        if (!lots.isNullOrEmpty()) {
            lots.groupBy { CommodityParser.parseBaseCommodity(it.cropName) }.forEach { (base, ls) ->
                out.getOrPut(base) { mutableListOf() }.add(
                    StockBill(
                        id = e.id,
                        isBuy = true,
                        partyName = names[e.farmerId] ?: e.farmerId,
                        slip = slipLabel(e.slipNo, e.id),
                        date = day(e.entryDate),
                        grams = ls.sumOf { it.totalWeightGrams },
                        at = e.entryDate
                    )
                )
            }
        } else if (e.commodityId.isNotBlank() && e.totalWeightGrams > 0) {
            val base = CommodityParser.parseBaseCommodity(e.commodityId)
            out.getOrPut(base) { mutableListOf() }.add(
                StockBill(
                    id = e.id,
                    isBuy = true,
                    partyName = names[e.farmerId] ?: e.farmerId,
                    slip = slipLabel(e.slipNo, e.id),
                    date = day(e.entryDate),
                    grams = e.totalWeightGrams,
                    at = e.entryDate
                )
            )
        }
    }

    sells.filter { !it.isVoid }.forEach { e ->
        val raw = e.remarks?.trim()
        val lots = raw?.takeIf { it.startsWith("[") }?.let { runCatching { json.decodeFromString<List<SellCropLot>>(it) }.getOrNull() }
        if (!lots.isNullOrEmpty()) {
            lots.groupBy { CommodityParser.parseBaseCommodity(it.cropName) }.forEach { (base, ls) ->
                out.getOrPut(base) { mutableListOf() }.add(
                    StockBill(
                        id = e.id,
                        isBuy = false,
                        partyName = names[e.buyerId] ?: e.buyerId,
                        slip = slipLabel(e.slipNo, e.id),
                        date = day(e.entryDate),
                        grams = ls.sumOf { it.totalWeightGrams },
                        at = e.entryDate
                    )
                )
            }
        } else if (e.commodityId.isNotBlank() && e.totalWeightGrams > 0) {
            val base = CommodityParser.parseBaseCommodity(e.commodityId)
            out.getOrPut(base) { mutableListOf() }.add(
                StockBill(
                    id = e.id,
                    isBuy = false,
                    partyName = names[e.buyerId] ?: e.buyerId,
                    slip = slipLabel(e.slipNo, e.id),
                    date = day(e.entryDate),
                    grams = e.totalWeightGrams,
                    at = e.entryDate
                )
            )
        }
    }

    return out.mapValues { (_, v) -> v.sortedByDescending { it.at } }
}
