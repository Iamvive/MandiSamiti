package com.appwork.mandisamiti.domain.repository

import kotlinx.serialization.Serializable

@Serializable
data class DrawerCount(
    val countedPaisa: Long,
    val countedAt: Long,
    val cashFingerprint: Long,
    val closedAt: Long? = null,
    val reason: String? = null,
)

/** Device-local physical observations, scoped to a shop and calendar date. */
interface DrawerCountRepository {
    suspend fun load(shopId: String, date: String): DrawerCount?
    suspend fun save(shopId: String, date: String, count: DrawerCount?)
}
