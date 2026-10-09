package com.appwork.mandisamiti.ui.ledger

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PartyPhoneTest {
    @Test
    fun inputKeepsDigitsOnlyAndAtMostTen() {
        assertEquals("9876543210", sanitizePartyPhone("98765-43210"))
        assertEquals("9876543210", sanitizePartyPhone("987654321012345"))
        assertEquals("", sanitizePartyPhone("abc"))
    }

    @Test
    fun emptyPhoneIsAllowedOtherwiseTenDigitsStartingSixToNine() {
        assertTrue(isValidPartyPhone(""))
        assertTrue(isValidPartyPhone("9876543210"))
        assertTrue(isValidPartyPhone("6000000000"))
        assertFalse(isValidPartyPhone("987654321"))
        assertFalse(isValidPartyPhone("5876543210"))
        assertFalse(isValidPartyPhone("98765432101"))
        assertFalse(isValidPartyPhone("98765 4321"))
    }
}
