package com.appwork.mandisamiti.ui.register

import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.domain.repository.BuyRepository
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** P2: credit purchases move no cash, so they must not open the day with a false अंतर. */
@OptIn(ExperimentalCoroutinesApi::class)
class DayCloseCreditBuyTest {

    private class FakeBuyRepo(buys: List<BuyEntry>) : BuyRepository {
        private val flow = MutableStateFlow(buys)
        override fun getBuyEntriesStream(shopId: String): Flow<List<BuyEntry>> = flow
        override suspend fun getBuyEntryById(id: String) = flow.value.firstOrNull { it.id == id }
        override suspend fun saveBuyEntry(entry: BuyEntry) { flow.value = flow.value + entry }
        override suspend fun editBuyEntry(entry: BuyEntry) = Unit
        override suspend fun voidBuyEntry(id: String, reason: VoidReason) = Unit
    }

    private fun buy(at: Long, advance: Long, paid: Long) = BuyEntry(
        id = "buy-$at-$advance", shopId = "shop-1", farmerId = "farmer-1", commodityId = "wheat",
        entryDate = at, bagsCount = 10, weightPerBagGrams = 50_000, extraWeightGrams = 0,
        totalWeightGrams = 500_000, ratePaisaPerQuintal = 300_000, commissionBps = 0, labourPerBagPaisa = 0,
        advancePaidPaisa = advance, grossPaisa = 1_500_000, commissionPaisa = 0, labourPaisa = 0,
        netPayablePaisa = 1_500_000 - advance, paidPaisa = paid, createdAt = at, updatedAt = at,
    )

    @Test
    fun creditBuyAndAdvanceAdjustmentDoNotCreateFalseDifference() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val now = Instant.parse("2026-10-08T05:30:00Z")
        val clock = object : Clock { override fun now() = now }
        val yesterday = Instant.parse("2026-10-07T10:00:00Z").toEpochMilliseconds()
        val today = Instant.parse("2026-10-08T04:00:00Z").toEpochMilliseconds()

        partyRepo.saveParty(Party(id = "farmer-1", shopId = "shop-1", name = "TEST Ramesh", village = "TEST", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L))
        cashRepo.recordTransaction(CashTransaction(
            id = "open", shopId = "shop-1", partyId = "farmer-1", transactionType = TransactionType.JAMA_RECEIVED,
            amountPaisa = 700_000, transactionDate = yesterday, createdAt = yesterday, updatedAt = yesterday,
        ))
        // Two credit purchases (nothing paid in cash), one of them adjusting an older advance.
        val buys = listOf(buy(today, advance = 0, paid = 0), buy(today + 1, advance = 200_000, paid = 0))

        val viewModel = DailyRegisterViewModel(
            shopId = "shop-1", cashRepository = cashRepo, partyRepository = partyRepo,
            shopProfileRepository = shopRepo, ttsManager = SoundboxTtsManager(),
            buyRepository = FakeBuyRepo(buys),
            viewModelScope = backgroundScope, clock = clock, timeZone = TimeZone.of("Asia/Kolkata"),
        )
        val state = viewModel.uiState.first { it.bahiDayPage != null && it.openingCashPaisa == 700_000L }

        val page = state.bahiDayPage!!
        assertEquals(700_000L, state.inHandCashDrawerPaisa)
        assertTrue(page.isBalanced, "false अंतर of ${page.differencePaisa} paisa from credit buys")
        assertEquals(700_000L, page.leftTotalPaisa)
        assertEquals(700_000L, page.rightTotalPaisa)
    }
}
