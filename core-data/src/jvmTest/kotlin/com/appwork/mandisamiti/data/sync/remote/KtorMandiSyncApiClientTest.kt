package com.appwork.mandisamiti.data.sync.remote

import com.appwork.mandisamiti.data.auth.AuthError
import com.appwork.mandisamiti.data.auth.InMemorySessionStore
import com.appwork.mandisamiti.data.auth.Session
import com.appwork.mandisamiti.data.auth.mandiHttpClient
import com.appwork.mandisamiti.data.sync.model.PartySyncDto
import com.appwork.mandisamiti.data.sync.model.SyncPushRequestDto
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class KtorMandiSyncApiClientTest {
    private val base = "https://api.test"

    private fun MockRequestHandleScope.json(status: HttpStatusCode, body: String): HttpResponseData =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun client(
        token: String? = "test-token",
        captured: MutableList<HttpRequestData> = mutableListOf(),
        handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): KtorMandiSyncApiClient {
        val engine = MockEngine { req -> captured += req; handler(req) }
        val store = InMemorySessionStore().apply { if (token != null) save(Session("shop-1", token, "r")) }
        return KtorMandiSyncApiClient(mandiHttpClient(engine), base, store) { Result.failure(IllegalStateException("unexpected refresh")) }
    }

    @Test
    fun pushSync_sendsBearerAndReturnsSyncedIds() = runTest {
        val reqs = mutableListOf<HttpRequestData>()
        val api = client(captured = reqs) {
            json(
                HttpStatusCode.OK,
                """{"success":true,"synced_parties":["p1"],"synced_deals":[],"synced_transactions":[],"synced_revisions":[],"server_sync_time":1000,"server_seq":50}"""
            )
        }

        val res = api.pushSync(SyncPushRequestDto(parties = listOf(PartySyncDto(id = "p1", name = "Test", role = "FARMER"))))
        assertTrue(res.isSuccess)
        val dto = res.getOrThrow()
        assertEquals(listOf("p1"), dto.synced_parties)
        assertEquals(50L, dto.server_seq)

        val req = reqs.single()
        assertEquals("$base/api/v1/sync/push", req.url.toString())
        assertEquals(HttpMethod.Post, req.method)
        assertEquals("Bearer test-token", req.headers[HttpHeaders.Authorization])
    }

    @Test
    fun pullSync_passesAfterSeqAndLimitParameters() = runTest {
        val reqs = mutableListOf<HttpRequestData>()
        val api = client(captured = reqs) {
            json(
                HttpStatusCode.OK,
                """{"last_sync_timestamp":100,"after_seq":100,"next_seq":250,"has_more":false,"parties":[{"id":"p1","name":"सुरेश","role":"FARMER"}],"deals":[],"transactions":[],"revisions":[],"server_sync_time":250}"""
            )
        }

        val res = api.pullSync(afterSeq = 100L, limit = 500)
        assertTrue(res.isSuccess)
        val dto = res.getOrThrow()
        assertEquals(1, dto.parties.size)
        assertEquals("सुरेश", dto.parties[0].name)
        assertEquals(250L, dto.next_seq)
        assertEquals(false, dto.has_more)

        val req = reqs.single()
        assertEquals("$base/api/v1/sync/pull?after_seq=100&limit=500", req.url.toString())
        assertEquals(HttpMethod.Get, req.method)
        assertEquals("Bearer test-token", req.headers[HttpHeaders.Authorization])
    }

    @Test
    fun pullSync_unauthenticated_failsCleanly() = runTest {
        val api = client(token = null) { json(HttpStatusCode.OK, "{}") }
        val res = api.pullSync()
        assertTrue(res.isFailure)
        assertIs<SyncAuthExpired>(res.exceptionOrNull())
    }

    @Test
    fun on401RefreshesOnceAndRetriesWithTheNewToken() = runTest {
        val seen = mutableListOf<String?>()
        val engine = MockEngine { req ->
            seen += req.headers[HttpHeaders.Authorization]
            if (req.headers[HttpHeaders.Authorization] == "Bearer old") respond("", HttpStatusCode.Unauthorized)
            else respond("{\"next_seq\":0,\"server_sync_time\":1}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val store = InMemorySessionStore().apply { save(Session("shop-1", "old", "r-old")) }
        var refreshCalls = 0
        val client = KtorMandiSyncApiClient(mandiHttpClient(engine), "http://x", store) { refreshCalls++; Result.success("new" to "r-new") }

        assertTrue(client.pullSync(0, 500).isSuccess)
        assertEquals(listOf<String?>("Bearer old", "Bearer new"), seen)
        assertEquals(1, refreshCalls)
        assertEquals("r-new", store.current()!!.refreshToken)
    }

    @Test
    fun rejectedRefreshFailsWithSyncAuthExpiredAndKeepsTheSession() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
        val store = InMemorySessionStore().apply { save(Session("shop-1", "old", "r-old")) }
        val client = KtorMandiSyncApiClient(mandiHttpClient(engine), "http://x", store) { Result.failure(AuthError.SessionExpired) }
        assertIs<SyncAuthExpired>(client.pullSync(0, 500).exceptionOrNull())
        assertEquals("shop-1", store.current()!!.shopId)
    }

    @Test
    fun refreshNetworkErrorIsAPlainFailureNotNeedsLogin() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
        val store = InMemorySessionStore().apply { save(Session("shop-1", "old", "r-old")) }
        val client = KtorMandiSyncApiClient(mandiHttpClient(engine), "http://x", store) { Result.failure(RuntimeException("offline")) }
        val err = client.pullSync(0, 500).exceptionOrNull()
        assertTrue(err != null && err !is SyncAuthExpired)
    }
}
