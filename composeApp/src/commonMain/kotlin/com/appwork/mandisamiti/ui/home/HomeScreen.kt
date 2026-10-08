package com.appwork.mandisamiti.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToNewEntry: () -> Unit,
    onNavigateToPartyKhata: (Party) -> Unit,
    onNavigateToDayClosing: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

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
                onToggleSound = { viewModel.toggleSoundSetting() }
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
                        onAddNewPartyClick = {
                            onNavigateToPartyKhata(
                                Party(
                                    id = "new_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}",
                                    shopId = uiState.shopProfile?.id ?: "shop_default",
                                    name = "",
                                    partyType = PartyType.FARMER,
                                    createdAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                                    updatedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                                )
                            )
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


