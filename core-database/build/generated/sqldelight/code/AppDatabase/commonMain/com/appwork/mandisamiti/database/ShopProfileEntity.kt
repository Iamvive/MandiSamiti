package com.appwork.mandisamiti.database

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class ShopProfileEntity(
  public val id: String,
  public val shop_name: String,
  public val owner_name: String,
  public val mandi_name: String,
  public val shop_number: String?,
  public val phone_number: String,
  public val pin_hash: String,
  public val default_monthly_interest_rate: Double,
  public val is_sound_enabled: Long,
  public val created_at: Long,
  public val updated_at: Long,
  public val sync_status: Long,
)
