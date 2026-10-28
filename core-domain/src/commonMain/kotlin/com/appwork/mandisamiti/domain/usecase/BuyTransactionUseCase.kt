package com.appwork.mandisamiti.domain.usecase

import com.appwork.mandisamiti.domain.math.*
import com.appwork.mandisamiti.domain.model.*
import com.appwork.mandisamiti.domain.repository.BuyRepository

data class BuyForm(
    val shopId: String, val farmerId: String, val commodityId: String,
    val bags: Long, val weightPerBagGrams: Long, val extraWeightGrams: Long,
    val ratePaisaPerQuintal: Long, val commissionBps: Long,
    val labourPerBagPaisa: Long, val advancePaidPaisa: Long,
    val vehicleNumber: String? = null, val slipNumber: String? = null, val remarks: String? = null,
)

class BuyTransactionUseCase(
    private val repo: BuyRepository,
    private val newId: () -> String,
    private val nowMillis: () -> Long,
) {
    suspend fun create(form: BuyForm): BuyEntry {
        require(form.bags > 0 || form.extraWeightGrams > 0) { "Weight is required" }
        require(form.ratePaisaPerQuintal > 0) { "Rate is required" }
        val m = BuySellMath.calculateBuy(
            BuyMathInput(form.bags, form.weightPerBagGrams, form.extraWeightGrams, form.ratePaisaPerQuintal,
                form.commissionBps, form.labourPerBagPaisa, form.advancePaidPaisa)
        )
        val now = nowMillis()
        val entry = BuyEntry(
            id = newId(), shopId = form.shopId, farmerId = form.farmerId, commodityId = form.commodityId,
            entryDate = now, bagsCount = form.bags, weightPerBagGrams = form.weightPerBagGrams,
            extraWeightGrams = form.extraWeightGrams, totalWeightGrams = m.totalWeightGrams,
            ratePaisaPerQuintal = form.ratePaisaPerQuintal, commissionBps = form.commissionBps,
            labourPerBagPaisa = form.labourPerBagPaisa, advancePaidPaisa = form.advancePaidPaisa,
            grossPaisa = m.grossPaisa, commissionPaisa = m.commissionPaisa, labourPaisa = m.labourPaisa,
            netPayablePaisa = m.netPayablePaisa, vehicleNumber = form.vehicleNumber,
            slipNumber = form.slipNumber, remarks = form.remarks, createdAt = now, updatedAt = now,
        )
        repo.saveBuyEntry(entry)
        return entry
    }

    suspend fun recordPayment(id: String, paidPaisa: Long) {
        val current = repo.getBuyEntryById(id) ?: throw IllegalArgumentException("Buy entry $id does not exist")
        require(paidPaisa >= 0) { "Payment cannot be negative" }
        repo.editBuyEntry(current.copy(paidPaisa = paidPaisa))
    }
}
