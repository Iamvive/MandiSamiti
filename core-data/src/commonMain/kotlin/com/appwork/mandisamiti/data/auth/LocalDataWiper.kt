package com.appwork.mandisamiti.data.auth

import com.appwork.mandisamiti.database.AppDatabase

class LocalDataWiper(private val database: AppDatabase) {
    /** Deletes every local row, children first, in one transaction. */
    suspend fun wipeAll() {
        val q = database.appDatabaseQueries
        database.transaction {
            q.wipeRevisions()
            q.wipeCashTransactions()
            q.wipeDeals()
            q.wipeCommodities()
            q.wipeParties()
            q.wipeShopProfiles()
        }
    }
}
