package com.appwork.mandisamiti.ui.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.appwork.mandisamiti.domain.model.ExpenseEntry
import com.appwork.mandisamiti.ui.components.DeleteConfirmDialog
import com.appwork.mandisamiti.ui.components.MandiCard

/** Past expenses, newest first. Tap a row to edit it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    entries: List<ExpenseEntry>,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    isEnglish: Boolean = false,
    onDeleteExpense: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val visible = entries.filter { !it.isVoid }.sortedByDescending { it.entryDate }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEnglish) "Past Expenses" else "पिछले खर्च") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (isEnglish) "Back" else "पीछे")
                    }
                },
            )
        },
    ) { padding ->
        if (visible.isEmpty()) {
            Column(
                modifier = modifier.fillMaxSize().padding(padding).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(if (isEnglish) "No expenses yet" else "अभी कोई खर्च नहीं", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visible, key = { it.id }) { e ->
                    MandiCard(onClick = { onOpen(e.id) }, padding = 16.dp, spacing = 0.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val remarks = e.remarks.orEmpty()
                                Text(remarks.substringBefore('\n').ifBlank { e.category.label(isEnglish) }, fontWeight = FontWeight.SemiBold)
                                val sub = listOfNotNull(e.paidTo, remarks.substringAfter('\n', "").takeIf { it.isNotBlank() }).joinToString(" · ")
                                if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(MandiMathEngine.formatRupees(e.amountPaisa), fontWeight = FontWeight.Bold)
                            if (onDeleteExpense != null && !e.isVoid) {
                                IconButton(onClick = { pendingDeleteId = e.id }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = if (isEnglish) "Delete expense" else "खर्च हटाएं",
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val deleteId = pendingDeleteId
    if (deleteId != null && onDeleteExpense != null) {
        DeleteConfirmDialog(
            isEnglish = isEnglish,
            onConfirm = {
                onDeleteExpense(deleteId)
                pendingDeleteId = null
            },
            onDismiss = { pendingDeleteId = null },
        )
    }
}
