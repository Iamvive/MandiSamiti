package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.math.DeductionsInput
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MandiMathEngineTest {

    @Test
    fun testStandardSettlementCalculation() {
        val grossWeightGrams = 1_840_000L
        val cutWeightGrams = 35_000L
        val ratePaisaPerQuintal = 542_000L

        val deductions = DeductionsInput(
            farmerCommissionPaisa = 244_578L,
            buyerCommissionPaisa = 146_747L,
            labourChargePaisa = 17_500L,
            weighingChargePaisa = 7_000L,
            otherDeductionsPaisa = 5_000L
        )

        val result = MandiMathEngine.calculateSettlement(
            grossWeightGrams = grossWeightGrams,
            cutWeightGrams = cutWeightGrams,
            ratePaisaPerQuintal = ratePaisaPerQuintal,
            deductions = deductions
        )

        assertEquals(1_805_000L, result.netWeightGrams)
        assertEquals(9_783_100L, result.grossAmountPaisa)

        val expectedFarmerDeductions = 244_578L + 17_500L + 7_000L + 5_000L
        val expectedFarmerPayable = 9_783_100L - expectedFarmerDeductions
        assertEquals(expectedFarmerPayable, result.netFarmerPayablePaisa)

        val expectedBuyerReceivable = 9_783_100L + 146_747L
        assertEquals(expectedBuyerReceivable, result.netBuyerReceivablePaisa)
    }

    @Test
    fun testGramsToQuintalsDisplayConversion() {
        val quintals = MandiMathEngine.gramsToQuintals(1_840_000L)
        assertTrue(abs(quintals - 18.40) < 0.001)

        val grams = MandiMathEngine.quintalsToGrams(18.40)
        assertEquals(1_840_000L, grams)
    }

    @Test
    fun testPaisaToRupeesFormatting() {
        val rupees = MandiMathEngine.paisaToRupeesString(9_509_022L)
        assertEquals("95,090.22", rupees)
    }
}
