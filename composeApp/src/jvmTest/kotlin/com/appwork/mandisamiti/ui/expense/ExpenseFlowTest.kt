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
}
