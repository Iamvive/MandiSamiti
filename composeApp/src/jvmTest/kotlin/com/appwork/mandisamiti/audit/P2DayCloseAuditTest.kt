package com.appwork.mandisamiti.audit

import com.appwork.mandisamiti.data.repository.OfflineFirstBuyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import com.appwork.mandisamiti.ui.register.BahiReconciliationStatus
import com.appwork.mandisamiti.ui.register.DailyRegisterViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * X4 Audit Test Suite for Finding P2 (Day Close Cash Drawer Reconciliation).
 * Verifies:
 * - Credit purchases (where farmer receives no cash) do NOT distort cash out / naame totals.
 * - Days with equal cash inflows and cash outflows open with 0 discrepancy, even in the presence of large credit purchases.
 * - Physical count matching book cash yields BALANCED (₹0 अंतर) without prior false warnings.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class P2DayCloseAuditTest {

    private suspend fun seedShopAndParty(db: AppDatabase, shopId: String = "shop-p2", partyId: String = "party-p2") {
        db.appDatabaseQueries.insertShopProfile(
            shopId, "दुकान", "मालिक", "मंडी", null, "9876543210", "", 1.5, 1L, 0L, 0L, 1L
        )
        OfflineFirstPartyRepository(db).saveParty(
            Party(
                id = partyId,
                shopId = shopId,
                name = "TEST रमेश",
                phone = "9876543211",
                village = "मथुरा",
                partyType = PartyType.FARMER,
                createdAt = 1L,
                updatedAt = 1L
            )
        )
    }

    @Test
    fun auditP2_creditPurchasesDoNotCreateDiscrepancyOnBalancedCashDay() = runTest {
        val db = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(db, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(db, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(db, ioDispatcher = ioDispatcher)
        val buyRepo = OfflineFirstBuyRepository(db, ioDispatcher = ioDispatcher)

        seedShopAndParty(db)

        val fixedNow = Instant.parse("2026-10-08T05:30:00Z")
        val clock = object : Clock { override fun now() = fixedNow }
        val nowMs = fixedNow.toEpochMilliseconds()

        // 1. Record ₹7,000 cash in (JAMA / buyer payment)
        cashRepo.recordTransaction(
            CashTransaction(
                id = "cash-in-1",
                shopId = "shop-p2",
                partyId = "party-p2",
                amountPaisa = 700_000L, // ₹7,000
                transactionType = TransactionType.JAMA_RECEIVED,
                paymentMode = PaymentMode.CASH,
                transactionDate = nowMs,
                remarks = "क्रेता नकद भुगतान",
                createdAt = nowMs,
                updatedAt = nowMs
            )
        )

        // 2. Record ₹7,000 cash out (Expense / advance / udhar given)
        cashRepo.recordTransaction(
            CashTransaction(
                id = "cash-out-1",
                shopId = "shop-p2",
                partyId = "party-p2",
                amountPaisa = 700_000L, // ₹7,000
                transactionType = TransactionType.UDHAR_GIVEN,
                paymentMode = PaymentMode.CASH,
                transactionDate = nowMs,
                remarks = "मजदूरी पेशगी",
                createdAt = nowMs,
                updatedAt = nowMs
            )
        )

        // 3. Record ₹47,885 CREDIT purchase (Buy slip with ₹0 cash paid out)
        buyRepo.saveBuyEntry(
            BuyEntry(
                id = "buy-credit-1",
                shopId = "shop-p2",
                farmerId = "party-p2",
                commodityId = "wheat",
                entryDate = nowMs,
                bagsCount = 50L,
                weightPerBagGrams = 50_000L,
                extraWeightGrams = 0L,
                totalWeightGrams = 2_500_000L,
                ratePaisaPerQuintal = 200_000L,
                commissionBps = 0L,
                labourPerBagPaisa = 0L,
                advancePaidPaisa = 0L,
                grossPaisa = 5_000_000L,
                commissionPaisa = 100_000L,
                labourPaisa = 50_000L,
                netPayablePaisa = 4_788_500L, // ₹47,885
                paidPaisa = 0L, // Credit purchase: nothing paid in cash
                createdAt = nowMs,
                updatedAt = nowMs
            )
        )

        val vm = DailyRegisterViewModel(
            shopId = "shop-p2",
            cashRepository = cashRepo,
            partyRepository = partyRepo,
            shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(),
            buyRepository = buyRepo,
            viewModelScope = backgroundScope,
            clock = clock,
            timeZone = TimeZone.of("Asia/Kolkata")
        )

        val state = vm.uiState.first { it.bahiDayPage != null }

        // Net cash flow for the day is ₹7,000 in - ₹7,000 out = ₹0.
        // Credit purchase must NOT show up as a ₹47,885 cash drawer discrepancy!
        assertEquals(0L, state.discrepancyPaisa, "Opening discrepancy must be ₹0, ignoring credit purchases")
        assertNotEquals(
            -4_788_500L,
            state.discrepancyPaisa,
            "Must NOT headline credit purchases as cash shortfall"
        )
    }

    @Test
    fun auditP2_physicalCashCountMatchesZeroDifference() = runTest {
        val db = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(db, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(db, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(db, ioDispatcher = ioDispatcher)

        seedShopAndParty(db)

        val vm = DailyRegisterViewModel(
            shopId = "shop-p2",
            cashRepository = cashRepo,
            partyRepository = partyRepo,
            shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope
        )

        // Record ₹5,000 cash in
        vm.recordDailyEntry(
            partyId = "party-p2",
            transactionType = TransactionType.JAMA_RECEIVED,
            amountRs = 5000L
        )
        vm.events.first()

        val initialState = vm.uiState.first { it.inHandCashDrawerPaisa == 500_000L }
        assertEquals(null, initialState.physicalCountPaisa)
        assertEquals(BahiReconciliationStatus.NOT_COUNTED, initialState.reconciliationStatus)

        // Perform physical count: ₹5,000 counted
        vm.setPhysicalCashCount(5000L)

        val matchState = vm.uiState.first { it.physicalCountPaisa == 500_000L }
        assertEquals(BahiReconciliationStatus.BALANCED, matchState.reconciliationStatus)
        assertEquals(0L, matchState.discrepancyPaisa)
    }
}
