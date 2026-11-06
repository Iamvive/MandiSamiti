package com.appwork.mandisamiti.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.ui.theme.MandiRedPayable
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

/**
 * Standardized Empty State View.
 * Displays clean vector icon, headline, helpful description, and optional action.
 * Pure vector icons only — strictly zero emoji.
 */
@Composable
fun MandiEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector = Icons.Default.Info,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MandiTextMuted,
            modifier = Modifier.size(44.dp)
        )

        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MandiTextPrimary,
            textAlign = TextAlign.Center
        )

        if (description != null) {
            Text(
                text = description,
                fontSize = 13.sp,
                color = MandiTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }

        if (action != null) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                action()
            }
        }
    }
}

/**
 * Standardized Error State View.
 * Displays warning icon, clear error message, and a retry button.
 */
@Composable
fun MandiErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = "पुनः प्रयास करें",
    icon: ImageVector = Icons.Default.Warning
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MandiRedPayable,
            modifier = Modifier.size(40.dp)
        )

        Text(
            text = message,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MandiTextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        MandiOutlinedButton(
            text = retryLabel,
            onClick = onRetry,
            fullWidth = false,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

