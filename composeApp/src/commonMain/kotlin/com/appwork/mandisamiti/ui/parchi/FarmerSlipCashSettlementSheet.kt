package com.appwork.mandisamiti.ui.parchi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.slipLabel
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenText
import com.appwork.mandisamiti.ui.theme.MandiGreenReceivable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

@Composable
fun FarmerSlipCashSettlementSheet(
    buy: BuyEntry,
    farmerName: String,
    gallaCashPaisa: Long,
    onDismiss: () -> Unit,
    onConfirmPayment: (amountPaisa: Long) -> Unit,
    isEnglish: Boolean = false
) {
    SlipCashSettlementSheet(
        slipLabelText = slipLabel(buy.slipNo, buy.id),
        partyName = farmerName,
        totalPaisa = buy.netPayablePaisa,
        settledPaisa = buy.paidPaisa,
        gallaCashPaisa = gallaCashPaisa,
        isReceiving = false,
        onDismiss = onDismiss,
        onConfirmPayment = onConfirmPayment,
        isEnglish = isEnglish
    )
}

@Composable
fun TraderSlipCashSettlementSheet(
    sell: SellEntry,
    traderName: String,
    gallaCashPaisa: Long,
    onDismiss: () -> Unit,
    onConfirmReceipt: (amountPaisa: Long) -> Unit,
    isEnglish: Boolean = false
) {
    SlipCashSettlementSheet(
        slipLabelText = slipLabel(sell.slipNo, sell.id),
        partyName = traderName,
        totalPaisa = sell.totalReceivablePaisa,
        settledPaisa = sell.receivedPaisa,
        gallaCashPaisa = gallaCashPaisa,
        isReceiving = true,
        onDismiss = onDismiss,
        onConfirmPayment = onConfirmReceipt,
        isEnglish = isEnglish
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlipCashSettlementSheet(
    slipLabelText: String,
    partyName: String,
    totalPaisa: Long,
    settledPaisa: Long,
    gallaCashPaisa: Long,
    isReceiving: Boolean,
    onDismiss: () -> Unit,
    onConfirmPayment: (amountPaisa: Long) -> Unit,
    isEnglish: Boolean
) {
    val remainingPaisa = (totalPaisa - settledPaisa).coerceAtLeast(0L)
    val remainingRupees = remainingPaisa / 100
    var inputRupeesText by remember { mutableStateOf(remainingRupees.toString()) }

    val enteredRupees = inputRupeesText.toLongOrNull() ?: 0L
    val enteredPaisa = enteredRupees * 100L
    val isValid = enteredPaisa in 1L..remainingPaisa

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MandiSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MandiBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isReceiving) {
                            if (isEnglish) "Slip #$slipLabelText Cash Received" else "पर्ची #$slipLabelText नकद प्राप्त"
                        } else {
                            if (isEnglish) "Slip #$slipLabelText Cash Payment" else "पर्ची #$slipLabelText नकद भुगतान"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                    Text(
                        text = partyName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MandiTextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MandiTextSecondary)
                }
            }

            // Galla Cash Liquidity Indicator
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MandiSurfaceElevated,
                border = BorderStroke(1.dp, MandiBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Galla Cash Available:" else "गल्ला नकद उपलब्ध:",
                        fontSize = 11.sp,
                        color = MandiTextSecondary
                    )
                    Text(
                        text = MandiMathEngine.formatRupees(gallaCashPaisa),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (gallaCashPaisa >= 0L) MandiGreenText else MandiAmberDark
                    )
                }
            }

            // Context Summary Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MandiSurfaceElevated,
                border = BorderStroke(1.dp, MandiBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(if (isEnglish) "Total Bill" else "कुल पर्ची", fontSize = 10.sp, color = MandiTextSecondary)
                        Text("₹${MandiMathEngine.formatIndianGrouping(totalPaisa / 100)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("|", color = MandiBorder)
                    Column {
                        Text(if (isEnglish) (if (isReceiving) "Received" else "Paid") else (if (isReceiving) "पूर्व प्राप्त" else "पूर्व भुगतान"), fontSize = 10.sp, color = MandiTextSecondary)
                        Text("₹${MandiMathEngine.formatIndianGrouping(settledPaisa / 100)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("|", color = MandiBorder)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (isEnglish) "Balance Due" else (if (isReceiving) "बाकी लेना" else "बाकी देय"), fontSize = 10.sp, color = MandiAmberDark)
                        Text("₹${MandiMathEngine.formatIndianGrouping(remainingRupees)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MandiAmberDark)
                    }
                }
            }

            // Editable Cash Amount Field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isReceiving) {
                        if (isEnglish) "Cash amount received into Galla (₹):" else "आज गल्ले में आने वाला नकद (₹):"
                    } else {
                        if (isEnglish) "Cash amount to pay from Galla (₹):" else "आज गल्ला से दिया जाने वाला नकद (₹):"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                )
                OutlinedTextField(
                    value = inputRupeesText,
                    onValueChange = { inputRupeesText = it.filter { ch -> ch.isDigit() } },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MandiTextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MandiSurface,
                        unfocusedContainerColor = MandiSurface,
                        focusedBorderColor = MandiGreenReceivable,
                        unfocusedBorderColor = MandiBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(4.dp))

            // Primary Tactile CTA
            Button(
                onClick = {
                    if (isValid) {
                        onConfirmPayment(enteredPaisa)
                    }
                },
                enabled = isValid,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MandiGreenReceivable,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (isReceiving) {
                        if (isEnglish) "Receive ₹${MandiMathEngine.formatIndianGrouping(enteredRupees)} Cash into Galla" else "गल्ले में ₹${MandiMathEngine.formatIndianGrouping(enteredRupees)} नकद लें →"
                    } else {
                        if (isEnglish) "Pay ₹${MandiMathEngine.formatIndianGrouping(enteredRupees)} Cash from Galla" else "गल्ला से ₹${MandiMathEngine.formatIndianGrouping(enteredRupees)} नकद दें →"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
