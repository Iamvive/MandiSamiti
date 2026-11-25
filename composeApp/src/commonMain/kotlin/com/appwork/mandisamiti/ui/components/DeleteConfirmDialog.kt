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
 * NGDL v1.2 compliant two-button confirmation dialog before voiding/deleting an entry.
 */
@Composable
fun DeleteConfirmDialog(
    isEnglish: Boolean = false,
    title: String = if (isEnglish) "Delete entry?" else "प्रविष्टि हटाएं?",
    message: String = if (isEnglish) {
        "Are you sure you want to delete this entry? It will be removed from totals and lists."
    } else {
        "क्या आप वाकई इस प्रविष्टि को हटाना चाहते हैं? यह सूची और कुल हिसाब से हट जाएगी।"
    },
    confirmText: String = if (isEnglish) "Delete" else "हटाएं",
    dismissText: String = if (isEnglish) "Cancel" else "रद्द करें",
    onConfirm: () -> Unit,
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
                onClick = onConfirm,
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
