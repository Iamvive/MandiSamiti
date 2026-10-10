package com.appwork.mandisamiti.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.appwork.mandisamiti.ui.theme.MandiAccent
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiBorderActive
import com.appwork.mandisamiti.ui.theme.MandiGreenBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenLight
import com.appwork.mandisamiti.ui.theme.MandiGreenText
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

/**
 * High-touch NGDL v1.2 In-App Video Tutorial Bottom Sheet.
 * Allows Indian Aadhatis (Age 25–65) to watch bite-sized 30–45s YouTube demos
 * or read offline step-by-step guides without leaving their active workflow.
 */
@Composable
fun MandiTutorialSheet(
    initialTutorial: MandiTutorial = MandiTutorial.DEAL_ENTRY,
    isEnglish: Boolean = false,
    onDismissRequest: () -> Unit
) {
    var selectedTutorial by remember { mutableStateOf(initialTutorial) }
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()

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
                    .fillMaxHeight(0.88f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MandiSurface)
                    .border(
                        width = 1.dp,
                        color = MandiBorder,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .clickable(enabled = false) {} // Consume clicks to prevent dismissing
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MandiTextMuted.copy(alpha = 0.4f))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Header Row with Title and 48dp Close Target
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MandiGreenLight)
                                .border(1.dp, MandiGreenBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = MandiGreenText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = selectedTutorial.title(isEnglish),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiTextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "वीडियो गाइड • ${selectedTutorial.duration(isEnglish)}",
                                fontSize = 12.sp,
                                color = MandiTextSecondary
                            )
                        }
                    }

                    // Explicit high-contrast Close Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MandiSurfaceElevated)
                            .border(1.dp, MandiBorder, CircleShape)
                            .clickable(role = Role.Button, onClick = onDismissRequest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isEnglish) "Close Guide" else "गाइड बंद करें",
                            tint = MandiTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MandiBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Video Card Container
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MandiBorderActive, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MandiSurfaceElevated)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = selectedTutorial.description(isEnglish),
                                fontSize = 13.sp,
                                color = MandiTextSecondary,
                                lineHeight = 18.sp
                            )

                            // High-Impact Video Play Trigger
                            Button(
                                onClick = {
                                    uriHandler.openUri(selectedTutorial.videoUrl())
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MandiPrimaryAction,
                                    contentColor = MandiPrimaryActionText
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = MandiPrimaryActionText,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = if (isEnglish) "Play Video Demo (${selectedTutorial.duration(true)})" else "वीडियो डेमो चलाएं (${selectedTutorial.duration(false)})",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        tint = MandiPrimaryActionText.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Offline Fallback Step-by-Step Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MandiBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MandiSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "3-Step Guide (Works Without Internet):" else "आसान 3 स्टेप्स (बिना इंटरनेट के):",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiTextPrimary
                            )

                            val steps = selectedTutorial.fallbackSteps(isEnglish)
                            steps.forEachIndexed { index, stepText ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(MandiSurfaceElevated)
                                            .border(1.dp, MandiBorder, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MandiGreenText
                                        )
                                    }
                                    Text(
                                        text = stepText,
                                        fontSize = 13.sp,
                                        color = MandiTextPrimary,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Other Tutorials Tray
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = MandiTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isEnglish) "Other Video Guides:" else "अन्य वीडियो सहायता:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MandiTextSecondary
                            )
                        }

                        MandiTutorial.ALL_TUTORIALS.forEach { tutorial ->
                            val isSelected = tutorial.id == selectedTutorial.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MandiGreenLight else MandiSurfaceElevated)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MandiGreenBorder else MandiBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedTutorial = tutorial }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = if (isSelected) MandiGreenText else MandiTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = tutorial.title(isEnglish),
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MandiGreenText else MandiTextPrimary,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = tutorial.duration(isEnglish),
                                    fontSize = 12.sp,
                                    color = if (isSelected) MandiGreenText else MandiTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
