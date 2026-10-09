package com.appwork.mandisamiti.ui.register

import app.cash.turbine.test
import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.createTestDatabase
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
}
