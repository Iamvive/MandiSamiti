package com.appwork.mandisamiti.ui.register

import com.appwork.mandisamiti.data.repository.*
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.*
import com.appwork.mandisamiti.domain.repository.DrawerCount
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.datetime.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DrawerReviewTest {
    private val now = Instant.parse("2026-10-31T10:00:00Z")
    private fun tx(id: String = "one", mode: PaymentMode = PaymentMode.CASH) = CashTransaction(
        id = id, shopId = "shop", partyId = "party", transactionType = TransactionType.JAMA_RECEIVED,
        amountPaisa = 100_000, paymentMode = mode, transactionDate = now.toEpochMilliseconds(),
        createdAt = now.toEpochMilliseconds(), updatedAt = now.toEpochMilliseconds(),
    )

    @Test fun denominationCounterRejectsMalformedAndOverflowingCounts() {
        assertEquals(1_205L, denominationTotal(mapOf(500 to "2", 200 to "1", 5 to "1")))
        assertEquals(0L, denominationTotal(emptyMap()))
        assertNull(denominationTotal(mapOf(500 to "-1")))
        assertNull(denominationTotal(mapOf(500 to "1.5")))
        assertNull(denominationTotal(mapOf(500 to Long.MAX_VALUE.toString())))
        assertNull(parseDrawerRupees("-1"))
        assertNull(parseDrawerRupees("92233720368547759"))
        assertEquals(0L, parseDrawerRupees("0"))
    }

    @Test fun cashFingerprintDetectsNetZeroEditsVoidsAndCarryForwardButIgnoresOnline() {
        val end = Instant.parse("2026-11-01T00:00:00Z").toEpochMilliseconds()
        val original = listOf(tx(), tx("online", PaymentMode.UPI))
        val signature = drawerCashFingerprint(original, end)
        assertEquals(signature, drawerCashFingerprint(original.reversed(), end))
        assertEquals(signature, drawerCashFingerprint(listOf(tx(), tx("online", PaymentMode.UPI).copy(amountPaisa = 1)), end))
        assertNotEquals(signature, drawerCashFingerprint(listOf(tx().copy(isVoid = true)), end))
        assertNotEquals(signature, drawerCashFingerprint(listOf(tx().copy(revision = 2)), end))
        assertNotEquals(signature, drawerCashFingerprint(listOf(tx(), tx("prior").copy(transactionDate = 0)), end))
        assertEquals(signature, drawerCashFingerprint(original + tx("future").copy(transactionDate = end), end))
    }

    @Test fun countPersistsPerShopAndDateAndResetSurvivesRepositoryRecreation() = runTest {
        val db = createTestDatabase()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repo = OfflineFirstDrawerCountRepository(db, dispatcher)
        val count = DrawerCount(1_000L, 20L, 30L, 40L, "Counted by owner")
        repo.save("shop", "2026-10-31", count)
        val recreated = OfflineFirstDrawerCountRepository(db, dispatcher)
        assertEquals(count, recreated.load("shop", "2026-10-31"))
        assertNull(recreated.load("other", "2026-10-31"))
        assertNull(recreated.load("shop", "2026-10-30"))
        recreated.save("shop", "2026-10-31", null)
        assertNull(repo.load("shop", "2026-10-31"))
    }

    @Test fun closingPreservesShortageAndRequiresRecountAfterCashChanges() = runTest {
        val db = createTestDatabase()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val cash = OfflineFirstCashTransactionRepository(db, dispatcher)
        val counts = OfflineFirstDrawerCountRepository(db, dispatcher)
        cash.recordTransaction(tx())
        val vm = DailyRegisterViewModel(
            "shop", cash, OfflineFirstPartyRepository(db, dispatcher),
            OfflineFirstShopProfileRepository(db, dispatcher), SoundboxTtsManager(),
            viewModelScope = backgroundScope, clock = object : Clock { override fun now() = now },
            timeZone = TimeZone.UTC, drawerCountRepository = counts,
        )
        runCurrent()
        assertEquals(100_000L, vm.uiState.value.inHandCashDrawerPaisa)
        vm.setPhysicalCashCount(900)
        runCurrent()
        assertFalse(vm.uiState.value.isDayClosed)
        vm.closeDay(900)
        runCurrent()
        assertTrue(vm.uiState.value.isDayClosed)
        assertEquals(-10_000L, vm.uiState.value.discrepancyPaisa)
        assertEquals(100_000L, vm.uiState.value.inHandCashDrawerPaisa)
        cash.recordTransaction(tx("online", PaymentMode.UPI))
        runCurrent()
        assertFalse(vm.uiState.value.needsRecount)
        cash.recordTransaction(tx("later"))
        runCurrent()
        assertTrue(vm.uiState.value.needsRecount)
        assertFalse(vm.uiState.value.isDayClosed)
        assertEquals(BahiReconciliationStatus.RECOUNT_REQUIRED, vm.uiState.value.reconciliationStatus)
        vm.navigateToPreviousDay()
        runCurrent()
        assertNull(vm.uiState.value.physicalCountPaisa)
        vm.navigateToToday()
        runCurrent()
        assertEquals(90_000L, vm.uiState.value.physicalCountPaisa)
        assertTrue(vm.uiState.value.needsRecount)
        val recreated = DailyRegisterViewModel(
            "shop", cash, OfflineFirstPartyRepository(db, dispatcher),
            OfflineFirstShopProfileRepository(db, dispatcher), SoundboxTtsManager(),
            viewModelScope = backgroundScope, clock = object : Clock { override fun now() = now },
            timeZone = TimeZone.UTC, drawerCountRepository = OfflineFirstDrawerCountRepository(db, dispatcher),
        )
        runCurrent()
        assertEquals(90_000L, recreated.uiState.value.physicalCountPaisa)
        assertTrue(recreated.uiState.value.needsRecount)
    }
}
