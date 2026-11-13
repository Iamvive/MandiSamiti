package com.appwork.mandisamiti.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class Migration11Test {
    @Test
    fun rewritesOldRatesAndAddsLoanColumn() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "CREATE TABLE lendEntryEntity (id TEXT PRIMARY KEY NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE shopProfileEntity (id TEXT PRIMARY KEY NOT NULL, default_monthly_interest_rate REAL NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE partyEntity (id TEXT PRIMARY KEY NOT NULL, monthly_interest_rate REAL)", 0)
        driver.execute(null, "INSERT INTO lendEntryEntity(id) VALUES ('l1')", 0)
        driver.execute(null, "INSERT INTO shopProfileEntity VALUES ('s1', 1.5), ('s2', 3.0)", 0)
        driver.execute(null, "INSERT INTO partyEntity VALUES ('p1', 1.5), ('p2', NULL)", 0)

        val sql = File("src/commonMain/sqldelight/com/appwork/mandisamiti/database/11.sqm").readText()
        sql.split(";").map { it.trim() }.filter { it.isNotEmpty() }.forEach { driver.execute(null, it, 0) }

        fun one(q: String): Double? = driver.executeQuery(null, q, { c ->
            QueryResult.Value(if (c.next().value) c.getDouble(0) else null)
        }, 0).value
        assertEquals(2.0, one("SELECT monthly_interest_rate FROM lendEntryEntity WHERE id='l1'"))
        assertEquals(2.0, one("SELECT default_monthly_interest_rate FROM shopProfileEntity WHERE id='s1'"))
        assertEquals(3.0, one("SELECT default_monthly_interest_rate FROM shopProfileEntity WHERE id='s2'"))
        assertEquals(2.0, one("SELECT monthly_interest_rate FROM partyEntity WHERE id='p1'"))
        assertEquals(null, one("SELECT monthly_interest_rate FROM partyEntity WHERE id='p2'"))
    }
}
