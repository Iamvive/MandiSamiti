package com.appwork.mandisamiti.database

import kotlin.Long
import kotlin.String

public data class CommodityEntity(
  public val id: String,
  public val shop_id: String,
  public val name_hi: String,
  public val name_en: String,
  public val default_unit: String,
  public val is_active: Long,
  public val created_at: Long,
  public val updated_at: Long,
  public val is_deleted: Long,
  public val sync_status: Long,
)
