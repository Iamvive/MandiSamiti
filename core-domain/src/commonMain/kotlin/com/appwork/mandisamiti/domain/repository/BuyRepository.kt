package com.appwork.mandisamiti.domain.repository

import com.appwork.mandisamiti.domain.model.*
import kotlinx.coroutines.flow.Flow

interface BuyRepository {
    fun getBuyEntriesStream(shopId: String): Flow<List<BuyEntry>>
    suspend fun getBuyEntryById(id: String): BuyEntry?
    suspend fun saveBuyEntry(entry: BuyEntry)
    suspend fun editBuyEntry(entry: BuyEntry)
    suspend fun voidBuyEntry(id: String, reason: VoidReason)
}
