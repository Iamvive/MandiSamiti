package com.appwork.mandisamiti.audit

import com.appwork.mandisamiti.data.repository.OfflineFirstBuyRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import com.appwork.mandisamiti.ui.buy.BuyEntryFormState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Characterizes the money behaviour that existing pilot slips were saved with.
 *
 * Expected values are hand-checked review examples. Keeping them as literals makes these tests
 * fail if new-slip arithmetic later starts reinterpreting historical amounts.
 */
class BaselineMoneyBehaviourAuditTest {

    @Test
    fun reviewBuyExampleKeepsTheTareReductionVisibleInCommodityAmount() {
        val beforeTare = reviewBuyForm(tareQuintals = "0")
        val afterTare = reviewBuyForm(tareQuintals = "0.35")

        assertEquals(2_400_000L, beforeTare.aggregatePreview().totalGrossPaisa)
        assertEquals(2_316_000L, afterTare.aggregatePreview().totalGrossPaisa)
        assertEquals(
            84_000L,
            beforeTare.aggregatePreview().totalGrossPaisa - afterTare.aggregatePreview().totalGrossPaisa,
        )
        assertEquals(965_000L, afterTare.aggregatePreview().totalWeightGrams)
    }

    @Test
    fun commissionRoundsToNearestTenRupeesInTheReviewBuyExample() {
        val preview = reviewBuyForm().aggregatePreview()

        assertEquals(16_000L, preview.commissionPaisa)
        assertEquals(2_280_000L, preview.netPayablePaisa)
    }

    @Test
    fun commissionAboveFiveRupeeMidpointRoundsUpToNextTenRupees() {
        val preview = reviewBuyForm(commissionPercent = "0.72").aggregatePreview()

        assertEquals(17_000L, preview.commissionPaisa)
        assertEquals(2_279_000L, preview.netPayablePaisa)
    }

    @Test
    fun labourChangesPayableByExactlyRatePerBagTimesBagCount() {
        val withoutLabour = reviewBuyForm(labourPerBag = "0").aggregatePreview()
        val withLabour = reviewBuyForm(labourPerBag = "10").aggregatePreview()

        assertEquals(20_000L, withLabour.labourPaisa)
        assertEquals(20_000L, withoutLabour.netPayablePaisa - withLabour.netPayablePaisa)
    }

    // Changed on purpose (Phase 3 C6, Vivek's sheet 2026-11-01): net is the commodity value, advance is tracked in paid.
    @Test
    fun advanceDoesNotChangeNetPayableAndReducesPayoutByExactlyTheAdvance() {
        val withoutAdvance = reviewBuyForm(advancePaid = "0").aggregatePreview()
        val withAdvance = reviewBuyForm(advancePaid = "1000").aggregatePreview()

        assertEquals(withoutAdvance.netPayablePaisa, withAdvance.netPayablePaisa)
        assertEquals(100_000L, withoutAdvance.payoutPaisa - withAdvance.payoutPaisa)
        assertEquals(2_280_000L, withAdvance.netPayablePaisa)
        assertEquals(2_180_000L, withAdvance.payoutPaisa)
    }

    @Test
    fun savedBuySlipReadsBackStoredTotalsAndHistoricalTareUnchanged() = runTest {
        val database = createTestDatabase()
        val repository = OfflineFirstBuyRepository(database)
        val entry = reviewBuyForm().toBuyEntry(shopId = "TEST-shop").copy(
            id = "TEST-saved-buy",
            farmerId = "TEST-farmer",
            createdAt = 1_000L,
            updatedAt = 1_000L,
        )

        repository.saveBuyEntry(entry)

        val saved = requireNotNull(repository.getBuyEntryById("TEST-saved-buy"))
        assertEquals(965_000L, saved.totalWeightGrams)
        assertEquals(2_316_000L, saved.grossPaisa)
        assertEquals(16_000L, saved.commissionPaisa)
        assertEquals(20_000L, saved.labourPaisa)
        assertEquals(100_000L, saved.advancePaidPaisa)
        assertEquals(2_280_000L, saved.netPayablePaisa)

        val restored = BuyEntryFormState().apply { populateFrom(saved) }
        assertEquals("0.35", restored.tareQuintals)
        assertEquals(2_280_000L, restored.aggregatePreview().netPayablePaisa)
    }

    @Test
    fun loanInterestUsesTheBaselineThirtyDayRuleAndReturnsIntegerPaisa() {
        val interest = RuralInterestEngine.calculateAccruedInterestByDays(
            principalPaisa = 500_000L,
            monthlyRatePercent = 1.5,
            elapsedDays = 30,
        )

        assertEquals(7_500L, interest.accruedInterestPaisa)
        assertEquals(507_500L, interest.totalPayablePaisa)
    }

    private fun reviewBuyForm(
        tareQuintals: String = "0.35",
        commissionPercent: String = "0.70",
        labourPerBag: String = "10",
        advancePaid: String = "1000",
    ) = BuyEntryFormState().apply {
        farmerName = "TEST farmer"
        bags = "20"
        weightPerBagKg = "50"
        ratePerQuintal = "2400"
        this.tareQuintals = tareQuintals
        this.commissionPercent = commissionPercent
        this.labourPerBag = labourPerBag
        this.advancePaid = advancePaid
    }
}
