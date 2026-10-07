package com.appwork.mandisamiti

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstDealRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.domain.model.Deal
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import com.appwork.mandisamiti.platform.WhatsAppShareManager
import com.appwork.mandisamiti.platform.rememberCameraSlipPicker
import com.appwork.mandisamiti.ui.auth.RegisterScreen
import com.appwork.mandisamiti.ui.auth.RegisterViewModel
import com.appwork.mandisamiti.ui.deal.DealEntryScreen
import com.appwork.mandisamiti.ui.deal.DealEntryViewModel
import com.appwork.mandisamiti.ui.home.HomeScreen
import com.appwork.mandisamiti.ui.home.HomeViewModel
import com.appwork.mandisamiti.ui.ledger.PartyLedgerScreen
import com.appwork.mandisamiti.ui.ledger.PartyLedgerViewModel
import com.appwork.mandisamiti.ui.register.DailyCashRegisterScreen
import com.appwork.mandisamiti.ui.register.DailyRegisterViewModel
import com.appwork.mandisamiti.ui.slip.ReceiptPreviewScreen
import com.appwork.mandisamiti.ui.theme.MandiSamitiTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Register : Screen
    data object Home : Screen
    data class DealEntry(val existingDealId: String? = null) : Screen
    data class PartyLedger(val partyId: String) : Screen
    data class ReceiptPreview(val deal: Deal, val farmer: Party, val buyer: Party?) : Screen
    data object DailyRegister : Screen
}

@Composable
fun App(
    database: AppDatabase,
    ttsManager: SoundboxTtsManager,
    whatsAppShareManager: WhatsAppShareManager
) {
    val coroutineScope = rememberCoroutineScope()
    val shopRepo = remember { OfflineFirstShopProfileRepository(database) }
    val partyRepo = remember { OfflineFirstPartyRepository(database) }
    val dealRepo = remember { OfflineFirstDealRepository(database) }
    val cashRepo = remember { OfflineFirstCashTransactionRepository(database) }
    val cameraPicker = rememberCameraSlipPicker()

    val shopId = "shop_default"

    // Demo dataset seeding for parties
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val parties = partyRepo.getPartiesStream(shopId).firstOrNull()
            if (parties.isNullOrEmpty()) {
                partyRepo.saveParty(
                    Party(
                        id = "farmer_1",
                        shopId = shopId,
                        name = "रामवीर सिंह",
                        village = "राया (मथुरा)",
                        phone = "9837000001",
                        partyType = PartyType.FARMER,
                        monthlyInterestRate = 1.5,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )
                partyRepo.saveParty(
                    Party(
                        id = "farmer_2",
                        shopId = shopId,
                        name = "महेन्द्र प्रधान",
                        village = "गोवर्धन",
                        phone = "9837000002",
                        partyType = PartyType.FARMER,
                        monthlyInterestRate = 1.5,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )
                partyRepo.saveParty(
                    Party(
                        id = "farmer_3",
                        shopId = shopId,
                        name = "बृजकिशोर शर्मा",
                        village = "बरसाना",
                        phone = "9837000003",
                        partyType = PartyType.FARMER,
                        monthlyInterestRate = 1.5,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )
                partyRepo.saveParty(
                    Party(
                        id = "buyer_1",
                        shopId = shopId,
                        name = "अग्रवाल ट्रेडर्स",
                        village = "मथुरा शहर",
                        phone = "9837000004",
                        partyType = PartyType.BUYER,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )
                partyRepo.saveParty(
                    Party(
                        id = "buyer_2",
                        shopId = shopId,
                        name = "राधे श्याम फ्लोर मिल",
                        village = "कोसी कलां",
                        phone = "9837000005",
                        partyType = PartyType.BUYER,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )
            }
        }
    }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Register) }

    LaunchedEffect(Unit) {
        val existingProfile = shopRepo.getShopProfileStream().firstOrNull()
        if (existingProfile != null) {
            currentScreen = Screen.Home
        }
    }

    MandiSamitiTheme {
        when (val screen = currentScreen) {
            is Screen.Register -> {
                val registerViewModel = remember {
                    RegisterViewModel(
                        shopProfileRepository = shopRepo,
                        ttsManager = ttsManager,
                        viewModelScope = coroutineScope
                    )
                }
                RegisterScreen(
                    viewModel = registerViewModel,
                    onRegistrationSuccess = {
                        currentScreen = Screen.Home
                    }
                )
            }

            is Screen.Home -> {
                val homeViewModel = remember {
                    HomeViewModel(
                        shopProfileRepository = shopRepo,
                        partyRepository = partyRepo,
                        viewModelScope = coroutineScope
                    )
                }
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToNewEntry = {
                        currentScreen = Screen.DealEntry()
                    },
                    onNavigateToPartyKhata = { party ->
                        currentScreen = Screen.PartyLedger(party.id)
                    },
                    onNavigateToDayClosing = {
                        currentScreen = Screen.DailyRegister
                    },
                    onSignOut = {
                        currentScreen = Screen.Register
                    }
                )
            }

            is Screen.DealEntry -> {
                MandiBackHandler { currentScreen = Screen.Home }
                val dealViewModel = remember(screen.existingDealId) {
                    DealEntryViewModel(
                        shopId = shopId,
                        existingDealId = screen.existingDealId,
                        dealRepository = dealRepo,
                        partyRepository = partyRepo,
                        shopProfileRepository = shopRepo,
                        ttsManager = ttsManager,
                        viewModelScope = coroutineScope
                    )
                }
                DealEntryScreen(
                    viewModel = dealViewModel,
                    onNavigateBack = { currentScreen = Screen.Home },
                    onDealSavedSuccess = { dealId ->
                        coroutineScope.launch {
                            val deal = dealRepo.getDealById(dealId)
                            if (deal != null) {
                                val farmer = partyRepo.getPartyById(deal.farmerId)
                                val buyer = deal.buyerId?.let { partyRepo.getPartyById(it) }
                                if (farmer != null) {
                                    currentScreen = Screen.ReceiptPreview(deal, farmer, buyer)
                                    return@launch
                                }
                            }
                            currentScreen = Screen.Home
                        }
                    }
                )
            }

            is Screen.PartyLedger -> {
                MandiBackHandler { currentScreen = Screen.Home }
                val ledgerViewModel = remember(screen.partyId) {
                    PartyLedgerViewModel(
                        shopId = shopId,
                        partyId = screen.partyId,
                        partyRepository = partyRepo,
                        cashRepository = cashRepo,
                        dealRepository = dealRepo,
                        shopProfileRepository = shopRepo,
                        ttsManager = ttsManager,
                        viewModelScope = coroutineScope
                    )
                }
                PartyLedgerScreen(
                    viewModel = ledgerViewModel,
                    onNavigateBack = { currentScreen = Screen.Home },
                    onShareWhatsAppReceipt = { _ -> }
                )
            }

            is Screen.ReceiptPreview -> {
                MandiBackHandler { currentScreen = Screen.Home }
                var shopProfile by remember {
                    mutableStateOf(
                        ShopProfile(
                            id = shopId,
                            shopName = "श्री गणेश ट्रेडिंग",
                            ownerName = "लाला मदन लाल जी",
                            mandiName = "मथुरा मंडी",
                            shopNumber = "A-1",
                            phoneNumber = "9837123456",
                            pinHash = "1234",
                            createdAt = 1000L,
                            updatedAt = 1000L
                        )
                    )
                }
                LaunchedEffect(Unit) {
                    shopRepo.getShopProfileStream().collect { profile ->
                        if (profile != null) shopProfile = profile
                    }
                }
                ReceiptPreviewScreen(
                    shopProfile = shopProfile,
                    farmer = screen.farmer,
                    buyer = screen.buyer,
                    deal = screen.deal,
                    whatsAppShareManager = whatsAppShareManager,
                    ttsManager = ttsManager,
                    onNavigateBack = { currentScreen = Screen.Home }
                )
            }

            is Screen.DailyRegister -> {
                MandiBackHandler { currentScreen = Screen.Home }
                val registerViewModel = remember {
                    DailyRegisterViewModel(
                        shopId = shopId,
                        cashRepository = cashRepo,
                        partyRepository = partyRepo,
                        shopProfileRepository = shopRepo,
                        ttsManager = ttsManager,
                        viewModelScope = coroutineScope
                    )
                }
                DailyCashRegisterScreen(
                    viewModel = registerViewModel,
                    onNavigateBack = { currentScreen = Screen.Home }
                )
            }
        }
    }
}
