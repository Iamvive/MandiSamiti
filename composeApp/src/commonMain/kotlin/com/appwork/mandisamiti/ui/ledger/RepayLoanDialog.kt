package com.appwork.mandisamiti.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessBg
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessFg
import com.appwork.mandisamiti.ui.theme.MandiGreenReceivable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

/** D4.4: how a payment lands once the trader has settled on the interest figure. All values in paisa. */
data class RepayPreview(
    val interestPaisa: Long,
    val principalPaisa: Long,
    val remainingPrincipalPaisa: Long,
    val generalJamaPaisa: Long
)

fun repayPreview(paymentPaisa: Long, interestPaisa: Long, outstandingPrincipalPaisa: Long): RepayPreview {
    val byInterest = RuralInterestEngine.splitRepayment(paymentPaisa, interestPaisa)
    val principal = minOf(byInterest.principalPaisa, outstandingPrincipalPaisa.coerceAtLeast(0L))
    return RepayPreview(
        interestPaisa = byInterest.interestPaisa,
        principalPaisa = principal,
        remainingPrincipalPaisa = (outstandingPrincipalPaisa - principal).coerceAtLeast(0L),
        generalJamaPaisa = byInterest.principalPaisa - principal
    )
}

private fun rupees(paisa: Long) = "₹${MandiMathEngine.formatIndianGrouping(paisa / 100L)}"

/**
 * Repayment against a money loan: amount and an editable interest figure (prefilled with the calculated one),
 * with a live preview of the interest / principal / remaining split before anything is written.
 */
@Composable
fun RepayLoanDialog(
    outstandingPrincipalPaisa: Long,
    calculatedInterestPaisa: Long,
    monthlyRatePercent: Double,
    isEnglish: Boolean,
    onConfirm: (paymentPaisa: Long, interestPaisa: Long, calculatedInterestPaisa: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var interestInput by remember { mutableStateOf((calculatedInterestPaisa / 100L).toString()) }

    val paymentPaisa = (amountInput.toLongOrNull() ?: 0L) * 100L
    val interestPaisa = (interestInput.toLongOrNull() ?: 0L) * 100L
    val preview = repayPreview(paymentPaisa, interestPaisa, outstandingPrincipalPaisa)
    val interestChanged = interestPaisa != calculatedInterestPaisa / 100L * 100L

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MandiSurface,
        title = { Text(if (isEnglish) "Repay loan" else "उधार वापसी", fontWeight = FontWeight.Bold, color = MandiTextPrimary) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = (if (isEnglish) "Principal due " else "बकाया मूल ") + rupees(outstandingPrincipalPaisa),
                    fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MandiGreenReceivable
                )
                Text(
                    text = (if (isEnglish) "Interest due " else "ब्याज बकाया ") + rupees(calculatedInterestPaisa) +
                        " ($monthlyRatePercent%" + (if (isEnglish) "/month)" else "/माह)"),
                    fontSize = 13.sp, color = MandiTextSecondary
                )
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { v -> amountInput = v.filter { it.isDigit() }.take(9) },
                    label = { Text(if (isEnglish) "Amount received (₹)" else "प्राप्त राशि (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = interestInput,
                    onValueChange = { v -> interestInput = v.filter { it.isDigit() }.take(9) },
                    label = { Text(if (isEnglish) "Interest (₹)" else "ब्याज (₹)") },
                    supportingText = if (interestChanged) {
                        { Text((if (isEnglish) "Calculated " else "हिसाब से ") + rupees(calculatedInterestPaisa), color = MandiTextMuted) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (paymentPaisa > 0L) {
                    Text(
                        text = if (isEnglish) {
                            "Interest ${rupees(preview.interestPaisa)} · Principal ${rupees(preview.principalPaisa)} · Principal left ${rupees(preview.remainingPrincipalPaisa)}"
                        } else {
                            "ब्याज ${rupees(preview.interestPaisa)} · मूल ${rupees(preview.principalPaisa)} · बाकी मूल ${rupees(preview.remainingPrincipalPaisa)}"
                        },
                        fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MandiTextPrimary
                    )
                    if (preview.generalJamaPaisa > 0L) {
                        Text(
                            text = if (isEnglish) "${rupees(preview.generalJamaPaisa)} is more than owed and goes to general credit"
                            else "${rupees(preview.generalJamaPaisa)} बकाया से ज्यादा है, सामान्य जमा में जाएगा",
                            fontSize = 12.sp, color = MandiTextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(paymentPaisa, interestPaisa, calculatedInterestPaisa) },
                enabled = paymentPaisa > 0L,
                colors = ButtonDefaults.buttonColors(containerColor = MandiBtnSuccessBg, contentColor = MandiBtnSuccessFg)
            ) { Text(if (isEnglish) "Save" else "दर्ज करें", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isEnglish) "Cancel" else "रद्द करें", color = MandiTextSecondary) }
        }
    )
}
