package com.appwork.mandisamiti.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiBtnDangerBg
import com.appwork.mandisamiti.ui.theme.MandiBtnDangerFg
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessBg
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessFg
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiSecondaryAction
import com.appwork.mandisamiti.ui.theme.MandiSecondaryActionText
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary

/**
 * Primary Solid Action Button.
 * Inverts with theme: dark text on white in dark theme; white text on black in light theme.
 */
@Composable
fun MandiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    fullWidth: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 48.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp
) {
    val haptics = rememberMandiHaptics()
    Button(
        onClick = {
            haptics.click()
            onClick()
        },
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MandiPrimaryAction,
            contentColor = MandiPrimaryActionText
        ),
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(height)
            .mandiPressEffect(enabled = enabled && !isLoading)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = MandiPrimaryActionText,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = text,
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Secondary Elevated Button.
 */
@Composable
fun MandiSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    fullWidth: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 48.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp
) {
    val haptics = rememberMandiHaptics()
    Button(
        onClick = {
            haptics.click()
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MandiSecondaryAction,
            contentColor = MandiSecondaryActionText
        ),
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(height)
            .mandiPressEffect(enabled = enabled)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Success Positive Button (e.g., Receive Cash, Approve).
 * Uses on-colour pair: MandiBtnSuccessBg with MandiBtnSuccessFg (≥ 4.5:1).
 */
@Composable
fun MandiSuccessButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    fullWidth: Boolean = true
) {
    val haptics = rememberMandiHaptics()
    Button(
        onClick = {
            haptics.success()
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MandiBtnSuccessBg,
            contentColor = MandiBtnSuccessFg
        ),
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(48.dp)
            .mandiPressEffect(enabled = enabled)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Destructive Danger Button (e.g., Void, Delete, Cancel).
 * Uses on-colour pair: MandiBtnDangerBg with MandiBtnDangerFg (≥ 4.5:1).
 */
@Composable
fun MandiDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    fullWidth: Boolean = true
) {
    val haptics = rememberMandiHaptics()
    Button(
        onClick = {
            haptics.longPress()
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MandiBtnDangerBg,
            contentColor = MandiBtnDangerFg
        ),
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(48.dp)
            .mandiPressEffect(enabled = enabled)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Outlined Neutral Button.
 */
@Composable
fun MandiOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    fullWidth: Boolean = true
) {
    val haptics = rememberMandiHaptics()
    OutlinedButton(
        onClick = {
            haptics.click()
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MandiBorder),
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(48.dp)
            .mandiPressEffect(enabled = enabled)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MandiTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MandiTextPrimary
            )
        }
    }
}

