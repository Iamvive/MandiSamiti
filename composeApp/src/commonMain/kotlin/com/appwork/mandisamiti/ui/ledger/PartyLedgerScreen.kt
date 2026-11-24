package com.appwork.mandisamiti.ui.ledger

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.validation.MandiValidators
import kotlinx.datetime.Clock
import kotlinx.coroutines.launch
import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import com.appwork.mandisamiti.domain.model.LendDirection
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.domain.model.slipLabel
import com.appwork.mandisamiti.domain.usecase.CashSide
import com.appwork.mandisamiti.ui.udhaar.LoanCardItem
import com.appwork.mandisamiti.ui.udhaar.UdhaarPersonUiState
import com.appwork.mandisamiti.ui.udhaar.UdhaarPersonViewModel
import com.appwork.mandisamiti.ui.udhaar.UdhaarRepayDialog
import com.appwork.mandisamiti.ui.components.SoundboxTopBar
import com.appwork.mandisamiti.ui.components.PhoneNeededBadge
import com.appwork.mandisamiti.ui.theme.MandiAccent
import com.appwork.mandisamiti.ui.theme.MandiAmberPrimary
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiAmberLight
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiBtnDangerBg
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessBg
import com.appwork.mandisamiti.ui.theme.MandiGreenBorder
import com.appwork.mandisamiti.ui.theme.MandiGreenLight
import com.appwork.mandisamiti.ui.theme.MandiGreenReceivable
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiGreenText
import com.appwork.mandisamiti.ui.theme.MandiNavy
import com.appwork.mandisamiti.ui.theme.MandiNeutralLight
import com.appwork.mandisamiti.ui.theme.MandiNeutralText
import com.appwork.mandisamiti.ui.theme.MandiOrangeLend
import com.appwork.mandisamiti.ui.theme.MandiRedBorder
import com.appwork.mandisamiti.ui.theme.MandiRedLight
import com.appwork.mandisamiti.ui.theme.MandiRedPayable
import com.appwork.mandisamiti.ui.theme.MandiRedText
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

@Composable
fun PartyLedgerScreen(
    viewModel: PartyLedgerViewModel,
    onNavigateBack: () -> Unit,
    onShareWhatsAppReceipt: (String) -> Unit,
    onEditDeal: (String) -> Unit = {},
    onOpenBuyReceipt: (String) -> Unit = {},
    onOpenSellReceipt: (String) -> Unit = {},
    isEnglish: Boolean = false,
    udhaarPersonViewModelFactory: ((LendDirection) -> UdhaarPersonViewModel)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val party = uiState.party

    var showPlainCashSide by rememberSaveable { mutableStateOf<String?>(null) }
    var plainCashAmountInput by rememberSaveable { mutableStateOf("") }
    var plainCashRemarksInput by rememberSaveable { mutableStateOf("") }

    val givenPersonViewModel = remember(party?.id, viewModel.shopId) {
        if (viewModel.lendRepository != null) {
            udhaarPersonViewModelFactory?.invoke(LendDirection.GIVEN)
                ?: UdhaarPersonViewModel(
                    partyId = viewModel.partyId,
                    direction = LendDirection.GIVEN,
                    shopId = viewModel.shopId,
                    lendRepository = viewModel.lendRepository,
                    partyRepository = viewModel.partyRepository,
                    udhaarUseCase = viewModel.udhaarUseCase
                )
        } else null
    }

    val takenPersonViewModel = remember(party?.id, viewModel.shopId) {
        if (viewModel.lendRepository != null) {
            udhaarPersonViewModelFactory?.invoke(LendDirection.TAKEN)
                ?: UdhaarPersonViewModel(
                    partyId = viewModel.partyId,
                    direction = LendDirection.TAKEN,
                    shopId = viewModel.shopId,
                    lendRepository = viewModel.lendRepository,
                    partyRepository = viewModel.partyRepository,
                    udhaarUseCase = viewModel.udhaarUseCase
                )
        } else null
    }

    var editingSettlement by remember { mutableStateOf<com.appwork.mandisamiti.domain.model.LendSettlement?>(null) }
    var historySettlement by remember { mutableStateOf<com.appwork.mandisamiti.domain.model.LendSettlement?>(null) }

    var voidTarget by remember { mutableStateOf<LedgerItem?>(null) }
    val slipNoById = remember(uiState.ledgerItems) {
        buildMap<String, Long> {
            uiState.ledgerItems.forEach { li ->
                when (li) {
                    is LedgerItem.BuyItem -> li.buy.slipNo?.let { put(li.buy.id, it) }
                    is LedgerItem.SellItem -> li.sell.slipNo?.let { put(li.sell.id, it) }
                    else -> Unit
                }
            }
        }
    }
    var lendEditTarget by remember { mutableStateOf<LedgerItem.LendItem?>(null) }
    var cashEditTarget by remember { mutableStateOf<LedgerItem.CashItem?>(null) }
    var cashEditAmount by remember { mutableStateOf("") }
    var cashEditRemarks by remember { mutableStateOf("") }

    var showEditPartyDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }
    var editVillageInput by remember { mutableStateOf("") }
    var editPhoneInput by remember { mutableStateOf("") }
    var editGstinInput by remember { mutableStateOf("") }
    var editMandiLicenseInput by remember { mutableStateOf("") }
    var editDuplicateParty by remember { mutableStateOf<com.appwork.mandisamiti.domain.model.Party?>(null) }

    val openEditPartyDialog: () -> Unit = {
        if (party != null) {
            editNameInput = party.name
            editVillageInput = party.village ?: ""
            editPhoneInput = party.phone ?: ""
            editGstinInput = party.gstin ?: ""
            editMandiLicenseInput = party.mandiLicense ?: ""
            editDuplicateParty = null
            showEditPartyDialog = true
        }
    }

    var showSettlementDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is PartyLedgerEvent.TransactionRecorded) {
                showPlainCashSide = null
                plainCashAmountInput = ""
                plainCashRemarksInput = ""
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        topBar = {
            SoundboxTopBar(
                shopName = party?.name ?: (if (isEnglish) "Account Details" else "खाता विवरण"),
                mandiLocation = if (party?.village != null) (if (isEnglish) "Village: ${party.village}" else "गांव: ${party.village}") else (if (isEnglish) "Mandi account" else "मंडी खाता"),
                isSoundEnabled = uiState.isSoundEnabled,
                onToggleSound = viewModel::toggleSoundSetting,
                isEnglish = isEnglish,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEnglish) "Back" else "पीछे जाएं",
                            tint = MandiTextPrimary
                        )
                    }
                },
                actions = {
                    if (party != null) {
                        IconButton(
                            onClick = openEditPartyDialog,
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = if (isEnglish) "Edit account" else "खाता विवरण बदलें",
                                tint = MandiTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
        },
        containerColor = MandiBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (party?.phone == null) {
                item(key = "phone_needed") {
                    PhoneNeededBadge(
                        isEnglish = isEnglish,
                        onClick = if (party != null) openEditPartyDialog else null,
                        modifier = Modifier.padding(start = 14.dp, top = 10.dp),
                    )
                }
            }

            // Two balance cards side by side
            item(key = "balance_cards") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                // 1. Slip balance card (पर्चियों का बाकी)
                val slip = uiState.slipBalancePaisa
                val isSlipReceivable = slip > 0L
                val isSlipPayable = slip < 0L
                val absSlip = if (slip < 0L) -slip else slip
                val slipStatusText = when {
                    isSlipReceivable -> if (isEnglish) "To receive" else "लेना है"
                    isSlipPayable -> if (isEnglish) "To pay" else "देना है"
                    else -> if (isEnglish) "Settled" else "हिसाब चुकता"
                }
                val slipStatusColor = when {
                    isSlipReceivable -> MandiGreenReceivable
                    isSlipPayable -> MandiRedPayable
                    else -> MandiTextPrimary
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MandiSurface),
                    border = BorderStroke(1.dp, MandiBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Slip Balance" else "पर्चियों का बाकी",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MandiTextSecondary,
                            maxLines = 1
                        )
                        Text(
                            text = "₹${MandiMathEngine.paisaToRupeesString(absSlip)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = slipStatusColor,
                            maxLines = 1
                        )
                        Text(
                            text = slipStatusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = slipStatusColor
                        )
                    }
                }

                // 2. Udhaar balance card (उधार बाकी)
                val givenTotal = if (uiState.udhaarGivenTotalPaisa > 0L) uiState.udhaarGivenTotalPaisa else uiState.udhaarGivenPaisa
                val takenTotal = if (uiState.udhaarTakenTotalPaisa > 0L) uiState.udhaarTakenTotalPaisa else uiState.udhaarTakenPaisa
                val hasGiven = givenTotal > 0L
                val hasTaken = takenTotal > 0L
                val bothNonZero = hasGiven && hasTaken
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MandiSurface),
                    border = BorderStroke(1.dp, MandiBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Udhaar Balance" else "उधार बाकी",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MandiTextSecondary,
                            maxLines = 1
                        )
                        if (bothNonZero) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "${if (isEnglish) "Given: " else "दिया: "}₹${MandiMathEngine.paisaToRupeesString(givenTotal)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MandiGreenReceivable,
                                    maxLines = 1
                                )
                                if (uiState.udhaarGivenInterestPaisa > 0L) {
                                    Text(
                                        text = "+ ${if (isEnglish) "Interest: " else "ब्याज: "}₹${MandiMathEngine.paisaToRupeesString(uiState.udhaarGivenInterestPaisa)}",
                                        fontSize = 10.sp,
                                        color = MandiTextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "${if (isEnglish) "Taken: " else "लिया: "}₹${MandiMathEngine.paisaToRupeesString(takenTotal)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MandiRedPayable,
                                    maxLines = 1
                                )
                                if (uiState.udhaarTakenInterestPaisa > 0L) {
                                    Text(
                                        text = "+ ${if (isEnglish) "Interest: " else "ब्याज: "}₹${MandiMathEngine.paisaToRupeesString(uiState.udhaarTakenInterestPaisa)}",
                                        fontSize = 10.sp,
                                        color = MandiTextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        } else if (hasGiven) {
                            Text(
                                text = "₹${MandiMathEngine.paisaToRupeesString(givenTotal)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiGreenReceivable,
                                maxLines = 1
                            )
                            if (uiState.udhaarGivenInterestPaisa > 0L) {
                                Text(
                                    text = "+ ${if (isEnglish) "Interest: " else "ब्याज: "}₹${MandiMathEngine.paisaToRupeesString(uiState.udhaarGivenInterestPaisa)}",
                                    fontSize = 11.sp,
                                    color = MandiTextSecondary,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = if (isEnglish) "Given (To receive)" else "दिया (लेना है)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MandiGreenReceivable
                            )
                        } else if (hasTaken) {
                            Text(
                                text = "₹${MandiMathEngine.paisaToRupeesString(takenTotal)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiRedPayable,
                                maxLines = 1
                            )
                            if (uiState.udhaarTakenInterestPaisa > 0L) {
                                Text(
                                    text = "+ ${if (isEnglish) "Interest: " else "ब्याज: "}₹${MandiMathEngine.paisaToRupeesString(uiState.udhaarTakenInterestPaisa)}",
                                    fontSize = 11.sp,
                                    color = MandiTextSecondary,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = if (isEnglish) "Taken (To pay)" else "लिया (देना है)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MandiRedPayable
                            )
                        } else {
                            Text(
                                text = "₹0",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MandiTextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = if (isEnglish) "No udhaar" else "कोई उधार नहीं",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MandiTextMuted
                            )
                        }
                    }
                }
            }
            }

            // Tax details: one muted line, whole line opens the edit dialog
            if (party?.partyType == PartyType.BUYER || party?.gstin != null || party?.mandiLicense != null) {
                item(key = "tax_details") {
                    val notProvided = if (isEnglish) "Not provided" else "दर्ज नहीं"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = openEditPartyDialog)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GSTIN ${party?.gstin ?: notProvided}  ·  ${if (isEnglish) "Licence" else "लाइसेंस"} ${party?.mandiLicense ?: notProvided}",
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp,
                            color = MandiTextSecondary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isEnglish) "Edit" else "बदलें",
                            modifier = Modifier.padding(start = 8.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiPrimaryAction
                        )
                    }
                }
            }

            // Quick Action Buttons
            item(key = "quick_actions") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Primary Settle Action Button (हिसाब चुकता)
                    Button(
                        onClick = { showSettlementDialog = true },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MandiPrimaryAction,
                            contentColor = MandiPrimaryActionText
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MandiPrimaryActionText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = if (isEnglish) "Settle Slips (Hisab Chukta)" else "हिसाब चुकता (पर्ची)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiPrimaryActionText
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FintechActionButton(
                            label = if (isEnglish) "Money Received" else "रुपये मिले",
                            icon = Icons.Default.ArrowDownward,
                            color = MandiGreenReceivable,
                            backgroundColor = MandiGreenLight,
                            borderColor = MandiGreenBorder,
                            textColor = MandiGreenText,
                            onClick = {
                                viewModel.clearError()
                                showPlainCashSide = "GOT"
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FintechActionButton(
                            label = if (isEnglish) "Money Paid" else "रुपये दिए",
                            icon = Icons.Default.ArrowUpward,
                            color = MandiRedPayable,
                            backgroundColor = MandiRedLight,
                            borderColor = MandiRedBorder,
                            textColor = MandiRedText,
                            onClick = {
                                viewModel.clearError()
                                showPlainCashSide = "GAVE"
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Udhaar Section: shown only when the party has loans in either direction
            val hasUdhaar = uiState.udhaarGivenPaisa > 0L || uiState.udhaarTakenPaisa > 0L || uiState.openLoans.isNotEmpty()
            if (hasUdhaar) {
                item(key = "udhaar_section") {
                    PartyUdhaarSection(
                        uiState = uiState,
                        givenViewModel = givenPersonViewModel,
                        takenViewModel = takenPersonViewModel,
                        isEnglish = isEnglish,
                        onEditRepayment = { editingSettlement = it },
                        onShowRepaymentHistory = { historySettlement = it },
                        onRefresh = { viewModel.loadPartyData() }
                    )
                }
            }

            // Transaction Stream Header
            item(key = "tx_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = (if (isEnglish) "Transactions" else "लेन-देन विवरण") + " (${uiState.ledgerItems.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                }
            }

            // Transaction List
            items(uiState.ledgerItems) { item ->
                Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                    when (item) {
                        is LedgerItem.BuyItem -> {
                            FintechBuyCard(
                                isEnglish = isEnglish,
                                buy = item.buy,
                                onOpenReceipt = { onOpenBuyReceipt(item.buy.id) },
                                onLongPress = { voidTarget = item }
                            )
                        }
                        is LedgerItem.SellItem -> {
                            FintechSellCard(
                                isEnglish = isEnglish,
                                sell = item.sell,
                                onOpenReceipt = { onOpenSellReceipt(item.sell.id) },
                                onLongPress = { voidTarget = item }
                            )
                        }
                        is LedgerItem.LendItem -> {
                            val matchingLoan = uiState.loansInfoById[item.lend.id] ?: uiState.openLoans.find { it.lend.id == item.lend.id }
                            FintechLendCard(
                                isEnglish = isEnglish,
                                lend = item.lend,
                                loanInfo = matchingLoan,
                                tag = item.tag,
                                onEditRepayment = { editingSettlement = it },
                                onShowRepaymentHistory = { historySettlement = it },
                                onLongPress = { voidTarget = item }
                            )
                        }
                        is LedgerItem.DealItem -> {
                            FintechDealCard(
                                isEnglish = isEnglish,
                                deal = item.deal,
                                isBuyerLedger = party?.partyType == PartyType.BUYER,
                                tag = item.tag,
                                onShareSlip = { onShareWhatsAppReceipt(item.deal.id) },
                                onEditDeal = { onEditDeal(item.deal.id) },
                                onLongPress = { voidTarget = item }
                            )
                        }
                        is LedgerItem.CashItem -> {
                            FintechCashCard(
                                isEnglish = isEnglish,
                                transaction = item.transaction,
                                tag = item.tag,
                                slipNo = (item.tag as? com.appwork.mandisamiti.domain.ledger.LedgerTag.Slip)?.let { slipNoById[it.slipId] },
                                onLongPress = { voidTarget = item }
                            )
                        }
                    }
                }
            }

            if (uiState.ledgerItems.isEmpty()) {
                item(key = "empty_ledger") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEnglish) "No transactions in this account" else "इस खाते में कोई लेन-देन नहीं है",
                            color = MandiTextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    if (showEditPartyDialog && party != null) {
        val isBuyer = party.partyType == PartyType.BUYER
        val gstinClean = editGstinInput.trim().uppercase()
        val gstinValid = !isBuyer || gstinClean.isEmpty() || MandiValidators.validateGstin(gstinClean, false).isValid
        val phoneValid = isValidPartyPhone(editPhoneInput)

        AlertDialog(
            onDismissRequest = { showEditPartyDialog = false; editDuplicateParty = null },
            title = {
                Text(
                    text = if (isEnglish) "Edit Account Details" else "खाता विवरण बदलें",
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text(if (isEnglish) "Party Name *" else "नाम *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        value = editVillageInput,
                        onValueChange = { editVillageInput = it },
                        label = { Text(if (isEnglish) "Village / Location" else "गांव / पता (वैकल्पिक)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhoneInput,
                        onValueChange = {
                            editPhoneInput = sanitizePartyPhone(it)
                            editDuplicateParty = null
                        },
                        label = { Text(if (isEnglish) "Mobile Number *" else "मोबाइल नंबर *") },
                        isError = editPhoneInput.isNotEmpty() && !phoneValid,
                        supportingText = if (editPhoneInput.isNotEmpty() && !phoneValid) {
                            { Text(if (isEnglish) "Enter a 10-digit mobile number" else "10 अंकों का मोबाइल नंबर डालें (6–9 से शुरू)") }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isBuyer) {
                        OutlinedTextField(
                            value = editGstinInput,
                            onValueChange = { if (it.length <= 15) editGstinInput = it.uppercase() },
                            label = { Text(if (isEnglish) "GSTIN (15 chars, Optional)" else "GSTIN (15 अक्षर, वैकल्पिक)") },
                            isError = !gstinValid,
                            supportingText = if (!gstinValid) {
                                { Text(if (isEnglish) "15-character GSTIN required" else "GSTIN 15 अक्षरों का होना चाहिए") }
                            } else null,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrect = false, keyboardType = KeyboardType.Ascii),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrect = false, keyboardType = KeyboardType.Ascii),
                            value = editMandiLicenseInput,
                            onValueChange = { if (it.length <= 50) editMandiLicenseInput = it },
                            label = { Text(if (isEnglish) "Mandi Licence No. (Optional)" else "मंडी लाइसेंस नंबर (वैकल्पिक)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    editDuplicateParty?.let { existing ->
                        Text(
                            text = if (isEnglish) {
                                "This number already belongs to ${existing.name}${existing.village?.let { ", $it" } ?: ""}"
                            } else {
                                "यह नंबर पहले से ${existing.name}${existing.village?.let { ", $it" } ?: ""} के नाम पर है"
                            },
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNameInput.trim().isNotBlank() && phoneValid && gstinValid) {
                            viewModel.updatePartyDetails(
                                name = editNameInput,
                                village = editVillageInput,
                                phone = editPhoneInput,
                                gstin = if (isBuyer) gstinClean.ifBlank { null } else null,
                                mandiLicense = if (isBuyer) editMandiLicenseInput.trim().ifBlank { null } else null,
                                onSuccess = {
                                    editDuplicateParty = null
                                    showEditPartyDialog = false
                                },
                                onDuplicatePhone = { existing ->
                                    editDuplicateParty = existing
                                }
                            )
                        }
                    },
                    enabled = editNameInput.trim().isNotBlank() && phoneValid && gstinValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MandiPrimaryAction,
                        contentColor = MandiPrimaryActionText
                    )
                ) {
                    Text(
                        text = if (isEnglish) "Save" else "सहेजें",
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPartyDialog = false; editDuplicateParty = null }) {
                    Text(if (isEnglish) "Cancel" else "रद्द करें", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    voidTarget?.let { target ->
        val cutPaisa = remember(target, uiState.ledgerItems) {
            if (target is LedgerItem.BuyItem) {
                val buy = target.buy
                val buyIdTag = "[BUYCUT:${buy.id}]"
                val slipNo = buy.slipNo
                uiState.ledgerItems
                    .filterIsInstance<LedgerItem.CashItem>()
                    .map { it.transaction }
                    .filter { tx ->
                        !tx.isVoid &&
                        tx.paymentMode == PaymentMode.BOOK_ENTRY &&
                        (tx.remarks?.contains(buyIdTag) == true ||
                            (slipNo != null && tx.remarks?.contains("पर्ची #$slipNo से कटौती") == true))
                    }
                    .sumOf { it.amountPaisa }
            } else {
                0L
            }
        }
        AlertDialog(
            onDismissRequest = { voidTarget = null },
            title = { Text(if (isEnglish) "Cancel this entry?" else "प्रविष्टि रद्द करें?", fontWeight = FontWeight.Bold, color = MandiTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isEnglish) "The cancelled entry stays visible in the account but is removed from the totals." else "रद्द प्रविष्टि खाते में दिखेगी, पर हिसाब से हट जाएगी।", color = MandiTextSecondary, fontSize = 14.sp)
                    if (cutPaisa > 0L) {
                        val formattedCut = MandiMathEngine.formatRupees(cutPaisa)
                        Text(
                            text = if (isEnglish) "Loan deduction of $formattedCut will also be reversed." else "इस पर्ची की उधार कटौती ($formattedCut) भी वापस हो जाएगी।",
                            color = MandiOrangeLend,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                    val remarksTag = (target as? LedgerItem.CashItem)?.transaction?.remarks.orEmpty()
                    val isLinkedCash = remarksTag.contains("[LEND:") || remarksTag.contains("[EXPENSE:") || remarksTag.contains("[SETTLE:")
                    if (target is LedgerItem.CashItem && !isLinkedCash) {
                        Button(
                            onClick = {
                                cashEditAmount = (target.transaction.amountPaisa / 100L).toString()
                                cashEditRemarks = target.transaction.remarks.orEmpty()
                                cashEditTarget = target
                                voidTarget = null
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) { Text(if (isEnglish) "Edit amount / note" else "रकम / नोट बदलें", fontSize = 16.sp) }
                    }
                    if (target is LedgerItem.LendItem) {
                        Button(
                            onClick = {
                                cashEditAmount = (target.lend.amountPaisa / 100L).toString()
                                cashEditRemarks = target.lend.notes.orEmpty()
                                lendEditTarget = target
                                voidTarget = null
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) { Text(if (isEnglish) "Edit amount / note" else "रकम / नोट बदलें", fontSize = 16.sp) }
                    }
                    val filteredReasons = if (target is LedgerItem.CashItem) {
                        VoidReason.entries.filter { it.appliesToCash }
                    } else {
                        VoidReason.entries.filter { it.appliesToDeal }
                    }
                    filteredReasons.forEach { reason ->
                        OutlinedButton(
                            onClick = { viewModel.voidEntry(target, reason); voidTarget = null },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) { Text(if (isEnglish) reason.labelEn else reason.labelHi, color = MandiTextPrimary, fontSize = 16.sp) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { voidTarget = null }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (isEnglish) "Back" else "वापस", color = MandiTextSecondary)
                }
            }
        )
    }

    lendEditTarget?.let { target ->
        val isMoney = target.lend.lendType == com.appwork.mandisamiti.domain.model.LendType.MONEY
        val newAmount = cashEditAmount.toLongOrNull() ?: 0L
        AlertDialog(
            onDismissRequest = { lendEditTarget = null },
            title = { Text(if (isEnglish) "Edit loan" else "उधार सुधारें", fontWeight = FontWeight.Bold, color = MandiTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isMoney) {
                        OutlinedTextField(
                            value = cashEditAmount,
                            onValueChange = { cashEditAmount = it.filter(Char::isDigit) },
                            label = { Text(if (isEnglish) "Amount (Rs)" else "रकम (Rs)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        value = cashEditRemarks,
                        onValueChange = { cashEditRemarks = it },
                        label = { Text(if (isEnglish) "Note" else "नोट") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isMoney || newAmount > 0,
                    onClick = {
                        viewModel.editLend(target, if (isMoney) newAmount * 100L else target.lend.amountPaisa, cashEditRemarks.trim().ifEmpty { null })
                        lendEditTarget = null
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(if (isEnglish) "Save" else "सेव करें") }
            },
            dismissButton = {
                TextButton(onClick = { lendEditTarget = null }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (isEnglish) "Back" else "वापस", color = MandiTextSecondary)
                }
            }
        )
    }

    cashEditTarget?.let { target ->
        val newAmount = cashEditAmount.toLongOrNull() ?: 0L
        AlertDialog(
            onDismissRequest = { cashEditTarget = null },
            title = { Text(if (isEnglish) "Edit entry" else "प्रविष्टि सुधारें", fontWeight = FontWeight.Bold, color = MandiTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cashEditAmount,
                        onValueChange = { cashEditAmount = it.filter(Char::isDigit) },
                        label = { Text(if (isEnglish) "Amount (Rs)" else "रकम (Rs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        value = cashEditRemarks,
                        onValueChange = { cashEditRemarks = it },
                        label = { Text(if (isEnglish) "Note" else "नोट") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = newAmount > 0,
                    onClick = {
                        viewModel.editCash(target, newAmount * 100L, cashEditRemarks.trim().ifEmpty { null })
                        cashEditTarget = null
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(if (isEnglish) "Save" else "सेव करें") }
            },
            dismissButton = {
                TextButton(onClick = { cashEditTarget = null }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (isEnglish) "Back" else "वापस", color = MandiTextSecondary)
                }
            }
        )
    }

    // Plain Cash Entry Dialog (Routed through UdhaarUseCase.plainCash)
    if (showPlainCashSide != null) {
        val isGot = showPlainCashSide == "GOT"
        AlertDialog(
            onDismissRequest = {
                showPlainCashSide = null
                plainCashAmountInput = ""
                plainCashRemarksInput = ""
                viewModel.clearError()
            },
            title = {
                Text(
                    text = if (isGot) (if (isEnglish) "Money Received" else "रुपये मिले") else (if (isEnglish) "Money Paid" else "रुपये दिए"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MandiTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        value = plainCashAmountInput,
                        onValueChange = {
                            plainCashAmountInput = it.filter(Char::isDigit)
                            viewModel.clearError()
                        },
                        label = { Text(if (isEnglish) "Amount (₹)" else "राशि (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        value = plainCashRemarksInput,
                        onValueChange = { plainCashRemarksInput = it },
                        label = { Text(if (isEnglish) "Note (optional)" else "विवरण / नोट (वैकल्पिक)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (uiState.error != null) {
                        Text(
                            text = uiState.error ?: "",
                            color = MandiRedText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                val amount = plainCashAmountInput.toLongOrNull() ?: 0L
                Button(
                    onClick = {
                        if (amount > 0L && !uiState.isSaving) {
                            val side = if (isGot) CashSide.GOT else CashSide.GAVE
                            viewModel.onPlainCash(
                                side = side,
                                amountPaisa = amount * 100L,
                                note = plainCashRemarksInput.ifBlank { null }
                            )
                        }
                    },
                    enabled = amount > 0L && !uiState.isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGot) MandiBtnSuccessBg else MandiBtnDangerBg,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isEnglish) "Save" else "सुरक्षित करें", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPlainCashSide = null
                    plainCashAmountInput = ""
                    plainCashRemarksInput = ""
                    viewModel.clearError()
                }) {
                    Text(if (isEnglish) "Cancel" else "रद्द करें", color = MandiTextSecondary)
                }
            }
        )
    }

    // Settlement Modal (हिसाब चुकता - Slips only)
    if (showSettlementDialog) {
        val nonVoidBuys = uiState.ledgerItems.filterIsInstance<LedgerItem.BuyItem>()
            .map { it.buy }
            .filter { !it.isVoid && it.paidPaisa < it.netPayablePaisa }
        val nonVoidSells = uiState.ledgerItems.filterIsInstance<LedgerItem.SellItem>()
            .map { it.sell }
            .filter { !it.isVoid && it.balanceDuePaisa > 0 }

        var selectedTargetType by remember {
            mutableStateOf(
                when {
                    nonVoidBuys.isNotEmpty() -> "BUY"
                    nonVoidSells.isNotEmpty() -> "SELL"
                    else -> "NONE"
                }
            )
        }
        var selectedBuyId by remember { mutableStateOf(nonVoidBuys.firstOrNull()?.id) }
        var selectedSellId by remember { mutableStateOf(nonVoidSells.firstOrNull()?.id) }
        var isFullSettle by remember { mutableStateOf(true) }
        var partialAmountInput by remember { mutableStateOf("") }
        var settlementNote by remember { mutableStateOf("") }

        val activeTargetMaxPaisa = when (selectedTargetType) {
            "BUY" -> nonVoidBuys.find { it.id == selectedBuyId }?.let { it.netPayablePaisa - it.paidPaisa } ?: 0L
            "SELL" -> nonVoidSells.find { it.id == selectedSellId }?.balanceDuePaisa ?: 0L
            else -> 0L
        }

        val settlementAmountPaisa = if (isFullSettle) {
            activeTargetMaxPaisa
        } else {
            (partialAmountInput.toLongOrNull() ?: 0L) * 100L
        }

        AlertDialog(
            onDismissRequest = { showSettlementDialog = false },
            title = {
                Text(
                    text = if (isEnglish) "Settle Account (Hisab Chukta)" else "हिसाब चुकता",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MandiTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (nonVoidBuys.isEmpty() && nonVoidSells.isEmpty()) {
                        Text(
                            text = if (isEnglish) "No pending slips to settle for this party." else "इस खातेदार की कोई बकाया पर्ची नहीं है।",
                            color = MandiTextSecondary,
                            fontSize = 14.sp
                        )
                    } else {
                        Text(
                            text = if (isEnglish) "Choose slip to settle:" else "चुकता करने के लिए पर्ची चुनें:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MandiTextPrimary
                        )

                        // Target Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // 1. Buy Slips
                            nonVoidBuys.forEach { buy ->
                                val due = buy.netPayablePaisa - buy.paidPaisa
                                val isSelected = selectedTargetType == "BUY" && selectedBuyId == buy.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MandiPrimaryAction else MandiBorder,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .background(if (isSelected) MandiSurfaceElevated else MandiSurface)
                                        .clickable {
                                            selectedTargetType = "BUY"
                                            selectedBuyId = buy.id
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isEnglish) "Purchase Slip #${slipLabel(buy.slipNo, buy.id)}" else "खरीद पर्ची #${slipLabel(buy.slipNo, buy.id)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MandiTextPrimary
                                        )
                                        Text(
                                            text = if (isEnglish) "Pending payment (We pay)" else "बाकी, हमें देना है",
                                            fontSize = 11.sp,
                                            color = MandiTextSecondary
                                        )
                                    }
                                    Text(
                                        text = "₹${MandiMathEngine.paisaToRupeesString(due)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiRedPayable
                                    )
                                }
                            }

                            // 2. Sell Slips
                            nonVoidSells.forEach { sell ->
                                val due = sell.balanceDuePaisa
                                val isSelected = selectedTargetType == "SELL" && selectedSellId == sell.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MandiPrimaryAction else MandiBorder,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .background(if (isSelected) MandiSurfaceElevated else MandiSurface)
                                        .clickable {
                                            selectedTargetType = "SELL"
                                            selectedSellId = sell.id
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isEnglish) "Sale Slip #${slipLabel(sell.slipNo, sell.id)}" else "बिक्री पर्ची #${slipLabel(sell.slipNo, sell.id)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MandiTextPrimary
                                        )
                                        Text(
                                            text = if (isEnglish) "Due from buyer (We receive)" else "व्यापारी से बाकी (लेना है)",
                                            fontSize = 11.sp,
                                            color = MandiTextSecondary
                                        )
                                    }
                                    Text(
                                        text = "₹${MandiMathEngine.paisaToRupeesString(due)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiGreenReceivable
                                    )
                                }
                            }
                        }

                        // Full vs Partial Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { isFullSettle = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFullSettle) MandiPrimaryAction else MandiSurfaceElevated,
                                    contentColor = if (isFullSettle) MandiPrimaryActionText else MandiTextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isEnglish) "Full" else "पूरा चुकता", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Button(
                                onClick = { isFullSettle = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isFullSettle) MandiPrimaryAction else MandiSurfaceElevated,
                                    contentColor = if (!isFullSettle) MandiPrimaryActionText else MandiTextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isEnglish) "Partial" else "आंशिक", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        if (!isFullSettle) {
                            val overMax = settlementAmountPaisa > activeTargetMaxPaisa
                            OutlinedTextField(
                                value = partialAmountInput,
                                onValueChange = { partialAmountInput = it.filter(Char::isDigit) },
                                label = { Text(if (isEnglish) "Amount (₹)" else "राशि (₹)") },
                                isError = overMax,
                                supportingText = if (overMax) {
                                    {
                                        val maxStr = MandiMathEngine.paisaToRupeesString(activeTargetMaxPaisa)
                                        Text(if (isEnglish) "Cannot exceed remaining ₹$maxStr" else "राशि बाकी ₹$maxStr से ज़्यादा नहीं हो सकती")
                                    }
                                } else null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            value = settlementNote,
                            onValueChange = { settlementNote = it },
                            label = { Text(if (isEnglish) "Note (optional)" else "विवरण / नोट (वैकल्पिक)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Cash Impact Strip
                        val cashDirectionText = when (selectedTargetType) {
                            "BUY" -> if (isEnglish) "Cash goes OUT of Galla (paid to farmer)" else "गल्ले से नकद निकलेगा (किसान को भुगतान)"
                            "SELL" -> if (isEnglish) "Cash comes INTO Galla (received from buyer)" else "गल्ले में नकद आएगा (व्यापारी से वसूली)"
                            else -> ""
                        }
                        if (settlementAmountPaisa > 0L) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MandiNeutralLight)
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "₹${MandiMathEngine.paisaToRupeesString(settlementAmountPaisa)} - $cashDirectionText",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MandiTextPrimary
                                )
                                if (selectedTargetType == "BUY") {
                                    Text(
                                        text = if (isEnglish) "Slip updates with revised payment status and revised mark." else "पर्ची संशोधित मार्क और अद्यतन भुगतान के साथ अपडेट होगी।",
                                        fontSize = 11.sp,
                                        color = MandiTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val canSave = settlementAmountPaisa > 0L && settlementAmountPaisa <= activeTargetMaxPaisa
                Button(
                    onClick = {
                        when (selectedTargetType) {
                            "BUY" -> {
                                selectedBuyId?.let { buyId ->
                                    viewModel.settleBuySlip(
                                        buyId = buyId,
                                        amountPaisa = settlementAmountPaisa,
                                        note = settlementNote.ifBlank { null }
                                    )
                                }
                            }
                            "SELL" -> {
                                selectedSellId?.let { sellId ->
                                    viewModel.settleSellSlip(
                                        sellId = sellId,
                                        amountPaisa = settlementAmountPaisa,
                                        note = settlementNote.ifBlank { null }
                                    )
                                }
                            }
                        }
                        showSettlementDialog = false
                    },
                    enabled = canSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MandiPrimaryAction,
                        contentColor = MandiPrimaryActionText
                    )
                ) {
                    Text(if (isEnglish) "Confirm Settlement" else "सुरक्षित करें", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettlementDialog = false }) {
                    Text(if (isEnglish) "Cancel" else "रद्द करें", color = MandiTextSecondary)
                }
            }
        )
    }

    editingSettlement?.let { s ->
        // The loan's own rate; settled loans are not in openLoans but can still be edited.
        val rate = uiState.loansInfoById[s.lendId]?.monthlyRatePercent
            ?: RuralInterestEngine.DEFAULT_MONTHLY_RATE_PERCENT
        EditRepaymentDialog(
            settlement = s,
            isEnglish = isEnglish,
            preview = uiState.repaymentEditPreview,
            errorCode = uiState.repaymentEditError,
            onPreview = { amount, at -> viewModel.previewRepaymentEdit(s.lendId, s.id, amount, at, rate) },
            onConfirm = { amount, at ->
                viewModel.editRepayment(s.id, amount, at, rate)
                editingSettlement = null
            },
            onDismiss = { viewModel.clearRepaymentEdit(); editingSettlement = null },
        )
    }

    historySettlement?.let { s ->
        LaunchedEffect(s.id) { viewModel.loadRepaymentHistory(s.id) }
        RepaymentHistoryDialog(
            history = uiState.repaymentHistory,
            isEnglish = isEnglish,
            onDismiss = { viewModel.clearRepaymentEdit(); historySettlement = null },
        )
    }
}

@Composable
private fun FintechActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MandiSurface,
    borderColor: Color = MandiBorder,
    textColor: Color = color
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun LedgerTagChip(
    tag: com.appwork.mandisamiti.domain.ledger.LedgerTag?,
    slipNo: Long?,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    if (tag == null) return
    val chipText = when (tag) {
        is com.appwork.mandisamiti.domain.ledger.LedgerTag.Slip ->
            if (slipNo != null) (if (isEnglish) "Slip #$slipNo" else "पर्ची #$slipNo") else (if (isEnglish) "Slip" else "पर्ची")
        com.appwork.mandisamiti.domain.ledger.LedgerTag.Udhaar -> if (isEnglish) "Udhaar" else "उधार"
    }
    Text(
        text = chipText,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MandiTextMuted,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, MandiBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun PartyUdhaarSection(
    uiState: PartyLedgerUiState,
    givenViewModel: UdhaarPersonViewModel?,
    takenViewModel: UdhaarPersonViewModel?,
    isEnglish: Boolean,
    modifier: Modifier = Modifier,
    onEditRepayment: (com.appwork.mandisamiti.domain.model.LendSettlement) -> Unit = {},
    onShowRepaymentHistory: (com.appwork.mandisamiti.domain.model.LendSettlement) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    if (givenViewModel == null || takenViewModel == null) return
    val givenState by givenViewModel.uiState.collectAsState()
    val takenState by takenViewModel.uiState.collectAsState()

    val totalGivenLoans = givenState?.loans?.size ?: 0
    val totalTakenLoans = takenState?.loans?.size ?: 0
    if (totalGivenLoans == 0 && totalTakenLoans == 0) return

    val initialPage = if (totalGivenLoans == 0 && totalTakenLoans > 0) 1 else 0
    val pagerState = rememberPagerState(initialPage = initialPage) { 2 }
    val coroutineScope = rememberCoroutineScope()
    var showRepayForDirection by rememberSaveable { mutableStateOf<LendDirection?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        border = BorderStroke(1.dp, MandiBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tab Row: दिया (Given) / लिया (Taken)
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MandiSurface,
                contentColor = MandiPrimaryAction,
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = MandiPrimaryAction
                        )
                    }
                },
                divider = {},
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MandiBorder, RoundedCornerShape(8.dp))
            ) {
                val tabs = listOf(
                    if (isEnglish) "Given ($totalGivenLoans)" else "दिया ($totalGivenLoans)",
                    if (isEnglish) "Taken ($totalTakenLoans)" else "लिया ($totalTakenLoans)"
                )
                tabs.forEachIndexed { index, title ->
                    val isSelected = pagerState.currentPage == index
                    Tab(
                        selected = isSelected,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MandiTextPrimary else MandiTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val isGiven = page == 0
                val personState = if (isGiven) givenState else takenState
                val activeVm = if (isGiven) givenViewModel else takenViewModel

                if (personState == null) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MandiPrimaryAction)
                    }
                } else if (personState.loans.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isGiven) {
                                if (isEnglish) "No loans given" else "कोई उधार दिया नहीं है"
                            } else {
                                if (isEnglish) "No loans taken" else "कोई उधार लिया नहीं है"
                            },
                            fontSize = 13.sp,
                            color = MandiTextMuted
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header with Due and Repay Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "${if (isEnglish) "Due: " else "बाकी: "}₹${MandiMathEngine.paisaToRupeesString(personState.totalDuePaisa)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGiven) MandiGreenReceivable else MandiRedPayable
                                )
                                if (personState.interestDuePaisa > 0L) {
                                    Text(
                                        text = "+ ${if (isEnglish) "Interest: " else "ब्याज: "}₹${MandiMathEngine.paisaToRupeesString(personState.interestDuePaisa)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MandiAmberDark
                                    )
                                }
                            }

                            if (!personState.isSettled) {
                                Button(
                                    onClick = { showRepayForDirection = personState.direction },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MandiPrimaryAction,
                                        contentColor = MandiPrimaryActionText
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    val btnLabel = if (personState.direction == LendDirection.GIVEN) {
                                        if (isEnglish) "Repay (Got)" else "पैसे मिले"
                                    } else {
                                        if (isEnglish) "Repay (Paid)" else "पैसे दिए"
                                    }
                                    Text(btnLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MandiPrimaryActionText)
                                }
                            }
                        }

                        // Loans list
                        personState.loans.forEach { loanItem ->
                            LoanCardItem(
                                item = loanItem,
                                isEnglish = isEnglish,
                                isGiven = isGiven,
                                onEditRepayment = onEditRepayment,
                                onViewHistory = { settlementId ->
                                    loanItem.settlements.firstOrNull { it.id == settlementId }?.let(onShowRepaymentHistory)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Repay dialog triggered from udhaar section
    if (showRepayForDirection != null) {
        val isGiven = showRepayForDirection == LendDirection.GIVEN
        val targetState = if (isGiven) givenState else takenState
        val targetVm = if (isGiven) givenViewModel else takenViewModel
        if (targetState != null) {
            UdhaarRepayDialog(
                uiState = targetState,
                isEnglish = isEnglish,
                viewModel = targetVm,
                onDismiss = { showRepayForDirection = null },
                onSaved = {
                    showRepayForDirection = null
                    onRefresh()
                }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FintechDealCard(
    isEnglish: Boolean,
    deal: com.appwork.mandisamiti.domain.model.Deal,
    isBuyerLedger: Boolean = false,
    tag: com.appwork.mandisamiti.domain.ledger.LedgerTag? = null,
    onShareSlip: () -> Unit,
    onEditDeal: () -> Unit = {},
    onLongPress: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val isVoid = deal.isVoid
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(onClick = { if (!isVoid) onEditDeal() }, onLongClick = { if (!isVoid) onLongPress() })
            .background(MandiSurface)
            .border(1.dp, MandiBorder, shape)
            .padding(12.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(
            modifier = Modifier.alpha(if (isVoid) 0.55f else 1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MandiAmberPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isEnglish) "Deal slip #${deal.id.takeLast(5)}" else "सौदा पर्ची #${deal.id.takeLast(5)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                    LedgerTagChip(tag = tag, slipNo = null, isEnglish = isEnglish)
                    if (deal.syncStatus == 1) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Synced",
                            tint = MandiGreenReceivable,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Pending Local",
                            tint = MandiTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (!isVoid) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onShareSlip,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = if (isEnglish) "Share slip" else "पर्ची शेयर करें",
                                tint = MandiAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onLongPress,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = if (isEnglish) "Options" else "विकल्प",
                                tint = MandiTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isEnglish) "${deal.bagsCount} bags • ${MandiMathEngine.gramsToQuintals(deal.netWeightGrams)} Qtl" else "${deal.bagsCount} बोरी • ${MandiMathEngine.gramsToQuintals(deal.netWeightGrams)} Qtl",
                    fontSize = 13.sp,
                    color = MandiTextSecondary
                )
                if (isBuyerLedger) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isEnglish) "Due from buyer" else "व्यापारी से वसूली",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MandiTextSecondary
                        )
                        Text(
                            text = "₹${MandiMathEngine.paisaToRupeesString(deal.netBuyerReceivablePaisa)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary,
                            textDecoration = if (isVoid) TextDecoration.LineThrough else null
                        )
                    }
                } else {
                    // A negative payable means the farmer owes the shop: say so instead of printing "₹-…".
                    val farmerOwesShop = deal.netFarmerPayablePaisa < 0
                    Column(horizontalAlignment = Alignment.End) {
                        if (farmerOwesShop) {
                            Text(
                                text = if (isEnglish) "To receive from farmer" else "किसान से लेना है",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MandiGreenReceivable
                            )
                        }
                        Text(
                            text = "₹${MandiMathEngine.paisaToRupeesString(kotlin.math.abs(deal.netFarmerPayablePaisa))}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (farmerOwesShop) MandiGreenReceivable else MandiAmberDark,
                            textDecoration = if (isVoid) TextDecoration.LineThrough else null
                        )
                    }
                }
            }
        }

            if (isVoid) {
                Text(
                    text = if (isEnglish) "Cancelled · ${deal.voidReason?.labelEn ?: ""}" else "रद्द · ${deal.voidReason?.labelHi ?: ""}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MandiTextSecondary
                )
            }
      }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FintechCashCard(
    isEnglish: Boolean,
    transaction: com.appwork.mandisamiti.domain.model.CashTransaction,
    tag: com.appwork.mandisamiti.domain.ledger.LedgerTag?,
    slipNo: Long?,
    onLongPress: () -> Unit
) {
    val isVoid = transaction.isVoid
    val isDeposit = transaction.transactionType == TransactionType.JAMA_RECEIVED ||
        transaction.transactionType == TransactionType.INTEREST_RECEIVED
    val shape = RoundedCornerShape(10.dp)
    val remarks = com.appwork.mandisamiti.domain.util.RemarksFormatter.cleanForDisplay(transaction.remarks)
    val titleText = when (transaction.transactionType) {
        TransactionType.JAMA_RECEIVED -> when (transaction.paymentMode) {
            PaymentMode.BOOK_ENTRY -> if (isEnglish) "Book credit" else "बही जमा (क्रेडिट)"
            PaymentMode.ONLINE, PaymentMode.UPI, PaymentMode.RTGS, PaymentMode.NEFT, PaymentMode.BANK ->
                if (isEnglish) "Online / bank received" else "ऑनलाइन / बैंक जमा"
            PaymentMode.CHEQUE -> if (isEnglish) "Cheque received" else "चेक जमा प्राप्त"
            PaymentMode.CASH -> if (isEnglish) "Cash received" else "नकद जमा प्राप्त"
        }
        TransactionType.UDHAR_GIVEN -> when (transaction.paymentMode) {
            PaymentMode.BOOK_ENTRY -> if (isEnglish) "Book debit" else "बही भुगतान (डेबिट)"
            PaymentMode.ONLINE, PaymentMode.UPI, PaymentMode.RTGS, PaymentMode.NEFT, PaymentMode.BANK ->
                if (isEnglish) "Online / bank paid" else "ऑनलाइन / बैंक भुगतान"
            PaymentMode.CHEQUE -> if (isEnglish) "Cheque paid" else "चेक भुगतान दिया"
            PaymentMode.CASH -> if (isEnglish) "Cash paid" else "नकद भुगतान दिया"
        }
        TransactionType.INTEREST_RECEIVED -> if (isEnglish) "Interest received" else "ब्याज मिला"
        TransactionType.INTEREST_ADDED -> if (isEnglish) "Interest added" else "ब्याज जोड़ा गया"
        TransactionType.DISCOUNT_GIVEN -> if (isEnglish) "Discount / settlement" else "छूट / समझौता"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(onClick = {}, onLongClick = { if (!isVoid) onLongPress() })
            .background(MandiSurface)
            .border(1.dp, MandiBorder, shape)
            .padding(12.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().alpha(if (isVoid) 0.55f else 1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isDeposit) MandiGreenLight else MandiRedLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDeposit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isDeposit) MandiGreenReceivable else MandiRedPayable,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = titleText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                        if (transaction.syncStatus != 1) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Pending Local",
                                tint = MandiTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        LedgerTagChip(tag = tag, slipNo = slipNo, isEnglish = isEnglish)
                    }
                    if (!remarks.isNullOrBlank()) {
                        Text(
                            text = remarks,
                            fontSize = 12.sp,
                            color = MandiTextSecondary
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (isDeposit) "+" else "-"}₹${MandiMathEngine.paisaToRupeesString(transaction.amountPaisa)}",
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 8.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDeposit) MandiGreenReceivable else MandiRedPayable,
                    textDecoration = if (isVoid) TextDecoration.LineThrough else null
                )
                if (!isVoid) {
                    IconButton(
                        onClick = onLongPress,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = if (isEnglish) "Options" else "विकल्प",
                            tint = MandiTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

            if (isVoid) {
                Text(
                    text = if (isEnglish) "Cancelled · ${transaction.voidReason?.labelEn ?: ""}" else "रद्द · ${transaction.voidReason?.labelHi ?: ""}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MandiTextSecondary
                )
            }
      }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FintechBuyCard(
    isEnglish: Boolean,
    buy: com.appwork.mandisamiti.domain.model.BuyEntry,
    onOpenReceipt: () -> Unit,
    onLongPress: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val isVoid = buy.isVoid
    val cropName = buy.commodityId.ifBlank { if (isEnglish) "Crop" else "फसल" }
    val bagsText = if (isEnglish) "${buy.bagsCount} bags" else "${buy.bagsCount} बोरी"
    val weightText = "${MandiMathEngine.gramsToQuintals(buy.totalWeightGrams)} ${if (isEnglish) "Qtl" else "क्विंटल"}"
    val rateText = "₹${MandiMathEngine.paisaToRupeesString(buy.ratePaisaPerQuintal)}/${if (isEnglish) "Qtl" else "क्विंटल"}"
    val netRupees = MandiMathEngine.paisaToRupeesString(buy.netPayablePaisa)
    val balanceDuePaisa = (buy.netPayablePaisa - buy.paidPaisa).coerceAtLeast(0L)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                onClick = { if (!isVoid) onOpenReceipt() },
                onLongClick = { if (!isVoid) onLongPress() }
            )
            .background(MandiSurface)
            .border(1.dp, MandiBorder, shape)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.alpha(if (isVoid) 0.55f else 1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MandiAmberPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isEnglish) "Purchase slip #${slipLabel(buy.slipNo, buy.id)}" else "खरीद पर्ची #${slipLabel(buy.slipNo, buy.id)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                    if (buy.revision > 1) {
                        Text(
                            text = if (isEnglish) "Revised" else "संशोधित",
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹$netRupees",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary,
                        textDecoration = if (isVoid) TextDecoration.LineThrough else null
                    )
                    if (!isVoid) {
                        IconButton(
                            onClick = onLongPress,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = if (isEnglish) "Options" else "विकल्प",
                                tint = MandiTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "$cropName • $bagsText • $weightText (@ $rateText)",
                fontSize = 12.sp,
                color = MandiTextSecondary
            )

            // Breakdown Tile (Paid & Left)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MandiSurfaceElevated)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEnglish) "Paid" else "भुगतान हुआ",
                        fontSize = 12.sp,
                        color = MandiTextSecondary
                    )
                    Text(
                        text = "₹${MandiMathEngine.paisaToRupeesString(buy.paidPaisa)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MandiTextPrimary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEnglish) "Left (To pay)" else "बाकी (देना है)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (balanceDuePaisa > 0) MandiRedPayable else MandiGreenReceivable
                    )
                    Text(
                        text = "₹${MandiMathEngine.paisaToRupeesString(balanceDuePaisa)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (balanceDuePaisa > 0) MandiRedPayable else MandiGreenReceivable
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${if (buy.revision > 1) (if (isEnglish) "Revised · " else "संशोधित · ") else ""}${formatEpochDate(buy.entryDate)}",
                    fontSize = 11.sp,
                    color = MandiTextMuted
                )
                Text(
                    text = if (isEnglish) "View slip →" else "पर्ची देखें →",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MandiPrimaryAction
                )
            }

            if (isVoid) {
                Text(
                    text = if (isEnglish) "Cancelled · ${formatVoidReason(buy.voidReason, isEnglish)}" else "रद्द · ${formatVoidReason(buy.voidReason, false)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MandiTextSecondary
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FintechSellCard(
    isEnglish: Boolean,
    sell: com.appwork.mandisamiti.domain.model.SellEntry,
    onOpenReceipt: () -> Unit,
    onLongPress: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val isVoid = sell.isVoid
    val cropName = sell.commodityId.ifBlank { if (isEnglish) "Crop" else "फसल" }
    val bagsText = if (isEnglish) "${sell.bagsCount} bags" else "${sell.bagsCount} बोरी"
    val weightText = "${MandiMathEngine.gramsToQuintals(sell.totalWeightGrams)} ${if (isEnglish) "Qtl" else "क्विंटल"}"
    val rateText = "₹${MandiMathEngine.paisaToRupeesString(sell.ratePaisaPerQuintal)}/${if (isEnglish) "Qtl" else "क्विंटल"}"
    val grossRupees = MandiMathEngine.paisaToRupeesString(sell.totalReceivablePaisa)
    val balanceDuePaisa = sell.balanceDuePaisa

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                onClick = { if (!isVoid) onOpenReceipt() },
                onLongClick = { if (!isVoid) onLongPress() }
            )
            .background(MandiSurface)
            .border(1.dp, MandiBorder, shape)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.alpha(if (isVoid) 0.55f else 1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MandiGreenReceivable,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isEnglish) "Sale slip #${slipLabel(sell.slipNo, sell.id)}" else "बिक्री पर्ची #${slipLabel(sell.slipNo, sell.id)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                    if (sell.revision > 1) {
                        Text(
                            text = if (isEnglish) "Revised" else "संशोधित",
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹$grossRupees",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary,
                        textDecoration = if (isVoid) TextDecoration.LineThrough else null
                    )
                    if (!isVoid) {
                        IconButton(
                            onClick = onLongPress,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = if (isEnglish) "Options" else "विकल्प",
                                tint = MandiTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "$cropName • $bagsText • $weightText (@ $rateText)",
                fontSize = 12.sp,
                color = MandiTextSecondary
            )

            // Breakdown Tile (Received & Left)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MandiSurfaceElevated)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEnglish) "Received" else "वसूली हुई",
                        fontSize = 12.sp,
                        color = MandiTextSecondary
                    )
                    Text(
                        text = "₹${MandiMathEngine.paisaToRupeesString(sell.receivedPaisa)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MandiTextPrimary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEnglish) "Left (To receive)" else "बाकी (लेना है)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (balanceDuePaisa > 0) MandiGreenReceivable else MandiTextSecondary
                    )
                    Text(
                        text = "₹${MandiMathEngine.paisaToRupeesString(balanceDuePaisa)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (balanceDuePaisa > 0) MandiGreenReceivable else MandiTextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${if (sell.revision > 1) (if (isEnglish) "Revised · " else "संशोधित · ") else ""}${formatEpochDate(sell.entryDate)}",
                    fontSize = 11.sp,
                    color = MandiTextMuted
                )
                Text(
                    text = if (isEnglish) "View slip →" else "पर्ची देखें →",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MandiPrimaryAction
                )
            }

            if (isVoid) {
                Text(
                    text = if (isEnglish) "Cancelled · ${formatVoidReason(sell.voidReason, isEnglish)}" else "रद्द · ${formatVoidReason(sell.voidReason, false)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MandiTextSecondary
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FintechLendCard(
    isEnglish: Boolean,
    lend: com.appwork.mandisamiti.domain.model.LendEntry,
    loanInfo: OpenLoanInfo? = null,
    tag: com.appwork.mandisamiti.domain.ledger.LedgerTag? = null,
    onEditRepayment: (com.appwork.mandisamiti.domain.model.LendSettlement) -> Unit = {},
    onShowRepaymentHistory: (com.appwork.mandisamiti.domain.model.LendSettlement) -> Unit = {},
    onLongPress: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val isVoid = lend.isVoid
    val amountRupees = MandiMathEngine.paisaToRupeesString(lend.amountPaisa)
    val isAdvanceGiven = lend.lendType == com.appwork.mandisamiti.domain.model.LendType.MONEY
    val outstandingPaisa = loanInfo?.outstandingPaisa ?: lend.amountPaisa
    val interestDuePaisa = loanInfo?.interestDuePaisa ?: 0L
    val repaidPaisa = (lend.amountPaisa - outstandingPaisa).coerceAtLeast(0L)
    val totalLeftPaisa = outstandingPaisa + interestDuePaisa

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                onClick = {},
                onLongClick = { if (!isVoid) onLongPress() }
            )
            .background(MandiSurface)
            .border(1.dp, MandiBorder, shape)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.alpha(if (isVoid) 0.55f else 1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isAdvanceGiven) MandiGreenReceivable else MandiRedPayable,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isAdvanceGiven) (if (isEnglish) "Loan #${slipLabel(lend.slipNo, lend.id)}" else "उधार #${slipLabel(lend.slipNo, lend.id)}") else (if (isEnglish) "Bag loan #${slipLabel(lend.slipNo, lend.id)}" else "बोरी उधार #${slipLabel(lend.slipNo, lend.id)}"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    )
                    LedgerTagChip(tag = tag, slipNo = null, isEnglish = isEnglish)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹$amountRupees",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary,
                        textDecoration = if (isVoid) TextDecoration.LineThrough else null
                    )
                    if (!isVoid) {
                        IconButton(
                            onClick = onLongPress,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = if (isEnglish) "Options" else "विकल्प",
                                tint = MandiTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "${if (isEnglish) "Principal · cash given · " else "मूल · नकद दिया · "}${formatEpochDate(lend.createdAt)}",
                fontSize = 12.sp,
                color = MandiTextSecondary
            )

            if (isAdvanceGiven) {
                // Direction B Final Breakdown Tile
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MandiSurfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (interestDuePaisa > 0L) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (isEnglish) "Interest" else "ब्याज", fontSize = 12.sp, color = MandiTextSecondary)
                            Text(text = "₹${MandiMathEngine.paisaToRupeesString(interestDuePaisa)}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MandiAmberDark)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = if (isEnglish) "Repaid" else "वापस मिला", fontSize = 12.sp, color = MandiTextSecondary)
                        Text(text = "₹${MandiMathEngine.paisaToRupeesString(repaidPaisa)}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MandiTextPrimary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = if (isEnglish) "Left" else "बाकी", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MandiGreenReceivable)
                        Text(text = "₹${MandiMathEngine.paisaToRupeesString(totalLeftPaisa)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MandiGreenReceivable)
                    }
                    if (interestDuePaisa > 0L) {
                        Text(
                            text = if (isEnglish) "Principal ₹${MandiMathEngine.paisaToRupeesString(outstandingPaisa)} + Interest ₹${MandiMathEngine.paisaToRupeesString(interestDuePaisa)}" else "मूल ₹${MandiMathEngine.paisaToRupeesString(outstandingPaisa)} + ब्याज ₹${MandiMathEngine.paisaToRupeesString(interestDuePaisa)}",
                            fontSize = 11.sp,
                            color = MandiTextMuted
                        )
                    }
                }

                // Nested Repayments List if any
                val settlements = loanInfo?.settlements ?: emptyList()
                if (settlements.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Repayments" else "वापसी",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                        settlements.forEach { s ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${formatEpochDate(s.settledAt)} · ${if (isEnglish) "Cash" else "नकद"}",
                                        fontSize = 11.sp,
                                        color = MandiTextSecondary
                                    )
                                    if (s.revision > 1L) {
                                        Text(
                                            text = if (isEnglish) "edited" else "सुधारा",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MandiAmberDark,
                                            modifier = Modifier
                                                .clickable { onShowRepaymentHistory(s) }
                                                .padding(horizontal = 2.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "+₹${MandiMathEngine.paisaToRupeesString(s.amountPaisa)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MandiGreenReceivable
                                    )
                                    if (lend.lendType == com.appwork.mandisamiti.domain.model.LendType.MONEY && !isVoid) {
                                        IconButton(
                                            onClick = { onEditRepayment(s) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = if (isEnglish) "Edit payment" else "वापसी सुधारें",
                                                tint = MandiTextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val lendNote = com.appwork.mandisamiti.domain.util.RemarksFormatter.cleanForDisplay(lend.notes)
            if (lendNote != null) {
                Text(
                    text = lendNote,
                    fontSize = 12.sp,
                    color = MandiTextSecondary
                )
            }

            if (isVoid) {
                Text(
                    text = if (isEnglish) "Cancelled · ${formatVoidReason(lend.voidReason, isEnglish)}" else "रद्द · ${formatVoidReason(lend.voidReason, false)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MandiTextSecondary
                )
            }
        }
    }
}

private fun formatVoidReason(reasonStr: String?, isEnglish: Boolean): String {
    if (reasonStr.isNullOrBlank()) return ""
    return VoidReason.entries.find { it.name == reasonStr }?.let { if (isEnglish) it.labelEn else it.labelHi } ?: reasonStr
}

internal fun formatEpochDate(epochMs: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val monthStr = monthNames.getOrElse(dt.monthNumber - 1) { dt.month.name.take(3) }
    return "${dt.dayOfMonth} $monthStr ${dt.year}"
}
