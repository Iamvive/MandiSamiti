package com.appwork.mandisamiti.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenText
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

/**
 * Reusable NGDL v1.2 Tutorial Action Pill.
 * Touch target conforms strictly to >= 48dp ergonomics for Age 25–65.
 * Placed in the thumb-reachable zone of core screens.
 */
@Composable
fun TutorialPill(
    tutorial: MandiTutorial,
    isEnglish: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(20.dp)

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(pillShape)
            .background(MandiSurfaceElevated)
            .border(1.dp, MandiBorder, pillShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PlayCircle,
            contentDescription = null,
            tint = MandiGreenText,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = tutorial.title(isEnglish),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MandiTextPrimary
        )
        Text(
            text = "• ${tutorial.duration(isEnglish)}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MandiTextSecondary
        )
    }
}
