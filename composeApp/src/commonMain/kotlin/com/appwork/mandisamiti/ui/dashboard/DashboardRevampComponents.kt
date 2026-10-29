package com.appwork.mandisamiti.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiCurrencyFormatter
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.StockLine

object DashboardStrings {
    fun kpiPurchases(isEnglish: Boolean) = if (isEnglish) "Total Purchases (All-time)" else "कुल खरीद (अब तक)"
    fun kpiSales(isEnglish: Boolean) = if (isEnglish) "Total Sales (All-time)" else "कुल बिक्री (अब तक)"
    fun kpiOutstanding(isEnglish: Boolean) = if (isEnglish) "Total Outstanding (All-time)" else "कुल उधार (अब तक)"
    fun kpiExpenses(isEnglish: Boolean) = if (isEnglish) "Total Expenses (All-time)" else "कुल खर्च (अब तक)"
    fun profitLossNetLoss(isEnglish: Boolean) = if (isEnglish) "Estimated Net Loss" else "अनुमानित शुद्ध हानि (Loss)"
    fun profitLossNetProfit(isEnglish: Boolean) = if (isEnglish) "Estimated Net Profit" else "अनुमानित शुद्ध लाभ (Profit)"
    fun godownTitle(isEnglish: Boolean) = if (isEnglish) "Godown Stock" else "गोदाम स्टॉक (Godown Stock)"
    fun godownViewAll(isEnglish: Boolean) = if (isEnglish) "View All" else "सभी देखें"
    fun godownEmpty(isEnglish: Boolean) = if (isEnglish) "No stock recorded" else "कोई स्टॉक रिकॉर्ड नहीं"
    fun godownOversold(isEnglish: Boolean) = if (isEnglish) "Oversold warning" else "स्टॉक से ज्यादा बिक्री"
    fun quickActionBuy(isEnglish: Boolean) = if (isEnglish) "Buy" else "खरीद"
    fun quickActionBuySub(isEnglish: Boolean) = if (isEnglish) "From Farmer" else "आवक"
    fun quickActionSell(isEnglish: Boolean) = if (isEnglish) "Sell" else "बिक्री"
    fun quickActionSellSub(isEnglish: Boolean) = if (isEnglish) "To Buyer" else "जावक"
    fun quickActionLend(isEnglish: Boolean) = if (isEnglish) "Lend" else "उधार"
    fun quickActionLendSub(isEnglish: Boolean) = if (isEnglish) "Advance" else "पेशगी"
    fun quickActionExpense(isEnglish: Boolean) = if (isEnglish) "Expense" else "खर्च"
    fun quickActionExpenseSub(isEnglish: Boolean) = if (isEnglish) "Mandi" else "मंडी व्यय"
    fun soundVoiceOn(isEnglish: Boolean) = if (isEnglish) "Voice On" else "वॉइस ऑन"
    fun soundMute(isEnglish: Boolean) = if (isEnglish) "Mute" else "म्यूट"
    fun metricReceivableTitle(isEnglish: Boolean) = if (isEnglish) "Receivable (Market)" else "कुल लेना (बाजार बाकी)"
    fun metricPayableTitle(isEnglish: Boolean) = if (isEnglish) "Payable (Farmers)" else "कुल देना (किसान जमा)"
    fun activeAccountsHeader(isEnglish: Boolean) = if (isEnglish) "Active Ledger Accounts" else "सक्रिय खाते (हाल का लेन-देन)"
    fun viewAll(isEnglish: Boolean) = if (isEnglish) "View All" else "सभी देखें"
    fun noAccountsEmpty(isEnglish: Boolean) = if (isEnglish) "No accounts added yet" else "अभी कोई खाता नहीं है"
}

@Composable
fun RevampKpiGrid(
    kpis: DashboardKpis,
    onBuyClick: () -> Unit,
    onSellClick: () -> Unit,
    onLendClick: () -> Unit,
    onExpenseClick: () -> Unit,
    isEnglish: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard(
                label = DashboardStrings.kpiPurchases(isEnglish),
                amountPaisa = kpis.totalBuyPaisa,
                onClick = onBuyClick,
                isEnglish = isEnglish,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KpiCard(
                label = DashboardStrings.kpiSales(isEnglish),
                amountPaisa = kpis.totalSellPaisa,
                onClick = onSellClick,
                isEnglish = isEnglish,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard(
                label = DashboardStrings.kpiOutstanding(isEnglish),
                amountPaisa = kpis.totalLendPaisa,
                onClick = onLendClick,
                isEnglish = isEnglish,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KpiCard(
                label = DashboardStrings.kpiExpenses(isEnglish),
                amountPaisa = kpis.totalExpensePaisa,
                onClick = onExpenseClick,
                isEnglish = isEnglish,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    amountPaisa: Long,
    onClick: () -> Unit,
    isEnglish: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showExact by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.clickable {
            showExact = !showExact
            onClick()
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (showExact) "₹${MandiMathEngine.paisaToRupeesString(amountPaisa)}"
                       else MandiCurrencyFormatter.formatCompactRupees(amountPaisa, isEnglish),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProfitLossCard(
    profitLossPaisa: Long,
    isEnglish: Boolean = false,
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
                    text = if (isLoss) DashboardStrings.profitLossNetLoss(isEnglish) else DashboardStrings.profitLossNetProfit(isEnglish),
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
    isEnglish: Boolean = false,
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
                    text = DashboardStrings.godownTitle(isEnglish),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = DashboardStrings.godownViewAll(isEnglish)
                )
            }

            if (stockLines.isEmpty()) {
                Text(
                    text = DashboardStrings.godownEmpty(isEnglish),
                    fontSize = 14.sp
                )
            } else {
                stockLines.take(3).forEach { line ->
                    val isNeg = line.onHandGrams < 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(line.commodityId, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isNeg) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(DashboardStrings.godownOversold(isEnglish), fontSize = 14.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        labelColor = MaterialTheme.colorScheme.onError
                                    )
                                )
                            }
                            Text(
                                text = "${MandiMathEngine.gramsToQuintals(line.onHandGrams)} qtl",
                                fontSize = 14.sp,
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
