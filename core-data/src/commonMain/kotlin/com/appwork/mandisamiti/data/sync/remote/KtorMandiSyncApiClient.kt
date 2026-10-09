package com.appwork.mandisamiti.data.sync.remote

import com.appwork.mandisamiti.data.sync.model.SyncPushRequestDto
import com.appwork.mandisamiti.data.sync.model.SyncPushResponseDto
import com.appwork.mandisamiti.data.sync.model.SyncPullResponseDto
import com.appwork.mandisamiti.data.auth.AuthError
import com.appwork.mandisamiti.data.auth.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SyncAuthExpired : Exception("refresh token rejected")

// One refresh at a time per process: refresh tokens rotate, so a parallel refresh with the old token would fail.
private val refreshLock = Mutex()

class KtorMandiSyncApiClient(
    private val http: HttpClient,
    private val baseUrl: String,
    private val sessionStore: SessionStore,
    private val refresh: suspend (refreshToken: String) -> Result<Pair<String, String>>
) : MandiSyncApiClient {

    override suspend fun pushSync(request: SyncPushRequestDto): Result<SyncPushResponseDto> =
        safeCall {
            val url = "$baseUrl/api/v1/sync/push"
            val response: HttpResponse = send { token ->
                http.post(url) {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(request)
                }
            }
            if (response.status == HttpStatusCode.OK) {
                response.body<SyncPushResponseDto>()
            } else {
                val errorBody = response.bodyAsText()
                throw RuntimeException("Sync push failed (HTTP ${response.status.value}): $errorBody")
            }
        }

    override suspend fun pullSync(afterSeq: Long, limit: Int): Result<SyncPullResponseDto> =
        safeCall {
            val url = "$baseUrl/api/v1/sync/pull"
            val response: HttpResponse = send { token ->
                http.get(url) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    parameter("after_seq", afterSeq)
                    parameter("limit", limit)
                }
            }
            if (response.status == HttpStatusCode.OK) {
                response.body<SyncPullResponseDto>()
            } else {
                val errorBody = response.bodyAsText()
                throw RuntimeException("Sync pull failed (HTTP ${response.status.value}): $errorBody")
            }
        }

    private suspend fun send(request: suspend (token: String) -> HttpResponse): HttpResponse {
        val session = sessionStore.current() ?: throw SyncAuthExpired()
        val first = request(session.accessToken)
        if (first.status != HttpStatusCode.Unauthorized) return first
        val token = refreshLock.withLock {
            val now = sessionStore.current() ?: throw SyncAuthExpired()
            if (now.accessToken != session.accessToken) {
                now.accessToken // someone else already refreshed
            } else {
                val (access, refreshToken) = refresh(now.refreshToken).getOrElse { e ->
                    throw if (e is AuthError.SessionExpired) SyncAuthExpired() else e
                }
                sessionStore.updateTokens(access, refreshToken)
                access
            }
        }
        val retried = request(token)
        if (retried.status == HttpStatusCode.Unauthorized) throw SyncAuthExpired()
        return retried
    }

    private inline fun <T> safeCall(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
