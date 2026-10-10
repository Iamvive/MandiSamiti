package com.appwork.mandisamiti.domain.repository

import com.appwork.mandisamiti.domain.model.TradeSettings
import kotlinx.coroutines.flow.Flow

interface TradeSettingsRepository {
    fun getTradeSettingsStream(): Flow<TradeSettings>
    suspend fun getTradeSettings(): TradeSettings
    suspend fun saveTradeSettings(settings: TradeSettings)
}
