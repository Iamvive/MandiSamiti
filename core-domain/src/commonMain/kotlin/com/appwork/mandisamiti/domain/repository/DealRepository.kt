package com.appwork.mandisamiti.domain.repository

import com.appwork.mandisamiti.domain.model.Deal
import com.appwork.mandisamiti.domain.model.VoidReason
import kotlinx.coroutines.flow.Flow

interface DealRepository {
    fun getDealsByShopStream(shopId: String): Flow<List<Deal>>
    fun getDealsByFarmerStream(farmerId: String): Flow<List<Deal>>
    fun getDealsByBuyerStream(buyerId: String): Flow<List<Deal>>
    fun getPendingDealsStream(shopId: String): Flow<List<Deal>>
    suspend fun getDealById(dealId: String): Deal?
    suspend fun saveDeal(deal: Deal)
    suspend fun editDeal(deal: Deal)
    /** Marks the deal void with a reason; it stays in lists but leaves all balances. */
    suspend fun voidDeal(dealId: String, reason: VoidReason)
}
