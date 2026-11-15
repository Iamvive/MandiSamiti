package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.math.sellSlipAmounts
import com.appwork.mandisamiti.domain.model.BillMode
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.SellPartyKind
import kotlin.test.Test
import kotlin.test.assertEquals

class SellSlipAmountsTest {

    private fun entry(
        mode: BillMode,
        gross: Long,
        commission: Long,
        shulk: Long,
        tulak: Long,
        total: Long,
    ) = SellEntry(
        id = "s1", shopId = "shop", buyerId = "b", commodityId = "गेहूं", entryDate = 0L,
        billMode = mode, partyKind = SellPartyKind.MERCHANT,
        bagsCount = 10, weightPerBagGrams = 50_000, totalWeightGrams = 521_000, ratePaisaPerQuintal = 200_000,
        aratBps = 100, tulakPaisa = tulak, grossPaisa = gross, aratPaisa = commission, mandiShulkPaisa = shulk,
        totalReceivablePaisa = total, receivedPaisa = 0, balanceDuePaisa = total,
        createdAt = 0L, updatedAt = 0L,
    )

    @Test
    fun cashMemoWithTraderLabourSplitsCommissionTulakAndLabour() {
        // gross 10,420 Rs, commission 104.20, tulak 5.21, labour 25.59 (includes rounding to whole rupee)
        val e = entry(BillMode.NORMAL, gross = 1_042_000, commission = 10_420, shulk = 0, tulak = 521, total = 1_055_500)
        val s = sellSlipAmounts(e)
        assertEquals(10_420, s.commissionPaisa)
        assertEquals(521, s.tulakPaisa)
        assertEquals(2_559, s.labourPaisa)
        assertEquals(e.totalReceivablePaisa, e.grossPaisa + s.commissionPaisa + s.tulakPaisa + s.labourPaisa)
    }

    @Test
    fun oldCashMemoWithoutLabourHasNoLabourRow() {
        // tulak field holds total minus gross minus commission (no per-quintal labour)
        val e = entry(BillMode.NORMAL, gross = 1_042_000, commission = 10_420, shulk = 0, tulak = 1_055_500 - 1_042_000 - 10_420, total = 1_055_500)
        val s = sellSlipAmounts(e)
        assertEquals(0, s.labourPaisa)
        assertEquals(e.totalReceivablePaisa, e.grossPaisa + s.commissionPaisa + s.tulakPaisa + s.labourPaisa)
    }

    @Test
    fun billOfSupplyShowsCommissionSeparatelyFromLabour() {
        // Bill of Supply without labour: commission used to be hidden inside the labour row
        val e = entry(BillMode.BILL_OF_SUPPLY, gross = 1_042_000, commission = 10_420, shulk = 15_630, tulak = 521, total = 1_068_571)
        val s = sellSlipAmounts(e)
        assertEquals(10_420, s.commissionPaisa)
        assertEquals(15_630, s.mandiShulkPaisa)
        assertEquals(0, s.labourPaisa)
    }

    @Test
    fun billOfSupplyWithLabourKeepsLabourSeparate() {
        val e = entry(BillMode.BILL_OF_SUPPLY, gross = 1_042_000, commission = 10_420, shulk = 15_630, tulak = 521, total = 1_068_571 + 500)
        val s = sellSlipAmounts(e)
        assertEquals(10_420, s.commissionPaisa)
        assertEquals(500, s.labourPaisa)
        assertEquals(e.totalReceivablePaisa, e.grossPaisa + s.commissionPaisa + s.mandiShulkPaisa + s.tulakPaisa + s.labourPaisa)
    }
}
