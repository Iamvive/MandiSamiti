package com.appwork.mandisamiti.database

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class PartyEntity(
  public val id: String,
  public val shop_id: String,
  public val name: String,
  public val phone: String?,
  public val village: String?,
  public val party_type: String,
  public val monthly_interest_rate: Double?,
  public val photo_uri: String?,
  public val created_at: Long,
  public val updated_at: Long,
  public val is_deleted: Long,
  public val sync_status: Long,
)
