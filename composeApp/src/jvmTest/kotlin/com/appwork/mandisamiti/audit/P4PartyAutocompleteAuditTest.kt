package com.appwork.mandisamiti.audit

import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.ui.components.TypedPartyCheck
import com.appwork.mandisamiti.ui.components.checkTypedParty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * X4 Audit Test Suite for Finding P4 (Party Name Validation & Anti-Silent Creation).
 * Verifies:
 * - Exact matches (Hindi and Latin, case-insensitive) automatically bind to existing party.
 * - Exact phone matches bind to existing party.
 * - Partial name matches (e.g. "TEST R" vs "TEST Ramesh") mandate explicit confirmation (NeedsConfirm),
 *   never silently creating a new party.
 * - Blank or whitespace names resolve to Blank.
 * - Unknown names require confirmation before creating a new account.
 */
class P4PartyAutocompleteAuditTest {

    private val rameshLatin = Party(
        id = "p-ramesh-latin",
        shopId = "shop-p4",
        name = "TEST Ramesh",
        phone = "9876543210",
        village = "Mathura",
        partyType = PartyType.FARMER,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private val rameshHindi = Party(
        id = "p-ramesh-hindi",
        shopId = "shop-p4",
        name = "रमेश कुमार",
        phone = "9876543211",
        village = "बरसाना",
        partyType = PartyType.FARMER,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private val testParties = listOf(rameshLatin, rameshHindi)

    @Test
    fun auditP4_exactMatchResolvesToExistingPartyWithoutDialog() {
        // Exact match Latin
        val resultLatin = checkTypedParty("TEST Ramesh", selectedId = null, parties = testParties)
        assertTrue(resultLatin is TypedPartyCheck.ExactMatch)
        assertEquals("p-ramesh-latin", (resultLatin as TypedPartyCheck.ExactMatch).party.id)

        // Exact match case-insensitive
        val resultLower = checkTypedParty("test ramesh", selectedId = null, parties = testParties)
        assertTrue(resultLower is TypedPartyCheck.ExactMatch)
        assertEquals("p-ramesh-latin", (resultLower as TypedPartyCheck.ExactMatch).party.id)

        // Exact match Hindi
        val resultHindi = checkTypedParty("रमेश कुमार", selectedId = null, parties = testParties)
        assertTrue(resultHindi is TypedPartyCheck.ExactMatch)
        assertEquals("p-ramesh-hindi", (resultHindi as TypedPartyCheck.ExactMatch).party.id)
    }

    @Test
    fun auditP4_exactPhoneMatchResolvesToExistingParty() {
        val resultPhone = checkTypedParty("9876543210", selectedId = null, parties = testParties)
        assertTrue(resultPhone is TypedPartyCheck.ExactMatch)
        assertEquals("p-ramesh-latin", (resultPhone as TypedPartyCheck.ExactMatch).party.id)
    }

    @Test
    fun auditP4_partialNameNeverSilentlyCreatesPartyAndMandatesConfirmation() {
        // Review reproduction case: "TEST R" typed without picking "TEST Ramesh"
        val result = checkTypedParty("TEST R", selectedId = null, parties = testParties)

        // MUST be NeedsConfirm, not auto-create
        assertTrue(result is TypedPartyCheck.NeedsConfirm, "Partial match must require confirmation")
        val confirm = result as TypedPartyCheck.NeedsConfirm
        assertEquals("TEST R", confirm.typed)
        assertTrue(confirm.similar.any { it.name == "TEST Ramesh" }, "Similar list must suggest TEST Ramesh")

        // Hindi partial match: "रमेश"
        val resultHi = checkTypedParty("रमेश", selectedId = null, parties = testParties)
        assertTrue(resultHi is TypedPartyCheck.NeedsConfirm)
        val confirmHi = resultHi as TypedPartyCheck.NeedsConfirm
        assertEquals("रमेश", confirmHi.typed)
        assertTrue(confirmHi.similar.any { it.name == "रमेश कुमार" })
    }

    @Test
    fun auditP4_blankOrWhitespaceResolvesToBlank() {
        val blank1 = checkTypedParty("", selectedId = null, parties = testParties)
        assertEquals(TypedPartyCheck.Blank, blank1)

        val blank2 = checkTypedParty("    ", selectedId = null, parties = testParties)
        assertEquals(TypedPartyCheck.Blank, blank2)
    }

    @Test
    fun auditP4_newUniqueNameRequiresConfirmationBeforeCreation() {
        val result = checkTypedParty("TEST नया किसान", selectedId = null, parties = testParties)
        assertTrue(result is TypedPartyCheck.NeedsConfirm)
        val confirm = result as TypedPartyCheck.NeedsConfirm
        assertEquals("TEST नया किसान", confirm.typed)
        assertTrue(confirm.similar.isEmpty(), "No similar parties for unique name")
    }
}
