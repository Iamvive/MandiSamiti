package com.appwork.mandisamiti.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.appwork.mandisamiti.data.auth.SessionStore
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.ShopQuotaEntity
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuotaResponseDto(
    @SerialName("plan_tier") val planTier: String = "FREE",
    @SerialName("bills_used_this_month") val billsUsedThisMonth: Long = 0L,
    @SerialName("monthly_limit") val monthlyLimit: Long = 50L,
    @SerialName("emergency_extensions_used") val emergencyExtensionsUsed: Long = 0L,
    @SerialName("emergency_extensions_max") val emergencyExtensionsMax: Long = 2L,
    @SerialName("emergency_bills_added") val emergencyBillsAdded: Long = 0L,
    @SerialName("effective_limit") val effectiveLimit: Long = 50L,
    @SerialName("can_create_bill") val canCreateBill: Boolean = true,
    @SerialName("is_emergency_extension_available") val isEmergencyExtensionAvailable: Boolean = true
)

data class ShopQuota(
    val shopId: String,
    val planTier: String,
    val monthlyLimit: Long,
    val emergencyExtensionsUsed: Long,
    val emergencyBillsAdded: Long,
    val billsCreatedThisMonth: Long,
    val lastSyncedAt: Long = 0L
) {
    val effectiveLimit: Long
        get() = monthlyLimit + emergencyBillsAdded

    val canCreateBill: Boolean
        get() = planTier == "PRO" || billsCreatedThisMonth < effectiveLimit

    val canClaimEmergencyExtension: Boolean
        get() = emergencyExtensionsUsed < 2L
}

class QuotaRepository(
    private val database: AppDatabase,
    private val sessionStore: SessionStore,
    private val httpClient: HttpClient? = null,
    private val baseUrl: String = "https://api.mandisamiti.appwork.co.in",
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    companion object {
        val INTERNAL_UNLIMITED_PHONES = setOf(
            "9058666306",
            "9058389891",
            "9997661012",
            "9410617691"
        )
    }

    private val queries = database.appDatabaseQueries

    private fun resolveShopId(shopId: String?): String {
        return shopId ?: sessionStore.current()?.shopId
            ?: throw IllegalStateException("Active shop ID required for quota tracking")
    }

    suspend fun canCreateBill(shopId: String? = null): Boolean = withContext(ioDispatcher) {
        val targetShopId = resolveShopId(shopId)
        val entity = queries.getShopQuota(targetShopId).executeAsOneOrNull()
        if (entity != null) {
            entity.toDomain().canCreateBill
        } else {
            true // default FREE tier with 0 bills allows creation
        }
    }

    suspend fun canClaimEmergencyExtension(shopId: String? = null): Boolean = withContext(ioDispatcher) {
        val targetShopId = resolveShopId(shopId)
        val entity = queries.getShopQuota(targetShopId).executeAsOneOrNull()
        if (entity != null) {
            entity.toDomain().canClaimEmergencyExtension
        } else {
            true // default 0 extensions used
        }
    }

    fun getQuotaStatus(shopId: String? = null): Flow<ShopQuota> {
        val targetShopId = resolveShopId(shopId)
        return queries.getShopQuota(targetShopId)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { entity ->
                if (entity != null) {
                    entity.toDomain()
                } else {
                    ShopQuota(
                        shopId = targetShopId,
                        planTier = "FREE",
                        monthlyLimit = 50L,
                        emergencyExtensionsUsed = 0L,
                        emergencyBillsAdded = 0L,
                        billsCreatedThisMonth = 0L,
                        lastSyncedAt = 0L
                    )
                }
            }
    }

    suspend fun incrementLocalBillCount(shopId: String? = null) = withContext(ioDispatcher) {
        val targetShopId = resolveShopId(shopId)
        database.transaction {
            val existing = queries.getShopQuota(targetShopId).executeAsOneOrNull()
            if (existing == null) {
                queries.upsertShopQuota(
                    shop_id = targetShopId,
                    plan_tier = "FREE",
                    monthly_limit = 50L,
                    emergency_extensions_used = 0L,
                    emergency_bills_added = 0L,
                    bills_created_this_month = 1L,
                    last_synced_at = Clock.System.now().toEpochMilliseconds()
                )
            } else {
                queries.incrementBillCount(targetShopId)
            }
        }
    }

    suspend fun syncQuotaWithBackend(shopId: String? = null): Result<ShopQuota> = withContext(ioDispatcher) {
        val client = httpClient ?: return@withContext Result.failure(IllegalStateException("No HttpClient configured"))
        val currentSession = sessionStore.current()
        val token = currentSession?.accessToken
        val targetShopId = resolveShopId(shopId)

        try {
            val response = client.get("$baseUrl/api/v1/billing/quota") {
                contentType(ContentType.Application.Json)
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
            if (response.status == HttpStatusCode.OK) {
                val dto = response.body<QuotaResponseDto>()
                val now = Clock.System.now().toEpochMilliseconds()
                queries.upsertShopQuota(
                    shop_id = targetShopId,
                    plan_tier = dto.planTier,
                    monthly_limit = dto.monthlyLimit,
                    emergency_extensions_used = dto.emergencyExtensionsUsed,
                    emergency_bills_added = dto.emergencyBillsAdded,
                    bills_created_this_month = dto.billsUsedThisMonth,
                    last_synced_at = now
                )
                val updated = ShopQuota(
                    shopId = targetShopId,
                    planTier = dto.planTier,
                    monthlyLimit = dto.monthlyLimit,
                    emergencyExtensionsUsed = dto.emergencyExtensionsUsed,
                    emergencyBillsAdded = dto.emergencyBillsAdded,
                    billsCreatedThisMonth = dto.billsUsedThisMonth,
                    lastSyncedAt = now
                )
                Result.success(updated)
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(RuntimeException("Sync quota failed with ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun claimEmergencyExtension(shopId: String? = null): Result<ShopQuota> = withContext(ioDispatcher) {
        val client = httpClient ?: return@withContext Result.failure(IllegalStateException("No HttpClient configured"))
        val currentSession = sessionStore.current()
        val token = currentSession?.accessToken
        val targetShopId = resolveShopId(shopId)

        try {
            val response = client.post("$baseUrl/api/v1/billing/claim-emergency-extension") {
                contentType(ContentType.Application.Json)
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
            if (response.status == HttpStatusCode.OK) {
                val dto = response.body<QuotaResponseDto>()
                val now = Clock.System.now().toEpochMilliseconds()
                queries.upsertShopQuota(
                    shop_id = targetShopId,
                    plan_tier = dto.planTier,
                    monthly_limit = dto.monthlyLimit,
                    emergency_extensions_used = dto.emergencyExtensionsUsed,
                    emergency_bills_added = dto.emergencyBillsAdded,
                    bills_created_this_month = dto.billsUsedThisMonth,
                    last_synced_at = now
                )
                val updated = ShopQuota(
                    shopId = targetShopId,
                    planTier = dto.planTier,
                    monthlyLimit = dto.monthlyLimit,
                    emergencyExtensionsUsed = dto.emergencyExtensionsUsed,
                    emergencyBillsAdded = dto.emergencyBillsAdded,
                    billsCreatedThisMonth = dto.billsUsedThisMonth,
                    lastSyncedAt = now
                )
                Result.success(updated)
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(RuntimeException("Claim extension failed with ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun setMockQuota(
        shopId: String,
        monthlyLimit: Long = 50L,
        used: Long = 0L,
        emergencyExtensionsUsed: Long = 0L,
        emergencyBillsAdded: Long = 0L,
        planTier: String = "FREE"
    ) = withContext(ioDispatcher) {
        queries.upsertShopQuota(
            shop_id = shopId,
            plan_tier = planTier,
            monthly_limit = monthlyLimit,
            emergency_extensions_used = emergencyExtensionsUsed,
            emergency_bills_added = emergencyBillsAdded,
            bills_created_this_month = used,
            last_synced_at = Clock.System.now().toEpochMilliseconds()
        )
    }

    private fun isInternalUnlimitedUser(): Boolean {
        val profile = queries.getShopProfile().executeAsOneOrNull()
        val phone = (profile?.phone_number ?: "")
            .replace("+91", "")
            .replace(" ", "")
            .trim()
        return phone in INTERNAL_UNLIMITED_PHONES
    }

    private fun ShopQuotaEntity.toDomain(): ShopQuota {
        val isVip = isInternalUnlimitedUser()
        val effectiveTier = if (isVip) "PRO" else plan_tier
        return ShopQuota(
            shopId = shop_id,
            planTier = effectiveTier,
            monthlyLimit = monthly_limit,
            emergencyExtensionsUsed = emergency_extensions_used,
            emergencyBillsAdded = emergency_bills_added,
            billsCreatedThisMonth = bills_created_this_month,
            lastSyncedAt = last_synced_at
        )
    }
}
