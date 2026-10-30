package com.appwork.mandisamiti.ui.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.StockLine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockBreakdownSheet(
    stockLines: List<StockLine>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "गोदाम स्टॉक विवरण (Godown Inventory)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (stockLines.isEmpty()) {
                Text("वर्तमान में कोई स्टॉक उपलब्ध नहीं है।", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(stockLines, key = { it.commodityId }) { line ->
                        StockLineCard(line)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun StockLineCard(line: StockLine) {
    val isNegative = line.onHandGrams < 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isNegative) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                 else CardDefaults.cardColors()
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
                    text = line.commodityId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isNegative) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("स्टॉक से ज्यादा बिक्री") },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            labelColor = MaterialTheme.colorScheme.onError
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "खरीद: ${MandiMathEngine.gramsToQuintals(line.boughtGrams)} qtl • बिक्री: ${MandiMathEngine.gramsToQuintals(line.soldGrams)} qtl",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "शेष: ${MandiMathEngine.gramsToQuintals(line.onHandGrams)} qtl",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            if (line.varieties.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "वैरायटी विवरण (Varieties):",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                line.varieties.forEach { v ->
                    val vNegative = v.onHandGrams < 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• ${v.varietyName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "खरीद ${MandiMathEngine.gramsToQuintals(v.boughtGrams)} | बिक्री ${MandiMathEngine.gramsToQuintals(v.soldGrams)} | शेष ${MandiMathEngine.gramsToQuintals(v.onHandGrams)} qtl",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (vNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
