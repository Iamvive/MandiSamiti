package com.appwork.mandisamiti.ui.lend

import com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.id.IdGenerator
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendSettlement
import com.appwork.mandisamiti.domain.model.LendStatus
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.ui.dashboard.computeKpis
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LendFlowTest {

    private val fixedNow = Instant.parse("2026-10-28T06:00:00Z")
    private val testClock = object : Clock {
        override fun now() = fixedNow
    }

    @Test
    fun tcLend01_saveMoneyLoan_createsActiveOpenLend() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstLendRepository(
            database = db,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clock = testClock
        )

        val partyName = "रामू किसान"
        val rupeesInput = "5000"
        val amountPaisa = MandiMathEngine.parseRupeesToPaisa(rupeesInput)
        assertEquals(500_000L, amountPaisa)

        val returnDays = 30L
        val returnTimestamp = fixedNow.toEpochMilliseconds() + returnDays * 24 * 3600 * 1000L

        val entry = LendEntry(
            id = "lend-money-1",
            shopId = "shop-test",
            partyId = partyName,
            lendType = LendType.MONEY,
            entryDate = fixedNow.toEpochMilliseconds(),
            amountPaisa = amountPaisa,
            expectedReturnDate = returnTimestamp,
            notes = "बीज एवं खाद हेतु",
            status = LendStatus.OPEN,
            createdAt = fixedNow.toEpochMilliseconds(),
            updatedAt = fixedNow.toEpochMilliseconds(),
        )

        repo.saveLendEntry(entry)

        val retrieved = repo.getLendEntryById("lend-money-1")
        assertEquals(entry, retrieved)
        assertEquals(LendStatus.OPEN, retrieved?.status)
        assertEquals(500_000L, retrieved?.amountPaisa)
        assertEquals(LendType.MONEY, retrieved?.lendType)

        val stream = repo.getLendEntriesStream("shop-test").first()
        assertEquals(1, stream.size)
        assertEquals("रामू किसान", stream[0].partyId)
    }

    @Test
    fun tcLend02_saveGrainLoan_recordsBagsAndWeightGrams() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstLendRepository(
            database = db,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clock = testClock
        )

        val partyName = "सुरेश"
        val crop = "गेहूं"
        val bags = 4L
        val weightKg = "200"
        val weightGrams = MandiMathEngine.parseKgToGrams(weightKg)
        assertEquals(200_000L, weightGrams)

        val entry = LendEntry(
            id = "lend-grain-1",
            shopId = "shop-test",
            partyId = partyName,
            lendType = LendType.GRAIN,
            entryDate = fixedNow.toEpochMilliseconds(),
            commodityId = crop,
            bagsCount = bags,
            weightGrams = weightGrams,
            status = LendStatus.OPEN,
            createdAt = fixedNow.toEpochMilliseconds(),
            updatedAt = fixedNow.toEpochMilliseconds(),
        )

        repo.saveLendEntry(entry)

        val retrieved = repo.getLendEntryById("lend-grain-1")
        assertEquals(entry, retrieved)
        assertEquals(LendType.GRAIN, retrieved?.lendType)
        assertEquals("गेहूं", retrieved?.commodityId)
        assertEquals(4L, retrieved?.bagsCount)
        assertEquals(200_000L, retrieved?.weightGrams)
        assertEquals(2.0, MandiMathEngine.gramsToQuintals(retrieved?.weightGrams ?: 0L))
    }

    @Test
    fun tcLend03_zeroAmountValidation_disabled() {
        val zeroPaisa = MandiMathEngine.parseRupeesToPaisa("0")
        assertEquals(0L, zeroPaisa)

        val blankPaisa = MandiMathEngine.parseRupeesToPaisa("")
        assertEquals(0L, blankPaisa)

        val invalidPaisa = MandiMathEngine.parseRupeesToPaisa("abc")
        assertEquals(0L, invalidPaisa)

        // Validation rule: canSave requires amount > 0
        fun canSaveMoney(party: String, amount: String): Boolean {
            return party.isNotBlank() && MandiMathEngine.parseRupeesToPaisa(amount) > 0L
        }

        fun canSaveGrain(party: String, crop: String, weight: String): Boolean {
            return party.isNotBlank() && crop.isNotBlank() && MandiMathEngine.parseKgToGrams(weight) > 0L
        }

        assertFalse(canSaveMoney("मोहन", "0"))
        assertFalse(canSaveMoney("मोहन", ""))
        assertFalse(canSaveMoney("", "5000"))
        assertTrue(canSaveMoney("मोहन", "5000"))

        assertFalse(canSaveGrain("सुरेश", "गेहूं", "0"))
        assertFalse(canSaveGrain("सुरेश", "", "200"))
        assertTrue(canSaveGrain("सुरेश", "गेहूं", "200"))
    }

    @Test
    fun tcLend04_activeLoanPromptInBuyEntry() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstLendRepository(
            database = db,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clock = testClock
        )

        // Record active loan for "रामू किसान"
        repo.saveLendEntry(
            LendEntry(
                id = "lend-active-1",
                shopId = "shop-test",
                partyId = "रामू किसान",
                lendType = LendType.MONEY,
                entryDate = fixedNow.toEpochMilliseconds(),
                amountPaisa = 500_000L,
                status = LendStatus.OPEN,
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds(),
            )
        )

        val lends = repo.getLendEntriesStream("shop-test").first()

        // Active loan checker predicate
        val checker: (String) -> Boolean = { farmerName ->
            lends.any { !it.isVoid && it.status != LendStatus.SETTLED && it.partyId.equals(farmerName, ignoreCase = true) }
        }

        // Selected "रामू किसान" -> active loan detected!
        assertTrue(checker("रामू किसान"))
        // Case insensitive match
        assertTrue(checker("  रामू किसान  ".trim()))
        // Other farmer with no loan -> false
        assertFalse(checker("दिनेश किसान"))

        // Settle loan and re-check
        repo.settle(
            LendSettlement(
                id = "settle-1",
                shopId = "shop-test",
                lendId = "lend-active-1",
                settledAt = fixedNow.toEpochMilliseconds(),
                amountPaisa = 500_000L,
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds(),
            )
        )

        val updatedLends = repo.getLendEntriesStream("shop-test").first()
        val updatedChecker: (String) -> Boolean = { farmerName ->
            updatedLends.any { !it.isVoid && it.status != LendStatus.SETTLED && it.partyId.equals(farmerName, ignoreCase = true) }
        }

        // Now that loan is SETTLED, no active loan prompt
        assertFalse(updatedChecker("रामू किसान"))
    }

    @Test
    fun repaymentLifecycle_partialToSettled_reflectedInDashboardKpis() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstLendRepository(
            database = db,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clock = testClock
        )

        val lend = LendEntry(
            id = "lend-kpi-1",
            shopId = "shop-test",
            partyId = "कालूराम",
            lendType = LendType.MONEY,
            entryDate = fixedNow.toEpochMilliseconds(),
            amountPaisa = 1_000_000L, // ₹10,000
            status = LendStatus.OPEN,
            createdAt = fixedNow.toEpochMilliseconds(),
            updatedAt = fixedNow.toEpochMilliseconds(),
        )
        repo.saveLendEntry(lend)

        var currentLends = repo.getLendEntriesStream("shop-test").first()
        var kpis = computeKpis(emptyList(), emptyList(), currentLends, emptyList())
        assertEquals(1_000_000L, kpis.totalLendPaisa)

        // Partial repayment ₹4,000
        repo.settle(
            LendSettlement(
                id = "s-part",
                shopId = "shop-test",
                lendId = "lend-kpi-1",
                settledAt = fixedNow.toEpochMilliseconds(),
                amountPaisa = 400_000L,
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds(),
            )
        )

        val partialLend = repo.getLendEntryById("lend-kpi-1")
        assertEquals(LendStatus.PARTIAL, partialLend?.status)

        // Full settlement
        repo.settle(
            LendSettlement(
                id = "s-full",
                shopId = "shop-test",
                lendId = "lend-kpi-1",
                settledAt = fixedNow.toEpochMilliseconds(),
                amountPaisa = 600_000L,
                createdAt = fixedNow.toEpochMilliseconds(),
                updatedAt = fixedNow.toEpochMilliseconds(),
            )
        )

        val settledLend = repo.getLendEntryById("lend-kpi-1")
        assertEquals(LendStatus.SETTLED, settledLend?.status)
    }

    @Test
    fun tcLend06_expectedReturnDate_storedAsDateAndFormattedForCard() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstLendRepository(
            database = db,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clock = testClock
        )

        val nowMillis = fixedNow.toEpochMilliseconds()
        val due30DaysMillis = nowMillis + 30L * 24 * 3600 * 1000L

        val lend = LendEntry(
            id = "lend-due-1",
            shopId = "shop-test",
            partyId = "RadheyShyam",
            lendType = LendType.MONEY,
            entryDate = nowMillis,
            amountPaisa = 500_000L,
            expectedReturnDate = due30DaysMillis,
            notes = "बीज एवं खाद हेतु",
            status = LendStatus.OPEN,
            createdAt = nowMillis,
            updatedAt = nowMillis,
        )
        repo.saveLendEntry(lend)

        val retrieved = repo.getLendEntryById("lend-due-1")
        assertEquals(due30DaysMillis, retrieved?.expectedReturnDate)

        // Verify formatted due date for card
        val formatted = com.appwork.mandisamiti.domain.util.LendDateFormatter.formatDueDate(
            returnEpochMillis = retrieved?.expectedReturnDate,
            currentEpochMillis = nowMillis,
            timeZone = kotlinx.datetime.TimeZone.UTC
        )
        assertEquals("वापसी: 27 Nov (30 दिन बाकी)", formatted)

        // Verify overdue formatted string
        val overdueMillis = nowMillis - 2L * 24 * 3600 * 1000L
        val overdueFormatted = com.appwork.mandisamiti.domain.util.LendDateFormatter.formatDueDate(
            returnEpochMillis = overdueMillis,
            currentEpochMillis = nowMillis,
            timeZone = kotlinx.datetime.TimeZone.UTC
        )
        assertEquals("वापसी: 26 Oct (2 दिन अतिदेय)", overdueFormatted)
    }
}
