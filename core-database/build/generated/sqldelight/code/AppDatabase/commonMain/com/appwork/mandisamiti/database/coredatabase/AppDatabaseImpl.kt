package com.appwork.mandisamiti.database.coredatabase

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.AppDatabaseQueries
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<AppDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = AppDatabaseImpl.Schema

internal fun KClass<AppDatabase>.newInstance(driver: SqlDriver): AppDatabase =
    AppDatabaseImpl(driver)

private class AppDatabaseImpl(
  driver: SqlDriver,
) : TransacterImpl(driver), AppDatabase {
  override val appDatabaseQueries: AppDatabaseQueries = AppDatabaseQueries(driver)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE shopProfileEntity (
          |    id TEXT PRIMARY KEY NOT NULL,
          |    shop_name TEXT NOT NULL,
          |    owner_name TEXT NOT NULL,
          |    mandi_name TEXT NOT NULL,
          |    shop_number TEXT,
          |    phone_number TEXT NOT NULL,
          |    pin_hash TEXT NOT NULL,
          |    default_monthly_interest_rate REAL NOT NULL DEFAULT 1.5,
          |    is_sound_enabled INTEGER NOT NULL DEFAULT 1,
          |    created_at INTEGER NOT NULL,
          |    updated_at INTEGER NOT NULL,
          |    sync_status INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE partyEntity (
          |    id TEXT PRIMARY KEY NOT NULL,
          |    shop_id TEXT NOT NULL,
          |    name TEXT NOT NULL,
          |    phone TEXT,
          |    village TEXT,
          |    party_type TEXT NOT NULL, -- 'FARMER' | 'BUYER'
          |    monthly_interest_rate REAL,
          |    photo_uri TEXT,
          |    created_at INTEGER NOT NULL,
          |    updated_at INTEGER NOT NULL,
          |    is_deleted INTEGER NOT NULL DEFAULT 0,
          |    sync_status INTEGER NOT NULL DEFAULT 0,
          |    FOREIGN KEY(shop_id) REFERENCES shopProfileEntity(id)
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE commodityEntity (
          |    id TEXT PRIMARY KEY NOT NULL,
          |    shop_id TEXT NOT NULL,
          |    name_hi TEXT NOT NULL,
          |    name_en TEXT NOT NULL,
          |    default_unit TEXT NOT NULL DEFAULT 'QUINTAL',
          |    is_active INTEGER NOT NULL DEFAULT 1,
          |    created_at INTEGER NOT NULL,
          |    updated_at INTEGER NOT NULL,
          |    is_deleted INTEGER NOT NULL DEFAULT 0,
          |    sync_status INTEGER NOT NULL DEFAULT 0,
          |    FOREIGN KEY(shop_id) REFERENCES shopProfileEntity(id)
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE dealEntity (
          |    id TEXT PRIMARY KEY NOT NULL,
          |    shop_id TEXT NOT NULL,
          |    farmer_id TEXT NOT NULL,
          |    buyer_id TEXT,
          |    commodity_id TEXT NOT NULL,
          |    deal_status TEXT NOT NULL, -- 'PENDING_SETTLEMENT' | 'SETTLED' | 'CANCELLED'
          |    deal_date INTEGER NOT NULL,
          |
          |    -- Weight details (Grams)
          |    bags_count INTEGER NOT NULL,
          |    gross_weight_grams INTEGER NOT NULL,
          |    cut_weight_grams INTEGER NOT NULL DEFAULT 0,
          |    net_weight_grams INTEGER NOT NULL,
          |
          |    -- Pricing details (Paisa)
          |    rate_paisa_per_unit INTEGER,
          |    gross_amount_paisa INTEGER NOT NULL DEFAULT 0,
          |
          |    -- Manual Deductions (Paisa)
          |    farmer_commission_paisa INTEGER NOT NULL DEFAULT 0,
          |    buyer_commission_paisa INTEGER NOT NULL DEFAULT 0,
          |    labour_charge_paisa INTEGER NOT NULL DEFAULT 0,
          |    weighing_charge_paisa INTEGER NOT NULL DEFAULT 0,
          |    other_deductions_paisa INTEGER NOT NULL DEFAULT 0,
          |
          |    -- Final Settlements (Paisa)
          |    net_farmer_payable_paisa INTEGER NOT NULL DEFAULT 0,
          |    net_buyer_receivable_paisa INTEGER NOT NULL DEFAULT 0,
          |
          |    receipt_photo_uri TEXT,
          |    voice_note_uri TEXT,
          |    remarks TEXT,
          |
          |    created_at INTEGER NOT NULL,
          |    updated_at INTEGER NOT NULL,
          |    is_deleted INTEGER NOT NULL DEFAULT 0,
          |    sync_status INTEGER NOT NULL DEFAULT 0,
          |    FOREIGN KEY(farmer_id) REFERENCES partyEntity(id),
          |    FOREIGN KEY(buyer_id) REFERENCES partyEntity(id),
          |    FOREIGN KEY(commodity_id) REFERENCES commodityEntity(id)
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE cashTransactionEntity (
          |    id TEXT PRIMARY KEY NOT NULL,
          |    shop_id TEXT NOT NULL,
          |    party_id TEXT NOT NULL,
          |    deal_id TEXT,
          |    transaction_type TEXT NOT NULL, -- 'UDHAR_GIVEN', 'JAMA_RECEIVED', 'INTEREST_ADDED', 'DISCOUNT_GIVEN'
          |    amount_paisa INTEGER NOT NULL,
          |    payment_mode TEXT NOT NULL DEFAULT 'CASH',
          |    transaction_date INTEGER NOT NULL,
          |    voice_note_uri TEXT,
          |    remarks TEXT,
          |    created_at INTEGER NOT NULL,
          |    updated_at INTEGER NOT NULL,
          |    is_deleted INTEGER NOT NULL DEFAULT 0,
          |    sync_status INTEGER NOT NULL DEFAULT 0,
          |    FOREIGN KEY(party_id) REFERENCES partyEntity(id),
          |    FOREIGN KEY(deal_id) REFERENCES dealEntity(id)
          |)
          """.trimMargin(), 0)
      driver.execute(null, "CREATE INDEX idx_party_shop ON partyEntity(shop_id, is_deleted)", 0)
      driver.execute(null, "CREATE INDEX idx_party_name ON partyEntity(name)", 0)
      driver.execute(null, "CREATE INDEX idx_deal_farmer ON dealEntity(farmer_id, is_deleted)", 0)
      driver.execute(null, "CREATE INDEX idx_deal_buyer ON dealEntity(buyer_id, is_deleted)", 0)
      driver.execute(null, "CREATE INDEX idx_deal_date ON dealEntity(deal_date)", 0)
      driver.execute(null,
          "CREATE INDEX idx_tx_party ON cashTransactionEntity(party_id, is_deleted)", 0)
      driver.execute(null, "CREATE INDEX idx_tx_date ON cashTransactionEntity(transaction_date)", 0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
