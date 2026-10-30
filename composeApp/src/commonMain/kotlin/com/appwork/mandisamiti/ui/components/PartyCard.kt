package com.appwork.mandisamiti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhoneDisabled
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiAmberLight
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenLight
import com.appwork.mandisamiti.ui.theme.MandiGreenReceivable
import com.appwork.mandisamiti.ui.theme.MandiGreenText
import com.appwork.mandisamiti.ui.theme.MandiNavy
import com.appwork.mandisamiti.ui.theme.MandiNeutralBorder
import com.appwork.mandisamiti.ui.theme.MandiNeutralLight
import com.appwork.mandisamiti.ui.theme.MandiNeutralText
import com.appwork.mandisamiti.ui.theme.MandiRedBorder
import com.appwork.mandisamiti.ui.theme.MandiRedLight
import com.appwork.mandisamiti.ui.theme.MandiRedPayable
import com.appwork.mandisamiti.ui.theme.MandiRedText
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

@Composable
fun PartyCard(
    party: Party,
    balancePaisa: Long,
    onClick: () -> Unit,
    isEnglish: Boolean = false,
    onPhoneNeededClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val cardShape = RoundedCornerShape(12.dp)
    val chipShape = RoundedCornerShape(6.dp)

    val isReceivable = balancePaisa > 0 // लेना है (Green)
    val isPayable = balancePaisa < 0    // देना है (Red)

    val balanceTextColor = when {
        isReceivable -> MandiGreenReceivable
        isPayable -> MandiRedPayable
        else -> MandiTextSecondary
    }

    val chipBg = when {
        isReceivable -> MandiGreenLight
        isPayable -> MandiRedLight
        else -> MandiNeutralLight
    }

    val chipBorder = when {
        isReceivable -> MandiGreenBorder
        isPayable -> MandiRedBorder
        else -> MandiNeutralBorder
    }

    val chipText = when {
        isReceivable -> if (isEnglish) "To receive" else "लेना है"
        isPayable -> if (isEnglish) "To pay" else "देना है"
        else -> if (isEnglish) "Settled" else "हिसाब चुकता"
    }

    val chipTextColor = when {
        isReceivable -> MandiGreenText
        isPayable -> MandiRedText
        else -> MandiNeutralText
    }

    val leftAccentColor = when {
        isReceivable -> MandiGreenReceivable
        isPayable -> MandiRedPayable
        else -> MandiBorder
    }

    val roleLabel = if (party.partyType == PartyType.FARMER) {
        if (isEnglish) "Farmer" else "किसान"
    } else {
        if (isEnglish) "Trader" else "व्यापारी"
    }
    val avatarInitial = party.name.firstOrNull()?.toString() ?: "प"
    val village = party.village

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MandiSurface)
            .border(1.dp, MandiBorder, cardShape)
            .clickable { onClick() }
    ) {
        // 3dp Subtle Left Status Accent Bar (Properly constrained)
        if (isReceivable || isPayable) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .matchParentSize()
                    .align(Alignment.CenterStart)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(leftAccentColor)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 14.dp, top = 13.dp, bottom = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Avatar + Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Minimal Monogram Avatar
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MandiSurfaceElevated)
                        .border(1.dp, MandiBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = avatarInitial,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = party.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        PartyRoleIcon(partyType = party.partyType, contentDescription = roleLabel)
                    }
                    if (party.syncStatus != 1) {
                        SyncStatusPill(isSynced = false, isEnglish = isEnglish)
                    }

                    if (!village.isNullOrBlank()) {
                        Text(
                            text = village,
                            fontSize = 13.sp,
                            color = MandiTextSecondary
                        )
                    }
                    if (!party.gstin.isNullOrBlank()) {
                        Text(
                            text = "GST: ${party.gstin}",
                            fontSize = 11.sp,
                            color = MandiTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Right: Financial Amount & Status Badge
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val hasPhone = !party.phone.isNullOrBlank()
                    // Phone icon: tap to call when saved, tap to be prompted to add one when missing.
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (hasPhone) {
                                    runCatching { uriHandler.openUri("tel:${party.phone}") }
                                } else {
                                    onPhoneNeededClick?.invoke()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasPhone) Icons.Outlined.Phone else Icons.Outlined.PhoneDisabled,
                            contentDescription = when {
                                hasPhone -> if (isEnglish) "Call" else "कॉल करें"
                                else -> if (isEnglish) "No phone number" else "फ़ोन नंबर नहीं है"
                            },
                            tint = if (hasPhone) MandiNavy else MandiAmberDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Semantic Status Pill
                    Box(
                        modifier = Modifier
                            .clip(chipShape)
                            .background(chipBg)
                            .border(1.dp, chipBorder, chipShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = chipText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = chipTextColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartyRoleIcon(
    partyType: PartyType,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val isFarmer = partyType == PartyType.FARMER
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isFarmer) MandiAmberLight else MandiNeutralLight)
            .border(1.dp, if (isFarmer) MandiAmberDark.copy(alpha = 0.35f) else MandiBorder, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isFarmer) Icons.Default.Agriculture else Icons.Default.Store,
            contentDescription = contentDescription,
            tint = if (isFarmer) MandiAmberDark else MandiNavy,
            modifier = Modifier.size(13.dp)
        )
    }
}

