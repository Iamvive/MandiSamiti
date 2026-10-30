package com.appwork.mandisamiti.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class VoidReason(
    val labelHi: String,
    val labelEn: String,
    val appliesToDeal: Boolean = true,
    val appliesToCash: Boolean = true
) {
    WRONG_ENTRY("गलत प्रविष्टि", "Wrong entry", appliesToDeal = true, appliesToCash = true),
    WEIGHING_ERROR("तौल त्रुटि", "Weighing error", appliesToDeal = true, appliesToCash = false),
    DEAL_CANCELLED("सौदा निरस्त", "Deal cancelled", appliesToDeal = true, appliesToCash = false),
    CASH_RETURNED("रकम वापसी / निरस्त", "Cash returned / cancelled", appliesToDeal = false, appliesToCash = true),
    DUPLICATE_ENTRY("दोहरी प्रविष्टि", "Duplicate entry", appliesToDeal = true, appliesToCash = true)
}
