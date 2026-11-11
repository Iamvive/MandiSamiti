package com.appwork.mandisamiti.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.ui.components.SoundboxTopBar
import com.appwork.mandisamiti.ui.components.SpeedDialFab
import com.appwork.mandisamiti.ui.dashboard.DashboardScreen
import com.appwork.mandisamiti.ui.ledger.KhataLedgerTabScreen
import com.appwork.mandisamiti.ui.navigation.MandiBottomBar
import com.appwork.mandisamiti.ui.navigation.NavigationTab
import com.appwork.mandisamiti.ui.stock.StockRegisterScreen
import com.appwork.mandisamiti.ui.register.DailyCashRegisterScreen
import com.appwork.mandisamiti.ui.register.DailyRegisterViewModel
import com.appwork.mandisamiti.ui.settings.SettingsScreen
import com.appwork.mandisamiti.ui.tutorial.MandiTutorial
import com.appwork.mandisamiti.ui.tutorial.MandiTutorialSheet
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    shopId: String,
    onNavigateToNewEntry: () -> Unit,
    onSyncNow: suspend () -> Result<Int>,
    onNavigateToPartyKhata: (Party) -> Unit,
    onNavigateToDayClosing: () -> Unit = {},
    onSignOut: () -> Unit,
    registerViewModel: DailyRegisterViewModel? = null,
    onShareWhatsApp: ((String) -> Unit)? = null,
    needsLogin: Boolean = false,
    onReLogin: () -> Unit = {},
    onNavigateToBuy: () -> Unit = {},
    onNavigateToSell: () -> Unit = {},
    onNavigateToLend: () -> Unit = {},
    onNavigateToExpense: () -> Unit = {},
    onNavigateToBuyList: () -> Unit = {},
    onNavigateToSellList: () -> Unit = {},
    onNavigateToLendList: () -> Unit = {},
    onNavigateToStockSheet: () -> Unit = {},
    stockBuys: List<com.appwork.mandisamiti.domain.model.BuyEntry> = emptyList(),
    stockSells: List<com.appwork.mandisamiti.domain.model.SellEntry> = emptyList(),
    stockPartyNames: Map<String, String> = emptyMap(),
    onNavigateToBuyReceipt: (String) -> Unit = {},
    onNavigateToSellReceipt: (String) -> Unit = {},
    onNavigateToQuotaPlans: () -> Unit = {},
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var activeTutorial by remember { mutableStateOf<MandiTutorial?>(null) }
    var isSyncing by remember { mutableStateOf(false) }

    val onSyncClick = {
        if (!isSyncing) {
            isSyncing = true
            coroutineScope.launch {
                val succeeded = try {
                    onSyncNow().isSuccess
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    false
                } finally {
                    isSyncing = false
                }
                snackbarHostState.showSnackbar(
                    message = when {
                        succeeded && uiState.isEnglish -> "Sync complete"
                        succeeded -> "सिंक पूरा हुआ"
                        uiState.isEnglish -> "Sync failed. Check your connection and try again."
                        else -> "सिंक नहीं हुआ। इंटरनेट जांचकर फिर कोशिश करें।"
                    },
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // Shown when the user taps the no-phone icon on a party card; "Add" runs the screen's own add-phone step.
    val promptAddPhone: (() -> Unit) -> Unit = { onAdd ->
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = if (uiState.isEnglish) "No phone number saved" else "फ़ोन नंबर नहीं है",
                actionLabel = if (uiState.isEnglish) "Add" else "जोड़ें",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onAdd()
        }
    }

    // When not on Dashboard, hardware/system back returns to Dashboard
    MandiBackHandler(enabled = uiState.currentTab != NavigationTab.DASHBOARD) {
        viewModel.selectTab(NavigationTab.DASHBOARD)
    }

    Scaffold(
        topBar = {
            SoundboxTopBar(
                shopName = uiState.shopProfile?.shopName ?: "",
                mandiLocation = uiState.shopProfile?.mandiName ?: "",
                isSoundEnabled = uiState.isSoundEnabled,
                onToggleSound = { viewModel.toggleSoundSetting() },
                isEnglish = uiState.isEnglish,
                onOpenSettings = { viewModel.selectTab(NavigationTab.SETTINGS) },
                onOpenHelp = {
                    activeTutorial = when (uiState.currentTab) {
                        NavigationTab.DASHBOARD -> MandiTutorial.DEAL_ENTRY
                        NavigationTab.PARCHI -> MandiTutorial.KHATA_STATEMENT
                        NavigationTab.GODOWN -> MandiTutorial.KHATA_STATEMENT
                        NavigationTab.GALLA -> MandiTutorial.GALLA_REGISTER
                        NavigationTab.SETTINGS -> MandiTutorial.SOUNDBOX_SETTINGS
                    }
                }
            )
        },
        bottomBar = {
            MandiBottomBar(
                currentTab = uiState.currentTab,
                onTabSelected = { tab ->
                    viewModel.selectTab(tab)
                },
                isEnglish = uiState.isEnglish
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MandiPrimaryAction,
                    contentColor = MandiPrimaryActionText,
                    actionColor = MandiPrimaryActionText,
                )
            }
        },
        containerColor = MandiBackground
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MandiBackground)
        ) {
            if (needsLogin) ReLoginBanner(onReLogin, isEnglish = uiState.isEnglish)
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (uiState.currentTab) {
                    NavigationTab.DASHBOARD -> {
                        DashboardScreen(
                            uiState = uiState,
                            onNavigateToBuy = onNavigateToBuy,
                            onNavigateToSell = onNavigateToSell,
                            onNavigateToLend = onNavigateToLend,
                            onNavigateToExpense = onNavigateToExpense,
                            onNavigateToBuyList = onNavigateToBuyList,
                            onNavigateToSellList = onNavigateToSellList,
                            onNavigateToLendList = onNavigateToLendList,
                            onNavigateToStockSheet = onNavigateToStockSheet,
                            onNavigateToGalla = { viewModel.selectTab(NavigationTab.GALLA) },
                            onNavigateToPartyKhata = onNavigateToPartyKhata,
                            onNavigateToAllKhata = { viewModel.selectTab(NavigationTab.PARCHI) },
                            onNavigateToBuyReceipt = onNavigateToBuyReceipt,
                            onNavigateToSellReceipt = onNavigateToSellReceipt,
                            onNavigateToNewEntry = onNavigateToNewEntry,
                            onNavigateToQuotaPlans = onNavigateToQuotaPlans,
                            onNavigateToSettings = { viewModel.selectTab(NavigationTab.SETTINGS) },
                            onPhoneNeeded = promptAddPhone,
                            needsLogin = needsLogin,
                            isEnglish = uiState.isEnglish
                        )
                    }

                    NavigationTab.PARCHI -> {
                        com.appwork.mandisamiti.ui.parchi.ParchiListScreen(
                            buys = uiState.buys,
                            sells = uiState.sells,
                            partyNamesById = uiState.allParties.associate { it.party.id to it.party.name },
                            parties = uiState.allParties.map { it.party },
                            gallaCashPaisa = uiState.rokdiBalancePaisa,
                            onSettleBuySlipCash = { buyId, cashAmountPaisa ->
                                viewModel.settleBuySlipCash(buyId, cashAmountPaisa)
                            },
                            onSettleSellSlipCash = { sellId, cashAmountPaisa ->
                                viewModel.settleSellSlipCash(sellId, cashAmountPaisa)
                            },
                            onOpenBuyReceipt = onNavigateToBuyReceipt,
                            onOpenSellReceipt = onNavigateToSellReceipt,
                            onNavigateToNewBuy = onNavigateToBuy,
                            onNavigateToNewSell = onNavigateToSell,
                            isEnglish = uiState.isEnglish
                        )
                    }

                    NavigationTab.GODOWN -> {
                        StockRegisterScreen(
                            stockLines = uiState.stockLines,
                            isEnglish = uiState.isEnglish,
                            buys = stockBuys,
                            sells = stockSells,
                            partyNames = stockPartyNames,
                            onOpenBuy = onNavigateToBuyReceipt,
                            onOpenSell = onNavigateToSellReceipt,
                            onSaveStockAudit = { commodityId, physicalBags, bagKg, reason, farmerCustodyBags ->
                                viewModel.saveStockAudit(commodityId, physicalBags, bagKg, reason, farmerCustodyBags)
                            }
                        )
                    }

                    NavigationTab.GALLA -> {
                        if (registerViewModel != null) {
                            DailyCashRegisterScreen(
                                viewModel = registerViewModel,
                                onNavigateBack = { viewModel.selectTab(NavigationTab.DASHBOARD) },
                                onShareWhatsApp = onShareWhatsApp,
                                isEnglish = uiState.isEnglish,
                                showBackButton = false
                            )
                        }
                    }

                    NavigationTab.SETTINGS -> {
                        SettingsScreen(
                            uiState = uiState,
                            isEnglish = uiState.isEnglish,
                            onLanguageToggle = { viewModel.setLanguage(it) },
                            onToggleSound = { viewModel.toggleSoundSetting() },
                            onSignOutClick = onSignOut,
                            onUpdateTradeSettings = { viewModel.updateTradeSettings(it) },
                            onSyncClick = onSyncClick,
                            isSyncing = isSyncing
                        )
                    }
                }
            }
        }
    }

    activeTutorial?.let { tutorial ->
        MandiTutorialSheet(
            initialTutorial = tutorial,
            isEnglish = uiState.isEnglish,
            onDismissRequest = { activeTutorial = null }
        )
    }
}

@Composable
private fun ReLoginBanner(onReLogin: () -> Unit, isEnglish: Boolean = false) {
    // Theme-aware colours (not the static light Mandi* aliases) so the banner is readable in dark mode too.
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(2.dp, shape)
            .clip(shape)
            .background(colors.surfaceVariant)
            .border(1.dp, colors.outline, shape)
            .clickable(role = Role.Button, onClick = onReLogin)
            .heightIn(min = 56.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (isEnglish) "Log in again" else "दोबारा लॉगिन करें",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Text(
                text = if (isEnglish) "Your entries are safe on this phone" else "आपकी एंट्री फ़ोन में सुरक्षित हैं",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }
        Button(
            onClick = onReLogin,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (isEnglish) "Log in" else "लॉगिन करें",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

