package com.appwork.mandisamiti.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.ShopProfileEntity
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.repository.ShopProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class OfflineFirstShopProfileRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ShopProfileRepository {

    private val queries = database.appDatabaseQueries

    override fun getShopProfileStream(): Flow<ShopProfile?> {
        return queries.getShopProfile()
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toDomain() }
    }

    override suspend fun saveShopProfile(profile: ShopProfile) = withContext(ioDispatcher) {
        queries.insertShopProfile(
            id = profile.id,
            shop_name = profile.shopName,
            owner_name = profile.ownerName,
            mandi_name = profile.mandiName,
            shop_number = profile.shopNumber,
            phone_number = profile.phoneNumber,
            pin_hash = profile.pinHash,
            default_monthly_interest_rate = profile.defaultMonthlyInterestRate,
            is_sound_enabled = if (profile.isSoundEnabled) 1L else 0L,
            created_at = profile.createdAt,
            updated_at = profile.updatedAt,
            sync_status = profile.syncStatus.toLong()
        )
    }

    override suspend fun updateSoundSetting(shopId: String, isSoundEnabled: Boolean) = withContext(ioDispatcher) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        queries.updateSoundSetting(
            is_sound_enabled = if (isSoundEnabled) 1L else 0L,
            updated_at = now,
            id = shopId
        )
    }

    private fun ShopProfileEntity.toDomain(): ShopProfile {
        return ShopProfile(
            id = id,
            shopName = shop_name,
            ownerName = owner_name,
            mandiName = mandi_name,
            shopNumber = shop_number,
            phoneNumber = phone_number,
            pinHash = pin_hash,
            defaultMonthlyInterestRate = default_monthly_interest_rate,
            isSoundEnabled = is_sound_enabled == 1L,
            createdAt = created_at,
            updatedAt = updated_at,
            syncStatus = sync_status.toInt()
        )
    }
}
