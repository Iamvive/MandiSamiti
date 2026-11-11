package com.appwork.mandisamiti.ui.register

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    val countActionSemantics = if (isEnglish) "Count cash · Close day" else "नकद गिनें · दिन बंद करें"

    val isMatched = state.reconciliationStatus == BahiReconciliationStatus.BALANCED
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("drawer-summary"),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        border = BorderStroke(1.dp, MandiBorder),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // SLIM TOP ROW: Compact Date Chip + More Menu
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = onDateClick,
                    shape = RoundedCornerShape(12.dp),
                    color = MandiSurfaceElevated,
                    border = BorderStroke(1.dp, MandiBorder),
                    modifier = Modifier.heightIn(min = 32.dp).testTag("drawer-date-chip"),
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MandiTextPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            drawerChipDate(state.selectedDate, isEnglish, state.isToday),
                            color = MandiTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = if (isEnglish) "Pick date" else "तारीख चुनें",
                            tint = MandiTextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = if (isEnglish) "More" else "और विकल्प",
                            tint = MandiTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
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

            // COMPACT HERO ROW: Cash in Drawer + Balance + Status Pill ON LEFT | Fast CTA Button ON RIGHT
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (state.inHandCashDrawerPaisa >= 0) MandiGreenReceivable else MandiRedPayable)
                        )
                        Text(
                            text = if (isEnglish) "CASH IN DRAWER" else "गल्ले में उपलब्ध नकद",
                            color = MandiTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = MandiCurrencyFormatter.formatRupees(state.inHandCashDrawerPaisa),
                            color = MandiTextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            lineHeight = 24.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isMatched) MandiGreenLight else MandiSurfaceElevated,
                            border = BorderStroke(1.dp, if (isMatched) MandiGreenBorder else MandiBorder),
                            modifier = Modifier.testTag("drawer-status-chip")
                        ) {
                            Text(
                                text = drawerCountStatus(state, isEnglish),
                                color = if (isMatched) MandiGreenText else MandiTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (!state.isReadOnly) {
                    Button(
                        onClick = onCount,
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("drawer-count-button")
                            .semantics { contentDescription = countActionSemantics },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MandiSurfaceElevated,
                            contentColor = MandiTextPrimary
                        ),
                        border = BorderStroke(1.dp, MandiBorder),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Count cash →" else "हिसाब मिलाएँ →",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MandiTextPrimary
                        )
                    }
                }
            }

            // SUBTLE 1-LINE WARNING (ONLY IF CASH IS BELOW ZERO)
            if (state.isNegativeCash) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !state.isReadOnly, onClick = onOpening)
                        .padding(top = 1.dp)
                        .testTag("drawer-negative-cash"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MandiAmberDark,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (isEnglish) "Negative balance · set opening cash" else "रकम माइनस है · शुरुआती रोकड़ दर्ज करें",
                        color = MandiAmberDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
