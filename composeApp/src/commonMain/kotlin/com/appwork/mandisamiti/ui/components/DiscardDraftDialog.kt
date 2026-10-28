package com.appwork.mandisamiti.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.ui.theme.MandiBtnDangerBg
import com.appwork.mandisamiti.ui.theme.MandiBtnDangerFg
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

/**
 * NGDL v1.2 compliant confirmation dialog before discarding unsaved form input.
 */
@Composable
fun DiscardDraftDialog(
    title: String = "बदलाव रद्द करें?",
    message: String = "क्या आप बिना सुरक्षित किए बाहर जाना चाहते हैं? भरी गई जानकारी मिट जाएगी।",
    confirmText: String = "हाँ, बाहर निकलें",
    dismissText: String = "भरना जारी रखें",
    onConfirmDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = message,
                color = MandiTextSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDiscard,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MandiBtnDangerBg,
                    contentColor = MandiBtnDangerFg
                ),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(confirmText, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(dismissText, color = MandiTextPrimary, fontWeight = FontWeight.Medium)
            }
        }
    )
}
