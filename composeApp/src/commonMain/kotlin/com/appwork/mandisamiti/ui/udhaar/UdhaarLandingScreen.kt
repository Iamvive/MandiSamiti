package com.appwork.mandisamiti.ui.udhaar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.domain.math.MandiCurrencyFormatter
import com.appwork.mandisamiti.domain.model.LendDirection
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.repository.LendRepository
import com.appwork.mandisamiti.domain.repository.PartyRepository
import com.appwork.mandisamiti.domain.usecase.UdhaarUseCase
import com.appwork.mandisamiti.platform.MandiBackHandler
import com.appwork.mandisamiti.ui.lend.LendPartyResolver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class UdhaarTabData(
    val rows: List<UdhaarPersonRow>,
    val totalPrincipalPaisa: Long,
    val totalInterestPaisa: Long,
) {
    val totalDuePaisa: Long get() = totalPrincipalPaisa + totalInterestPaisa
}

data class UdhaarLandingUiState(
    val diya: UdhaarTabData,
    val liya: UdhaarTabData,
    val parties: List<Party> = emptyList(),
)

class UdhaarLandingViewModel(
    private val shopId: String?,
    private val lendRepository: LendRepository,
    private val partyRepository: PartyRepository,
    val udhaarUseCase: UdhaarUseCase? = null,
    coroutineScope: CoroutineScope? = null,
) {
    private val scope = coroutineScope ?: CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _uiState = MutableStateFlow<UdhaarLandingUiState?>(null)
    val uiState: StateFlow<UdhaarLandingUiState?> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        val effectiveShopId = shopId ?: ""
        val lendsFlow = lendRepository.getLendEntriesStream(effectiveShopId)
        val settlementsFlow = lendRepository.getSettlementsStream(effectiveShopId)
        val partiesFlow = partyRepository.getPartiesStream(effectiveShopId)

        combine(lendsFlow, settlementsFlow, partiesFlow) { lends, settlements, parties ->
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val diyaRows = buildUdhaarRows(LendDirection.GIVEN, parties, lends, settlements, nowMs)
            val liyaRows = buildUdhaarRows(LendDirection.TAKEN, parties, lends, settlements, nowMs)

            UdhaarLandingUiState(
                diya = UdhaarTabData(
                    rows = diyaRows,
                    totalPrincipalPaisa = diyaRows.sumOf { it.principalDuePaisa },
                    totalInterestPaisa = diyaRows.sumOf { it.interestDuePaisa },
                ),
                liya = UdhaarTabData(
                    rows = liyaRows,
                    totalPrincipalPaisa = liyaRows.sumOf { it.principalDuePaisa },
                    totalInterestPaisa = liyaRows.sumOf { it.interestDuePaisa },
                ),
                parties = parties,
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(scope)
    }

    suspend fun saveLoan(state: UdhaarAddFormState): Result<Unit> {
        val effectiveShopId = shopId ?: return Result.failure(IllegalStateException("No shop selected"))
        val useCase = udhaarUseCase ?: return Result.failure(IllegalStateException("UdhaarUseCase not available"))
        return try {
            val parties = partyRepository.getPartiesStream(effectiveShopId).first()
            val matchingParty = LendPartyResolver.existing(parties, state.selectedPartyId, state.phone)
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val partyToSave = LendPartyResolver.partyToSave(
                name = state.name,
                phone = state.phone,
                village = null,
                shopId = effectiveShopId,
                existing = matchingParty,
                now = nowMs,
            )
            val resolvedPartyId = if (partyToSave != null) {
                partyRepository.saveParty(partyToSave)
                partyToSave.id
            } else {
                matchingParty?.id ?: state.selectedPartyId ?: throw IllegalStateException("Unable to resolve party")
            }

            val loanForm = state.toForm(effectiveShopId, resolvedPartyId)
            useCase.createLoan(loanForm)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UdhaarLandingScreen(
    viewModel: UdhaarLandingViewModel,
    isEnglish: Boolean,
    initialPage: Int = 0,
    onPageChanged: (Int) -> Unit = {},
    onPartyClick: (partyId: String) -> Unit,
    onAddClick: () -> Unit = {},
    onBack: () -> Unit,
    onLoanSaved: () -> Unit = {},
) {
    MandiBackHandler(enabled = true) { onBack() }

    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onPageChanged(page)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEnglish) "Udhaar" else "उधार",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MandiTextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEnglish) "Back" else "पीछे",
                            tint = MandiTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MandiBackground,
                    titleContentColor = MandiTextPrimary,
                    navigationIconContentColor = MandiTextPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MandiPrimaryAction,
                contentColor = MandiPrimaryActionText,
                shape = RoundedCornerShape(14.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MandiPrimaryActionText,
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = {
                    Text(
                        text = if (isEnglish) "Add" else "जोड़ें",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        },
        containerColor = MandiBackground,
        contentColor = MandiTextPrimary
    ) { padding ->
        val state = uiState
        if (state == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MandiPrimaryAction)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = MandiSurface,
                        contentColor = MandiPrimaryAction,
                        indicator = { tabPositions ->
                            if (pagerState.currentPage < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                    color = MandiPrimaryAction
                                )
                            }
                        },
                        divider = {},
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MandiBorder, RoundedCornerShape(12.dp))
                    ) {
                        val tabs = listOf(
                            if (isEnglish) "Loan Given" else "उधार दिया",
                            if (isEnglish) "Loan Taken" else "उधार लिया"
                        )
                        tabs.forEachIndexed { index, title ->
                            val isSelected = pagerState.currentPage == index
                            Tab(
                                selected = isSelected,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) MandiTextPrimary else MandiTextSecondary
                                    )
                                }
                            )
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val isDiya = page == 0
                    val tabData = if (isDiya) state.diya else state.liya

                    if (tabData.rows.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val emptyText = if (isDiya) {
                                if (isEnglish) "No loans given yet" else "अभी कोई उधार दिया नहीं है"
                            } else {
                                if (isEnglish) "No loans taken yet" else "अभी कोई उधार लिया नहीं है"
                            }
                            Text(
                                text = emptyText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MandiTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item(key = "total_header") {
                                TotalCard(
                                    isDiya = isDiya,
                                    tabData = tabData,
                                    isEnglish = isEnglish
                                )
                            }

                            items(
                                items = tabData.rows,
                                key = { it.partyId }
                            ) { row ->
                                UdhaarPersonRowItem(
                                    row = row,
                                    isEnglish = isEnglish,
                                    onClick = { onPartyClick(row.partyId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        val currentDirection = if (pagerState.currentPage == 0) LendDirection.GIVEN else LendDirection.TAKEN
        UdhaarAddSheet(
            direction = currentDirection,
            parties = uiState?.parties.orEmpty(),
            isEnglish = isEnglish,
            onDismiss = { showAddSheet = false },
            onSave = { formState, onComplete ->
                coroutineScope.launch {
                    val result = viewModel.saveLoan(formState)
                    onComplete(result)
                    result.onSuccess {
                        showAddSheet = false
                        onLoanSaved()
                        val msg = if (isEnglish) "Loan recorded successfully" else "उधार सफलतापूर्वक दर्ज किया गया"
                        snackbarHostState.showSnackbar(msg)
                    }
                }
            }
        )
    }
}

@Composable
private fun TotalCard(
    isDiya: Boolean,
    tabData: UdhaarTabData,
    isEnglish: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MandiSurfaceElevated),
        border = BorderStroke(1.dp, MandiBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            val headlineLabel = when {
                isDiya && isEnglish -> "Total Loan Given"
                isDiya -> "कुल दिया गया उधार"
                isEnglish -> "Total Loan Taken"
                else -> "कुल लिया गया उधार"
            }
            Text(
                text = headlineLabel,
                style = MaterialTheme.typography.labelMedium.copy(color = MandiTextSecondary)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = MandiCurrencyFormatter.formatRupees(tabData.totalPrincipalPaisa),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MandiTextPrimary
                )
            )
            if (tabData.totalInterestPaisa > 0L) {
                Spacer(modifier = Modifier.height(4.dp))
                val interestLine = if (isEnglish) {
                    "+ ${MandiCurrencyFormatter.formatRupees(tabData.totalInterestPaisa)} interest · Total ${MandiCurrencyFormatter.formatRupees(tabData.totalDuePaisa)}"
                } else {
                    "+ ${MandiCurrencyFormatter.formatRupees(tabData.totalInterestPaisa)} ब्याज · कुल ${MandiCurrencyFormatter.formatRupees(tabData.totalDuePaisa)}"
                }
                Text(
                    text = interestLine,
                    style = MaterialTheme.typography.bodySmall.copy(color = MandiTextSecondary)
                )
            }
        }
    }
}

@Composable
private fun UdhaarPersonRowItem(
    row: UdhaarPersonRow,
    isEnglish: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        border = BorderStroke(1.dp, MandiBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val displayName = row.name ?: if (isEnglish) "Unknown" else "अज्ञात"
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MandiTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (row.interestOn) {
                        Text(
                            text = if (isEnglish) "Interest" else "ब्याज",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MandiTextMuted,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, MandiBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (!row.village.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = row.village,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MandiTextSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                val totalDue = row.principalDuePaisa + row.interestDuePaisa
                Text(
                    text = if (isEnglish) "Due ${MandiCurrencyFormatter.formatRupees(totalDue)}"
                           else "बाकी ${MandiCurrencyFormatter.formatRupees(totalDue)}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MandiTextPrimary
                    ),
                    maxLines = 1,
                    softWrap = false
                )
                if (row.interestDuePaisa > 0L) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isEnglish) "+ ${MandiCurrencyFormatter.formatRupees(row.interestDuePaisa)} interest"
                               else "+ ${MandiCurrencyFormatter.formatRupees(row.interestDuePaisa)} ब्याज",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MandiTextSecondary
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
