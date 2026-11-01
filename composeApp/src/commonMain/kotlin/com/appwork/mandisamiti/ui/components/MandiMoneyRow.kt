package com.appwork.mandisamiti.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.ui.theme.MandiGreenReceivable
import com.appwork.mandisamiti.ui.theme.MandiRedPayable
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

enum class MoneyRowTone {
    NEUTRAL,
    RECEIVABLE, // Green (लेना है)
    PAYABLE     // Red (देना है)
}

/**
 * Standardized Financial Money Row.
 * Label left wraps gracefully on narrow viewports; amount right remains mono-aligned.
 * Strictly adheres to NGDL §5.5 (no horizontal scroll at 390dp).
 */
@Composable
fun MandiMoneyRow(
    label: String,
    amountText: String,
    modifier: Modifier = Modifier,
    subLabel: String? = null,
    emphasized: Boolean = false,
    tone: MoneyRowTone = MoneyRowTone.NEUTRAL
) {
    val amountColor = when (tone) {
        MoneyRowTone.RECEIVABLE -> MandiGreenReceivable
        MoneyRowTone.PAYABLE -> MandiRedPayable
        MoneyRowTone.NEUTRAL -> if (emphasized) MandiTextPrimary else MandiTextPrimary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (emphasized) 6.dp else 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                fontSize = if (emphasized) 15.sp else 13.sp,
                fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
                color = if (emphasized) MandiTextPrimary else MandiTextSecondary
            )
            if (subLabel != null) {
                Text(
                    text = subLabel,
                    fontSize = 11.sp,
                    color = MandiTextSecondary
                )
            }
        }

        Text(
            text = amountText,
            fontSize = if (emphasized) 17.sp else 14.sp,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Default,
            color = amountColor,
            maxLines = 1
        )
    }
}

