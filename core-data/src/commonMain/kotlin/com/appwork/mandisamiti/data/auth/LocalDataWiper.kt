package com.appwork.mandisamiti.data.auth

import com.appwork.mandisamiti.database.AppDatabase

class LocalDataWiper(private val database: AppDatabase) {
    /** Deletes every local row, children first, in one transaction. */
    suspend fun wipeAll() {
        val q = database.appDatabaseQueries
        database.transaction {
            q.wipeSyncMetadata()
            q.wipeRevisions()
            q.wipeLendSettlements()
            q.wipeLendEntries()
            q.wipeBuyEntries()
            q.wipeSellEntries()
            q.wipeExpenses()
            q.wipeCashTransactions()
            q.wipeDeals()
            q.wipeCommodities()
            q.wipeParties()
            q.wipeShopProfiles()
            q.wipeShopQuota()
            q.wipeAnalyticsEvents()
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

    /** True if every other local shop profile has [phone]: the same business under a shop id the server no longer knows. */
    fun foreignShopsAllHavePhone(shopId: String, phone: String): Boolean =
        database.appDatabaseQueries.countForeignProfilesWithOtherPhone(shopId, phone).executeAsOne() == 0L

    /** Moves every foreign row to [shopId] and marks it pending so the normal sync pushes it. Call after saving the [shopId] profile. */
    fun rehomeForeignRowsTo(shopId: String) {
        val q = database.appDatabaseQueries
        database.transaction {
            q.rehomeParties(shopId)
            q.rehomeCommodities(shopId)
            q.rehomeDeals(shopId)
            q.rehomeCashTransactions(shopId)
            q.rehomeRevisions(shopId)
            q.rehomeBuyEntries(shopId)
            q.rehomeSellEntries(shopId)
            q.rehomeLendEntries(shopId)
            q.rehomeLendSettlements(shopId)
            q.rehomeDayClosings(shopId)
            q.rehomeExpenses(shopId)
            q.deleteForeignShopQuota(shopId)
            q.deleteForeignShopProfiles(shopId)
        }
    }
}
