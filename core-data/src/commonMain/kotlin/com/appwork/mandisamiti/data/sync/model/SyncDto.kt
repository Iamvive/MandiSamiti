package com.appwork.mandisamiti.data.sync.model

import kotlinx.serialization.SerialName
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
data class BuyEntrySyncDto(
    val id: String,
    val farmer_id: String,
    val commodity_id: String,
    val entry_date: Long = 0L,
    val bags_count: Long = 0L,
    val weight_per_bag_grams: Long = 0L,
    val extra_weight_grams: Long = 0L,
    val total_weight_grams: Long = 0L,
    val rate_paisa_per_quintal: Long = 0L,
    val commission_bps: Long = 0L,
    val labour_per_bag_paisa: Long = 0L,
    val advance_paid_paisa: Long = 0L,
    val gross_paisa: Long = 0L,
    val commission_paisa: Long = 0L,
    val labour_paisa: Long = 0L,
    val net_payable_paisa: Long = 0L,
    val paid_paisa: Long = 0L,
    val vehicle_number: String? = null,
    val slip_number: String? = null,
    val slip_no: Long? = null,
    val remarks: String? = null,
    val is_void: Int = 0,
    val void_reason: String? = null,
    val revision: Int = 1,
    val is_deleted: Int = 0,
    val created_at: Long = 0L,
    val updated_at: Long = 0L
)

@Serializable
data class SellEntrySyncDto(
    val id: String,
    val buyer_id: String,
    val commodity_id: String,
    val entry_date: Long = 0L,
    val bill_mode: String = "NORMAL",
    val party_kind: String = "MERCHANT",
    val gstin: String? = null,
    val bags_count: Long = 0L,
    val weight_per_bag_grams: Long = 0L,
    val total_weight_grams: Long = 0L,
    val rate_paisa_per_quintal: Long = 0L,
    val arat_bps: Long = 0L,
    val tulak_paisa: Long = 0L,
    val gross_paisa: Long = 0L,
    val arat_paisa: Long = 0L,
    val mandi_shulk_paisa: Long = 0L,
    val total_receivable_paisa: Long = 0L,
    val received_paisa: Long = 0L,
    val balance_due_paisa: Long = 0L,
    val vehicle_number: String? = null,
    val invoice_number: String? = null,
    val slip_no: Long? = null,
    val remarks: String? = null,
    val is_void: Int = 0,
    val void_reason: String? = null,
    val revision: Int = 1,
    val is_deleted: Int = 0,
    val created_at: Long = 0L,
    val updated_at: Long = 0L
)

@Serializable
data class LendEntrySyncDto(
    val id: String,
    val party_id: String,
    val lend_type: String = "MONEY",
    val entry_date: Long = 0L,
    val amount_paisa: Long = 0L,
    val commodity_id: String? = null,
    val bags_count: Long = 0L,
    val weight_grams: Long = 0L,
    val expected_return_date: Long? = null,
    val slip_no: Long? = null,
    val notes: String? = null,
    val status: String = "OPEN",
    val is_void: Int = 0,
    val void_reason: String? = null,
    val revision: Int = 1,
    val is_deleted: Int = 0,
    val created_at: Long = 0L,
    val updated_at: Long = 0L
)

@Serializable
data class LendSettlementSyncDto(
    val id: String,
    val lend_id: String,
    val settled_at: Long = 0L,
    val amount_paisa: Long = 0L,
    val weight_grams: Long = 0L,
    val bags_count: Long = 0L,
    val is_void: Int = 0,
    val void_reason: String? = null,
    val revision: Int = 1,
    val created_at: Long = 0L,
    val updated_at: Long = 0L,
    @SerialName("principal_paisa")
    val principal_paisa: Long = 0L,
    @SerialName("interest_paisa")
    val interest_paisa: Long = 0L,
    @SerialName("calculated_interest_paisa")
    val calculated_interest_paisa: Long = 0L
)

@Serializable
data class ExpenseEntrySyncDto(
    val id: String,
    val entry_date: Long = 0L,
    val category: String = "OTHER",
    val amount_paisa: Long = 0L,
    val paid_to: String? = null,
    val payment_mode: String = "CASH",
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
    val buy_entries: List<BuyEntrySyncDto> = emptyList(),
    val sell_entries: List<SellEntrySyncDto> = emptyList(),
    val lend_entries: List<LendEntrySyncDto> = emptyList(),
    val lend_settlements: List<LendSettlementSyncDto> = emptyList(),
    val expenses: List<ExpenseEntrySyncDto> = emptyList(),
    val revisions: List<EntryRevisionSyncDto> = emptyList()
)

@Serializable
data class SyncPushResponseDto(
    val success: Boolean = true,
    val synced_parties: List<String> = emptyList(),
    val synced_deals: List<String> = emptyList(),
    val synced_transactions: List<String> = emptyList(),
    val synced_buy_entries: List<String> = emptyList(),
    val synced_sell_entries: List<String> = emptyList(),
    val synced_lend_entries: List<String> = emptyList(),
    val synced_lend_settlements: List<String> = emptyList(),
    val synced_expenses: List<String> = emptyList(),
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
    val buy_entries: List<BuyEntrySyncDto> = emptyList(),
    val sell_entries: List<SellEntrySyncDto> = emptyList(),
    val lend_entries: List<LendEntrySyncDto> = emptyList(),
    val lend_settlements: List<LendSettlementSyncDto> = emptyList(),
    val expenses: List<ExpenseEntrySyncDto> = emptyList(),
    val revisions: List<EntryRevisionSyncDto> = emptyList(),
    val server_sync_time: Long = 0L
)
