package com.appwork.mandisamiti.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

fun createTestDatabase(): AppDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    AppDatabase.Schema.create(driver)
    return AppDatabase(driver)
}
