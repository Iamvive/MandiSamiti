package com.appwork.mandisamiti.data.auth

import com.appwork.mandisamiti.database.AppDatabase

class LocalDataWiper(private val database: AppDatabase) {
    /** Deletes every local row, children first, in one transaction. */
    suspend fun wipeAll() {
        val q = database.appDatabaseQueries
        database.transaction {
            q.wipeSyncMetadata()
            q.wipeRevisions()
            q.wipeCashTransactions()
            q.wipeDeals()
            q.wipeCommodities()
            q.wipeParties()
            q.wipeShopProfiles()
        }
    }

    /** True if any profile or any party/deal/cash/etc. row belongs to a shop other than [shopId]. */
    fun hasDataOutsideShop(shopId: String): Boolean =
        database.appDatabaseQueries.countForeignRows(shopId).executeAsOne() > 0

    /**
     * True if another shop that was adopted from the server (profile sync_status = 1) still has
     * unsynced rows here. Wiping those would lose entries that never reached the server.
     */
    fun hasUnsyncedServerShopDataOutside(shopId: String): Boolean =
        database.appDatabaseQueries.countForeignPendingFromServerShop(shopId).executeAsOne() > 0
}
