package com.appwork.mandisamiti.ui.home

import com.appwork.mandisamiti.ui.ledger.isValidPartyPhone
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.search.BilingualSearchEngine
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.StockLine
import com.appwork.mandisamiti.domain.repository.BuyRepository
import com.appwork.mandisamiti.domain.repository.ExpenseRepository
import com.appwork.mandisamiti.domain.repository.LendRepository
import com.appwork.mandisamiti.domain.repository.PartyRepository
import com.appwork.mandisamiti.domain.repository.SellRepository
import com.appwork.mandisamiti.domain.repository.ShopProfileRepository
import com.appwork.mandisamiti.domain.repository.TradeSettingsRepository
import com.appwork.mandisamiti.domain.usecase.StockInventoryUseCase
import com.appwork.mandisamiti.ui.dashboard.DashboardKpis
import com.appwork.mandisamiti.ui.dashboard.computeKpis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

import com.appwork.mandisamiti.ui.navigation.NavigationTab

enum class PartyFilter {
    ALL,
    RECEIVABLE, // 🔴 लेना है
    PAYABLE     // 🟢 देना है
}

data class PartyWithBalance(
    val party: Party,
    val balancePaisa: Long
)

data class HomeUiState(
    val shopProfile: ShopProfile? = null,
    val tradeSettings: com.appwork.mandisamiti.domain.model.TradeSettings = com.appwork.mandisamiti.domain.model.TradeSettings(),
    val totalMarketReceivablePaisa: Long = 0L,
    val totalFarmerPayablePaisa: Long = 0L,
    val unlinkedLendAmountPaisa: Long = 0L,
    val unlinkedLendCount: Int = 0,
    val kpis: DashboardKpis = DashboardKpis(0L, 0L, 0L, 0L, 0L),
    val stockLines: List<StockLine> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: PartyFilter = PartyFilter.ALL,
    val allParties: List<PartyWithBalance> = emptyList(),
    val filteredParties: List<PartyWithBalance> = emptyList(),
    val currentTab: NavigationTab = NavigationTab.DASHBOARD,
    val pendingSyncCount: Int = 0,
    val isEnglish: Boolean = false,
    val isLoading: Boolean = true
) {
    val isSoundEnabled: Boolean get() = shopProfile?.isSoundEnabled ?: true
    val allPartiesWithBalance: List<PartyWithBalance> get() = allParties
}

private const val PENDING_SYNC_POLL_MS = 5_000L

class HomeViewModel(
    private val shopId: String,
    private val shopProfileRepository: ShopProfileRepository,
    private val partyRepository: PartyRepository,
    private val tradeSettingsRepository: TradeSettingsRepository? = null,
    private val buyRepository: BuyRepository? = null,
    private val sellRepository: SellRepository? = null,
    private val lendRepository: LendRepository? = null,
    private val expenseRepository: ExpenseRepository? = null,
    private val stockUseCase: StockInventoryUseCase? = null,
    private val viewModelScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val onLocalWrite: () -> Unit = {},
    /** Entries saved on this phone that have not reached the server yet; null in tests and previews. */
    private val pendingSyncCountProvider: (suspend () -> Int)? = null
) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadShopProfileAndParties()
        observeDashboardData()
        observePendingSyncCount()
    }

    /** A cheap count query every few seconds, so the "waiting to upload" line clears soon after a sync lands. */
    private fun observePendingSyncCount() {
        val provider = pendingSyncCountProvider ?: return
        viewModelScope.launch {
            while (isActive) {
                runCatching { provider() }.onSuccess { count ->
                    if (count != _uiState.value.pendingSyncCount) {
                        _uiState.value = _uiState.value.copy(pendingSyncCount = count)
                    }
                }
                delay(PENDING_SYNC_POLL_MS)
            }
        }
    }

    /** Parties always follow the session's [shopId]; the profile row is only used for display. */
    private fun loadShopProfileAndParties() {
        shopProfileRepository.getShopProfileStream()
            .onEach { profile -> _uiState.value = _uiState.value.copy(shopProfile = profile) }
            .launchIn(viewModelScope)

        tradeSettingsRepository?.getTradeSettingsStream()
            ?.onEach { settings ->
                _uiState.value = _uiState.value.copy(
                    tradeSettings = settings,
                    isEnglish = settings.isEnglish
                )
            }
            ?.launchIn(viewModelScope)

        observeParties(shopId)
    }

    private fun observeDashboardData() {
        if (buyRepository == null && sellRepository == null && lendRepository == null && expenseRepository == null) {
            return
        }

        val buysFlow = buyRepository?.getBuyEntriesStream(shopId) ?: flowOf(emptyList())
        val sellsFlow = sellRepository?.getSellEntriesStream(shopId) ?: flowOf(emptyList())
        val lendsFlow = lendRepository?.getLendEntriesStream(shopId) ?: flowOf(emptyList())
        val expensesFlow = expenseRepository?.getExpensesStream(shopId) ?: flowOf(emptyList())

        combine(buysFlow, sellsFlow, lendsFlow, expensesFlow) { buys, sells, lends, expenses ->
            val computedKpis = computeKpis(buys, sells, lends, expenses)
            val lines = stockUseCase?.lines(shopId) ?: emptyList()
            Triple(computedKpis, lines, lends)
        }.onEach { (kpis, lines, lends) ->
            val partyIds = _uiState.value.allParties.map { it.party.id }.toSet()
            val unlinkedMoneyLends = lends.filter {
                !it.isVoid &&
                it.status != com.appwork.mandisamiti.domain.model.LendStatus.SETTLED &&
                it.lendType == com.appwork.mandisamiti.domain.model.LendType.MONEY &&
                it.partyId !in partyIds
            }
            val unlinkedAmount = unlinkedMoneyLends.sumOf { it.amountPaisa }
            val unlinkedCount = unlinkedMoneyLends.size

            var partyRec = 0L
            _uiState.value.allParties.forEach {
                if (it.balancePaisa > 0) partyRec += it.balancePaisa
            }

            _uiState.value = _uiState.value.copy(
                kpis = kpis,
                stockLines = lines,
                unlinkedLendAmountPaisa = unlinkedAmount,
                unlinkedLendCount = unlinkedCount,
                totalMarketReceivablePaisa = partyRec + unlinkedAmount
            )
        }.launchIn(viewModelScope)
    }

    private var partyBalanceJobs = mutableListOf<kotlinx.coroutines.Job>()

    private fun observeParties(shopId: String) {
        partyRepository.getPartiesStream(shopId)
            .onEach { parties ->
                partyBalanceJobs.forEach { it.cancel() }
                partyBalanceJobs.clear()

                val partyWithBalances = parties.map { party ->
                    PartyWithBalance(
                        party = party,
                        balancePaisa = 0L
                    )
                }

                _uiState.value = _uiState.value.copy(
                    allParties = partyWithBalances,
                    filteredParties = filterList(partyWithBalances, _uiState.value.searchQuery, _uiState.value.activeFilter),
                    isLoading = false
                )

                parties.forEach { party ->
                    val job = viewModelScope.launch {
                        partyRepository.getPartyBalanceStream(party.id).collect { balance ->
                            if (balance != null) {
                                val currentList = _uiState.value.allParties.toMutableList()
                                val index = currentList.indexOfFirst { it.party.id == party.id }
                                val pWithB = PartyWithBalance(party, balance.balancePaisa)
                                if (index >= 0) {
                                    currentList[index] = pWithB
                                } else {
                                    currentList.add(pWithB)
                                }

                                var rec = 0L
                                var pay = 0L
                                currentList.forEach {
                                    if (it.balancePaisa > 0) rec += it.balancePaisa
                                    if (it.balancePaisa < 0) pay += -it.balancePaisa
                                }

                                _uiState.value = _uiState.value.copy(
                                    allParties = currentList,
                                    totalMarketReceivablePaisa = rec + _uiState.value.unlinkedLendAmountPaisa,
                                    totalFarmerPayablePaisa = pay,
                                    filteredParties = filterList(currentList, _uiState.value.searchQuery, _uiState.value.activeFilter),
                                    isLoading = false
                                )
                            }
                        }
                    }
                    partyBalanceJobs.add(job)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredParties = filterList(_uiState.value.allParties, query, _uiState.value.activeFilter)
        )
    }

    fun onFilterSelected(filter: PartyFilter) {
        _uiState.value = _uiState.value.copy(
            activeFilter = filter,
            filteredParties = filterList(_uiState.value.allParties, _uiState.value.searchQuery, filter)
        )
    }

    fun selectTab(tab: NavigationTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    fun setLanguage(isEnglish: Boolean) {
        _uiState.value = _uiState.value.copy(isEnglish = isEnglish)
        val currentSettings = _uiState.value.tradeSettings
        viewModelScope.launch {
            tradeSettingsRepository?.saveTradeSettings(currentSettings.copy(isEnglish = isEnglish))
            onLocalWrite()
        }
    }

    fun toggleSoundSetting() {
        val currentProfile = _uiState.value.shopProfile ?: return
        val newSoundState = !currentProfile.isSoundEnabled
        viewModelScope.launch {
            shopProfileRepository.updateSoundSetting(currentProfile.id, newSoundState)
        }
    }

    fun updateTradeSettings(settings: com.appwork.mandisamiti.domain.model.TradeSettings) {
        viewModelScope.launch {
            tradeSettingsRepository?.saveTradeSettings(settings)
            onLocalWrite()
        }
    }

    suspend fun createParty(
        name: String,
        village: String?,
        phoneNumber: String?,
        partyType: PartyType
    ): Party {
        val cleanName = name.trim()
        require(cleanName.isNotBlank()) { "Party name cannot be blank" }
        val cleanPhone = phoneNumber?.trim()?.ifBlank { null }
        require(cleanPhone == null || isValidPartyPhone(cleanPhone)) { "Party phone must be a 10-digit mobile number" }
        val now = Clock.System.now().toEpochMilliseconds()
        val newParty = Party(
            id = IdGenerator.newId(),
            shopId = shopId,
            name = cleanName,
            village = village?.trim()?.ifBlank { null },
            phone = cleanPhone,
            partyType = partyType,
            createdAt = now,
            updatedAt = now
        )
        partyRepository.saveParty(newParty)
        onLocalWrite()
        return newParty
    }

    private fun filterList(
        list: List<PartyWithBalance>,
        query: String,
        filter: PartyFilter
    ): List<PartyWithBalance> {
        val cleanQuery = query.trim()
        return list.filter { item ->
            val matchesQuery = cleanQuery.isEmpty() ||
                    BilingualSearchEngine.matchesAny(
                        cleanQuery,
                        item.party.name,
                        item.party.village,
                        item.party.phone
                    )

            val matchesFilter = when (filter) {
                PartyFilter.ALL -> true
                PartyFilter.RECEIVABLE -> item.balancePaisa > 0
                PartyFilter.PAYABLE -> item.balancePaisa < 0
            }

            matchesQuery && matchesFilter
        }
    }
}
