package com.appwork.mandisamiti.ui.register

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.appwork.mandisamiti.domain.math.MandiCurrencyFormatter
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
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
import com.appwork.mandisamiti.ui.theme.MandiGreenReceivable
import com.appwork.mandisamiti.ui.theme.MandiNavy
import com.appwork.mandisamiti.ui.theme.MandiNeutralLight
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiRedLight
import com.appwork.mandisamiti.ui.theme.MandiRedPayable
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
    var showOpeningBalanceDialog by remember { mutableStateOf(false) }
    var openingBalanceInput by remember { mutableStateOf("") }
    var openingBalanceRemarks by remember { mutableStateOf("") }
    var showCashHomeDialog by remember { mutableStateOf(false) }
    var cashHomeInput by remember { mutableStateOf("") }
    var cashHomeRemarks by remember { mutableStateOf("") }
    var bahiViewMode by remember { mutableStateOf(BahiViewMode.SPLIT) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val jamaList = remember(uiState.todayTransactions) {
        uiState.todayTransactions.filter { it.transaction.transactionType == TransactionType.JAMA_RECEIVED }
    }
    val nameList = remember(uiState.todayTransactions) {
        uiState.todayTransactions.filter { it.transaction.transactionType == TransactionType.UDHAR_GIVEN }
    }
    val totalJamaSidePaisa = uiState.totalJamaSidePaisa
    val totalNameSidePaisa = uiState.totalNameSidePaisa
    val isTallied = uiState.reconciliationStatus == BahiReconciliationStatus.BALANCED

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
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MandiSurface,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MandiBorder)
            ) {
                if (!uiState.isReadOnly) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showDirectCashDialog = TransactionType.JAMA_RECEIVED },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
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
                                text = if (isEnglish) "Cash In (Jama)" else "नकद जमा (In)",
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
                                .height(44.dp),
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
                                text = if (isEnglish) "Cash Out (Naame)" else "नकद निकासी (Out)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiBtnDangerFg,
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MandiTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) "Historical bahi is view-only. Cash entry disabled." else "पुराने दिन की बही सुरक्षित है — नया लेन-देन बंद है।",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MandiTextSecondary
                        )
                    }
                }
            }
        },
        containerColor = MandiBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // High-Density 1-Row Financial Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MandiSurface),
                border = BorderStroke(1.dp, MandiBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Closing Cash (Prominent)
                    Column(
                        modifier = Modifier
                            .weight(1.3f)
                            .clickable(enabled = !uiState.isReadOnly) {
                                physicalCashInput = uiState.physicalCountPaisa?.let { (it / 100L).toString() } ?: ""
                                viewModel.openDayClosingSummary()
                            },
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "In Drawer" else "गल्ला रोकड़",
                            fontSize = 11.sp,
                            color = MandiTextSecondary
                        )
                        Text(
                            text = MandiCurrencyFormatter.formatRupees(uiState.inHandCashDrawerPaisa),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isNegativeCash) MandiRedPayable else MandiNavy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MandiBorder))

                    // 2. Opening Balance
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .padding(start = 6.dp)
                            .clickable(enabled = !uiState.isReadOnly) {
                                openingBalanceInput = if (uiState.openingCashPaisa > 0) (uiState.openingCashPaisa / 100L).toString() else ""
                                openingBalanceRemarks = ""
                                showOpeningBalanceDialog = true
                            },
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Opening" else "पिछला शेष",
                            fontSize = 10.sp,
                            color = MandiTextSecondary
                        )
                        Text(
                            text = MandiCurrencyFormatter.formatRupees(uiState.openingCashPaisa),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MandiNavy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MandiBorder))

                    // 3. Today Cash In
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "In (+)" else "आवक (+)",
                            fontSize = 10.sp,
                            color = MandiTextSecondary
                        )
                        Text(
                            text = "₹${MandiMathEngine.paisaToRupeesString(uiState.todayCashInPaisa)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiGreenReceivable,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MandiBorder))

                    // 4. Today Cash Out
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Out (-)" else "निकासी (-)",
                            fontSize = 10.sp,
                            color = MandiTextSecondary
                        )
                        Text(
                            text = "₹${MandiMathEngine.paisaToRupeesString(uiState.todayCashOutPaisa)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiRedPayable,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 5. Overflow menu button
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = if (isEnglish) "More options" else "अन्य विकल्प",
                                tint = MandiTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isEnglish) "Set Opening Float" else "शुरुआती रोकड़ (Opening Float)") },
                                onClick = {
                                    showOverflowMenu = false
                                    openingBalanceInput = if (uiState.openingCashPaisa > 0) (uiState.openingCashPaisa / 100L).toString() else ""
                                    openingBalanceRemarks = ""
                                    showOpeningBalanceDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isEnglish) "Cash to Home (Drawings)" else "घर निकासी (Cash to Home)") },
                                onClick = {
                                    showOverflowMenu = false
                                    cashHomeInput = ""
                                    cashHomeRemarks = ""
                                    showCashHomeDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isEnglish) "Day Closing Summary" else "दैनिक रोज़नामा (Day Closing)") },
                                onClick = {
                                    showOverflowMenu = false
                                    physicalCashInput = uiState.physicalCountPaisa?.let { (it / 100L).toString() } ?: ""
                                    viewModel.openDayClosingSummary()
                                }
                            )
                        }
                    }
                }
            }

            // Negative Cash Warning Banner (Opening float missing / cash below zero)
            if (uiState.isNegativeCash) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MandiAmberLight
                    ),
                    border = BorderStroke(1.dp, MandiAmberDark.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MandiAmberDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isEnglish) "Cash is below zero. Opening balance may be missing." else "गल्ले में रकम माइनस है। शुरुआती रोकड़ दर्ज करें?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MandiAmberDark,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                openingBalanceInput = if (uiState.openingCashPaisa > 0) (uiState.openingCashPaisa / 100L).toString() else ""
                                openingBalanceRemarks = ""
                                showOpeningBalanceDialog = true
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MandiAmberDark,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Set Float" else "रोकड़ दर्ज करें",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Sticky Date Navigation Bar (दैनिक तारीख चयन)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MandiSurface),
                border = BorderStroke(1.dp, MandiBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateToPreviousDay() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = if (isEnglish) "Previous Day" else "पिछला दिन",
                            tint = MandiNavy,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formatBahiDate(uiState.selectedDate, isEnglish, uiState.isToday),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiNavy
                        )
                        if (uiState.isReadOnly) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MandiNeutralLight)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) "Archived" else "सुरक्षित",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MandiTextSecondary
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { viewModel.navigateToNextDay() },
                        enabled = !uiState.isToday,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = if (isEnglish) "Next Day" else "अगला दिन",
                            tint = if (!uiState.isToday) MandiNavy else MandiTextMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // View-Only Past Day Banner
            if (uiState.isReadOnly) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MandiNeutralLight),
                    border = BorderStroke(1.dp, MandiBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MandiTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isEnglish) "Past date — View Only" else "पिछला दिन, सिर्फ देखने के लिए (View Only)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MandiTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { viewModel.navigateToToday() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Back to Today" else "आज पर जाएं",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiAmberDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

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
                            color = if (bahiViewMode == BahiViewMode.JAMA) MandiGreenReceivable else MandiBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { bahiViewMode = BahiViewMode.JAMA },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEnglish) "Jama (${jamaList.size})" else "जमा पक्ष (${jamaList.size})",
                        fontSize = 12.sp,
                        fontWeight = if (bahiViewMode == BahiViewMode.JAMA) FontWeight.Bold else FontWeight.Medium,
                        color = if (bahiViewMode == BahiViewMode.JAMA) MandiGreenReceivable else MandiTextSecondary
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
                            color = if (bahiViewMode == BahiViewMode.NAME) MandiRedPayable else MandiBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { bahiViewMode = BahiViewMode.NAME },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEnglish) "Naame (${nameList.size})" else "नामे पक्ष (${nameList.size})",
                        fontSize = 12.sp,
                        fontWeight = if (bahiViewMode == BahiViewMode.NAME) FontWeight.Bold else FontWeight.Medium,
                        color = if (bahiViewMode == BahiViewMode.NAME) MandiRedPayable else MandiTextSecondary
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
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MandiGreenReceivable))
                                        Text(
                                            text = if (isEnglish) "Jama (Cash In)" else "जमा पक्ष (आवक)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MandiGreenReceivable
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
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MandiRedPayable))
                                        Text(
                                            text = if (isEnglish) "Naame (Cash Out)" else "नामे पक्ष (निकासी)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MandiRedPayable
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
                                            color = MandiNavy,
                                            actionLabel = if (isEnglish) "Edit" else "बदलें",
                                            onClick = {
                                                openingBalanceInput = if (uiState.openingCashPaisa > 0) (uiState.openingCashPaisa / 100L).toString() else ""
                                                openingBalanceRemarks = ""
                                                showOpeningBalanceDialog = true
                                            }
                                        )
                                    }

                                    items(jamaList, key = { it.transaction.id }) { item ->
                                        BahiTransactionItem(
                                            item = item,
                                            isDeposit = true,
                                            isEnglish = isEnglish,
                                            isCompact = true
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
                                            isEnglish = isEnglish,
                                            isCompact = true
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
                                            subtitle = if (uiState.physicalCountPaisa != null) {
                                                if (isEnglish) "Counted physical cash" else "गिनी हुई शुद्ध नकदी"
                                            } else {
                                                if (isEnglish) "Book closing (uncounted)" else "बही शेष (गिनती बाकी)"
                                            },
                                            amountPaisa = uiState.effectiveClosingCashPaisa,
                                            color = MandiNavy,
                                            actionLabel = if (uiState.physicalCountPaisa == null) (if (isEnglish) "Count" else "गिनती") else (if (isEnglish) "Edit" else "बदलें"),
                                            onClick = {
                                                physicalCashInput = uiState.physicalCountPaisa?.let { (it / 100L).toString() } ?: ""
                                                viewModel.openDayClosingSummary()
                                            }
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
                                        color = MandiGreenReceivable
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            physicalCashInput = uiState.physicalCountPaisa?.let { (it / 100L).toString() } ?: ""
                                            viewModel.openDayClosingSummary()
                                        }
                                        .background(
                                            when (uiState.reconciliationStatus) {
                                                BahiReconciliationStatus.NOT_COUNTED -> MandiBorder.copy(alpha = 0.35f)
                                                BahiReconciliationStatus.BALANCED -> MandiGreenLight
                                                BahiReconciliationStatus.DISCREPANCY -> MandiAmberLight
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val badgeIcon = when (uiState.reconciliationStatus) {
                                        BahiReconciliationStatus.NOT_COUNTED -> Icons.Default.Info
                                        BahiReconciliationStatus.BALANCED -> Icons.Default.CheckCircle
                                        BahiReconciliationStatus.DISCREPANCY -> Icons.Default.Warning
                                    }
                                    val badgeColor = when (uiState.reconciliationStatus) {
                                        BahiReconciliationStatus.NOT_COUNTED -> MandiTextSecondary
                                        BahiReconciliationStatus.BALANCED -> MandiGreenReceivable
                                        BahiReconciliationStatus.DISCREPANCY -> MandiAmberDark
                                    }
                                    val badgeText = when (uiState.reconciliationStatus) {
                                        BahiReconciliationStatus.NOT_COUNTED -> if (isEnglish) "Count Pending" else "गिनती बाकी"
                                        BahiReconciliationStatus.BALANCED -> if (isEnglish) "Balanced" else "जोड़ बराबर"
                                        BahiReconciliationStatus.DISCREPANCY -> {
                                            val diffAbs = if (uiState.discrepancyPaisa >= 0) uiState.discrepancyPaisa else -uiState.discrepancyPaisa
                                            val diffStr = MandiMathEngine.paisaToRupeesString(diffAbs)
                                            if (uiState.discrepancyPaisa > 0) {
                                                if (isEnglish) "Diff: +₹$diffStr" else "अंतर: +₹$diffStr"
                                            } else {
                                                if (isEnglish) "Diff: -₹$diffStr" else "अंतर: -₹$diffStr"
                                            }
                                        }
                                    }
                                    Icon(
                                        imageVector = badgeIcon,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = badgeText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }

                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isEnglish) "Total Naame Side" else "कुल नामे जोड़ (दायां)",
                                        fontSize = 11.sp,
                                        color = MandiTextSecondary
                                    )
                                    Text(
                                        text = "₹${MandiMathEngine.paisaToRupeesString(totalNameSidePaisa)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiRedPayable
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
                                color = MandiNavy,
                                actionLabel = if (isEnglish) "Edit" else "बदलें",
                                onClick = {
                                    openingBalanceInput = if (uiState.openingCashPaisa > 0) (uiState.openingCashPaisa / 100L).toString() else ""
                                    openingBalanceRemarks = ""
                                    showOpeningBalanceDialog = true
                                }
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
                                        color = MandiGreenReceivable
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
                                subtitle = if (uiState.physicalCountPaisa != null) {
                                    if (isEnglish) "Counted physical cash in drawer" else "गल्ले में गिनी हुई शुद्ध नकदी"
                                } else {
                                    if (isEnglish) "Book closing cash in drawer" else "बही शेष (गिनती बाकी)"
                                },
                                amountPaisa = uiState.effectiveClosingCashPaisa,
                                color = MandiNavy,
                                actionLabel = if (uiState.physicalCountPaisa == null) (if (isEnglish) "Count" else "गिनती") else (if (isEnglish) "Edit" else "बदलें"),
                                onClick = {
                                    physicalCashInput = uiState.physicalCountPaisa?.let { (it / 100L).toString() } ?: ""
                                    viewModel.openDayClosingSummary()
                                }
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
                                        text = if (isEnglish) "Total Naame (Outflows + Silak)" else "कुल नामे जोड़ (खर्च + सिल्क बाकी)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiTextPrimary
                                    )
                                    Text(
                                        text = "₹${MandiMathEngine.paisaToRupeesString(totalNameSidePaisa)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiRedPayable
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
                            Text("+₹${MandiMathEngine.paisaToRupeesString(uiState.todayCashInPaisa)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MandiGreenReceivable)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isEnglish) "Total Cash Out:" else "कुल नकद निकासी (Out):", fontSize = 13.sp, color = MandiTextSecondary)
                            Text("-₹${MandiMathEngine.paisaToRupeesString(uiState.todayCashOutPaisa)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MandiRedPayable)
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
                            diffPaisa == 0L -> MandiGreenReceivable
                            diffPaisa > 0L -> MandiAmberDark
                            else -> MandiRedPayable
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

                    // Physical Cash Actions (Save & Reset)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.setPhysicalCashCount(physicalCashInput.toLongOrNull())
                                viewModel.closeDayClosingSummary()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MandiGreenReceivable,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = if (isEnglish) "Save Count" else "गिनती दर्ज करें",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        if (uiState.physicalCountPaisa != null) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.setPhysicalCashCount(null)
                                    physicalCashInput = ""
                                    viewModel.closeDayClosingSummary()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (isEnglish) "Reset" else "हटाएं",
                                    color = MandiBtnDangerBg,
                                    fontSize = 13.sp
                                )
                            }
                        }
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
                        val count = physicalCashInput.toLongOrNull()
                        if (count != null) {
                            viewModel.setPhysicalCashCount(count)
                        }
                        val reportText = viewModel.generateDayClosingReportText(count)
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

    // Opening Balance Setup Dialog
    if (showOpeningBalanceDialog) {
        AlertDialog(
            onDismissRequest = { showOpeningBalanceDialog = false },
            title = {
                Text(
                    text = if (isEnglish) "Set / Adjust Opening Cash" else "शुरुआती रोकड़ शेष (बाकी निकाला)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MandiTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isEnglish) {
                            "Set opening cash float in drawer. Current: ₹${MandiMathEngine.paisaToRupeesString(uiState.openingCashPaisa)}"
                        } else {
                            "दुकान खोलने के समय गल्ले में उपलब्ध नकदी निर्धारित करें। वर्तमान: ₹${MandiMathEngine.paisaToRupeesString(uiState.openingCashPaisa)}"
                        },
                        fontSize = 12.sp,
                        color = MandiTextSecondary
                    )
                    OutlinedTextField(
                        value = openingBalanceInput,
                        onValueChange = { openingBalanceInput = it },
                        label = { Text(if (isEnglish) "Opening Cash Float ₹" else "प्रारंभिक रोकड़ शेष ₹") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = openingBalanceRemarks,
                        onValueChange = { openingBalanceRemarks = it },
                        label = { Text(if (isEnglish) "Note / Reason (Optional)" else "कारण / नोट (वैकल्पिक)") },
                        placeholder = { Text(if (isEnglish) "e.g. Day 1 initial float" else "जैसे: पहला दिन या घर से लाई नकदी") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = openingBalanceInput.toLongOrNull() ?: 0L
                        viewModel.setOrAdjustOpeningBalance(
                            targetOpeningRs = amount,
                            remarks = openingBalanceRemarks.ifBlank { null }
                        )
                        showOpeningBalanceDialog = false
                        openingBalanceInput = ""
                        openingBalanceRemarks = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MandiPrimaryAction,
                        contentColor = MandiPrimaryActionText
                    )
                ) {
                    Text(if (isEnglish) "Save Opening Cash" else "रोकड़ शेष दर्ज करें", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpeningBalanceDialog = false }) {
                    Text(if (isEnglish) "Cancel" else "रद्द करें", color = MandiTextSecondary)
                }
            }
        )
    }

    // Cash to Home (Owner Drawings) Dialog
    if (showCashHomeDialog) {
        AlertDialog(
            onDismissRequest = { showCashHomeDialog = false },
            title = {
                Text(
                    text = if (isEnglish) "Cash to Home (Owner Drawings)" else "गल्ले से घर निकासी (मालिक का आहरण)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MandiTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isEnglish) {
                            "Record cash taken home from drawer. Current in drawer: ₹${MandiMathEngine.paisaToRupeesString(uiState.inHandCashDrawerPaisa)}"
                        } else {
                            "दुकानदार/मालिक द्वारा गल्ले से घर ले जाई गई नकदी दर्ज करें। वर्तमान गल्ला: ₹${MandiMathEngine.paisaToRupeesString(uiState.inHandCashDrawerPaisa)}"
                        },
                        fontSize = 12.sp,
                        color = MandiTextSecondary
                    )
                    OutlinedTextField(
                        value = cashHomeInput,
                        onValueChange = { cashHomeInput = it },
                        label = { Text(if (isEnglish) "Amount ₹" else "निकासी राशि ₹") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cashHomeRemarks,
                        onValueChange = { cashHomeRemarks = it },
                        label = { Text(if (isEnglish) "Note (Optional)" else "विवरण / नोट (वैकल्पिक)") },
                        placeholder = { Text(if (isEnglish) "e.g. Daily night drawings" else "जैसे: शाम को घर ले गए") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = cashHomeInput.toLongOrNull() ?: 0L
                        if (amount > 0L) {
                            viewModel.recordCashWithdrawalHome(
                                amountRs = amount,
                                remarks = cashHomeRemarks.ifBlank { null }
                            )
                            showCashHomeDialog = false
                            cashHomeInput = ""
                            cashHomeRemarks = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MandiBtnDangerBg,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isEnglish) "Record Cash Out" else "निकासी दर्ज करें", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCashHomeDialog = false }) {
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
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MandiNeutralLight)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MandiTextSecondary
                )
                if (actionLabel != null) {
                    Text(
                        text = actionLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiAmberDark,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MandiAmberLight)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                text = MandiCurrencyFormatter.formatRupees(amountPaisa),
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
    isEnglish: Boolean,
    isCompact: Boolean = false
) {
    val remarks = item.transaction.remarks
    val isVoid = item.transaction.isVoid
    val amountStr = MandiMathEngine.paisaToRupeesString(item.transaction.amountPaisa)
    val displayName = when {
        item.partyName.isNotBlank() && item.partyName != "खाता" -> item.partyName
        !remarks.isNullOrBlank() -> remarks
        isDeposit -> if (isEnglish) "Jama (Cash In)" else "नकद जमा"
        else -> if (isEnglish) "Naame (Cash Out)" else "विविध निकासी (नामे)"
    }
    val secondaryText = buildString {
        if (!item.village.isNullOrBlank()) {
            append(item.village)
        }
        if (!remarks.isNullOrBlank() && remarks != displayName) {
            if (isNotEmpty()) append(" • ")
            append(remarks)
        }
        if (isEmpty()) {
            append(if (isDeposit) (if (isEnglish) "Jama" else "जमा") else (if (isEnglish) "Naame" else "नामे"))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MandiNeutralLight)
            .alpha(if (isVoid) 0.5f else 1f)
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "${if (isDeposit) "+" else "-"}₹$amountStr",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDeposit) MandiGreenReceivable else MandiRedPayable,
                    textDecoration = if (isVoid) TextDecoration.LineThrough else null
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = secondaryText,
                    fontSize = 14.sp,
                    color = MandiTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (item.transaction.paymentMode != PaymentMode.CASH) {
                    Text(
                        text = item.transaction.paymentMode.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MandiTextSecondary
                    )
                }
            }

            if (isVoid) {
                Text(
                    text = if (isEnglish) "VOID" else "रद्द प्रविष्टि",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiRedPayable
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
    color: Color,
    actionLabel: String? = null,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.08f))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (actionLabel != null) {
                        Text(
                            text = actionLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiAmberDark,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MandiAmberLight)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MandiTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = MandiCurrencyFormatter.formatRupees(amountPaisa),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

private fun formatBahiDate(date: LocalDate, isEnglish: Boolean, isToday: Boolean): String {
    val dayNameHindi = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "सोमवार"
        DayOfWeek.TUESDAY -> "मंगलवार"
        DayOfWeek.WEDNESDAY -> "बुधवार"
        DayOfWeek.THURSDAY -> "गुरुवार"
        DayOfWeek.FRIDAY -> "शुक्रवार"
        DayOfWeek.SATURDAY -> "शनिवार"
        DayOfWeek.SUNDAY -> "रविवार"
        else -> ""
    }
    val monthNameHindi = when (date.monthNumber) {
        1 -> "जनवरी"
        2 -> "फ़रवरी"
        3 -> "मार्च"
        4 -> "अप्रैल"
        5 -> "मई"
        6 -> "जून"
        7 -> "जुलाई"
        8 -> "अगस्त"
        9 -> "सितंबर"
        10 -> "अक्टूबर"
        11 -> "नवंबर"
        12 -> "दिसंबर"
        else -> ""
    }
    val monthNameEnglish = when (date.monthNumber) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> ""
    }
    val dayNameEnglish = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "Monday"
        DayOfWeek.TUESDAY -> "Tuesday"
        DayOfWeek.WEDNESDAY -> "Wednesday"
        DayOfWeek.THURSDAY -> "Thursday"
        DayOfWeek.FRIDAY -> "Friday"
        DayOfWeek.SATURDAY -> "Saturday"
        DayOfWeek.SUNDAY -> "Sunday"
        else -> ""
    }
    return if (isEnglish) {
        if (isToday) "Today, ${date.dayOfMonth} $monthNameEnglish ${date.year}"
        else "${date.dayOfMonth} $monthNameEnglish ${date.year}, $dayNameEnglish"
    } else {
        if (isToday) "आज, ${date.dayOfMonth} $monthNameHindi ${date.year}"
        else "${date.dayOfMonth} $monthNameHindi ${date.year}, $dayNameHindi"
    }
}

