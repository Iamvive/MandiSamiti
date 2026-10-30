package com.appwork.mandisamiti.ui.sell

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.FilterChip
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
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.slipLabel
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.domain.search.BilingualSearchEngine
import com.appwork.mandisamiti.ui.components.VoidEntryDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellTransactionsListScreen(
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    onBack: () -> Unit,
    entries: List<SellEntry> = emptyList(),
    onVoid: ((id: String, reason: VoidReason) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterOnlyDue by remember { mutableStateOf(false) }
    var voidTarget by remember { mutableStateOf<SellEntry?>(null) }

    val filteredEntries = entries
        .filter { !it.isVoid }
        .filter { !filterOnlyDue || it.balanceDuePaisa > 0 }
        .filter {
            if (searchQuery.isBlank()) return@filter true
            BilingualSearchEngine.matchesAny(searchQuery, it.buyerId, it.slipNo?.toString(), it.invoiceNumber)
        }
        .sortedByDescending { it.entryDate }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("बिक्री बिल") },
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
                Icon(Icons.Default.Add, contentDescription = "नई बिक्री")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("खरीदार या बिल नंबर खोजें") },
                    modifier = Modifier.fillMaxWidth()
                )
                FilterChip(
                    selected = filterOnlyDue,
                    onClick = { filterOnlyDue = !filterOnlyDue },
                    label = { Text("केवल बकाया (Balance Due > 0)") }
                )
            }

            if (filteredEntries.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("कोई बिक्री रिकॉर्ड नहीं मिला", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredEntries, key = { it.id }) { entry ->
                        SellEntryCard(
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
            title = "बिक्री बिल रद्द करें?",
            subtitle = "रद्द बिल सूची से हट जाएगा और खरीदार का हिसाब शून्य हो जाएगा।",
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
private fun SellEntryCard(
    entry: SellEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = entry.buyerId.ifBlank { "खरीदार" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "बिल #${slipLabel(entry.slipNo, entry.id)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (entry.billMode == com.appwork.mandisamiti.domain.model.BillMode.BILL_OF_SUPPLY) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("सप्लाई बिल") },
                        colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                } else {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(if (entry.partyKind == com.appwork.mandisamiti.domain.model.SellPartyKind.MERCHANT) "व्यापारी" else "ग्राहक") },
                        colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${entry.bagsCount} बोरी • ${MandiMathEngine.gramsToQuintals(entry.totalWeightGrams)} क्विंटल",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "देय: ₹${MandiMathEngine.paisaToRupeesString(entry.totalReceivablePaisa)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
