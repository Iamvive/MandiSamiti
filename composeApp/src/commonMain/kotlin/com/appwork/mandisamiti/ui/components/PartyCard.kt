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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.appwork.mandisamiti.ui.theme.MandiGreenBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenLight
import com.appwork.mandisamiti.ui.theme.MandiGreenPayable
import com.appwork.mandisamiti.ui.theme.MandiGreenText
import com.appwork.mandisamiti.ui.theme.MandiNeutralBorder
import com.appwork.mandisamiti.ui.theme.MandiNeutralLight
import com.appwork.mandisamiti.ui.theme.MandiNeutralText
import com.appwork.mandisamiti.ui.theme.MandiRedBorder
import com.appwork.mandisamiti.ui.theme.MandiRedLight
import com.appwork.mandisamiti.ui.theme.MandiRedReceivable
import com.appwork.mandisamiti.ui.theme.MandiRedText
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
    val chipShape = RoundedCornerShape(6.dp)

    val isReceivable = balancePaisa > 0 // लेना है (Red)
    val isPayable = balancePaisa < 0    // देना है (Green)
    val isSettled = balancePaisa == 0L

    val balanceTextColor = when {
        isReceivable -> MandiRedReceivable
        isPayable -> MandiGreenPayable
        else -> MandiTextSecondary
    }

    val chipBg = when {
        isReceivable -> MandiRedLight
        isPayable -> MandiGreenLight
        else -> MandiNeutralLight
    }

    val chipBorder = when {
        isReceivable -> MandiRedBorder
        isPayable -> MandiGreenBorder
        else -> MandiNeutralBorder
    }

    val chipText = when {
        isReceivable -> "लेना है"
        isPayable -> "देना है"
        else -> "हिसाब चुकता"
    }

    val chipTextColor = when {
        isReceivable -> MandiRedText
        isPayable -> MandiGreenText
        else -> MandiNeutralText
    }

    val roleLabel = if (party.partyType == PartyType.FARMER) "किसान" else "व्यापारी"
    val avatarInitial = party.name.firstOrNull()?.toString() ?: "प"
    val village = party.village

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MandiSurface)
            .border(1.dp, MandiBorder, cardShape)
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Avatar + Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Monogram Avatar
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MandiNeutralLight)
                        .border(1.dp, MandiBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = avatarInitial,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = party.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                        // Role Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MandiNeutralLight)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = roleLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MandiTextSecondary
                            )
                        }
                    }

                    if (!village.isNullOrBlank()) {
                        Text(
                            text = village,
                            fontSize = 13.sp,
                            color = MandiTextSecondary
                        )
                    }
                }
            }

            // Right: Clean Financial Amount & Status Badge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val displayAmount = if (balancePaisa < 0) -balancePaisa else balancePaisa
                Text(
                    text = "₹${MandiMathEngine.paisaToRupeesString(displayAmount)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = balanceTextColor
                )

                // Semantic Pill Badge (No emojis)
                Box(
                    modifier = Modifier
                        .clip(chipShape)
                        .background(chipBg)
                        .border(1.dp, chipBorder, chipShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = chipText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = chipTextColor
                    )
                }
            }
        }
    }
}
