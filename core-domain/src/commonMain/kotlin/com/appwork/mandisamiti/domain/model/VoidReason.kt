package com.appwork.mandisamiti.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class VoidReason(val labelHi: String) {
    WEIGHING_ERROR("तौल त्रुटि"),
    WRONG_ENTRY("गलत प्रविष्टि"),
    DEAL_CANCELLED("सौदा निरस्त")
}
