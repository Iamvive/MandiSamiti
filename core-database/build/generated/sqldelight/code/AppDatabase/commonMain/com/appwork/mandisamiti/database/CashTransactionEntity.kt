package com.appwork.mandisamiti.database

import kotlin.Long
import kotlin.String

public data class CashTransactionEntity(
  public val id: String,
  public val shop_id: String,
  public val party_id: String,
  public val deal_id: String?,
  public val transaction_type: String,
  public val amount_paisa: Long,
  public val payment_mode: String,
  public val transaction_date: Long,
  public val voice_note_uri: String?,
  public val remarks: String?,
  public val created_at: Long,
  public val updated_at: Long,
  public val is_deleted: Long,
  public val sync_status: Long,
)
