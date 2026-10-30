package com.appwork.mandisamiti.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PilotSchemaMigrationTest {

    @Test
    fun versionOneSnapshotUpgradesToCurrentSchemaWithoutDataLoss() {
        assertEquals(CURRENT_SCHEMA_VERSION, AppDatabase.Schema.version)

        val snapshot = committedV1Snapshot()
        val database = Files.createTempFile("mandisamiti-v1-", ".db")
        Files.copy(snapshot, database, StandardCopyOption.REPLACE_EXISTING)
        val driver = JdbcSqliteDriver("jdbc:sqlite:${database.toAbsolutePath()}")

        try {
            AppDatabase.Schema.migrate(driver, 1, PRE_PILOT_VERSION)
            insertOneRowInEveryTable(driver)

            val countsBefore = TABLES.associateWith { driver.scalarLong("SELECT COUNT(*) FROM $it") }
            val paisaBefore = PAISA_VALUES.associateWith { driver.scalarLong(it.sql) }

            AppDatabase.Schema.migrate(driver, PRE_PILOT_VERSION, CURRENT_SCHEMA_VERSION)

            assertEquals(countsBefore, TABLES.associateWith { driver.scalarLong("SELECT COUNT(*) FROM $it") })
            assertEquals(paisaBefore, PAISA_VALUES.associateWith { driver.scalarLong(it.sql) })
            assertEquals(0L, driver.scalarLong("SELECT COUNT(slip_no) FROM buyEntryEntity"))
            assertEquals(0L, driver.scalarLong("SELECT COUNT(slip_no) FROM sellEntryEntity"))
            assertEquals(0L, driver.scalarLong("SELECT COUNT(slip_no) FROM lendEntryEntity"))
            assertEquals(1L, driver.scalarLong("SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND name = 'idx_party_shop_phone'"))
            assertEquals(0L, driver.scalarLong("SELECT COUNT(gstin) FROM partyEntity"))
            assertEquals(0L, driver.scalarLong("SELECT COUNT(mandi_license) FROM partyEntity"))
        } finally {
            driver.close()
            database.deleteIfExists()
        }
    }

    @Test
    fun versionSevenSnapshotAddsNullablePartyTaxFieldsWithoutDataLoss() {
        val snapshot = committedSnapshot(7)
        val database = Files.createTempFile("mandisamiti-v7-", ".db")
        Files.copy(snapshot, database, StandardCopyOption.REPLACE_EXISTING)
        val driver = JdbcSqliteDriver("jdbc:sqlite:${database.toAbsolutePath()}")

        try {
            driver.execute(
                null,
                """INSERT INTO shopProfileEntity
                    (id, shop_name, owner_name, mandi_name, phone_number, pin_hash, created_at, updated_at)
                    VALUES ('shop-tax', 'Shop', 'Owner', 'Mandi', '9000000000', 'hash', 1, 1)""".trimIndent(),
                0
            )
            driver.execute(
                null,
                """INSERT INTO partyEntity
                    (id, shop_id, name, phone, village, party_type, monthly_interest_rate, photo_uri,
                     created_at, updated_at, is_deleted, sync_status)
                    VALUES ('party-tax', 'shop-tax', 'Party', '9111111111', 'Village', 'BUYER', 1.5,
                            'photo', 2, 3, 0, 1)""".trimIndent(),
                0
            )

            AppDatabase.Schema.migrate(driver, 7, CURRENT_SCHEMA_VERSION)

            assertEquals(1L, driver.scalarLong("SELECT COUNT(*) FROM partyEntity WHERE id = 'party-tax'"))
            assertEquals(1L, driver.scalarLong("SELECT COUNT(*) FROM partyEntity WHERE name = 'Party' AND village = 'Village'"))
            assertEquals(0L, driver.scalarLong("SELECT COUNT(gstin) FROM partyEntity WHERE id = 'party-tax'"))
            assertEquals(0L, driver.scalarLong("SELECT COUNT(mandi_license) FROM partyEntity WHERE id = 'party-tax'"))
        } finally {
            driver.close()
            database.deleteIfExists()
        }
    }

    private fun committedV1Snapshot(): Path {
        return committedSnapshot(1)
    }

    private fun committedSnapshot(version: Int): Path {
        val relative = Path.of("src/commonMain/sqldelight/databases/$version.db")
        val snapshot = listOf(relative, Path.of("core-database").resolve(relative)).firstOrNull(Files::isRegularFile)
        assertNotNull(snapshot, "Committed SQLDelight snapshot $version.db is missing")
        return snapshot
    }

    private fun insertOneRowInEveryTable(driver: SqlDriver) {
        listOf(
            """INSERT INTO shopProfileEntity
                (id, shop_name, owner_name, mandi_name, phone_number, pin_hash, created_at, updated_at)
                VALUES ('shop-1', 'Shop', 'Owner', 'Mandi', '9000000000', 'hash', 1, 1)""",
            """INSERT INTO partyEntity
                (id, shop_id, name, phone, party_type, created_at, updated_at)
                VALUES ('party-1', 'shop-1', 'Party', '9111111111', 'FARMER', 2, 2)""",
            """INSERT INTO commodityEntity
                (id, shop_id, name_hi, name_en, created_at, updated_at)
                VALUES ('commodity-1', 'shop-1', 'गेहूँ', 'Wheat', 3, 3)""",
            """INSERT INTO dealEntity
                (id, shop_id, farmer_id, commodity_id, deal_status, deal_date, bags_count,
                 gross_weight_grams, net_weight_grams, gross_amount_paisa,
                 net_farmer_payable_paisa, net_buyer_receivable_paisa, created_at, updated_at)
                VALUES ('deal-1', 'shop-1', 'party-1', 'commodity-1', 'SETTLED', 4, 10,
                        1000000, 990000, 123456789, 120000000, 125000000, 4, 4)""",
            """INSERT INTO cashTransactionEntity
                (id, shop_id, party_id, transaction_type, amount_paisa, transaction_date, created_at, updated_at)
                VALUES ('cash-1', 'shop-1', 'party-1', 'JAMA_RECEIVED', 987654321, 5, 5, 5)""",
            """INSERT INTO entryRevisionEntity
                (id, shop_id, entry_id, entry_kind, revision, change_kind, snapshot_json, changed_at)
                VALUES ('revision-1', 'shop-1', 'cash-1', 'CASH', 1, 'CREATE', '{}', 6)""",
            """INSERT INTO buyEntryEntity
                (id, shop_id, farmer_id, commodity_id, entry_date, bags_count, weight_per_bag_grams,
                 total_weight_grams, rate_paisa_per_quintal, gross_paisa, commission_paisa,
                 labour_paisa, net_payable_paisa, created_at, updated_at)
                VALUES ('buy-1', 'shop-1', 'party-1', 'commodity-1', 7, 8, 50000,
                        400000, 250000, 1000000, 15000, 8000, 977000, 7, 7)""",
            """INSERT INTO sellEntryEntity
                (id, shop_id, buyer_id, commodity_id, entry_date, bill_mode, party_kind, bags_count,
                 weight_per_bag_grams, total_weight_grams, rate_paisa_per_quintal, gross_paisa,
                 arat_paisa, mandi_shulk_paisa, total_receivable_paisa, balance_due_paisa,
                 created_at, updated_at)
                VALUES ('sell-1', 'shop-1', 'party-1', 'commodity-1', 8, 'NORMAL', 'MERCHANT', 8,
                        50000, 400000, 275000, 1100000, 22000, 11000, 1133000, 1133000, 8, 8)""",
            """INSERT INTO lendEntryEntity
                (id, shop_id, party_id, lend_type, entry_date, amount_paisa, created_at, updated_at)
                VALUES ('lend-1', 'shop-1', 'party-1', 'MONEY', 9, 765432100, 9, 9)""",
            """INSERT INTO lendSettlementEntity
                (id, shop_id, lend_id, settled_at, amount_paisa, created_at, updated_at,
                 principal_paisa, interest_paisa, calculated_interest_paisa)
                VALUES ('settlement-1', 'shop-1', 'lend-1', 10, 1234500, 10, 10,
                        1000000, 234500, 250000)""",
            """INSERT INTO expenseEntryEntity
                (id, shop_id, entry_date, category, amount_paisa, created_at, updated_at)
                VALUES ('expense-1', 'shop-1', 11, 'OTHER', 45678900, 11, 11)""",
            """INSERT INTO dayClosingEntity
                (id, shop_id, day_date, closed_at, expected_paisa, counted_paisa,
                 difference_paisa, created_at, updated_at)
                VALUES ('closing-1', 'shop-1', 12, 12, 111111111, 111111100, -11, 12, 12)""",
            """INSERT INTO syncMetadataEntity (key, value_text, value_long)
                VALUES ('last_pull', 'cursor-1', 13)"""
        ).forEach { driver.execute(null, it.trimIndent(), 0) }
    }

    private fun SqlDriver.scalarLong(sql: String): Long = executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor ->
            assertTrue(cursor.next().value)
            QueryResult.Value(cursor.getLong(0)!!)
        },
        parameters = 0
    ).value

    private data class PaisaValue(val sql: String)

    private companion object {
        const val PRE_PILOT_VERSION = 6L
        const val CURRENT_SCHEMA_VERSION = 8L

        val TABLES = listOf(
            "shopProfileEntity",
            "partyEntity",
            "commodityEntity",
            "dealEntity",
            "cashTransactionEntity",
            "entryRevisionEntity",
            "buyEntryEntity",
            "sellEntryEntity",
            "lendEntryEntity",
            "lendSettlementEntity",
            "expenseEntryEntity",
            "dayClosingEntity",
            "syncMetadataEntity"
        )

        val PAISA_VALUES = listOf(
            PaisaValue("SELECT gross_amount_paisa FROM dealEntity WHERE id = 'deal-1'"),
            PaisaValue("SELECT amount_paisa FROM cashTransactionEntity WHERE id = 'cash-1'"),
            PaisaValue("SELECT net_payable_paisa FROM buyEntryEntity WHERE id = 'buy-1'"),
            PaisaValue("SELECT total_receivable_paisa FROM sellEntryEntity WHERE id = 'sell-1'"),
            PaisaValue("SELECT amount_paisa FROM lendEntryEntity WHERE id = 'lend-1'"),
            PaisaValue("SELECT principal_paisa FROM lendSettlementEntity WHERE id = 'settlement-1'"),
            PaisaValue("SELECT interest_paisa FROM lendSettlementEntity WHERE id = 'settlement-1'"),
            PaisaValue("SELECT calculated_interest_paisa FROM lendSettlementEntity WHERE id = 'settlement-1'"),
            PaisaValue("SELECT amount_paisa FROM expenseEntryEntity WHERE id = 'expense-1'"),
            PaisaValue("SELECT difference_paisa FROM dayClosingEntity WHERE id = 'closing-1'")
        )
    }
}
