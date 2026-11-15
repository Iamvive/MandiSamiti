package com.appwork.mandisamiti.ui.register

import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DailyRegisterCreditJamaTest {

    @Test
    fun bookEntryCreditJamaAppearsInRegisterWithoutInflatingPhysicalDrawerCash() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val now = Instant.parse("2026-11-15T10:00:00Z")
        val clock = object : Clock { override fun now() = now }

        val shopId = "shop-credit"
        val partyId = "farmer-monty"

        shopRepo.saveShopProfile(
            ShopProfile(
                id = shopId,
                shopName = "Omveer Singh & Brothers",
                ownerName = "Omveer Singh",
                mandiName = "Hathras",
                phoneNumber = "9876543210",
                pinHash = "1234",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        partyRepo.saveParty(
            Party(
                id = partyId,
                shopId = shopId,
                name = "Monty",
                village = "Hathras",
                partyType = PartyType.FARMER,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val txDate = now.toEpochMilliseconds()

        // 1. Physical Cash Out (initial payment on slip): ₹25,000 cash paid from drawer
        cashRepo.recordTransaction(
            CashTransaction(
                id = "tx-cash-pay",
                shopId = shopId,
                partyId = partyId,
                transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = 2_500_000L, // ₹25,000
                paymentMode = PaymentMode.CASH,
                transactionDate = txDate,
                remarks = "खरीद भुगतान: पर्ची #1",
                createdAt = txDate,
                updatedAt = txDate,
                sourceEntryType = "BUY",
                sourceEntryId = "buy-slip-1"
            )
        )

        // 2. Non-Cash Credit Jama (unpaid balance on slip): ₹24,000 BOOK_ENTRY
        cashRepo.recordTransaction(
            CashTransaction(
                id = "tx-credit-jama",
                shopId = shopId,
                partyId = partyId,
                transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = 2_400_000L, // ₹24,000
                paymentMode = PaymentMode.BOOK_ENTRY,
                transactionDate = txDate,
                remarks = "Monty • खरीद पर्ची #1 बाकी जमा (क्रेडिट)",
                createdAt = txDate,
                updatedAt = txDate,
                sourceEntryType = "BUY_CREDIT",
                sourceEntryId = "buy-slip-1"
            )
        )

        val viewModel = DailyRegisterViewModel(
            shopId = shopId,
            cashRepository = cashRepo,
            partyRepository = partyRepo,
            shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope,
            clock = clock,
            timeZone = TimeZone.of("Asia/Kolkata")
        )

        val state = viewModel.uiState.first { it.todayTransactions.size >= 2 }

        // Physical Cash Drawer checks:
        assertEquals(0L, state.todayCashInPaisa, "Physical cash in must be 0")
        assertEquals(2_500_000L, state.todayCashOutPaisa, "Physical cash out is ₹25,000")
        assertEquals(-2_500_000L, state.inHandCashDrawerPaisa, "Drawer balance reflects only physical cash (-₹25,000)")

        // Credit Jama checks:
        assertEquals(2_400_000L, state.todayCreditInPaisa, "Credit Jama must be ₹24,000")
        assertEquals(2_400_000L, state.totalJamaSidePaisa, "Total Jama side includes Credit Jama (0 opening + 0 cash in + 24,000 credit)")

        // Both transactions are in todayTransactions:
        val txModes = state.todayTransactions.associate { it.transaction.id to it.transaction.paymentMode }
        assertEquals(PaymentMode.CASH, txModes["tx-cash-pay"])
        assertEquals(PaymentMode.BOOK_ENTRY, txModes["tx-credit-jama"])
    }
}
