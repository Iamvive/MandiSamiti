package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstBuyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BuyLoanCutTest {
    private val party = Party(
        id = "p1", shopId = "shop", name = "TEST रमेश", phone = "9876543210",
        village = "TEST गाँव", partyType = PartyType.FARMER, createdAt = 1, updatedAt = 1
    )

    private fun slip(slipNo: Long? = null, paidPaisa: Long = 0L) = BuyEntry(
        id = "b1", shopId = "shop", farmerId = "p1", commodityId = "गेहूं", entryDate = 1_000_000L,
        bagsCount = 10, weightPerBagGrams = 50_000, extraWeightGrams = 0, totalWeightGrams = 500_000,
        ratePaisaPerQuintal = 200_000, commissionBps = 70, labourPerBagPaisa = 1_000, advancePaidPaisa = 0L,
        grossPaisa = 1_000_000, commissionPaisa = 7_000, labourPaisa = 10_000, netPayablePaisa = 983_000,
        paidPaisa = paidPaisa, slipNo = slipNo, createdAt = 1_000_000L, updatedAt = 1_000_000L,
    )

    @Test
    fun twoGivenLoansOldestFirstCutAllocation() = runTest {
        val db = createTestDatabase()
        val lendRepo = OfflineFirstLendRepository(db)
        // Loan 1: Rs 5,000 on day 1
        lendRepo.saveLendEntry(
            LendEntry(
                id = "l1", shopId = "shop", partyId = "p1", lendType = LendType.MONEY,
                direction = LendDirection.GIVEN, entryDate = 1_000_000L, amountPaisa = 500_000L,
                createdAt = 1_000_000L, updatedAt = 1_000_000L
            )
        )
        // Loan 2: Rs 3,000 on day 2
        lendRepo.saveLendEntry(
            LendEntry(
                id = "l2", shopId = "shop", partyId = "p1", lendType = LendType.MONEY,
                direction = LendDirection.GIVEN, entryDate = 2_000_000L, amountPaisa = 300_000L,
                createdAt = 2_000_000L, updatedAt = 2_000_000L
            )
        )

        // Initial drawer cash (CASH mode)
        val initialCashRow = CashTransaction(
            id = "c-initial", shopId = "shop", partyId = "p1", transactionType = TransactionType.JAMA_RECEIVED,
            amountPaisa = 5_000_000L, paymentMode = PaymentMode.CASH, transactionDate = 500_000L,
            createdAt = 500_000L, updatedAt = 500_000L
        )
        db.appDatabaseQueries.insertCashTransaction(
            id = initialCashRow.id, shop_id = initialCashRow.shopId, party_id = initialCashRow.partyId, deal_id = null,
            transaction_type = initialCashRow.transactionType.name, amount_paisa = initialCashRow.amountPaisa,
            payment_mode = initialCashRow.paymentMode.name, transaction_date = initialCashRow.transactionDate,
            voice_note_uri = null, remarks = null, created_at = initialCashRow.createdAt,
            updated_at = initialCashRow.updatedAt, is_deleted = 0L, sync_status = 0L, revision = 1L,
            is_void = 0L, void_reason = null, source_entry_type = null, source_entry_id = null, lend_id = null
        )

        val buyRepo = OfflineFirstBuyRepository(db)
        // Cut Rs 6,000 (600_000 paisa)
        buyRepo.saveBuyCheckout(
            entry = slip(paidPaisa = 600_000L),
            party = party,
            payments = emptyList(),
            loanCutPaisa = 600_000L
        )

        val q = db.appDatabaseQueries
        // Loan 1 fully settled
        val l1 = lendRepo.getLendEntryById("l1")!!
        assertEquals(LendStatus.SETTLED, l1.status)
        // Loan 2 has Rs 2,000 left
        val l2 = lendRepo.getLendEntryById("l2")!!
        assertEquals(LendStatus.PARTIAL, l2.status)
        val s2 = lendRepo.getSettlements("l2").filter { !it.isVoid }
        assertEquals(100_000L, s2.sumOf { it.principalPaisa })

        // Cash rows are BOOK_ENTRY with lendId set and sourceEntryId == null
        val bookRows = q.getCashTransactionsByShop("shop").executeAsList()
            .filter { it.payment_mode == PaymentMode.BOOK_ENTRY.name }
        assertEquals(2, bookRows.size)
        assertTrue(bookRows.all { it.lend_id != null && it.source_entry_id == null })
        assertTrue(bookRows.all { it.remarks?.startsWith("पर्ची #1 से कटौती") == true })

        // Galla drawer cash (CASH mode) unchanged
        val drawerCash = q.getCashTransactionsByShop("shop").executeAsList()
            .filter { it.payment_mode == PaymentMode.CASH.name && it.is_void == 0L }
            .sumOf { it.amount_paisa }
        assertEquals(5_000_000L, drawerCash)

        // Udhaar given drops by exactly Rs 6,000 (from 800_000 to 200_000)
        val balance = q.getPartyBalance("p1").executeAsOne()
        assertEquals(200_000L, balance.udhaar_given_paisa)

        // Slip balance reflects only the slip: 983_000 - 600_000 paid = -383_000
        assertEquals(-383_000L, balance.slip_balance_paisa)
    }

    @Test
    fun cutAboveLoanTotalIsRejectedAndNothingSaved() = runTest {
        val db = createTestDatabase()
        val lendRepo = OfflineFirstLendRepository(db)
        lendRepo.saveLendEntry(
            LendEntry(
                id = "l1", shopId = "shop", partyId = "p1", lendType = LendType.MONEY,
                direction = LendDirection.GIVEN, entryDate = 1_000_000L, amountPaisa = 500_000L,
                createdAt = 1_000_000L, updatedAt = 1_000_000L
            )
        )

        val buyRepo = OfflineFirstBuyRepository(db)
        assertFailsWith<Exception> {
            buyRepo.saveBuyCheckout(
                entry = slip(paidPaisa = 600_000L),
                party = party,
                payments = emptyList(),
                loanCutPaisa = 600_000L
            )
        }

        val q = db.appDatabaseQueries
        assertNull(buyRepo.getBuyEntryById("b1"))
        assertEquals(0, lendRepo.getSettlements("l1").size)
        assertEquals(0, q.getCashTransactionsByShop("shop").executeAsList().size)
    }

    @Test
    fun interestOnLoanClearsInterestFirstThenPrincipal() = runTest {
        val db = createTestDatabase()
        val fixedClock = object : Clock {
            override fun now(): Instant = Instant.fromEpochMilliseconds(1_000_000L + 30L * 86_400_000L)
        }
        val lendRepo = OfflineFirstLendRepository(db, clock = fixedClock)
        // Loan: Rs 10,000 at 2.0% monthly, 30 days old => interest = Rs 200 (20_000 paisa), total due = Rs 10,200
        lendRepo.saveLendEntry(
            LendEntry(
                id = "l1", shopId = "shop", partyId = "p1", lendType = LendType.MONEY,
                direction = LendDirection.GIVEN, entryDate = 1_000_000L, amountPaisa = 1_000_000L,
                monthlyRatePercent = 2.0, createdAt = 1_000_000L, updatedAt = 1_000_000L
            )
        )

        val buyRepo = OfflineFirstBuyRepository(db, clock = fixedClock)
        // Cut exact total due: Rs 10,200 (1_020_000 paisa)
        buyRepo.saveBuyCheckout(
            entry = slip(paidPaisa = 1_020_000L).copy(netPayablePaisa = 2_000_000L),
            party = party,
            payments = emptyList(),
            loanCutPaisa = 1_020_000L
        )

        val l1 = lendRepo.getLendEntryById("l1")!!
        assertEquals(LendStatus.SETTLED, l1.status)
        val settlements = lendRepo.getSettlements("l1").filter { !it.isVoid }
        assertEquals(1, settlements.size)
        assertEquals(1_000_000L, settlements.first().principalPaisa)
        assertEquals(20_000L, settlements.first().interestPaisa)

        val cashRows = db.appDatabaseQueries.getCashTransactionsByShop("shop").executeAsList()
        assertEquals(2, cashRows.size)
        val interestRow = cashRows.first { it.transaction_type == TransactionType.INTEREST_RECEIVED.name }
        assertEquals(20_000L, interestRow.amount_paisa)
        assertEquals(PaymentMode.BOOK_ENTRY.name, interestRow.payment_mode)
        val principalRow = cashRows.first { it.transaction_type == TransactionType.JAMA_RECEIVED.name }
        assertEquals(1_000_000L, principalRow.amount_paisa)
        assertEquals(PaymentMode.BOOK_ENTRY.name, principalRow.payment_mode)
    }

    @Test
    fun pesgiLoanPlusOrdinaryLoanOlderCutFirst() = runTest {
        val db = createTestDatabase()
        val lendRepo = OfflineFirstLendRepository(db)
        // Day 1: PESGI Rs 2,000
        lendRepo.saveLendEntry(
            LendEntry(
                id = "l1", shopId = "shop", partyId = "p1", lendType = LendType.MONEY,
                direction = LendDirection.GIVEN, kind = LendKind.PESGI, entryDate = 1_000_000L,
                amountPaisa = 200_000L, createdAt = 1_000_000L, updatedAt = 1_000_000L
            )
        )
        // Day 2: Ordinary LOAN Rs 5,000
        lendRepo.saveLendEntry(
            LendEntry(
                id = "l2", shopId = "shop", partyId = "p1", lendType = LendType.MONEY,
                direction = LendDirection.GIVEN, kind = LendKind.LOAN, entryDate = 2_000_000L,
                amountPaisa = 500_000L, createdAt = 2_000_000L, updatedAt = 2_000_000L
            )
        )

        val buyRepo = OfflineFirstBuyRepository(db)
        // Cut Rs 3,000 (300_000 paisa)
        buyRepo.saveBuyCheckout(
            entry = slip(paidPaisa = 300_000L),
            party = party,
            payments = emptyList(),
            loanCutPaisa = 300_000L
        )

        // Day 1 PESGI is fully settled first
        val l1 = lendRepo.getLendEntryById("l1")!!
        assertEquals(LendStatus.SETTLED, l1.status)
        // Day 2 ordinary LOAN has Rs 4,000 remaining (1,000 cut)
        val l2 = lendRepo.getLendEntryById("l2")!!
        assertEquals(LendStatus.PARTIAL, l2.status)
        val s2 = lendRepo.getSettlements("l2").filter { !it.isVoid }
        assertEquals(100_000L, s2.sumOf { it.principalPaisa })
    }

    @Test
    fun takenLoanNeverTouchedByCut() = runTest {
        val db = createTestDatabase()
        val lendRepo = OfflineFirstLendRepository(db)
        // TAKEN loan of Rs 5,000 (we owe the farmer)
        lendRepo.saveLendEntry(
            LendEntry(
                id = "l1", shopId = "shop", partyId = "p1", lendType = LendType.MONEY,
                direction = LendDirection.TAKEN, entryDate = 1_000_000L, amountPaisa = 500_000L,
                createdAt = 1_000_000L, updatedAt = 1_000_000L
            )
        )

        val buyRepo = OfflineFirstBuyRepository(db)
        // Trying to cut Rs 2,000 must fail because farmer has 0 GIVEN loans
        assertFailsWith<Exception> {
            buyRepo.saveBuyCheckout(
                entry = slip(paidPaisa = 200_000L),
                party = party,
                payments = emptyList(),
                loanCutPaisa = 200_000L
            )
        }

        val l1 = lendRepo.getLendEntryById("l1")!!
        assertEquals(LendStatus.OPEN, l1.status)
        assertEquals(0, lendRepo.getSettlements("l1").size)
    }

    @Test
    fun slipNumberAppearsInCashRowRemarks() = runTest {
        val db = createTestDatabase()
        val lendRepo = OfflineFirstLendRepository(db)
        lendRepo.saveLendEntry(
            LendEntry(
                id = "l1", shopId = "shop", partyId = "p1", lendType = LendType.MONEY,
                direction = LendDirection.GIVEN, entryDate = 1_000_000L, amountPaisa = 500_000L,
                createdAt = 1_000_000L, updatedAt = 1_000_000L
            )
        )

        val buyRepo = OfflineFirstBuyRepository(db)
        buyRepo.saveBuyCheckout(
            entry = slip(slipNo = 42L, paidPaisa = 100_000L),
            party = party,
            payments = emptyList(),
            loanCutPaisa = 100_000L
        )

        val rows = db.appDatabaseQueries.getCashTransactionsByShop("shop").executeAsList()
        assertEquals(1, rows.size)
        assertTrue(rows.first().remarks?.startsWith("पर्ची #42 से कटौती") == true)
        assertTrue(rows.first().remarks?.contains("[SETTLE:") == true)
    }

    @Test
    fun voidBuySlipReversesLoanCutAndRestoresLoansToOpen() = runTest {
        val db = createTestDatabase()
        val lendRepo = OfflineFirstLendRepository(db)
        // Loan 1: Rs 5,000 on day 1
        lendRepo.saveLendEntry(
            LendEntry(
                id = "l1", shopId = "shop", partyId = "p1", lendType = LendType.MONEY,
                direction = LendDirection.GIVEN, entryDate = 1_000_000L, amountPaisa = 500_000L,
                createdAt = 1_000_000L, updatedAt = 1_000_000L
            )
        )
        // Loan 2: Rs 3,000 on day 2
        lendRepo.saveLendEntry(
            LendEntry(
                id = "l2", shopId = "shop", partyId = "p1", lendType = LendType.MONEY,
                direction = LendDirection.GIVEN, entryDate = 2_000_000L, amountPaisa = 300_000L,
                createdAt = 2_000_000L, updatedAt = 2_000_000L
            )
        )

        val buyRepo = OfflineFirstBuyRepository(db)
        // Cut Rs 6,000 (600_000 paisa)
        buyRepo.saveBuyCheckout(
            entry = slip(paidPaisa = 600_000L),
            party = party,
            payments = emptyList(),
            loanCutPaisa = 600_000L
        )

        assertEquals(LendStatus.SETTLED, lendRepo.getLendEntryById("l1")!!.status)
        assertEquals(LendStatus.PARTIAL, lendRepo.getLendEntryById("l2")!!.status)

        // Void the buy slip
        buyRepo.voidBuyEntry("b1", VoidReason.WRONG_ENTRY)

        // 1. Slip is void
        val voidedSlip = buyRepo.getBuyEntryById("b1")!!
        assertTrue(voidedSlip.isVoid)
        assertEquals(VoidReason.WRONG_ENTRY.name, voidedSlip.voidReason)

        // 2. Both settlements are void
        val q = db.appDatabaseQueries
        val allSettlements = q.getAllLendSettlementsByLend("l1").executeAsList() + q.getAllLendSettlementsByLend("l2").executeAsList()
        assertEquals(2, allSettlements.size)
        assertTrue(allSettlements.all { it.is_void == 1L })

        // 3. Cut cash rows are void
        val cutCashRows = q.getCashTransactionsByShop("shop").executeAsList()
            .filter { it.payment_mode == PaymentMode.BOOK_ENTRY.name }
        assertEquals(2, cutCashRows.size)
        assertTrue(cutCashRows.all { it.is_void == 1L })

        // 4. Both loans restored to OPEN
        val l1After = lendRepo.getLendEntryById("l1")!!
        val l2After = lendRepo.getLendEntryById("l2")!!
        assertEquals(LendStatus.OPEN, l1After.status)
        assertEquals(LendStatus.OPEN, l2After.status)

        // 5. Total udhaar given back to pre-slip amount (Rs 8,000 = 800_000 paisa)
        val balance = q.getPartyBalance("p1").executeAsOne()
        assertEquals(800_000L, balance.udhaar_given_paisa)
    }

    @Test
    fun voidBuySlipWithoutLoanCutLeavesUdhaarUntouched() = runTest {
        val db = createTestDatabase()
        val lendRepo = OfflineFirstLendRepository(db)
        lendRepo.saveLendEntry(
            LendEntry(
                id = "l1", shopId = "shop", partyId = "p1", lendType = LendType.MONEY,
                direction = LendDirection.GIVEN, entryDate = 1_000_000L, amountPaisa = 500_000L,
                createdAt = 1_000_000L, updatedAt = 1_000_000L
            )
        )

        val buyRepo = OfflineFirstBuyRepository(db)
        buyRepo.saveBuyCheckout(
            entry = slip(paidPaisa = 0L),
            party = party,
            payments = emptyList(),
            loanCutPaisa = 0L
        )

        buyRepo.voidBuyEntry("b1", VoidReason.WRONG_ENTRY)

        val voidedSlip = buyRepo.getBuyEntryById("b1")!!
        assertTrue(voidedSlip.isVoid)
        val l1 = lendRepo.getLendEntryById("l1")!!
        assertEquals(LendStatus.OPEN, l1.status)
        assertEquals(0, lendRepo.getSettlements("l1").size)
    }

    @Test
    fun voidBuySlipTwiceChangesNothing() = runTest {
        val db = createTestDatabase()
        val lendRepo = OfflineFirstLendRepository(db)
        lendRepo.saveLendEntry(
            LendEntry(
                id = "l1", shopId = "shop", partyId = "p1", lendType = LendType.MONEY,
                direction = LendDirection.GIVEN, entryDate = 1_000_000L, amountPaisa = 500_000L,
                createdAt = 1_000_000L, updatedAt = 1_000_000L
            )
        )

        val buyRepo = OfflineFirstBuyRepository(db)
        buyRepo.saveBuyCheckout(
            entry = slip(paidPaisa = 200_000L),
            party = party,
            payments = emptyList(),
            loanCutPaisa = 200_000L
        )

        buyRepo.voidBuyEntry("b1", VoidReason.WRONG_ENTRY)
        // Second call
        buyRepo.voidBuyEntry("b1", VoidReason.WRONG_ENTRY)

        val l1 = lendRepo.getLendEntryById("l1")!!
        assertEquals(LendStatus.OPEN, l1.status)
    }

    @Test
    fun voidBuySlipPreservesLaterRepaymentOnLoan() = runTest {
        val db = createTestDatabase()
        val lendRepo = OfflineFirstLendRepository(db)
        lendRepo.saveLendEntry(
            LendEntry(
                id = "l1", shopId = "shop", partyId = "p1", lendType = LendType.MONEY,
                direction = LendDirection.GIVEN, entryDate = 1_000_000L, amountPaisa = 500_000L,
                createdAt = 1_000_000L, updatedAt = 1_000_000L
            )
        )
        lendRepo.saveLendEntry(
            LendEntry(
                id = "l2", shopId = "shop", partyId = "p1", lendType = LendType.MONEY,
                direction = LendDirection.GIVEN, entryDate = 2_000_000L, amountPaisa = 300_000L,
                createdAt = 2_000_000L, updatedAt = 2_000_000L
            )
        )

        val buyRepo = OfflineFirstBuyRepository(db)
        // Slip cut Rs 6,000 (l1: 5k settled, l2: 1k partial)
        buyRepo.saveBuyCheckout(
            entry = slip(paidPaisa = 600_000L),
            party = party,
            payments = emptyList(),
            loanCutPaisa = 600_000L
        )

        // Later direct repayment on l2: Rs 500 (50_000 paisa)
        lendRepo.settle(
            LendSettlement(
                id = "s-later", shopId = "shop", lendId = "l2", settledAt = 3_000_000L,
                amountPaisa = 50_000L, principalPaisa = 50_000L, interestPaisa = 0L,
                createdAt = 3_000_000L, updatedAt = 3_000_000L
            ),
            emptyList()
        )

        // Void the buy slip
        buyRepo.voidBuyEntry("b1", VoidReason.WRONG_ENTRY)

        // l1 is back to OPEN (its cut settlement was voided)
        assertEquals(LendStatus.OPEN, lendRepo.getLendEntryById("l1")!!.status)

        // l2 is still PARTIAL because the Rs 500 later repayment is untouched
        val l2 = lendRepo.getLendEntryById("l2")!!
        assertEquals(LendStatus.PARTIAL, l2.status)
        val liveSettlements = lendRepo.getSettlements("l2").filter { !it.isVoid }
        assertEquals(1, liveSettlements.size)
        assertEquals("s-later", liveSettlements.first().id)
        assertEquals(50_000L, liveSettlements.first().principalPaisa)
    }

    @Test
    fun voidBuySlipSkipsAlreadyVoidedLoanWithoutError() = runTest {
        val db = createTestDatabase()
        val lendRepo = OfflineFirstLendRepository(db)
        lendRepo.saveLendEntry(
            LendEntry(
                id = "l1", shopId = "shop", partyId = "p1", lendType = LendType.MONEY,
                direction = LendDirection.GIVEN, entryDate = 1_000_000L, amountPaisa = 500_000L,
                createdAt = 1_000_000L, updatedAt = 1_000_000L
            )
        )
        lendRepo.saveLendEntry(
            LendEntry(
                id = "l2", shopId = "shop", partyId = "p1", lendType = LendType.MONEY,
                direction = LendDirection.GIVEN, entryDate = 2_000_000L, amountPaisa = 300_000L,
                createdAt = 2_000_000L, updatedAt = 2_000_000L
            )
        )

        val buyRepo = OfflineFirstBuyRepository(db)
        buyRepo.saveBuyCheckout(
            entry = slip(paidPaisa = 600_000L),
            party = party,
            payments = emptyList(),
            loanCutPaisa = 600_000L
        )

        // Void l1 directly before voiding the slip
        lendRepo.voidLendEntry("l1", VoidReason.WRONG_ENTRY)

        // Void the buy slip - should not fail even though l1 is already void
        buyRepo.voidBuyEntry("b1", VoidReason.WRONG_ENTRY)

        // l1 remains void
        assertTrue(lendRepo.getLendEntryById("l1")!!.isVoid)
        // l2 becomes OPEN
        assertEquals(LendStatus.OPEN, lendRepo.getLendEntryById("l2")!!.status)
    }

    @Test
    fun twoSlipsSameFarmerVoidOnlySecondPreservesFirst() = runTest {
        val db = createTestDatabase()
        val lendRepo = OfflineFirstLendRepository(db)
        lendRepo.saveLendEntry(
            LendEntry(
                id = "l1", shopId = "shop", partyId = "p1", lendType = LendType.MONEY,
                direction = LendDirection.GIVEN, entryDate = 1_000_000L, amountPaisa = 1_000_000L,
                createdAt = 1_000_000L, updatedAt = 1_000_000L
            )
        )

        val buyRepo = OfflineFirstBuyRepository(db)
        // Slip 1 cut Rs 3,000 (300_000 paisa)
        buyRepo.saveBuyCheckout(
            entry = slip(slipNo = 1L, paidPaisa = 300_000L).copy(id = "b1"),
            party = party,
            payments = emptyList(),
            loanCutPaisa = 300_000L
        )

        // Slip 2 cut Rs 4,000 (400_000 paisa)
        buyRepo.saveBuyCheckout(
            entry = slip(slipNo = 2L, paidPaisa = 400_000L).copy(id = "b2"),
            party = party,
            payments = emptyList(),
            loanCutPaisa = 400_000L
        )

        assertEquals(2, lendRepo.getSettlements("l1").filter { !it.isVoid }.size)

        // Void slip 2 only
        buyRepo.voidBuyEntry("b2", VoidReason.WRONG_ENTRY)

        // Slip 1 settlement stays alive
        val liveSettlements = lendRepo.getSettlements("l1").filter { !it.isVoid }
        assertEquals(1, liveSettlements.size)
        assertEquals(300_000L, liveSettlements.first().principalPaisa)

        // l1 is PARTIAL with 300_000 repaid (700_000 remaining)
        val l1 = lendRepo.getLendEntryById("l1")!!
        assertEquals(LendStatus.PARTIAL, l1.status)
    }

    @Test
    fun getUnlinkedFarmerAdvancesBeforeExcludesUdhaarLoanRows() = runTest {
        val db = createTestDatabase()
        val lendRepo = OfflineFirstLendRepository(db)
        var idCounter = 0
        val udhaarUseCase = com.appwork.mandisamiti.domain.usecase.UdhaarUseCase(
            repo = lendRepo,
            newId = { "loan-${++idCounter}" },
            nowMillis = { 1_000_000L }
        )

        // 1. Save an Udhaar loan of Rs 5,000 for farmer p1
        val loanForm = com.appwork.mandisamiti.domain.usecase.UdhaarLoanForm(
            shopId = "shop", partyId = "p1", direction = LendDirection.GIVEN,
            amountPaisa = 500_000L, date = 1_000_000L
        )
        udhaarUseCase.createLoan(loanForm)

        // 2. Save a legacy direct advance of Rs 1,000 for farmer p1
        val legacyAdvance = CashTransaction(
            id = "c-adv", shopId = "shop", partyId = "p1", transactionType = TransactionType.UDHAR_GIVEN,
            amountPaisa = 100_000L, paymentMode = PaymentMode.CASH, transactionDate = 1_000_000L,
            createdAt = 1_000_000L, updatedAt = 1_000_000L, lendId = null
        )
        db.appDatabaseQueries.insertCashTransaction(
            id = legacyAdvance.id, shop_id = legacyAdvance.shopId, party_id = legacyAdvance.partyId, deal_id = null,
            transaction_type = legacyAdvance.transactionType.name, amount_paisa = legacyAdvance.amountPaisa,
            payment_mode = legacyAdvance.paymentMode.name, transaction_date = legacyAdvance.transactionDate,
            voice_note_uri = null, remarks = "पुरानी पेशगी", created_at = legacyAdvance.createdAt,
            updated_at = legacyAdvance.updatedAt, is_deleted = 0L, sync_status = 0L, revision = 1L,
            is_void = 0L, void_reason = null, source_entry_type = null, source_entry_id = null, lend_id = null
        )

        // 3. Query unlinked advances before a purchase at 2_000_000L
        val advances = db.bahiDayQueriesQueries.getUnlinkedFarmerAdvancesBefore(
            shopId = "shop", farmerId = "p1", purchaseMillis = 2_000_000L
        ).executeAsList()

        // MUST contain only the legacy advance, NOT the loan row!
        assertEquals(1, advances.size)
        assertEquals("c-adv", advances.first().id)
    }
}
