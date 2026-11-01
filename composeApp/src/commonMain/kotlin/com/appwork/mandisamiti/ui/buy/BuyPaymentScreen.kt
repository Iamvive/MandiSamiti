package com.appwork.mandisamiti.ui.buy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.slipLabel
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.ui.components.MandiCard
import com.appwork.mandisamiti.ui.components.MandiChip
import com.appwork.mandisamiti.ui.components.MandiPrimaryButton
import com.appwork.mandisamiti.ui.components.MandiScreenHeader
import com.appwork.mandisamiti.ui.components.MandiSecondaryButton
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessBg
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

enum class PaymentMode { PARTIAL, FULL }

/**
 * Direction B final: Post-Save Payment Screen (किसान को भुगतान).
 * Displays immediately after saving a new Buy slip.
 * Allows recording cash payout out of Galla or skipping to view the bill/receipt.
 */
@Composable
fun BuyPaymentScreen(
    entry: BuyEntry,
    farmerName: String,
    onSavePayment: (amountPaisa: Long) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    isEnglish: Boolean = false,
) {
    MandiBackHandler { onSkip() }

    val totalPayablePaisa = entry.netPayablePaisa
    val advanceOrCutPaisa = entry.paidPaisa
    val finalPayablePaisa = BuyPaymentCalculations.calculateFinalPayable(totalPayablePaisa, advanceOrCutPaisa)

    var payMode by remember { mutableStateOf(PaymentMode.FULL) }
    var customAmountText by remember { mutableStateOf("") }

    val amountToPayPaisa = when (payMode) {
        PaymentMode.FULL -> finalPayablePaisa
        PaymentMode.PARTIAL -> {
            val enteredRupees = customAmountText.toLongOrNull() ?: 0L
            BuyPaymentCalculations.rupeesToPaisa(enteredRupees).coerceAtMost(finalPayablePaisa)
        }
    }

    val remainingLeftPaisa = BuyPaymentCalculations.calculateRemainingLeft(finalPayablePaisa, amountToPayPaisa)
    val slipNum = slipLabel(entry.slipNo, entry.id)

    Scaffold(
        topBar = {
            MandiScreenHeader(
                title = if (isEnglish) "Pay the Farmer" else "किसान को भुगतान",
                subtitle = if (isEnglish) "Slip #$slipNum • $farmerName" else "पर्ची #$slipNum • $farmerName",
                onBack = onSkip
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MandiPrimaryButton(
                    text = if (isEnglish) "Save Payment" else "भुगतान सुरक्षित करें",
                    onClick = {
                        if (amountToPayPaisa > 0L) {
                            onSavePayment(amountToPayPaisa)
                        } else {
                            onSkip()
                        }
                    },
                    enabled = finalPayablePaisa > 0L && amountToPayPaisa > 0L,
                    modifier = Modifier.fillMaxWidth()
                )

                MandiSecondaryButton(
                    text = if (isEnglish) "Not Now" else "अभी नहीं",
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Success confirmation tile
            MandiCard(modifier = Modifier.fillMaxWidth(), isElevated = true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MandiBtnSuccessBg,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isEnglish) "Slip #$slipNum Saved" else "पर्ची #$slipNum सुरक्षित हुई",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MandiTextPrimary
                    )
                }
            }

            // Final payable card
            MandiCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isEnglish) "Final Payable" else "अंतिम देय राशि",
                    fontSize = 13.sp,
                    color = MandiTextSecondary
                )
                Text(
                    text = MandiMathEngine.formatRupees(finalPayablePaisa),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Payment mode & input card
            if (finalPayablePaisa > 0) {
                MandiCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isEnglish) "Payment Amount" else "भुगतान राशि",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MandiTextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MandiChip(
                            label = if (isEnglish) "Full Payout" else "पूरा भुगतान",
                            selected = payMode == PaymentMode.FULL,
                            onClick = { payMode = PaymentMode.FULL }
                        )
                        MandiChip(
                            label = if (isEnglish) "Partial Cash" else "आंशिक नकद",
                            selected = payMode == PaymentMode.PARTIAL,
                            onClick = {
                                payMode = PaymentMode.PARTIAL
                                if (customAmountText.isBlank()) {
                                    customAmountText = ""
                                }
                            }
                        )
                    }

                    if (payMode == PaymentMode.PARTIAL) {
                        OutlinedTextField(
                            value = customAmountText,
                            onValueChange = { input ->
                                customAmountText = input.filter { it.isDigit() }
                            },
                            label = { Text(if (isEnglish) "Pay Cash Now (₹)" else "अभी नकद दें (रु)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Slip Left: ${MandiMathEngine.formatRupees(remainingLeftPaisa)}" else "पर्ची बाकी: ${MandiMathEngine.formatRupees(remainingLeftPaisa)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MandiTextPrimary
                        )
                        Text(
                            text = if (isEnglish) "${MandiMathEngine.formatRupees(amountToPayPaisa)} cash goes out of the Galla" else "गल्ले से ${MandiMathEngine.formatRupees(amountToPayPaisa)} नकद निकलेगा",
                            fontSize = 12.sp,
                            color = MandiTextSecondary
                        )
                    }
                }
            } else {
                MandiCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isEnglish) "No payout pending. Slip fully settled by advance / loan cut." else "कोई भुगतान शेष नहीं है। पर्ची अग्रिम / उधार कटौती से पूर्णतः चुकता है।",
                        fontSize = 14.sp,
                        color = MandiTextSecondary
                    )
                }
            }
        }
    }
}
