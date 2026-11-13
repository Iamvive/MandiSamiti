package com.appwork.mandisamiti.ui.home

import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyBalance
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.domain.repository.CashTransactionRepository
import com.appwork.mandisamiti.domain.repository.PartyRepository
import com.appwork.mandisamiti.domain.repository.SellRepository
import com.appwork.mandisamiti.domain.repository.ShopProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeSellRepo(initial: SellEntry) : SellRepository {
    var entry: SellEntry = initial
    var edits = 0
    override fun getSellEntriesStream(shopId: String): Flow<List<SellEntry>> = emptyFlow()
    override suspend fun getSellEntryById(id: String): SellEntry? = entry.takeIf { it.id == id }
    override suspend fun saveSellEntry(entry: SellEntry) { this.entry = entry }
    override suspend fun editSellEntry(entry: SellEntry) { this.entry = entry; edits++ }
    override suspend fun voidSellEntry(id: String, reason: VoidReason) { entry = entry.copy(isVoid = true) }
}

private class FakePartyRepo : PartyRepository {
    override fun getPartiesStream(shopId: String): Flow<List<Party>> = emptyFlow()
    override fun getPartyBalanceStream(partyId: String): Flow<PartyBalance?> = emptyFlow()
    override suspend fun getPartyById(partyId: String): Party? = null
    override suspend fun getPartyByPhone(shopId: String, phone: String): Party? = null
    override suspend fun saveParty(party: Party) = Unit
    override suspend fun deleteParty(partyId: String) = Unit
}

private class FakeShopRepo : ShopProfileRepository {
    override fun getShopProfileStream(): Flow<ShopProfile?> = emptyFlow()
    override suspend fun saveShopProfile(profile: ShopProfile) = Unit
    override suspend fun updateSoundSetting(shopId: String, isSoundEnabled: Boolean) = Unit
}

private class FakeCashRepo : CashTransactionRepository {
    val recorded = mutableListOf<CashTransaction>()
    override fun getTransactionsByPartyStream(partyId: String): Flow<List<CashTransaction>> = emptyFlow()
    override fun getTransactionsByShopStream(shopId: String): Flow<List<CashTransaction>> = emptyFlow()
    override suspend fun recordTransaction(transaction: CashTransaction) { recorded += transaction }
    override suspend fun editTransaction(
        transactionId: String, amountPaisa: Long, paymentMode: PaymentMode, transactionDate: Long, remarks: String?
    ) = Unit
    override suspend fun voidTransaction(transactionId: String, reason: VoidReason) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelSellSettlementTest {

    private fun sell(
        received: Long = 0L,
        due: Long = 1_000_000L,
        isVoid: Boolean = false,
    ) = SellEntry(
        id = "s1", shopId = "shop", buyerId = "buyer1", commodityId = "Wheat", entryDate = 1000L,
        billMode = BillMode.NORMAL, partyKind = SellPartyKind.MERCHANT,
        bagsCount = 10, weightPerBagGrams = 50_000, totalWeightGrams = 500_000,
        ratePaisaPerQuintal = 200_000, aratBps = 0, tulakPaisa = 0, grossPaisa = 1_000_000,
        aratPaisa = 0, mandiShulkPaisa = 0, totalReceivablePaisa = 1_000_000,
        receivedPaisa = received, balanceDuePaisa = due, slipNo = 7L,
        createdAt = 1000L, updatedAt = 1000L, revision = 1L, isVoid = isVoid
    )

    private class Rig(val vm: HomeViewModel, val sells: FakeSellRepo, val cash: FakeCashRepo, val writes: IntArray)

    private fun TestScope.rig(entry: SellEntry): Rig {
        val sells = FakeSellRepo(entry)
        val cash = FakeCashRepo()
        val writes = IntArray(1)
        val vm = HomeViewModel(
            shopId = "shop",
            shopProfileRepository = FakeShopRepo(),
            partyRepository = FakePartyRepo(),
            sellRepository = sells,
            cashRepository = cash,
            viewModelScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            onLocalWrite = { writes[0]++ }
        )
        return Rig(vm, sells, cash, writes)
    }

    @Test
    fun partialPaymentMovesMoneyFromDueToReceivedAndRecordsOneCashIn() = runTest {
        val r = rig(sell())
        var done = false
        r.vm.settleSellSlipCash("s1", 400_000L) { done = true }
        runCurrent()

        assertEquals(400_000L, r.sells.entry.receivedPaisa)
        assertEquals(600_000L, r.sells.entry.balanceDuePaisa)
        assertEquals(2L, r.sells.entry.revision)
        val tx = r.cash.recorded.single()
        assertEquals(TransactionType.JAMA_RECEIVED, tx.transactionType)
        assertEquals(PaymentMode.CASH, tx.paymentMode)
        assertEquals(400_000L, tx.amountPaisa)
        assertEquals("buyer1", tx.partyId)
        assertEquals("SELL", tx.sourceEntryType)
        assertEquals("s1", tx.sourceEntryId)
        assertEquals(1, r.writes[0])
        assertTrue(done)
    }

    @Test
    fun paymentLargerThanDueIsCappedAtTheBalance() = runTest {
        val r = rig(sell(received = 700_000L, due = 300_000L))
        r.vm.settleSellSlipCash("s1", 500_000L)
        runCurrent()

        assertEquals(1_000_000L, r.sells.entry.receivedPaisa)
        assertEquals(0L, r.sells.entry.balanceDuePaisa)
        assertEquals(300_000L, r.cash.recorded.single().amountPaisa)
    }

    @Test
    fun settlingAnAlreadyPaidSlipWritesNothing() = runTest {
        val r = rig(sell(received = 1_000_000L, due = 0L))
        r.vm.settleSellSlipCash("s1", 100_000L)
        runCurrent()

        assertEquals(0, r.sells.edits)
        assertTrue(r.cash.recorded.isEmpty())
        assertEquals(0, r.writes[0])
    }

    @Test
    fun voidSlipIsNeverSettled() = runTest {
        val r = rig(sell(isVoid = true))
        r.vm.settleSellSlipCash("s1", 100_000L)
        runCurrent()

        assertEquals(0, r.sells.edits)
        assertTrue(r.cash.recorded.isEmpty())
    }

    @Test
    fun zeroOrNegativeAmountIsIgnored() = runTest {
        val r = rig(sell())
        r.vm.settleSellSlipCash("s1", 0L)
        r.vm.settleSellSlipCash("s1", -5L)
        runCurrent()

        assertEquals(0, r.sells.edits)
        assertTrue(r.cash.recorded.isEmpty())
    }

    @Test
    fun unknownSlipWritesNothingAndSkipsCallback() = runTest {
        val r = rig(sell())
        var done = false
        r.vm.settleSellSlipCash("missing", 100_000L) { done = true }
        runCurrent()

        assertEquals(0, r.sells.edits)
        assertTrue(r.cash.recorded.isEmpty())
        assertFalse(done)
    }
}
