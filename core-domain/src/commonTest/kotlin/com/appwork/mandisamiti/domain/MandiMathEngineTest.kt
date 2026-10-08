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

    @Test
    fun parseRupeesToPaisaKeepsPaisa() {
        assertEquals(227_550L, MandiMathEngine.parseRupeesToPaisa("2275.50"))
        assertEquals(227_550L, MandiMathEngine.parseRupeesToPaisa("2275.5"))
        assertEquals(227_500L, MandiMathEngine.parseRupeesToPaisa("2275"))
        assertEquals(5L, MandiMathEngine.parseRupeesToPaisa("0.05"))
        assertEquals(0L, MandiMathEngine.parseRupeesToPaisa(""))
        assertEquals(0L, MandiMathEngine.parseRupeesToPaisa("12.3.4"))
        assertEquals(0L, MandiMathEngine.parseRupeesToPaisa("abc"))
    }

    @Test
    fun paisaToInputStringRoundTrips() {
        assertEquals("2275.50", MandiMathEngine.paisaToInputString(227_550L))
        assertEquals("2275", MandiMathEngine.paisaToInputString(227_500L))
        assertEquals("0.05", MandiMathEngine.paisaToInputString(5L))
        listOf(0L, 1L, 5L, 99L, 227_500L, 227_550L).forEach { paisa ->
            assertEquals(paisa, MandiMathEngine.parseRupeesToPaisa(MandiMathEngine.paisaToInputString(paisa)))
        }
    }

    @Test
    fun percentIsParsedToBasisPoints() {
        assertEquals(150L, MandiMathEngine.parsePercentToBasisPoints("1.5"))
        assertEquals(200L, MandiMathEngine.parsePercentToBasisPoints("2"))
        assertEquals(25L, MandiMathEngine.parsePercentToBasisPoints("0.25"))
        assertEquals("1.50", MandiMathEngine.paisaToInputString(150L))
    }

    @Test
    fun gramsToQuintalsInputStringIsExact() {
        assertEquals("18.4", MandiMathEngine.gramsToQuintalsInputString(1_840_000L))
        assertEquals("0.35", MandiMathEngine.gramsToQuintalsInputString(35_000L))
        assertEquals("18.05123", MandiMathEngine.gramsToQuintalsInputString(1_805_123L))
        assertEquals("0", MandiMathEngine.gramsToQuintalsInputString(0L))
        listOf(1_840_000L, 35_000L, 1_805_123L).forEach { grams ->
            assertEquals(grams, MandiMathEngine.parseQuintalsStringToGrams(MandiMathEngine.gramsToQuintalsInputString(grams)))
        }
    }

    @Test
    fun commissionRoundsHalfUp() {
        // ₹97,831 x 1.5% = ₹1,467.465 -> 146_747 paisa (old Double code truncated to 146_746)
        assertEquals(146_747L, MandiMathEngine.percentageOf(9_783_100L, 150L))
        assertEquals(0L, MandiMathEngine.percentageOf(9_783_100L, 0L))
    }

    @Test
    fun grossAmountRoundsHalfUp() {
        // 1 kg at ₹2,500.50/qtl = ₹25.005 -> 2_501 paisa (old code floored to 2_500)
        assertEquals(2_501L, MandiMathEngine.grossAmountPaisa(1_000L, 250_050L))
        assertEquals(9_783_100L, MandiMathEngine.grossAmountPaisa(1_805_000L, 542_000L))
    }

    @Test
    fun farmerPayableGoesNegativeWhenDeductionsExceedGross() {
        val calc = MandiMathEngine.calculateSettlement(
            grossWeightGrams = 100_000L,          // 1 qtl
            cutWeightGrams = 0L,
            ratePaisaPerQuintal = 100_000L,       // ₹1,000/qtl
            deductions = DeductionsInput(labourChargePaisa = 150_000L) // ₹1,500
        )
        assertEquals(100_000L, calc.grossAmountPaisa)
        assertEquals(-50_000L, calc.netFarmerPayablePaisa) // farmer owes ₹500
    }
}
