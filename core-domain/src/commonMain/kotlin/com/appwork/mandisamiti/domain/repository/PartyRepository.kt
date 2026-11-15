package com.appwork.mandisamiti.domain.repository

import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyBalance
import kotlinx.coroutines.flow.Flow

interface PartyRepository {
    fun getPartiesStream(shopId: String): Flow<List<Party>>
    fun getPartyBalanceStream(partyId: String): Flow<PartyBalance?>
    suspend fun getPartyById(partyId: String): Party?
    suspend fun getPartyByPhone(shopId: String, phone: String): Party?
    suspend fun saveParty(party: Party)
    suspend fun deleteParty(partyId: String)

    /** Creates the hidden khaate system party for [shopId] if missing. Never listed by [getPartiesStream]. */
    suspend fun ensureKhaateParty(shopId: String, partyType: String) {}
}
