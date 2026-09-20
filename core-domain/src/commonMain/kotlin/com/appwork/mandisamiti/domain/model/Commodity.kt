package com.appwork.mandisamiti.domain.model

import kotlinx.serialization.Serializable

enum class CommodityUnit {
    QUINTAL, // क्विंटल (100 kg)
    KG,      // किलोग्राम
    BAG      // कट्टा / बोरी
}

@Serializable
data class Commodity(
    val id: String,
    val shopId: String,
    val nameHi: String,
    val nameEn: String,
    val defaultUnit: CommodityUnit = CommodityUnit.QUINTAL,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val syncStatus: Int = 0
)
