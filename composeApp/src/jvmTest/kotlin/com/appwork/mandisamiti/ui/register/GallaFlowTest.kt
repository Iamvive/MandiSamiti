package com.appwork.mandisamiti.ui.register

import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.cash.CashDrawer
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GallaFlowTest {

    private val fixedNow = Instant.parse("2026-10-28T08:00:00Z")
    private val testClock = object : Clock {
        override fun now() = fixedNow
    }
    private val kolkataTz = TimeZone.of("Asia/Kolkata")

    @Test
    fun tcGal01_openingBalanceCarriedForwardFromPreviousDays() = runTest {
        val db = createTestDatabase()
        val io = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(db, ioDispatcher = io)
        val partyRepo = OfflineFirstPartyRepository(db, ioDispatcher = io)
        val cashRepo = OfflineFirstCashTransactionRepository(db, ioDispatcher = io, clock = testClock)

        val shopId = "shop-galla"
        val farmerId = "farmer-ramesh"

        shopRepo.saveShopProfile(
            ShopProfile(
                id = shopId,
                shopName = "जय किसान ट्रेडर्स",
                ownerName = "दीपक जी",
                mandiName = "हाथरस मंडी",
                phoneNumber = "9897000000",
                pinHash = "1234",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )
        partyRepo.saveParty(
            Party(
                id = farmerId,
                shopId = shopId,
                name = "रमेश चंद",
                village = "मुरसान",
                partyType = PartyType.FARMER,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        // Previous day transaction (yesterday: 2026-10-27)
        val yesterdayMs = Instant.parse("2026-10-27T10:00:00Z").toEpochMilliseconds()
        cashRepo.recordTransaction(
            CashTransaction(
                id = "tx-prev-1",
                shopId = shopId,
                partyId = farmerId,
                transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = 1_200_000L, // ₹12,000 opening
                paymentMode = PaymentMode.CASH,
                transactionDate = yesterdayMs,
                createdAt = yesterdayMs,
                updatedAt = yesterdayMs
            )
        )

        // Today transactions (2026-10-28)
        val todayMs = fixedNow.toEpochMilliseconds()
        cashRepo.recordTransaction(
            CashTransaction(
                id = "tx-today-in",
                shopId = shopId,
                partyId = farmerId,
                transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = 500_000L, // +₹5,000
                paymentMode = PaymentMode.CASH,
                transactionDate = todayMs,
                createdAt = todayMs,
                updatedAt = todayMs
            )
        )
        cashRepo.recordTransaction(
            CashTransaction(
                id = "tx-today-out",
                shopId = shopId,
                partyId = farmerId,
                transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = 200_000L, // -₹2,000
                paymentMode = PaymentMode.CASH,
                transactionDate = todayMs,
                createdAt = todayMs,
                updatedAt = todayMs
            )
        )

        val vm = DailyRegisterViewModel(
            shopId = shopId,
            cashRepository = cashRepo,
            partyRepository = partyRepo,
            shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope,
            clock = testClock,
            timeZone = kolkataTz
        )

        val state = vm.uiState.first { it.todayTransactions.size == 2 && it.openingCashPaisa > 0L }

        assertEquals(1_200_000L, state.openingCashPaisa) // ₹12,000 carried forward
        assertEquals(500_000L, state.todayCashInPaisa)   // ₹5,000
        assertEquals(200_000L, state.todayCashOutPaisa)  // ₹2,000
        assertEquals(1_500_000L, state.inHandCashDrawerPaisa) // ₹15,000 closing

        // Ensure yesterday's row is not in today's transaction list
        assertFalse(state.todayTransactions.any { it.transaction.id == "tx-prev-1" })
        assertEquals(2, state.todayTransactions.size)
    }

    @Test
    fun tcGal02_todayDirectCashInflow_incrementsCashInAndClosing() = runTest {
        val db = createTestDatabase()
        val io = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(db, ioDispatcher = io)
        val partyRepo = OfflineFirstPartyRepository(db, ioDispatcher = io)
        val cashRepo = OfflineFirstCashTransactionRepository(db, ioDispatcher = io, clock = testClock)

        val shopId = "shop-1"
        val partyId = "buyer-1"
        partyRepo.saveParty(Party(id = partyId, shopId = shopId, name = "अग्रवाल ट्रेडर्स", village = "हाथरस", partyType = PartyType.BUYER, createdAt = 1L, updatedAt = 1L))

        val vm = DailyRegisterViewModel(
            shopId = shopId,
            cashRepository = cashRepo,
            partyRepository = partyRepo,
            shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope,
            clock = testClock,
            timeZone = kolkataTz
        )

        // Record Direct Cash In (जमा): ₹15,000
        vm.recordDailyEntry(
            partyId = partyId,
            transactionType = TransactionType.JAMA_RECEIVED,
            amountRs = 15000L,
            remarks = "बिक्री नकद प्राप्ति"
        )

        val state = vm.uiState.first { it.todayTransactions.isNotEmpty() }
        assertEquals(1_500_000L, state.todayCashInPaisa)
        assertEquals(0L, state.todayCashOutPaisa)
        assertEquals(1_500_000L, state.inHandCashDrawerPaisa)
    }

    @Test
    fun tcGal03_todayDirectCashOutflow_incrementsCashOutAndReducesClosing() = runTest {
        val db = createTestDatabase()
        val io = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(db, ioDispatcher = io)
        val partyRepo = OfflineFirstPartyRepository(db, ioDispatcher = io)
        val cashRepo = OfflineFirstCashTransactionRepository(db, ioDispatcher = io, clock = testClock)

        val shopId = "shop-1"
        val partyId = "farmer-1"
        partyRepo.saveParty(Party(id = partyId, shopId = shopId, name = "सुनील कुमार", village = "इगलास", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L))

        // Opening balance in drawer: ₹10,000
        cashRepo.recordTransaction(
            CashTransaction(
                id = "tx-init",
                shopId = shopId,
                partyId = partyId,
                transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = 1_000_000L,
                paymentMode = PaymentMode.CASH,
                transactionDate = fixedNow.toEpochMilliseconds(),
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val vm = DailyRegisterViewModel(
            shopId = shopId,
            cashRepository = cashRepo,
            partyRepository = partyRepo,
            shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope,
            clock = testClock,
            timeZone = kolkataTz
        )

        // Record Cash Outflow (निकासी / पेशगी): ₹3,500
        vm.recordDailyEntry(
            partyId = partyId,
            transactionType = TransactionType.UDHAR_GIVEN,
            amountRs = 3500L,
            remarks = "डीजल हेतु नकद पेशगी"
        )

        val state = vm.uiState.first { it.todayTransactions.size == 2 }
        assertEquals(1_000_000L, state.todayCashInPaisa)
        assertEquals(350_000L, state.todayCashOutPaisa)
        assertEquals(650_000L, state.inHandCashDrawerPaisa) // ₹10,000 - ₹3,500 = ₹6,500
    }

    @Test
    fun tcGal04_expenseOutflow_reducesCashDrawer() = runTest {
        val db = createTestDatabase()
        val io = StandardTestDispatcher(testScheduler)
        val cashRepo = OfflineFirstCashTransactionRepository(db, ioDispatcher = io, clock = testClock)

        val shopId = "shop-1"
        // Seed initial cash: ₹5,000
        cashRepo.recordTransaction(
            CashTransaction(
                id = "tx-open",
                shopId = shopId,
                partyId = "cash-desk",
                transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = 500_000L,
                paymentMode = PaymentMode.CASH,
                transactionDate = fixedNow.toEpochMilliseconds(),
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        // Simulate Cash Expense (Hammali: ₹800 recorded as UDHAR_GIVEN)
        cashRepo.recordTransaction(
            CashTransaction(
                id = "exp-hammali-1",
                shopId = shopId,
                partyId = "HAMMALI",
                transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = 80_000L, // ₹800
                paymentMode = PaymentMode.CASH,
                transactionDate = fixedNow.toEpochMilliseconds(),
                remarks = "खर्च: हम्माली मजदूर टोली",
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val window = CashDrawer.dayWindow(fixedNow, kolkataTz)
        val allTx = cashRepo.getTransactionsByShopStream(shopId).first()
        val summary = CashDrawer.summarize(allTx, window)

        assertEquals(500_000L, summary.cashInPaisa)
        assertEquals(80_000L, summary.cashOutPaisa)
        assertEquals(420_000L, summary.closingPaisa) // ₹4,200
    }

    @Test
    fun tcGal05_lendOutflowAndRepayInflow_reconcilesCashDrawer() = runTest {
        val db = createTestDatabase()
        val io = StandardTestDispatcher(testScheduler)
        val cashRepo = OfflineFirstCashTransactionRepository(db, ioDispatcher = io, clock = testClock)

        val shopId = "shop-1"
        val farmerId = "farmer-ramesh"

        // Initial cash ₹20,000
        cashRepo.recordTransaction(
            CashTransaction(
                id = "tx-seed",
                shopId = shopId,
                partyId = "cash-desk",
                transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = 2_000_000L,
                paymentMode = PaymentMode.CASH,
                transactionDate = fixedNow.toEpochMilliseconds(),
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        // 1. Money Loan advanced to farmer: ₹4,000 cash out
        cashRepo.recordTransaction(
            CashTransaction(
                id = "tx-lend-out",
                shopId = shopId,
                partyId = farmerId,
                transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = 400_000L,
                paymentMode = PaymentMode.CASH,
                transactionDate = fixedNow.toEpochMilliseconds(),
                remarks = "उधार ऋण: खाद बीज",
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        // 2. Loan repayment by farmer: ₹1,500 cash in
        cashRepo.recordTransaction(
            CashTransaction(
                id = "tx-lend-repay",
                shopId = shopId,
                partyId = farmerId,
                transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = 150_000L,
                paymentMode = PaymentMode.CASH,
                transactionDate = fixedNow.toEpochMilliseconds(),
                remarks = "उधार वापसी जमा",
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val window = CashDrawer.dayWindow(fixedNow, kolkataTz)
        val allTx = cashRepo.getTransactionsByShopStream(shopId).first()
        val summary = CashDrawer.summarize(allTx, window)

        assertEquals(2_150_000L, summary.cashInPaisa) // 20,000 + 1,500
        assertEquals(400_000L, summary.cashOutPaisa)   // 4,000
        assertEquals(1_750_000L, summary.closingPaisa) // ₹17,500
    }

    @Test
    fun tcGal06_physicalCashReconciliation_computesExactDifference() = runTest {
        val db = createTestDatabase()
        val io = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(db, ioDispatcher = io)
        val partyRepo = OfflineFirstPartyRepository(db, ioDispatcher = io)
        val cashRepo = OfflineFirstCashTransactionRepository(db, ioDispatcher = io, clock = testClock)

        val shopId = "shop-recon"
        cashRepo.recordTransaction(
            CashTransaction(
                id = "tx-1",
                shopId = shopId,
                partyId = "cash-desk",
                transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = 1_000_000L, // Expected: ₹10,000
                paymentMode = PaymentMode.CASH,
                transactionDate = fixedNow.toEpochMilliseconds(),
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val vm = DailyRegisterViewModel(
            shopId = shopId,
            cashRepository = cashRepo,
            partyRepository = partyRepo,
            shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope,
            clock = testClock,
            timeZone = kolkataTz
        )

        vm.uiState.first { it.inHandCashDrawerPaisa == 1_000_000L }

        // 1. Exact match (₹10,000)
        val matchRecon = vm.reconcilePhysicalCash(10000L)
        assertEquals(CashReconciliationStatus.MATCH, matchRecon.status)
        assertEquals(0L, matchRecon.differencePaisa)

        // 2. Surplus cash (+₹500: ₹10,500 counted)
        val surplusRecon = vm.reconcilePhysicalCash(10500L)
        assertEquals(CashReconciliationStatus.SURPLUS, surplusRecon.status)
        assertEquals(50_000L, surplusRecon.differencePaisa)

        // 3. Shortage cash (-₹200: ₹9,800 counted)
        val shortageRecon = vm.reconcilePhysicalCash(9800L)
        assertEquals(CashReconciliationStatus.SHORTAGE, shortageRecon.status)
        assertEquals(-20_000L, shortageRecon.differencePaisa)
    }

    @Test
    fun tcGal07_dayClosingReportFormatting_includesAllMetricsAndReconciliation() = runTest {
        val db = createTestDatabase()
        val io = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(db, ioDispatcher = io)
        val partyRepo = OfflineFirstPartyRepository(db, ioDispatcher = io)
        val cashRepo = OfflineFirstCashTransactionRepository(db, ioDispatcher = io, clock = testClock)

        val shopId = "shop-closing"
        shopRepo.saveShopProfile(
            ShopProfile(
                id = shopId,
                shopName = "बालाजी ट्रेडर्स",
                ownerName = "मोहन लाल",
                mandiName = "अलीगढ़ मंडी",
                phoneNumber = "9837001122",
                pinHash = "1234",
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        cashRepo.recordTransaction(
            CashTransaction(
                id = "tx-c1",
                shopId = shopId,
                partyId = "cash-desk",
                transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = 250_000L, // ₹2,500
                paymentMode = PaymentMode.CASH,
                transactionDate = fixedNow.toEpochMilliseconds(),
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val vm = DailyRegisterViewModel(
            shopId = shopId,
            cashRepository = cashRepo,
            partyRepository = partyRepo,
            shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope,
            clock = testClock,
            timeZone = kolkataTz
        )

        vm.uiState.first { it.shopProfile != null && it.todayCashInPaisa == 250_000L }

        // Report without physical count
        val standardReport = vm.generateDayClosingReportText()
        assertTrue(standardReport.contains("बालाजी ट्रेडर्स"))
        assertTrue(standardReport.contains("अलीगढ़ मंडी"))
        assertTrue(standardReport.contains("2,500"))
        assertFalse(standardReport.contains("गल्ला मिलान"))

        // Report with physical count (matching ₹2,500)
        val verifiedReport = vm.generateDayClosingReportText(physicalCountRs = 2500L)
        assertTrue(verifiedReport.contains("गल्ला मिलान:* ₹2,500 (बिल्कुल बराबर)"))

        // Report with shortage (counted ₹2,400)
        val shortageReport = vm.generateDayClosingReportText(physicalCountRs = 2400L)
        assertTrue(shortageReport.contains("गल्ला मिलान:* ₹2,400 (कमी: -₹100)"))
    }

    @Test
    fun tcGal08_voiceAnnouncement_speaksClosingBalanceInHindi() = runTest {
        val db = createTestDatabase()
        val io = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(db, ioDispatcher = io)
        val partyRepo = OfflineFirstPartyRepository(db, ioDispatcher = io)
        val cashRepo = OfflineFirstCashTransactionRepository(db, ioDispatcher = io, clock = testClock)

        val tts = SoundboxTtsManager()

        val shopId = "shop-tts"
        shopRepo.saveShopProfile(
            ShopProfile(
                id = shopId,
                shopName = "दुकान",
                ownerName = "मालिक",
                mandiName = "मंडी",
                phoneNumber = "9837000000",
                pinHash = "1234",
                isSoundEnabled = true,
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        cashRepo.recordTransaction(
            CashTransaction(
                id = "tx-tts",
                shopId = shopId,
                partyId = "cash-desk",
                transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = 180_000L, // ₹1,800
                paymentMode = PaymentMode.CASH,
                transactionDate = fixedNow.toEpochMilliseconds(),
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val vm = DailyRegisterViewModel(
            shopId = shopId,
            cashRepository = cashRepo,
            partyRepository = partyRepo,
            shopProfileRepository = shopRepo,
            ttsManager = tts,
            viewModelScope = backgroundScope,
            clock = testClock,
            timeZone = kolkataTz
        )

        vm.uiState.first { it.inHandCashDrawerPaisa == 180_000L && it.isSoundEnabled }

        val spokenText = vm.announceDayClosingSummary()
        assertEquals("आज का दिन समाप्त। गल्ले में कुल नकदी ₹1,800 है।", spokenText)
    }

    @Test
    fun tcGal09_voidedTransactions_excludedFromDrawerTotals() = runTest {
        val db = createTestDatabase()
        val io = StandardTestDispatcher(testScheduler)
        val cashRepo = OfflineFirstCashTransactionRepository(db, ioDispatcher = io, clock = testClock)

        val shopId = "shop-void"
        val nowMs = fixedNow.toEpochMilliseconds()

        cashRepo.recordTransaction(
            CashTransaction(
                id = "valid-tx",
                shopId = shopId,
                partyId = "farmer-1",
                transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = 100_000L, // ₹1,000 valid
                paymentMode = PaymentMode.CASH,
                transactionDate = nowMs,
                createdAt = nowMs,
                updatedAt = nowMs
            )
        )

        cashRepo.recordTransaction(
            CashTransaction(
                id = "void-tx",
                shopId = shopId,
                partyId = "farmer-1",
                transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = 50_000L, // ₹500 wrong entry
                paymentMode = PaymentMode.CASH,
                transactionDate = nowMs,
                createdAt = nowMs,
                updatedAt = nowMs
            )
        )

        cashRepo.voidTransaction("void-tx", VoidReason.WRONG_ENTRY)

        val window = CashDrawer.dayWindow(fixedNow, kolkataTz)
        val allTx = cashRepo.getTransactionsByShopStream(shopId).first()
        val summary = CashDrawer.summarize(allTx, window)

        assertEquals(100_000L, summary.cashInPaisa) // Only ₹1,000, voided ₹500 excluded
        assertEquals(0L, summary.cashOutPaisa)
        assertEquals(100_000L, summary.closingPaisa)
    }
}
