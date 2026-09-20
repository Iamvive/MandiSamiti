package com.appwork.mandisamiti.domain.repository

import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyBalance
import kotlinx.coroutines.flow.Flow

interface PartyRepository {
    fun getPartiesStream(shopId: String): Flow<List<Party>>
    fun getPartyBalanceStream(partyId: String): Flow<PartyBalance?>
    suspend fun getPartyById(partyId: String): Party?
    suspend fun saveParty(party: Party)
    suspend fun deleteParty(partyId: String)
}
