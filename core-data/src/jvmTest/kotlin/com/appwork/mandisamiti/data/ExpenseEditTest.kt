package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstExpenseRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.ExpenseCategory
import com.appwork.mandisamiti.domain.model.ExpenseEntry
import com.appwork.mandisamiti.domain.model.ExpensePaymentMode
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExpenseEditTest {
    private val clock = object : Clock { override fun now() = Instant.fromEpochMilliseconds(5_000L) }

    private fun expense(mode: ExpensePaymentMode = ExpensePaymentMode.CASH) = ExpenseEntry(
        id = "e1", shopId = "shop", entryDate = 1_000L, category = ExpenseCategory.HAMMALI,
        amountPaisa = 10_000L, paymentMode = mode, createdAt = 1_000L, updatedAt = 1_000L,
    )

    private fun linkedCash(db: com.appwork.mandisamiti.database.AppDatabase) =
        db.appDatabaseQueries.getCashTransactionsByTag("[EXPENSE:e1]").executeAsList()

    @Test
    fun editUpdatesExpenseAndCreatesCashRowWhenModeBecomesCash() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstExpenseRepository(db, clock = clock)
        repo.saveExpense(expense(ExpensePaymentMode.UPI))
        assertEquals(0, linkedCash(db).size)

        repo.editExpense(expense().copy(amountPaisa = 25_000L, category = ExpenseCategory.KIRAYA), "खर्च: किराया")

        val saved = repo.getExpenseById("e1")!!
        assertEquals(25_000L, saved.amountPaisa)
        assertEquals(2L, saved.revision)
        val cash = linkedCash(db).single()
        assertEquals(25_000L, cash.amount_paisa)
        assertEquals("EXPENSE_KIRAYA", cash.party_id)
    }

    @Test
    fun editCascadesAmountToExistingCashRow() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstExpenseRepository(db, clock = clock)
        repo.saveExpense(expense())
        repo.editExpense(expense(), "खर्च: हम्माली")
        repo.editExpense(expense().copy(amountPaisa = 40_000L), "खर्च: हम्माली")

        val cash = linkedCash(db).single()
        assertEquals(40_000L, cash.amount_paisa)
        assertEquals(3L, repo.getExpenseById("e1")!!.revision)
    }

    @Test
    fun editVoidsCashRowWhenModeLeavesCash() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstExpenseRepository(db, clock = clock)
        repo.saveExpense(expense())
        repo.editExpense(expense(), "खर्च: हम्माली")
        repo.editExpense(expense(ExpensePaymentMode.UPI), "खर्च: हम्माली")

        assertEquals(0, linkedCash(db).size)
    }

    @Test
    fun editRejectsVoidedExpense() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstExpenseRepository(db, clock = clock)
        repo.saveExpense(expense())
        repo.voidExpense("e1", com.appwork.mandisamiti.domain.model.VoidReason.WRONG_ENTRY)
        assertFailsWith<IllegalArgumentException> { repo.editExpense(expense(), "x") }
    }
}
