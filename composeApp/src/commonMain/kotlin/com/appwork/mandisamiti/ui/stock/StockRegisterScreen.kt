package com.appwork.mandisamiti.ui.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BuyCropLot
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.CommodityParser
import com.appwork.mandisamiti.domain.model.SellCropLot
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.StockLine
import com.appwork.mandisamiti.domain.model.slipLabel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json

private enum class StockSort { ON_HAND_DESC, NAME }

/** Dedicated Godown tab: summary, search, grain filter, sort and a per-grain table with variety drill-down. */
@OptIn(ExperimentalLayoutApi::class)
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
        .filter { query.isBlank() || it.commodityId.contains(query.trim(), ignoreCase = true) }
        .let { list ->
            when (sort) {
                StockSort.ON_HAND_DESC -> list.sortedByDescending { it.onHandGrams }
                StockSort.NAME -> list.sortedBy { it.commodityId.lowercase() }
            }
        }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 0.dp),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (isEnglish) "Godown Stock Register" else "गोदाम स्टॉक रजिस्टर",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val bought = visible.sumOf { it.boughtGrams }
                    val sold = visible.sumOf { it.soldGrams }
                    val oversold = visible.count { it.onHandGrams < 0 }
                    Text(
                        (if (isEnglish) "Bought " else "खरीद ") + "${MandiMathEngine.gramsToQuintals(bought)} qtl  |  " +
                            (if (isEnglish) "Sold " else "बिक्री ") + "${MandiMathEngine.gramsToQuintals(sold)} qtl  |  " +
                            (if (isEnglish) "On hand " else "शेष ") + "${MandiMathEngine.gramsToQuintals(bought - sold)} qtl",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (oversold > 0) {
                        Text(
                            if (isEnglish) "$oversold grain(s) sold more than stock" else "$oversold जिंस में स्टॉक से ज्यादा बिक्री",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text(if (isEnglish) "Search grain..." else "जिंस खोजें...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = grain == null,
                    onClick = { grain = null },
                    label = { Text(if (isEnglish) "All" else "सभी") }
                )
                stockLines.forEach { line ->
                    FilterChip(
                        selected = grain == line.commodityId,
                        onClick = { grain = line.commodityId },
                        label = { Text(line.commodityId) }
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (isEnglish) "Sort:" else "क्रम:", style = MaterialTheme.typography.bodySmall)
                FilterChip(
                    selected = sort == StockSort.ON_HAND_DESC,
                    onClick = { sort = StockSort.ON_HAND_DESC },
                    label = { Text(if (isEnglish) "Most stock" else "ज्यादा स्टॉक") }
                )
                FilterChip(
                    selected = sort == StockSort.NAME,
                    onClick = { sort = StockSort.NAME },
                    label = { Text(if (isEnglish) "Name" else "नाम") }
                )
            }
        }

        if (visible.isEmpty()) {
            item {
                Text(
                    if (stockLines.isEmpty()) {
                        if (isEnglish) "No stock recorded yet." else "अभी कोई स्टॉक दर्ज नहीं है।"
                    } else {
                        if (isEnglish) "No grain matches the filter." else "फ़िल्टर से कोई जिंस नहीं मिली।"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(visible, key = { it.commodityId }) { line -> StockRegisterRow(line, isEnglish, bills[line.commodityId].orEmpty(), onOpenBuy, onOpenSell) }
        }
    }
}

@Composable
private fun StockRegisterRow(
    line: StockLine,
    isEnglish: Boolean,
    bills: List<StockBill>,
    onOpenBuy: (String) -> Unit,
    onOpenSell: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val negative = line.onHandGrams < 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (negative) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        else CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier
                .clickable(enabled = line.varieties.isNotEmpty() || bills.isNotEmpty()) { expanded = !expanded }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    line.commodityId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    (if (isEnglish) "On hand " else "शेष ") + "${MandiMathEngine.gramsToQuintals(line.onHandGrams)} qtl",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (negative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                if (line.varieties.isNotEmpty() || bills.isNotEmpty()) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            }
            Text(
                (if (isEnglish) "Bought " else "खरीद ") + "${MandiMathEngine.gramsToQuintals(line.boughtGrams)} qtl  |  " +
                    (if (isEnglish) "Sold " else "बिक्री ") + "${MandiMathEngine.gramsToQuintals(line.soldGrams)} qtl",
                style = MaterialTheme.typography.bodySmall
            )
            if (negative) {
                Text(
                    if (isEnglish) "Sold more than stock - check entries" else "स्टॉक से ज्यादा बिक्री - एंट्री जाँचें",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (expanded) {
                line.varieties.forEach { v ->
                    Row(Modifier.fillMaxWidth().padding(start = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(v.varietyName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text(
                            (if (isEnglish) "In " else "खरीद ") + MandiMathEngine.gramsToQuintals(v.boughtGrams) +
                                (if (isEnglish) " | Out " else " | बिक्री ") + MandiMathEngine.gramsToQuintals(v.soldGrams) +
                                (if (isEnglish) " | Left " else " | शेष ") + MandiMathEngine.gramsToQuintals(v.onHandGrams) + " qtl",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (v.onHandGrams < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                        )
                    }
                }
                if (bills.isNotEmpty()) {
                    Text(
                        if (isEnglish) "Bills (tap to open, edit from there)" else "बिल (खोलने के लिए टैप करें, वहीं से सुधारें)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    bills.take(30).forEach { b ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { if (b.isBuy) onOpenBuy(b.id) else onOpenSell(b.id) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    (if (b.isBuy) (if (isEnglish) "IN  " else "आवक  ") else (if (isEnglish) "OUT  " else "बिक्री  ")) + b.partyName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text("#${b.slip}  |  ${b.date}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("${MandiMathEngine.gramsToQuintals(b.grams)} qtl", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = if (isEnglish) "Open to edit" else "खोलकर सुधारें",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
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

private fun buildBills(buys: List<BuyEntry>, sells: List<SellEntry>, names: Map<String, String>): Map<String, List<StockBill>> {
    val json = Json { ignoreUnknownKeys = true }
    fun day(ms: Long) = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    val out = mutableMapOf<String, MutableList<StockBill>>()
    buys.filter { !it.isVoid }.forEach { e ->
        val raw = e.remarks?.takeIf { it.contains("DATA: ") }?.substringAfter("DATA: ")?.trim() ?: return@forEach
        val lots = runCatching { json.decodeFromString<List<BuyCropLot>>(raw) }.getOrNull() ?: return@forEach
        lots.groupBy { CommodityParser.parseBaseCommodity(it.cropName) }.forEach { (base, ls) ->
            out.getOrPut(base) { mutableListOf() }.add(
                StockBill(e.id, true, names[e.farmerId] ?: e.farmerId, slipLabel(e.slipNo, e.id), day(e.entryDate), ls.sumOf { it.totalWeightGrams }, e.entryDate)
            )
        }
    }
    sells.filter { !it.isVoid }.forEach { e ->
        val raw = e.remarks ?: return@forEach
        val lots = runCatching { json.decodeFromString<List<SellCropLot>>(raw) }.getOrNull() ?: return@forEach
        lots.groupBy { CommodityParser.parseBaseCommodity(it.cropName) }.forEach { (base, ls) ->
            out.getOrPut(base) { mutableListOf() }.add(
                StockBill(e.id, false, names[e.buyerId] ?: e.buyerId, slipLabel(e.slipNo, e.id), day(e.entryDate), ls.sumOf { it.totalWeightGrams }, e.entryDate)
            )
        }
    }
    return out.mapValues { (_, v) -> v.sortedByDescending { it.at } }
}
