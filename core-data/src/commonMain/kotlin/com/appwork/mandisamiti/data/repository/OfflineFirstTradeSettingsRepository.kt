package com.appwork.mandisamiti.data.repository

import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.domain.model.TradeSettings
import com.appwork.mandisamiti.domain.repository.TradeSettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class OfflineFirstTradeSettingsRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : TradeSettingsRepository {

    private val queries = database.appDatabaseQueries

    private val _settingsFlow = MutableStateFlow(loadSettingsFromDb())

    override fun getTradeSettingsStream(): Flow<TradeSettings> = _settingsFlow.asStateFlow()

    override suspend fun getTradeSettings(): TradeSettings = withContext(ioDispatcher) {
        loadSettingsFromDb()
    }

    override suspend fun saveTradeSettings(settings: TradeSettings) = withContext(ioDispatcher) {
        database.transaction {
            queries.setSyncMetadataLong(KEY_FARMER_COMM_BPS, settings.farmerCommissionBps)
            queries.setSyncMetadataLong(KEY_BUYER_COMM_BPS, settings.buyerCommissionBps)
            queries.setSyncMetadataLong(KEY_LABOUR_PAISA, settings.defaultLabourPaisa)
            queries.setSyncMetadataLong(KEY_TARE_GRAMS, settings.defaultTareGrams)
        }
        _settingsFlow.value = settings
    }

    private fun loadSettingsFromDb(): TradeSettings {
        val farmerBps = queries.getSyncMetadataLong(KEY_FARMER_COMM_BPS).executeAsOneOrNull() ?: DEFAULT_FARMER_COMM_BPS
        val buyerBps = queries.getSyncMetadataLong(KEY_BUYER_COMM_BPS).executeAsOneOrNull() ?: DEFAULT_BUYER_COMM_BPS
        val labourPaisa = queries.getSyncMetadataLong(KEY_LABOUR_PAISA).executeAsOneOrNull() ?: DEFAULT_LABOUR_PAISA
        val tareGrams = queries.getSyncMetadataLong(KEY_TARE_GRAMS).executeAsOneOrNull() ?: DEFAULT_TARE_GRAMS
        return TradeSettings(
            farmerCommissionBps = farmerBps,
            buyerCommissionBps = buyerBps,
            defaultLabourPaisa = labourPaisa,
            defaultTareGrams = tareGrams
        )
    }

    companion object {
        const val KEY_FARMER_COMM_BPS = "trade_settings_farmer_comm_bps"
        const val KEY_BUYER_COMM_BPS = "trade_settings_buyer_comm_bps"
        const val KEY_LABOUR_PAISA = "trade_settings_labour_paisa"
        const val KEY_TARE_GRAMS = "trade_settings_tare_grams"

        const val DEFAULT_FARMER_COMM_BPS = 150L
        const val DEFAULT_BUYER_COMM_BPS = 150L
        const val DEFAULT_LABOUR_PAISA = 15000L
        const val DEFAULT_TARE_GRAMS = 35000L
    }
}
