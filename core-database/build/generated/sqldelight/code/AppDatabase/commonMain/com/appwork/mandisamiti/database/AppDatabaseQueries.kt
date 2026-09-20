package com.appwork.mandisamiti.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Double
import kotlin.Long
import kotlin.String

public class AppDatabaseQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> getShopProfile(mapper: (
    id: String,
    shop_name: String,
    owner_name: String,
    mandi_name: String,
    shop_number: String?,
    phone_number: String,
    pin_hash: String,
    default_monthly_interest_rate: Double,
    is_sound_enabled: Long,
    created_at: Long,
    updated_at: Long,
    sync_status: Long,
  ) -> T): Query<T> = Query(1_844_467_553, arrayOf("shopProfileEntity"), driver, "AppDatabase.sq",
      "getShopProfile",
      "SELECT shopProfileEntity.id, shopProfileEntity.shop_name, shopProfileEntity.owner_name, shopProfileEntity.mandi_name, shopProfileEntity.shop_number, shopProfileEntity.phone_number, shopProfileEntity.pin_hash, shopProfileEntity.default_monthly_interest_rate, shopProfileEntity.is_sound_enabled, shopProfileEntity.created_at, shopProfileEntity.updated_at, shopProfileEntity.sync_status FROM shopProfileEntity LIMIT 1") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getDouble(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!
    )
  }

  public fun getShopProfile(): Query<ShopProfileEntity> = getShopProfile { id, shop_name,
      owner_name, mandi_name, shop_number, phone_number, pin_hash, default_monthly_interest_rate,
      is_sound_enabled, created_at, updated_at, sync_status ->
    ShopProfileEntity(
      id,
      shop_name,
      owner_name,
      mandi_name,
      shop_number,
      phone_number,
      pin_hash,
      default_monthly_interest_rate,
      is_sound_enabled,
      created_at,
      updated_at,
      sync_status
    )
  }

  public fun <T : Any> getAllParties(shop_id: String, mapper: (
    id: String,
    shop_id: String,
    name: String,
    phone: String?,
    village: String?,
    party_type: String,
    monthly_interest_rate: Double?,
    photo_uri: String?,
    created_at: Long,
    updated_at: Long,
    is_deleted: Long,
    sync_status: Long,
  ) -> T): Query<T> = GetAllPartiesQuery(shop_id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getString(4),
      cursor.getString(5)!!,
      cursor.getDouble(6),
      cursor.getString(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!
    )
  }

  public fun getAllParties(shop_id: String): Query<PartyEntity> = getAllParties(shop_id) { id,
      shop_id_, name, phone, village, party_type, monthly_interest_rate, photo_uri, created_at,
      updated_at, is_deleted, sync_status ->
    PartyEntity(
      id,
      shop_id_,
      name,
      phone,
      village,
      party_type,
      monthly_interest_rate,
      photo_uri,
      created_at,
      updated_at,
      is_deleted,
      sync_status
    )
  }

  public fun <T : Any> getPartyById(id: String, mapper: (
    id: String,
    shop_id: String,
    name: String,
    phone: String?,
    village: String?,
    party_type: String,
    monthly_interest_rate: Double?,
    photo_uri: String?,
    created_at: Long,
    updated_at: Long,
    is_deleted: Long,
    sync_status: Long,
  ) -> T): Query<T> = GetPartyByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getString(4),
      cursor.getString(5)!!,
      cursor.getDouble(6),
      cursor.getString(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!
    )
  }

  public fun getPartyById(id: String): Query<PartyEntity> = getPartyById(id) { id_, shop_id, name,
      phone, village, party_type, monthly_interest_rate, photo_uri, created_at, updated_at,
      is_deleted, sync_status ->
    PartyEntity(
      id_,
      shop_id,
      name,
      phone,
      village,
      party_type,
      monthly_interest_rate,
      photo_uri,
      created_at,
      updated_at,
      is_deleted,
      sync_status
    )
  }

  public fun <T : Any> getAllCommodities(shop_id: String, mapper: (
    id: String,
    shop_id: String,
    name_hi: String,
    name_en: String,
    default_unit: String,
    is_active: Long,
    created_at: Long,
    updated_at: Long,
    is_deleted: Long,
    sync_status: Long,
  ) -> T): Query<T> = GetAllCommoditiesQuery(shop_id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!
    )
  }

  public fun getAllCommodities(shop_id: String): Query<CommodityEntity> =
      getAllCommodities(shop_id) { id, shop_id_, name_hi, name_en, default_unit, is_active,
      created_at, updated_at, is_deleted, sync_status ->
    CommodityEntity(
      id,
      shop_id_,
      name_hi,
      name_en,
      default_unit,
      is_active,
      created_at,
      updated_at,
      is_deleted,
      sync_status
    )
  }

  public fun <T : Any> getDealsByShop(shop_id: String, mapper: (
    id: String,
    shop_id: String,
    farmer_id: String,
    buyer_id: String?,
    commodity_id: String,
    deal_status: String,
    deal_date: Long,
    bags_count: Long,
    gross_weight_grams: Long,
    cut_weight_grams: Long,
    net_weight_grams: Long,
    rate_paisa_per_unit: Long?,
    gross_amount_paisa: Long,
    farmer_commission_paisa: Long,
    buyer_commission_paisa: Long,
    labour_charge_paisa: Long,
    weighing_charge_paisa: Long,
    other_deductions_paisa: Long,
    net_farmer_payable_paisa: Long,
    net_buyer_receivable_paisa: Long,
    receipt_photo_uri: String?,
    voice_note_uri: String?,
    remarks: String?,
    created_at: Long,
    updated_at: Long,
    is_deleted: Long,
    sync_status: Long,
  ) -> T): Query<T> = GetDealsByShopQuery(shop_id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11),
      cursor.getLong(12)!!,
      cursor.getLong(13)!!,
      cursor.getLong(14)!!,
      cursor.getLong(15)!!,
      cursor.getLong(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getString(20),
      cursor.getString(21),
      cursor.getString(22),
      cursor.getLong(23)!!,
      cursor.getLong(24)!!,
      cursor.getLong(25)!!,
      cursor.getLong(26)!!
    )
  }

  public fun getDealsByShop(shop_id: String): Query<DealEntity> = getDealsByShop(shop_id) { id,
      shop_id_, farmer_id, buyer_id, commodity_id, deal_status, deal_date, bags_count,
      gross_weight_grams, cut_weight_grams, net_weight_grams, rate_paisa_per_unit,
      gross_amount_paisa, farmer_commission_paisa, buyer_commission_paisa, labour_charge_paisa,
      weighing_charge_paisa, other_deductions_paisa, net_farmer_payable_paisa,
      net_buyer_receivable_paisa, receipt_photo_uri, voice_note_uri, remarks, created_at,
      updated_at, is_deleted, sync_status ->
    DealEntity(
      id,
      shop_id_,
      farmer_id,
      buyer_id,
      commodity_id,
      deal_status,
      deal_date,
      bags_count,
      gross_weight_grams,
      cut_weight_grams,
      net_weight_grams,
      rate_paisa_per_unit,
      gross_amount_paisa,
      farmer_commission_paisa,
      buyer_commission_paisa,
      labour_charge_paisa,
      weighing_charge_paisa,
      other_deductions_paisa,
      net_farmer_payable_paisa,
      net_buyer_receivable_paisa,
      receipt_photo_uri,
      voice_note_uri,
      remarks,
      created_at,
      updated_at,
      is_deleted,
      sync_status
    )
  }

  public fun <T : Any> getDealsByFarmer(farmer_id: String, mapper: (
    id: String,
    shop_id: String,
    farmer_id: String,
    buyer_id: String?,
    commodity_id: String,
    deal_status: String,
    deal_date: Long,
    bags_count: Long,
    gross_weight_grams: Long,
    cut_weight_grams: Long,
    net_weight_grams: Long,
    rate_paisa_per_unit: Long?,
    gross_amount_paisa: Long,
    farmer_commission_paisa: Long,
    buyer_commission_paisa: Long,
    labour_charge_paisa: Long,
    weighing_charge_paisa: Long,
    other_deductions_paisa: Long,
    net_farmer_payable_paisa: Long,
    net_buyer_receivable_paisa: Long,
    receipt_photo_uri: String?,
    voice_note_uri: String?,
    remarks: String?,
    created_at: Long,
    updated_at: Long,
    is_deleted: Long,
    sync_status: Long,
  ) -> T): Query<T> = GetDealsByFarmerQuery(farmer_id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11),
      cursor.getLong(12)!!,
      cursor.getLong(13)!!,
      cursor.getLong(14)!!,
      cursor.getLong(15)!!,
      cursor.getLong(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getString(20),
      cursor.getString(21),
      cursor.getString(22),
      cursor.getLong(23)!!,
      cursor.getLong(24)!!,
      cursor.getLong(25)!!,
      cursor.getLong(26)!!
    )
  }

  public fun getDealsByFarmer(farmer_id: String): Query<DealEntity> = getDealsByFarmer(farmer_id) {
      id, shop_id, farmer_id_, buyer_id, commodity_id, deal_status, deal_date, bags_count,
      gross_weight_grams, cut_weight_grams, net_weight_grams, rate_paisa_per_unit,
      gross_amount_paisa, farmer_commission_paisa, buyer_commission_paisa, labour_charge_paisa,
      weighing_charge_paisa, other_deductions_paisa, net_farmer_payable_paisa,
      net_buyer_receivable_paisa, receipt_photo_uri, voice_note_uri, remarks, created_at,
      updated_at, is_deleted, sync_status ->
    DealEntity(
      id,
      shop_id,
      farmer_id_,
      buyer_id,
      commodity_id,
      deal_status,
      deal_date,
      bags_count,
      gross_weight_grams,
      cut_weight_grams,
      net_weight_grams,
      rate_paisa_per_unit,
      gross_amount_paisa,
      farmer_commission_paisa,
      buyer_commission_paisa,
      labour_charge_paisa,
      weighing_charge_paisa,
      other_deductions_paisa,
      net_farmer_payable_paisa,
      net_buyer_receivable_paisa,
      receipt_photo_uri,
      voice_note_uri,
      remarks,
      created_at,
      updated_at,
      is_deleted,
      sync_status
    )
  }

  public fun <T : Any> getDealsByBuyer(buyer_id: String?, mapper: (
    id: String,
    shop_id: String,
    farmer_id: String,
    buyer_id: String?,
    commodity_id: String,
    deal_status: String,
    deal_date: Long,
    bags_count: Long,
    gross_weight_grams: Long,
    cut_weight_grams: Long,
    net_weight_grams: Long,
    rate_paisa_per_unit: Long?,
    gross_amount_paisa: Long,
    farmer_commission_paisa: Long,
    buyer_commission_paisa: Long,
    labour_charge_paisa: Long,
    weighing_charge_paisa: Long,
    other_deductions_paisa: Long,
    net_farmer_payable_paisa: Long,
    net_buyer_receivable_paisa: Long,
    receipt_photo_uri: String?,
    voice_note_uri: String?,
    remarks: String?,
    created_at: Long,
    updated_at: Long,
    is_deleted: Long,
    sync_status: Long,
  ) -> T): Query<T> = GetDealsByBuyerQuery(buyer_id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11),
      cursor.getLong(12)!!,
      cursor.getLong(13)!!,
      cursor.getLong(14)!!,
      cursor.getLong(15)!!,
      cursor.getLong(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getString(20),
      cursor.getString(21),
      cursor.getString(22),
      cursor.getLong(23)!!,
      cursor.getLong(24)!!,
      cursor.getLong(25)!!,
      cursor.getLong(26)!!
    )
  }

  public fun getDealsByBuyer(buyer_id: String?): Query<DealEntity> = getDealsByBuyer(buyer_id) { id,
      shop_id, farmer_id, buyer_id_, commodity_id, deal_status, deal_date, bags_count,
      gross_weight_grams, cut_weight_grams, net_weight_grams, rate_paisa_per_unit,
      gross_amount_paisa, farmer_commission_paisa, buyer_commission_paisa, labour_charge_paisa,
      weighing_charge_paisa, other_deductions_paisa, net_farmer_payable_paisa,
      net_buyer_receivable_paisa, receipt_photo_uri, voice_note_uri, remarks, created_at,
      updated_at, is_deleted, sync_status ->
    DealEntity(
      id,
      shop_id,
      farmer_id,
      buyer_id_,
      commodity_id,
      deal_status,
      deal_date,
      bags_count,
      gross_weight_grams,
      cut_weight_grams,
      net_weight_grams,
      rate_paisa_per_unit,
      gross_amount_paisa,
      farmer_commission_paisa,
      buyer_commission_paisa,
      labour_charge_paisa,
      weighing_charge_paisa,
      other_deductions_paisa,
      net_farmer_payable_paisa,
      net_buyer_receivable_paisa,
      receipt_photo_uri,
      voice_note_uri,
      remarks,
      created_at,
      updated_at,
      is_deleted,
      sync_status
    )
  }

  public fun <T : Any> getDealById(id: String, mapper: (
    id: String,
    shop_id: String,
    farmer_id: String,
    buyer_id: String?,
    commodity_id: String,
    deal_status: String,
    deal_date: Long,
    bags_count: Long,
    gross_weight_grams: Long,
    cut_weight_grams: Long,
    net_weight_grams: Long,
    rate_paisa_per_unit: Long?,
    gross_amount_paisa: Long,
    farmer_commission_paisa: Long,
    buyer_commission_paisa: Long,
    labour_charge_paisa: Long,
    weighing_charge_paisa: Long,
    other_deductions_paisa: Long,
    net_farmer_payable_paisa: Long,
    net_buyer_receivable_paisa: Long,
    receipt_photo_uri: String?,
    voice_note_uri: String?,
    remarks: String?,
    created_at: Long,
    updated_at: Long,
    is_deleted: Long,
    sync_status: Long,
  ) -> T): Query<T> = GetDealByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11),
      cursor.getLong(12)!!,
      cursor.getLong(13)!!,
      cursor.getLong(14)!!,
      cursor.getLong(15)!!,
      cursor.getLong(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getString(20),
      cursor.getString(21),
      cursor.getString(22),
      cursor.getLong(23)!!,
      cursor.getLong(24)!!,
      cursor.getLong(25)!!,
      cursor.getLong(26)!!
    )
  }

  public fun getDealById(id: String): Query<DealEntity> = getDealById(id) { id_, shop_id, farmer_id,
      buyer_id, commodity_id, deal_status, deal_date, bags_count, gross_weight_grams,
      cut_weight_grams, net_weight_grams, rate_paisa_per_unit, gross_amount_paisa,
      farmer_commission_paisa, buyer_commission_paisa, labour_charge_paisa, weighing_charge_paisa,
      other_deductions_paisa, net_farmer_payable_paisa, net_buyer_receivable_paisa,
      receipt_photo_uri, voice_note_uri, remarks, created_at, updated_at, is_deleted, sync_status ->
    DealEntity(
      id_,
      shop_id,
      farmer_id,
      buyer_id,
      commodity_id,
      deal_status,
      deal_date,
      bags_count,
      gross_weight_grams,
      cut_weight_grams,
      net_weight_grams,
      rate_paisa_per_unit,
      gross_amount_paisa,
      farmer_commission_paisa,
      buyer_commission_paisa,
      labour_charge_paisa,
      weighing_charge_paisa,
      other_deductions_paisa,
      net_farmer_payable_paisa,
      net_buyer_receivable_paisa,
      receipt_photo_uri,
      voice_note_uri,
      remarks,
      created_at,
      updated_at,
      is_deleted,
      sync_status
    )
  }

  public fun <T : Any> getPendingDeals(shop_id: String, mapper: (
    id: String,
    shop_id: String,
    farmer_id: String,
    buyer_id: String?,
    commodity_id: String,
    deal_status: String,
    deal_date: Long,
    bags_count: Long,
    gross_weight_grams: Long,
    cut_weight_grams: Long,
    net_weight_grams: Long,
    rate_paisa_per_unit: Long?,
    gross_amount_paisa: Long,
    farmer_commission_paisa: Long,
    buyer_commission_paisa: Long,
    labour_charge_paisa: Long,
    weighing_charge_paisa: Long,
    other_deductions_paisa: Long,
    net_farmer_payable_paisa: Long,
    net_buyer_receivable_paisa: Long,
    receipt_photo_uri: String?,
    voice_note_uri: String?,
    remarks: String?,
    created_at: Long,
    updated_at: Long,
    is_deleted: Long,
    sync_status: Long,
  ) -> T): Query<T> = GetPendingDealsQuery(shop_id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11),
      cursor.getLong(12)!!,
      cursor.getLong(13)!!,
      cursor.getLong(14)!!,
      cursor.getLong(15)!!,
      cursor.getLong(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getString(20),
      cursor.getString(21),
      cursor.getString(22),
      cursor.getLong(23)!!,
      cursor.getLong(24)!!,
      cursor.getLong(25)!!,
      cursor.getLong(26)!!
    )
  }

  public fun getPendingDeals(shop_id: String): Query<DealEntity> = getPendingDeals(shop_id) { id,
      shop_id_, farmer_id, buyer_id, commodity_id, deal_status, deal_date, bags_count,
      gross_weight_grams, cut_weight_grams, net_weight_grams, rate_paisa_per_unit,
      gross_amount_paisa, farmer_commission_paisa, buyer_commission_paisa, labour_charge_paisa,
      weighing_charge_paisa, other_deductions_paisa, net_farmer_payable_paisa,
      net_buyer_receivable_paisa, receipt_photo_uri, voice_note_uri, remarks, created_at,
      updated_at, is_deleted, sync_status ->
    DealEntity(
      id,
      shop_id_,
      farmer_id,
      buyer_id,
      commodity_id,
      deal_status,
      deal_date,
      bags_count,
      gross_weight_grams,
      cut_weight_grams,
      net_weight_grams,
      rate_paisa_per_unit,
      gross_amount_paisa,
      farmer_commission_paisa,
      buyer_commission_paisa,
      labour_charge_paisa,
      weighing_charge_paisa,
      other_deductions_paisa,
      net_farmer_payable_paisa,
      net_buyer_receivable_paisa,
      receipt_photo_uri,
      voice_note_uri,
      remarks,
      created_at,
      updated_at,
      is_deleted,
      sync_status
    )
  }

  public fun <T : Any> getCashTransactionsByParty(party_id: String, mapper: (
    id: String,
    shop_id: String,
    party_id: String,
    deal_id: String?,
    transaction_type: String,
    amount_paisa: Long,
    payment_mode: String,
    transaction_date: Long,
    voice_note_uri: String?,
    remarks: String?,
    created_at: Long,
    updated_at: Long,
    is_deleted: Long,
    sync_status: Long,
  ) -> T): Query<T> = GetCashTransactionsByPartyQuery(party_id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getString(4)!!,
      cursor.getLong(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getString(8),
      cursor.getString(9),
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getLong(13)!!
    )
  }

  public fun getCashTransactionsByParty(party_id: String): Query<CashTransactionEntity> =
      getCashTransactionsByParty(party_id) { id, shop_id, party_id_, deal_id, transaction_type,
      amount_paisa, payment_mode, transaction_date, voice_note_uri, remarks, created_at, updated_at,
      is_deleted, sync_status ->
    CashTransactionEntity(
      id,
      shop_id,
      party_id_,
      deal_id,
      transaction_type,
      amount_paisa,
      payment_mode,
      transaction_date,
      voice_note_uri,
      remarks,
      created_at,
      updated_at,
      is_deleted,
      sync_status
    )
  }

  public fun <T : Any> getCashTransactionsByShop(shop_id: String, mapper: (
    id: String,
    shop_id: String,
    party_id: String,
    deal_id: String?,
    transaction_type: String,
    amount_paisa: Long,
    payment_mode: String,
    transaction_date: Long,
    voice_note_uri: String?,
    remarks: String?,
    created_at: Long,
    updated_at: Long,
    is_deleted: Long,
    sync_status: Long,
  ) -> T): Query<T> = GetCashTransactionsByShopQuery(shop_id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getString(4)!!,
      cursor.getLong(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getString(8),
      cursor.getString(9),
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getLong(13)!!
    )
  }

  public fun getCashTransactionsByShop(shop_id: String): Query<CashTransactionEntity> =
      getCashTransactionsByShop(shop_id) { id, shop_id_, party_id, deal_id, transaction_type,
      amount_paisa, payment_mode, transaction_date, voice_note_uri, remarks, created_at, updated_at,
      is_deleted, sync_status ->
    CashTransactionEntity(
      id,
      shop_id_,
      party_id,
      deal_id,
      transaction_type,
      amount_paisa,
      payment_mode,
      transaction_date,
      voice_note_uri,
      remarks,
      created_at,
      updated_at,
      is_deleted,
      sync_status
    )
  }

  public fun <T : Any> getPartyBalance(id: String, mapper: (
    party_id: String,
    party_name: String,
    party_village: String?,
    party_type: String,
    balance_paisa: Long,
  ) -> T): Query<T> = GetPartyBalanceQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getString(3)!!,
      cursor.getLong(4)!!
    )
  }

  public fun getPartyBalance(id: String): Query<GetPartyBalance> = getPartyBalance(id) { party_id,
      party_name, party_village, party_type, balance_paisa ->
    GetPartyBalance(
      party_id,
      party_name,
      party_village,
      party_type,
      balance_paisa
    )
  }

  public fun <T : Any> getPendingSyncParties(mapper: (
    id: String,
    shop_id: String,
    name: String,
    phone: String?,
    village: String?,
    party_type: String,
    monthly_interest_rate: Double?,
    photo_uri: String?,
    created_at: Long,
    updated_at: Long,
    is_deleted: Long,
    sync_status: Long,
  ) -> T): Query<T> = Query(1_786_564_484, arrayOf("partyEntity"), driver, "AppDatabase.sq",
      "getPendingSyncParties",
      "SELECT partyEntity.id, partyEntity.shop_id, partyEntity.name, partyEntity.phone, partyEntity.village, partyEntity.party_type, partyEntity.monthly_interest_rate, partyEntity.photo_uri, partyEntity.created_at, partyEntity.updated_at, partyEntity.is_deleted, partyEntity.sync_status FROM partyEntity WHERE sync_status = 0") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getString(4),
      cursor.getString(5)!!,
      cursor.getDouble(6),
      cursor.getString(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!
    )
  }

  public fun getPendingSyncParties(): Query<PartyEntity> = getPendingSyncParties { id, shop_id,
      name, phone, village, party_type, monthly_interest_rate, photo_uri, created_at, updated_at,
      is_deleted, sync_status ->
    PartyEntity(
      id,
      shop_id,
      name,
      phone,
      village,
      party_type,
      monthly_interest_rate,
      photo_uri,
      created_at,
      updated_at,
      is_deleted,
      sync_status
    )
  }

  public fun <T : Any> getPendingSyncDeals(mapper: (
    id: String,
    shop_id: String,
    farmer_id: String,
    buyer_id: String?,
    commodity_id: String,
    deal_status: String,
    deal_date: Long,
    bags_count: Long,
    gross_weight_grams: Long,
    cut_weight_grams: Long,
    net_weight_grams: Long,
    rate_paisa_per_unit: Long?,
    gross_amount_paisa: Long,
    farmer_commission_paisa: Long,
    buyer_commission_paisa: Long,
    labour_charge_paisa: Long,
    weighing_charge_paisa: Long,
    other_deductions_paisa: Long,
    net_farmer_payable_paisa: Long,
    net_buyer_receivable_paisa: Long,
    receipt_photo_uri: String?,
    voice_note_uri: String?,
    remarks: String?,
    created_at: Long,
    updated_at: Long,
    is_deleted: Long,
    sync_status: Long,
  ) -> T): Query<T> = Query(-759_957_753, arrayOf("dealEntity"), driver, "AppDatabase.sq",
      "getPendingSyncDeals",
      "SELECT dealEntity.id, dealEntity.shop_id, dealEntity.farmer_id, dealEntity.buyer_id, dealEntity.commodity_id, dealEntity.deal_status, dealEntity.deal_date, dealEntity.bags_count, dealEntity.gross_weight_grams, dealEntity.cut_weight_grams, dealEntity.net_weight_grams, dealEntity.rate_paisa_per_unit, dealEntity.gross_amount_paisa, dealEntity.farmer_commission_paisa, dealEntity.buyer_commission_paisa, dealEntity.labour_charge_paisa, dealEntity.weighing_charge_paisa, dealEntity.other_deductions_paisa, dealEntity.net_farmer_payable_paisa, dealEntity.net_buyer_receivable_paisa, dealEntity.receipt_photo_uri, dealEntity.voice_note_uri, dealEntity.remarks, dealEntity.created_at, dealEntity.updated_at, dealEntity.is_deleted, dealEntity.sync_status FROM dealEntity WHERE sync_status = 0") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11),
      cursor.getLong(12)!!,
      cursor.getLong(13)!!,
      cursor.getLong(14)!!,
      cursor.getLong(15)!!,
      cursor.getLong(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getString(20),
      cursor.getString(21),
      cursor.getString(22),
      cursor.getLong(23)!!,
      cursor.getLong(24)!!,
      cursor.getLong(25)!!,
      cursor.getLong(26)!!
    )
  }

  public fun getPendingSyncDeals(): Query<DealEntity> = getPendingSyncDeals { id, shop_id,
      farmer_id, buyer_id, commodity_id, deal_status, deal_date, bags_count, gross_weight_grams,
      cut_weight_grams, net_weight_grams, rate_paisa_per_unit, gross_amount_paisa,
      farmer_commission_paisa, buyer_commission_paisa, labour_charge_paisa, weighing_charge_paisa,
      other_deductions_paisa, net_farmer_payable_paisa, net_buyer_receivable_paisa,
      receipt_photo_uri, voice_note_uri, remarks, created_at, updated_at, is_deleted, sync_status ->
    DealEntity(
      id,
      shop_id,
      farmer_id,
      buyer_id,
      commodity_id,
      deal_status,
      deal_date,
      bags_count,
      gross_weight_grams,
      cut_weight_grams,
      net_weight_grams,
      rate_paisa_per_unit,
      gross_amount_paisa,
      farmer_commission_paisa,
      buyer_commission_paisa,
      labour_charge_paisa,
      weighing_charge_paisa,
      other_deductions_paisa,
      net_farmer_payable_paisa,
      net_buyer_receivable_paisa,
      receipt_photo_uri,
      voice_note_uri,
      remarks,
      created_at,
      updated_at,
      is_deleted,
      sync_status
    )
  }

  public fun <T : Any> getPendingSyncTransactions(mapper: (
    id: String,
    shop_id: String,
    party_id: String,
    deal_id: String?,
    transaction_type: String,
    amount_paisa: Long,
    payment_mode: String,
    transaction_date: Long,
    voice_note_uri: String?,
    remarks: String?,
    created_at: Long,
    updated_at: Long,
    is_deleted: Long,
    sync_status: Long,
  ) -> T): Query<T> = Query(1_432_417_237, arrayOf("cashTransactionEntity"), driver,
      "AppDatabase.sq", "getPendingSyncTransactions",
      "SELECT cashTransactionEntity.id, cashTransactionEntity.shop_id, cashTransactionEntity.party_id, cashTransactionEntity.deal_id, cashTransactionEntity.transaction_type, cashTransactionEntity.amount_paisa, cashTransactionEntity.payment_mode, cashTransactionEntity.transaction_date, cashTransactionEntity.voice_note_uri, cashTransactionEntity.remarks, cashTransactionEntity.created_at, cashTransactionEntity.updated_at, cashTransactionEntity.is_deleted, cashTransactionEntity.sync_status FROM cashTransactionEntity WHERE sync_status = 0") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getString(4)!!,
      cursor.getLong(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getString(8),
      cursor.getString(9),
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getLong(13)!!
    )
  }

  public fun getPendingSyncTransactions(): Query<CashTransactionEntity> =
      getPendingSyncTransactions { id, shop_id, party_id, deal_id, transaction_type, amount_paisa,
      payment_mode, transaction_date, voice_note_uri, remarks, created_at, updated_at, is_deleted,
      sync_status ->
    CashTransactionEntity(
      id,
      shop_id,
      party_id,
      deal_id,
      transaction_type,
      amount_paisa,
      payment_mode,
      transaction_date,
      voice_note_uri,
      remarks,
      created_at,
      updated_at,
      is_deleted,
      sync_status
    )
  }

  public fun insertShopProfile(
    id: String,
    shop_name: String,
    owner_name: String,
    mandi_name: String,
    shop_number: String?,
    phone_number: String,
    pin_hash: String,
    default_monthly_interest_rate: Double,
    is_sound_enabled: Long,
    created_at: Long,
    updated_at: Long,
    sync_status: Long,
  ) {
    driver.execute(2_052_930_582, """
        |INSERT OR REPLACE INTO shopProfileEntity(id, shop_name, owner_name, mandi_name, shop_number, phone_number, pin_hash, default_monthly_interest_rate, is_sound_enabled, created_at, updated_at, sync_status)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 12) {
          bindString(0, id)
          bindString(1, shop_name)
          bindString(2, owner_name)
          bindString(3, mandi_name)
          bindString(4, shop_number)
          bindString(5, phone_number)
          bindString(6, pin_hash)
          bindDouble(7, default_monthly_interest_rate)
          bindLong(8, is_sound_enabled)
          bindLong(9, created_at)
          bindLong(10, updated_at)
          bindLong(11, sync_status)
        }
    notifyQueries(2_052_930_582) { emit ->
      emit("shopProfileEntity")
    }
  }

  public fun updateSoundSetting(
    is_sound_enabled: Long,
    updated_at: Long,
    id: String,
  ) {
    driver.execute(493_481_582,
        """UPDATE shopProfileEntity SET is_sound_enabled = ?, updated_at = ? WHERE id = ?""", 3) {
          bindLong(0, is_sound_enabled)
          bindLong(1, updated_at)
          bindString(2, id)
        }
    notifyQueries(493_481_582) { emit ->
      emit("shopProfileEntity")
    }
  }

  public fun insertParty(
    id: String,
    shop_id: String,
    name: String,
    phone: String?,
    village: String?,
    party_type: String,
    monthly_interest_rate: Double?,
    photo_uri: String?,
    created_at: Long,
    updated_at: Long,
    is_deleted: Long,
    sync_status: Long,
  ) {
    driver.execute(-1_333_862_551, """
        |INSERT OR REPLACE INTO partyEntity(id, shop_id, name, phone, village, party_type, monthly_interest_rate, photo_uri, created_at, updated_at, is_deleted, sync_status)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 12) {
          bindString(0, id)
          bindString(1, shop_id)
          bindString(2, name)
          bindString(3, phone)
          bindString(4, village)
          bindString(5, party_type)
          bindDouble(6, monthly_interest_rate)
          bindString(7, photo_uri)
          bindLong(8, created_at)
          bindLong(9, updated_at)
          bindLong(10, is_deleted)
          bindLong(11, sync_status)
        }
    notifyQueries(-1_333_862_551) { emit ->
      emit("partyEntity")
    }
  }

  public fun softDeleteParty(updated_at: Long, id: String) {
    driver.execute(1_554_725_677,
        """UPDATE partyEntity SET is_deleted = 1, updated_at = ?, sync_status = 0 WHERE id = ?""",
        2) {
          bindLong(0, updated_at)
          bindString(1, id)
        }
    notifyQueries(1_554_725_677) { emit ->
      emit("partyEntity")
    }
  }

  public fun insertCommodity(
    id: String,
    shop_id: String,
    name_hi: String,
    name_en: String,
    default_unit: String,
    is_active: Long,
    created_at: Long,
    updated_at: Long,
    is_deleted: Long,
    sync_status: Long,
  ) {
    driver.execute(-1_614_355_920, """
        |INSERT OR REPLACE INTO commodityEntity(id, shop_id, name_hi, name_en, default_unit, is_active, created_at, updated_at, is_deleted, sync_status)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 10) {
          bindString(0, id)
          bindString(1, shop_id)
          bindString(2, name_hi)
          bindString(3, name_en)
          bindString(4, default_unit)
          bindLong(5, is_active)
          bindLong(6, created_at)
          bindLong(7, updated_at)
          bindLong(8, is_deleted)
          bindLong(9, sync_status)
        }
    notifyQueries(-1_614_355_920) { emit ->
      emit("commodityEntity")
    }
  }

  public fun insertDeal(
    id: String,
    shop_id: String,
    farmer_id: String,
    buyer_id: String?,
    commodity_id: String,
    deal_status: String,
    deal_date: Long,
    bags_count: Long,
    gross_weight_grams: Long,
    cut_weight_grams: Long,
    net_weight_grams: Long,
    rate_paisa_per_unit: Long?,
    gross_amount_paisa: Long,
    farmer_commission_paisa: Long,
    buyer_commission_paisa: Long,
    labour_charge_paisa: Long,
    weighing_charge_paisa: Long,
    other_deductions_paisa: Long,
    net_farmer_payable_paisa: Long,
    net_buyer_receivable_paisa: Long,
    receipt_photo_uri: String?,
    voice_note_uri: String?,
    remarks: String?,
    created_at: Long,
    updated_at: Long,
    is_deleted: Long,
    sync_status: Long,
  ) {
    driver.execute(95_165_321, """
        |INSERT OR REPLACE INTO dealEntity(
        |    id, shop_id, farmer_id, buyer_id, commodity_id, deal_status, deal_date,
        |    bags_count, gross_weight_grams, cut_weight_grams, net_weight_grams,
        |    rate_paisa_per_unit, gross_amount_paisa,
        |    farmer_commission_paisa, buyer_commission_paisa, labour_charge_paisa, weighing_charge_paisa, other_deductions_paisa,
        |    net_farmer_payable_paisa, net_buyer_receivable_paisa,
        |    receipt_photo_uri, voice_note_uri, remarks,
        |    created_at, updated_at, is_deleted, sync_status
        |) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 27) {
          bindString(0, id)
          bindString(1, shop_id)
          bindString(2, farmer_id)
          bindString(3, buyer_id)
          bindString(4, commodity_id)
          bindString(5, deal_status)
          bindLong(6, deal_date)
          bindLong(7, bags_count)
          bindLong(8, gross_weight_grams)
          bindLong(9, cut_weight_grams)
          bindLong(10, net_weight_grams)
          bindLong(11, rate_paisa_per_unit)
          bindLong(12, gross_amount_paisa)
          bindLong(13, farmer_commission_paisa)
          bindLong(14, buyer_commission_paisa)
          bindLong(15, labour_charge_paisa)
          bindLong(16, weighing_charge_paisa)
          bindLong(17, other_deductions_paisa)
          bindLong(18, net_farmer_payable_paisa)
          bindLong(19, net_buyer_receivable_paisa)
          bindString(20, receipt_photo_uri)
          bindString(21, voice_note_uri)
          bindString(22, remarks)
          bindLong(23, created_at)
          bindLong(24, updated_at)
          bindLong(25, is_deleted)
          bindLong(26, sync_status)
        }
    notifyQueries(95_165_321) { emit ->
      emit("dealEntity")
    }
  }

  public fun softDeleteDeal(updated_at: Long, id: String) {
    driver.execute(-1_335_675_067,
        """UPDATE dealEntity SET is_deleted = 1, updated_at = ?, sync_status = 0 WHERE id = ?""", 2)
        {
          bindLong(0, updated_at)
          bindString(1, id)
        }
    notifyQueries(-1_335_675_067) { emit ->
      emit("dealEntity")
    }
  }

  public fun insertCashTransaction(
    id: String,
    shop_id: String,
    party_id: String,
    deal_id: String?,
    transaction_type: String,
    amount_paisa: Long,
    payment_mode: String,
    transaction_date: Long,
    voice_note_uri: String?,
    remarks: String?,
    created_at: Long,
    updated_at: Long,
    is_deleted: Long,
    sync_status: Long,
  ) {
    driver.execute(2_038_320_750, """
        |INSERT OR REPLACE INTO cashTransactionEntity(id, shop_id, party_id, deal_id, transaction_type, amount_paisa, payment_mode, transaction_date, voice_note_uri, remarks, created_at, updated_at, is_deleted, sync_status)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 14) {
          bindString(0, id)
          bindString(1, shop_id)
          bindString(2, party_id)
          bindString(3, deal_id)
          bindString(4, transaction_type)
          bindLong(5, amount_paisa)
          bindString(6, payment_mode)
          bindLong(7, transaction_date)
          bindString(8, voice_note_uri)
          bindString(9, remarks)
          bindLong(10, created_at)
          bindLong(11, updated_at)
          bindLong(12, is_deleted)
          bindLong(13, sync_status)
        }
    notifyQueries(2_038_320_750) { emit ->
      emit("cashTransactionEntity")
    }
  }

  public fun softDeleteCashTransaction(updated_at: Long, id: String) {
    driver.execute(692_962_610,
        """UPDATE cashTransactionEntity SET is_deleted = 1, updated_at = ?, sync_status = 0 WHERE id = ?""",
        2) {
          bindLong(0, updated_at)
          bindString(1, id)
        }
    notifyQueries(692_962_610) { emit ->
      emit("cashTransactionEntity")
    }
  }

  public fun markPartySynced(id: String) {
    driver.execute(1_975_085_999, """UPDATE partyEntity SET sync_status = 1 WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(1_975_085_999) { emit ->
      emit("partyEntity")
    }
  }

  public fun markDealSynced(id: String) {
    driver.execute(1_053_508_983, """UPDATE dealEntity SET sync_status = 1 WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(1_053_508_983) { emit ->
      emit("dealEntity")
    }
  }

  public fun markTransactionSynced(id: String) {
    driver.execute(-699_676_057,
        """UPDATE cashTransactionEntity SET sync_status = 1 WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(-699_676_057) { emit ->
      emit("cashTransactionEntity")
    }
  }

  private inner class GetAllPartiesQuery<out T : Any>(
    public val shop_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("partyEntity", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("partyEntity", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-336_828_971,
        """SELECT partyEntity.id, partyEntity.shop_id, partyEntity.name, partyEntity.phone, partyEntity.village, partyEntity.party_type, partyEntity.monthly_interest_rate, partyEntity.photo_uri, partyEntity.created_at, partyEntity.updated_at, partyEntity.is_deleted, partyEntity.sync_status FROM partyEntity WHERE shop_id = ? AND is_deleted = 0 ORDER BY name ASC""",
        mapper, 1) {
      bindString(0, shop_id)
    }

    override fun toString(): String = "AppDatabase.sq:getAllParties"
  }

  private inner class GetPartyByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("partyEntity", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("partyEntity", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-846_838_554,
        """SELECT partyEntity.id, partyEntity.shop_id, partyEntity.name, partyEntity.phone, partyEntity.village, partyEntity.party_type, partyEntity.monthly_interest_rate, partyEntity.photo_uri, partyEntity.created_at, partyEntity.updated_at, partyEntity.is_deleted, partyEntity.sync_status FROM partyEntity WHERE id = ? AND is_deleted = 0""",
        mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "AppDatabase.sq:getPartyById"
  }

  private inner class GetAllCommoditiesQuery<out T : Any>(
    public val shop_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("commodityEntity", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("commodityEntity", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-64_087_076,
        """SELECT commodityEntity.id, commodityEntity.shop_id, commodityEntity.name_hi, commodityEntity.name_en, commodityEntity.default_unit, commodityEntity.is_active, commodityEntity.created_at, commodityEntity.updated_at, commodityEntity.is_deleted, commodityEntity.sync_status FROM commodityEntity WHERE shop_id = ? AND is_deleted = 0 ORDER BY name_hi ASC""",
        mapper, 1) {
      bindString(0, shop_id)
    }

    override fun toString(): String = "AppDatabase.sq:getAllCommodities"
  }

  private inner class GetDealsByShopQuery<out T : Any>(
    public val shop_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("dealEntity", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("dealEntity", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(109_086_178,
        """SELECT dealEntity.id, dealEntity.shop_id, dealEntity.farmer_id, dealEntity.buyer_id, dealEntity.commodity_id, dealEntity.deal_status, dealEntity.deal_date, dealEntity.bags_count, dealEntity.gross_weight_grams, dealEntity.cut_weight_grams, dealEntity.net_weight_grams, dealEntity.rate_paisa_per_unit, dealEntity.gross_amount_paisa, dealEntity.farmer_commission_paisa, dealEntity.buyer_commission_paisa, dealEntity.labour_charge_paisa, dealEntity.weighing_charge_paisa, dealEntity.other_deductions_paisa, dealEntity.net_farmer_payable_paisa, dealEntity.net_buyer_receivable_paisa, dealEntity.receipt_photo_uri, dealEntity.voice_note_uri, dealEntity.remarks, dealEntity.created_at, dealEntity.updated_at, dealEntity.is_deleted, dealEntity.sync_status FROM dealEntity WHERE shop_id = ? AND is_deleted = 0 ORDER BY deal_date DESC""",
        mapper, 1) {
      bindString(0, shop_id)
    }

    override fun toString(): String = "AppDatabase.sq:getDealsByShop"
  }

  private inner class GetDealsByFarmerQuery<out T : Any>(
    public val farmer_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("dealEntity", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("dealEntity", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_374_048_079,
        """SELECT dealEntity.id, dealEntity.shop_id, dealEntity.farmer_id, dealEntity.buyer_id, dealEntity.commodity_id, dealEntity.deal_status, dealEntity.deal_date, dealEntity.bags_count, dealEntity.gross_weight_grams, dealEntity.cut_weight_grams, dealEntity.net_weight_grams, dealEntity.rate_paisa_per_unit, dealEntity.gross_amount_paisa, dealEntity.farmer_commission_paisa, dealEntity.buyer_commission_paisa, dealEntity.labour_charge_paisa, dealEntity.weighing_charge_paisa, dealEntity.other_deductions_paisa, dealEntity.net_farmer_payable_paisa, dealEntity.net_buyer_receivable_paisa, dealEntity.receipt_photo_uri, dealEntity.voice_note_uri, dealEntity.remarks, dealEntity.created_at, dealEntity.updated_at, dealEntity.is_deleted, dealEntity.sync_status FROM dealEntity WHERE farmer_id = ? AND is_deleted = 0 ORDER BY deal_date DESC""",
        mapper, 1) {
      bindString(0, farmer_id)
    }

    override fun toString(): String = "AppDatabase.sq:getDealsByFarmer"
  }

  private inner class GetDealsByBuyerQuery<out T : Any>(
    public val buyer_id: String?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("dealEntity", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("dealEntity", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null,
        """SELECT dealEntity.id, dealEntity.shop_id, dealEntity.farmer_id, dealEntity.buyer_id, dealEntity.commodity_id, dealEntity.deal_status, dealEntity.deal_date, dealEntity.bags_count, dealEntity.gross_weight_grams, dealEntity.cut_weight_grams, dealEntity.net_weight_grams, dealEntity.rate_paisa_per_unit, dealEntity.gross_amount_paisa, dealEntity.farmer_commission_paisa, dealEntity.buyer_commission_paisa, dealEntity.labour_charge_paisa, dealEntity.weighing_charge_paisa, dealEntity.other_deductions_paisa, dealEntity.net_farmer_payable_paisa, dealEntity.net_buyer_receivable_paisa, dealEntity.receipt_photo_uri, dealEntity.voice_note_uri, dealEntity.remarks, dealEntity.created_at, dealEntity.updated_at, dealEntity.is_deleted, dealEntity.sync_status FROM dealEntity WHERE buyer_id ${ if (buyer_id == null) "IS" else "=" } ? AND is_deleted = 0 ORDER BY deal_date DESC""",
        mapper, 1) {
      bindString(0, buyer_id)
    }

    override fun toString(): String = "AppDatabase.sq:getDealsByBuyer"
  }

  private inner class GetDealByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("dealEntity", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("dealEntity", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_371_381_648,
        """SELECT dealEntity.id, dealEntity.shop_id, dealEntity.farmer_id, dealEntity.buyer_id, dealEntity.commodity_id, dealEntity.deal_status, dealEntity.deal_date, dealEntity.bags_count, dealEntity.gross_weight_grams, dealEntity.cut_weight_grams, dealEntity.net_weight_grams, dealEntity.rate_paisa_per_unit, dealEntity.gross_amount_paisa, dealEntity.farmer_commission_paisa, dealEntity.buyer_commission_paisa, dealEntity.labour_charge_paisa, dealEntity.weighing_charge_paisa, dealEntity.other_deductions_paisa, dealEntity.net_farmer_payable_paisa, dealEntity.net_buyer_receivable_paisa, dealEntity.receipt_photo_uri, dealEntity.voice_note_uri, dealEntity.remarks, dealEntity.created_at, dealEntity.updated_at, dealEntity.is_deleted, dealEntity.sync_status FROM dealEntity WHERE id = ?""",
        mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "AppDatabase.sq:getDealById"
  }

  private inner class GetPendingDealsQuery<out T : Any>(
    public val shop_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("dealEntity", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("dealEntity", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-726_303_134,
        """SELECT dealEntity.id, dealEntity.shop_id, dealEntity.farmer_id, dealEntity.buyer_id, dealEntity.commodity_id, dealEntity.deal_status, dealEntity.deal_date, dealEntity.bags_count, dealEntity.gross_weight_grams, dealEntity.cut_weight_grams, dealEntity.net_weight_grams, dealEntity.rate_paisa_per_unit, dealEntity.gross_amount_paisa, dealEntity.farmer_commission_paisa, dealEntity.buyer_commission_paisa, dealEntity.labour_charge_paisa, dealEntity.weighing_charge_paisa, dealEntity.other_deductions_paisa, dealEntity.net_farmer_payable_paisa, dealEntity.net_buyer_receivable_paisa, dealEntity.receipt_photo_uri, dealEntity.voice_note_uri, dealEntity.remarks, dealEntity.created_at, dealEntity.updated_at, dealEntity.is_deleted, dealEntity.sync_status FROM dealEntity WHERE shop_id = ? AND deal_status = 'PENDING_SETTLEMENT' AND is_deleted = 0 ORDER BY deal_date DESC""",
        mapper, 1) {
      bindString(0, shop_id)
    }

    override fun toString(): String = "AppDatabase.sq:getPendingDeals"
  }

  private inner class GetCashTransactionsByPartyQuery<out T : Any>(
    public val party_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("cashTransactionEntity", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("cashTransactionEntity", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_401_978_859,
        """SELECT cashTransactionEntity.id, cashTransactionEntity.shop_id, cashTransactionEntity.party_id, cashTransactionEntity.deal_id, cashTransactionEntity.transaction_type, cashTransactionEntity.amount_paisa, cashTransactionEntity.payment_mode, cashTransactionEntity.transaction_date, cashTransactionEntity.voice_note_uri, cashTransactionEntity.remarks, cashTransactionEntity.created_at, cashTransactionEntity.updated_at, cashTransactionEntity.is_deleted, cashTransactionEntity.sync_status FROM cashTransactionEntity WHERE party_id = ? AND is_deleted = 0 ORDER BY transaction_date DESC""",
        mapper, 1) {
      bindString(0, party_id)
    }

    override fun toString(): String = "AppDatabase.sq:getCashTransactionsByParty"
  }

  private inner class GetCashTransactionsByShopQuery<out T : Any>(
    public val shop_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("cashTransactionEntity", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("cashTransactionEntity", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(370_512_871,
        """SELECT cashTransactionEntity.id, cashTransactionEntity.shop_id, cashTransactionEntity.party_id, cashTransactionEntity.deal_id, cashTransactionEntity.transaction_type, cashTransactionEntity.amount_paisa, cashTransactionEntity.payment_mode, cashTransactionEntity.transaction_date, cashTransactionEntity.voice_note_uri, cashTransactionEntity.remarks, cashTransactionEntity.created_at, cashTransactionEntity.updated_at, cashTransactionEntity.is_deleted, cashTransactionEntity.sync_status FROM cashTransactionEntity WHERE shop_id = ? AND is_deleted = 0 ORDER BY transaction_date DESC""",
        mapper, 1) {
      bindString(0, shop_id)
    }

    override fun toString(): String = "AppDatabase.sq:getCashTransactionsByShop"
  }

  private inner class GetPartyBalanceQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("partyEntity", "cashTransactionEntity", "dealEntity", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("partyEntity", "cashTransactionEntity", "dealEntity", listener =
          listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-184_222_392, """
    |SELECT
    |    p.id AS party_id,
    |    p.name AS party_name,
    |    p.village AS party_village,
    |    p.party_type AS party_type,
    |    (
    |        -- 1. Cash Advances given to party (+)
    |        COALESCE((SELECT SUM(amount_paisa) FROM cashTransactionEntity WHERE party_id = p.id AND transaction_type = 'UDHAR_GIVEN' AND is_deleted = 0), 0)
    |        +
    |        -- 2. Interest added to party (+)
    |        COALESCE((SELECT SUM(amount_paisa) FROM cashTransactionEntity WHERE party_id = p.id AND transaction_type = 'INTEREST_ADDED' AND is_deleted = 0), 0)
    |        -
    |        -- 3. Cash Received from party (-)
    |        COALESCE((SELECT SUM(amount_paisa) FROM cashTransactionEntity WHERE party_id = p.id AND transaction_type = 'JAMA_RECEIVED' AND is_deleted = 0), 0)
    |        -
    |        -- 4. Discounts given to party (-)
    |        COALESCE((SELECT SUM(amount_paisa) FROM cashTransactionEntity WHERE party_id = p.id AND transaction_type = 'DISCOUNT_GIVEN' AND is_deleted = 0), 0)
    |        +
    |        -- 5. Deals where party is BUYER: Buyer owes shop (+)
    |        COALESCE((SELECT SUM(net_buyer_receivable_paisa) FROM dealEntity WHERE buyer_id = p.id AND deal_status = 'SETTLED' AND is_deleted = 0), 0)
    |        -
    |        -- 6. Deals where party is FARMER: Shop owes farmer (-)
    |        COALESCE((SELECT SUM(net_farmer_payable_paisa) FROM dealEntity WHERE farmer_id = p.id AND deal_status = 'SETTLED' AND is_deleted = 0), 0)
    |    ) AS balance_paisa
    |FROM partyEntity p
    |WHERE p.id = ? AND p.is_deleted = 0
    """.trimMargin(), mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "AppDatabase.sq:getPartyBalance"
  }
}
