package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class UdhaarSchemaRoundTripTest {
    private val clock = object : Clock { override fun now() = Instant.fromEpochMilliseconds(2_000L) }

    @Test
    fun newLendAndCashFieldsSurviveSaveAndRead() = runTest {
        val db = createTestDatabase()
        db.appDatabaseQueries.insertParty("party", "shop", "Party", null, null, "FARMER", null, null, null, null, 1L, 1L, 0L, 0L)
        val d = StandardTestDispatcher(testScheduler)
        val lendRepo = OfflineFirstLendRepository(db, ioDispatcher = d, clock = clock)
        val cashRepo = OfflineFirstCashTransactionRepository(db, ioDispatcher = d, clock = clock)

        lendRepo.saveLendEntry(LendEntry(
            id = "l1", shopId = "shop", partyId = "party", lendType = LendType.MONEY, entryDate = 1_000L,
            amountPaisa = 500_000L, createdAt = 1_000L, updatedAt = 1_000L,
            direction = LendDirection.TAKEN, kind = LendKind.PESGI,
            receiverName = "Suresh", receiverPhone = "9876543210", monthlyRatePercent = 0.0,
        ))
        cashRepo.recordTransaction(CashTransaction(
            id = "c1", shopId = "shop", partyId = "party", transactionType = TransactionType.JAMA_RECEIVED,
            amountPaisa = 500_000L, transactionDate = 1_000L, createdAt = 1_000L, updatedAt = 1_000L, lendId = "l1",
        ))

        val lend = lendRepo.getLendEntryById("l1")!!
        assertEquals(LendDirection.TAKEN, lend.direction)
        assertEquals(LendKind.PESGI, lend.kind)
        assertEquals("Suresh", lend.receiverName)
        assertEquals("9876543210", lend.receiverPhone)
        assertEquals(0.0, lend.monthlyRatePercent)
        assertEquals("l1", cashRepo.getTransactionsByPartyStream("party").first().single().lendId)
    }
}
