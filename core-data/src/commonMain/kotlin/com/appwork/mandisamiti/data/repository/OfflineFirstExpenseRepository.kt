package com.appwork.mandisamiti.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.ExpenseEntryEntity
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.model.ExpenseCategory
import com.appwork.mandisamiti.domain.model.ExpenseEntry
import com.appwork.mandisamiti.domain.model.ExpensePaymentMode
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
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

    override suspend fun editExpense(entry: ExpenseEntry, cashRemarks: String) = withContext(ioDispatcher) {
        require(entry.amountPaisa > 0) { "Amount must be positive" }
        val now = clock.now().toEpochMilliseconds()
        database.transaction {
            val current = queries.getExpenseById(entry.id).executeAsOneOrNull()?.toDomain()
                ?: throw IllegalArgumentException("Expense ${entry.id} does not exist")
            require(!current.isVoid) { "Voided expense cannot be edited" }
            val edited = current.copy(
                category = entry.category, amountPaisa = entry.amountPaisa, paidTo = entry.paidTo,
                paymentMode = entry.paymentMode, remarks = entry.remarks,
                revision = current.revision + 1, updatedAt = now, syncStatus = 0,
            )
            insertOrReplace(edited)
            recordRevision(edited, "EDIT", now)

            val tag = "[EXPENSE:${entry.id}]"
            val linked = queries.getCashTransactionsByTag(tag).executeAsList().filter { it.is_void == 0L }
            if (edited.paymentMode != ExpensePaymentMode.CASH) {
                linked.forEach { tx -> writeCash(tx, tx.amount_paisa, tx.party_id, tx.remarks, now, void = true) }
            } else if (linked.isEmpty()) {
                queries.insertCashTransaction(
                    id = IdGenerator.newId(), shop_id = edited.shopId, party_id = "EXPENSE_${edited.category.name}",
                    deal_id = null, transaction_type = TransactionType.UDHAR_GIVEN.name, amount_paisa = edited.amountPaisa,
                    payment_mode = PaymentMode.CASH.name, transaction_date = edited.entryDate, voice_note_uri = null,
                    remarks = "$cashRemarks $tag", created_at = now, updated_at = now, is_deleted = 0L,
                    sync_status = 0L, revision = 1L, is_void = 0L, void_reason = null,
                )
            } else {
                linked.forEach { tx ->
                    writeCash(tx, edited.amountPaisa, "EXPENSE_${edited.category.name}", "$cashRemarks $tag", now, void = false)
                }
            }
        }
    }

    private fun writeCash(
        tx: com.appwork.mandisamiti.database.CashTransactionEntity, amountPaisa: Long, partyId: String,
        remarks: String?, now: Long, void: Boolean,
    ) {
        queries.insertCashTransaction(
            id = tx.id, shop_id = tx.shop_id, party_id = partyId, deal_id = tx.deal_id,
            transaction_type = tx.transaction_type, amount_paisa = amountPaisa, payment_mode = tx.payment_mode,
            transaction_date = tx.transaction_date, voice_note_uri = tx.voice_note_uri, remarks = remarks,
            created_at = tx.created_at, updated_at = now, is_deleted = tx.is_deleted, sync_status = 0L,
            revision = tx.revision + 1L, is_void = if (void) 1L else 0L,
            void_reason = if (void) VoidReason.WRONG_ENTRY.name else null,
        )
        queries.insertRevision(
            id = IdGenerator.newId(), shop_id = tx.shop_id, entry_id = tx.id, entry_kind = "CASH",
            revision = tx.revision + 1L, change_kind = if (void) "VOID" else "EDIT", snapshot_json = "",
            void_reason = if (void) VoidReason.WRONG_ENTRY.name else null, changed_at = now,
        )
    }

    override suspend fun voidExpense(id: String, reason: VoidReason) = withContext(ioDispatcher) {
        val now = clock.now().toEpochMilliseconds()
        database.transaction { voidExpenseInTransaction(id, reason, now) }
    }

    /** Voids the expense and its linked cash rows. Must run inside the caller's DB transaction. */
    internal fun voidExpenseInTransaction(id: String, reason: VoidReason, now: Long) {
        val current = queries.getExpenseById(id).executeAsOneOrNull()?.toDomain()
            ?: throw IllegalArgumentException("Expense $id does not exist")
        if (current.isVoid) return
        val voided = current.copy(
            isVoid = true, voidReason = reason.name, revision = current.revision + 1,
            updatedAt = now, syncStatus = 0,
        )
        insertOrReplace(voided)
        recordRevision(voided, "VOID", now)

        // Cascade to linked cash transactions
        val expTagMatches = queries.getCashTransactionsByTag("[EXPENSE:$id]").executeAsList()
        expTagMatches.filter { it.is_void == 0L }.forEach { txEntity ->
            queries.insertCashTransaction(
                id = txEntity.id,
                shop_id = txEntity.shop_id,
                party_id = txEntity.party_id,
                deal_id = txEntity.deal_id,
                transaction_type = txEntity.transaction_type,
                amount_paisa = txEntity.amount_paisa,
                payment_mode = txEntity.payment_mode,
                transaction_date = txEntity.transaction_date,
                voice_note_uri = txEntity.voice_note_uri,
                remarks = txEntity.remarks,
                created_at = txEntity.created_at,
                updated_at = now,
                is_deleted = txEntity.is_deleted,
                sync_status = 0L,
                revision = txEntity.revision + 1L,
                is_void = 1L,
                void_reason = reason.name
            )
            queries.insertRevision(
                id = IdGenerator.newId(),
                shop_id = txEntity.shop_id,
                entry_id = txEntity.id,
                entry_kind = "CASH",
                revision = txEntity.revision + 1L,
                change_kind = "VOID",
                snapshot_json = "",
                void_reason = reason.name,
                changed_at = now
            )
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
