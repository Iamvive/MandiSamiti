package com.appwork.mandisamiti

import com.appwork.mandisamiti.ui.buy.BuyEntryScreen
import com.appwork.mandisamiti.ui.buy.BuyTransactionsListScreen
import com.appwork.mandisamiti.ui.buy.BuyTransactionReceiptScreen
import com.appwork.mandisamiti.ui.sell.SellEntryScreen
import com.appwork.mandisamiti.ui.sell.SellTransactionsListScreen
import com.appwork.mandisamiti.ui.sell.SellTransactionReceiptScreen
import com.appwork.mandisamiti.ui.lend.LendEntryScreen
import com.appwork.mandisamiti.ui.lend.LendTransactionsListScreen
import com.appwork.mandisamiti.ui.expense.ExpenseEntryScreen
import com.appwork.mandisamiti.ui.expense.labelHi
import com.appwork.mandisamiti.ui.stock.StockBreakdownSheet
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.appwork.mandisamiti.data.auth.AuthApi
import com.appwork.mandisamiti.data.auth.AuthRepository
import com.appwork.mandisamiti.data.auth.LocalDataWiper
import com.appwork.mandisamiti.data.auth.mandiHttpClient
import com.appwork.mandisamiti.data.auth.SessionStore
import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstDealRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.data.sync.NoOpSyncScheduler
import com.appwork.mandisamiti.data.sync.SyncEngine
import com.appwork.mandisamiti.data.sync.SyncScheduler
import com.appwork.mandisamiti.data.sync.remote.KtorMandiSyncApiClient
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.domain.model.Deal
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.platform.apiBaseUrl
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
import com.appwork.mandisamiti.ui.settings.LogoutResult
import com.appwork.mandisamiti.ui.settings.LogoutUseCase
import com.appwork.mandisamiti.ui.settings.logoutBlockedMessage
import com.appwork.mandisamiti.ui.slip.ReceiptPreviewScreen
import com.appwork.mandisamiti.ui.sync.FirstSyncScreen
import com.appwork.mandisamiti.ui.sync.FirstSyncViewModel
import com.appwork.mandisamiti.ui.theme.MandiSamitiTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

sealed interface Screen {
    data class Register(val isReLogin: Boolean = false) : Screen
    data object FirstSync : Screen
    data object Home : Screen
    data class DealEntry(val existingDealId: String? = null) : Screen
    data class PartyLedger(val partyId: String, val returnScreen: Screen? = null) : Screen
    data class ReceiptPreview(val deal: Deal, val farmer: Party, val buyer: Party?) : Screen
    data object DailyRegister : Screen

    // Revamp Screens (Tasks 11-15)
    data class BuyEntry(val existingId: String? = null) : Screen
    data object BuyList : Screen
    data class BuyReceipt(val id: String, val returnScreen: Screen? = null) : Screen
    data class SellEntry(val existingId: String? = null) : Screen
    data object SellList : Screen
    data class SellReceipt(val id: String, val returnScreen: Screen? = null) : Screen
    data object LendEntry : Screen
    data object LendList : Screen
    data object ExpenseEntry : Screen
    data object StockSheet : Screen
}

fun Screen.backTarget(): Screen = when (this) {
    is Screen.BuyEntry -> Screen.BuyList
    is Screen.BuyReceipt -> returnScreen ?: Screen.BuyList
    is Screen.BuyList -> Screen.Home
    is Screen.SellEntry -> Screen.SellList
    is Screen.SellReceipt -> returnScreen ?: Screen.SellList
    is Screen.SellList -> Screen.Home
    is Screen.LendEntry -> Screen.LendList
    is Screen.LendList -> Screen.Home
    is Screen.ExpenseEntry -> Screen.Home
    is Screen.StockSheet -> Screen.Home
    is Screen.DealEntry -> Screen.Home
    is Screen.PartyLedger -> returnScreen ?: Screen.Home
    is Screen.ReceiptPreview -> Screen.Home
    is Screen.DailyRegister -> Screen.Home
    is Screen.Register, is Screen.FirstSync, is Screen.Home -> Screen.Home
}

@Composable
fun App(
    database: AppDatabase,
    ttsManager: SoundboxTtsManager,
    whatsAppShareManager: WhatsAppShareManager,
    sessionStore: SessionStore,
    authApi: AuthApi,
    syncScheduler: SyncScheduler = remember { NoOpSyncScheduler() },
) {
    val coroutineScope = rememberCoroutineScope()
    val shopRepo = remember { OfflineFirstShopProfileRepository(database) }
    val partyRepo = remember { OfflineFirstPartyRepository(database) }
    val dealRepo = remember { OfflineFirstDealRepository(database) }
    val cashRepo = remember { OfflineFirstCashTransactionRepository(database) }
    val tradeSettingsRepo = remember { com.appwork.mandisamiti.data.repository.OfflineFirstTradeSettingsRepository(database) }
    val buyRepo = remember { com.appwork.mandisamiti.data.repository.OfflineFirstBuyRepository(database) }
    val sellRepo = remember { com.appwork.mandisamiti.data.repository.OfflineFirstSellRepository(database) }
    val lendRepo = remember { com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository(database) }
    val expenseRepo = remember { com.appwork.mandisamiti.data.repository.OfflineFirstExpenseRepository(database) }
    val stockUseCase = remember { com.appwork.mandisamiti.domain.usecase.StockInventoryUseCase(expenseRepo) }
    val wiper = remember { LocalDataWiper(database) }
    val authRepo = remember {
        AuthRepository(
            api = authApi,
            sessionStore = sessionStore,
            shopProfileRepository = shopRepo,
            wiper = wiper,
        )
    }
    val syncEngine = remember {
        SyncEngine(
            database,
            KtorMandiSyncApiClient(mandiHttpClient(), apiBaseUrl, sessionStore, authApi::refresh)
        )
    }
    val logoutUseCase = remember {
        LogoutUseCase(syncEngine, authApi, sessionStore, wiper, onBeforeWipe = { syncScheduler.cancelAll() })
    }
    val cameraPicker = rememberCameraSlipPicker()
    val snackbarHostState = remember { SnackbarHostState() }

    // The stored session alone decides the start screen, synchronously: no async lookup, no Register flash.
    var session by remember { mutableStateOf(sessionStore.current()) }
    var currentScreen by remember { mutableStateOf<Screen>(if (session != null) Screen.Home else Screen.Register()) }
    var signOutInFlight by remember { mutableStateOf(false) }

    MandiSamitiTheme {
        val activeSession = session
        // Every shop-scoped screen needs a session; without one the only place to go is Register.
        val screen = if (activeSession == null) Screen.Register() else currentScreen
        val shopId = activeSession?.shopId
        val tradeSettings by tradeSettingsRepo.getTradeSettingsStream().collectAsState(initial = null)
        when (screen) {
            is Screen.Register -> {
                val shopProfile by shopRepo.getShopProfileStream().collectAsState(initial = null)
                val prefillPhone = if (screen.isReLogin) shopProfile?.phoneNumber else null
                val registerViewModel = remember(screen.isReLogin, prefillPhone) {
                    RegisterViewModel(
                        authRepository = authRepo,
                        ttsManager = ttsManager,
                        viewModelScope = coroutineScope,
                        initialPhoneNumber = prefillPhone
                    )
                }
                RegisterScreen(
                    viewModel = registerViewModel,
                    onRegistrationSuccess = { newSession ->
                        // Use the session AuthRepository returned (already saved): no read-back that could fail silently.
                        session = newSession
                        syncScheduler.schedulePeriodicSync()
                        syncScheduler.scheduleOneTimeSync()
                        coroutineScope.launch { syncEngine.setNeedsLogin(false) }
                        currentScreen = Screen.FirstSync
                    },
                    onBack = if (screen.isReLogin || activeSession != null) {
                        { currentScreen = Screen.Home }
                    } else null
                )
            }

            is Screen.FirstSync -> {
                val firstSyncViewModel = remember(shopId) {
                    FirstSyncViewModel(
                        shopId = shopId!!,
                        // Push first: pending edits from before a re-login must not be overtaken by the pull.
                        pull = { id, p -> syncEngine.pushThenPull(id, p) },
                        viewModelScope = coroutineScope,
                    )
                }
                val firstSyncState by firstSyncViewModel.state.collectAsState()
                FirstSyncScreen(
                    state = firstSyncState,
                    onRetry = firstSyncViewModel::retry,
                    onSkip = { currentScreen = Screen.Home },
                    onDone = { currentScreen = Screen.Home },
                )
            }

            is Screen.Home -> {
                val homeViewModel = remember(shopId) {
                    HomeViewModel(
                        shopId = shopId!!,
                        shopProfileRepository = shopRepo,
                        partyRepository = partyRepo,
                        tradeSettingsRepository = tradeSettingsRepo,
                        buyRepository = buyRepo,
                        sellRepository = sellRepo,
                        lendRepository = lendRepo,
                        expenseRepository = expenseRepo,
                        stockUseCase = stockUseCase,
                        viewModelScope = coroutineScope,
                        onLocalWrite = { syncScheduler.scheduleOneTimeSync() },
                        pendingSyncCountProvider = { syncEngine.getPendingEntryCount().toInt() }
                    )
                }
                val registerViewModel = remember(shopId) {
                    DailyRegisterViewModel(
                        shopId = shopId!!,
                        cashRepository = cashRepo,
                        partyRepository = partyRepo,
                        shopProfileRepository = shopRepo,
                        ttsManager = ttsManager,
                        viewModelScope = coroutineScope,
                        onLocalWrite = { syncScheduler.scheduleOneTimeSync() }
                    )
                }
                val needsLogin by produceState(false, shopId) { value = syncEngine.needsLogin() }
                HomeScreen(
                    viewModel = homeViewModel,
                    registerViewModel = registerViewModel,
                    shopId = shopId!!,
                    needsLogin = needsLogin,
                    onReLogin = {
                        currentScreen = Screen.Register(isReLogin = true)
                    },
                    snackbarHostState = snackbarHostState,
                    onNavigateToNewEntry = {
                        currentScreen = Screen.BuyEntry(null)
                    },
                    onNavigateToPartyKhata = { party ->
                        currentScreen = Screen.PartyLedger(party.id, returnScreen = Screen.Home)
                    },
                    onNavigateToDayClosing = {
                        currentScreen = Screen.DailyRegister
                    },
                    onShareWhatsApp = { reportText ->
                        whatsAppShareManager.shareText(reportText, null)
                    },
                    onNavigateToBuy = { currentScreen = Screen.BuyEntry(null) },
                    onNavigateToSell = { currentScreen = Screen.SellEntry(null) },
                    onNavigateToLend = { currentScreen = Screen.LendEntry },
                    onNavigateToExpense = { currentScreen = Screen.ExpenseEntry },
                    onNavigateToBuyList = { currentScreen = Screen.BuyList },
                    onNavigateToSellList = { currentScreen = Screen.SellList },
                    onNavigateToLendList = { currentScreen = Screen.LendList },
                    onNavigateToStockSheet = { currentScreen = Screen.StockSheet },
                    onNavigateToBuyReceipt = { id -> currentScreen = Screen.BuyReceipt(id, Screen.Home) },
                    onNavigateToSellReceipt = { id -> currentScreen = Screen.SellReceipt(id, Screen.Home) },
                    onSignOut = {
                        // Ignore a second tap while a sign-out is already running.
                        if (!signOutInFlight) {
                            signOutInFlight = true
                            coroutineScope.launch {
                                try {
                                    when (val result = logoutUseCase()) {
                                        is LogoutResult.Blocked ->
                                            snackbarHostState.showSnackbar(logoutBlockedMessage(result.pendingCount))
                                        LogoutResult.LoggedOut -> {
                                            session = null
                                            currentScreen = Screen.Register()
                                        }
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("लॉगआउट नहीं हो सका — दोबारा कोशिश करें")
                                } finally {
                                    signOutInFlight = false
                                }
                            }
                        }
                    }
                )
            }

            is Screen.DealEntry -> {
                MandiBackHandler { currentScreen = Screen.Home }
                val dealViewModel = remember(shopId, screen.existingDealId) {
                    DealEntryViewModel(
                        shopId = shopId!!,
                        existingDealId = screen.existingDealId,
                        dealRepository = dealRepo,
                        partyRepository = partyRepo,
                        shopProfileRepository = shopRepo,
                        tradeSettingsRepository = tradeSettingsRepo,
                        ttsManager = ttsManager,
                        viewModelScope = coroutineScope
                    )
                }
                DealEntryScreen(
                    viewModel = dealViewModel,
                    onNavigateBack = { currentScreen = Screen.Home },
                    onDealSavedSuccess = { dealId ->
                        syncScheduler.scheduleOneTimeSync()
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
                MandiBackHandler { currentScreen = screen.backTarget() }
                val ledgerViewModel = remember(shopId, screen.partyId) {
                    PartyLedgerViewModel(
                        shopId = shopId!!,
                        partyId = screen.partyId,
                        partyRepository = partyRepo,
                        cashRepository = cashRepo,
                        dealRepository = dealRepo,
                        shopProfileRepository = shopRepo,
                        ttsManager = ttsManager,
                        buyRepository = buyRepo,
                        sellRepository = sellRepo,
                        lendRepository = lendRepo,
                        syncScheduler = syncScheduler,
                        viewModelScope = coroutineScope
                    )
                }
                PartyLedgerScreen(
                    viewModel = ledgerViewModel,
                    onNavigateBack = { currentScreen = screen.backTarget() },
                    onShareWhatsAppReceipt = { dealId ->
                        coroutineScope.launch {
                            val deal = dealRepo.getDealById(dealId)
                            if (deal != null) {
                                val farmer = partyRepo.getPartyById(deal.farmerId)
                                val buyer = deal.buyerId?.let { partyRepo.getPartyById(it) }
                                if (farmer != null) {
                                    currentScreen = Screen.ReceiptPreview(deal, farmer, buyer)
                                }
                            }
                        }
                    },
                    onEditDeal = { dealId -> currentScreen = Screen.DealEntry(dealId) },
                    onOpenBuyReceipt = { buyId -> currentScreen = Screen.BuyReceipt(buyId, Screen.PartyLedger(screen.partyId, screen.returnScreen)) },
                    onOpenSellReceipt = { sellId -> currentScreen = Screen.SellReceipt(sellId, Screen.PartyLedger(screen.partyId, screen.returnScreen)) }
                )
            }

            is Screen.ReceiptPreview -> {
                MandiBackHandler { currentScreen = Screen.Home }
                val shopProfile by shopRepo.getShopProfileStream().collectAsState(initial = null)
                shopProfile?.let { profile ->
                    ReceiptPreviewScreen(
                        shopProfile = profile,
                        farmer = screen.farmer,
                        buyer = screen.buyer,
                        deal = screen.deal,
                        whatsAppShareManager = whatsAppShareManager,
                        ttsManager = ttsManager,
                        onNavigateBack = { currentScreen = Screen.Home }
                    )
                }
            }

            is Screen.DailyRegister -> {
                MandiBackHandler { currentScreen = Screen.Home }
                val registerViewModel = remember(shopId) {
                    DailyRegisterViewModel(
                        shopId = shopId!!,
                        cashRepository = cashRepo,
                        partyRepository = partyRepo,
                        shopProfileRepository = shopRepo,
                        ttsManager = ttsManager,
                        viewModelScope = coroutineScope,
                        onLocalWrite = { syncScheduler.scheduleOneTimeSync() }
                    )
                }
                DailyCashRegisterScreen(
                    viewModel = registerViewModel,
                    onNavigateBack = { currentScreen = Screen.Home },
                    onShareWhatsApp = { reportText ->
                        whatsAppShareManager.shareText(reportText, null)
                    },
                    isEnglish = tradeSettings?.isEnglish ?: false,
                    showBackButton = true
                )
            }

            is Screen.BuyEntry -> {
                val lends by lendRepo.getLendEntriesStream(shopId ?: "").collectAsState(initial = emptyList())
                BuyEntryScreen(
                    shopId = shopId ?: "",
                    existingId = screen.existingId,
                    buyRepository = buyRepo,
                    tradeSettings = tradeSettings,
                    activeLoanChecker = { farmerName ->
                        lends.any { !it.isVoid && it.status != com.appwork.mandisamiti.domain.model.LendStatus.SETTLED && it.partyId.equals(farmerName, ignoreCase = true) }
                    },
                    onSaved = { entry ->
                        coroutineScope.launch {
                            if (screen.existingId == null) {
                                buyRepo.saveBuyEntry(entry)
                            } else {
                                buyRepo.editBuyEntry(entry)
                            }
                            syncScheduler.scheduleOneTimeSync()
                            currentScreen = Screen.BuyReceipt(entry.id)
                        }
                    },
                    onBack = { currentScreen = screen.backTarget() }
                )
            }
            is Screen.BuyList -> {
                MandiBackHandler { currentScreen = screen.backTarget() }
                val buys by buyRepo.getBuyEntriesStream(shopId ?: "").collectAsState(initial = emptyList())
                BuyTransactionsListScreen(
                    entries = buys,
                    onOpen = { id -> currentScreen = Screen.BuyReceipt(id) },
                    onNew = { currentScreen = Screen.BuyEntry(null) },
                    onBack = { currentScreen = screen.backTarget() },
                    onVoid = { id, reason ->
                        coroutineScope.launch {
                            buyRepo.voidBuyEntry(id, reason)
                            syncScheduler.scheduleOneTimeSync()
                        }
                    }
                )
            }
            is Screen.BuyReceipt -> {
                MandiBackHandler { currentScreen = screen.backTarget() }
                var buyEntry by remember { mutableStateOf<com.appwork.mandisamiti.domain.model.BuyEntry?>(null) }
                LaunchedEffect(screen.id) {
                    buyEntry = buyRepo.getBuyEntryById(screen.id)
                }
                BuyTransactionReceiptScreen(
                    id = screen.id,
                    entry = buyEntry,
                    onShareWhatsApp = { text -> whatsAppShareManager.shareText(text, null) },
                    onBack = { currentScreen = screen.backTarget() }
                )
            }
            is Screen.SellEntry -> {
                SellEntryScreen(
                    shopId = shopId ?: "",
                    existingId = screen.existingId,
                    sellRepository = sellRepo,
                    tradeSettings = tradeSettings,
                    onSaved = { entry ->
                        coroutineScope.launch {
                            if (screen.existingId == null) {
                                sellRepo.saveSellEntry(entry)
                            } else {
                                sellRepo.editSellEntry(entry)
                            }
                            syncScheduler.scheduleOneTimeSync()
                            currentScreen = Screen.SellReceipt(entry.id)
                        }
                    },
                    onBack = { currentScreen = screen.backTarget() }
                )
            }
            is Screen.SellList -> {
                MandiBackHandler { currentScreen = screen.backTarget() }
                val sells by sellRepo.getSellEntriesStream(shopId ?: "").collectAsState(initial = emptyList())
                SellTransactionsListScreen(
                    entries = sells,
                    onOpen = { id -> currentScreen = Screen.SellReceipt(id) },
                    onNew = { currentScreen = Screen.SellEntry(null) },
                    onBack = { currentScreen = screen.backTarget() },
                    onVoid = { id, reason ->
                        coroutineScope.launch {
                            sellRepo.voidSellEntry(id, reason)
                            syncScheduler.scheduleOneTimeSync()
                        }
                    }
                )
            }
            is Screen.SellReceipt -> {
                MandiBackHandler { currentScreen = screen.backTarget() }
                var sellEntry by remember { mutableStateOf<com.appwork.mandisamiti.domain.model.SellEntry?>(null) }
                LaunchedEffect(screen.id) {
                    sellEntry = sellRepo.getSellEntryById(screen.id)
                }
                SellTransactionReceiptScreen(
                    id = screen.id,
                    entry = sellEntry,
                    onShareWhatsApp = { text -> whatsAppShareManager.shareText(text, null) },
                    onBack = { currentScreen = screen.backTarget() }
                )
            }
            is Screen.LendEntry -> {
                LendEntryScreen(
                    shopId = shopId ?: "",
                    onSaved = { entry ->
                        coroutineScope.launch {
                            lendRepo.saveLendEntry(entry)
                            if (entry.lendType == com.appwork.mandisamiti.domain.model.LendType.MONEY && entry.amountPaisa > 0L) {
                                val noteText = entry.notes?.trim()
                                val rem = if (noteText.isNullOrBlank()) "उधार ऋण" else "उधार ऋण: $noteText"
                                cashRepo.recordTransaction(
                                    com.appwork.mandisamiti.domain.model.CashTransaction(
                                        id = com.appwork.mandisamiti.domain.id.IdGenerator.newId(),
                                        shopId = shopId ?: "",
                                        partyId = entry.partyId,
                                        transactionType = com.appwork.mandisamiti.domain.model.TransactionType.UDHAR_GIVEN,
                                        amountPaisa = entry.amountPaisa,
                                        paymentMode = com.appwork.mandisamiti.domain.model.PaymentMode.CASH,
                                        transactionDate = entry.entryDate,
                                        remarks = "$rem [LEND:${entry.id}]",
                                        createdAt = entry.createdAt,
                                        updatedAt = entry.updatedAt,
                                    )
                                )
                            }
                            syncScheduler.scheduleOneTimeSync()
                            currentScreen = Screen.LendList
                        }
                    },
                    onBack = { currentScreen = screen.backTarget() }
                )
            }
            is Screen.LendList -> {
                MandiBackHandler { currentScreen = screen.backTarget() }
                val lends by lendRepo.getLendEntriesStream(shopId ?: "").collectAsState(initial = emptyList())
                LendTransactionsListScreen(
                    entries = lends,
                    onNew = { currentScreen = Screen.LendEntry },
                    onPartyClick = { partyId -> currentScreen = Screen.PartyLedger(partyId, returnScreen = Screen.LendList) },
                    getSettlements = { lendId -> lendRepo.getSettlements(lendId) },
                    onRepay = { lendId, amountPaisa, weightGrams, bagsCount, note ->
                        coroutineScope.launch {
                            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                            val settleId = com.appwork.mandisamiti.domain.id.IdGenerator.newId()
                            lendRepo.settle(
                                com.appwork.mandisamiti.domain.model.LendSettlement(
                                    id = settleId,
                                    shopId = shopId ?: "",
                                    lendId = lendId,
                                    settledAt = now,
                                    amountPaisa = amountPaisa,
                                    weightGrams = weightGrams,
                                    bagsCount = bagsCount,
                                    createdAt = now,
                                    updatedAt = now,
                                )
                            )
                            if (amountPaisa > 0L) {
                                val settledLend = lends.find { it.id == lendId }
                                val noteText = note.trim()
                                val rem = if (noteText.isBlank()) "उधार वापसी जमा" else "उधार वापसी जमा: $noteText"
                                cashRepo.recordTransaction(
                                    com.appwork.mandisamiti.domain.model.CashTransaction(
                                        id = com.appwork.mandisamiti.domain.id.IdGenerator.newId(),
                                        shopId = shopId ?: "",
                                        partyId = settledLend?.partyId ?: "किसान",
                                        transactionType = com.appwork.mandisamiti.domain.model.TransactionType.JAMA_RECEIVED,
                                        amountPaisa = amountPaisa,
                                        paymentMode = com.appwork.mandisamiti.domain.model.PaymentMode.CASH,
                                        transactionDate = now,
                                        remarks = "$rem [SETTLE:$settleId:$lendId]",
                                        createdAt = now,
                                        updatedAt = now,
                                    )
                                )
                            }
                            syncScheduler.scheduleOneTimeSync()
                        }
                    },
                    onVoid = { id, reason ->
                        coroutineScope.launch {
                            lendRepo.voidLendEntry(id, reason)
                            syncScheduler.scheduleOneTimeSync()
                        }
                    },
                    onBack = { currentScreen = screen.backTarget() }
                )
            }
            is Screen.ExpenseEntry -> {
                ExpenseEntryScreen(
                    shopId = shopId ?: "",
                    onSaved = { entry ->
                        coroutineScope.launch {
                            expenseRepo.saveExpense(entry)
                            if (entry.paymentMode == com.appwork.mandisamiti.domain.model.ExpensePaymentMode.CASH) {
                                val expRemarks = entry.remarks?.trim()
                                val baseRem = if (expRemarks.isNullOrBlank()) "खर्च: ${entry.category.labelHi()}" else "खर्च: ${entry.category.labelHi()} $expRemarks"
                                cashRepo.recordTransaction(
                                    com.appwork.mandisamiti.domain.model.CashTransaction(
                                        id = com.appwork.mandisamiti.domain.id.IdGenerator.newId(),
                                        shopId = shopId ?: "",
                                        partyId = entry.paidTo?.ifBlank { null } ?: entry.category.name,
                                        transactionType = com.appwork.mandisamiti.domain.model.TransactionType.UDHAR_GIVEN,
                                        amountPaisa = entry.amountPaisa,
                                        paymentMode = com.appwork.mandisamiti.domain.model.PaymentMode.CASH,
                                        transactionDate = entry.entryDate,
                                        remarks = "$baseRem [EXPENSE:${entry.id}]",
                                        createdAt = entry.createdAt,
                                        updatedAt = entry.updatedAt,
                                    )
                                )
                            }
                            syncScheduler.scheduleOneTimeSync()
                            currentScreen = Screen.Home
                        }
                    },
                    onBack = { currentScreen = screen.backTarget() }
                )
            }
            is Screen.StockSheet -> {
                MandiBackHandler { currentScreen = screen.backTarget() }
                var stockLines by remember { mutableStateOf<List<com.appwork.mandisamiti.domain.model.StockLine>>(emptyList()) }
                LaunchedEffect(shopId) {
                    shopId?.let { stockLines = stockUseCase.lines(it) }
                }
                StockBreakdownSheet(
                    stockLines = stockLines,
                    onDismiss = { currentScreen = screen.backTarget() }
                )
            }
        }
    }
}
