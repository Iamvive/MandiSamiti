package com.appwork.mandisamiti.domain.repository

import com.appwork.mandisamiti.domain.model.StockAudit

interface StockAuditRepository {
    suspend fun loadAudits(shopId: String): Map<String, StockAudit>
    suspend fun saveAudit(shopId: String, audit: StockAudit)
}

