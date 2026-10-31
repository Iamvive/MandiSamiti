package com.appwork.mandisamiti.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiRedPayable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

/** One crop line on a bill: name + amount, with qty/rate underneath. */
data class ReceiptLot(val title: String, val detail: String, val amount: String)

/** Shared frame for buy slip and sell memo: top bar, one card, one share button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScaffold(
    title: String,
    onBack: () -> Unit,
    onEdit: (() -> Unit)?,
    shareLabel: String,
    onShare: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "पीछे")
                    }
                },
                actions = {
                    if (onEdit != null) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "सुधारें (Edit)")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
            if (onShare != null) {
                Button(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MandiPrimaryAction,
                        contentColor = MandiPrimaryActionText
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(shareLabel, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
fun ReceiptCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(MandiBorder))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) { content() }
    }
}

/** Top of the card: who, what number, when, and the one primary amount. */
@Composable
fun ReceiptHero(
    partyName: String,
    meta: String,
    amountLabel: String,
    amount: String,
    isVoid: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            partyName,
            modifier = Modifier.weight(1f),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MandiTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (isVoid) Text("रद्द", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MandiRedPayable)
    }
    Text(meta, fontSize = 13.sp, color = MandiTextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
    Text(amountLabel, fontSize = 13.sp, color = MandiTextSecondary, modifier = Modifier.padding(top = 8.dp))
    Text(
        amount,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = MandiTextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    HorizontalDivider(color = MandiBorder, modifier = Modifier.padding(vertical = 6.dp))
}

@Composable
fun ReceiptLotRow(lot: ReceiptLot) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                lot.title,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
                color = MandiTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(lot.amount, fontWeight = FontWeight.SemiBold, color = MandiTextPrimary, maxLines = 1)
        }
        Text(lot.detail, fontSize = 12.sp, color = MandiTextSecondary)
    }
}

/** Label left (wraps), amount right (one line). */
@Composable
fun ReceiptAmountRow(label: String, value: String, emphasized: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontSize = if (emphasized) 16.sp else 14.sp,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
            color = if (emphasized) MandiTextPrimary else MandiTextSecondary
        )
        Text(
            value,
            fontSize = if (emphasized) 16.sp else 14.sp,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            color = MandiTextPrimary,
            maxLines = 1
        )
    }
}

@Composable
fun ReceiptDivider() {
    HorizontalDivider(color = MandiBorder, modifier = Modifier.padding(vertical = 6.dp))
}
