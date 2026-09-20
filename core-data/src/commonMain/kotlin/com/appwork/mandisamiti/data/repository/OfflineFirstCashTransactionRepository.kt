package com.appwork.mandisamiti.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.CashTransactionEntity
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.repository.CashTransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class OfflineFirstCashTransactionRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : CashTransactionRepository {

    private val queries = database.appDatabaseQueries

    override fun getTransactionsByPartyStream(partyId: String): Flow<List<CashTransaction>> {
        return queries.getCashTransactionsByParty(partyId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsByShopStream(shopId: String): Flow<List<CashTransaction>> {
        return queries.getCashTransactionsByShop(shopId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun recordTransaction(transaction: CashTransaction) = withContext(ioDispatcher) {
        queries.insertCashTransaction(
            id = transaction.id,
            shop_id = transaction.shopId,
            party_id = transaction.partyId,
            deal_id = transaction.dealId,
            transaction_type = transaction.transactionType.name,
            amount_paisa = transaction.amountPaisa,
            payment_mode = transaction.paymentMode.name,
            transaction_date = transaction.transactionDate,
            voice_note_uri = transaction.voiceNoteUri,
            remarks = transaction.remarks,
            created_at = transaction.createdAt,
            updated_at = transaction.updatedAt,
            is_deleted = if (transaction.isDeleted) 1L else 0L,
            sync_status = transaction.syncStatus.toLong()
        )
    }

    override suspend fun deleteTransaction(transactionId: String) = withContext(ioDispatcher) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        queries.softDeleteCashTransaction(updated_at = now, id = transactionId)
    }

    private fun CashTransactionEntity.toDomain(): CashTransaction {
        return CashTransaction(
            id = id,
            shopId = shop_id,
            partyId = party_id,
            dealId = deal_id,
            transactionType = TransactionType.valueOf(transaction_type),
            amountPaisa = amount_paisa,
            paymentMode = PaymentMode.valueOf(payment_mode),
            transactionDate = transaction_date,
            voiceNoteUri = voice_note_uri,
            remarks = remarks,
            createdAt = created_at,
            updatedAt = updated_at,
            isDeleted = is_deleted == 1L,
            syncStatus = sync_status.toInt()
        )
    }
}
