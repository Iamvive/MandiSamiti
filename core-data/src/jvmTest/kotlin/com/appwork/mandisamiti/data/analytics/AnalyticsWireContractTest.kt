package com.appwork.mandisamiti.data.analytics

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/** The same sample file is validated by backend/tests/test_events_contract.py. */
class AnalyticsWireContractTest {
    @Test
    fun clientJsonMatchesTheSharedSample() {
        val sample = javaClass.getResource("/telemetry-events.sample.json")!!.readText()
        val request = AnalyticsEventsPushRequestDto(
            listOf(
                AnalyticsEventDto(
                    "sample-event-1", AnalyticsEventName.SCREEN_VIEW.wire, AnalyticsScreen.BUY_ENTRY.wire,
                    "sample-session", 1_795_000_000_000L, null, "2.0.0",
                ),
                AnalyticsEventDto(
                    "sample-event-2", AnalyticsEventName.SYNC_OK.wire, null,
                    "sample-session", 1_795_000_001_000L, 1_200L, "2.0.0",
                ),
            ),
        )
        assertEquals(
            Json.parseToJsonElement(sample),
            Json.encodeToJsonElement(AnalyticsEventsPushRequestDto.serializer(), request),
        )
    }
}
