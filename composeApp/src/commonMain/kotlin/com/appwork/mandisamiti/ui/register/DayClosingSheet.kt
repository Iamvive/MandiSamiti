package com.appwork.mandisamiti.ui.register

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiCurrencyFormatter
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.ui.components.MandiKeyboard
import com.appwork.mandisamiti.ui.theme.*

/**
 * Premium Tier-1 Day-closing Sheet (inspired by Cred / Zerodha financial interfaces).
 * - Hero comparison card at the top (Expected vs Counted vs Live Delta).
 * - Segmented tab selector between "नोट-सिक्के से गिनें" and "सीधे कुल रकम भरें".
 * - Compact denomination rows preserving exact accessibility semantics.
 * - Always-visible sticky bottom action dock for zero-scroll instant signoff.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DayClosingSheet(
    state: DailyRegisterUiState,
    isEnglish: Boolean,
    cashInput: String,
    onCashInputChange: (String) -> Unit,
    reason: String,
    onReasonChange: (String) -> Unit,
    useDenominations: Boolean,
    denominationQuantities: Map<Int, String>,
    onToggleDenominations: () -> Unit,
    onDenominationChange: (Int, String) -> Unit,
    onDismiss: () -> Unit,
    onSaveCount: (Long) -> Unit,
    onCloseDay: () -> Unit,
    onClearCount: () -> Unit,
) {
    val countedRs = parseDrawerRupees(cashInput)
    val diffPaisa = countedRs?.let { it * 100L - state.inHandCashDrawerPaisa }
    var detailsOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MandiSurface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("day-closing-sheet")
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = if (isEnglish) "Close day" else "दिन बंद करें",
                        color = MandiTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = drawerChipDate(state.selectedDate, isEnglish, state.isToday),
                        color = MandiTextSecondary,
                        fontSize = 12.sp,
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = if (isEnglish) "Close" else "बंद करें",
                        tint = MandiTextSecondary,
                    )
                }
            }

            // Top Hero Card: Side-by-Side Comparison
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MandiSurfaceElevated),
                border = BorderStroke(1.dp, MandiBorder),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEnglish) "EXPECTED (PER BAHI)" else "बही अनुसार नकद",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MandiTextSecondary,
                                letterSpacing = 0.5.sp,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = MandiCurrencyFormatter.formatRupees(state.inHandCashDrawerPaisa),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiTextPrimary,
                            )
                        }

                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isEnglish) "ACTUAL COUNTED" else "गिनी हुई नकदी",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MandiTextSecondary,
                                letterSpacing = 0.5.sp,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (countedRs != null) "₹${MandiMathEngine.formatIndianGrouping(countedRs)}" else "—",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (countedRs != null) MandiTextPrimary else MandiTextSecondary,
                            )
                        }
                    }

                    // Delta Status Badge
                    if (diffPaisa != null) {
                        val isMatch = diffPaisa == 0L
                        val isSurplus = diffPaisa > 0L
                        val badgeBg = if (isMatch) MandiGreenLight else MandiAmberLight
                        val badgeBorder = if (isMatch) MandiGreenBorder else MandiAmberBorder
                        val badgeText = if (isMatch) MandiGreenText else MandiAmberDark
                        val absDiff = if (diffPaisa < 0) -diffPaisa else diffPaisa
                        val diffFormatted = MandiCurrencyFormatter.formatRupees(absDiff)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(badgeBg)
                                .border(1.dp, badgeBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = when {
                                    isMatch -> if (isEnglish) "✓ Exact match" else "✓ बिल्कुल बराबर"
                                    isSurplus -> if (isEnglish) "Surplus (+)" else "फालतू नकदी (+)"
                                    else -> if (isEnglish) "Shortage (−)" else "कमी नकदी (−)"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeText,
                            )
                            if (!isMatch) {
                                Text(
                                    text = if (isSurplus) "+$diffFormatted" else "-$diffFormatted",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeText,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Segmented Switcher: [ नोट-सिक्के से गिनें ] vs [ सीधे कुल रकम भरें ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MandiSurfaceElevated)
                    .border(1.dp, MandiBorder, RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Denominations Tab
                val denomActive = useDenominations
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (denomActive) MandiPrimaryAction else Color.Transparent)
                        .clickable(enabled = !denomActive) { onToggleDenominations() }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isEnglish) "Count notes and coins" else "नोट-सिक्के से गिनें",
                        fontSize = 13.sp,
                        fontWeight = if (denomActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (denomActive) MandiPrimaryActionText else MandiTextSecondary,
                    )
                }

                // Direct Total Tab
                val directActive = !useDenominations
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (directActive) MandiPrimaryAction else Color.Transparent)
                        .clickable(enabled = !directActive) { onToggleDenominations() }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isEnglish) "Enter total instead" else "सीधे कुल रकम भरें",
                        fontSize = 13.sp,
                        fontWeight = if (directActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (directActive) MandiPrimaryActionText else MandiTextSecondary,
                    )
                }
            }

            // Scrollable Content Area (fills remaining vertical room above sticky dock)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!useDenominations) {
                    // Direct Total Input Mode
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isEnglish) "Counted cash ₹" else "गिनी हुई नकदी ₹",
                            fontSize = 13.sp,
                            color = MandiTextSecondary,
                        )
                        OutlinedTextField(
                            value = cashInput,
                            onValueChange = onCashInputChange,
                            keyboardOptions = MandiKeyboard.Whole,
                            isError = cashInput.isNotEmpty() && countedRs == null,
                            prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MandiTextPrimary) },
                            label = { Text(if (isEnglish) "Counted cash ₹" else "गिनी हुई नकदी ₹") },
                            placeholder = { Text("0") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MandiBorderActive,
                                unfocusedBorderColor = MandiBorder,
                                focusedTextColor = MandiTextPrimary,
                                unfocusedTextColor = MandiTextPrimary,
                            ),
                        )
                    }
                } else {
                    // Denomination Calculator Mode
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        drawerDenominations.forEach { denomination ->
                            val countStr = denominationQuantities[denomination].orEmpty()
                            val count = countStr.trim().toLongOrNull() ?: 0L
                            val subtotal = count * denomination

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MandiSurfaceElevated),
                                border = BorderStroke(1.dp, MandiBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (denomination <= 5) "₹$denomination (${if (isEnglish) "Coins" else "सिक्के"})" else "₹$denomination",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MandiTextPrimary,
                                        modifier = Modifier.width(76.dp)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val newCount = (count - 1).coerceAtLeast(0L)
                                                onDenominationChange(denomination, if (newCount > 0) newCount.toString() else "")
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "−", tint = MandiTextPrimary)
                                        }

                                        OutlinedTextField(
                                            value = countStr,
                                            onValueChange = { onDenominationChange(denomination, it) },
                                            label = { Text("₹$denomination × " + if (isEnglish) "qty" else "संख्या", fontSize = 9.sp) },
                                            singleLine = true,
                                            keyboardOptions = MandiKeyboard.Whole,
                                            modifier = Modifier.width(82.dp).heightIn(min = 40.dp),
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MandiTextPrimary
                                            )
                                        )

                                        IconButton(
                                            onClick = {
                                                val newCount = count + 1
                                                onDenominationChange(denomination, newCount.toString())
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "+", tint = MandiTextPrimary)
                                        }
                                    }

                                    Text(
                                        text = "₹${MandiMathEngine.formatIndianGrouping(subtotal)}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = if (subtotal > 0) MandiTextPrimary else MandiTextSecondary,
                                        modifier = Modifier.width(72.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }

                // Discrepancy Reason Input
                if (diffPaisa != null && diffPaisa != 0L) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = onReasonChange,
                        label = { Text(if (isEnglish) "Reason (optional)" else "कारण (वैकल्पिक)") },
                        keyboardOptions = MandiKeyboard.Note,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MandiBorderActive,
                            unfocusedBorderColor = MandiBorder,
                            focusedTextColor = MandiTextPrimary,
                            unfocusedTextColor = MandiTextPrimary,
                        ),
                    )
                }

                // Collapsible Day Details Accordion
                Surface(
                    onClick = { detailsOpen = !detailsOpen },
                    color = MandiSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MandiBorder),
                    modifier = Modifier.fillMaxWidth().testTag("day-closing-details"),
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (isEnglish) "Details" else "विवरण",
                                color = MandiTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                if (detailsOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MandiTextPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        if (detailsOpen) {
                            listOf(
                                (if (isEnglish) "Opening" else "पिछला शेष") to "₹${MandiMathEngine.paisaToRupeesString(state.openingCashPaisa)}",
                                (if (isEnglish) "Cash in (+)" else "नकद आवक (+)") to "₹${MandiMathEngine.paisaToRupeesString(state.todayCashInPaisa)}",
                                (if (isEnglish) "Cash out (−)" else "नकद निकासी (−)") to "₹${MandiMathEngine.paisaToRupeesString(state.todayCashOutPaisa)}",
                                (if (isEnglish) "Mohari buy (Asal)" else "मोहरी खरीद (असल)") to "₹${MandiMathEngine.paisaToRupeesString(state.bahiDayPage?.mohariBuyAsalPaisa ?: state.bahiDayPage?.khaateBuyAsalPaisa ?: 0L)}",
                                (if (isEnglish) "Mohari sell (Asal)" else "मोहरी बिक्री (असल)") to "₹${MandiMathEngine.paisaToRupeesString(state.bahiDayPage?.khaateSellAsalPaisa ?: 0L)}",
                            ).forEach { (label, value) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(label, color = MandiTextSecondary, fontSize = 12.sp)
                                    Text(value, color = MandiTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (state.physicalCountPaisa != null) {
                                TextButton(onClick = onClearCount, contentPadding = PaddingValues(0.dp)) {
                                    Text(if (isEnglish) "Remove count" else "गिनती हटाएं", color = MandiRedPayable, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // STICKY BOTTOM ACTION DOCK (Always visible without scrolling)
            Surface(
                color = MandiSurface,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, MandiBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Secondary CTA: Save count only
                        OutlinedButton(
                            onClick = { countedRs?.let(onSaveCount) },
                            enabled = countedRs != null && !state.isSaving,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MandiBorder),
                        ) {
                            Text(
                                text = if (isEnglish) "Save count · keep day open" else "गिनती सहेजें · दिन खुला रखें",
                                color = MandiTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        // Primary CTA: Close Day
                        Button(
                            onClick = onCloseDay,
                            enabled = countedRs != null && !state.isSaving,
                            modifier = Modifier
                                .weight(1.1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MandiBtnSuccessBg,
                                contentColor = MandiBtnSuccessFg,
                            ),
                        ) {
                            Text(
                                text = if (isEnglish) "Close day" else "दिन बंद करें",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
