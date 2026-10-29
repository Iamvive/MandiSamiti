package com.appwork.mandisamiti.ui.expense

import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstExpenseRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.cash.CashDrawer
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.ExpenseCategory
import com.appwork.mandisamiti.domain.model.ExpenseEntry
import com.appwork.mandisamiti.domain.model.ExpensePaymentMode
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.ui.dashboard.computeKpis
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

class ExpenseFlowTest {

    private val fixedNow = Instant.parse("2026-10-28T07:00:00Z")
    private val testClock = object : Clock {
        override fun now() = fixedNow
    }

    @Test
    fun tcExp01_standardCashExpense_savesExpenseAndReducesCashDrawer() = runTest {
        val db = createTestDatabase()
        val expenseRepo = OfflineFirstExpenseRepository(
            database = db,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clock = testClock
        )
        val cashRepo = OfflineFirstCashTransactionRepository(
            database = db,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clock = testClock
        )

        // Initial cash in drawer: ₹1,000 received
        val openingTx = CashTransaction(
            id = "opening-1",
            shopId = "shop-test",
            partyId = "opening-balance",
            transactionType = TransactionType.JAMA_RECEIVED,
            amountPaisa = 100_000L, // ₹1,000
            paymentMode = PaymentMode.CASH,
            transactionDate = fixedNow.toEpochMilliseconds(),
            createdAt = fixedNow.toEpochMilliseconds(),
            updatedAt = fixedNow.toEpochMilliseconds(),
        )
        cashRepo.recordTransaction(openingTx)

        val initialAllTx = cashRepo.getTransactionsByShopStream("shop-test").first()
        val window = CashDrawer.dayWindow(fixedNow, TimeZone.UTC)
        val initialSummary = CashDrawer.summarize(initialAllTx, window)
        assertEquals(100_000L, initialSummary.closingPaisa)

        // Expense: Chai-Pani ₹150 in CASH
        val amountRupees = "150"
        val amountPaisa = MandiMathEngine.parseRupeesToPaisa(amountRupees)
        assertEquals(15_000L, amountPaisa)

        val expense = ExpenseEntry(
            id = "exp-1",
            shopId = "shop-test",
            entryDate = fixedNow.toEpochMilliseconds(),
            category = ExpenseCategory.CHAI_PANI,
            amountPaisa = amountPaisa,
            paidTo = "दुकान चायवाला",
            paymentMode = ExpensePaymentMode.CASH,
            remarks = "सुबह की चाय-नाश्ता",
            createdAt = fixedNow.toEpochMilliseconds(),
            updatedAt = fixedNow.toEpochMilliseconds(),
        )
        expenseRepo.saveExpense(expense)

        // Record cash outflow
        cashRepo.recordTransaction(
            CashTransaction(
                id = IdGenerator.newId(),
                shopId = "shop-test",
                partyId = expense.paidTo ?: expense.category.name,
                transactionType = TransactionType.UDHAR_GIVEN, // Cash out
                amountPaisa = expense.amountPaisa,
                paymentMode = PaymentMode.CASH,
                transactionDate = expense.entryDate,
                remarks = "खर्च: ${expense.category.labelHi()} ${expense.remarks ?: ""}".trim(),
                createdAt = expense.createdAt,
                updatedAt = expense.updatedAt,
            )
        )

        // Verify expense in repository
        val saved = expenseRepo.getExpenseById("exp-1")
        assertEquals(expense, saved)
        assertEquals(ExpenseCategory.CHAI_PANI, saved?.category)
        assertEquals(15_000L, saved?.amountPaisa)

        // Verify cash drawer reduced by ₹150 (₹1,000 - ₹150 = ₹850 = 85,000 paisa)
        val updatedTx = cashRepo.getTransactionsByShopStream("shop-test").first()
        val updatedSummary = CashDrawer.summarize(updatedTx, window)
        assertEquals(15_000L, updatedSummary.cashOutPaisa)
        assertEquals(85_000L, updatedSummary.closingPaisa)
    }

    @Test
    fun tcExp02_hammaliLabourBatch_recordsPayeeAndCategory() = runTest {
        val db = createTestDatabase()
        val expenseRepo = OfflineFirstExpenseRepository(
            database = db,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clock = testClock
        )

        val amountRupees = "2400"
        val amountPaisa = MandiMathEngine.parseRupeesToPaisa(amountRupees)
        assertEquals(240_000L, amountPaisa)

        val expense = ExpenseEntry(
            id = "exp-hammali-1",
            shopId = "shop-test",
            entryDate = fixedNow.toEpochMilliseconds(),
            category = ExpenseCategory.HAMMALI,
            amountPaisa = amountPaisa,
            paidTo = "मजदूर टोली",
            paymentMode = ExpensePaymentMode.CASH,
            remarks = "48 बोरी उतराई एवं भराई",
            createdAt = fixedNow.toEpochMilliseconds(),
            updatedAt = fixedNow.toEpochMilliseconds(),
        )
        expenseRepo.saveExpense(expense)

        val retrieved = expenseRepo.getExpenseById("exp-hammali-1")
        assertEquals(expense, retrieved)
        assertEquals("हम्माली", retrieved?.category?.labelHi())
        assertEquals("मजदूर टोली", retrieved?.paidTo)
        assertEquals(240_000L, retrieved?.amountPaisa)
    }

    @Test
    fun tcExp03_zeroAmountValidation_disabled() {
        val zeroPaisa = MandiMathEngine.parseRupeesToPaisa("0")
        assertEquals(0L, zeroPaisa)

        val blankPaisa = MandiMathEngine.parseRupeesToPaisa("")
        assertEquals(0L, blankPaisa)

        val dotZeroPaisa = MandiMathEngine.parseRupeesToPaisa("0.00")
        assertEquals(0L, dotZeroPaisa)

        fun canSaveExpense(amount: String): Boolean {
            return amount.isNotBlank() && MandiMathEngine.parseRupeesToPaisa(amount) > 0L
        }

        assertFalse(canSaveExpense("0"))
        assertFalse(canSaveExpense(""))
        assertFalse(canSaveExpense("0.00"))
        assertFalse(canSaveExpense("abc"))
        assertTrue(canSaveExpense("150"))
        assertTrue(canSaveExpense("2400.50"))
    }

    @Test
    fun dashboardKpiReflection_expensesDeductFromProfit() = runTest {
        val db = createTestDatabase()
        val expenseRepo = OfflineFirstExpenseRepository(
            database = db,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clock = testClock
        )

        // 2 expenses: Hammali ₹2,400 + Chai-Pani ₹150 = ₹2,550 (255,000 paisa)
        expenseRepo.saveExpense(
            ExpenseEntry(
                id = "exp-1",
                shopId = "shop-test",
                entryDate = fixedNow.toEpochMilliseconds(),
                category = ExpenseCategory.HAMMALI,
                amountPaisa = 240_000L,
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds(),
            )
        )
        expenseRepo.saveExpense(
            ExpenseEntry(
                id = "exp-2",
                shopId = "shop-test",
                entryDate = fixedNow.toEpochMilliseconds(),
                category = ExpenseCategory.CHAI_PANI,
                amountPaisa = 15_000L,
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds(),
            )
        )

        val expenses = expenseRepo.getExpensesStream("shop-test").first()
        val kpis = computeKpis(
            buys = emptyList(),
            sells = emptyList(),
            lends = emptyList(),
            expenses = expenses
        )

        assertEquals(255_000L, kpis.totalExpensePaisa)
        // Profit = Sales (0) - Buys (0) - Expenses (255,000) = -255,000 paisa (Loss)
        assertEquals(-255_000L, kpis.profitLossPaisa)
    }

    @Test
    fun tcExp05_voidExpense_dropsTotalExpensesKpi() = runTest {
        val db = createTestDatabase()
        val expenseRepo = OfflineFirstExpenseRepository(
            database = db,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clock = testClock
        )
        val cashRepo = OfflineFirstCashTransactionRepository(
            database = db,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clock = testClock
        )

        // Expense 1: ₹1,500
        val exp1 = ExpenseEntry(
            id = "exp-1",
            shopId = "shop-test",
            entryDate = fixedNow.toEpochMilliseconds(),
            category = ExpenseCategory.MAJDOORI,
            amountPaisa = 150_000L, // ₹1,500
            paidTo = "मजदूर",
            paymentMode = ExpensePaymentMode.CASH,
            createdAt = fixedNow.toEpochMilliseconds(),
            updatedAt = fixedNow.toEpochMilliseconds(),
        )
        expenseRepo.saveExpense(exp1)
        cashRepo.recordTransaction(
            CashTransaction(
                id = "cash-exp-1",
                shopId = "shop-test",
                partyId = "मजदूर",
                transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = 150_000L,
                paymentMode = PaymentMode.CASH,
                transactionDate = fixedNow.toEpochMilliseconds(),
                remarks = "खर्च: मजदूरी [EXPENSE:exp-1]",
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds(),
            )
        )

        // Expense 2: ₹300
        val exp2 = ExpenseEntry(
            id = "exp-2",
            shopId = "shop-test",
            entryDate = fixedNow.toEpochMilliseconds(),
            category = ExpenseCategory.CHAI_PANI,
            amountPaisa = 30_000L, // ₹300
            paidTo = "दुकान चायवाला",
            paymentMode = ExpensePaymentMode.CASH,
            createdAt = fixedNow.toEpochMilliseconds(),
            updatedAt = fixedNow.toEpochMilliseconds(),
        )
        expenseRepo.saveExpense(exp2)
        cashRepo.recordTransaction(
            CashTransaction(
                id = "cash-exp-2",
                shopId = "shop-test",
                partyId = "दुकान चायवाला",
                transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = 30_000L,
                paymentMode = PaymentMode.CASH,
                transactionDate = fixedNow.toEpochMilliseconds(),
                remarks = "खर्च: चाय-पानी [EXPENSE:exp-2]",
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds(),
            )
        )

        // Initial KPI check: total expenses = ₹1,800 (180,000 paisa)
        val initialExpenses = expenseRepo.getExpensesStream("shop-test").first()
        val initialKpis = computeKpis(
            buys = emptyList(),
            sells = emptyList(),
            lends = emptyList(),
            expenses = initialExpenses
        )
        assertEquals(180_000L, initialKpis.totalExpensePaisa)

        // Void the ₹300 expense
        expenseRepo.voidExpense("exp-2", VoidReason.WRONG_ENTRY)

        // Updated KPI check: total expenses should drop to ₹1,500 (150,000 paisa)
        val updatedExpenses = expenseRepo.getExpensesStream("shop-test").first()
        val updatedKpis = computeKpis(
            buys = emptyList(),
            sells = emptyList(),
            lends = emptyList(),
            expenses = updatedExpenses
        )
        assertEquals(150_000L, updatedKpis.totalExpensePaisa)
        // R5: the voided row stays in the list (struck through in the UI), only the total drops it
        val voidedRow = updatedExpenses.single { it.id == "exp-2" }
        assertTrue(voidedRow.isVoid)
        assertEquals(initialExpenses.size, updatedExpenses.size)

        // Verify linked cash transaction is also voided
        val cashTx2 = db.appDatabaseQueries.getCashTransactionById("cash-exp-2").executeAsOneOrNull()
        assertTrue(cashTx2?.is_void == 1L)
    }

    @Test
    fun tcExp06_expensePayeeDoesNotAffectPartyBalanceOrLedger() = runTest {
        val db = createTestDatabase()
        val expenseRepo = OfflineFirstExpenseRepository(
            database = db,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clock = testClock
        )
        val cashRepo = OfflineFirstCashTransactionRepository(
            database = db,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clock = testClock
        )

        val partyId = "party-ramesh"
        val now = fixedNow.toEpochMilliseconds()
        db.appDatabaseQueries.insertParty(
            id = partyId,
            shop_id = "shop-test",
            name = "Ramesh Patel",
            phone = "9876543210",
            village = "Khedi",
            party_type = "FARMER",
            monthly_interest_rate = 2.0,
            photo_uri = null,
            created_at = now,
            updated_at = now,
            is_deleted = 0L,
            sync_status = 0L
        )

        // Initial balance should be 0
        val initialBalance = db.appDatabaseQueries.getPartyBalance(partyId).executeAsOneOrNull()
        assertEquals(0L, initialBalance?.balance_paisa ?: 0L)

        // Record an expense paid to Ramesh (free text payee)
        val expense = ExpenseEntry(
            id = "exp-ramesh-1",
            shopId = "shop-test",
            entryDate = now,
            category = ExpenseCategory.HAMMALI,
            amountPaisa = 30_000L, // ₹300
            paidTo = "Ramesh Patel",
            paymentMode = ExpensePaymentMode.CASH,
            remarks = "उतराई",
            createdAt = now,
            updatedAt = now,
        )
        expenseRepo.saveExpense(expense)

        // Follow App.kt logic: partyId is EXPENSE_<CAT> and remarks contain [EXPENSE:id]
        cashRepo.recordTransaction(
            CashTransaction(
                id = "cash-exp-r1",
                shopId = "shop-test",
                partyId = "EXPENSE_${expense.category.name}",
                transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = expense.amountPaisa,
                paymentMode = PaymentMode.CASH,
                transactionDate = expense.entryDate,
                remarks = "खर्च: ${expense.category.labelHi()} (${expense.paidTo}) [EXPENSE:${expense.id}]",
                createdAt = expense.createdAt,
                updatedAt = expense.updatedAt,
            )
        )

        // Also test defensive safeguard: even if cash transaction had partyId = partyId with [EXPENSE:id]
        cashRepo.recordTransaction(
            CashTransaction(
                id = "cash-exp-r2",
                shopId = "shop-test",
                partyId = partyId,
                transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = 50_000L, // ₹500
                paymentMode = PaymentMode.CASH,
                transactionDate = expense.entryDate,
                remarks = "खर्च: मजदूरी (${expense.paidTo}) [EXPENSE:exp-dummy]",
                createdAt = expense.createdAt,
                updatedAt = expense.updatedAt,
            )
        )

        // Party balance must remain strictly 0 (neither EXPENSE_HAMMALI nor [EXPENSE:...] affects party balance)
        val afterExpenseBalance = db.appDatabaseQueries.getPartyBalance(partyId).executeAsOneOrNull()
        assertEquals(0L, afterExpenseBalance?.balance_paisa ?: 0L)
    }
}
