package com.appwork.mandisamiti.ui.buy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.PaymentStatus
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.domain.search.BilingualSearchEngine
import com.appwork.mandisamiti.ui.components.VoidEntryDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyTransactionsListScreen(
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    onBack: () -> Unit,
    entries: List<BuyEntry> = emptyList(),
    onVoid: ((id: String, reason: VoidReason) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var voidTarget by remember { mutableStateOf<BuyEntry?>(null) }

    val filteredEntries = entries
        .filter { entry ->
            if (entry.isVoid) return@filter false
            if (searchQuery.isBlank()) return@filter true
            val village = extractVillage(entry.remarks)
            BilingualSearchEngine.matchesAny(
                searchQuery,
                entry.farmerId,
                village,
                entry.slipNumber,
                entry.vehicleNumber,
                entry.commodityId
            )
        }
        .sortedByDescending { it.entryDate }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("खरीद सौदे") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "पीछे")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNew,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "नई खरीद")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("किसान, गाँव या पर्चा नंबर खोजें") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            if (filteredEntries.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("कोई खरीद एंट्री नहीं मिली", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredEntries, key = { it.id }) { entry ->
                        BuyEntryCard(
                            entry = entry,
                            onClick = { onOpen(entry.id) },
                            onLongClick = { voidTarget = entry }
                        )
                    }
                }
            }
        }
    }

    voidTarget?.let { target ->
        VoidEntryDialog(
            title = "खरीद सौदा रद्द करें?",
            subtitle = "रद्द सौदा सूची से हट जाएगा और किसान का हिसाब शून्य हो जाएगा।",
            onConfirm = { reason ->
                onVoid?.invoke(target.id, reason)
                voidTarget = null
            },
            onDismiss = { voidTarget = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BuyEntryCard(
    entry: BuyEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val farmerName = entry.farmerId.ifBlank { "किसान" }
    val village = extractVillage(entry.remarks)
    val slipNumber = entry.slipNumber

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = farmerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val subtitleParts = buildList {
                        if (!village.isNullOrBlank()) add("गाँव: $village")
                        if (!slipNumber.isNullOrBlank()) add("पर्चा #$slipNumber")
                    }
                    if (subtitleParts.isNotEmpty()) {
                        Text(
                            text = subtitleParts.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusChip(status = entry.paymentStatus)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val cropLabel = entry.commodityId.ifBlank { "फसल" }
                Text(
                    text = "$cropLabel • ${entry.bagsCount} बोरी • ${MandiMathEngine.gramsToQuintals(entry.totalWeightGrams)} क्विंटल",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "₹${MandiMathEngine.paisaToRupeesString(entry.netPayablePaisa)} देय",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: PaymentStatus) {
    val (label, containerColor, contentColor) = when (status) {
        PaymentStatus.PENDING -> Triple("लंबित", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        PaymentStatus.PARTIAL -> Triple("आंशिक", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        PaymentStatus.COMPLETED -> Triple("पूर्ण", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
    }

    SuggestionChip(
        onClick = {},
        label = { Text(label) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor,
            labelColor = contentColor
        )
    )
}
