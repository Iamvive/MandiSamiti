package com.appwork.mandisamiti.ui.deal

import com.appwork.mandisamiti.data.auth.InMemorySessionStore
import com.appwork.mandisamiti.data.auth.Session
import com.appwork.mandisamiti.data.repository.QuotaRepository
import com.appwork.mandisamiti.database.DriverFactory
import com.appwork.mandisamiti.database.createDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DealQuotaInterceptorTest {

    @Test
    fun testNormalSaveWhenQuotaAvailableIncrementsBillCount() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val database = createDatabase(DriverFactory())
        val sessionStore = InMemorySessionStore().apply {
            save(Session("shop-1", "access", "refresh"))
        }
        val quotaRepository = QuotaRepository(
            database = database,
            sessionStore = sessionStore,
            ioDispatcher = dispatcher
        )
        quotaRepository.setMockQuota(
            shopId = "shop-1",
            monthlyLimit = 50,
            used = 10,
            emergencyExtensionsUsed = 0,
            emergencyBillsAdded = 0,
            planTier = "FREE"
        )

        val interceptor = DealQuotaInterceptor(quotaRepository)
        var saveExecuted = false

        val state = interceptor.checkQuotaAndExecuteSave("shop-1") {
            saveExecuted = true
        }

        assertTrue(saveExecuted)
        assertTrue(state is DealSaveState.Success)
        val updatedQuota = quotaRepository.getQuotaStatus("shop-1").first()
        assertEquals(11L, updatedQuota.billsCreatedThisMonth)
    }

    @Test
    fun testSaveInterceptedWhenQuotaFullAndEmergencyAvailable() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val database = createDatabase(DriverFactory())
        val sessionStore = InMemorySessionStore().apply {
            save(Session("shop-1", "access", "refresh"))
        }
        val quotaRepository = QuotaRepository(
            database = database,
            sessionStore = sessionStore,
            ioDispatcher = dispatcher
        )
        // Limit 50, used 50, emergency 0 of 2 used
        quotaRepository.setMockQuota(
            shopId = "shop-1",
            monthlyLimit = 50,
            used = 50,
            emergencyExtensionsUsed = 0,
            emergencyBillsAdded = 0,
            planTier = "FREE"
        )

        val interceptor = DealQuotaInterceptor(quotaRepository)
        var saveExecuted = false

        val state = interceptor.checkQuotaAndExecuteSave("shop-1") {
            saveExecuted = true
        }

        kotlin.test.assertFalse(saveExecuted)
        assertTrue(state is DealSaveState.EmergencyExtensionPrompt)
        val prompt = state as DealSaveState.EmergencyExtensionPrompt
        assertEquals(50L, prompt.quota.billsCreatedThisMonth)
        assertEquals(50L, prompt.quota.effectiveLimit)
        assertEquals(0L, prompt.quota.emergencyExtensionsUsed)
    }

    @Test
    fun testSaveInterceptedWhenQuotaFullAndEmergencyExhausted() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val database = createDatabase(DriverFactory())
        val sessionStore = InMemorySessionStore().apply {
            save(Session("shop-1", "access", "refresh"))
        }
        val quotaRepository = QuotaRepository(
            database = database,
            sessionStore = sessionStore,
            ioDispatcher = dispatcher
        )
        // Limit 50, 2 extensions used (+20), total 70, used 70
        quotaRepository.setMockQuota(
            shopId = "shop-1",
            monthlyLimit = 50,
            used = 70,
            emergencyExtensionsUsed = 2,
            emergencyBillsAdded = 20,
            planTier = "FREE"
        )

        val interceptor = DealQuotaInterceptor(quotaRepository)
        var saveExecuted = false

        val state = interceptor.checkQuotaAndExecuteSave("shop-1") {
            saveExecuted = true
        }

        kotlin.test.assertFalse(saveExecuted)
        assertTrue(state is DealSaveState.QuotaExhaustedUpgradeRequired)
        val exhausted = state as DealSaveState.QuotaExhaustedUpgradeRequired
        assertEquals(70L, exhausted.quota.billsCreatedThisMonth)
        assertEquals(70L, exhausted.quota.effectiveLimit)
        assertEquals(2L, exhausted.quota.emergencyExtensionsUsed)
    }

    @Test
    fun testEditModeNeverConsumesQuotaEvenIfExhausted() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val database = createDatabase(DriverFactory())
        val sessionStore = InMemorySessionStore().apply {
            save(Session("shop-1", "access", "refresh"))
        }
        val quotaRepository = QuotaRepository(
            database = database,
            sessionStore = sessionStore,
            ioDispatcher = dispatcher
        )
        quotaRepository.setMockQuota(
            shopId = "shop-1",
            monthlyLimit = 50,
            used = 70,
            emergencyExtensionsUsed = 2,
            emergencyBillsAdded = 20,
            planTier = "FREE"
        )

        val interceptor = DealQuotaInterceptor(quotaRepository)
        var editExecuted = false

        val state = interceptor.checkQuotaAndExecuteSave("shop-1", isEditMode = true) {
            editExecuted = true
        }

        assertTrue(editExecuted)
        assertTrue(state is DealSaveState.Success)
        val quota = quotaRepository.getQuotaStatus("shop-1").first()
        assertEquals(70L, quota.billsCreatedThisMonth) // not incremented
    }
}
