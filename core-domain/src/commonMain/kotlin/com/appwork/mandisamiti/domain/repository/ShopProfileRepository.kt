package com.appwork.mandisamiti.domain.repository

import com.appwork.mandisamiti.domain.model.ShopProfile
import kotlinx.coroutines.flow.Flow

interface ShopProfileRepository {
    fun getShopProfileStream(): Flow<ShopProfile?>
    suspend fun saveShopProfile(profile: ShopProfile)
    suspend fun updateSoundSetting(shopId: String, isSoundEnabled: Boolean)
}
