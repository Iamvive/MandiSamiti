package com.appwork.mandisamiti.ui.ledger

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

@Composable
fun RepaymentHistoryDialog(
    history: List<LendSettlement>,
    isEnglish: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MandiSurface,
        title = {
            Text(
                text = if (isEnglish) "Repayment history" else "वापसी का इतिहास",
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (history.size < 2) {
                    Text(
                        text = if (isEnglish) "No changes" else "कोई बदलाव नहीं",
                        fontSize = 13.sp,
                        color = MandiTextSecondary
                    )
                } else {
                    val pairs = history.zipWithNext()
                    pairs.forEach { (prev, next) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MandiSurfaceElevated,
                            border = BorderStroke(1.dp, MandiBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (prev.amountPaisa != next.amountPaisa) {
                                    val prevRs = MandiMathEngine.formatIndianGrouping(prev.amountPaisa / 100L)
                                    val nextRs = MandiMathEngine.formatIndianGrouping(next.amountPaisa / 100L)
                                    val amtText = if (isEnglish) {
                                        "Was ₹$prevRs, now ₹$nextRs"
                                    } else {
                                        "पहले ₹$prevRs, अब ₹$nextRs"
                                    }
                                    Text(
                                        text = amtText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MandiTextPrimary
                                    )
                                }

                                if (prev.settledAt != next.settledAt) {
                                    val prevDate = formatEpochDate(prev.settledAt)
                                    val nextDate = formatEpochDate(next.settledAt)
                                    val dateText = if (isEnglish) {
                                        "Date: $prevDate to $nextDate"
                                    } else {
                                        "तारीख: $prevDate से $nextDate"
                                    }
                                    Text(
                                        text = dateText,
                                        fontSize = 12.sp,
                                        color = MandiTextSecondary
                                    )
                                }

                                if (prev.amountPaisa == next.amountPaisa && prev.settledAt == next.settledAt) {
                                    val splitText = if (isEnglish) {
                                        "Interest and principal split recalculated after an earlier payment changed"
                                    } else {
                                        "पहले की वापसी बदलने से ब्याज और मूल का बँटवारा दोबारा निकाला गया"
                                    }
                                    Text(
                                        text = splitText,
                                        fontSize = 12.sp,
                                        color = MandiTextSecondary
                                    )
                                }

                                val changedText = if (isEnglish) {
                                    "Changed on ${formatEpochDate(next.updatedAt)}"
                                } else {
                                    "बदलाव की तारीख ${formatEpochDate(next.updatedAt)}"
                                }
                                Text(
                                    text = changedText,
                                    fontSize = 11.sp,
                                    color = MandiTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (isEnglish) "Close" else "बंद करें",
                    color = MandiTextPrimary
                )
            }
        }
    )
}
