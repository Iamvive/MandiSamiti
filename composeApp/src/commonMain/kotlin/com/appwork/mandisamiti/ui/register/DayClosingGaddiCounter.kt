package com.appwork.mandisamiti.ui.register

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiCurrencyFormatter
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.ui.theme.*

@Composable
internal fun DayClosingGaddiCounter(
    draft: GaddiDraft,
    onDraftChange: (GaddiDraft) -> Unit,
    isEnglish: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Denomination Strip (5 x 2 chips, all ten denominations)
        DenominationStrip(
            selected = draft.selected,
            counts = draft.counts,
            isEnglish = isEnglish,
            onSelect = { onDraftChange(draft.select(it)) },
        )

        // Focus Line: Selected denomination, pieces, and amount
        val currentCount = draft.counts[draft.selected] ?: GaddiCount(0, 0)
        val pieces = currentCount.pieces
        val totalRs = pieces.toLong() * draft.selected
        val isCoin = draft.selected <= 5

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "₹${draft.selected}" + if (isCoin) " (${if (isEnglish) "Coin" else "सिक्का"})" else "",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary,
            )
            Text(
                text = "$pieces ${if (isEnglish) "pcs" else "नग"} = ₹${MandiMathEngine.formatIndianGrouping(totalRs)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MandiTextPrimary,
            )
        }

        // Entry Boxes: गड्डी (bundle of 100) and खुले (loose)
        // For coins (<= 5): single box labelled "गिनती" / "Count", no bundle box
        if (isCoin) {
            val spokenLoose = "${draft.selected} के सिक्के, ${currentCount.loose} गिनती"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MandiSurfaceElevated)
                    .border(2.dp, MandiBorderActive, RoundedCornerShape(8.dp))
                    .clickable { onDraftChange(draft.focus(GaddiField.LOOSE)) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .semantics { contentDescription = spokenLoose },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isEnglish) "Count" else "गिनती",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MandiTextSecondary,
                    )
                    Text(
                        text = currentCount.loose.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary,
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Bundles Box (गड्डी 100 की)
                val isBundleActive = draft.field == GaddiField.BUNDLES
                val spokenBundle = "${draft.selected} के नोट, ${currentCount.bundles} गड्डी"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MandiSurfaceElevated)
                        .border(
                            if (isBundleActive) 2.dp else 1.dp,
                            if (isBundleActive) MandiBorderActive else MandiBorder,
                            RoundedCornerShape(8.dp),
                        )
                        .clickable { onDraftChange(draft.focus(GaddiField.BUNDLES)) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .semantics { contentDescription = spokenBundle },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isEnglish) "Bundles (of 100)" else "गड्डी (100 की)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isBundleActive) MandiTextPrimary else MandiTextSecondary,
                        )
                        Text(
                            text = currentCount.bundles.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary,
                        )
                    }
                }

                // Loose Box (खुले)
                val isLooseActive = draft.field == GaddiField.LOOSE
                val spokenLoose = "${draft.selected} के नोट, ${currentCount.loose} खुले"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MandiSurfaceElevated)
                        .border(
                            if (isLooseActive) 2.dp else 1.dp,
                            if (isLooseActive) MandiBorderActive else MandiBorder,
                            RoundedCornerShape(8.dp),
                        )
                        .clickable { onDraftChange(draft.focus(GaddiField.LOOSE)) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .semantics { contentDescription = spokenLoose },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isEnglish) "Loose" else "खुले",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isLooseActive) MandiTextPrimary else MandiTextSecondary,
                        )
                        Text(
                            text = currentCount.loose.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DenominationStrip(
    selected: Int,
    counts: Map<Int, GaddiCount>,
    isEnglish: Boolean,
    onSelect: (Int) -> Unit,
) {
    // 5 x 2 chips: 2000, 500, 200, 100, 50 in row 1; 20, 10, 5, 2, 1 in row 2
    val row1 = listOf(2000, 500, 200, 100, 50)
    val row2 = listOf(20, 10, 5, 2, 1)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            row1.forEach { denom ->
                DenominationChip(
                    denomination = denom,
                    isSelected = selected == denom,
                    count = counts[denom],
                    isEnglish = isEnglish,
                    onSelect = { onSelect(denom) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            row2.forEach { denom ->
                DenominationChip(
                    denomination = denom,
                    isSelected = selected == denom,
                    count = counts[denom],
                    isEnglish = isEnglish,
                    onSelect = { onSelect(denom) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DenominationChip(
    denomination: Int,
    isSelected: Boolean,
    count: GaddiCount?,
    isEnglish: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCounted = count != null
    val label = if (isCounted) "× ${count.pieces}" else (if (isEnglish) "pending" else "बाकी")
    val spoken = "₹$denomination, $label"

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) MandiSurfaceElevated else MandiSurface)
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) MandiBorderActive else MandiBorder,
                RoundedCornerShape(6.dp),
            )
            .clickable(onClick = onSelect)
            .semantics { contentDescription = spoken }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "₹$denomination",
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = MandiTextPrimary,
                maxLines = 1,
            )
            Text(
                text = label,
                fontSize = 9.sp,
                color = if (isCounted) MandiTextPrimary else MandiTextSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun GaddiNumberPad(
    draft: GaddiDraft,
    onDraftChange: (GaddiDraft) -> Unit,
    isEnglish: Boolean,
    modifier: Modifier = Modifier,
) {
    val idx = drawerDenominations.indexOf(draft.selected)
    val hasPrev = idx > 0
    val hasNext = idx in 0 until (drawerDenominations.size - 1)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Row 1: 1, 2, 3, Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PadDigitKey("1", Modifier.weight(1f)) { onDraftChange(draft.digit(1)) }
            PadDigitKey("2", Modifier.weight(1f)) { onDraftChange(draft.digit(2)) }
            PadDigitKey("3", Modifier.weight(1f)) { onDraftChange(draft.digit(3)) }
            PadIconKey(
                icon = Icons.AutoMirrored.Filled.Backspace,
                contentDesc = if (isEnglish) "Delete" else "मिटाएँ",
                modifier = Modifier.weight(1f),
            ) { onDraftChange(draft.backspace()) }
        }

        // Row 2: 4, 5, 6, Previous
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PadDigitKey("4", Modifier.weight(1f)) { onDraftChange(draft.digit(4)) }
            PadDigitKey("5", Modifier.weight(1f)) { onDraftChange(draft.digit(5)) }
            PadDigitKey("6", Modifier.weight(1f)) { onDraftChange(draft.digit(6)) }
            PadTextKey(
                text = if (isEnglish) "Prev" else "पिछला",
                enabled = hasPrev,
                modifier = Modifier.weight(1f),
            ) { onDraftChange(draft.previous()) }
        }

        // Row 3: 7, 8, 9, Next
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PadDigitKey("7", Modifier.weight(1f)) { onDraftChange(draft.digit(7)) }
            PadDigitKey("8", Modifier.weight(1f)) { onDraftChange(draft.digit(8)) }
            PadDigitKey("9", Modifier.weight(1f)) { onDraftChange(draft.digit(9)) }
            PadTextKey(
                text = if (isEnglish) "Next" else "अगला",
                enabled = hasNext,
                modifier = Modifier.weight(1f),
            ) { onDraftChange(draft.next()) }
        }

        // Row 4: 0 करें, 0, +1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PadTextKey(
                text = if (isEnglish) "Set 0" else "0 करें",
                enabled = true,
                modifier = Modifier.weight(1f),
            ) { onDraftChange(draft.zero()) }
            PadDigitKey("0", Modifier.weight(2f)) { onDraftChange(draft.digit(0)) }
            PadTextKey(
                text = "+1",
                enabled = true,
                modifier = Modifier.weight(1f),
                contentDesc = if (isEnglish) "Add one" else "एक जोड़ें",
            ) { onDraftChange(draft.addOne()) }
        }
    }
}

@Composable
private fun PadDigitKey(
    digit: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MandiSurfaceElevated)
            .border(1.dp, MandiBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = digit },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = digit,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MandiTextPrimary,
        )
    }
}

@Composable
private fun PadIconKey(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MandiSurfaceElevated)
            .border(1.dp, MandiBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MandiTextPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun PadTextKey(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    contentDesc: String? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) MandiSurfaceElevated else MandiSurface)
            .border(1.dp, if (enabled) MandiBorder else MandiBorder, RoundedCornerShape(8.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = contentDesc ?: text },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) MandiTextPrimary else MandiTextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
