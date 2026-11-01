package com.appwork.mandisamiti.ui.buy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.slipLabel
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.ui.components.MandiCard
import com.appwork.mandisamiti.ui.components.MandiChip
import com.appwork.mandisamiti.ui.components.MandiFlatTextField
import com.appwork.mandisamiti.ui.components.MandiPrimaryButton
import com.appwork.mandisamiti.ui.components.MandiScreenHeader
import com.appwork.mandisamiti.ui.components.MandiSecondaryButton
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

enum class PaymentMode { PARTIAL, FULL }

/**
 * Direction B final: Post-Save Payment Screen (किसान को भुगतान).
 * Displays immediately after saving a new Buy slip.
 * Allows recording cash payout out of Galla or skipping ("अभी नहीं") to view the bill/receipt.
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

    var payMode by remember { mutableStateOf(PaymentMode.PARTIAL) }
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
                title = if (isEnglish) "Pay the farmer" else "किसान को भुगतान",
                subtitle = if (isEnglish) "Slip #$slipNum · $farmerName" else "पर्ची #$slipNum · $farmerName",
                onBack = onSkip
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MandiPrimaryButton(
                    text = if (isEnglish) "Save payment" else "भुगतान सुरक्षित करें",
                    onClick = {
                        if (amountToPayPaisa > 0L) {
                            onSavePayment(amountToPayPaisa)
                        } else {
                            onSkip()
                        }
                    },
                    height = 56.dp,
                    fontSize = 16.sp,
                    enabled = finalPayablePaisa > 0L && amountToPayPaisa > 0L,
                    modifier = Modifier.fillMaxWidth()
                )

                MandiSecondaryButton(
                    text = if (isEnglish) "Not now" else "अभी नहीं",
                    onClick = onSkip,
                    height = 48.dp,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        modifier = modifier.fillMaxSize().background(MandiBackground)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Success confirmation tile (tile row f14 w6)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MandiSurfaceElevated, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MandiTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isEnglish) "Slip #$slipNum saved" else "पर्ची #$slipNum सुरक्षित हुई",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MandiTextPrimary
                    )
                }
            }

            // Final payable card (card pad stack)
            MandiCard(padding = 14.dp, spacing = 2.dp) {
                Text(
                    text = if (isEnglish) "Final payable" else "अंतिम देय राशि",
                    fontSize = 14.sp,
                    color = MandiTextSecondary
                )
                Text(
                    text = MandiMathEngine.formatRupees(finalPayablePaisa),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                )
            }

            // Payment amount & mode card
            if (finalPayablePaisa > 0) {
                MandiCard(padding = 14.dp, spacing = 12.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MandiChip(
                            label = if (isEnglish) "Partial" else "आंशिक",
                            selected = payMode == PaymentMode.PARTIAL,
                            onClick = {
                                payMode = PaymentMode.PARTIAL
                            }
                        )
                        MandiChip(
                            label = if (isEnglish) "Full" else "पूरा",
                            selected = payMode == PaymentMode.FULL,
                            onClick = {
                                payMode = PaymentMode.FULL
                                customAmountText = MandiMathEngine.paisaToRupeesString(finalPayablePaisa)
                            }
                        )
                    }

                    MandiFlatTextField(
                        value = if (payMode == PaymentMode.FULL) MandiMathEngine.paisaToRupeesString(finalPayablePaisa) else customAmountText,
                        onValueChange = { input ->
                            payMode = PaymentMode.PARTIAL
                            customAmountText = input.filter { it.isDigit() }
                        },
                        label = if (isEnglish) "Pay cash now (₹)" else "अभी नकद दें (रु)",
                        placeholder = MandiMathEngine.paisaToRupeesString(finalPayablePaisa),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        isTall = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Slip left ${MandiMathEngine.formatRupees(remainingLeftPaisa)}" else "पर्ची बाकी ${MandiMathEngine.formatRupees(remainingLeftPaisa)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MandiTextPrimary
                        )
                        Text(
                            text = if (isEnglish) "${MandiMathEngine.formatRupees(amountToPayPaisa)} cash goes out of the galla" else "गल्ले से ${MandiMathEngine.formatRupees(amountToPayPaisa)} नकद निकलेगा",
                            fontSize = 12.sp,
                            color = MandiTextSecondary
                        )
                    }
                }
            } else {
                MandiCard(padding = 14.dp, spacing = 8.dp) {
                    Text(
                        text = if (isEnglish) "No payout pending. Slip fully settled." else "कोई भुगतान शेष नहीं है। पर्ची पूर्णतः चुकता है।",
                        fontSize = 14.sp,
                        color = MandiTextSecondary
                    )
                }
            }
        }
    }
}
