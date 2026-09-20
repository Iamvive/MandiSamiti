package com.appwork.mandisamiti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenLight
import com.appwork.mandisamiti.ui.theme.MandiGreenPayable
import com.appwork.mandisamiti.ui.theme.MandiNavy
import com.appwork.mandisamiti.ui.theme.MandiRedLight
import com.appwork.mandisamiti.ui.theme.MandiRedReceivable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

@Composable
fun PartyCard(
    party: Party,
    balancePaisa: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(12.dp)
    val isReceivable = balancePaisa > 0 // लेना है (Red)
    val isPayable = balancePaisa < 0    // देना है (Green)
    val isSettled = balancePaisa == 0L

    val balanceColor = when {
        isReceivable -> MandiRedReceivable
        isPayable -> MandiGreenPayable
        else -> MandiTextMuted
    }

    val balanceTagText = when {
        isReceivable -> "🔴 लेना है"
        isPayable -> "🟢 देना है"
        else -> "⚪ बराबर"
    }

    val badgeBg = when {
        isReceivable -> MandiRedLight
        isPayable -> MandiGreenLight
        else -> Color(0xFFF1F5F9)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MandiSurface)
            .border(1.5.dp, MandiBorder, cardShape)
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Party Details with Village Badge
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = party.name,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                    if (!party.village.isNullOrBlank()) {
                        Text(
                            text = "(${party.village})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MandiNavy
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (party.partyType == PartyType.FARMER) "🌾 किसान" else "🏭 व्यापारी",
                        fontSize = 13.sp,
                        color = MandiTextSecondary
                    )
                    if (!party.phone.isNullOrBlank()) {
                        Text(
                            text = "📞 ${party.phone}",
                            fontSize = 13.sp,
                            color = MandiTextMuted
                        )
                    }
                }
            }

            // Right: Bold High-Contrast Balance & Tag
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "₹ ${MandiMathEngine.paisaToRupeesString(if (balancePaisa < 0) -balancePaisa else balancePaisa)}",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = balanceColor
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = balanceTagText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor
                    )
                }
            }
        }
    }
}
