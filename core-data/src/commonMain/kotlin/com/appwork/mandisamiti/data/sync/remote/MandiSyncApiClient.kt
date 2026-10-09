package com.appwork.mandisamiti.data.sync.remote

import com.appwork.mandisamiti.data.sync.model.SyncPushRequestDto
import com.appwork.mandisamiti.data.sync.model.SyncPushResponseDto
import com.appwork.mandisamiti.data.sync.model.SyncPullResponseDto

interface MandiSyncApiClient {
    suspend fun pushSync(request: SyncPushRequestDto): Result<SyncPushResponseDto>
    suspend fun pullSync(afterSeq: Long = 0L, limit: Int = 500): Result<SyncPullResponseDto>
}
