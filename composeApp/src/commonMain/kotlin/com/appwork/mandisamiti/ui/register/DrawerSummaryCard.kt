package com.appwork.mandisamiti.ui.register

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiCurrencyFormatter
import com.appwork.mandisamiti.ui.theme.*
import kotlinx.datetime.LocalDate

private val hindiShortMonths = listOf("जन", "फ़र", "मार्च", "अप्रैल", "मई", "जून", "जुला", "अग", "सित", "अक्तू", "नवं", "दिसं")
private val englishShortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

internal fun drawerChipDate(date: LocalDate, isEnglish: Boolean, isToday: Boolean): String {
    val months = if (isEnglish) englishShortMonths else hindiShortMonths
    val text = "${date.dayOfMonth} ${months[date.monthNumber - 1]}"
    return if (isToday) (if (isEnglish) "Today, $text" else "आज, $text") else "$text ${date.year}"
}

@Composable
internal fun DrawerSummaryCard(
    state: DailyRegisterUiState,
    isEnglish: Boolean,
    onDateClick: () -> Unit,
    onCount: () -> Unit,
    onOpening: () -> Unit,
    onCashHome: () -> Unit,
    onWhatsApp: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp).testTag("drawer-summary"),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        border = BorderStroke(1.dp, MandiBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onDateClick,
                    shape = RoundedCornerShape(20.dp),
                    color = MandiSurfaceElevated,
                    border = BorderStroke(1.dp, MandiBorder),
                    modifier = Modifier.heightIn(min = 40.dp).testTag("drawer-date-chip"),
                ) {
                    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MandiTextPrimary, modifier = Modifier.size(18.dp))
                        Text(drawerChipDate(state.selectedDate, isEnglish, state.isToday), color = MandiTextPrimary,
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.ExpandMore, contentDescription = if (isEnglish) "Pick date" else "तारीख चुनें",
                            tint = MandiTextPrimary, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = if (isEnglish) "More" else "और विकल्प", tint = MandiTextPrimary)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(if (isEnglish) "Cash to home" else "घर निकासी", color = MandiTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = MandiTextPrimary) },
                            enabled = !state.isReadOnly,
                            onClick = { menuOpen = false; onCashHome() },
                        )
                        DropdownMenuItem(
                            text = { Text(if (isEnglish) "Send on WhatsApp" else "व्हाट्सएप पर भेजें", color = MandiTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MandiTextPrimary) },
                            onClick = { menuOpen = false; onWhatsApp() },
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(if (isEnglish) "Cash in drawer (per bahi)" else "गल्ले में नकद (बही के अनुसार)",
                        color = MandiTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                    Text(MandiCurrencyFormatter.formatRupees(state.inHandCashDrawerPaisa),
                        color = MandiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 28.sp)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = MandiSurfaceElevated, border = BorderStroke(1.dp, MandiBorder),
                    modifier = Modifier.widthIn(max = 150.dp).testTag("drawer-status-chip")) {
                    Text(drawerCountStatus(state, isEnglish), color = MandiTextPrimary, fontSize = 12.sp, lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium, maxLines = 3, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
            if (state.isNegativeCash) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 40.dp).clickable(enabled = !state.isReadOnly, onClick = onOpening)
                        .testTag("drawer-negative-cash"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MandiAmberDark, modifier = Modifier.size(16.dp))
                    Text(if (isEnglish) "Cash is below zero · set opening cash" else "रकम माइनस है · शुरुआती रोकड़ दर्ज करें",
                        color = MandiAmberDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (!state.isReadOnly) {
                Button(
                    onClick = onCount,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).testTag("drawer-count-button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MandiPrimaryAction, contentColor = MandiPrimaryActionText),
                ) {
                    Text(if (isEnglish) "Count cash · Close day" else "नकद गिनें · दिन बंद करें", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
