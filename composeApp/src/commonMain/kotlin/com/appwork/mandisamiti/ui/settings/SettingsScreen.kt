package com.appwork.mandisamiti.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.ui.home.HomeUiState
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenPayable
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiRedReceivable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.OutlinedTextField
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.TradeSettings

@Composable
fun SettingsScreen(
    uiState: HomeUiState,
    isEnglish: Boolean,
    onLanguageToggle: (Boolean) -> Unit,
    onToggleSound: () -> Unit,
    onSignOutClick: () -> Unit,
    onUpdateTradeSettings: (TradeSettings) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSignOutConfirmDialog by remember { mutableStateOf(false) }
    var showEditTradeSettingsDialog by remember { mutableStateOf(false) }
    var editFarmerCommInput by remember { mutableStateOf("") }
    var editBuyerCommInput by remember { mutableStateOf("") }
    var editLabourInput by remember { mutableStateOf("") }
    var editTareInput by remember { mutableStateOf("") }

    if (showSignOutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmDialog = false },
            title = {
                Text(
                    text = if (isEnglish) "Sign Out?" else "साइन आउट करें?",
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                )
            },
            text = {
                Text(
                    text = if (isEnglish) "Are you sure you want to lock the app and sign out of this shop?" else "क्या आप इस दुकान के खाते से बाहर निकलना चाहते हैं?",
                    color = MandiTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirmDialog = false
                        onSignOutClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MandiRedReceivable)
                ) {
                    Text(text = if (isEnglish) "Sign Out" else "साइन आउट", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmDialog = false }) {
                    Text(text = if (isEnglish) "Cancel" else "रद्द करें", color = MandiTextSecondary)
                }
            },
            containerColor = MandiSurface
        )
    }

    if (showEditTradeSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showEditTradeSettingsDialog = false },
            title = {
                Text(
                    text = if (isEnglish) "Trade & Commission Rates" else "व्यापार व आढ़त दरें",
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isEnglish) "Set standard default rates for new deals:" else "नए सौदों के लिए डिफ़ॉल्ट दरें तय करें:",
                        fontSize = 13.sp,
                        color = MandiTextSecondary
                    )
                    OutlinedTextField(
                        value = editFarmerCommInput,
                        onValueChange = { editFarmerCommInput = it },
                        label = { Text(if (isEnglish) "Farmer Commission (%)" else "किसान आढ़त (%)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBuyerCommInput,
                        onValueChange = { editBuyerCommInput = it },
                        label = { Text(if (isEnglish) "Buyer Commission (%)" else "व्यापारी आढ़त (%)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editLabourInput,
                        onValueChange = { editLabourInput = it },
                        label = { Text(if (isEnglish) "Labour Charge (₹)" else "हम्माली / पल्लेदारी (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTareInput,
                        onValueChange = { editTareInput = it },
                        label = { Text(if (isEnglish) "Default Tare Weight (Qtl)" else "डिफ़ॉल्ट काट / बारदाना (कुंतल)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val farmerBps = MandiMathEngine.parsePercentToBasisPoints(editFarmerCommInput).takeIf { it > 0L } ?: uiState.tradeSettings.farmerCommissionBps
                        val buyerBps = MandiMathEngine.parsePercentToBasisPoints(editBuyerCommInput).takeIf { it > 0L } ?: uiState.tradeSettings.buyerCommissionBps
                        val labourPaisa = MandiMathEngine.parseRupeesToPaisa(editLabourInput).takeIf { it > 0L } ?: uiState.tradeSettings.defaultLabourPaisa
                        val tareGrams = MandiMathEngine.parseQuintalsStringToGrams(editTareInput).takeIf { it > 0L } ?: uiState.tradeSettings.defaultTareGrams

                        onUpdateTradeSettings(
                            TradeSettings(
                                farmerCommissionBps = farmerBps,
                                buyerCommissionBps = buyerBps,
                                defaultLabourPaisa = labourPaisa,
                                defaultTareGrams = tareGrams
                            )
                        )
                        showEditTradeSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MandiPrimaryAction)
                ) {
                    Text(text = if (isEnglish) "Save" else "सहेजें", color = MandiPrimaryActionText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTradeSettingsDialog = false }) {
                    Text(text = if (isEnglish) "Cancel" else "रद्द करें", color = MandiTextSecondary)
                }
            },
            containerColor = MandiSurface
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MandiBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Shop Profile Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MandiSurface)
                    .border(1.dp, MandiBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MandiSurfaceElevated)
                            .border(1.dp, MandiBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = MandiTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = uiState.shopProfile?.shopName ?: "",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                        Text(
                            text = uiState.shopProfile?.mandiName ?: "",
                            fontSize = 13.sp,
                            color = MandiTextSecondary
                        )
                        Text(
                            text = "फ़ोन: ${uiState.shopProfile?.phoneNumber ?: ""}",
                            fontSize = 12.sp,
                            color = MandiTextMuted
                        )
                    }
                }
            }
        }

        // 2. Language Switcher Card (हिन्दी / English)
        item {
            SettingsGroupCard(title = if (isEnglish) "Language / भाषा" else "भाषा / Language") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LanguageOptionPill(
                        label = "हिन्दी",
                        isSelected = !isEnglish,
                        onClick = { onLanguageToggle(false) },
                        modifier = Modifier.weight(1f)
                    )
                    LanguageOptionPill(
                        label = "English",
                        isSelected = isEnglish,
                        onClick = { onLanguageToggle(true) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Trade & Commission Rates Card (व्यापार व आढ़त दरें)
        item {
            SettingsGroupCard(title = if (isEnglish) "Trade & Commission Rates" else "व्यापार व आढ़त दरें") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnglish) "Farmer Commission:" else "किसान आढ़त दर:",
                            fontSize = 14.sp,
                            color = MandiTextSecondary
                        )
                        Text(
                            text = "${MandiMathEngine.paisaToInputString(uiState.tradeSettings.farmerCommissionBps)}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnglish) "Buyer Commission:" else "व्यापारी आढ़त दर:",
                            fontSize = 14.sp,
                            color = MandiTextSecondary
                        )
                        Text(
                            text = "${MandiMathEngine.paisaToInputString(uiState.tradeSettings.buyerCommissionBps)}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnglish) "Labour Charge:" else "हम्माली / पल्लेदारी:",
                            fontSize = 14.sp,
                            color = MandiTextSecondary
                        )
                        Text(
                            text = "₹${MandiMathEngine.paisaToRupeesString(uiState.tradeSettings.defaultLabourPaisa)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnglish) "Default Tare Weight:" else "डिफ़ॉल्ट काट / बारदाना:",
                            fontSize = 14.sp,
                            color = MandiTextSecondary
                        )
                        Text(
                            text = "${MandiMathEngine.gramsToQuintalsInputString(uiState.tradeSettings.defaultTareGrams)} Qtl",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MandiSurfaceElevated)
                            .border(1.dp, MandiBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                editFarmerCommInput = MandiMathEngine.paisaToInputString(uiState.tradeSettings.farmerCommissionBps)
                                editBuyerCommInput = MandiMathEngine.paisaToInputString(uiState.tradeSettings.buyerCommissionBps)
                                editLabourInput = MandiMathEngine.paisaToRupeesString(uiState.tradeSettings.defaultLabourPaisa)
                                editTareInput = MandiMathEngine.gramsToQuintalsInputString(uiState.tradeSettings.defaultTareGrams)
                                showEditTradeSettingsDialog = true
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEnglish) "Edit Default Rates" else "दरें बदलें",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiPrimaryAction
                        )
                    }
                }
            }
        }

        // 4. Vernacular Voice Soundbox Card
        item {
            SettingsGroupCard(title = if (isEnglish) "Sound & Voice Assistant" else "ध्वनि एवं वॉइस साउंडबॉक्स") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MandiTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (isEnglish) "Voice Confirmation" else "सौदा वॉइस साउंडबॉक्स",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MandiTextPrimary
                            )
                            Text(
                                text = if (isEnglish) "Speaks out transaction amounts" else "लेन-देन दर्ज होते ही बोलकर पुष्टि करे",
                                fontSize = 12.sp,
                                color = MandiTextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = uiState.isSoundEnabled,
                        onCheckedChange = { onToggleSound() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MandiGreenPayable
                        )
                    )
                }
            }
        }

        // 4. Data & Sync Info
        item {
            SettingsGroupCard(title = if (isEnglish) "Data Storage & Backup" else "डाटा सुरक्षा एवं बैकअप") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = MandiGreenPayable,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (isEnglish) "Local Offline Storage" else "लोकल ऑफलाइन बहीखाता",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MandiTextPrimary
                            )
                            Text(
                                text = if (isEnglish) "SQLite encrypted database active" else "सुरक्षित एवं ऑफलाइन डेटाबेस सक्रिय",
                                fontSize = 12.sp,
                                color = MandiTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 5. Sign Out / Exit Session Button
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MandiSurface)
                    .border(1.dp, MandiRedReceivable.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .clickable { showSignOutConfirmDialog = true }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MandiRedReceivable,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isEnglish) "Sign Out / Switch Shop" else "साइन आउट करें (दुकान बदलें)",
                        color = MandiRedReceivable,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MandiSurface)
            .border(1.dp, MandiBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MandiTextSecondary
            )
            content()
        }
    }
}

@Composable
private fun LanguageOptionPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isSelected) MandiPrimaryAction else MandiSurfaceElevated)
            .border(1.dp, if (isSelected) MandiPrimaryAction else MandiBorder, shape)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MandiPrimaryActionText else MandiTextPrimary
        )
    }
}
