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
}
