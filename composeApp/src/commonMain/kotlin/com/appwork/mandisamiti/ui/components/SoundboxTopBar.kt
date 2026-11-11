package com.appwork.mandisamiti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenText
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

@Composable
fun SoundboxTopBar(
    shopName: String,
    mandiLocation: String,
    isSoundEnabled: Boolean,
    onToggleSound: () -> Unit,
    modifier: Modifier = Modifier,
    isEnglish: Boolean = false,
    onOpenHelp: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val monogramInitial = remember(shopName) {
        val trimmed = shopName.trim()
        if (trimmed.isEmpty()) {
            "मं"
        } else {
            val chars = trimmed.toCharArray()
            val buf = StringBuilder()
            var i = 0
            while (i < chars.size) {
                buf.append(chars[i])
                i++
                // Handle Devanagari virama / halant (0x094D) to keep conjuncts (e.g. क्ष for क्षितिज, श्र for श्री)
                if (i < chars.size && chars[i] == '\u094D') {
                    buf.append(chars[i])
                    i++
                    if (i < chars.size) {
                        buf.append(chars[i])
                        i++
                    }
                    continue
                }
                // Include nukta if present
                if (i < chars.size && chars[i] == '\u093C') {
                    buf.append(chars[i])
                    i++
                    continue
                }
                break
            }
            buf.toString()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MandiSurface)
            .border(1.dp, MandiBorder)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (navigationIcon != null) {
                navigationIcon()
            } else {
                // Minimal Monogram Avatar
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MandiSurfaceElevated)
                        .border(1.dp, MandiBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = monogramInitial,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shopName,
                    color = MandiTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = mandiLocation,
                    color = MandiTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (actions != null) {
                actions()
            }

            IconButton(
                onClick = onToggleSound,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Icon(
                    imageVector = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = if (isEnglish) {
                        if (isSoundEnabled) "Turn sound off" else "Turn sound on"
                    } else {
                        if (isSoundEnabled) "आवाज़ बंद करें" else "आवाज़ चालू करें"
                    },
                    tint = if (isSoundEnabled) MandiGreenText else MandiTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (onOpenHelp != null || onOpenSettings != null) {
                Box {
                    IconButton(
                        onClick = { isMenuExpanded = true },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = if (isEnglish) "More options" else "अन्य विकल्प",
                            tint = MandiTextPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        if (onOpenHelp != null) {
                            DropdownMenuItem(
                                text = { Text(if (isEnglish) "Help & guides" else "सहायता और गाइड") },
                                onClick = {
                                    isMenuExpanded = false
                                    onOpenHelp()
                                }
                            )
                        }
                        if (onOpenSettings != null) {
                            DropdownMenuItem(
                                text = { Text(if (isEnglish) "Settings" else "सेटिंग्स") },
                                onClick = {
                                    isMenuExpanded = false
                                    onOpenSettings()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

