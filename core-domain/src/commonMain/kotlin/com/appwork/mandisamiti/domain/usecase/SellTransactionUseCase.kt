package com.appwork.mandisamiti.domain.usecase

import com.appwork.mandisamiti.domain.math.*
import com.appwork.mandisamiti.domain.model.*
import com.appwork.mandisamiti.domain.repository.SellRepository

data class SellForm(
    val shopId: String, val buyerId: String, val commodityId: String,
    val billMode: BillMode, val partyKind: SellPartyKind, val gstin: String? = null,
    val bags: Long, val weightPerBagGrams: Long, val ratePaisaPerQuintal: Long,
    val aratBps: Long, val tulakPaisaOverride: Long? = null, val receivedPaisa: Long,
    val vehicleNumber: String? = null, val invoiceNumber: String? = null, val remarks: String? = null,
)

class SellTransactionUseCase(
    private val repo: SellRepository,
    private val newId: () -> String,
    private val nowMillis: () -> Long,
) {
    suspend fun create(form: SellForm): SellEntry {
        require(form.bags > 0) { "Bags are required" }
        require(form.ratePaisaPerQuintal > 0) { "Rate is required" }
        val m = BuySellMath.calculateSell(
            SellMathInput(form.bags, form.weightPerBagGrams, form.ratePaisaPerQuintal, form.billMode,
                form.aratBps, form.tulakPaisaOverride, form.receivedPaisa)
        )
        val now = nowMillis()
        val entry = SellEntry(
            id = newId(), shopId = form.shopId, buyerId = form.buyerId, commodityId = form.commodityId,
            entryDate = now, billMode = form.billMode, partyKind = form.partyKind, gstin = form.gstin,
            bagsCount = form.bags, weightPerBagGrams = form.weightPerBagGrams,
            totalWeightGrams = m.totalWeightGrams, ratePaisaPerQuintal = form.ratePaisaPerQuintal,
            aratBps = form.aratBps, tulakPaisa = m.tulakPaisa, grossPaisa = m.grossPaisa,
            aratPaisa = m.aratPaisa, mandiShulkPaisa = m.mandiShulkPaisa,
            totalReceivablePaisa = m.totalReceivablePaisa, receivedPaisa = form.receivedPaisa,
            balanceDuePaisa = m.balanceDuePaisa, vehicleNumber = form.vehicleNumber,
            invoiceNumber = form.invoiceNumber, remarks = form.remarks, createdAt = now, updatedAt = now,
        )
        repo.saveSellEntry(entry)
        return entry
    }
}
