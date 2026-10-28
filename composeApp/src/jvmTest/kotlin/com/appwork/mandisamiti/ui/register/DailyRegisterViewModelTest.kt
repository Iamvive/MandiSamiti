package com.appwork.mandisamiti.ui.register

import app.cash.turbine.test
import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.math.MandiCurrencyFormatter
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import com.appwork.mandisamiti.domain.repository.PartyRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DailyRegisterViewModelTest {

    @Test
    fun testDailyRegisterCashDrawerReconciliation() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler) // repos and VM share the test scheduler: no real threads
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val ttsManager = SoundboxTtsManager()

        val shopId = "shop-1"
        val partyId = "farmer-1"

        shopRepo.saveShopProfile(
            ShopProfile(
                id = shopId,
                shopName = "श्री गणेश ट्रेडिंग",
                ownerName = "लाला जी",
                mandiName = "मथुरा मंडी",
                phoneNumber = "9837000000",
                pinHash = "1234",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val farmer = Party(
            id = partyId,
            shopId = shopId,
            name = "रामवीर सिंह",
            village = "राया",
            partyType = PartyType.FARMER,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        partyRepo.saveParty(farmer)

        val viewModel = DailyRegisterViewModel(
            shopId = shopId,
            cashRepository = cashRepo,
            partyRepository = partyRepo,
            shopProfileRepository = shopRepo,
            ttsManager = ttsManager,
            viewModelScope = backgroundScope
        )

        // 1. Record Cash Received (JAMA): ₹20,000
        viewModel.recordDailyEntry(
            partyId = partyId,
            transactionType = TransactionType.JAMA_RECEIVED,
            amountRs = 20000L,
            remarks = "बिक्री जमा"
        )

        // 2. Record Cash Advance (UDHAR): ₹5,000
        viewModel.recordDailyEntry(
            partyId = partyId,
            transactionType = TransactionType.UDHAR_GIVEN,
            amountRs = 5000L,
            remarks = "खाद पेशगी"
        )

        // Check cash register summary via uiState
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.todayTransactions.size < 2) {
                state = awaitItem()
            }
            assertEquals(2000000L, state.todayCashInPaisa) // ₹20,000
            assertEquals(500000L, state.todayCashOutPaisa)  // ₹5,000
            assertEquals(1500000L, state.inHandCashDrawerPaisa) // ₹15,000 net in drawer
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun gallaShowsOnlyTodayWithOpeningCarriedForward() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val now = Instant.parse("2026-10-08T05:30:00Z") // 11:00 IST
        val clock = object : Clock { override fun now() = now }
        val yesterday = Instant.parse("2026-10-07T10:00:00Z").toEpochMilliseconds()
        val today = Instant.parse("2026-10-08T04:00:00Z").toEpochMilliseconds()

        partyRepo.saveParty(Party(id = "farmer-1", shopId = "shop-1", name = "रामवीर सिंह", village = "राया", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L))
        fun cash(id: String, type: TransactionType, rupees: Long, at: Long) = CashTransaction(
            id = id, shopId = "shop-1", partyId = "farmer-1", transactionType = type,
            amountPaisa = rupees * 100, transactionDate = at, createdAt = at, updatedAt = at
        )
        cashRepo.recordTransaction(cash("y1", TransactionType.JAMA_RECEIVED, 10_000, yesterday))
        cashRepo.recordTransaction(cash("t1", TransactionType.JAMA_RECEIVED, 20_000, today))
        cashRepo.recordTransaction(cash("t2", TransactionType.UDHAR_GIVEN, 5_000, today))
        cashRepo.recordTransaction(cash("t3", TransactionType.JAMA_RECEIVED, 1_000, today))
        cashRepo.voidTransaction("t3", VoidReason.WRONG_ENTRY)

        val viewModel = DailyRegisterViewModel(
            shopId = "shop-1", cashRepository = cashRepo, partyRepository = partyRepo,
            shopProfileRepository = shopRepo, ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope, clock = clock, timeZone = TimeZone.of("Asia/Kolkata")
        )
        val expectedIds = setOf("t1", "t2", "t3")
        val state = viewModel.uiState.first { s ->
            s.todayTransactions.map { it.transaction.id }.toSet() == expectedIds && s.openingCashPaisa != 0L
        }

        assertEquals(1_000_000L, state.openingCashPaisa)
        assertEquals(2_000_000L, state.todayCashInPaisa)
        assertEquals(500_000L, state.todayCashOutPaisa)
        assertEquals(2_500_000L, state.inHandCashDrawerPaisa)
        assertEquals(expectedIds, state.todayTransactions.map { it.transaction.id }.toSet()) // voided stays visible
    }

    @Test
    fun transactionRecordedBeforePartiesEmitStillShowsThePartyName() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val realPartyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val now = Instant.parse("2026-10-08T05:30:00Z")
        val clock = object : Clock { override fun now() = now }
        realPartyRepo.saveParty(Party(id = "farmer-1", shopId = "shop-1", name = "रामवीर सिंह", village = "राया", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L))
        cashRepo.recordTransaction(
            CashTransaction(
                id = "t1", shopId = "shop-1", partyId = "farmer-1", transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = 100_000L, transactionDate = now.toEpochMilliseconds(), createdAt = 1L, updatedAt = 1L
            )
        )
        // Parties are held back until the test releases them, so the cash stream emits first.
        val partiesGate = CompletableDeferred<Unit>()
        val gatedPartyRepo = object : PartyRepository by realPartyRepo {
            override fun getPartiesStream(shopId: String) = flow {
                partiesGate.await()
                emitAll(realPartyRepo.getPartiesStream(shopId))
            }
        }

        val viewModel = DailyRegisterViewModel(
            shopId = "shop-1", cashRepository = cashRepo, partyRepository = gatedPartyRepo,
            shopProfileRepository = shopRepo, ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope, clock = clock, timeZone = TimeZone.of("Asia/Kolkata")
        )
        testScheduler.runCurrent() // the cash stream emits while parties are still gated
        partiesGate.complete(Unit)

        val state = viewModel.uiState.first { it.todayTransactions.isNotEmpty() && it.availableParties.isNotEmpty() }
        assertEquals("रामवीर सिंह", state.todayTransactions.single().partyName)
    }

    @Test
    fun testDayClosingReportGeneration() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val now = Instant.parse("2026-10-08T05:30:00Z")
        val clock = object : Clock { override fun now() = now }

        shopRepo.saveShopProfile(
            ShopProfile(
                id = "shop-1",
                shopName = "श्री गणेश ट्रेडिंग",
                ownerName = "लाला जी",
                mandiName = "मथुरा मंडी",
                phoneNumber = "9837000000",
                pinHash = "1234",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )
        partyRepo.saveParty(Party(id = "farmer-1", shopId = "shop-1", name = "रामवीर सिंह", village = "राया", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L))

        cashRepo.recordTransaction(
            CashTransaction(
                id = "t1", shopId = "shop-1", partyId = "farmer-1", transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = 150_000L, transactionDate = now.toEpochMilliseconds(), createdAt = 1L, updatedAt = 1L
            )
        )

        val viewModel = DailyRegisterViewModel(
            shopId = "shop-1", cashRepository = cashRepo, partyRepository = partyRepo,
            shopProfileRepository = shopRepo, ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope, clock = clock, timeZone = TimeZone.of("Asia/Kolkata")
        )

        val state = viewModel.uiState.first { it.todayTransactions.isNotEmpty() && it.shopProfile != null }
        assertEquals(150000L, state.todayCashInPaisa)

        viewModel.openDayClosingSummary()
        assertTrue(viewModel.uiState.value.isDayClosingSummaryOpen)

        val reportText = viewModel.generateDayClosingReportText()
        assertTrue(reportText.contains("श्री गणेश ट्रेडिंग"))
        assertTrue(reportText.contains("मथुरा मंडी"))
        assertTrue(reportText.contains("1,500")) // Cash In
        assertTrue(reportText.contains("2026-10-08"))

        viewModel.closeDayClosingSummary()
        assertEquals(false, viewModel.uiState.value.isDayClosingSummaryOpen)
    }

    @Test
    fun recordingAnEntryAsksForASync() = runTest {
        val database = createTestDatabase()
        val io = StandardTestDispatcher(testScheduler)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = io)
        partyRepo.saveParty(Party(id = "farmer-1", shopId = "shop-1", name = "रामवीर", village = null,
            partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L))
        var writes = 0
        val viewModel = DailyRegisterViewModel(
            shopId = "shop-1",
            cashRepository = OfflineFirstCashTransactionRepository(database, ioDispatcher = io),
            partyRepository = partyRepo,
            shopProfileRepository = OfflineFirstShopProfileRepository(database, ioDispatcher = io),
            ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope,
            onLocalWrite = { writes++ }
        )

        viewModel.recordDailyEntry(partyId = "farmer-1", transactionType = TransactionType.JAMA_RECEIVED, amountRs = 500L)
        viewModel.events.first()   // EntryRecorded: the save has finished

        assertEquals(1, writes)
    }

    @Test
    fun testPhysicalCashReconciliationLifecycle() = runTest {
        val database = createTestDatabase()
        val io = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = io)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = io)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = io)
        val ttsManager = SoundboxTtsManager()

        val shopId = "shop-recon"
        val partyId = "farmer-recon"
        partyRepo.saveParty(Party(id = partyId, shopId = shopId, name = "राधेश्याम", village = "मथुरा", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L))

        val viewModel = DailyRegisterViewModel(
            shopId = shopId,
            cashRepository = cashRepo,
            partyRepository = partyRepo,
            shopProfileRepository = shopRepo,
            ttsManager = ttsManager,
            viewModelScope = backgroundScope
        )

        // Record 10,000 cash in
        viewModel.recordDailyEntry(partyId = partyId, transactionType = TransactionType.JAMA_RECEIVED, amountRs = 10000L)
        viewModel.events.first()

        // Wait until inHandCashDrawerPaisa reflects 10,000 (1_000_000 paisa)
        val initialState = viewModel.uiState.first { it.inHandCashDrawerPaisa == 1_000_000L }

        // 1. Initial State: Uncounted
        assertEquals(null, initialState.physicalCountPaisa)
        assertEquals(BahiReconciliationStatus.NOT_COUNTED, initialState.reconciliationStatus)
        assertEquals(1_000_000L, initialState.effectiveClosingCashPaisa)
        assertEquals(0L, initialState.discrepancyPaisa)

        // 2. Exact match: Counted ₹10,000
        viewModel.setPhysicalCashCount(10000L)
        val matchState = viewModel.uiState.first { it.physicalCountPaisa == 1_000_000L }
        assertEquals(BahiReconciliationStatus.BALANCED, matchState.reconciliationStatus)
        assertEquals(0L, matchState.discrepancyPaisa)
        assertEquals(1_000_000L, matchState.effectiveClosingCashPaisa)
        assertEquals(matchState.totalJamaSidePaisa, matchState.totalNameSidePaisa)

        // 3. Shortage: Counted ₹9,500 (₹500 short)
        viewModel.setPhysicalCashCount(9500L)
        val shortState = viewModel.uiState.first { it.physicalCountPaisa == 950_000L }
        assertEquals(BahiReconciliationStatus.DISCREPANCY, shortState.reconciliationStatus)
        assertEquals(-50_000L, shortState.discrepancyPaisa)
        assertEquals(950_000L, shortState.effectiveClosingCashPaisa)
        assertEquals(1_000_000L, shortState.totalJamaSidePaisa)
        assertEquals(950_000L, shortState.totalNameSidePaisa)

        // 4. Surplus: Counted ₹10,200 (+₹200)
        viewModel.setPhysicalCashCount(10200L)
        val surplusState = viewModel.uiState.first { it.physicalCountPaisa == 1_020_000L }
        assertEquals(BahiReconciliationStatus.DISCREPANCY, surplusState.reconciliationStatus)
        assertEquals(20_000L, surplusState.discrepancyPaisa)
        assertEquals(1_020_000L, surplusState.effectiveClosingCashPaisa)
        assertEquals(1_000_000L, surplusState.totalJamaSidePaisa)
        assertEquals(1_020_000L, surplusState.totalNameSidePaisa)

        // 5. Reset count to null
        viewModel.setPhysicalCashCount(null)
        val resetState = viewModel.uiState.first { it.physicalCountPaisa == null }
        assertEquals(BahiReconciliationStatus.NOT_COUNTED, resetState.reconciliationStatus)
        assertEquals(1_000_000L, resetState.effectiveClosingCashPaisa)
    }

    @Test
    fun testNegativeCashStateDetection() = runTest {
        val database = createTestDatabase()
        val io = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = io)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = io)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = io)
        val ttsManager = SoundboxTtsManager()

        val shopId = "shop-neg"
        val partyId = "farmer-neg"
        partyRepo.saveParty(Party(id = partyId, shopId = shopId, name = "राधेश्याम", village = "मथुरा", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L))

        val viewModel = DailyRegisterViewModel(
            shopId = shopId,
            cashRepository = cashRepo,
            partyRepository = partyRepo,
            shopProfileRepository = shopRepo,
            ttsManager = ttsManager,
            viewModelScope = backgroundScope
        )

        // Record 11,500 cash out with 0 cash in
        viewModel.recordDailyEntry(partyId = partyId, transactionType = TransactionType.UDHAR_GIVEN, amountRs = 11500L)
        viewModel.events.first()

        val state = viewModel.uiState.first { it.inHandCashDrawerPaisa == -1_150_000L }
        assertTrue(state.isNegativeCash)
        assertEquals("-₹11,500", MandiCurrencyFormatter.formatRupees(state.inHandCashDrawerPaisa))
    }

    @Test
    fun testUnlinkedPartyFallbackUsesCategoryOrRemarksNotGenericKhata() = runTest {
        val database = createTestDatabase()
        val io = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = io)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = io)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = io)
        val ttsManager = SoundboxTtsManager()

        val shopId = "shop-fallback"
        val viewModel = DailyRegisterViewModel(
            shopId = shopId,
            cashRepository = cashRepo,
            partyRepository = partyRepo,
            shopProfileRepository = shopRepo,
            ttsManager = ttsManager,
            viewModelScope = backgroundScope
        )

        // Record entry with unlinked party id and custom remarks "हम्माली खर्च"
        viewModel.recordDailyEntry(
            partyId = "unknown-party-1",
            transactionType = TransactionType.UDHAR_GIVEN,
            amountRs = 500L,
            remarks = "हम्माली खर्च"
        )
        viewModel.events.first()

        val state = viewModel.uiState.first { it.todayTransactions.isNotEmpty() }
        val item = state.todayTransactions.first()
        assertEquals("हम्माली खर्च", item.partyName)
        assertTrue(item.partyName != "खाता")
    }
}
