package com.appwork.mandisamiti.ui.slip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.Deal
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import com.appwork.mandisamiti.platform.WhatsAppShareManager
import com.appwork.mandisamiti.ui.components.SoundboxTopBar
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiGold
import com.appwork.mandisamiti.ui.theme.MandiGreenPayable
import com.appwork.mandisamiti.ui.theme.MandiNavy
import com.appwork.mandisamiti.ui.theme.MandiRedReceivable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

@Composable
fun ReceiptPreviewScreen(
    shopProfile: ShopProfile,
    farmer: Party,
    buyer: Party?,
    deal: Deal,
    whatsAppShareManager: WhatsAppShareManager,
    ttsManager: SoundboxTtsManager,
    onNavigateBack: () -> Unit
) {
    val formattedSlipText = DigitalSlipRenderer.formatWhatsAppReceiptText(
        shopProfile = shopProfile,
        farmer = farmer,
        buyer = buyer,
        deal = deal
    )

    val voiceAnnouncementText = "${farmer.name}, ${deal.bagsCount} बोरी, ₹${MandiMathEngine.paisaToRupeesString(deal.netFarmerPayablePaisa)} पक्के हिसाब में दर्ज हुए।"

    Scaffold(
        topBar = {
            SoundboxTopBar(
                shopName = "📜 पक्का पर्चा रसीद",
                mandiLocation = "मथुरा मंडी • डिजिटल पर्ची",
                isSoundEnabled = shopProfile.isSoundEnabled,
                onToggleSound = {}
            )
        },
        containerColor = MandiBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Traditional Branded Slip Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MandiSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Shop Header
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "🌾 ${shopProfile.shopName} 🌾",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiNavy
                        )
                        Text(
                            "${shopProfile.mandiName} • दूकान नं. ${shopProfile.shopNumber ?: "-"}",
                            fontSize = 13.sp,
                            color = MandiTextSecondary
                        )
                        Text(
                            "मो.: ${shopProfile.phoneNumber}",
                            fontSize = 12.sp,
                            color = MandiTextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MandiBorder))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Party Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("👨‍🌾 किसान:", fontSize = 12.sp, color = MandiTextMuted)
                            Text(farmer.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MandiNavy)
                            if (farmer.village != null) {
                                Text("गांव: ${farmer.village}", fontSize = 12.sp, color = MandiTextSecondary)
                            }
                        }
                        if (buyer != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("🏭 खरीदार:", fontSize = 12.sp, color = MandiTextMuted)
                                Text(buyer.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MandiNavy)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Weight Breakdown Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("बोरी (Bags)", fontSize = 12.sp, color = MandiTextMuted)
                            Text("${deal.bagsCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MandiNavy)
                        }
                        Column {
                            Text("कुल वजन (Gross)", fontSize = 12.sp, color = MandiTextMuted)
                            Text("${MandiMathEngine.gramsToQuintals(deal.grossWeightGrams)} Q", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MandiNavy)
                        }
                        Column {
                            Text("कटौती (Tare)", fontSize = 12.sp, color = MandiTextMuted)
                            Text("${MandiMathEngine.gramsToQuintals(deal.cutWeightGrams)} Q", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MandiRedReceivable)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("✨ शुद्ध वजन (Net Weight):", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MandiNavy)
                        Text("${MandiMathEngine.gramsToQuintals(deal.netWeightGrams)} कुंतल", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MandiGold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MandiBorder))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Settlement Math
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("💵 कुल रकम (Gross Amount):", fontSize = 14.sp, color = MandiTextSecondary)
                        Text("₹ ${MandiMathEngine.paisaToRupeesString(deal.grossAmountPaisa)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MandiNavy)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("➖ पल्लेदारी / मजदूरी:", fontSize = 13.sp, color = MandiTextMuted)
                        Text("₹ ${MandiMathEngine.paisaToRupeesString(deal.labourChargePaisa)}", fontSize = 13.sp, color = MandiTextSecondary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("➖ मंडी आढ़त:", fontSize = 13.sp, color = MandiTextMuted)
                        Text("₹ ${MandiMathEngine.paisaToRupeesString(deal.farmerCommissionPaisa)}", fontSize = 13.sp, color = MandiTextSecondary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Net Farmer Payable Highlight Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MandiGreenPayable.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🟢 शुद्ध देय रकम:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MandiGreenPayable)
                            Text(
                                "₹ ${MandiMathEngine.paisaToRupeesString(deal.netFarmerPayablePaisa)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = MandiGreenPayable
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("हस्ताक्षर: ${shopProfile.ownerName} (लाला जी)", fontSize = 12.sp, color = MandiTextMuted)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons (WhatsApp Share & Voice Replay)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        whatsAppShareManager.shareText(
                            text = formattedSlipText,
                            phoneNumber = farmer.phone
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MandiGreenPayable)
                ) {
                    Text(
                        "💬 WhatsApp पर पर्ची भेजें (Share)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = {
                        ttsManager.speak(voiceAnnouncementText, true)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MandiNavy)
                ) {
                    Text(
                        "🗣️ दोबारा सुनें [ 🔊 साउंडबॉक्स ]",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
