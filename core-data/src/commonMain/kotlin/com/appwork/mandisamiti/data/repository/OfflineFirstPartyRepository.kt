package com.appwork.mandisamiti.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.PartyEntity
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyBalance
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.repository.PartyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class OfflineFirstPartyRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : PartyRepository {

    private val queries = database.appDatabaseQueries

    override fun getPartiesStream(shopId: String): Flow<List<Party>> {
        return queries.getAllParties(shopId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getPartyBalanceStream(partyId: String): Flow<PartyBalance?> {
        return queries.getPartyBalance(partyId)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { row ->
                if (row == null) return@map null
                val party = queries.getPartyById(partyId).executeAsOneOrNull()?.toDomain()
                if (party != null) {
                    PartyBalance(
                        party = party,
                        balancePaisa = row.balance_paisa ?: 0L,
                        lastTransactionDate = null
                    )
                } else null
            }
    }

    override suspend fun getPartyById(partyId: String): Party? = withContext(ioDispatcher) {
        queries.getPartyById(partyId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun saveParty(party: Party) = withContext(ioDispatcher) {
        queries.insertParty(
            id = party.id,
            shop_id = party.shopId,
            name = party.name,
            phone = party.phone,
            village = party.village,
            party_type = party.partyType.name,
            monthly_interest_rate = party.monthlyInterestRate,
            photo_uri = party.photoUri,
            created_at = party.createdAt,
            updated_at = party.updatedAt,
            is_deleted = if (party.isDeleted) 1L else 0L,
            sync_status = party.syncStatus.toLong()
        )
    }

    override suspend fun deleteParty(partyId: String) = withContext(ioDispatcher) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        queries.softDeleteParty(updated_at = now, id = partyId)
    }

    private fun PartyEntity.toDomain(): Party {
        return Party(
            id = id,
            shopId = shop_id,
            name = name,
            phone = phone,
            village = village,
            partyType = PartyType.valueOf(party_type),
            monthlyInterestRate = monthly_interest_rate,
            photoUri = photo_uri,
            createdAt = created_at,
            updatedAt = updated_at,
            isDeleted = is_deleted == 1L,
            syncStatus = sync_status.toInt()
        )
    }
}
