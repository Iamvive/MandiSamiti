package com.appwork.mandisamiti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiNavy
import com.appwork.mandisamiti.ui.theme.MandiRedReceivable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

@Composable
fun AddPartyDialog(
    initialPartyType: PartyType = PartyType.FARMER,
    onDismiss: () -> Unit,
    onSaveParty: (name: String, phone: String?, village: String?, partyType: PartyType, interestRate: Double?) -> Unit
) {
    var partyType by remember { mutableStateOf(initialPartyType) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var interestRateText by remember { mutableStateOf(if (initialPartyType == PartyType.FARMER) "1.5" else "0.0") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MandiBorder, RoundedCornerShape(16.dp)),
            color = MandiSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "नया खाता जोड़ें",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiNavy
                        )
                        Text(
                            text = "किसान या व्यापारी का विवरण दर्ज करें",
                            fontSize = 12.sp,
                            color = MandiTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "बंद करें",
                            tint = MandiTextSecondary
                        )
                    }
                }

                // Party Type Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MandiBackground)
                        .border(1.dp, MandiBorder, RoundedCornerShape(8.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PartyTypeOption(
                        label = "किसान (विक्रेता)",
                        isSelected = partyType == PartyType.FARMER,
                        onClick = {
                            partyType = PartyType.FARMER
                            if (interestRateText == "0.0") interestRateText = "1.5"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PartyTypeOption(
                        label = "व्यापारी (खरीदार)",
                        isSelected = partyType == PartyType.BUYER,
                        onClick = {
                            partyType = PartyType.BUYER
                            if (interestRateText == "1.5") interestRateText = "0.0"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("पूरा नाम *") },
                    placeholder = { Text("उदा. रामवीर सिंह") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MandiNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MandiNavy,
                        unfocusedBorderColor = MandiBorder
                    )
                )

                // Phone Input
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("मोबाइल नंबर (वैकल्पिक)") },
                    placeholder = { Text("10 अंकों का फोन नंबर") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MandiNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MandiNavy,
                        unfocusedBorderColor = MandiBorder
                    )
                )

                // Village Input
                OutlinedTextField(
                    value = village,
                    onValueChange = { village = it },
                    label = { Text("गाँव / मंडी (वैकल्पिक)") },
                    placeholder = { Text("उदा. राया, मथुरा") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MandiNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MandiNavy,
                        unfocusedBorderColor = MandiBorder
                    )
                )

                // Interest Rate Input (Only relevant for Farmers or optional)
                OutlinedTextField(
                    value = interestRateText,
                    onValueChange = { interestRateText = it },
                    label = { Text("मासिक ब्याज % (प्रति माह)") },
                    placeholder = { Text("उदा. 1.5") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Percent,
                            contentDescription = null,
                            tint = MandiNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MandiNavy,
                        unfocusedBorderColor = MandiBorder
                    )
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MandiRedReceivable,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MandiBorder)
                    ) {
                        Text(
                            text = "रद्द करें",
                            color = MandiTextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }

                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            if (trimmedName.isEmpty()) {
                                errorMessage = "कृपया नाम दर्ज करें"
                                return@Button
                            }
                            val interestRate = interestRateText.toDoubleOrNull() ?: 0.0
                            onSaveParty(
                                trimmedName,
                                phone.trim().takeIf { it.isNotEmpty() },
                                village.trim().takeIf { it.isNotEmpty() },
                                partyType,
                                interestRate
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MandiNavy)
                    ) {
                        Text(
                            text = "खाता बनाएं",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartyTypeOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isSelected) MandiNavy else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MandiTextSecondary
        )
    }
}
