package com.appwork.mandisamiti.domain.repository

import com.appwork.mandisamiti.domain.model.Deal
import kotlinx.coroutines.flow.Flow

interface DealRepository {
    fun getDealsByShopStream(shopId: String): Flow<List<Deal>>
    fun getDealsByFarmerStream(farmerId: String): Flow<List<Deal>>
    fun getDealsByBuyerStream(buyerId: String): Flow<List<Deal>>
    fun getPendingDealsStream(shopId: String): Flow<List<Deal>>
    suspend fun getDealById(dealId: String): Deal?
    suspend fun saveDeal(deal: Deal)
    suspend fun editDeal(deal: Deal)
    suspend fun deleteDeal(dealId: String)
}
