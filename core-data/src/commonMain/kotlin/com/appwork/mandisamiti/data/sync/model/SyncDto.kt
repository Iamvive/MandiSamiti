package com.appwork.mandisamiti.data.sync.model

import kotlinx.serialization.Serializable

@Serializable
data class PartySyncDto(
    val id: String,
    val name: String,
    val phone: String? = null,
    val role: String,
    val village: String? = null,
    val monthly_interest_rate: Double? = 1.5,
    val photo_uri: String? = null,
    val is_deleted: Int = 0,
    val created_at: Long = 0L,
    val updated_at: Long = 0L
)

@Serializable
data class DealSyncDto(
    val id: String,
    val farmer_id: String,
    val buyer_id: String? = null,
    val commodity: String,
    val deal_status: String = "SETTLED",
    val deal_date: Long? = null,
    val bags_count: Int = 0,
    val gross_weight_grams: Long = 0L,
    val cut_weight_grams: Long = 0L,
    val net_weight_grams: Long = 0L,
    val rate_paisa_per_unit: Long = 0L,
    val gross_amount_paisa: Long = 0L,
    val farmer_commission_bps: Int = 0,
    val farmer_commission_paisa: Long = 0L,
    val buyer_commission_paisa: Long = 0L,
    val labour_charge_paisa: Long = 0L,
    val weighing_charge_paisa: Long = 0L,
    val other_deductions_paisa: Long = 0L,
    val net_farmer_payable_paisa: Long = 0L,
    val net_buyer_receivable_paisa: Long = 0L,
    val receipt_photo_uri: String? = null,
    val voice_note_uri: String? = null,
    val remarks: String? = null,
    val is_void: Int = 0,
    val void_reason: String? = null,
    val revision: Int = 1,
    val is_deleted: Int = 0,
    val created_at: Long = 0L,
    val updated_at: Long = 0L
)

@Serializable
data class CashTransactionSyncDto(
    val id: String,
    val party_id: String? = null,
    val deal_id: String? = null,
    val transaction_type: String,
    val amount_paisa: Long,
    val payment_mode: String = "CASH",
    val category: String = "TRADE_PAYMENT",
    val transaction_date: Long? = null,
    val voice_note_uri: String? = null,
    val remarks: String? = null,
    val is_void: Int = 0,
    val void_reason: String? = null,
    val revision: Int = 1,
    val is_deleted: Int = 0,
    val created_at: Long = 0L,
    val updated_at: Long = 0L
)

@Serializable
data class EntryRevisionSyncDto(
    val id: String,
    val entry_id: String,
    val entry_kind: String,
    val revision: Int,
    val change_kind: String,
    val snapshot_json: String,
    val void_reason: String? = null,
    val changed_at: Long? = null
)

@Serializable
data class SyncPushRequestDto(
    val parties: List<PartySyncDto> = emptyList(),
    val deals: List<DealSyncDto> = emptyList(),
    val transactions: List<CashTransactionSyncDto> = emptyList(),
    val revisions: List<EntryRevisionSyncDto> = emptyList()
)

@Serializable
data class SyncPushResponseDto(
    val success: Boolean = true,
    val synced_parties: List<String> = emptyList(),
    val synced_deals: List<String> = emptyList(),
    val synced_transactions: List<String> = emptyList(),
    val synced_revisions: List<String> = emptyList(),
    val server_sync_time: Long = 0L,
    val server_seq: Long = 0L,
    val conflicts: List<String> = emptyList()
)

@Serializable
data class SyncPullResponseDto(
    val after_seq: Long = 0L,
    val next_seq: Long = 0L,
    val has_more: Boolean = false,
    val parties: List<PartySyncDto> = emptyList(),
    val deals: List<DealSyncDto> = emptyList(),
    val transactions: List<CashTransactionSyncDto> = emptyList(),
    val revisions: List<EntryRevisionSyncDto> = emptyList(),
    val server_sync_time: Long = 0L
)
