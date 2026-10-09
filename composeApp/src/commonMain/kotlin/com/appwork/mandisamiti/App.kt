package com.appwork.mandisamiti

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
    data object Register : Screen
    data object FirstSync : Screen
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
    var currentScreen by remember { mutableStateOf<Screen>(if (session != null) Screen.Home else Screen.Register) }
    var signOutInFlight by remember { mutableStateOf(false) }

    MandiSamitiTheme {
        val activeSession = session
        // Every shop-scoped screen needs a session; without one the only place to go is Register.
        val screen = if (activeSession == null) Screen.Register else currentScreen
        val shopId = activeSession?.shopId
        when (screen) {
            is Screen.Register -> {
                // Leaving this branch drops the remembered VM, so after logout Register starts fresh at PHONE.
                val registerViewModel = remember {
                    RegisterViewModel(
                        authRepository = authRepo,
                        ttsManager = ttsManager,
                        viewModelScope = coroutineScope
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
                    }
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
                        viewModelScope = coroutineScope,
                        onLocalWrite = { syncScheduler.scheduleOneTimeSync() }
                    )
                }
                val needsLogin by produceState(false, shopId) { value = syncEngine.needsLogin() }
                HomeScreen(
                    viewModel = homeViewModel,
                    shopId = shopId!!,
                    needsLogin = needsLogin,
                    onReLogin = {
                        // Data stays on the phone; AuthRepository.adopt keeps it when the same shop logs in again.
                        sessionStore.clear()
                        session = null
                    },
                    snackbarHostState = snackbarHostState,
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
                                            currentScreen = Screen.Register
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
                MandiBackHandler { currentScreen = Screen.Home }
                val ledgerViewModel = remember(shopId, screen.partyId) {
                    PartyLedgerViewModel(
                        shopId = shopId!!,
                        partyId = screen.partyId,
                        partyRepository = partyRepo,
                        cashRepository = cashRepo,
                        dealRepository = dealRepo,
                        shopProfileRepository = shopRepo,
                        ttsManager = ttsManager,
                        syncScheduler = syncScheduler,
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
                    }
                )
            }
        }
    }
}
