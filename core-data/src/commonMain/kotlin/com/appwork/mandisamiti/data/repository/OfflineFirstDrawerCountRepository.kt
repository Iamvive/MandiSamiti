package com.appwork.mandisamiti.data.repository

import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.domain.repository.DrawerCount
import com.appwork.mandisamiti.domain.repository.DrawerCountRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class OfflineFirstDrawerCountRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : DrawerCountRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private fun key(shopId: String, date: String) = "drawer_count:${shopId.length}:$shopId:$date"

    override suspend fun load(shopId: String, date: String): DrawerCount? = withContext(ioDispatcher) {
        database.appDatabaseQueries.getDrawerCount(key(shopId, date)).executeAsOneOrNull()?.value_text
            ?.let { json.decodeFromString<DrawerCount>(it) }
    }

    override suspend fun save(shopId: String, date: String, count: DrawerCount?) = withContext(ioDispatcher) {
        database.appDatabaseQueries.setDrawerCount(key(shopId, date), count?.let { json.encodeToString(it) })
    }
}
