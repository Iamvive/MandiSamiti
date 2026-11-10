package com.appwork.mandisamiti.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.data.repository.ShopQuota
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiRedPayable
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary
import kotlin.math.max
import kotlin.math.min

/**
 * Checks whether the quota warning banner should be displayed.
 * Only displays when bills created this month >= 80% of effective limit (and not PRO tier).
 */
fun shouldShowQuotaWarning(quota: ShopQuota): Boolean {
    if (quota.planTier == "PRO") return false
    val effectiveLimit = quota.effectiveLimit
    if (effectiveLimit <= 0L) return true
    return quota.billsCreatedThisMonth.toDouble() >= (effectiveLimit * 0.8)
}

/**
 * Calculates linear progress bar fraction [0f, 1f].
 */
fun calculateQuotaProgressFraction(quota: ShopQuota): Float {
    val effectiveLimit = quota.effectiveLimit
    if (effectiveLimit <= 0L) return 1f
    val fraction = quota.billsCreatedThisMonth.toFloat() / effectiveLimit.toFloat()
    return min(1f, max(0f, fraction))
}

/**
 * Quota warning banner conforming to NGDL §5.3.
 * Subtle warm elevated container, linear progress, clear typography, no unicode emojis.
 */
@Composable
fun QuotaProgressBanner(
    quota: ShopQuota,
    isEnglish: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    if (!shouldShowQuotaWarning(quota)) return

    val fraction = calculateQuotaProgressFraction(quota)
    val remaining = max(0L, quota.effectiveLimit - quota.billsCreatedThisMonth)
    val isLimitExceeded = quota.billsCreatedThisMonth >= quota.effectiveLimit

    val accentColor = if (isLimitExceeded) MandiRedPayable else MandiAmberDark

    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MandiSurfaceElevated
        ),
        border = BorderStroke(1.dp, MandiBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isEnglish) "Monthly Limit" else "मासिक कोटा सीमा",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MandiTextPrimary
                    )
                }

                Text(
                    text = "${quota.billsCreatedThisMonth} / ${quota.effectiveLimit} ${if (isEnglish) "Bills" else "पर्चे"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val subtitleText = when {
                    isLimitExceeded -> if (isEnglish) {
                        "Monthly limit reached. Upgrade or claim extension."
                    } else {
                        "मासिक सीमा समाप्त हो गई है। प्लान अपग्रेड करें।"
                    }
                    else -> if (isEnglish) {
                        "$remaining bills remaining this month."
                    } else {
                        "इस महीने $remaining पर्चे शेष हैं।"
                    }
                }

                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MandiTextSecondary,
                    modifier = Modifier.weight(1f)
                )

                if (onClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MandiTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
