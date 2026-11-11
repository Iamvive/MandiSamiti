package com.appwork.mandisamiti.data.repository

import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.domain.model.StockAudit
import com.appwork.mandisamiti.domain.repository.StockAuditRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class OfflineFirstStockAuditRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : StockAuditRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private fun key(shopId: String) = "stock_audits:$shopId"

    override suspend fun loadAudits(shopId: String): Map<String, StockAudit> = withContext(ioDispatcher) {
        val raw = database.appDatabaseQueries.getDrawerCount(key(shopId)).executeAsOneOrNull()?.value_text
        if (raw.isNullOrBlank()) return@withContext emptyMap()
        try {
            json.decodeFromString<Map<String, StockAudit>>(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    override suspend fun saveAudit(shopId: String, audit: StockAudit) = withContext(ioDispatcher) {
        val current = loadAudits(shopId).toMutableMap()
        current[audit.commodityId] = audit
        database.appDatabaseQueries.setDrawerCount(key(shopId), json.encodeToString(current))
    }
}

