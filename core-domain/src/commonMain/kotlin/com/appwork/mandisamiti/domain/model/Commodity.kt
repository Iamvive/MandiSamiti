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

object CommodityParser {
    val KNOWN_BASE_CROPS = listOf(
        "गेहूं", "चना", "सरसों", "धान", "सोयाबीन", "मक्का", "बाजरा", "ज्वार",
        "मटर", "उड़द", "मूंग", "मसूर", "आलू", "प्याज", "लहसुन", "तिल", "अलसी", "धनिया",
        "Wheat", "Gram", "Mustard", "Paddy", "Soybean", "Maize", "Barley", "Peas"
    )

    fun parseBaseCommodity(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "अन्य"
        for (crop in KNOWN_BASE_CROPS) {
            if (trimmed.equals(crop, ignoreCase = true)) {
                return trimmed
            }
            if (trimmed.startsWith("$crop ", ignoreCase = true) ||
                trimmed.startsWith("$crop-", ignoreCase = true) ||
                trimmed.startsWith("$crop/", ignoreCase = true) ||
                trimmed.startsWith("$crop(", ignoreCase = true)
            ) {
                return trimmed.take(crop.length)
            }
        }
        val firstWord = trimmed.split(Regex("[\\s\\-/\\(]")).firstOrNull()?.trim()
        return if (!firstWord.isNullOrBlank()) firstWord else trimmed
    }

    fun parseVarietyName(raw: String, base: String): String {
        val trimmed = raw.trim()
        if (trimmed.equals(base, ignoreCase = true)) {
            return "सामान्य"
        }
        if (trimmed.startsWith(base, ignoreCase = true)) {
            val remainder = trimmed.substring(base.length)
                .trim()
                .removePrefix("-")
                .removePrefix("/")
                .removePrefix("(")
                .removeSuffix(")")
                .trim()
            if (remainder.isNotBlank()) return remainder
        }
        return trimmed
    }
}
