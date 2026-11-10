package com.appwork.mandisamiti.ui.home

import com.appwork.mandisamiti.data.repository.ShopQuota
import com.appwork.mandisamiti.ui.components.shouldShowQuotaWarning
import com.appwork.mandisamiti.ui.components.calculateQuotaProgressFraction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuotaBannerTest {

    @Test
    fun testVisibility_notVisibleUnder80Percent() {
        val quota = ShopQuota(
            shopId = "shop_1",
            planTier = "FREE",
            monthlyLimit = 50L,
            emergencyExtensionsUsed = 0L,
            emergencyBillsAdded = 0L,
            billsCreatedThisMonth = 39L
        )
        assertFalse(shouldShowQuotaWarning(quota))
    }

    @Test
    fun testVisibility_visibleAtExactly80Percent() {
        val quota = ShopQuota(
            shopId = "shop_1",
            planTier = "FREE",
            monthlyLimit = 50L,
            emergencyExtensionsUsed = 0L,
            emergencyBillsAdded = 0L,
            billsCreatedThisMonth = 40L
        )
        assertTrue(shouldShowQuotaWarning(quota))
    }

    @Test
    fun testVisibility_visibleAbove80Percent() {
        val quota = ShopQuota(
            shopId = "shop_1",
            planTier = "FREE",
            monthlyLimit = 50L,
            emergencyExtensionsUsed = 0L,
            emergencyBillsAdded = 0L,
            billsCreatedThisMonth = 45L
        )
        assertTrue(shouldShowQuotaWarning(quota))
    }

    @Test
    fun testVisibility_visibleWhenLimitExceeded() {
        val quota = ShopQuota(
            shopId = "shop_1",
            planTier = "FREE",
            monthlyLimit = 50L,
            emergencyExtensionsUsed = 0L,
            emergencyBillsAdded = 0L,
            billsCreatedThisMonth = 55L
        )
        assertTrue(shouldShowQuotaWarning(quota))
    }

    @Test
    fun testVisibility_neverVisibleForProTier() {
        val quota = ShopQuota(
            shopId = "shop_1",
            planTier = "PRO",
            monthlyLimit = 50L,
            emergencyExtensionsUsed = 0L,
            emergencyBillsAdded = 0L,
            billsCreatedThisMonth = 100L
        )
        assertFalse(shouldShowQuotaWarning(quota))
    }

    @Test
    fun testVisibility_effectiveLimitConsidersEmergencyBillsAdded() {
        // Base limit 50 + 10 emergency = 60. 80% is 48.
        val quota47 = ShopQuota(
            shopId = "shop_1",
            planTier = "FREE",
            monthlyLimit = 50L,
            emergencyExtensionsUsed = 1L,
            emergencyBillsAdded = 10L,
            billsCreatedThisMonth = 47L
        )
        assertFalse(shouldShowQuotaWarning(quota47))

        val quota48 = ShopQuota(
            shopId = "shop_1",
            planTier = "FREE",
            monthlyLimit = 50L,
            emergencyExtensionsUsed = 1L,
            emergencyBillsAdded = 10L,
            billsCreatedThisMonth = 48L
        )
        assertTrue(shouldShowQuotaWarning(quota48))
    }

    @Test
    fun testProgressFractionCalculations() {
        val quotaZero = ShopQuota("shop_1", "FREE", 50L, 0L, 0L, 0L)
        assertEquals(0f, calculateQuotaProgressFraction(quotaZero))

        val quotaHalf = ShopQuota("shop_1", "FREE", 50L, 0L, 0L, 25L)
        assertEquals(0.5f, calculateQuotaProgressFraction(quotaHalf))

        val quotaEighty = ShopQuota("shop_1", "FREE", 50L, 0L, 0L, 40L)
        assertEquals(0.8f, calculateQuotaProgressFraction(quotaEighty))

        val quotaFull = ShopQuota("shop_1", "FREE", 50L, 0L, 0L, 50L)
        assertEquals(1f, calculateQuotaProgressFraction(quotaFull))

        val quotaOverflow = ShopQuota("shop_1", "FREE", 50L, 0L, 0L, 65L)
        assertEquals(1f, calculateQuotaProgressFraction(quotaOverflow))
    }

    @Test
    fun testProgressFraction_zeroEffectiveLimitHandledGracefully() {
        val zeroLimitQuota = ShopQuota("shop_1", "FREE", 0L, 0L, 0L, 0L)
        assertEquals(1f, calculateQuotaProgressFraction(zeroLimitQuota))
    }
}
