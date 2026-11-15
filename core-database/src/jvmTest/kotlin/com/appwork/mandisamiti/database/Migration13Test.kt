package com.appwork.mandisamiti.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class Migration13Test {
    @Test
    fun addsIsKhaateDefaultingToZeroAndKeepsExistingRows() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "CREATE TABLE buyEntryEntity (id TEXT PRIMARY KEY NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE sellEntryEntity (id TEXT PRIMARY KEY NOT NULL)", 0)
        driver.execute(null, "INSERT INTO buyEntryEntity VALUES ('b1')", 0)
        driver.execute(null, "INSERT INTO sellEntryEntity VALUES ('s1')", 0)

        File("src/commonMain/sqldelight/com/appwork/mandisamiti/database/13.sqm").readText()
            .split(";").map { it.trim() }.filter { it.isNotEmpty() }.forEach { driver.execute(null, it, 0) }

        fun one(q: String): Long = driver.executeQuery(null, q, { c ->
            QueryResult.Value(if (c.next().value) c.getLong(0) else null)
        }, 0).value!!

        assertEquals(0L, one("SELECT is_khaate FROM buyEntryEntity WHERE id='b1'"))
        assertEquals(0L, one("SELECT is_khaate FROM sellEntryEntity WHERE id='s1'"))
    }
}
