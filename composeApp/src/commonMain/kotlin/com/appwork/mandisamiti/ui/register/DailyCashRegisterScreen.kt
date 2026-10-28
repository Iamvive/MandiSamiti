package com.appwork.mandisamiti.ui.register

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.ui.components.SoundboxTopBar
import com.appwork.mandisamiti.ui.theme.MandiAccent
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiAmberLight
import com.appwork.mandisamiti.ui.theme.MandiAmberPrimary
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiBtnDangerBg
import com.appwork.mandisamiti.ui.theme.MandiBtnDangerFg
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessBg
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessFg
import com.appwork.mandisamiti.ui.theme.MandiGreenLight
import com.appwork.mandisamiti.ui.theme.MandiGreenPayable
import com.appwork.mandisamiti.ui.theme.MandiNavy
import com.appwork.mandisamiti.ui.theme.MandiNeutralLight
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiRedLight
import com.appwork.mandisamiti.ui.theme.MandiRedReceivable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

enum class BahiViewMode {
    SPLIT, // दो पल्ला (Left Jama, Right Name)
    JAMA,  // केवल जमा पक्ष
    NAME   // केवल नामे पक्ष
}

@Composable
fun DailyCashRegisterScreen(
    viewModel: DailyRegisterViewModel,
    onNavigateBack: () -> Unit = {},
    onShareWhatsApp: ((String) -> Unit)? = null,
    isEnglish: Boolean = false,
    showBackButton: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDirectCashDialog by remember { mutableStateOf<TransactionType?>(null) }
    var selectedPartyId by remember { mutableStateOf("") }
    var directAmountInput by remember { mutableStateOf("") }
    var directRemarksInput by remember { mutableStateOf("") }
    var physicalCashInput by remember { mutableStateOf("") }
    var bahiViewMode by remember { mutableStateOf(BahiViewMode.SPLIT) }

    val jamaList = remember(uiState.todayTransactions) {
        uiState.todayTransactions.filter { it.transaction.transactionType == TransactionType.JAMA_RECEIVED }
    }
    val nameList = remember(uiState.todayTransactions) {
        uiState.todayTransactions.filter { it.transaction.transactionType == TransactionType.UDHAR_GIVEN }
    }
    val totalJamaSidePaisa = uiState.openingCashPaisa + uiState.todayCashInPaisa
    val totalNameSidePaisa = uiState.todayCashOutPaisa + uiState.inHandCashDrawerPaisa
    val isTallied = totalJamaSidePaisa == totalNameSidePaisa

    Scaffold(
        topBar = {
            if (showBackButton) {
                SoundboxTopBar(
                    shopName = if (isEnglish) "Daily Cash Register" else "दैनिक गल्ला रोकड़ बही",
                    mandiLocation = if (isEnglish) "Shop: ${uiState.shopProfile?.shopName ?: "Cash Desk"}" else "दुकान: ${uiState.shopProfile?.shopName ?: "मंडी रोकड़"}",
                    isSoundEnabled = uiState.isSoundEnabled,
                    onToggleSound = {},
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isEnglish) "Go back" else "पीछे जाएं",
                                tint = MandiTextPrimary
                            )
                        }
                    }
                )
            }
        },
        containerColor = MandiBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Executive Cash In Hand Hero Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MandiSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MandiBorder)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (isEnglish) "Cash In Hand (Total)" else "गल्ले में कुल नकदी (Cash In Hand)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MandiTextSecondary
                            )
                            Text(
                                text = "₹${MandiMathEngine.paisaToRupeesString(uiState.inHandCashDrawerPaisa)}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiNavy
                            )
                        }

                        // Day Closing CTA Pill Button
                        Button(
                            onClick = { viewModel.openDayClosingSummary() },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MandiAmberLight,
                                contentColor = MandiAmberDark
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = MandiAmberDark,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.size(5.dp))
                            Text(
                                text = if (isEnglish) "Day Closing" else "दैनिक रोज़नामा",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiAmberDark
                            )
                        }
                    }

                    CashBreakdownItem(
                        label = if (isEnglish) "Opening Balance" else "पिछला शेष (Opening)",
                        amountPaisa = uiState.openingCashPaisa,
                        color = MandiNavy,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2-Way Metrics Breakdown (In vs Out)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CashBreakdownItem(
                            label = if (isEnglish) "Total Cash In" else "कुल जमा (In)",
                            amountPaisa = uiState.todayCashInPaisa,
                            color = MandiGreenPayable,
                            modifier = Modifier.weight(1f)
                        )
                        CashBreakdownItem(
                            label = if (isEnglish) "Total Cash Out" else "कुल निकासी (Out)",
                            amountPaisa = uiState.todayCashOutPaisa,
                            color = MandiRedReceivable,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick Direct Cash Entry Actions (No emojis, vector icons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showDirectCashDialog = TransactionType.JAMA_RECEIVED },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MandiBtnSuccessBg,
                        contentColor = MandiBtnSuccessFg
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MandiBtnSuccessFg,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = if (isEnglish) "Cash In" else "नकद जमा",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiBtnSuccessFg,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = { showDirectCashDialog = TransactionType.UDHAR_GIVEN },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MandiBtnDangerBg,
                        contentColor = MandiBtnDangerFg
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = null,
                        tint = MandiBtnDangerFg,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = if (isEnglish) "Cash Out" else "नकद निकासी",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiBtnDangerFg,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Authentic Indian Bahi Mode Switcher (पल्ला चयन)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Split View: दो पल्ला (जमा-नामे)
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (bahiViewMode == BahiViewMode.SPLIT) MandiAmberLight else MandiSurface)
                        .border(
                            width = 1.dp,
                            color = if (bahiViewMode == BahiViewMode.SPLIT) MandiAmberDark else MandiBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { bahiViewMode = BahiViewMode.SPLIT },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEnglish) "Split Bahi (L/R)" else "दो पल्ला (जमा-नामे)",
                        fontSize = 12.sp,
                        fontWeight = if (bahiViewMode == BahiViewMode.SPLIT) FontWeight.Bold else FontWeight.Medium,
                        color = if (bahiViewMode == BahiViewMode.SPLIT) MandiAmberDark else MandiTextSecondary
                    )
                }

                // Left: जमा पक्ष
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (bahiViewMode == BahiViewMode.JAMA) MandiGreenLight else MandiSurface)
                        .border(
                            width = 1.dp,
                            color = if (bahiViewMode == BahiViewMode.JAMA) MandiGreenPayable else MandiBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { bahiViewMode = BahiViewMode.JAMA },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEnglish) "Jama (${jamaList.size})" else "जमा पक्ष (${jamaList.size})",
                        fontSize = 12.sp,
                        fontWeight = if (bahiViewMode == BahiViewMode.JAMA) FontWeight.Bold else FontWeight.Medium,
                        color = if (bahiViewMode == BahiViewMode.JAMA) MandiGreenPayable else MandiTextSecondary
                    )
                }

                // Right: नामे पक्ष
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (bahiViewMode == BahiViewMode.NAME) MandiRedLight else MandiSurface)
                        .border(
                            width = 1.dp,
                            color = if (bahiViewMode == BahiViewMode.NAME) MandiRedReceivable else MandiBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { bahiViewMode = BahiViewMode.NAME },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEnglish) "Name (${nameList.size})" else "नामे पक्ष (${nameList.size})",
                        fontSize = 12.sp,
                        fontWeight = if (bahiViewMode == BahiViewMode.NAME) FontWeight.Bold else FontWeight.Medium,
                        color = if (bahiViewMode == BahiViewMode.NAME) MandiRedReceivable else MandiTextSecondary
                    )
                }
            }

            // Main Ledger Book Body (पन्ना)
            when (bahiViewMode) {
                BahiViewMode.SPLIT -> {
                    // Authentic 2-Column Split Bahi (Left Jama, Right Name)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MandiSurface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MandiBorder)
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Column Headers (जमा बायां | नामे दायां)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MandiNeutralLight)
                                    .padding(vertical = 8.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Header (जमा)
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MandiGreenPayable))
                                        Text(
                                            text = if (isEnglish) "JAMA (Cash In)" else "जमा पक्ष (आवक)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MandiGreenPayable
                                        )
                                    }
                                    Text(
                                        text = "${jamaList.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MandiTextSecondary
                                    )
                                }

                                // Center fold separator
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .width(1.dp)
                                        .height(18.dp)
                                        .background(MandiBorder)
                                )

                                // Right Header (नामे)
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MandiRedReceivable))
                                        Text(
                                            text = if (isEnglish) "NAME (Cash Out)" else "नामे पक्ष (निकासी)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MandiRedReceivable
                                        )
                                    }
                                    Text(
                                        text = "${nameList.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MandiTextSecondary
                                    )
                                }
                            }

                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MandiBorder))

                            // Scrollable Double Columns
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                // Left Side: Jama Entries
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentPadding = PaddingValues(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    item {
                                        BahiAnchorRow(
                                            title = if (isEnglish) "Opening" else "बाकी निकाला",
                                            subtitle = if (isEnglish) "Opening Cash" else "पिछला रोकड़ शेष",
                                            amountPaisa = uiState.openingCashPaisa,
                                            color = MandiNavy
                                        )
                                    }

                                    items(jamaList, key = { it.transaction.id }) { item ->
                                        BahiTransactionItem(
                                            item = item,
                                            isDeposit = true,
                                            isEnglish = isEnglish
                                        )
                                    }

                                    if (jamaList.isEmpty()) {
                                        item {
                                            Text(
                                                text = if (isEnglish) "No cash in" else "आज कोई जमा नहीं",
                                                fontSize = 11.sp,
                                                color = MandiTextMuted,
                                                modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Center Fold Line
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(1.dp)
                                        .background(MandiBorder)
                                )

                                // Right Side: Name Entries
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentPadding = PaddingValues(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(nameList, key = { it.transaction.id }) { item ->
                                        BahiTransactionItem(
                                            item = item,
                                            isDeposit = false,
                                            isEnglish = isEnglish
                                        )
                                    }

                                    if (nameList.isEmpty()) {
                                        item {
                                            Text(
                                                text = if (isEnglish) "No cash out" else "आज कोई निकासी नहीं",
                                                fontSize = 11.sp,
                                                color = MandiTextMuted,
                                                modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp)
                                            )
                                        }
                                    }

                                    item {
                                        BahiAnchorRow(
                                            title = if (isEnglish) "Cash In Hand" else "सिल्क बाकी",
                                            subtitle = if (isEnglish) "Closing Drawer" else "गल्ले में शुद्ध नकदी",
                                            amountPaisa = uiState.inHandCashDrawerPaisa,
                                            color = MandiNavy
                                        )
                                    }
                                }
                            }

                            // Bottom Tally Bar (दोनों पक्षों का जोड़)
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MandiBorder))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MandiNeutralLight)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEnglish) "Total Jama Side" else "कुल जमा जोड़ (बायां)",
                                        fontSize = 10.sp,
                                        color = MandiTextSecondary
                                    )
                                    Text(
                                        text = "₹${MandiMathEngine.paisaToRupeesString(totalJamaSidePaisa)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiGreenPayable
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isTallied) MandiGreenLight else MandiAmberLight)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isTallied) MandiGreenPayable else MandiAmberDark,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (isTallied) (if (isEnglish) "Balanced" else "जोड़ बराबर") else (if (isEnglish) "Difference" else "अंतर"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTallied) MandiGreenPayable else MandiAmberDark
                                    )
                                }

                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isEnglish) "Total Name Side" else "कुल नामे जोड़ (दायां)",
                                        fontSize = 10.sp,
                                        color = MandiTextSecondary
                                    )
                                    Text(
                                        text = "₹${MandiMathEngine.paisaToRupeesString(totalNameSidePaisa)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiRedReceivable
                                    )
                                }
                            }
                        }
                    }
                }

                BahiViewMode.JAMA -> {
                    // Full Width Jama Column
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            BahiAnchorRow(
                                title = if (isEnglish) "Opening Balance" else "बाकी निकाला (Opening Balance)",
                                subtitle = if (isEnglish) "Carried forward from yesterday" else "पिछला रोकड़ शेष",
                                amountPaisa = uiState.openingCashPaisa,
                                color = MandiNavy
                            )
                        }

                        items(jamaList, key = { it.transaction.id }) { item ->
                            BahiTransactionItem(
                                item = item,
                                isDeposit = true,
                                isEnglish = isEnglish
                            )
                        }

                        if (jamaList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isEnglish) "No cash deposits today" else "आज कोई रोकड़ जमा प्रविष्टि नहीं है",
                                        color = MandiTextMuted,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MandiNeutralLight)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEnglish) "Total Jama (Opening + Receipts)" else "कुल जमा जोड़ (पिछला + आज की आवक)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiTextPrimary
                                    )
                                    Text(
                                        text = "₹${MandiMathEngine.paisaToRupeesString(totalJamaSidePaisa)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiGreenPayable
                                    )
                                }
                            }
                        }
                    }
                }

                BahiViewMode.NAME -> {
                    // Full Width Name Column
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(nameList, key = { it.transaction.id }) { item ->
                            BahiTransactionItem(
                                item = item,
                                isDeposit = false,
                                isEnglish = isEnglish
                            )
                        }

                        if (nameList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isEnglish) "No cash withdrawals today" else "आज कोई रोकड़ निकासी प्रविष्टि नहीं है",
                                        color = MandiTextMuted,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        item {
                            BahiAnchorRow(
                                title = if (isEnglish) "Cash In Hand" else "सिल्क बाकी (Closing In Hand)",
                                subtitle = if (isEnglish) "Physical cash balance in drawer" else "गल्ले में शुद्ध नकदी",
                                amountPaisa = uiState.inHandCashDrawerPaisa,
                                color = MandiNavy
                            )
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MandiNeutralLight)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEnglish) "Total Name (Outflows + Silak)" else "कुल नामे जोड़ (खर्च + सिल्क बाकी)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiTextPrimary
                                    )
                                    Text(
                                        text = "₹${MandiMathEngine.paisaToRupeesString(totalNameSidePaisa)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiRedReceivable
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    // Day Closing Summary & Reconciliation Modal
    if (uiState.isDayClosingSummaryOpen) {
        val expectedInHand = uiState.inHandCashDrawerPaisa
        val countedRs = physicalCashInput.toLongOrNull()
        val countedPaisa = if (countedRs != null) countedRs * 100L else null
        val diffPaisa = if (countedPaisa != null) countedPaisa - expectedInHand else null

        AlertDialog(
            onDismissRequest = { viewModel.closeDayClosingSummary() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MandiAmberDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isEnglish) "Day Closing Reconciliation" else "दैनिक रोज़नामा (Day Closing)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MandiTextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Summary Details Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MandiNeutralLight)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isEnglish) "Opening Balance:" else "पिछला शेष (Opening):", fontSize = 13.sp, color = MandiTextSecondary)
                            Text("₹${MandiMathEngine.paisaToRupeesString(uiState.openingCashPaisa)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MandiNavy)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isEnglish) "Total Cash In:" else "कुल नकद आवक (In):", fontSize = 13.sp, color = MandiTextSecondary)
                            Text("+₹${MandiMathEngine.paisaToRupeesString(uiState.todayCashInPaisa)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MandiGreenPayable)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isEnglish) "Total Cash Out:" else "कुल नकद निकासी (Out):", fontSize = 13.sp, color = MandiTextSecondary)
                            Text("-₹${MandiMathEngine.paisaToRupeesString(uiState.todayCashOutPaisa)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MandiRedReceivable)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isEnglish) "Expected Cash In Hand:" else "गल्ले में शुद्ध नकदी (Expected):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MandiTextPrimary)
                            Text("₹${MandiMathEngine.paisaToRupeesString(expectedInHand)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MandiNavy)
                        }
                    }

                    // Physical Cash Verification Input
                    OutlinedTextField(
                        value = physicalCashInput,
                        onValueChange = { physicalCashInput = it },
                        label = { Text(if (isEnglish) "Physical Cash Count ₹ (Optional)" else "वास्तविक गल्ला गिनती ₹ (वैकल्पिक)") },
                        placeholder = { Text(if (isEnglish) "Enter counted cash in drawer" else "गल्ले में गिने हुए रुपये दर्ज करें") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Difference / Discrepancy Indicator
                    if (diffPaisa != null) {
                        val diffColor = when {
                            diffPaisa == 0L -> MandiGreenPayable
                            diffPaisa > 0L -> MandiAmberDark
                            else -> MandiRedReceivable
                        }
                        val diffLabel = when {
                            diffPaisa == 0L -> if (isEnglish) "Cash matches perfectly (₹0)" else "हिसाब बिल्कुल बराबर है (₹0)"
                            diffPaisa > 0L -> if (isEnglish) "Surplus Cash: +₹${MandiMathEngine.paisaToRupeesString(diffPaisa)}" else "फालतू नकदी: +₹${MandiMathEngine.paisaToRupeesString(diffPaisa)}"
                            else -> if (isEnglish) "Shortage: -₹${MandiMathEngine.paisaToRupeesString(-diffPaisa)}" else "कमी: -₹${MandiMathEngine.paisaToRupeesString(-diffPaisa)}"
                        }
                        Text(
                            text = diffLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = diffColor
                        )
                    }

                    // Soundbox Speech Action
                    OutlinedButton(
                        onClick = { viewModel.announceDayClosingSummary() },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isEnglish) "Announce on Soundbox" else "साउंडबॉक्स पर सुनें", color = MandiTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reportText = viewModel.generateDayClosingReportText(physicalCashInput.toLongOrNull())
                        if (onShareWhatsApp != null) {
                            onShareWhatsApp(reportText)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MandiPrimaryAction,
                        contentColor = MandiPrimaryActionText
                    )
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = MandiPrimaryActionText, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(if (isEnglish) "Share on WhatsApp" else "व्हाट्सएप शेयर", color = MandiPrimaryActionText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDayClosingSummary() }) {
                    Text(if (isEnglish) "Close" else "बंद करें", color = MandiTextSecondary)
                }
            }
        )
    }

    // Direct Cash Dialog
    if (showDirectCashDialog != null) {
        val isDeposit = showDirectCashDialog == TransactionType.JAMA_RECEIVED
        AlertDialog(
            onDismissRequest = { showDirectCashDialog = null },
            title = {
                Text(
                    text = if (isDeposit) (if (isEnglish) "Record Cash Deposit" else "नकद जमा प्रविष्टि") else (if (isEnglish) "Record Cash Withdrawal" else "नकद निकासी प्रविष्टि"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MandiTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Party selector
                    if (uiState.availableParties.isNotEmpty()) {
                        Text(
                            text = if (isEnglish) "Select Account (Optional):" else "खाता चुनें (वैकल्पिक):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MandiTextSecondary
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 120.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MandiNeutralLight)
                                .padding(4.dp)
                        ) {
                            items(uiState.availableParties) { party ->
                                val isSelected = selectedPartyId == party.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) MandiAmberLight else Color.Transparent)
                                        .clickable { selectedPartyId = party.id }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${party.name} (${party.village ?: (if (isEnglish) "Village" else "गांव")})",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = MandiTextPrimary
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MandiAmberDark,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = directAmountInput,
                        onValueChange = { directAmountInput = it },
                        label = { Text(if (isEnglish) "Amount (₹)" else "राशि (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = directRemarksInput,
                        onValueChange = { directRemarksInput = it },
                        label = { Text(if (isEnglish) "Remarks / Note (Optional)" else "विवरण / नोट (वैकल्पिक)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = directAmountInput.toLongOrNull() ?: 0L
                        if (amount > 0L) {
                            val partyId = selectedPartyId.ifBlank { uiState.availableParties.firstOrNull()?.id ?: "" }
                            viewModel.recordDailyEntry(
                                partyId = partyId,
                                transactionType = showDirectCashDialog!!,
                                amountRs = amount,
                                paymentMode = PaymentMode.CASH,
                                remarks = directRemarksInput.ifBlank { null }
                            )
                            showDirectCashDialog = null
                            directAmountInput = ""
                            directRemarksInput = ""
                            selectedPartyId = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDeposit) MandiBtnSuccessBg else MandiBtnDangerBg,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isEnglish) "Save Entry" else "सुरक्षित करें", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectCashDialog = null; selectedPartyId = "" }) {
                    Text(if (isEnglish) "Cancel" else "रद्द करें", color = MandiTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun CashBreakdownItem(
    label: String,
    amountPaisa: Long,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MandiNeutralLight)
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MandiTextSecondary
            )
            Text(
                text = "₹${MandiMathEngine.paisaToRupeesString(amountPaisa)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun BahiTransactionItem(
    item: TransactionWithParty,
    isDeposit: Boolean,
    isEnglish: Boolean
) {
    val remarks = item.transaction.remarks
    val isVoid = item.transaction.isVoid
    val amountStr = MandiMathEngine.paisaToRupeesString(item.transaction.amountPaisa)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MandiNeutralLight)
            .alpha(if (isVoid) 0.5f else 1f)
            .padding(6.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.partyName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "${if (isDeposit) "+" else "-"}₹$amountStr",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDeposit) MandiGreenPayable else MandiRedReceivable,
                    textDecoration = if (isVoid) TextDecoration.LineThrough else null
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (!remarks.isNullOrBlank()) remarks else (if (isDeposit) (if (isEnglish) "Cash In" else "नकद जमा") else (if (isEnglish) "Cash Out" else "नकद निकासी")),
                    fontSize = 10.sp,
                    color = MandiTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (item.transaction.paymentMode != PaymentMode.CASH) {
                    Text(
                        text = item.transaction.paymentMode.name,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MandiTextMuted
                    )
                }
            }

            if (isVoid) {
                Text(
                    text = if (isEnglish) "VOID" else "रद्द प्रविष्टि",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiRedReceivable
                )
            }
        }
    }
}

@Composable
private fun BahiAnchorRow(
    title: String,
    subtitle: String,
    amountPaisa: Long,
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = MandiTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "₹${MandiMathEngine.paisaToRupeesString(amountPaisa)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

