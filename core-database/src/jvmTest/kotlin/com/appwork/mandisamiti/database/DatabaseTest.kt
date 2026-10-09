package com.appwork.mandisamiti.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DatabaseTest {

    @Test
    fun testDatabasePartyAndDealWorkflow() {
        val driver = DriverFactory().createDriver()
        val db = AppDatabase(driver)
        val queries = db.appDatabaseQueries

        val shopId = "shop-uuid-1"
        val farmerId = "farmer-uuid-1"
        val buyerId = "buyer-uuid-1"
        val commodityId = "comm-uuid-1"

        // 1. Insert Shop Profile
        queries.insertShopProfile(
            id = shopId,
            shop_name = "श्री गणेश ट्रेडिंग",
            owner_name = "लाला रामेश्वर दयाल",
            mandi_name = "नवीन अनाज मंडी, मथुरा",
            shop_number = "दुकान नं. 42",
            phone_number = "9837000000",
            pin_hash = "1234",
            default_monthly_interest_rate = 1.5,
            is_sound_enabled = 1L,
            created_at = 1710928000000L,
            updated_at = 1710928000000L,
            sync_status = 0L
        )

        val shop = queries.getShopProfile().executeAsOneOrNull()
        assertNotNull(shop)
        assertEquals("श्री गणेश ट्रेडिंग", shop.shop_name)

        // 2. Insert Farmer and Buyer
        queries.insertParty(
            id = farmerId,
            shop_id = shopId,
            name = "रामवीर सिंह",
            phone = "9837111111",
            village = "राया",
            party_type = "FARMER",
            monthly_interest_rate = 1.5,
            photo_uri = null,
            created_at = 1710928000000L,
            updated_at = 1710928000000L,
            is_deleted = 0L,
            sync_status = 0L
        )

        queries.insertParty(
            id = buyerId,
            shop_id = shopId,
            name = "अग्रवाल ऑयल मिल",
            phone = "9837222222",
            village = "मथुरा शहर",
            party_type = "BUYER",
            monthly_interest_rate = null,
            photo_uri = null,
            created_at = 1710928000000L,
            updated_at = 1710928000000L,
            is_deleted = 0L,
            sync_status = 0L
        )

        val parties = queries.getAllParties(shopId).executeAsList()
        assertEquals(2, parties.size)

        // 3. Insert Commodity
        queries.insertCommodity(
            id = commodityId,
            shop_id = shopId,
            name_hi = "सरसों",
            name_en = "Mustard",
            default_unit = "QUINTAL",
            is_active = 1L,
            created_at = 1710928000000L,
            updated_at = 1710928000000L,
            is_deleted = 0L,
            sync_status = 0L
        )

        // 4. Record a Settled Deal
        // Gross Amount = ₹97,831 (9,783,100 paisa)
        // Net Farmer Payable = ₹95,090 (9,509,000 paisa) -> Shop owes farmer (-9,509,000)
        // Net Buyer Receivable = ₹99,298 (9,929,800 paisa) -> Buyer owes shop (+9,929,800)
        queries.insertDeal(
            id = "deal-1",
            shop_id = shopId,
            farmer_id = farmerId,
            buyer_id = buyerId,
            commodity_id = commodityId,
            deal_status = "SETTLED",
            deal_date = 1710928000000L,
            bags_count = 35L,
            gross_weight_grams = 1_840_000L,
            cut_weight_grams = 35_000L,
            net_weight_grams = 1_805_000L,
            rate_paisa_per_unit = 542_000L,
            gross_amount_paisa = 9_783_100L,
            farmer_commission_paisa = 244_500L,
            buyer_commission_paisa = 146_700L,
            labour_charge_paisa = 17_500L,
            weighing_charge_paisa = 7_100L,
            other_deductions_paisa = 5_000L,
            net_farmer_payable_paisa = 9_509_000L,
            net_buyer_receivable_paisa = 9_929_800L,
            receipt_photo_uri = null,
            voice_note_uri = null,
            remarks = "बढ़िया सूखी सरसों",
            created_at = 1710928000000L,
            updated_at = 1710928000000L,
            is_deleted = 0L,
            sync_status = 0L,
            farmer_commission_bps = 0L, revision = 1L, is_void = 0L, void_reason = null
        )

        // Check Farmer Balance: Shop owes farmer ₹95,090 -> Balance is -9,509,000 paisa
        val farmerBalance = queries.getPartyBalance(farmerId).executeAsOne()
        assertEquals(-9_509_000L, farmerBalance.balance_paisa)

        // Check Buyer Balance: Buyer owes shop ₹99,298 -> Balance is +9,929,800 paisa
        val buyerBalance = queries.getPartyBalance(buyerId).executeAsOne()
        assertEquals(9_929_800L, buyerBalance.balance_paisa)

        // 5. Pay Farmer ₹50,000 cash (Cash Out / Udhar)
        queries.insertCashTransaction(
            id = "tx-1",
            shop_id = shopId,
            party_id = farmerId,
            deal_id = "deal-1",
            transaction_type = "UDHAR_GIVEN",
            amount_paisa = 5_000_000L, // ₹50,000
            payment_mode = "CASH",
            transaction_date = 1710928000000L,
            voice_note_uri = null,
            remarks = "रोकड़ अदायगी",
            created_at = 1710928000000L,
            updated_at = 1710928000000L,
            is_deleted = 0L,
            sync_status = 0L,
            revision = 1L, is_void = 0L, void_reason = null
        )

        // Farmer balance should now be -9,509,000 + 5,000,000 = -4,509,000 paisa (Shop still owes ₹45,090)
        val updatedFarmerBalance = queries.getPartyBalance(farmerId).executeAsOne()
        assertEquals(-4_509_000L, updatedFarmerBalance.balance_paisa)

        // 6. Test Outbox Sync Queries
        val pendingDeals = queries.getPendingSyncDeals().executeAsList()
        assertEquals(1, pendingDeals.size)
        queries.markDealSynced("deal-1", 1L)
        val remainingPending = queries.getPendingSyncDeals().executeAsList()
        assertEquals(0, remainingPending.size)
    }

    @Test
    fun schemaIsVersion3AndBalanceIgnoresVoids() {
        val db = AppDatabase(DriverFactory().createDriver())
        val q = db.appDatabaseQueries
        assertEquals(3L, AppDatabase.Schema.version)

        q.insertParty("farmer-1", "shop-1", "रामवीर", null, null, "FARMER", null, null, 1L, 1L, 0L, 0L)
        q.insertCashTransaction(
            id = "tx-1", shop_id = "shop-1", party_id = "farmer-1", deal_id = null,
            transaction_type = "UDHAR_GIVEN", amount_paisa = 500_000L, payment_mode = "CASH",
            transaction_date = 1L, voice_note_uri = null, remarks = null,
            created_at = 1L, updated_at = 1L, is_deleted = 0L, sync_status = 0L,
            revision = 1L, is_void = 0L, void_reason = null
        )
        q.insertCashTransaction(
            id = "tx-2", shop_id = "shop-1", party_id = "farmer-1", deal_id = null,
            transaction_type = "UDHAR_GIVEN", amount_paisa = 99_900L, payment_mode = "CASH",
            transaction_date = 2L, voice_note_uri = null, remarks = null,
            created_at = 2L, updated_at = 2L, is_deleted = 0L, sync_status = 0L,
            revision = 2L, is_void = 1L, void_reason = "WRONG_ENTRY"
        )
        assertEquals(500_000L, q.getPartyBalance("farmer-1").executeAsOne().balance_paisa)

        q.insertRevision(
            id = "rev-1", shop_id = "shop-1", entry_id = "tx-2", entry_kind = "CASH",
            revision = 1L, change_kind = "CREATE", snapshot_json = "{}", void_reason = null, changed_at = 2L
        )
        q.insertRevision(
            id = "rev-2", shop_id = "shop-1", entry_id = "tx-2", entry_kind = "CASH",
            revision = 2L, change_kind = "VOID", snapshot_json = "{}", void_reason = "WRONG_ENTRY", changed_at = 3L
        )
        assertEquals(listOf("CREATE", "VOID"), q.getRevisionsForEntry("tx-2").executeAsList().map { it.change_kind })
    }

    @Test
    fun migrationFromV1BackfillsCommissionBpsAndKeepsBalance() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        V1_SCHEMA_DDL.forEach { driver.execute(null, it, 0) }

        driver.execute(null, """
            INSERT INTO shopProfileEntity (id, shop_name, owner_name, mandi_name, phone_number, pin_hash, created_at, updated_at)
            VALUES ('shop-1', 'S', 'O', 'M', '9', 'h', 1, 1)
        """.trimIndent(), 0)
        driver.execute(null, """
            INSERT INTO partyEntity (id, shop_id, name, party_type, created_at, updated_at)
            VALUES ('farmer-1', 'shop-1', 'रामवीर', 'FARMER', 1, 1)
        """.trimIndent(), 0)
        driver.execute(null, """
            INSERT INTO commodityEntity (id, shop_id, name_hi, name_en, created_at, updated_at)
            VALUES ('comm-1', 'shop-1', 'गेहूँ', 'Wheat', 1, 1)
        """.trimIndent(), 0)
        // Settled v1 deal: gross 97,831.00, farmer commission 1,467.47 (1.5%), payable 96,363.53
        driver.execute(null, """
            INSERT INTO dealEntity (id, shop_id, farmer_id, commodity_id, deal_status, deal_date,
                bags_count, gross_weight_grams, net_weight_grams, gross_amount_paisa,
                farmer_commission_paisa, net_farmer_payable_paisa, created_at, updated_at)
            VALUES ('deal-1', 'shop-1', 'farmer-1', 'comm-1', 'SETTLED', 1,
                40, 4000000, 4000000, 9783100, 146747, 9636353, 1, 1)
        """.trimIndent(), 0)
        driver.execute(null, """
            INSERT INTO cashTransactionEntity (id, shop_id, party_id, transaction_type, amount_paisa, transaction_date, created_at, updated_at)
            VALUES ('tx-1', 'shop-1', 'farmer-1', 'UDHAR_GIVEN', 1000000, 1, 1, 1)
        """.trimIndent(), 0)

        // Pre-migration balance, computed with the v1 balance rule (no is_void column yet).
        val preBalance = driver.executeQuery(null, """
            SELECT
              COALESCE((SELECT SUM(amount_paisa) FROM cashTransactionEntity WHERE party_id = 'farmer-1' AND transaction_type IN ('UDHAR_GIVEN','INTEREST_ADDED') AND is_deleted = 0), 0)
            - COALESCE((SELECT SUM(amount_paisa) FROM cashTransactionEntity WHERE party_id = 'farmer-1' AND transaction_type IN ('JAMA_RECEIVED','DISCOUNT_GIVEN') AND is_deleted = 0), 0)
            + COALESCE((SELECT SUM(net_buyer_receivable_paisa) FROM dealEntity WHERE buyer_id = 'farmer-1' AND deal_status = 'SETTLED' AND is_deleted = 0), 0)
            - COALESCE((SELECT SUM(net_farmer_payable_paisa) FROM dealEntity WHERE farmer_id = 'farmer-1' AND deal_status = 'SETTLED' AND is_deleted = 0), 0)
        """.trimIndent(), { cursor ->
            cursor.next()
            app.cash.sqldelight.db.QueryResult.Value(cursor.getLong(0)!!)
        }, 0).value
        assertEquals(1_000_000L - 9_636_353L, preBalance)

        AppDatabase.Schema.migrate(driver, 1, 2)

        val q = AppDatabase(driver).appDatabaseQueries
        val deal = q.getDealById("deal-1").executeAsOne()
        assertEquals(150L, deal.farmer_commission_bps)
        assertEquals(1L, deal.revision)
        assertEquals(0L, deal.is_void)
        assertEquals(1L, q.getCashTransactionById("tx-1").executeAsOne().revision)
        assertEquals(preBalance, q.getPartyBalance("farmer-1").executeAsOne().balance_paisa)
    }
}
