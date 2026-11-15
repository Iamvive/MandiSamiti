package com.appwork.mandisamiti.ui.khaate

import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyBalance
import com.appwork.mandisamiti.domain.repository.PartyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KhaateSavePathTest {
    private class Recorder(val log: MutableList<String>) : PartyRepository {
        override fun getPartiesStream(shopId: String): Flow<List<Party>> = emptyFlow()
        override fun getPartyBalanceStream(partyId: String): Flow<PartyBalance?> = emptyFlow()
        override suspend fun getPartyById(partyId: String): Party? = null
        override suspend fun getPartyByPhone(shopId: String, phone: String): Party? = null
        override suspend fun saveParty(party: Party) {}
        override suspend fun deleteParty(partyId: String) {}
        override suspend fun ensureKhaateParty(shopId: String, partyType: String) { log += "ensure:$shopId:$partyType" }
    }

    @Test fun khaatePartyIsEnsuredBeforeTheEntryIsSaved() = runTest {
        val log = mutableListOf<String>()
        saveKhaateAware(true, "s", "FARMER", Recorder(log)) { log += "save" }
        assertEquals(listOf("ensure:s:FARMER", "save"), log)
    }

    @Test fun normalEntryNeverTouchesKhaateParty() = runTest {
        val log = mutableListOf<String>()
        saveKhaateAware(false, "s", "BUYER", Recorder(log)) { log += "save" }
        assertEquals(listOf("save"), log)
    }
}
