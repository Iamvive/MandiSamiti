package com.appwork.mandisamiti.database

import kotlin.Long
import kotlin.String

public data class GetPartyBalance(
  public val party_id: String,
  public val party_name: String,
  public val party_village: String?,
  public val party_type: String,
  public val balance_paisa: Long,
)
