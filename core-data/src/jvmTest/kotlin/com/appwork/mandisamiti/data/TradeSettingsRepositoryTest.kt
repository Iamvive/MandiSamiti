package com.appwork.mandisamiti.data

import app.cash.turbine.test
import com.appwork.mandisamiti.data.repository.OfflineFirstTradeSettingsRepository
import com.appwork.mandisamiti.database.DriverFactory
import com.appwork.mandisamiti.database.createDatabase
import com.appwork.mandisamiti.domain.model.TradeSettings
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TradeSettingsRepositoryTest {

    @Test
    fun testDefaultSettingsAndPersistence() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val database = createDatabase(DriverFactory())
        val repo = OfflineFirstTradeSettingsRepository(database, dispatcher)

        // Initial defaults
        val initial = repo.getTradeSettings()
        assertEquals(150L, initial.farmerCommissionBps)
        assertEquals(150L, initial.buyerCommissionBps)
        assertEquals(15000L, initial.defaultLabourPaisa)
        assertEquals(35000L, initial.defaultTareGrams)

        // Save customized settings
        val custom = TradeSettings(
            farmerCommissionBps = 200L,   // 2.0%
            buyerCommissionBps = 100L,    // 1.0%
            defaultLabourPaisa = 18000L,  // ₹180
            defaultTareGrams = 40000L     // 0.40 qtl
        )
        repo.saveTradeSettings(custom)

        val retrieved = repo.getTradeSettings()
        assertEquals(200L, retrieved.farmerCommissionBps)
        assertEquals(100L, retrieved.buyerCommissionBps)
        assertEquals(18000L, retrieved.defaultLabourPaisa)
        assertEquals(40000L, retrieved.defaultTareGrams)

        // Verify fresh repository instance sees persisted values from SQLite
        val repo2 = OfflineFirstTradeSettingsRepository(database, dispatcher)
        val fromDb = repo2.getTradeSettings()
        assertEquals(200L, fromDb.farmerCommissionBps)
        assertEquals(100L, fromDb.buyerCommissionBps)
        assertEquals(18000L, fromDb.defaultLabourPaisa)
        assertEquals(40000L, fromDb.defaultTareGrams)
    }

    @Test
    fun testReactiveSettingsStream() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val database = createDatabase(DriverFactory())
        val repo = OfflineFirstTradeSettingsRepository(database, dispatcher)

        repo.getTradeSettingsStream().test {
            val first = awaitItem()
            assertEquals(150L, first.farmerCommissionBps)

            val updated = TradeSettings(farmerCommissionBps = 250L, buyerCommissionBps = 200L, defaultLabourPaisa = 20000L, defaultTareGrams = 30000L)
            repo.saveTradeSettings(updated)

            val second = awaitItem()
            assertEquals(250L, second.farmerCommissionBps)
            assertEquals(200L, second.buyerCommissionBps)
        }
    }
}
