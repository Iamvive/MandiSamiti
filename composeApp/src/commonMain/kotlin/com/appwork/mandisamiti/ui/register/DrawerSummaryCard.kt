package com.appwork.mandisamiti.ui.register

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiCurrencyFormatter
import com.appwork.mandisamiti.ui.theme.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun DrawerSummaryCard(
    state: DailyRegisterUiState,
    isEnglish: Boolean,
    onCount: () -> Unit,
    onOpening: () -> Unit,
    onCashHome: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        border = BorderStroke(1.dp, MandiBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (isEnglish) "Expected cash · cash payments only" else "बही के अनुसार नकद · सिर्फ नकद भुगतान", color = MandiTextSecondary, fontSize = 12.sp)
            Text(MandiCurrencyFormatter.formatRupees(state.inHandCashDrawerPaisa), color = MandiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 26.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(if (isEnglish) "Opening" else "पिछला शेष", color = MandiTextSecondary, fontSize = 12.sp)
                    Text(MandiCurrencyFormatter.formatRupees(state.openingCashPaisa), color = MandiTextPrimary, fontSize = 14.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(if (isEnglish) "Cash in (+)" else "नकद आवक (+)", color = MandiTextSecondary, fontSize = 12.sp)
                    Text(MandiCurrencyFormatter.formatRupees(state.todayCashInPaisa), color = MandiTextPrimary, fontSize = 14.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(if (isEnglish) "Cash out (−)" else "नकद निकासी (−)", color = MandiTextSecondary, fontSize = 12.sp)
                    Text(MandiCurrencyFormatter.formatRupees(state.todayCashOutPaisa), color = MandiTextPrimary, fontSize = 14.sp)
                }
            }
            HorizontalDivider(color = MandiBorder)
            Text(drawerCountStatus(state, isEnglish), color = MandiTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            state.drawerCount?.let { count ->
                val time = Instant.fromEpochMilliseconds(count.closedAt ?: count.countedAt)
                    .toLocalDateTime(TimeZone.currentSystemDefault()).time.toString().take(5)
                Text(
                    when {
                        state.needsRecount -> if (isEnglish) "Previous count $time · no longer verified" else "पिछली गिनती $time · दोबारा मिलान जरूरी"
                        state.isDayClosed -> if (isEnglish) "Day closed $time" else "दिन बंद किया $time"
                        else -> if (isEnglish) "Count saved $time · day still open" else "गिनती सहेजी $time · दिन खुला है"
                    }, color = MandiTextSecondary, fontSize = 12.sp,
                )
            }
            Button(
                onClick = onCount, enabled = !state.isReadOnly && !state.isSaving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MandiPrimaryAction, contentColor = MandiPrimaryActionText),
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(if (state.physicalCountPaisa == null) {
                    if (isEnglish) "Count cash" else "नकद गिनें"
                } else if (isEnglish) "Count again" else "फिर गिनें")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onOpening, enabled = !state.isReadOnly) {
                    Icon(Icons.Default.Edit, contentDescription = if (isEnglish) "Set opening" else "शुरुआती रोकड़", tint = MandiTextPrimary)
                }
                IconButton(onClick = onCashHome, enabled = !state.isReadOnly) {
                    Icon(Icons.Default.Home, contentDescription = if (isEnglish) "Cash to home" else "घर निकासी", tint = MandiTextPrimary)
                }
            }
        }
    }
}
