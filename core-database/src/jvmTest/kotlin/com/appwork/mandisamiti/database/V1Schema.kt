package com.appwork.mandisamiti.database

/**
 * Schema version 1 DDL, copied verbatim (CREATE statements only) from
 * `git show ddafefb:core-database/.../AppDatabase.sq`. Used to build a real
 * v1 database so data migrations can be exercised, not just the schema diff.
 */
internal val V1_SCHEMA_DDL: List<String> = """
CREATE TABLE shopProfileEntity (
    id TEXT PRIMARY KEY NOT NULL,
    shop_name TEXT NOT NULL,
    owner_name TEXT NOT NULL,
    mandi_name TEXT NOT NULL,
    shop_number TEXT,
    phone_number TEXT NOT NULL,
    pin_hash TEXT NOT NULL,
    default_monthly_interest_rate REAL NOT NULL DEFAULT 1.5,
    is_sound_enabled INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    sync_status INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE partyEntity (
    id TEXT PRIMARY KEY NOT NULL,
    shop_id TEXT NOT NULL,
    name TEXT NOT NULL,
    phone TEXT,
    village TEXT,
    party_type TEXT NOT NULL,
    monthly_interest_rate REAL,
    photo_uri TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    sync_status INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(shop_id) REFERENCES shopProfileEntity(id)
);
CREATE INDEX idx_party_shop ON partyEntity(shop_id, is_deleted);
CREATE INDEX idx_party_name ON partyEntity(name);
CREATE TABLE commodityEntity (
    id TEXT PRIMARY KEY NOT NULL,
    shop_id TEXT NOT NULL,
    name_hi TEXT NOT NULL,
    name_en TEXT NOT NULL,
    default_unit TEXT NOT NULL DEFAULT 'QUINTAL',
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    sync_status INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(shop_id) REFERENCES shopProfileEntity(id)
);
CREATE TABLE dealEntity (
    id TEXT PRIMARY KEY NOT NULL,
    shop_id TEXT NOT NULL,
    farmer_id TEXT NOT NULL,
    buyer_id TEXT,
    commodity_id TEXT NOT NULL,
    deal_status TEXT NOT NULL,
    deal_date INTEGER NOT NULL,

    bags_count INTEGER NOT NULL,
    gross_weight_grams INTEGER NOT NULL,
    cut_weight_grams INTEGER NOT NULL DEFAULT 0,
    net_weight_grams INTEGER NOT NULL,

    rate_paisa_per_unit INTEGER,
    gross_amount_paisa INTEGER NOT NULL DEFAULT 0,

    farmer_commission_paisa INTEGER NOT NULL DEFAULT 0,
    buyer_commission_paisa INTEGER NOT NULL DEFAULT 0,
    labour_charge_paisa INTEGER NOT NULL DEFAULT 0,
    weighing_charge_paisa INTEGER NOT NULL DEFAULT 0,
    other_deductions_paisa INTEGER NOT NULL DEFAULT 0,

    net_farmer_payable_paisa INTEGER NOT NULL DEFAULT 0,
    net_buyer_receivable_paisa INTEGER NOT NULL DEFAULT 0,

    receipt_photo_uri TEXT,
    voice_note_uri TEXT,
    remarks TEXT,

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    sync_status INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(farmer_id) REFERENCES partyEntity(id),
    FOREIGN KEY(buyer_id) REFERENCES partyEntity(id),
    FOREIGN KEY(commodity_id) REFERENCES commodityEntity(id)
);
CREATE INDEX idx_deal_farmer ON dealEntity(farmer_id, is_deleted);
CREATE INDEX idx_deal_buyer ON dealEntity(buyer_id, is_deleted);
CREATE INDEX idx_deal_date ON dealEntity(deal_date);
CREATE TABLE cashTransactionEntity (
    id TEXT PRIMARY KEY NOT NULL,
    shop_id TEXT NOT NULL,
    party_id TEXT NOT NULL,
    deal_id TEXT,
    transaction_type TEXT NOT NULL,
    amount_paisa INTEGER NOT NULL,
    payment_mode TEXT NOT NULL DEFAULT 'CASH',
    transaction_date INTEGER NOT NULL,
    voice_note_uri TEXT,
    remarks TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    sync_status INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(party_id) REFERENCES partyEntity(id),
    FOREIGN KEY(deal_id) REFERENCES dealEntity(id)
);
CREATE INDEX idx_tx_party ON cashTransactionEntity(party_id, is_deleted);
CREATE INDEX idx_tx_date ON cashTransactionEntity(transaction_date);
""".trimIndent().split(";").map { it.trim() }.filter { it.isNotEmpty() }
