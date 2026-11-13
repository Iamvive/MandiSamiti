package com.appwork.mandisamiti

import com.appwork.mandisamiti.ui.buy.BuyEntryScreen
import com.appwork.mandisamiti.ui.buy.BuyTransactionsListScreen
import com.appwork.mandisamiti.ui.buy.BuyTransactionReceiptScreen
import com.appwork.mandisamiti.ui.sell.SellEntryScreen
import com.appwork.mandisamiti.ui.sell.SellTransactionsListScreen
import com.appwork.mandisamiti.ui.sell.SellTransactionReceiptScreen
import com.appwork.mandisamiti.ui.lend.LendEntryScreen
import com.appwork.mandisamiti.ui.lend.LoanAdvanceEntryScreen
import com.appwork.mandisamiti.ui.lend.LendTransactionsListScreen
import com.appwork.mandisamiti.ui.expense.ExpenseEntryScreen
import com.appwork.mandisamiti.ui.expense.labelHi
import com.appwork.mandisamiti.ui.stock.StockBreakdownSheet
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.first

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
import com.appwork.mandisamiti.ui.settings.StartupLanguage
import com.appwork.mandisamiti.ui.settings.startupLanguage
import androidx.compose.foundation.layout.fillMaxSize
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
    data class BuyPayment(val entryId: String, val returnScreen: Screen? = null) : Screen
    data object BuyList : Screen
    data class BuyReceipt(val id: String, val returnScreen: Screen? = null) : Screen
    data class SellEntry(val existingId: String? = null) : Screen
    data object SellList : Screen
    data class SellReceipt(val id: String, val returnScreen: Screen? = null) : Screen
    data object LendEntry : Screen
    data class GrainLendEntry(val returnScreen: Screen = Screen.LendEntry) : Screen
    data object LendList : Screen
    data class ExpenseEntry(val existingId: String? = null) : Screen
    data object ExpenseList : Screen
    data object StockSheet : Screen
    data object SubscriptionContact : Screen
}

fun Screen.backTarget(): Screen = when (this) {
    is Screen.BuyEntry -> Screen.BuyList
    is Screen.BuyPayment -> Screen.BuyReceipt(entryId, returnScreen)
    is Screen.BuyReceipt -> returnScreen ?: Screen.BuyList
    is Screen.BuyList -> Screen.Home
    is Screen.SellEntry -> Screen.SellList
    is Screen.SellReceipt -> returnScreen ?: Screen.SellList
    is Screen.SellList -> Screen.Home
    is Screen.LendEntry -> Screen.LendList
    is Screen.GrainLendEntry -> returnScreen
    is Screen.LendList -> Screen.Home
    is Screen.ExpenseEntry -> if (existingId != null) Screen.ExpenseList else Screen.Home
    is Screen.ExpenseList -> Screen.ExpenseEntry()
    is Screen.StockSheet -> Screen.Home
    is Screen.SubscriptionContact -> Screen.Home
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
    val lendUseCase = remember {
        com.appwork.mandisamiti.domain.usecase.LendTransactionUseCase(
            lendRepo,
            { com.appwork.mandisamiti.domain.id.IdGenerator.newId() },
            { kotlinx.datetime.Clock.System.now().toEpochMilliseconds() },
        )
    }
    val expenseRepo = remember { com.appwork.mandisamiti.data.repository.OfflineFirstExpenseRepository(database) }
    val quotaRepo = remember { com.appwork.mandisamiti.data.repository.QuotaRepository(database, sessionStore, baseUrl = apiBaseUrl) }
    val stockAuditRepo = remember { com.appwork.mandisamiti.data.repository.OfflineFirstStockAuditRepository(database) }
    val stockUseCase = remember { com.appwork.mandisamiti.domain.usecase.StockInventoryUseCase(expenseRepo, stockAuditRepo) }
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
        // Settings are seeded synchronously but the first composition still sees null: wait instead of flashing Hindi.
        if (startupLanguage(tradeSettings) is StartupLanguage.Loading) {
            androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize())
        } else when (screen) {
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
                        cashRepository = cashRepo,
                        quotaRepository = quotaRepo,
                        stockUseCase = stockUseCase,
                        stockAuditRepository = stockAuditRepo,
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
                        buyRepository = buyRepo,
                        sellRepository = sellRepo,
                        lendRepository = lendRepo,
                        expenseRepository = expenseRepo,
                        viewModelScope = coroutineScope,
                        drawerCountRepository = com.appwork.mandisamiti.data.repository.OfflineFirstDrawerCountRepository(database),
                        onLocalWrite = { syncScheduler.scheduleOneTimeSync() },
                        onDayClosed = {
                            syncScheduler.scheduleOneTimeSync()
                            coroutineScope.launch {
                                try {
                                    if (!shopId.isNullOrBlank()) syncEngine.pushThenPull(shopId!!)
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }
                val needsLogin by produceState(false, shopId) { value = syncEngine.needsLogin() }
                val stockBuys by buyRepo.getBuyEntriesStream(shopId ?: "").collectAsState(initial = emptyList())
                val stockSells by sellRepo.getSellEntriesStream(shopId ?: "").collectAsState(initial = emptyList())
                val stockParties by partyRepo.getPartiesStream(shopId ?: "").collectAsState(initial = emptyList())
                HomeScreen(
                    stockBuys = stockBuys,
                    stockSells = stockSells,
                    stockPartyNames = stockParties.associate { it.id to it.name },
                    viewModel = homeViewModel,
                    registerViewModel = registerViewModel,
                    shopId = shopId!!,
                    onSyncNow = { syncEngine.pushThenPull(shopId!!) },
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
                    onNavigateToExpense = { currentScreen = Screen.ExpenseEntry() },
                    onNavigateToBuyList = { currentScreen = Screen.BuyList },
                    onNavigateToSellList = { currentScreen = Screen.SellList },
                    onNavigateToLendList = { currentScreen = Screen.LendList },
                    onNavigateToStockSheet = { currentScreen = Screen.StockSheet },
                    onNavigateToBuyReceipt = { id -> currentScreen = Screen.BuyReceipt(id, Screen.Home) },
                    onNavigateToSellReceipt = { id -> currentScreen = Screen.SellReceipt(id, Screen.Home) },
                    onNavigateToQuotaPlans = { currentScreen = Screen.SubscriptionContact },
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
                        quotaRepository = quotaRepo,
                        ttsManager = ttsManager,
                        viewModelScope = coroutineScope
                    )
                }
                DealEntryScreen(
                    viewModel = dealViewModel,
                    onNavigateBack = { currentScreen = Screen.Home },
                    onNavigateToUpgrade = { currentScreen = Screen.SubscriptionContact },
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
                    onOpenSellReceipt = { sellId -> currentScreen = Screen.SellReceipt(sellId, Screen.PartyLedger(screen.partyId, screen.returnScreen)) },
                    isEnglish = tradeSettings?.isEnglish ?: false,
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
                        buyRepository = buyRepo,
                        sellRepository = sellRepo,
                        lendRepository = lendRepo,
                        expenseRepository = expenseRepo,
                        viewModelScope = coroutineScope,
                        drawerCountRepository = com.appwork.mandisamiti.data.repository.OfflineFirstDrawerCountRepository(database),
                        onLocalWrite = { syncScheduler.scheduleOneTimeSync() },
                        onDayClosed = {
                            syncScheduler.scheduleOneTimeSync()
                            coroutineScope.launch {
                                try {
                                    if (!shopId.isNullOrBlank()) syncEngine.pushThenPull(shopId!!)
                                } catch (_: Exception) {}
                            }
                        }
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
                val parties by partyRepo.getPartiesStream(shopId ?: "").collectAsState(initial = emptyList())
                val buyForm = remember(tradeSettings) { com.appwork.mandisamiti.ui.buy.BuyEntryFormState(tradeSettings) }
                // Loan section: the picked or phone-matched party's oldest open money loan, with interest due as of now.
                val loanPartyId = com.appwork.mandisamiti.ui.buy.BuyPartyResolver.existing(parties, buyForm.selectedFarmerId, buyForm.phone)?.id
                LaunchedEffect(loanPartyId, lends) {
                    val open = lends
                        .filter { !it.isVoid && it.partyId == loanPartyId && it.lendType == com.appwork.mandisamiti.domain.model.LendType.MONEY }
                        .sortedBy { it.entryDate }
                    var found: Triple<String, Long, Long>? = null
                    for (lend in open) {
                        val live = lendRepo.getSettlements(lend.id).filter { !it.isVoid }
                        val principal = (lend.amountPaisa - live.sumOf { it.principalPaisa }).coerceAtLeast(0L)
                        if (principal <= 0L) continue
                        val rate = lend.monthlyRatePercent
                        val interest = com.appwork.mandisamiti.domain.math.RuralInterestEngine.interestDue(
                            lend.amountPaisa, rate, lend.entryDate, live, kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                        )
                        found = Triple(lend.id, principal, interest)
                        break
                    }
                    if (found?.first != buyForm.loanLendId || found?.second != buyForm.loanPrincipalPaisa || found?.third != buyForm.loanInterestDuePaisa) {
                        buyForm.setLoan(found?.first, found?.second ?: 0L, found?.third ?: 0L)
                    }
                    if (loanPartyId != null) {
                        val partyCash = cashRepo.getTransactionsByPartyStream(loanPartyId).first()
                        val deposit = partyCash.filter { !it.isVoid && it.transactionType == com.appwork.mandisamiti.domain.model.TransactionType.JAMA_RECEIVED }.sumOf { it.amountPaisa }
                        buyForm.setDeposit(deposit)
                    } else {
                        buyForm.setDeposit(0L)
                    }
                }
                BuyEntryScreen(
                    shopId = shopId ?: "",
                    existingId = screen.existingId,
                    buyRepository = buyRepo,
                    tradeSettings = tradeSettings,
                    formState = buyForm,
                    isEnglish = tradeSettings?.isEnglish ?: false,
                    parties = parties,
                    activeLoanChecker = { farmerName ->
                        lends.any { !it.isVoid && it.status != com.appwork.mandisamiti.domain.model.LendStatus.SETTLED && it.partyId.equals(farmerName, ignoreCase = true) }
                    },
                    onSaved = { entry, party ->
                        coroutineScope.launch {
                            if (screen.existingId == null) {
                                val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                                val advance = com.appwork.mandisamiti.domain.model.CashTransaction(
                                    id = com.appwork.mandisamiti.domain.id.IdGenerator.newId(),
                                    shopId = entry.shopId,
                                    partyId = entry.farmerId,
                                    transactionType = com.appwork.mandisamiti.domain.model.TransactionType.UDHAR_GIVEN,
                                    amountPaisa = entry.advancePaidPaisa,
                                    transactionDate = now,
                                    createdAt = now,
                                    updatedAt = now,
                                )
                                buyRepo.saveBuyCheckout(
                                    entry, party,
                                    if (advance.amountPaisa > 0) listOf(advance) else emptyList(),
                                    buyForm.buildLoanCut(entry.shopId, now),
                                )

                                val isSound = shopRepo.getShopProfileStream().first()?.isSoundEnabled ?: true
                                val weightQtl = com.appwork.mandisamiti.domain.math.MandiMathEngine.gramsToQuintals(entry.totalWeightGrams)
                                val amountRs = com.appwork.mandisamiti.domain.math.MandiMathEngine.paisaToRupeesString(entry.netPayablePaisa)
                                val speech = "कुल वजन $weightQtl क्विंटल, कुल राशि ₹$amountRs।"
                                ttsManager.speak(speech, isSound)
                            } else {
                                buyRepo.editBuyEntry(entry)
                            }
                            syncScheduler.scheduleOneTimeSync()
                            currentScreen = if (screen.existingId == null) {
                                Screen.BuyPayment(entry.id)
                            } else {
                                Screen.BuyReceipt(entry.id)
                            }
                        }
                    },
                    onBack = { currentScreen = screen.backTarget() }
                )
            }
            is Screen.BuyPayment -> {
                MandiBackHandler { currentScreen = screen.backTarget() }
                var buyEntry by remember { mutableStateOf<com.appwork.mandisamiti.domain.model.BuyEntry?>(null) }
                var paymentParty by remember { mutableStateOf<com.appwork.mandisamiti.domain.model.Party?>(null) }
                LaunchedEffect(screen.entryId) {
                    buyEntry = buyRepo.getBuyEntryById(screen.entryId)
                    paymentParty = buyEntry?.farmerId?.let { partyRepo.getPartyById(it) }
                }
                buyEntry?.let { entry ->
                    com.appwork.mandisamiti.ui.buy.BuyPaymentScreen(
                        entry = entry,
                        farmerName = paymentParty?.name ?: entry.farmerId,
                        isEnglish = tradeSettings?.isEnglish ?: false,
                        onSavePayment = { paidAmtPaisa ->
                            coroutineScope.launch {
                                val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                                val tx = com.appwork.mandisamiti.domain.model.CashTransaction(
                                    id = com.appwork.mandisamiti.domain.id.IdGenerator.newId(),
                                    shopId = entry.shopId,
                                    partyId = entry.farmerId,
                                    transactionType = com.appwork.mandisamiti.domain.model.TransactionType.UDHAR_GIVEN,
                                    amountPaisa = paidAmtPaisa,
                                    transactionDate = now,
                                    paymentMode = com.appwork.mandisamiti.domain.model.PaymentMode.CASH,
                                    remarks = "खरीद भुगतान: पर्ची #${entry.slipNo ?: ""}",
                                    createdAt = now,
                                    updatedAt = now,
                                    sourceEntryType = "BUY",
                                    sourceEntryId = entry.id,
                                )
                                cashRepo.recordTransaction(tx)
                                val updated = entry.copy(
                                    paidPaisa = entry.paidPaisa + paidAmtPaisa,
                                    updatedAt = now,
                                    revision = entry.revision + 1L
                                )
                                buyRepo.editBuyEntry(updated)
                                syncScheduler.scheduleOneTimeSync()
                                currentScreen = Screen.BuyReceipt(entry.id, screen.returnScreen)
                            }
                        },
                        onSkip = {
                            currentScreen = Screen.BuyReceipt(entry.id, screen.returnScreen)
                        }
                    )
                }
            }
            is Screen.BuyList -> {
                MandiBackHandler { currentScreen = screen.backTarget() }
                val buys by buyRepo.getBuyEntriesStream(shopId ?: "").collectAsState(initial = emptyList())
                val parties by partyRepo.getPartiesStream(shopId ?: "").collectAsState(initial = emptyList())
                BuyTransactionsListScreen(
                    entries = buys,
                    partyNamesById = parties.associate { it.id to it.name },
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
                var receiptParty by remember { mutableStateOf<com.appwork.mandisamiti.domain.model.Party?>(null) }
                val receiptShop by shopRepo.getShopProfileStream().collectAsState(initial = null)
                val allCashTransactions by cashRepo.getTransactionsByShopStream(shopId ?: "").collectAsState(initial = emptyList())
                val gallaCashPaisa = remember(allCashTransactions) {
                    com.appwork.mandisamiti.domain.cash.CashDrawer.balancePaisa(allCashTransactions)
                }
                LaunchedEffect(screen.id) {
                    buyEntry = buyRepo.getBuyEntryById(screen.id)
                    receiptParty = buyEntry?.farmerId?.let { partyRepo.getPartyById(it) }
                }
                BuyTransactionReceiptScreen(
                    id = screen.id,
                    entry = buyEntry,
                    farmerName = receiptParty?.name ?: buyEntry?.farmerId.orEmpty(),
                    shopName = receiptShop?.shopName.orEmpty(),
                    mandiName = receiptShop?.mandiName.orEmpty(),
                    partyVillage = receiptParty?.village,
                    gallaCashPaisa = gallaCashPaisa,
                    onSettleCash = { amountPaisa ->
                        val entryToSettle = buyEntry ?: return@BuyTransactionReceiptScreen
                        val currentShop = shopId ?: return@BuyTransactionReceiptScreen
                        coroutineScope.launch {
                            val newPaid = entryToSettle.paidPaisa + amountPaisa
                            val updatedBuy = entryToSettle.copy(
                                paidPaisa = newPaid,
                                updatedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                                revision = entryToSettle.revision + 1
                            )
                            buyRepo.editBuyEntry(updatedBuy)

                            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                            val slipRef = entryToSettle.slipNo?.let { "#$it" } ?: entryToSettle.id.takeLast(5)
                            val tx = com.appwork.mandisamiti.domain.model.CashTransaction(
                                id = com.appwork.mandisamiti.domain.id.IdGenerator.newId(),
                                shopId = currentShop,
                                partyId = entryToSettle.farmerId,
                                transactionType = com.appwork.mandisamiti.domain.model.TransactionType.UDHAR_GIVEN,
                                amountPaisa = amountPaisa,
                                paymentMode = com.appwork.mandisamiti.domain.model.PaymentMode.CASH,
                                transactionDate = now,
                                remarks = "खरीद पर्ची $slipRef भुगतान",
                                createdAt = now,
                                updatedAt = now,
                                sourceEntryType = "BUY",
                                sourceEntryId = entryToSettle.id
                            )
                            cashRepo.recordTransaction(tx)
                            syncScheduler.scheduleOneTimeSync()
                            buyEntry = buyRepo.getBuyEntryById(screen.id)
                        }
                    },
                    onShareWhatsApp = { text -> whatsAppShareManager.shareText(text, null) },
                    onEdit = { currentScreen = Screen.BuyEntry(it) },
                    onNewBuy = { currentScreen = Screen.BuyEntry(null) },
                    isEnglish = tradeSettings?.isEnglish ?: false,
                    onBack = { currentScreen = screen.backTarget() }
                )
            }
            is Screen.SellEntry -> {
                var stockLines by remember(shopId) { mutableStateOf<List<com.appwork.mandisamiti.domain.model.StockLine>>(emptyList()) }
                val parties by partyRepo.getPartiesStream(shopId ?: "").collectAsState(initial = emptyList())
                val lends by lendRepo.getLendEntriesStream(shopId ?: "").collectAsState(initial = emptyList())
                val sellForm = remember(tradeSettings) { com.appwork.mandisamiti.ui.sell.SellEntryFormState(tradeSettings) }
                val buyerPartyId = com.appwork.mandisamiti.ui.buy.BuyPartyResolver.existing(parties, sellForm.selectedBuyerId, sellForm.phone)?.id
                LaunchedEffect(buyerPartyId, lends) {
                    val open = lends
                        .filter { !it.isVoid && it.partyId == buyerPartyId && it.lendType == com.appwork.mandisamiti.domain.model.LendType.MONEY }
                        .sortedBy { it.entryDate }
                    var totalDebt = 0L
                    for (lend in open) {
                        val live = lendRepo.getSettlements(lend.id).filter { !it.isVoid }
                        val principal = (lend.amountPaisa - live.sumOf { it.principalPaisa }).coerceAtLeast(0L)
                        if (principal <= 0L) continue
                        val rate = lend.monthlyRatePercent
                        val interest = com.appwork.mandisamiti.domain.math.RuralInterestEngine.interestDue(
                            lend.amountPaisa, rate, lend.entryDate, live, kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                        )
                        totalDebt += (principal + interest)
                    }
                    sellForm.setLoan(totalDebt)

                    if (buyerPartyId != null) {
                        val partyCash = cashRepo.getTransactionsByPartyStream(buyerPartyId).first()
                        val deposit = partyCash.filter { !it.isVoid && it.transactionType == com.appwork.mandisamiti.domain.model.TransactionType.JAMA_RECEIVED }.sumOf { it.amountPaisa }
                        sellForm.setDeposit(deposit)
                    } else {
                        sellForm.setDeposit(0L)
                    }
                }
                LaunchedEffect(shopId) {
                    if (shopId != null) {
                        stockLines = stockUseCase.lines(shopId)
                    }
                }
                SellEntryScreen(
                    shopId = shopId ?: "",
                    existingId = screen.existingId,
                    sellRepository = sellRepo,
                    tradeSettings = tradeSettings,
                    formState = sellForm,
                    isEnglish = tradeSettings?.isEnglish ?: false,
                    stockLines = stockLines,
                    parties = parties,
                    onSaved = { entry, party ->
                        coroutineScope.launch {
                            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                            if (party != null) {
                                partyRepo.saveParty(party)
                            }
                            if (screen.existingId == null) {
                                sellRepo.saveSellEntry(entry)
                                if (entry.receivedPaisa > 0L) {
                                    val advanceTx = com.appwork.mandisamiti.domain.model.CashTransaction(
                                        id = com.appwork.mandisamiti.domain.id.IdGenerator.newId(),
                                        shopId = entry.shopId,
                                        partyId = entry.buyerId,
                                        transactionType = com.appwork.mandisamiti.domain.model.TransactionType.JAMA_RECEIVED,
                                        amountPaisa = entry.receivedPaisa,
                                        transactionDate = now,
                                        paymentMode = com.appwork.mandisamiti.domain.model.PaymentMode.CASH,
                                        remarks = "बिक्री अग्रिम: पर्ची #${entry.slipNo ?: ""}",
                                        createdAt = now,
                                        updatedAt = now,
                                        sourceEntryType = "SELL",
                                        sourceEntryId = entry.id,
                                    )
                                    cashRepo.recordTransaction(advanceTx)
                                }

                                val isSound = shopRepo.getShopProfileStream().first()?.isSoundEnabled ?: true
                                val weightQtl = com.appwork.mandisamiti.domain.math.MandiMathEngine.gramsToQuintals(entry.totalWeightGrams)
                                val amountRs = com.appwork.mandisamiti.domain.math.MandiMathEngine.paisaToRupeesString(entry.totalReceivablePaisa)
                                val speech = "कुल वजन $weightQtl क्विंटल, कुल राशि ₹$amountRs।"
                                ttsManager.speak(speech, isSound)
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
                val parties by partyRepo.getPartiesStream(shopId ?: "").collectAsState(initial = emptyList())
                SellTransactionsListScreen(
                    entries = sells,
                    partyNamesById = parties.associate { it.id to it.name },
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
                var receiptParty by remember { mutableStateOf<com.appwork.mandisamiti.domain.model.Party?>(null) }
                val receiptShop by shopRepo.getShopProfileStream().collectAsState(initial = null)
                val sellCashTransactions by cashRepo.getTransactionsByShopStream(shopId ?: "").collectAsState(initial = emptyList())
                val sellGallaCashPaisa = remember(sellCashTransactions) {
                    com.appwork.mandisamiti.domain.cash.CashDrawer.balancePaisa(sellCashTransactions)
                }
                LaunchedEffect(screen.id) {
                    sellEntry = sellRepo.getSellEntryById(screen.id)
                    receiptParty = sellEntry?.buyerId?.let { partyRepo.getPartyById(it) }
                }
                SellTransactionReceiptScreen(
                    id = screen.id,
                    entry = sellEntry,
                    buyerName = receiptParty?.name ?: sellEntry?.buyerId.orEmpty(),
                    shopName = receiptShop?.shopName.orEmpty(),
                    mandiName = receiptShop?.mandiName.orEmpty(),
                    partyVillage = receiptParty?.village,
                    gallaCashPaisa = sellGallaCashPaisa,
                    isEnglish = tradeSettings?.isEnglish ?: false,
                    onSettleCash = { amountPaisa ->
                        val current = sellEntry ?: return@SellTransactionReceiptScreen
                        val currentShop = shopId ?: return@SellTransactionReceiptScreen
                        coroutineScope.launch {
                            val fresh = sellRepo.getSellEntryById(current.id) ?: return@launch
                            if (fresh.isVoid) return@launch
                            val applied = minOf(amountPaisa, fresh.balanceDuePaisa)
                            if (applied <= 0L) return@launch
                            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                            sellRepo.editSellEntry(
                                fresh.copy(
                                    receivedPaisa = fresh.receivedPaisa + applied,
                                    balanceDuePaisa = fresh.balanceDuePaisa - applied,
                                    updatedAt = now,
                                    revision = fresh.revision + 1
                                )
                            )
                            val slipRef = fresh.slipNo?.let { "#$it" } ?: fresh.id.takeLast(5)
                            cashRepo.recordTransaction(
                                com.appwork.mandisamiti.domain.model.CashTransaction(
                                    id = com.appwork.mandisamiti.domain.id.IdGenerator.newId(),
                                    shopId = currentShop,
                                    partyId = fresh.buyerId,
                                    transactionType = com.appwork.mandisamiti.domain.model.TransactionType.JAMA_RECEIVED,
                                    amountPaisa = applied,
                                    paymentMode = com.appwork.mandisamiti.domain.model.PaymentMode.CASH,
                                    transactionDate = now,
                                    remarks = "बिक्री पर्ची $slipRef नकद प्राप्त",
                                    createdAt = now,
                                    updatedAt = now,
                                    sourceEntryType = "SELL",
                                    sourceEntryId = fresh.id
                                )
                            )
                            syncScheduler.scheduleOneTimeSync()
                            sellEntry = sellRepo.getSellEntryById(screen.id)
                        }
                    },
                    onShareWhatsApp = { text -> whatsAppShareManager.shareText(text, null) },
                    onEdit = { currentScreen = Screen.SellEntry(it) },
                    onBack = { currentScreen = screen.backTarget() }
                )
            }
            is Screen.LendEntry -> {
                val parties by partyRepo.getPartiesStream(shopId ?: "").collectAsState(initial = emptyList())
                LoanAdvanceEntryScreen(
                    shopId = shopId ?: "",
                    parties = parties,
                    lendRepository = lendRepo,
                    isEnglish = tradeSettings?.isEnglish ?: false,
                    onLoanGiven = { entry, party ->
                        coroutineScope.launch {
                            if (party != null) {
                                partyRepo.saveParty(party)
                            }
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
                    onLoanReceived = { transaction, party ->
                        coroutineScope.launch {
                            if (party != null) partyRepo.saveParty(party)
                            cashRepo.recordTransaction(transaction)
                            syncScheduler.scheduleOneTimeSync()
                            currentScreen = Screen.Home
                        }
                    },
                    onBack = { currentScreen = screen.backTarget() }
                )
            }
            is Screen.GrainLendEntry -> {
                val parties by partyRepo.getPartiesStream(shopId ?: "").collectAsState(initial = emptyList())
                LendEntryScreen(
                    shopId = shopId ?: "",
                    parties = parties,
                    lendRepository = lendRepo,
                    isEnglish = tradeSettings?.isEnglish ?: false,
                    grainOnly = true,
                    onSaved = { entry, party ->
                        coroutineScope.launch {
                            if (party != null) partyRepo.saveParty(party)
                            lendRepo.saveLendEntry(entry)
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
                val parties by partyRepo.getPartiesStream(shopId ?: "").collectAsState(initial = emptyList())
                LendTransactionsListScreen(
                    entries = lends,
                    partyNamesById = parties.associate { it.id to it.name },
                    onNew = { currentScreen = Screen.LendEntry },
                    onPartyClick = { partyId -> currentScreen = Screen.PartyLedger(partyId, returnScreen = Screen.LendList) },
                    getSettlements = { lendId -> lendRepo.getSettlements(lendId) },
                    onRepay = { lendId, amountPaisa, weightGrams, bagsCount, note, paymentMode ->
                        coroutineScope.launch {
                            try {
                                lendUseCase.repay(lendId, amountPaisa, weightGrams, bagsCount, note, paymentMode = paymentMode)
                            } catch (_: com.appwork.mandisamiti.domain.usecase.LendException) {
                                return@launch
                            }
                            syncScheduler.scheduleOneTimeSync()
                        }
                    },
                    onRepayMoney = { lendId, paymentPaisa, interestPaisa, calculatedPaisa ->
                        coroutineScope.launch {
                            try {
                                lendUseCase.repayWithInterest(lendId, paymentPaisa, interestPaisa, calculatedPaisa)
                            } catch (_: IllegalArgumentException) {
                                return@launch
                            }
                            syncScheduler.scheduleOneTimeSync()
                        }
                    },
                    isEnglish = tradeSettings?.isEnglish ?: false,
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
                var existing by remember(screen.existingId) { mutableStateOf<com.appwork.mandisamiti.domain.model.ExpenseEntry?>(null) }
                var loaded by remember(screen.existingId) { mutableStateOf(screen.existingId == null) }
                LaunchedEffect(screen.existingId) {
                    screen.existingId?.let { existing = expenseRepo.getExpenseById(it); loaded = true }
                }
                if (loaded) ExpenseEntryScreen(
                    shopId = shopId ?: "",
                    isEnglish = tradeSettings?.isEnglish ?: false,
                    existing = existing,
                    onOpenList = { currentScreen = Screen.ExpenseList },
                    onSaved = { entry ->
                        coroutineScope.launch {
                            val expenseDetails = entry.remarks?.trim().orEmpty()
                            val expenseLabel = expenseDetails.substringBefore('\n').ifBlank { entry.category.labelHi() }
                            val note = expenseDetails.substringAfter('\n', "").trim()
                            val baseRem = "खर्च: $expenseLabel${if (note.isBlank()) "" else " - $note"}"
                            if (screen.existingId != null) {
                                expenseRepo.editExpense(entry, baseRem)
                            } else {
                                expenseRepo.saveExpense(entry)
                                if (entry.paymentMode == com.appwork.mandisamiti.domain.model.ExpensePaymentMode.CASH) {
                                    cashRepo.recordTransaction(
                                        com.appwork.mandisamiti.domain.model.CashTransaction(
                                            id = com.appwork.mandisamiti.domain.id.IdGenerator.newId(),
                                            shopId = shopId ?: "",
                                            partyId = "EXPENSE_${entry.category.name}",
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
                            }
                            syncScheduler.scheduleOneTimeSync()
                            currentScreen = if (screen.existingId != null) Screen.ExpenseList else Screen.Home
                        }
                    },
                    onBack = { currentScreen = screen.backTarget() }
                )
            }
            is Screen.ExpenseList -> {
                MandiBackHandler { currentScreen = screen.backTarget() }
                val expenses by expenseRepo.getExpensesStream(shopId ?: "").collectAsState(initial = emptyList())
                com.appwork.mandisamiti.ui.expense.ExpenseListScreen(
                    entries = expenses,
                    isEnglish = tradeSettings?.isEnglish ?: false,
                    onOpen = { id -> currentScreen = Screen.ExpenseEntry(id) },
                    onBack = { currentScreen = Screen.Home },
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
            is Screen.SubscriptionContact -> {
                MandiBackHandler { currentScreen = screen.backTarget() }
                val quota by quotaRepo.getQuotaStatus(shopId).collectAsState(initial = null)
                val shopProfile by shopRepo.getShopProfileStream().collectAsState(initial = null)
                com.appwork.mandisamiti.ui.billing.SubscriptionContactScreen(
                    quota = quota,
                    shopName = shopProfile?.shopName,
                    isEnglish = tradeSettings?.isEnglish ?: false,
                    onBack = { currentScreen = Screen.Home }
                )
            }
        }
    }
}
