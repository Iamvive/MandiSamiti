package com.appwork.mandisamiti.ui.udhaar

import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import com.appwork.mandisamiti.domain.model.LendDirection
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendKind
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UdhaarRowsTest {

    private fun party(
        id: String,
        name: String,
        village: String? = null,
        isDeleted: Boolean = false
    ) = Party(
        id = id,
        shopId = "shop1",
        name = name,
        village = village,
        partyType = PartyType.FARMER,
        createdAt = 0L,
        updatedAt = 0L,
        isDeleted = isDeleted,
    )

    private fun loan(
        id: String,
        partyId: String,
        amountPaisa: Long,
        direction: LendDirection = LendDirection.GIVEN,
        kind: LendKind = LendKind.LOAN,
        lendType: LendType = LendType.MONEY,
        monthlyRatePercent: Double = 0.0,
        entryDate: Long = 0L,
        isVoid: Boolean = false,
    ) = LendEntry(
        id = id,
        shopId = "shop1",
        partyId = partyId,
        lendType = lendType,
        entryDate = entryDate,
        amountPaisa = amountPaisa,
        direction = direction,
        kind = kind,
        monthlyRatePercent = monthlyRatePercent,
        isVoid = isVoid,
        createdAt = entryDate,
        updatedAt = entryDate,
    )

    private fun settlement(
        id: String,
        lendId: String,
        amountPaisa: Long,
        principalPaisa: Long = 0L,
        interestPaisa: Long = 0L,
        settledAt: Long = 0L,
        isVoid: Boolean = false,
    ) = LendSettlement(
        id = id,
        shopId = "shop1",
        lendId = lendId,
        amountPaisa = amountPaisa,
        principalPaisa = principalPaisa,
        interestPaisa = interestPaisa,
        settledAt = settledAt,
        isVoid = isVoid,
        createdAt = settledAt,
        updatedAt = settledAt,
    )

    @Test
    fun planCaseA_twoGivenLoansForOnePartyCollapseIntoOneRowWithSummedPrincipal() {
        val parties = listOf(party("p1", "रमेश", "रामपुर"))
        val loans = listOf(
            loan("l1", "p1", 10_000L, direction = LendDirection.GIVEN),
            loan("l2", "p1", 20_000L, direction = LendDirection.GIVEN)
        )
        val rows = buildUdhaarRows(LendDirection.GIVEN, parties, loans, emptyList(), nowMs = 1000L)
        assertEquals(1, rows.size)
        val row = rows[0]
        assertEquals("p1", row.partyId)
        assertEquals("रमेश", row.name)
        assertEquals("रामपुर", row.village)
        assertEquals(30_000L, row.principalDuePaisa)
        assertEquals(0L, row.interestDuePaisa)
        assertFalse(row.interestOn)
    }

    @Test
    fun planCaseB_takenLoanDoesNotAppearInGivenList() {
        val parties = listOf(party("p1", "सुरेश"), party("p2", "महेश"))
        val loans = listOf(
            loan("l1", "p1", 10_000L, direction = LendDirection.GIVEN),
            loan("l2", "p2", 25_000L, direction = LendDirection.TAKEN)
        )
        val givenRows = buildUdhaarRows(LendDirection.GIVEN, parties, loans, emptyList(), nowMs = 1000L)
        assertEquals(1, givenRows.size)
        assertEquals("p1", givenRows[0].partyId)

        val takenRows = buildUdhaarRows(LendDirection.TAKEN, parties, loans, emptyList(), nowMs = 1000L)
        assertEquals(1, takenRows.size)
        assertEquals("p2", takenRows[0].partyId)
    }

    @Test
    fun planCaseC_fullyRepaidPartyIsAbsent() {
        val parties = listOf(party("p1", "रमेश"))
        val loans = listOf(loan("l1", "p1", 10_000L))
        val settlements = listOf(settlement("s1", "l1", 10_000L, principalPaisa = 10_000L))
        val rows = buildUdhaarRows(LendDirection.GIVEN, parties, loans, settlements, nowMs = 1000L)
        assertTrue(rows.isEmpty())
    }

    @Test
    fun planCaseD_interestOnIsTrueOnlyWhenOpenLoanHasRateAboveZero() {
        val parties = listOf(party("p1", "रमेश"), party("p2", "सुरेश"))
        val loans = listOf(
            loan("l1", "p1", 10_000L, monthlyRatePercent = 2.0),
            loan("l2", "p2", 20_000L, monthlyRatePercent = 0.0)
        )
        val rows = buildUdhaarRows(LendDirection.GIVEN, parties, loans, emptyList(), nowMs = 1000L)
        val p1Row = rows.first { it.partyId == "p1" }
        val p2Row = rows.first { it.partyId == "p2" }
        assertTrue(p1Row.interestOn)
        assertFalse(p2Row.interestOn)
    }

    @Test
    fun planCaseE_interestDueMatchesRuralInterestEngine() {
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000L
        val parties = listOf(party("p1", "रमेश"))
        val loans = listOf(loan("l1", "p1", 100_000L, monthlyRatePercent = 2.0, entryDate = 0L))
        val expectedInterest = RuralInterestEngine.interestDue(100_000L, 2.0, 0L, emptyList(), thirtyDaysMs)

        val rows = buildUdhaarRows(LendDirection.GIVEN, parties, loans, emptyList(), nowMs = thirtyDaysMs)
        assertEquals(1, rows.size)
        assertEquals(100_000L, rows[0].principalDuePaisa)
        assertEquals(expectedInterest, rows[0].interestDuePaisa)
    }

    @Test
    fun planCaseF_rowsSortedByTotalDueDescending() {
        val parties = listOf(party("p1", "कम"), party("p2", "ज्यादा"), party("p3", "बीच"))
        val loans = listOf(
            loan("l1", "p1", 10_000L),
            loan("l2", "p2", 50_000L),
            loan("l3", "p3", 25_000L)
        )
        val rows = buildUdhaarRows(LendDirection.GIVEN, parties, loans, emptyList(), nowMs = 1000L)
        assertEquals(listOf("p2", "p3", "p1"), rows.map { it.partyId })
    }

    @Test
    fun planCaseG_voidedLoanIsIgnored() {
        val parties = listOf(party("p1", "रमेश"))
        val loans = listOf(loan("l1", "p1", 10_000L, isVoid = true))
        val rows = buildUdhaarRows(LendDirection.GIVEN, parties, loans, emptyList(), nowMs = 1000L)
        assertTrue(rows.isEmpty())
    }

    @Test
    fun edgeCase1_kindFilterIncludesLoanPesgiAndCash() {
        val parties = listOf(party("p1", "रमेश"))
        val loans = listOf(
            loan("l1", "p1", 10_000L, kind = LendKind.LOAN),
            loan("l2", "p1", 5_000L, kind = LendKind.PESGI),
            loan("l3", "p1", 2_000L, kind = LendKind.CASH)
        )
        val rows = buildUdhaarRows(LendDirection.GIVEN, parties, loans, emptyList(), nowMs = 1000L)
        assertEquals(1, rows.size)
        assertEquals(17_000L, rows[0].principalDuePaisa)
    }

    @Test
    fun edgeCase2_interestOnlyLeftoverIsDroppedWhenPrincipalIsZero() {
        // Loan with rate 2%, 30 days old. Principal repaid in full, interest unpaid.
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000L
        val parties = listOf(party("p1", "रमेश"))
        val loans = listOf(loan("l1", "p1", 10_000L, monthlyRatePercent = 2.0, entryDate = 0L))
        val settlements = listOf(settlement("s1", "l1", 10_000L, principalPaisa = 10_000L, interestPaisa = 0L, settledAt = thirtyDaysMs))

        val rows = buildUdhaarRows(LendDirection.GIVEN, parties, loans, settlements, nowMs = thirtyDaysMs)
        // Outstanding principal is 0, so interest is zeroed like the person ledger, dropping the row.
        assertTrue(rows.isEmpty())
    }

    @Test
    fun edgeCase4_legacySettlementWithZeroPrincipalAndInterestCountsAsAllPrincipal() {
        val parties = listOf(party("p1", "रमेश"))
        val loans = listOf(loan("l1", "p1", 50_000L))
        // Legacy settlement: principalPaisa == 0L and interestPaisa == 0L -> amount counts as principal
        val settlements = listOf(settlement("s1", "l1", amountPaisa = 30_000L, principalPaisa = 0L, interestPaisa = 0L))

        val rows = buildUdhaarRows(LendDirection.GIVEN, parties, loans, settlements, nowMs = 1000L)
        assertEquals(1, rows.size)
        assertEquals(20_000L, rows[0].principalDuePaisa)
    }

    @Test
    fun edgeCase5_overpaymentClampsOutstandingToZero() {
        val parties = listOf(party("p1", "रमेश"))
        val loans = listOf(loan("l1", "p1", 10_000L))
        val settlements = listOf(settlement("s1", "l1", amountPaisa = 15_000L, principalPaisa = 15_000L))

        val rows = buildUdhaarRows(LendDirection.GIVEN, parties, loans, settlements, nowMs = 1000L)
        assertTrue(rows.isEmpty())
    }

    @Test
    fun edgeCase6_deletedOrMissingPartyStillShownWithFallback() {
        val parties = listOf(party("p1", "डिलीटेड पार्टी", isDeleted = true))
        val loans = listOf(
            loan("l1", "p1", 10_000L),
            loan("l2", "p_unknown", 15_000L)
        )
        val rows = buildUdhaarRows(LendDirection.GIVEN, parties, loans, emptyList(), nowMs = 1000L)
        assertEquals(2, rows.size)

        val deletedRow = rows.first { it.partyId == "p1" }
        assertEquals("डिलीटेड पार्टी", deletedRow.name)
        assertEquals(10_000L, deletedRow.principalDuePaisa)

        val unknownRow = rows.first { it.partyId == "p_unknown" }
        assertNull(unknownRow.name)
        assertEquals(15_000L, unknownRow.principalDuePaisa)
    }

    @Test
    fun edgeCase8_sortingTiesBrokenByNameAscending() {
        val parties = listOf(
            party("p2", "बी पार्टी"),
            party("p1", "ए पार्टी")
        )
        val loans = listOf(
            loan("l2", "p2", 10_000L),
            loan("l1", "p1", 10_000L)
        )
        val rows = buildUdhaarRows(LendDirection.GIVEN, parties, loans, emptyList(), nowMs = 1000L)
        assertEquals(2, rows.size)
        assertEquals("p1", rows[0].partyId)
        assertEquals("ए पार्टी", rows[0].name)
        assertEquals("p2", rows[1].partyId)
        assertEquals("बी पार्टी", rows[1].name)
    }

    @Test
    fun edgeCase9_repaidLoanWithRateDoesNotTurnInterestOn() {
        val parties = listOf(party("p1", "रमेश"))
        val loans = listOf(
            loan("l1", "p1", 10_000L, monthlyRatePercent = 2.0),
            loan("l2", "p1", 5_000L, monthlyRatePercent = 0.0)
        )
        // l1 is fully repaid
        val settlements = listOf(settlement("s1", "l1", 10_000L, principalPaisa = 10_000L))

        val rows = buildUdhaarRows(LendDirection.GIVEN, parties, loans, settlements, nowMs = 1000L)
        assertEquals(1, rows.size)
        assertEquals(5_000L, rows[0].principalDuePaisa)
        assertFalse(rows[0].interestOn)
    }
}
