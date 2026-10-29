package com.appwork.mandisamiti.ui.ledger

import com.appwork.mandisamiti.data.repository.*
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class PartyLedgerBalanceF01Test {

    private val fixedNow = Instant.parse("2026-10-29T10:00:00Z")
    private val testClock = object : Clock {
        override fun now() = fixedNow
    }

    @Test
    fun reproF01_partyBalance_exactScenario_gives13900ThenZero() = runTest {
        val db = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val queries = db.appDatabaseQueries

        val partyRepo = OfflineFirstPartyRepository(db, ioDispatcher = ioDispatcher)
        val buyRepo = OfflineFirstBuyRepository(db, ioDispatcher = ioDispatcher, clock = testClock)
        val lendRepo = OfflineFirstLendRepository(db, ioDispatcher = ioDispatcher, clock = testClock)
        val cashRepo = OfflineFirstCashTransactionRepository(db, ioDispatcher = ioDispatcher, clock = testClock)
        val expenseRepo = OfflineFirstExpenseRepository(db, ioDispatcher = ioDispatcher, clock = testClock)

        val shopId = "shop-test"
        val partyId = "party-x"
        val partyName = "Farmer X"

        partyRepo.saveParty(
            Party(
                id = partyId,
                shopId = shopId,
                name = partyName,
                village = "Test Village",
                partyType = PartyType.FARMER,
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds()
            )
        )

        // 1. Buy: 10 bags @ ₹2,500 -> net payable ₹12,200 (1,220,000 paisa)
        val buyId = "buy-1"
        buyRepo.saveBuyEntry(
            BuyEntry(
                id = buyId,
                shopId = shopId,
                farmerId = partyId,
                commodityId = "गेहूं",
                entryDate = fixedNow.toEpochMilliseconds(),
                bagsCount = 10,
                weightPerBagGrams = 50_000L,
                extraWeightGrams = 0L,
                totalWeightGrams = 500_000L,
                ratePaisaPerQuintal = 250000L,
                commissionBps = 200L,
                labourPerBagPaisa = 500L,
                advancePaidPaisa = 0L,
                grossPaisa = 1250000L,
                commissionPaisa = 25000L,
                labourPaisa = 5000L,
                netPayablePaisa = 1220000L, // ₹12,200
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds()
            )
        )

        // 2. Lend: ₹5,000 (500,000 paisa)
        val lendId = "lend-1"
        val lendEntry = LendEntry(
            id = lendId,
            shopId = shopId,
            partyId = partyId,
            lendType = LendType.MONEY,
            entryDate = fixedNow.toEpochMilliseconds(),
            amountPaisa = 500_000L,
            createdAt = fixedNow.toEpochMilliseconds(),
            updatedAt = fixedNow.toEpochMilliseconds()
        )
        lendRepo.saveLendEntry(lendEntry)
        // Cash row written by app on lend save
        val lendCashTxId = "tx-lend-cash"
        cashRepo.recordTransaction(
            CashTransaction(
                id = lendCashTxId,
                shopId = shopId,
                partyId = partyId,
                transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = 500_000L,
                paymentMode = PaymentMode.CASH,
                transactionDate = fixedNow.toEpochMilliseconds(),
                remarks = "उधार ऋण: [LEND:$lendId]",
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds()
            )
        )

        // 3. Repay: ₹7,000 (700,000 paisa)
        val settlementId = "settle-1"
        lendRepo.settle(
            LendSettlement(
                id = settlementId,
                shopId = shopId,
                lendId = lendId,
                settledAt = fixedNow.toEpochMilliseconds(),
                amountPaisa = 700_000L,
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds()
            )
        )
        // Cash row written by app on repay
        val repayCashTxId = "tx-repay-cash"
        cashRepo.recordTransaction(
            CashTransaction(
                id = repayCashTxId,
                shopId = shopId,
                partyId = partyId,
                transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = 700_000L,
                paymentMode = PaymentMode.CASH,
                transactionDate = fixedNow.toEpochMilliseconds(),
                remarks = "उधार वापसी जमा [SETTLE:$settlementId:$lendId]",
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds()
            )
        )

        // 4. Expense: ₹300 (30,000 paisa), paid to party X
        val expenseId = "exp-1"
        expenseRepo.saveExpense(
            ExpenseEntry(
                id = expenseId,
                shopId = shopId,
                entryDate = fixedNow.toEpochMilliseconds(),
                category = ExpenseCategory.HAMMALI,
                amountPaisa = 30_000L,
                paidTo = partyId,
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds()
            )
        )
        val expCashTxId = "tx-exp-cash"
        cashRepo.recordTransaction(
            CashTransaction(
                id = expCashTxId,
                shopId = shopId,
                partyId = partyId,
                transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = 30_000L,
                paymentMode = PaymentMode.CASH,
                transactionDate = fixedNow.toEpochMilliseconds(),
                remarks = "खर्च: हम्माली [EXPENSE:$expenseId]",
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds()
            )
        )

        // Verify balance before voiding:
        // Shop owes farmer 12,200 (Buy)
        // Farmer owes shop 5,000 (Lend)
        // Farmer repaid 7,000 (Repay)
        // Expense was 300 to X as cash advance
        // Net: -12,200 + 5,000 - 7,000 + 300 = -13,900 (-1,390,000 paisa = ₹13,900 देना है)
        val initialBalance = partyRepo.getPartyBalanceStream(partyId).first()
        assertEquals(-1_390_000L, initialBalance?.balancePaisa)

        // Now void every entry for X
        buyRepo.voidBuyEntry(buyId, VoidReason.WRONG_ENTRY)
        lendRepo.voidLendEntry(lendId, VoidReason.WRONG_ENTRY)
        expenseRepo.voidExpense(expenseId, VoidReason.WRONG_ENTRY)
        cashRepo.voidTransaction(expCashTxId, VoidReason.WRONG_ENTRY)

        // Verify balance after voiding:
        // Must be exactly ₹0
        val finalBalance = partyRepo.getPartyBalanceStream(partyId).first()
        assertEquals(0L, finalBalance?.balancePaisa)
    }

    @Test
    fun testLendRepayLifecycleAndVoidCascades() = runTest {
        val db = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)

        val partyRepo = OfflineFirstPartyRepository(db, ioDispatcher = ioDispatcher)
        val lendRepo = OfflineFirstLendRepository(db, ioDispatcher = ioDispatcher, clock = testClock)
        val cashRepo = OfflineFirstCashTransactionRepository(db, ioDispatcher = ioDispatcher, clock = testClock)

        val shopId = "shop-test"
        val partyId = "party-y"
        val partyName = "Farmer Y"

        partyRepo.saveParty(
            Party(
                id = partyId,
                shopId = shopId,
                name = partyName,
                village = "Test Village 2",
                partyType = PartyType.FARMER,
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds()
            )
        )

        // 1. Lend ₹10,000 (1,000,000 paisa)
        val lendId = "lend-y-1"
        lendRepo.saveLendEntry(
            LendEntry(
                id = lendId,
                shopId = shopId,
                partyId = partyId,
                lendType = LendType.MONEY,
                entryDate = fixedNow.toEpochMilliseconds(),
                amountPaisa = 1_000_000L,
                status = LendStatus.OPEN,
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds()
            )
        )
        val lendTxId = "tx-lend-y"
        cashRepo.recordTransaction(
            CashTransaction(
                id = lendTxId,
                shopId = shopId,
                partyId = partyId,
                transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = 1_000_000L,
                paymentMode = PaymentMode.CASH,
                transactionDate = fixedNow.toEpochMilliseconds(),
                remarks = "उधार ऋण [LEND:$lendId]",
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds()
            )
        )

        // Balance must be +₹10,000
        assertEquals(1_000_000L, partyRepo.getPartyBalanceStream(partyId).first()?.balancePaisa)

        // 2. Partial Repay ₹4,000 (400,000 paisa)
        val settle1Id = "settle-y-1"
        lendRepo.settle(
            LendSettlement(
                id = settle1Id,
                shopId = shopId,
                lendId = lendId,
                settledAt = fixedNow.toEpochMilliseconds(),
                amountPaisa = 400_000L,
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds()
            )
        )
        val repay1TxId = "tx-repay-y-1"
        cashRepo.recordTransaction(
            CashTransaction(
                id = repay1TxId,
                shopId = shopId,
                partyId = partyId,
                transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = 400_000L,
                paymentMode = PaymentMode.CASH,
                transactionDate = fixedNow.toEpochMilliseconds(),
                remarks = "उधार वापसी जमा [SETTLE:$settle1Id:$lendId]",
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds()
            )
        )

        // Balance must be +₹6,000 (10,000 - 4,000)
        assertEquals(600_000L, partyRepo.getPartyBalanceStream(partyId).first()?.balancePaisa)
        assertEquals(LendStatus.PARTIAL, lendRepo.getLendEntryById(lendId)?.status)

        // 3. Full Repay remaining ₹6,000 (600,000 paisa)
        val settle2Id = "settle-y-2"
        lendRepo.settle(
            LendSettlement(
                id = settle2Id,
                shopId = shopId,
                lendId = lendId,
                settledAt = fixedNow.toEpochMilliseconds(),
                amountPaisa = 600_000L,
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds()
            )
        )
        val repay2TxId = "tx-repay-y-2"
        cashRepo.recordTransaction(
            CashTransaction(
                id = repay2TxId,
                shopId = shopId,
                partyId = partyId,
                transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = 600_000L,
                paymentMode = PaymentMode.CASH,
                transactionDate = fixedNow.toEpochMilliseconds(),
                remarks = "उधार वापसी जमा [SETTLE:$settle2Id:$lendId]",
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds()
            )
        )

        // Balance must be ₹0 and loan SETTLED
        assertEquals(0L, partyRepo.getPartyBalanceStream(partyId).first()?.balancePaisa)
        assertEquals(LendStatus.SETTLED, lendRepo.getLendEntryById(lendId)?.status)

        // 4. Void Repay 2: voiding the second repayment cash transaction
        cashRepo.voidTransaction(repay2TxId, VoidReason.WRONG_ENTRY)

        // Settle 2 should now be void, lend status reverted to PARTIAL, balance back to +₹6,000
        val settlementsAfterVoid = lendRepo.getSettlements(lendId)
        assertEquals(1, settlementsAfterVoid.size) // getSettlements only returns is_void = 0
        assertEquals(400_000L, settlementsAfterVoid.first().amountPaisa)
        assertEquals(LendStatus.PARTIAL, lendRepo.getLendEntryById(lendId)?.status)
        assertEquals(600_000L, partyRepo.getPartyBalanceStream(partyId).first()?.balancePaisa)

        // 5. Void the lend entry
        lendRepo.voidLendEntry(lendId, VoidReason.WRONG_ENTRY)

        // Lend should be void, all settlements void, linked cash tx void, balance ₹0
        val lendEntry = lendRepo.getLendEntryById(lendId)
        assertEquals(true, lendEntry?.isVoid)
        assertEquals(0, lendRepo.getSettlements(lendId).size)
        assertEquals(true, cashRepo.getTransactionsByPartyStream(partyId).first().all { it.isVoid })
        assertEquals(0L, partyRepo.getPartyBalanceStream(partyId).first()?.balancePaisa)
    }
}

