package com.appwork.mandisamiti.data.sync.remote

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
        return KtorMandiSyncApiClient(mandiHttpClient(engine), base) { token }
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
        assertEquals("User not authenticated", res.exceptionOrNull()?.message)
    }
}
