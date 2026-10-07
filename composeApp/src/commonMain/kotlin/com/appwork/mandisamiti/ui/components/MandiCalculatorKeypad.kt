package com.appwork.mandisamiti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.ui.theme.MandiAmber50
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiAmberLight
import com.appwork.mandisamiti.ui.theme.MandiAmberPrimary
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenPayable
import com.appwork.mandisamiti.ui.theme.MandiRedLight
import com.appwork.mandisamiti.ui.theme.MandiRedReceivable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary

enum class KeypadAction {
    DIGIT_0, DIGIT_1, DIGIT_2, DIGIT_3, DIGIT_4,
    DIGIT_5, DIGIT_6, DIGIT_7, DIGIT_8, DIGIT_9,
    DOUBLE_ZERO, DECIMAL, CLEAR, BACKSPACE, NEXT, SUBMIT
}

@Composable
fun MandiCalculatorKeypad(
    modifier: Modifier = Modifier,
    onKeyPressed: (KeypadAction) -> Unit,
    showSubmitInsteadOfNext: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MandiAmber50.copy(alpha = 0.6f))
            .border(width = 1.dp, color = MandiBorder)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: 1, 2, 3, Clear (C)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(text = "1", modifier = Modifier.weight(1f)) { onKeyPressed(KeypadAction.DIGIT_1) }
            KeypadButton(text = "2", modifier = Modifier.weight(1f)) { onKeyPressed(KeypadAction.DIGIT_2) }
            KeypadButton(text = "3", modifier = Modifier.weight(1f)) { onKeyPressed(KeypadAction.DIGIT_3) }
            KeypadButton(
                text = "C (साफ़)",
                backgroundColor = MandiRedLight,
                textColor = MandiRedReceivable,
                modifier = Modifier.weight(1.2f)
            ) { onKeyPressed(KeypadAction.CLEAR) }
        }

        // Row 2: 4, 5, 6, Backspace (⌫)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(text = "4", modifier = Modifier.weight(1f)) { onKeyPressed(KeypadAction.DIGIT_4) }
            KeypadButton(text = "5", modifier = Modifier.weight(1f)) { onKeyPressed(KeypadAction.DIGIT_5) }
            KeypadButton(text = "6", modifier = Modifier.weight(1f)) { onKeyPressed(KeypadAction.DIGIT_6) }
            KeypadButton(
                text = "काटें",
                backgroundColor = MandiBorder,
                textColor = MandiTextPrimary,
                modifier = Modifier.weight(1.2f)
            ) { onKeyPressed(KeypadAction.BACKSPACE) }
        }

        // Row 3: 7, 8, 9, Next / Submit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(text = "7", modifier = Modifier.weight(1f)) { onKeyPressed(KeypadAction.DIGIT_7) }
            KeypadButton(text = "8", modifier = Modifier.weight(1f)) { onKeyPressed(KeypadAction.DIGIT_8) }
            KeypadButton(text = "9", modifier = Modifier.weight(1f)) { onKeyPressed(KeypadAction.DIGIT_9) }
            KeypadButton(
                text = if (showSubmitInsteadOfNext) "पूर्ण" else "अगला",
                icon = if (showSubmitInsteadOfNext) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                backgroundColor = if (showSubmitInsteadOfNext) MandiGreenPayable else MandiAmberPrimary,
                textColor = Color.White,
                modifier = Modifier.weight(1.2f)
            ) {
                onKeyPressed(if (showSubmitInsteadOfNext) KeypadAction.SUBMIT else KeypadAction.NEXT)
            }
        }

        // Row 4: 00, 0, .
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(text = "00", modifier = Modifier.weight(1f)) { onKeyPressed(KeypadAction.DOUBLE_ZERO) }
            KeypadButton(text = "0", modifier = Modifier.weight(1f)) { onKeyPressed(KeypadAction.DIGIT_0) }
            KeypadButton(text = ".", modifier = Modifier.weight(1f)) { onKeyPressed(KeypadAction.DECIMAL) }
            Box(modifier = Modifier.weight(1.2f)) // Spacer aligned with action column
        }
    }
}

@Composable
private fun KeypadButton(
    text: String = "",
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MandiSurface,
    textColor: Color = MandiTextPrimary,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .height(58.dp) // Minimum 56dp+ touch target
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, MandiBorder, shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (text.isNotEmpty()) {
                Text(
                    text = text,
                    color = textColor,
                    fontSize = if (icon != null) 15.sp else 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
