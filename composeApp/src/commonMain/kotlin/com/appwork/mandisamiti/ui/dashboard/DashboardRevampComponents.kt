package com.appwork.mandisamiti.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.StockLine

@Composable
fun RevampKpiGrid(
    kpis: DashboardKpis,
    onBuyClick: () -> Unit,
    onSellClick: () -> Unit,
    onLendClick: () -> Unit,
    onExpenseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard(
                label = "कुल खरीद",
                amountPaisa = kpis.totalBuyPaisa,
                onClick = onBuyClick,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                label = "कुल बिक्री",
                amountPaisa = kpis.totalSellPaisa,
                onClick = onSellClick,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard(
                label = "कुल उधार (बाकी)",
                amountPaisa = kpis.totalLendPaisa,
                onClick = onLendClick,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                label = "कुल खर्च",
                amountPaisa = kpis.totalExpensePaisa,
                onClick = onExpenseClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    amountPaisa: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "₹${MandiMathEngine.paisaToRupeesString(amountPaisa)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProfitLossCard(
    profitLossPaisa: Long,
    modifier: Modifier = Modifier
) {
    val isLoss = profitLossPaisa < 0
    val containerColor = if (isLoss) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (isLoss) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isLoss) "अनुमानित शुद्ध हानि (Loss)" else "अनुमानित शुद्ध लाभ (Profit)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isLoss) "- ₹${MandiMathEngine.paisaToRupeesString(-profitLossPaisa)}"
                           else "₹${MandiMathEngine.paisaToRupeesString(profitLossPaisa)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GodownStockSummaryCard(
    stockLines: List<StockLine>,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onViewAllClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "गोदाम स्टॉक (Godown Stock)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "सभी देखें")
            }

            if (stockLines.isEmpty()) {
                Text("कोई स्टॉक रिकॉर्ड नहीं", style = MaterialTheme.typography.bodySmall)
            } else {
                stockLines.take(3).forEach { line ->
                    val isNeg = line.onHandGrams < 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(line.commodityId, style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isNeg) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("स्टॉक से ज्यादा बिक्री") },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        labelColor = MaterialTheme.colorScheme.onError
                                    )
                                )
                            }
                            Text(
                                text = "${MandiMathEngine.gramsToQuintals(line.onHandGrams)} qtl",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isNeg) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
