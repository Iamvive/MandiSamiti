package com.appwork.mandisamiti.ui.lend

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendStatus
import com.appwork.mandisamiti.domain.model.LendType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendTransactionsListScreen(
    onBack: () -> Unit,
    onNew: () -> Unit = {},
    entries: List<LendEntry> = emptyList(),
    onRepay: (lendId: String, amountPaisa: Long, weightGrams: Long, bagsCount: Long, note: String) -> Unit = { _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedLendForRepayment by remember { mutableStateOf<LendEntry?>(null) }
    var showSettledSection by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = entries.filter { !it.isVoid && (it.partyId.contains(searchQuery, ignoreCase = true) || it.commodityId?.contains(searchQuery, ignoreCase = true) == true) }
    val activeEntries = filtered.filter { it.status != LendStatus.SETTLED }
    val settledEntries = filtered.filter { it.status == LendStatus.SETTLED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("उधार खाता (Loan Book)") },
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
                Icon(Icons.Default.Add, contentDescription = "नया उधार")
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
                label = { Text("किसान या जिंस खोजें") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeEntries.isEmpty() && settledEntries.isEmpty()) {
                    item {
                        Text("कोई उधार रिकॉर्ड नहीं मिला", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    item {
                        Text(
                            "सक्रिय एवं बकाया उधार (${activeEntries.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(activeEntries, key = { it.id }) { entry ->
                        LendCard(entry = entry, onRepayClick = { selectedLendForRepayment = entry })
                    }

                    if (settledEntries.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSettledSection = !showSettledSection }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "पूर्ण चुकता उधार (${settledEntries.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    if (showSettledSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }

                        if (showSettledSection) {
                            items(settledEntries, key = { it.id }) { entry ->
                                LendCard(entry = entry, onRepayClick = null)
                            }
                        }
                    }
                }
            }
        }

        selectedLendForRepayment?.let { lend ->
            RepaymentBottomSheet(
                lend = lend,
                onDismiss = { selectedLendForRepayment = null },
                onConfirm = { amountPaisa, weightGrams, bagsCount, note ->
                    onRepay(lend.id, amountPaisa, weightGrams, bagsCount, note)
                    selectedLendForRepayment = null
                }
            )
        }
    }
}

@Composable
private fun LendCard(entry: LendEntry, onRepayClick: (() -> Unit)?) {
    val isMoney = entry.lendType == LendType.MONEY
    val returnDate = entry.expectedReturnDate
    val isOverdue = returnDate != null && returnDate < System.currentTimeMillis()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.partyId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isOverdue && entry.status != LendStatus.SETTLED) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("अतिदेय") },
                            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                    StatusChip(entry.status)
                }
            }

            if (isMoney) {
                Text(
                    text = "उधार राशि: ₹${MandiMathEngine.paisaToRupeesString(entry.amountPaisa)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "अनाज: ${entry.commodityId ?: "फसल"} • ${entry.bagsCount} बोरी (${MandiMathEngine.gramsToQuintals(entry.weightGrams)} qtl)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!entry.notes.isNullOrBlank()) {
                Text(text = "नोट: ${entry.notes}", style = MaterialTheme.typography.bodySmall)
            }

            if (onRepayClick != null && entry.status != LendStatus.SETTLED) {
                OutlinedButton(
                    onClick = onRepayClick,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Text("वापसी दर्ज करें (Repay)")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: LendStatus) {
    val (label, containerColor, contentColor) = when (status) {
        LendStatus.OPEN -> Triple("बकाया (Open)", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        LendStatus.PARTIAL -> Triple("आंशिक वापसी", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        LendStatus.SETTLED -> Triple("पूर्ण चुकता", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepaymentBottomSheet(
    lend: LendEntry,
    onDismiss: () -> Unit,
    onConfirm: (amountPaisa: Long, weightGrams: Long, bagsCount: Long, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var bagsText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    val isMoney = lend.lendType == LendType.MONEY

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("उधार वापसी दर्ज करें", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("व्यक्ति: ${lend.partyId} • ${if (isMoney) "नकद उधार" else "जिंस उधार (${lend.commodityId ?: "अनाज"})"}")

            if (isMoney) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("वापसी राशि (रु) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bagsText,
                        onValueChange = { bagsText = it },
                        label = { Text("बोरी संख्या") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("वापसी वजन (किग्रा) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("नोट / विवरण (वैकल्पिक)") },
                modifier = Modifier.fillMaxWidth()
            )

            val canConfirm = if (isMoney) {
                MandiMathEngine.parseRupeesToPaisa(amountText) > 0L
            } else {
                MandiMathEngine.parseKgToGrams(weightText) > 0L
            }

            Button(
                onClick = {
                    if (canConfirm) {
                        val amountPaisa = if (isMoney) MandiMathEngine.parseRupeesToPaisa(amountText) else 0L
                        val weightGrams = if (!isMoney) MandiMathEngine.parseKgToGrams(weightText) else 0L
                        val bags = if (!isMoney) bagsText.trim().toLongOrNull() ?: 0L else 0L
                        onConfirm(amountPaisa, weightGrams, bags, note)
                    }
                },
                enabled = canConfirm,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("वापसी सुरक्षित करें")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
