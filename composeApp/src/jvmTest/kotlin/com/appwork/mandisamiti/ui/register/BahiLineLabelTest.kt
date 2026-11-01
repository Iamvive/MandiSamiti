package com.appwork.mandisamiti.ui.register

import com.appwork.mandisamiti.domain.bahi.BahiDayLine
import com.appwork.mandisamiti.domain.bahi.BahiEntryKind
import com.appwork.mandisamiti.domain.bahi.BahiPaymentMode
import kotlin.test.Test
import kotlin.test.assertEquals

class BahiLineLabelTest {
    private fun line(label: String) = BahiDayLine("x", BahiEntryKind.EXPENSE, label, 1L, BahiPaymentMode.CASH)

    @Test fun hindiModeHasNoEnglishOrRawEnum() {
        assertEquals("खुला बाकी", bahiLineLabel(line("Opening balance"), false))
        assertEquals("खर्च · यूपीआई", bahiLineLabel(line("खर्च · UPI"), false))
        assertEquals("TEST Ramesh", bahiLineLabel(line("TEST Ramesh"), false)) // party names pass through
    }

    @Test fun englishModeKeepsLabel() {
        assertEquals("Opening balance", bahiLineLabel(line("Opening balance"), true))
    }
}
