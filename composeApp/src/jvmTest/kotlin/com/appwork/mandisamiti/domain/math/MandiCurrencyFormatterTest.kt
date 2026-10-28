package com.appwork.mandisamiti.domain.math

import kotlin.test.Test
import kotlin.test.assertEquals

class MandiCurrencyFormatterTest {

    @Test
    fun testCompactIndianCurrencyFormatting() {
        // 99,999 -> "₹99,999" (en/hi)
        assertEquals("₹99,999", MandiCurrencyFormatter.formatCompactRupees(99_999_00L, isEnglish = true))
        assertEquals("₹99,999", MandiCurrencyFormatter.formatCompactRupees(99_999_00L, isEnglish = false))

        // 1,00,000 -> "₹1.00 L" / "₹1.00 लाख"
        assertEquals("₹1.00 L", MandiCurrencyFormatter.formatCompactRupees(100_000_00L, isEnglish = true))
        assertEquals("₹1.00 लाख", MandiCurrencyFormatter.formatCompactRupees(100_000_00L, isEnglish = false))

        // 99,99,999 -> "₹99.99 L" / "₹99.99 लाख"
        assertEquals("₹99.99 L", MandiCurrencyFormatter.formatCompactRupees(9_999_999_00L, isEnglish = true))
        assertEquals("₹99.99 लाख", MandiCurrencyFormatter.formatCompactRupees(9_999_999_00L, isEnglish = false))

        // 1,00,00,000 -> "₹1.00 Cr" / "₹1.00 करोड़"
        assertEquals("₹1.00 Cr", MandiCurrencyFormatter.formatCompactRupees(10_000_000_00L, isEnglish = true))
        assertEquals("₹1.00 करोड़", MandiCurrencyFormatter.formatCompactRupees(10_000_000_00L, isEnglish = false))

        // 12,27,31,344 -> "₹12.27 Cr" / "₹12.27 करोड़"
        assertEquals("₹12.27 Cr", MandiCurrencyFormatter.formatCompactRupees(122_731_344_00L, isEnglish = true))
        assertEquals("₹12.27 करोड़", MandiCurrencyFormatter.formatCompactRupees(122_731_344_00L, isEnglish = false))
    }

    @Test
    fun testNegativeCurrencyFormattingNeverProducesRupeeBeforeMinus() {
        // Zero
        assertEquals("₹0", MandiCurrencyFormatter.formatRupees(0L))

        // Standard positive
        assertEquals("₹11,500", MandiCurrencyFormatter.formatRupees(11_500_00L))

        // Standard negative: must be -₹11,500, NEVER ₹-11,500
        assertEquals("-₹11,500", MandiCurrencyFormatter.formatRupees(-11_500_00L))

        // Negative with paisa
        assertEquals("-₹11,500.50", MandiCurrencyFormatter.formatRupees(-11_500_50L))

        // Large negative amounts
        assertEquals("-₹1,25,000", MandiCurrencyFormatter.formatRupees(-125_000_00L))

        // Compact negative amounts
        assertEquals("-₹11,500", MandiCurrencyFormatter.formatCompactRupees(-11_500_00L, isEnglish = true))
        assertEquals("-₹1.00 L", MandiCurrencyFormatter.formatCompactRupees(-100_000_00L, isEnglish = true))
        assertEquals("-₹1.00 लाख", MandiCurrencyFormatter.formatCompactRupees(-100_000_00L, isEnglish = false))
        assertEquals("-₹1.00 Cr", MandiCurrencyFormatter.formatCompactRupees(-10_000_000_00L, isEnglish = true))
    }
}
