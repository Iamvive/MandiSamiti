package com.appwork.mandisamiti.data.repository

import com.appwork.mandisamiti.data.auth.InMemorySessionStore
import com.appwork.mandisamiti.data.auth.Session
import com.appwork.mandisamiti.data.auth.mandiHttpClient
import com.appwork.mandisamiti.database.DriverFactory
import com.appwork.mandisamiti.database.createDatabase
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuotaRepositoryTest {

    @Test
    fun testDefaultQuotaWhenNoRowExists() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val database = createDatabase(DriverFactory())
        val sessionStore = InMemorySessionStore().apply {
            save(Session("shop-1", "access", "refresh"))
        }
        val repo = QuotaRepository(
            database = database,
            sessionStore = sessionStore,
            ioDispatcher = dispatcher
        )

        val quota = repo.getQuotaStatus("shop-1").first()
        assertEquals("shop-1", quota.shopId)
        assertEquals("FREE", quota.planTier)
        assertEquals(50L, quota.monthlyLimit)
        assertEquals(0L, quota.emergencyExtensionsUsed)
        assertEquals(0L, quota.emergencyBillsAdded)
        assertEquals(0L, quota.billsCreatedThisMonth)
        assertTrue(quota.canCreateBill)
        assertTrue(quota.canClaimEmergencyExtension)
    }

    @Test
    fun testLocalQuotaBlocksOnLimitExceeded() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val database = createDatabase(DriverFactory())
        val sessionStore = InMemorySessionStore().apply {
            save(Session("shop-1", "access", "refresh"))
        }
        val repo = QuotaRepository(
            database = database,
            sessionStore = sessionStore,
            ioDispatcher = dispatcher
        )

        repo.setMockQuota(
            shopId = "shop-1",
            monthlyLimit = 50,
            used = 50,
            emergencyExtensionsUsed = 2,
            emergencyBillsAdded = 20,
            planTier = "FREE"
        )

        val status = repo.getQuotaStatus("shop-1").first()
        assertEquals(50L, status.monthlyLimit)
        assertEquals(70L, status.effectiveLimit)
        assertEquals(50L, status.billsCreatedThisMonth)
        // 50 < 50 + 20 = 70, so canCreateBill is true until used reaches 70!
        assertTrue(status.canCreateBill)
        // emergency used is 2, so cannot claim anymore
        assertFalse(status.canClaimEmergencyExtension)

        // Now set used = 70 (limit reached)
        repo.setMockQuota(
            shopId = "shop-1",
            monthlyLimit = 50,
            used = 70,
            emergencyExtensionsUsed = 2,
            emergencyBillsAdded = 20,
            planTier = "FREE"
        )
        val statusBlocked = repo.getQuotaStatus("shop-1").first()
        assertFalse(statusBlocked.canCreateBill)
        assertFalse(statusBlocked.canClaimEmergencyExtension)
    }

    @Test
    fun testProPlanNeverBlocks() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val database = createDatabase(DriverFactory())
        val sessionStore = InMemorySessionStore().apply {
            save(Session("shop-1", "access", "refresh"))
        }
        val repo = QuotaRepository(
            database = database,
            sessionStore = sessionStore,
            ioDispatcher = dispatcher
        )

        repo.setMockQuota(
            shopId = "shop-1",
            monthlyLimit = 50,
            used = 1000,
            emergencyExtensionsUsed = 2,
            emergencyBillsAdded = 20,
            planTier = "PRO"
        )

        val status = repo.getQuotaStatus("shop-1").first()
        assertTrue(status.canCreateBill)
    }

    @Test
    fun testIncrementLocalBillCount() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val database = createDatabase(DriverFactory())
        val sessionStore = InMemorySessionStore().apply {
            save(Session("shop-1", "access", "refresh"))
        }
        val repo = QuotaRepository(
            database = database,
            sessionStore = sessionStore,
            ioDispatcher = dispatcher
        )

        repo.setMockQuota(shopId = "shop-1", monthlyLimit = 50, used = 49)
        val before = repo.getQuotaStatus("shop-1").first()
        assertEquals(49L, before.billsCreatedThisMonth)
        assertTrue(before.canCreateBill)

        repo.incrementLocalBillCount("shop-1")

        val after = repo.getQuotaStatus("shop-1").first()
        assertEquals(50L, after.billsCreatedThisMonth)
        assertFalse(after.canCreateBill)
    }

    @Test
    fun testClaimEmergencyExtensionSuccess() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val database = createDatabase(DriverFactory())
        val sessionStore = InMemorySessionStore().apply {
            save(Session("shop-1", "access", "refresh"))
        }

        val engine = MockEngine { req ->
            assertEquals("/api/v1/billing/claim-emergency-extension", req.url.encodedPath)
            assertEquals(HttpMethod.Post, req.method)
            assertEquals("Bearer access", req.headers[HttpHeaders.Authorization])
            respond(
                content = """{
                    "plan_tier": "FREE",
                    "bills_used_this_month": 50,
                    "monthly_limit": 50,
                    "emergency_extensions_used": 1,
                    "emergency_extensions_max": 2,
                    "emergency_bills_added": 10,
                    "effective_limit": 60,
                    "can_create_bill": true,
                    "is_emergency_extension_available": true
                }""".trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val repo = QuotaRepository(
            database = database,
            sessionStore = sessionStore,
            httpClient = mandiHttpClient(engine),
            baseUrl = "https://api.test",
            ioDispatcher = dispatcher
        )

        repo.setMockQuota(shopId = "shop-1", monthlyLimit = 50, used = 50, emergencyExtensionsUsed = 0)
        val before = repo.getQuotaStatus("shop-1").first()
        assertFalse(before.canCreateBill)
        assertTrue(before.canClaimEmergencyExtension)

        val result = repo.claimEmergencyExtension("shop-1")
        assertTrue(result.isSuccess)

        val after = repo.getQuotaStatus("shop-1").first()
        assertEquals(1L, after.emergencyExtensionsUsed)
        assertEquals(10L, after.emergencyBillsAdded)
        assertEquals(60L, after.effectiveLimit)
        assertTrue(after.canCreateBill)
    }

    @Test
    fun testSyncQuotaWithBackendUpdatesLocalTable() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val database = createDatabase(DriverFactory())
        val sessionStore = InMemorySessionStore().apply {
            save(Session("shop-1", "access", "refresh"))
        }

        val engine = MockEngine { req ->
            assertEquals("/api/v1/billing/quota", req.url.encodedPath)
            assertEquals(HttpMethod.Get, req.method)
            respond(
                content = """{
                    "plan_tier": "FREE",
                    "bills_used_this_month": 35,
                    "monthly_limit": 50,
                    "emergency_extensions_used": 0,
                    "emergency_extensions_max": 2,
                    "emergency_bills_added": 0,
                    "effective_limit": 50,
                    "can_create_bill": true,
                    "is_emergency_extension_available": true
                }""".trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val repo = QuotaRepository(
            database = database,
            sessionStore = sessionStore,
            httpClient = mandiHttpClient(engine),
            baseUrl = "https://api.test",
            ioDispatcher = dispatcher
        )

        val syncRes = repo.syncQuotaWithBackend("shop-1")
        assertTrue(syncRes.isSuccess)

        val status = repo.getQuotaStatus("shop-1").first()
        assertEquals(35L, status.billsCreatedThisMonth)
        assertEquals(50L, status.monthlyLimit)
        assertEquals("FREE", status.planTier)
        assertTrue(status.canCreateBill)
    }
}
