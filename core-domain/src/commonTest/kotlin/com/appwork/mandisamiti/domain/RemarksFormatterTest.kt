package com.appwork.mandisamiti.domain

import com.appwork.mandisamiti.domain.util.RemarksFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RemarksFormatterTest {
    @Test
    fun blankNoteLeavesNoTrailingColon() {
        assertEquals("उधार ऋण", RemarksFormatter.cleanForDisplay("उधार ऋण: [LEND:l1]"))
        assertEquals("उधार ऋण", RemarksFormatter.cleanForDisplay("उधार ऋण:"))
        assertEquals("उधार वापसी जमा", RemarksFormatter.cleanForDisplay("उधार वापसी जमा: [SETTLE:s1:l1]"))
    }

    @Test
    fun nonBlankNoteKeepsItsText() {
        assertEquals("उधार ऋण: खाद", RemarksFormatter.cleanForDisplay("उधार ऋण: खाद [LEND:l1]"))
    }

    @Test
    fun emptyInputsGiveNull() {
        assertNull(RemarksFormatter.cleanForDisplay(null))
        assertNull(RemarksFormatter.cleanForDisplay("  "))
        assertNull(RemarksFormatter.cleanForDisplay("[LEND:l1]"))
    }
}
