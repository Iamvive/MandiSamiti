package com.appwork.mandisamiti.ui.home

import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.repository.PartyRepository
import com.appwork.mandisamiti.domain.repository.ShopProfileRepository
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

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
    val totalMarketReceivablePaisa: Long = 0L,
    val totalFarmerPayablePaisa: Long = 0L,
    val searchQuery: String = "",
    val activeFilter: PartyFilter = PartyFilter.ALL,
    val allParties: List<PartyWithBalance> = emptyList(),
    val filteredParties: List<PartyWithBalance> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val shopProfileRepository: ShopProfileRepository,
    private val partyRepository: PartyRepository,
    private val ttsManager: SoundboxTtsManager? = null,
    private val viewModelScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val partyBalanceJobs = mutableListOf<Job>()
    private val balancesMap = mutableMapOf<String, Long>()
    private var currentPartiesList: List<Party> = emptyList()

    init {
        loadShopProfileAndParties()
    }

    private fun loadShopProfileAndParties() {
        shopProfileRepository.getShopProfileStream()
            .onEach { profile ->
                _uiState.value = _uiState.value.copy(shopProfile = profile)
                if (profile != null) {
                    observeParties(profile.id)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeParties(shopId: String) {
        partyRepository.getPartiesStream(shopId)
            .onEach { parties ->
                currentPartiesList = parties
                partyBalanceJobs.forEach { it.cancel() }
                partyBalanceJobs.clear()

                recomputeUiState()

                parties.forEach { party ->
                    val job = viewModelScope.launch {
                        partyRepository.getPartyBalanceStream(party.id).collect { balance ->
                            if (balance != null) {
                                balancesMap[party.id] = balance.balancePaisa
                            } else {
                                balancesMap.remove(party.id)
                            }
                            recomputeUiState()
                        }
                    }
                    partyBalanceJobs.add(job)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun recomputeUiState() {
        val partyWithBalances = currentPartiesList.map { party ->
            PartyWithBalance(
                party = party,
                balancePaisa = balancesMap[party.id] ?: 0L
            )
        }

        var rec = 0L
        var pay = 0L
        partyWithBalances.forEach {
            if (it.balancePaisa > 0) rec += it.balancePaisa
            if (it.balancePaisa < 0) pay += -it.balancePaisa
        }

        _uiState.value = _uiState.value.copy(
            allParties = partyWithBalances,
            totalMarketReceivablePaisa = rec,
            totalFarmerPayablePaisa = pay,
            filteredParties = filterList(partyWithBalances, _uiState.value.searchQuery, _uiState.value.activeFilter),
            isLoading = false
        )
    }

    fun addNewParty(
        name: String,
        phone: String?,
        village: String?,
        partyType: PartyType,
        monthlyInterestRate: Double? = null
    ) {
        viewModelScope.launch {
            val profile = _uiState.value.shopProfile ?: shopProfileRepository.getShopProfileStream().firstOrNull()
            val shopId = profile?.id ?: "shop_mathura_default"
            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            val randomSuffix = (1000..9999).random()
            val prefix = if (partyType == PartyType.FARMER) "farmer" else "buyer"
            val newParty = Party(
                id = "${prefix}_${now}_$randomSuffix",
                shopId = shopId,
                name = name.trim(),
                phone = phone?.trim()?.takeIf { it.isNotEmpty() },
                village = village?.trim()?.takeIf { it.isNotEmpty() },
                partyType = partyType,
                monthlyInterestRate = monthlyInterestRate,
                createdAt = now,
                updatedAt = now
            )

            partyRepository.saveParty(newParty)
            val roleHindi = if (partyType == PartyType.FARMER) "किसान" else "व्यापारी"
            val isSoundEnabled = profile?.isSoundEnabled ?: true
            ttsManager?.speak("${newParty.name} जी का नया $roleHindi खाता जोड़ दिया गया है", isSoundEnabled)
        }
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

    fun toggleSoundSetting() {
        val currentProfile = _uiState.value.shopProfile ?: return
        val newSoundState = !currentProfile.isSoundEnabled
        viewModelScope.launch {
            shopProfileRepository.updateSoundSetting(currentProfile.id, newSoundState)
        }
    }

    private fun filterList(
        list: List<PartyWithBalance>,
        query: String,
        filter: PartyFilter
    ): List<PartyWithBalance> {
        val cleanQuery = query.trim().lowercase()
        return list.filter { item ->
            val matchesQuery = cleanQuery.isEmpty() ||
                    item.party.name.lowercase().contains(cleanQuery) ||
                    (item.party.village?.lowercase()?.contains(cleanQuery) == true) ||
                    (item.party.phone?.contains(cleanQuery) == true)

            val matchesFilter = when (filter) {
                PartyFilter.ALL -> true
                PartyFilter.RECEIVABLE -> item.balancePaisa > 0
                PartyFilter.PAYABLE -> item.balancePaisa < 0
            }

            matchesQuery && matchesFilter
        }
    }
}

