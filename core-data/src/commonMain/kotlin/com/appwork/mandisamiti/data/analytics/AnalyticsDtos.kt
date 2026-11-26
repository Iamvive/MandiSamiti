package com.appwork.mandisamiti.data.analytics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnalyticsEventDto(
    val id: String,
    val name: String,
    val screen: String?,
    @SerialName("session_id") val sessionId: String,
    @SerialName("occurred_at_ms") val occurredAtMs: Long,
    @SerialName("duration_ms") val durationMs: Long?,
    @SerialName("app_version") val appVersion: String,
)

@Serializable
data class AnalyticsEventsPushRequestDto(
    val events: List<AnalyticsEventDto>,
)

@Serializable
data class AnalyticsEventsPushResponseDto(
    val accepted: Int = 0,
)

class EventsPushHttpException(
    val statusCode: Int,
    body: String,
) : RuntimeException("Events push failed (HTTP $statusCode): $body")
