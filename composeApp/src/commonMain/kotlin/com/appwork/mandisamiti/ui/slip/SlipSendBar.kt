package com.appwork.mandisamiti.ui.slip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.platform.WhatsAppApp
import com.appwork.mandisamiti.ui.components.MandiSecondaryButton
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessBg
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessFg
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

private data class PendingSend(val phone: String?, val apps: List<WhatsAppApp>)

/**
 * Slip sharing controls. With a number: send to the party, then "Other contact".
 * Without one: a single button that opens WhatsApp's contact picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlipSendBar(
    recipientName: String,
    recipientPhone: String?,
    isEnglish: Boolean,
    installedApps: () -> List<WhatsAppApp>,
    onSend: (phone: String?, app: WhatsAppApp?) -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true
) {
    var pending by remember { mutableStateOf<PendingSend?>(null) }

    // Asked on each tap, so an app installed or removed meanwhile is picked up.
    fun send(phone: String?) {
        val apps = installedApps()
        if (apps.size > 1) pending = PendingSend(phone, apps) else onSend(phone, apps.firstOrNull())
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (recipientPhone == null) {
            Text(
                text = if (isEnglish) "No valid mobile number for $recipientName"
                else "$recipientName का सही मोबाइल नंबर नहीं है",
                fontSize = 12.sp,
                color = MandiTextSecondary,
                textAlign = TextAlign.Center
            )
        }
        Button(
            onClick = { send(recipientPhone) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (primary) MandiBtnSuccessBg else MandiSurface,
                contentColor = if (primary) MandiBtnSuccessFg else MandiTextPrimary
            ),
            border = if (primary) null else BorderStroke(1.dp, MandiBorder)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when {
                    recipientPhone == null -> if (isEnglish) "Choose contact to send" else "कॉन्टैक्ट चुनकर भेजें"
                    isEnglish -> "Send to $recipientName"
                    else -> "$recipientName को भेजें"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (recipientPhone != null) {
            Text(
                text = "${recipientPhone.take(5)} ${recipientPhone.drop(5)}",
                fontSize = 12.sp,
                color = MandiTextSecondary
            )
            TextButton(onClick = { send(null) }) {
                Text(
                    text = if (isEnglish) "Other contact" else "दूसरा कॉन्टैक्ट",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                )
            }
        }
    }

    pending?.let { request ->
        ModalBottomSheet(onDismissRequest = { pending = null }, containerColor = MandiSurface) {
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isEnglish) "Send with which app?" else "किस ऐप से भेजें?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                )
                request.apps.forEach { app ->
                    MandiSecondaryButton(
                        text = app.label,
                        onClick = {
                            pending = null
                            onSend(request.phone, app)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
