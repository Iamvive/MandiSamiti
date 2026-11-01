package com.appwork.mandisamiti.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

/**
 * Standardized MandiChip conforming strictly to NGDL tokens.
 * Selected fill uses MandiPrimaryAction (inverted with theme).
 * Never uses unapproved colors (no lavender).
 */
@Composable
fun MandiChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    count: Int? = null
) {
    val containerColor = if (selected) MandiPrimaryAction else MandiSurface
    val contentColor = if (selected) MandiPrimaryActionText else MandiTextPrimary
    val borderColor = if (selected) MandiPrimaryAction else MandiBorder

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            )
            if (count != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) MandiPrimaryActionText.copy(alpha = 0.2f) else MandiSurfaceElevated
                ) {
                    Text(
                        text = count.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

