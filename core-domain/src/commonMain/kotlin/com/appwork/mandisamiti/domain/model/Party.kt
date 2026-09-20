package com.appwork.mandisamiti.domain.model

import kotlinx.serialization.Serializable

enum class PartyType {
    FARMER, // किसान / विक्रेता
    BUYER   // व्यापारी / खरीदार
}

@Serializable
data class Party(
    val id: String,
    val shopId: String,
    val name: String,
    val phone: String? = null,
    val village: String? = null,
    val partyType: PartyType,
    val monthlyInterestRate: Double? = null,
    val photoUri: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val syncStatus: Int = 0
)

data class PartyBalance(
    val party: Party,
    val balancePaisa: Long, // Positive = लेना है (Receivable), Negative = देना है (Payable)
    val lastTransactionDate: Long?
)
