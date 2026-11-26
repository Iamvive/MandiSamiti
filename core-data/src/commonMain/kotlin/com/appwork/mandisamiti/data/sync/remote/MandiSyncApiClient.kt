package com.appwork.mandisamiti.data.sync.remote

import com.appwork.mandisamiti.data.analytics.AnalyticsEventsPushRequestDto
import com.appwork.mandisamiti.data.analytics.AnalyticsEventsPushResponseDto
import com.appwork.mandisamiti.data.sync.model.SyncPushRequestDto
import com.appwork.mandisamiti.data.sync.model.SyncPushResponseDto
import com.appwork.mandisamiti.data.sync.model.SyncPullResponseDto

interface MandiSyncApiClient {
    suspend fun pushSync(request: SyncPushRequestDto): Result<SyncPushResponseDto>
    suspend fun pullSync(afterSeq: Long = 0L, limit: Int = 500): Result<SyncPullResponseDto>
    suspend fun pushEvents(request: AnalyticsEventsPushRequestDto): Result<AnalyticsEventsPushResponseDto> =
        Result.failure(UnsupportedOperationException("pushEvents not implemented"))
}
