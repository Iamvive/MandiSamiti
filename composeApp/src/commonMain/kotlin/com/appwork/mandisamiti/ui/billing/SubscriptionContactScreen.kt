package com.appwork.mandisamiti.ui.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.data.repository.ShopQuota
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiAmberLight
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiBorderActive
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessBg
import com.appwork.mandisamiti.ui.theme.MandiGreenText
import com.appwork.mandisamiti.ui.theme.MandiRedPayable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

const val MANDI_SUPPORT_PHONE = "+919368299074"
const val MANDI_SUPPORT_WHATSAPP = "919368299074"
const val MANDI_SUPPORT_EMAIL = "appworkd@gmail.com"
const val DEFAULT_WHATSAPP_UPGRADE_MESSAGE = "Hello MandiSamiti Support, I want to upgrade my shop account."
const val DEFAULT_WHATSAPP_UPGRADE_MESSAGE_HI = "नमस्ते मंडीसमिति सहायता, मैं अपनी दुकान का खाता अपग्रेड कराना चाहता हूँ।"
const val REASSURANCE_FOOTER_TEXT = "All existing data, ledgers, and slips remain permanently saved and readable."
const val REASSURANCE_FOOTER_TEXT_HI = "सभी पुराना डेटा, बही-खाते और पर्चियां हमेशा सुरक्षित और पढ़ने के लिए उपलब्ध रहेंगी।"

data class SubscriptionState(
    val totalCreated: Long,
    val limit: Long,
    val planTier: String = "FREE",
    val shopName: String? = null
) {
    val isExhausted: Boolean get() = totalCreated >= limit
    val exhaustedMessage: String get() = "All $limit bills created for this month - Limit Reached"
    val activeMessage: String get() = "$totalCreated of $limit bills created this month"
}

sealed class BillingPlan(
    val id: String,
    val title: String,
    val titleHi: String,
    val description: String,
    val descriptionHi: String,
    val features: List<String>,
    val featuresHi: List<String>,
    val isHighlighted: Boolean = false
) {
    data object VyapariChhota : BillingPlan(
        id = "CHHOTA",
        title = "Vyapari Chhota Plan",
        titleHi = "व्यापारी छोटा प्लान",
        description = "Up to 150 bills/month, Rokad cashbook, 1 device",
        descriptionHi = "प्रति माह 150 बिल, रोकड़ बही-खाता, 1 डिवाइस",
        features = listOf(
            "Up to 150 bills/month",
            "Rokad cashbook",
            "1 device access",
            "WhatsApp slip share"
        ),
        featuresHi = listOf(
            "प्रति माह 150 बिल",
            "रोकड़ बही-खाता",
            "1 डिवाइस उपयोग",
            "व्हाट्सएप पर्ची शेयर"
        )
    )

    data object VyapariUnlimited : BillingPlan(
        id = "PRO",
        title = "Vyapari Unlimited Pro",
        titleHi = "व्यापारी अनलिमिटेड प्रो",
        description = "Unlimited bills, multi-phone sync, priority support",
        descriptionHi = "असीमित बिल, मल्टी-फ़ोन सिंक, प्राथमिकता सपोर्ट",
        features = listOf(
            "Unlimited bills",
            "Multi-phone sync",
            "Priority support",
            "Voice announcements"
        ),
        featuresHi = listOf(
            "असीमित बिल",
            "मल्टी-फ़ोन सिंक",
            "प्राथमिकता सपोर्ट",
            "वॉयस घोषणाएं"
        ),
        isHighlighted = true
    )
}

/**
 * Google Play Compliant Upgrade & Support Screen.
 * Displays merchant's current monthly usage, plan options, reassurance footer, and direct
 * Support actions (Phone call & WhatsApp chat).
 * Strictly compliant with Google Play billing rules: zero payment gateways, zero UPI QRs, zero bank details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionContactScreen(
    quota: ShopQuota?,
    shopName: String? = null,
    isEnglish: Boolean = false,
    onBack: () -> Unit,
    contactSupportHandler: ContactSupportHandler? = null,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    val totalCreated = quota?.billsCreatedThisMonth ?: 0L
    val limit = quota?.effectiveLimit ?: 50L
    val planTier = quota?.planTier ?: "FREE"
    val subState = SubscriptionState(totalCreated, limit, planTier, shopName)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isEnglish) "Plans & Support" else "प्लान और सहायता",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MandiTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEnglish) "Back" else "पीछे जाएं",
                            tint = MandiTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MandiSurface
                )
            )
        },
        bottomBar = {
            // Action Dock: Call Support & Chat on WhatsApp
            Surface(
                color = MandiSurface,
                border = BorderStroke(1.dp, MandiBorder),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (contactSupportHandler != null) {
                                contactSupportHandler.dialPhone(MANDI_SUPPORT_PHONE)
                            } else {
                                val uri = ContactSupportHandler.buildDialerUri(MANDI_SUPPORT_PHONE)
                                runCatching { uriHandler.openUri(uri) }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MandiBorderActive)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MandiTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnglish) "Call Support" else "कॉल सपोर्ट",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MandiTextPrimary
                        )
                    }

                    Button(
                        onClick = {
                            val msg = if (isEnglish) DEFAULT_WHATSAPP_UPGRADE_MESSAGE else DEFAULT_WHATSAPP_UPGRADE_MESSAGE_HI
                            if (contactSupportHandler != null) {
                                contactSupportHandler.openWhatsApp(MANDI_SUPPORT_WHATSAPP, msg)
                            } else {
                                val uri = ContactSupportHandler.buildWhatsAppUri(MANDI_SUPPORT_WHATSAPP, msg)
                                runCatching { uriHandler.openUri(uri) }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MandiBtnSuccessBg,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnglish) "Chat on WhatsApp" else "व्हाट्सएप चैट",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        },
        containerColor = MandiBackground,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header / Usage Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MandiSurfaceElevated),
                    border = BorderStroke(1.dp, if (subState.isExhausted) MandiAmberDark.copy(alpha = 0.5f) else MandiBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!subState.shopName.isNullOrBlank()) {
                            Text(
                                text = subState.shopName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiTextPrimary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (subState.isExhausted) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MandiAmberDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = if (subState.isExhausted) {
                                    if (isEnglish) subState.exhaustedMessage else "इस महीने के सभी $limit बिल बन चुके हैं - सीमा समाप्त"
                                } else {
                                    if (isEnglish) subState.activeMessage else "इस महीने $totalCreated / $limit बिल बने हैं"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (subState.isExhausted) MandiRedPayable else MandiTextPrimary
                            )
                        }

                        Text(
                            text = if (isEnglish) {
                                "Need to make more bills without interruption? Contact MandiSamiti support to upgrade your shop account."
                            } else {
                                "बिना किसी रुकावट के लगातार बिल बनाने के लिए मंडीसमिति सपोर्ट से संपर्क कर अपना खाता अपग्रेड करें।"
                            },
                            fontSize = 13.sp,
                            color = MandiTextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Plan Comparison Cards Header
            item {
                Text(
                    text = if (isEnglish) "Available Upgrade Plans" else "उपलब्ध प्लान",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MandiTextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Plan 1: Vyapari Chhota Plan
            item {
                PlanCard(
                    plan = BillingPlan.VyapariChhota,
                    isCurrentPlan = planTier == "CHHOTA",
                    isEnglish = isEnglish
                )
            }

            // Plan 2: Vyapari Unlimited Pro
            item {
                PlanCard(
                    plan = BillingPlan.VyapariUnlimited,
                    isCurrentPlan = planTier == "PRO",
                    isEnglish = isEnglish
                )
            }

            // Reassurance Footer: All existing data, ledgers, and slips remain permanently saved and readable
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MandiBtnSuccessBg.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, MandiBtnSuccessBg.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MandiBtnSuccessBg.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MandiGreenText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = if (isEnglish) REASSURANCE_FOOTER_TEXT else REASSURANCE_FOOTER_TEXT_HI,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MandiTextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Google Play Compliance Note & Support Info
            item {
                Text(
                    text = if (isEnglish) {
                        "Support email: $MANDI_SUPPORT_EMAIL\nAll plans are managed directly via MandiSamiti business advisory. We do not process direct consumer in-app purchases."
                    } else {
                        "सहायता ईमेल: $MANDI_SUPPORT_EMAIL\nसभी प्लान मंडीसमिति व्यापार सलाहकार द्वारा सक्रिय किए जाते हैं।"
                    },
                    fontSize = 12.sp,
                    color = MandiTextMuted,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: BillingPlan,
    isCurrentPlan: Boolean,
    isEnglish: Boolean
) {
    val borderColor = if (plan.isHighlighted) MandiBorderActive else MandiBorder
    val shape = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (plan.isHighlighted) MandiSurfaceElevated else MandiSurface
        ),
        border = BorderStroke(if (plan.isHighlighted) 1.5.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (plan.isHighlighted) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MandiAmberDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = if (isEnglish) plan.title else plan.titleHi,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MandiTextPrimary
                    )
                }

                if (isCurrentPlan) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MandiAmberLight)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Current" else "सक्रिय प्लान",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiAmberDark
                        )
                    }
                }
            }

            Text(
                text = if (isEnglish) plan.description else plan.descriptionHi,
                fontSize = 13.sp,
                color = MandiTextSecondary
            )

            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val features = if (isEnglish) plan.features else plan.featuresHi
                features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(MandiBtnSuccessBg.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MandiGreenText,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = feature,
                            fontSize = 13.sp,
                            color = MandiTextPrimary
                        )
                    }
                }
            }
        }
    }
}
