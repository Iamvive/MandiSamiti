package com.appwork.mandisamiti.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.ui.components.SoundboxTopBar
import com.appwork.mandisamiti.ui.dashboard.DashboardScreen
import com.appwork.mandisamiti.ui.ledger.KhataLedgerTabScreen
import com.appwork.mandisamiti.ui.navigation.MandiBottomBar
import com.appwork.mandisamiti.ui.navigation.NavigationTab
import com.appwork.mandisamiti.ui.settings.SettingsScreen
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    shopId: String,
    onNavigateToNewEntry: () -> Unit,
    onNavigateToPartyKhata: (Party) -> Unit,
    onNavigateToDayClosing: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

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
                pendingSyncCount = uiState.pendingSyncCount
            )
        },
        bottomBar = {
            MandiBottomBar(
                currentTab = uiState.currentTab,
                onTabSelected = { tab ->
                    if (tab == NavigationTab.GALLA) {
                        onNavigateToDayClosing()
                    } else {
                        viewModel.selectTab(tab)
                    }
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
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MandiBackground)
        ) {
            when (uiState.currentTab) {
                NavigationTab.DASHBOARD -> {
                    DashboardScreen(
                        uiState = uiState,
                        onNavigateToNewEntry = onNavigateToNewEntry,
                        onNavigateToPartyKhata = onNavigateToPartyKhata,
                        onNavigateToAllKhata = { viewModel.selectTab(NavigationTab.KHATA) },
                        isEnglish = uiState.isEnglish
                    )
                }

                NavigationTab.KHATA -> {
                    KhataLedgerTabScreen(
                        uiState = uiState,
                        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        onFilterSelected = { viewModel.onFilterSelected(it) },
                        onPartyClick = onNavigateToPartyKhata,
                        onCreateParty = { name, village, phone, type ->
                            coroutineScope.launch {
                                val newParty = viewModel.createParty(name, village, phone, type)
                                onNavigateToPartyKhata(newParty)
                            }
                        },
                        isEnglish = uiState.isEnglish
                    )
                }

                NavigationTab.GALLA -> {
                    // Handled via onNavigateToDayClosing
                }

                NavigationTab.SETTINGS -> {
                    SettingsScreen(
                        uiState = uiState,
                        isEnglish = uiState.isEnglish,
                        onLanguageToggle = { viewModel.setLanguage(it) },
                        onToggleSound = { viewModel.toggleSoundSetting() },
                        onSignOutClick = onSignOut
                    )
                }
            }
        }
    }
}


