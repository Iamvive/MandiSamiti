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
    val syncStatus: Int = 0,
    val gstin: String? = null,
    val mandiLicense: String? = null
)

class DuplicatePhoneException(val existing: Party) :
    IllegalArgumentException("Phone already belongs to ${existing.name}")

class KhataPartyPhoneException(val party: Party) :
    IllegalArgumentException("Phone belongs to Khata account ${party.name}")

data class PartyBalance(
    val party: Party,
    val slipBalancePaisa: Long, // Slips only: positive = लेना है (they owe us), negative = देना है (we owe them)
    val udhaarGivenPaisa: Long, // Udhaar principal still owed to us
    val udhaarTakenPaisa: Long, // Udhaar principal we still owe them
    val lastTransactionDate: Long?
) {
    val udhaarNetPaisa: Long get() = udhaarGivenPaisa - udhaarTakenPaisa
}
