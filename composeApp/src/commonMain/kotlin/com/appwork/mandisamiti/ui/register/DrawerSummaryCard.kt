package com.appwork.mandisamiti.ui.register

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiCurrencyFormatter
import com.appwork.mandisamiti.ui.theme.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DrawerSummaryCard(
    state: DailyRegisterUiState,
    isEnglish: Boolean,
    onCount: () -> Unit,
    onOpening: () -> Unit,
    onCashHome: () -> Unit,
) {
    var showDetails by remember { mutableStateOf(false) }
    val countLabel = if (state.physicalCountPaisa == null) {
        if (isEnglish) "Count cash" else "नकद गिनें"
    } else if (isEnglish) "Count again" else "फिर गिनें"
    val detailsLabel = if (isEnglish) "Cash details" else "नकद विवरण"
    val statusIcon = when (state.reconciliationStatus) {
        BahiReconciliationStatus.BALANCED -> Icons.Default.CheckCircle
        BahiReconciliationStatus.DISCREPANCY, BahiReconciliationStatus.RECOUNT_REQUIRED -> Icons.Default.Warning
        BahiReconciliationStatus.NOT_COUNTED -> Icons.Default.Info
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp).testTag("drawer-summary"),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        border = BorderStroke(1.dp, MandiBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).clickable(onClickLabel = detailsLabel) { showDetails = true }) {
                Text(if (isEnglish) "Expected cash" else "बही के अनुसार नकद",
                    color = MandiTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                Text(MandiCurrencyFormatter.formatRupees(state.inHandCashDrawerPaisa),
                    color = MandiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 28.sp)
            }
            Icon(statusIcon, contentDescription = drawerCountStatus(state, isEnglish),
                tint = MandiTextPrimary, modifier = Modifier.size(18.dp))
            IconButton(onClick = onCount, enabled = !state.isReadOnly && !state.isSaving,
                modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Calculate, contentDescription = countLabel, tint = MandiTextPrimary)
            }
            IconButton(onClick = { showDetails = true }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ExpandMore, contentDescription = detailsLabel, tint = MandiTextPrimary)
            }
        }
        if (state.needsRecount || state.reconciliationStatus == BahiReconciliationStatus.DISCREPANCY) {
            Text(drawerCountStatus(state, isEnglish), color = MandiTextPrimary, fontSize = 12.sp,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp))
        }
    }

    if (showDetails) {
        ModalBottomSheet(
            onDismissRequest = { showDetails = false },
            containerColor = MandiSurface,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(detailsLabel, color = MandiTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                listOf(
                    (if (isEnglish) "Opening" else "पिछला शेष") to state.openingCashPaisa,
                    (if (isEnglish) "Cash in (+)" else "नकद आवक (+)") to state.todayCashInPaisa,
                    (if (isEnglish) "Cash out (−)" else "नकद निकासी (−)") to state.todayCashOutPaisa,
                    (if (isEnglish) "Expected cash" else "बही के अनुसार नकद") to state.inHandCashDrawerPaisa,
                ).forEach { (label, amount) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(label, modifier = Modifier.weight(1f), color = MandiTextSecondary)
                        Text(MandiCurrencyFormatter.formatRupees(amount), color = MandiTextPrimary,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider(color = MandiBorder)
                Text(drawerCountStatus(state, isEnglish), color = MandiTextPrimary)
                state.drawerCount?.let { count ->
                    val time = Instant.fromEpochMilliseconds(count.closedAt ?: count.countedAt)
                        .toLocalDateTime(TimeZone.currentSystemDefault()).time.toString().take(5)
                    Text(when {
                        state.needsRecount -> if (isEnglish) "Previous count $time · no longer verified" else "पिछली गिनती $time · दोबारा मिलान जरूरी"
                        state.isDayClosed -> if (isEnglish) "Day closed $time" else "दिन बंद किया $time"
                        else -> if (isEnglish) "Count saved $time · day still open" else "गिनती सहेजी $time · दिन खुला है"
                    }, color = MandiTextSecondary)
                }
                Text(if (isEnglish) "Cash payments only. Online and cheque entries do not change drawer cash."
                    else "सिर्फ नकद भुगतान। ऑनलाइन और चेक से गल्ले की नकदी नहीं बदलती।",
                    color = MandiTextSecondary, fontSize = 12.sp)
                Button(onClick = { showDetails = false; onCount() },
                    enabled = !state.isReadOnly && !state.isSaving, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MandiPrimaryAction, contentColor = MandiPrimaryActionText)) {
                    Icon(Icons.Default.Calculate, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(countLabel)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { showDetails = false; onOpening() }, enabled = !state.isReadOnly) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MandiTextPrimary)
                        Spacer(Modifier.size(6.dp))
                        Text(if (isEnglish) "Set opening" else "शुरुआती रोकड़", color = MandiTextPrimary)
                    }
                    TextButton(onClick = { showDetails = false; onCashHome() }, enabled = !state.isReadOnly) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = MandiTextPrimary)
                        Spacer(Modifier.size(6.dp))
                        Text(if (isEnglish) "Cash to home" else "घर निकासी", color = MandiTextPrimary)
                    }
                }
            }
        }
    }
}
