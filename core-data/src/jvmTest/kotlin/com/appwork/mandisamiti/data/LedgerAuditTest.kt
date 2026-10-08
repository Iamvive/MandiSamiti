package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstDealRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Deal
import com.appwork.mandisamiti.domain.model.DealStatus
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.model.VoidReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LedgerAuditTest {
    private val fixedNow = Instant.parse("2026-10-08T05:30:00Z")
    private val clock = object : Clock { override fun now() = fixedNow }

    private fun farmer() = Party(
        id = "farmer-1", shopId = "shop-1", name = "रामवीर सिंह", village = "राया",
        partyType = PartyType.FARMER, createdAt = 1_000L, updatedAt = 1_000L
    )

    private fun deal(payable: Long = 100_000L) = Deal(
        id = "deal-1", shopId = "shop-1", farmerId = "farmer-1", buyerId = "buyer-1", commodityId = "comm-1",
        dealStatus = DealStatus.SETTLED, dealDate = 1_000L, bagsCount = 10,
        grossWeightGrams = 1_000_000L, netWeightGrams = 1_000_000L, ratePaisaPerUnit = 250_000L,
        grossAmountPaisa = 2_500_000L, netFarmerPayablePaisa = payable, netBuyerReceivablePaisa = 2_500_000L,
        createdAt = 1_000L, updatedAt = 1_000L
    )

    @Test
    fun editKeepsOriginalDatesAndRecordsRevision() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstDealRepository(db, clock = clock)
        repo.saveDeal(deal())

        repo.editDeal(deal(payable = 90_000L).copy(dealDate = 999_999L, createdAt = 999_999L))

        val saved = repo.getDealById("deal-1")!!
        assertEquals(1_000L, saved.dealDate)
        assertEquals(1_000L, saved.createdAt)
        assertEquals(fixedNow.toEpochMilliseconds(), saved.updatedAt)
        assertEquals(2, saved.revision)
        assertEquals(90_000L, saved.netFarmerPayablePaisa)

        val revisions = db.appDatabaseQueries.getRevisionsForEntry("deal-1").executeAsList()
        assertEquals(listOf("CREATE", "EDIT"), revisions.map { it.change_kind })
        val original = Json.decodeFromString(Deal.serializer(), revisions[0].snapshot_json)
        assertEquals(100_000L, original.netFarmerPayablePaisa)
    }

    @Test
    fun voidedDealLeavesBalanceButStaysInLedger() = runTest {
        val db = createTestDatabase()
        val partyRepo = OfflineFirstPartyRepository(db)
        val repo = OfflineFirstDealRepository(db, clock = clock)
        partyRepo.saveParty(farmer())
        repo.saveDeal(deal())
        assertEquals(-100_000L, partyRepo.getPartyBalanceStream("farmer-1").first()!!.balancePaisa)

        repo.voidDeal("deal-1", VoidReason.WEIGHING_ERROR)

        assertEquals(0L, partyRepo.getPartyBalanceStream("farmer-1").first()!!.balancePaisa)
        val listed = repo.getDealsByFarmerStream("farmer-1").first().single()
        assertTrue(listed.isVoid)
        assertEquals(VoidReason.WEIGHING_ERROR, listed.voidReason)
        assertEquals(
            listOf("CREATE", "VOID"),
            db.appDatabaseQueries.getRevisionsForEntry("deal-1").executeAsList().map { it.change_kind }
        )
    }

    @Test
    fun voidedDealCannotBeEdited() = runTest {
        val repo = OfflineFirstDealRepository(createTestDatabase(), clock = clock)
        repo.saveDeal(deal())
        repo.voidDeal("deal-1", VoidReason.DEAL_CANCELLED)
        assertFailsWith<IllegalArgumentException> { repo.editDeal(deal(payable = 1L)) }
    }

    @Test
    fun voidedCashEntryLeavesBalance() = runTest {
        val db = createTestDatabase()
        val partyRepo = OfflineFirstPartyRepository(db)
        val cashRepo = OfflineFirstCashTransactionRepository(db, clock = clock)
        partyRepo.saveParty(farmer())
        cashRepo.recordTransaction(
            CashTransaction(
                id = "tx-1", shopId = "shop-1", partyId = "farmer-1",
                transactionType = TransactionType.UDHAR_GIVEN, amountPaisa = 500_000L,
                transactionDate = 1_000L, createdAt = 1_000L, updatedAt = 1_000L
            )
        )
        assertEquals(500_000L, partyRepo.getPartyBalanceStream("farmer-1").first()!!.balancePaisa)

        cashRepo.voidTransaction("tx-1", VoidReason.WRONG_ENTRY)

        assertEquals(0L, partyRepo.getPartyBalanceStream("farmer-1").first()!!.balancePaisa)
        assertTrue(cashRepo.getTransactionsByPartyStream("farmer-1").first().single().isVoid)
        assertEquals(
            listOf("CREATE", "VOID"),
            db.appDatabaseQueries.getRevisionsForEntry("tx-1").executeAsList().map { it.change_kind }
        )
    }
}
