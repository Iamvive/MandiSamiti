package com.appwork.mandisamiti.database

import kotlin.test.Test
import kotlin.test.assertEquals

class ServerApplyQueriesTest {
    private fun party(q: AppDatabaseQueries, id: String, name: String, sync: Long) =
        q.insertParty(id, "shop-1", name, null, null, "FARMER", null, null, 1L, 1L, 0L, sync)

    @Test
    fun serverRowReplacesSyncedRowButNotPendingRow() {
        val q = createTestDatabase().appDatabaseQueries
        party(q, "synced", "old", 1L)
        party(q, "pending", "local edit", 0L)

        listOf("synced", "pending", "new").forEach { id ->
            q.deleteSyncedParty(id)
            q.insertPartyIfAbsent(id, "shop-1", "server", null, null, "FARMER", null, null, 2L, 2L, 0L, 1L)
        }

        assertEquals("server", q.getPartyById("synced").executeAsOne().name)
        assertEquals("local edit", q.getPartyById("pending").executeAsOne().name)
        assertEquals(0L, q.getPartyById("pending").executeAsOne().sync_status)
        assertEquals("server", q.getPartyById("new").executeAsOne().name)
    }

    @Test
    fun revisionFromServerIsIgnoredWhenHeldAndStoredAsSynced() {
        val q = createTestDatabase().appDatabaseQueries
        q.insertRevision("r1", "shop-1", "d1", "DEAL", 1L, "CREATE", "{}", null, 1L)
        q.insertRevisionFromServer("r1", "shop-1", "d1", "DEAL", 1L, "CREATE", "{}", null, 1L)       // own revision back
        q.insertRevisionFromServer("r2", "shop-1", "d1", "DEAL", 2L, "EDIT", "{}", null, 2L)
        assertEquals(1L, q.getPendingSyncRevisions().executeAsList().size.toLong())            // only r1, ours
        assertEquals(2, q.getRevisionsForEntry("d1").executeAsList().size)
    }

    @Test
    fun markPartySyncedAtSkipsRowEditedAfterPush() {
        val q = createTestDatabase().appDatabaseQueries
        party(q, "p", "v1", 0L)                                                     // pushed at updated_at = 1
        q.insertParty("p", "shop-1", "v2", null, null, "FARMER", null, null, 1L, 5L, 0L, 0L)  // edited meanwhile
        q.markPartySyncedAt("p", 1L)
        assertEquals(0L, q.getPartyById("p").executeAsOne().sync_status)
    }
}
