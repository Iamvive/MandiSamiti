package com.appwork.mandisamiti.database

import kotlin.Long
import kotlin.String

public data class DealEntity(
  public val id: String,
  public val shop_id: String,
  public val farmer_id: String,
  public val buyer_id: String?,
  public val commodity_id: String,
  public val deal_status: String,
  public val deal_date: Long,
  public val bags_count: Long,
  public val gross_weight_grams: Long,
  public val cut_weight_grams: Long,
  public val net_weight_grams: Long,
  public val rate_paisa_per_unit: Long?,
  public val gross_amount_paisa: Long,
  public val farmer_commission_paisa: Long,
  public val buyer_commission_paisa: Long,
  public val labour_charge_paisa: Long,
  public val weighing_charge_paisa: Long,
  public val other_deductions_paisa: Long,
  public val net_farmer_payable_paisa: Long,
  public val net_buyer_receivable_paisa: Long,
  public val receipt_photo_uri: String?,
  public val voice_note_uri: String?,
  public val remarks: String?,
  public val created_at: Long,
  public val updated_at: Long,
  public val is_deleted: Long,
  public val sync_status: Long,
)
