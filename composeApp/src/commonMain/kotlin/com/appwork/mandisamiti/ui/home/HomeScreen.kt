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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import com.appwork.mandisamiti.ui.register.DailyCashRegisterScreen
import com.appwork.mandisamiti.ui.register.DailyRegisterViewModel
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
    onNavigateToDayClosing: () -> Unit = {},
    onSignOut: () -> Unit,
    registerViewModel: DailyRegisterViewModel? = null,
    onShareWhatsApp: ((String) -> Unit)? = null,
    needsLogin: Boolean = false,
    onReLogin: () -> Unit = {},
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
            if (needsLogin) ReLoginBanner(onReLogin)
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                            onUpdateTradeSettings = { viewModel.updateTradeSettings(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReLoginBanner(onReLogin: () -> Unit) {
    // Theme-aware colours (not the static light Mandi* aliases) so the banner is readable in dark mode too.
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(2.dp, shape)
            .clip(shape)
            .background(colors.surfaceVariant)
            .border(1.dp, colors.outline, shape)
            .clickable(role = Role.Button, onClick = onReLogin)
            .heightIn(min = 48.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "दोबारा लॉगिन करें",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface
        )
        Text(
            text = "आपकी एंट्री फ़ोन में सुरक्षित हैं",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
    }
}

