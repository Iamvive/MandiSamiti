package com.appwork.mandisamiti.data.sync.remote

import com.appwork.mandisamiti.data.sync.model.SyncPushRequestDto
import com.appwork.mandisamiti.data.sync.model.SyncPushResponseDto
import com.appwork.mandisamiti.data.sync.model.SyncPullResponseDto
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

class KtorMandiSyncApiClient(
    private val http: HttpClient,
    private val baseUrl: String,
    private val tokenProvider: suspend () -> String?
) : MandiSyncApiClient {

    override suspend fun pushSync(request: SyncPushRequestDto): Result<SyncPushResponseDto> =
        safeCall {
            val token = tokenProvider() ?: throw IllegalStateException("User not authenticated")
            val url = "$baseUrl/api/v1/sync/push"
            val response: HttpResponse = http.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
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
            val token = tokenProvider() ?: throw IllegalStateException("User not authenticated")
            val url = "$baseUrl/api/v1/sync/pull"
            val response: HttpResponse = http.get(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("after_seq", afterSeq)
                parameter("limit", limit)
            }
            if (response.status == HttpStatusCode.OK) {
                response.body<SyncPullResponseDto>()
            } else {
                val errorBody = response.bodyAsText()
                throw RuntimeException("Sync pull failed (HTTP ${response.status.value}): $errorBody")
            }
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
