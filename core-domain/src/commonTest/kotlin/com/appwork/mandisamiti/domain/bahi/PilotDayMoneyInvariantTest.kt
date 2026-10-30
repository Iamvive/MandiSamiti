package com.appwork.mandisamiti.domain.bahi

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PilotDayMoneyInvariantTest {

    @Test
    fun generatedPilotDayWithTradeLoanInterestDepositExpenseAndVoidBalances() {
        val entries = listOf(
            entry("sell", BahiEntryKind.BUYER_PAYMENT, 500_000L),
            entry("loan-principal", BahiEntryKind.LOAN_PRINCIPAL_REPAID, 100_000L),
            entry("loan-interest", BahiEntryKind.LOAN_INTEREST, 20_000L),
            entry("general-jama", BahiEntryKind.BUYER_PAYMENT, 50_000L),
            entry("buy-gross", BahiEntryKind.GRAIN_LEDGER, 300_000L),
            entry("buy-net", BahiEntryKind.FARMER_RECEIPT_NET, 280_000L),
            entry("loan-given", BahiEntryKind.ADVANCE_PAID, 200_000L),
            entry("expense", BahiEntryKind.EXPENSE, 40_000L),
            entry("void-expense", BahiEntryKind.EXPENSE, 99_900L, isVoid = true),
        )

        val page = BahiDayPageBuilder.build(
            dateEpochDay = 20261031L,
            openingPaisa = 1_000_000L,
            closingPaisa = 850_000L,
            entries = entries,
        )

        assertTrue(page.isBalanced)
        assertEquals(1_670_000L, page.leftTotalPaisa)
        assertEquals(1_670_000L, page.rightTotalPaisa)
        assertTrue(page.right.single { it.id == "void-expense" }.isStruckThrough)
    }

    @Test
    fun fixedSeedRandomDaysMatchCheckedNaiveRecomputation() {
        val random = Random(20_261_031)
        repeat(200) { sequence ->
            val entries = List(random.nextInt(1, 41)) { index ->
                entry(
                    id = "$sequence:$index",
                    kind = if (random.nextBoolean()) LEFT_KINDS.random(random) else RIGHT_KINDS.random(random),
                    amountPaisa = random.nextLong(0L, 10_000_001L),
                    isVoid = random.nextInt(8) == 0,
                )
            }
            val activeLeft = checkedSum(entries.filter { !it.isVoid && it.kind in LEFT_KINDS }.map { it.amountPaisa })
            val activeRight = checkedSum(entries.filter { !it.isVoid && it.kind in RIGHT_KINDS }.map { it.amountPaisa })
            val base = random.nextLong(0L, 1_000_001L)
            val opening = Math.addExact(activeRight, base)
            val closing = Math.addExact(activeLeft, base)

            val page = BahiDayPageBuilder.build(sequence.toLong(), opening, closing, entries)
            val naiveLeft = Math.addExact(opening, activeLeft)
            val naiveRight = Math.addExact(activeRight, closing)

            assertEquals(naiveLeft, page.leftTotalPaisa, "left sequence $sequence")
            assertEquals(naiveRight, page.rightTotalPaisa, "right sequence $sequence")
            assertTrue(page.isBalanced, "sequence $sequence")
        }
    }

    @Test
    fun everyMoneyEntryKindIsExcludedWhenVoided() {
        (LEFT_KINDS + RIGHT_KINDS).forEach { kind ->
            val page = BahiDayPageBuilder.build(
                dateEpochDay = kind.ordinal.toLong(),
                openingPaisa = 100_000L,
                closingPaisa = 100_000L,
                entries = listOf(entry(kind.name, kind, 999_999L, isVoid = true)),
            )
            assertTrue(page.isBalanced, kind.name)
            assertTrue((page.left + page.right).single { it.id == kind.name }.isStruckThrough)
        }
    }

    private fun entry(
        id: String,
        kind: BahiEntryKind,
        amountPaisa: Long,
        isVoid: Boolean = false,
    ) = BahiDayEntry(
        id = id,
        kind = kind,
        label = id,
        amountPaisa = amountPaisa,
        occurredAt = id.hashCode().toLong(),
        isVoid = isVoid,
    )

    private fun checkedSum(values: List<Long>) = values.fold(0L, Math::addExact)

    private companion object {
        val LEFT_KINDS = listOf(
            BahiEntryKind.FARMER_MONEY_LEFT,
            BahiEntryKind.BUYER_PAYMENT,
            BahiEntryKind.LOAN_PRINCIPAL_REPAID,
            BahiEntryKind.LOAN_INTEREST,
            BahiEntryKind.ADVANCE_CARRIED_IN,
        )
        val RIGHT_KINDS = listOf(
            BahiEntryKind.EXPENSE,
            BahiEntryKind.ADVANCE_PAID,
            BahiEntryKind.FARMER_PAYMENT,
            BahiEntryKind.LABOUR,
            BahiEntryKind.GRAIN_LEDGER,
            BahiEntryKind.FARMER_RECEIPT_NET,
        )
    }
}
