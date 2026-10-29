package com.appwork.mandisamiti.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

/**
 * NGDL v1.2 compliant dialog to guard against duplicate entry saves within 2 minutes.
 */
@Composable
fun DuplicateEntryDialog(
    message: String = "यह एंट्री पहले से है, फिर भी सुरक्षित करें?",
    details: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "संभावित डुप्लीकेट एंट्री",
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = message,
                    color = MandiTextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
                if (!details.isNullOrBlank()) {
                    Text(
                        text = details,
                        color = MandiTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("हाँ, सुरक्षित करें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}
