package com.appwork.mandisamiti.ui.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.appwork.mandisamiti.data.repository.ShopQuota
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiAmberLight
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiBorderActive
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessBg
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiRedPayable
import com.appwork.mandisamiti.ui.theme.MandiSecondaryAction
import com.appwork.mandisamiti.ui.theme.MandiSecondaryActionText
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

/**
 * Emergency +10 Bills Bottom Sheet.
 * Triggered when a merchant attempts to save a bill beyond their monthly limit (e.g. 50/50),
 * allowing them to claim up to 2 lifetime emergency extensions of 10 bills each.
 * Strict NGDL compliance: elevation tokens, vector icons only, no unicode emoji.
 */
@Composable
fun EmergencyExtensionSheet(
    quota: ShopQuota,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onClaimExtension: () -> Unit,
    onViewUpgradePlans: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val remainingExtensions = (2L - quota.emergencyExtensionsUsed).coerceAtLeast(0L)
    val totalLimit = quota.effectiveLimit
    val billsUsed = quota.billsCreatedThisMonth

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Scrim overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onDismissRequest),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Sheet container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MandiSurface)
                    .border(
                        width = 1.dp,
                        color = MandiBorder,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .clickable(enabled = false) {} // Consume clicks to prevent dismiss
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MandiTextMuted.copy(alpha = 0.4f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Header Row with Title and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MandiAmberLight)
                                .border(1.dp, MandiAmberDark.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MandiAmberDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Monthly Quota Reached ($billsUsed/$totalLimit)",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiTextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "मासिक बिल सीमा समाप्त",
                                fontSize = 12.sp,
                                color = MandiTextSecondary
                            )
                        }
                    }

                    // High contrast close button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MandiSurfaceElevated)
                            .border(1.dp, MandiBorder, CircleShape)
                            .clickable(role = Role.Button, onClick = onDismissRequest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MandiTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MandiBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Emergency Extension Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MandiSurfaceElevated)
                        .border(1.dp, MandiBorderActive, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Emergency Extension: $remainingExtensions of 2 Left",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (remainingExtensions > 0) MandiAmberDark else MandiRedPayable
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Subtitle description
                Text(
                    text = "Claim an emergency extension to save this bill without interruption.",
                    fontSize = 14.sp,
                    color = MandiTextSecondary,
                    lineHeight = 20.sp
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 13.sp,
                        color = MandiRedPayable
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary CTA: Unlock +10 Emergency Bills
                Button(
                    onClick = onClaimExtension,
                    enabled = !isLoading && remainingExtensions > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MandiBtnSuccessBg,
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Unlock +10 Emergency Bills",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secondary CTA: View Upgrade Plans
                OutlinedButton(
                    onClick = onViewUpgradePlans,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MandiSecondaryAction,
                        contentColor = MandiSecondaryActionText
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MandiBorder)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "View Upgrade Plans",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MandiSecondaryActionText
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MandiSecondaryActionText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
