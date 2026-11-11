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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
    var showNegativeCashDialog by remember { mutableStateOf(false) }
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

            // VARIANT 1 ROW: LEFT (LABEL + CTA BUTTON) | RIGHT (AMOUNT + INDICATOR)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // LEFT SIDE: Label + Reconcile Button
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
                    Spacer(Modifier.height(4.dp))
                    if (!state.isReadOnly) {
                        Button(
                            onClick = onCount,
                            enabled = !state.isSaving,
                            modifier = Modifier
                                .heightIn(min = 32.dp)
                                .testTag("drawer-count-button")
                                .semantics { contentDescription = countActionSemantics },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MandiSurfaceElevated,
                                contentColor = MandiTextPrimary
                            ),
                            border = BorderStroke(1.dp, MandiBorder),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Count cash →" else "हिसाब मिलाएँ →",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = MandiTextPrimary
                            )
                        }
                    }
                }

                // RIGHT SIDE: Amount + Indicator / Status Pill
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (state.isNegativeCash) {
                            Surface(
                                onClick = { showNegativeCashDialog = true },
                                shape = RoundedCornerShape(6.dp),
                                color = MandiSurfaceElevated,
                                border = BorderStroke(1.dp, MandiBorder),
                                modifier = Modifier.testTag("drawer-negative-cash")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MandiTextPrimary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = if (isEnglish) "Negative" else "माइनस",
                                        color = MandiTextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline
                                    )
                                }
                            }
                        } else if (isMatched) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MandiGreenLight,
                                border = BorderStroke(1.dp, MandiGreenBorder),
                                modifier = Modifier.testTag("drawer-status-chip")
                            ) {
                                Text(
                                    text = if (isEnglish) "✓ Matched" else "✓ हिसाब बराबर",
                                    color = MandiGreenText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = MandiCurrencyFormatter.formatRupees(state.inHandCashDrawerPaisa),
                            color = if (state.inHandCashDrawerPaisa >= 0) MandiTextPrimary else MandiRedPayable,
                            fontWeight = FontWeight.Black,
                            fontSize = 21.sp,
                            lineHeight = 24.sp
                        )
                    }
                    Text(
                        text = if (state.isNegativeCash) {
                            if (isEnglish) "Tap ⚠️ to resolve" else "ठीक करने के लिए ⚠️ दबाएं"
                        } else {
                            drawerCountStatus(state, isEnglish)
                        },
                        fontSize = 10.sp,
                        color = if (state.isNegativeCash) MandiAmberDark else MandiTextMuted,
                        modifier = Modifier.padding(top = 1.dp).testTag("drawer-status-chip")
                    )
                }
            }
        }
    }

    // CONTEXTUAL EXPLANATION POPUP (ON TAP OF INDICATOR)
    if (showNegativeCashDialog) {
        AlertDialog(
            onDismissRequest = { showNegativeCashDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MandiAmberDark,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = if (isEnglish) "Cash is Below Zero" else "गल्ले में रकम माइनस में है",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MandiTextPrimary
                )
            },
            text = {
                Text(
                    text = if (isEnglish)
                        "Morning opening cash (Bohani) has not been entered for today. Recording opening cash will balance your drawer."
                    else
                        "सुबह की शुरुआती रोकड़ (बाकी निकाला) आज दर्ज नहीं हुई है, इसलिए निकासी से बैलेंस माइनस दिख रहा है। शुरुआती रोकड़ दर्ज करें।",
                    fontSize = 13.sp,
                    color = MandiTextSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNegativeCashDialog = false
                        onOpening()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MandiPrimaryAction,
                        contentColor = MandiPrimaryActionText
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isEnglish) "+ Set Opening Cash" else "+ शुरुआती रोकड़ दर्ज करें",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNegativeCashDialog = false }) {
                    Text(
                        text = if (isEnglish) "Dismiss" else "समझ गया",
                        color = MandiTextSecondary,
                        fontSize = 13.sp
                    )
                }
            },
            containerColor = MandiSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
