package com.appwork.mandisamiti.domain.usecase

import com.appwork.mandisamiti.domain.model.*
import com.appwork.mandisamiti.domain.repository.LendRepository

data class LendForm(
    val shopId: String, val partyId: String, val lendType: LendType,
    val amountPaisa: Long = 0L, val commodityId: String? = null, val bags: Long = 0L, val weightGrams: Long = 0L,
    val expectedReturnDate: Long? = null, val notes: String? = null,
)

class LendTransactionUseCase(
    private val repo: LendRepository,
    private val newId: () -> String,
    private val nowMillis: () -> Long,
) {
    suspend fun lend(form: LendForm): LendEntry {
        when (form.lendType) {
            LendType.MONEY -> require(form.amountPaisa > 0) { "Amount is required" }
            LendType.GRAIN -> require(form.weightGrams > 0 && form.commodityId != null) { "Grain and weight are required" }
        }
        val now = nowMillis()
        val entry = LendEntry(
            id = newId(), shopId = form.shopId, partyId = form.partyId, lendType = form.lendType, entryDate = now,
            amountPaisa = form.amountPaisa, commodityId = form.commodityId, bagsCount = form.bags,
            weightGrams = form.weightGrams, expectedReturnDate = form.expectedReturnDate, notes = form.notes,
            createdAt = now, updatedAt = now,
        )
        repo.saveLendEntry(entry)
        return entry
    }

    suspend fun repay(lendId: String, amountPaisa: Long, weightGrams: Long, bags: Long): LendSettlement {
        val lend = repo.getLendEntryById(lendId) ?: throw IllegalArgumentException("Lend $lendId does not exist")
        require(!lend.isVoid) { "Lend $lendId is void" }
        require(amountPaisa > 0 || weightGrams > 0) { "Nothing to settle" }
        val now = nowMillis()
        val s = LendSettlement(newId(), lend.shopId, lendId, now, amountPaisa, weightGrams, bags, now, now)
        repo.settle(s)
        return s
    }
}
