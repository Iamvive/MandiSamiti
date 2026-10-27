package com.appwork.mandisamiti.domain.repository

import com.appwork.mandisamiti.domain.model.*
import kotlinx.coroutines.flow.Flow

interface SellRepository {
    fun getSellEntriesStream(shopId: String): Flow<List<SellEntry>>
    suspend fun getSellEntryById(id: String): SellEntry?
    suspend fun saveSellEntry(entry: SellEntry)
    suspend fun editSellEntry(entry: SellEntry)
    suspend fun voidSellEntry(id: String, reason: VoidReason)
}
