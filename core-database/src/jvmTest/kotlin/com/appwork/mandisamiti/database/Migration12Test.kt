package com.appwork.mandisamiti.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class Migration12Test {
    @Test
    fun addsAddressGstinLicenceColumnsToShopProfile() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "CREATE TABLE shopProfileEntity (id TEXT PRIMARY KEY NOT NULL, shop_name TEXT NOT NULL)", 0)
        driver.execute(null, "INSERT INTO shopProfileEntity VALUES ('s1', 'Test Shop')", 0)

        val sql = File("src/commonMain/sqldelight/com/appwork/mandisamiti/database/12.sqm").readText()
        sql.split(";").map { it.trim() }.filter { it.isNotEmpty() }.forEach { driver.execute(null, it, 0) }

        driver.execute(null, "UPDATE shopProfileEntity SET address = 'Mandi Gate', gstin = '07AAAAA0000A1Z5', licence_no = 'LIC-123' WHERE id = 's1'", 0)

        fun one(q: String): String? = driver.executeQuery(null, q, { c ->
            QueryResult.Value(if (c.next().value) c.getString(0) else null)
        }, 0).value

        assertEquals("Mandi Gate", one("SELECT address FROM shopProfileEntity WHERE id='s1'"))
        assertEquals("07AAAAA0000A1Z5", one("SELECT gstin FROM shopProfileEntity WHERE id='s1'"))
        assertEquals("LIC-123", one("SELECT licence_no FROM shopProfileEntity WHERE id='s1'"))
    }
}
