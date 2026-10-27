package com.appwork.mandisamiti.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.ExpenseEntryEntity
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.model.ExpenseCategory
import com.appwork.mandisamiti.domain.model.ExpenseEntry
import com.appwork.mandisamiti.domain.model.ExpensePaymentMode
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.domain.repository.ExpenseRepository
import com.appwork.mandisamiti.domain.repository.StockTotals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

class OfflineFirstExpenseRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: Clock = Clock.System,
) : ExpenseRepository {

    private val queries = database.appDatabaseQueries
    private val json = Json { encodeDefaults = true }

    override fun getExpensesStream(shopId: String): Flow<List<ExpenseEntry>> =
        queries.getExpensesByShop(shopId).asFlow().mapToList(ioDispatcher).map { list -> list.map { it.toDomain() } }

    override suspend fun getExpenseById(id: String): ExpenseEntry? = withContext(ioDispatcher) {
        queries.getExpenseById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun saveExpense(entry: ExpenseEntry) = withContext(ioDispatcher) {
        database.transaction {
            val created = entry.copy(revision = 1, syncStatus = 0)
            insertOrReplace(created)
            recordRevision(created, "CREATE", created.updatedAt)
        }
    }

    override suspend fun voidExpense(id: String, reason: VoidReason) = withContext(ioDispatcher) {
        val now = clock.now().toEpochMilliseconds()
        database.transaction {
            val current = queries.getExpenseById(id).executeAsOneOrNull()?.toDomain()
                ?: throw IllegalArgumentException("Expense $id does not exist")
            if (current.isVoid) return@transaction
            val voided = current.copy(
                isVoid = true, voidReason = reason.name, revision = current.revision + 1,
                updatedAt = now, syncStatus = 0,
            )
            insertOrReplace(voided)
            recordRevision(voided, "VOID", now)
        }
    }

    override suspend fun stockTotals(shopId: String): StockTotals = withContext(ioDispatcher) {
        val bought = queries.getStockBuyTotals(shopId).executeAsList().associate { it.commodity_id to it.grams }
        val sold = queries.getStockSellTotals(shopId).executeAsList().associate { it.commodity_id to it.grams }
        StockTotals(bought, sold)
    }

    private fun recordRevision(entry: ExpenseEntry, changeKind: String, at: Long) {
        queries.insertRevision(
            id = IdGenerator.newId(),
            shop_id = entry.shopId,
            entry_id = entry.id,
            entry_kind = "EXPENSE",
            revision = entry.revision,
            change_kind = changeKind,
            snapshot_json = json.encodeToString(ExpenseEntry.serializer(), entry),
            void_reason = entry.voidReason,
            changed_at = at,
        )
    }

    private fun insertOrReplace(entry: ExpenseEntry) {
        queries.insertExpenseEntry(
            id = entry.id,
            shop_id = entry.shopId,
            entry_date = entry.entryDate,
            category = entry.category.name,
            amount_paisa = entry.amountPaisa,
            paid_to = entry.paidTo,
            payment_mode = entry.paymentMode.name,
            remarks = entry.remarks,
            created_at = entry.createdAt,
            updated_at = entry.updatedAt,
            is_deleted = 0L,
            sync_status = entry.syncStatus,
            revision = entry.revision,
            is_void = if (entry.isVoid) 1L else 0L,
            void_reason = entry.voidReason,
        )
    }

    private fun ExpenseEntryEntity.toDomain(): ExpenseEntry = ExpenseEntry(
        id = id,
        shopId = shop_id,
        entryDate = entry_date,
        category = ExpenseCategory.valueOf(category),
        amountPaisa = amount_paisa,
        paidTo = paid_to,
        paymentMode = ExpensePaymentMode.valueOf(payment_mode),
        remarks = remarks,
        createdAt = created_at,
        updatedAt = updated_at,
        revision = revision,
        isVoid = is_void == 1L,
        voidReason = void_reason,
        syncStatus = sync_status,
    )
}
