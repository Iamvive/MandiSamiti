package com.appwork.mandisamiti.ui.register

import com.appwork.mandisamiti.data.repository.*
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import com.appwork.mandisamiti.review.DrawerReviewScenario
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.datetime.*
import kotlin.test.*

class DrawerTwoDayScenarioTest {
    @Test fun twoDaysCarryForwardExcludeOnlineKeepCountsAndDoNotDuplicateOnReopen() = runBlocking {
        val db = createTestDatabase()
        DrawerReviewScenario.seed(db)
        val cash = OfflineFirstCashTransactionRepository(db)
        assertEquals(18, cash.getTransactionsByShopStream(DrawerReviewScenario.shopId).first().size)
        DrawerReviewScenario.seed(db)
        assertEquals(18, cash.getTransactionsByShopStream(DrawerReviewScenario.shopId).first().size)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val counts = OfflineFirstDrawerCountRepository(db)
        val vm = DailyRegisterViewModel(
            DrawerReviewScenario.shopId, cash, OfflineFirstPartyRepository(db), OfflineFirstShopProfileRepository(db),
            SoundboxTtsManager(), expenseRepository = OfflineFirstExpenseRepository(db), viewModelScope = scope,
            clock = object : Clock { override fun now() = DrawerReviewScenario.now },
            timeZone = TimeZone.of("Asia/Kolkata"), drawerCountRepository = counts,
        )
        try {
            vm.selectDate(LocalDate(2026, 10, 30))
            val first = withTimeout(10_000) { vm.uiState.first { it.selectedDate == LocalDate(2026, 10, 30) && it.todayTransactions.size == 7 } }
            assertEquals(2_000_000L, first.openingCashPaisa)
            assertEquals(4_700_000L, first.todayCashInPaisa)
            assertEquals(1_450_000L, first.todayCashOutPaisa)
            assertEquals(DrawerReviewScenario.dayOneClosing, first.inHandCashDrawerPaisa)
            assertTrue(first.bahiDayPage?.isBalanced == true)
            assertEquals("TEST labour", first.todayTransactions.first { it.transaction.id == "D1_EXPENSE_CASH" }.partyName)
            assertTrue(first.todayTransactions.none { it.partyName.contains("[EXPENSE:") })
            vm.closeDay(52_500)
            withTimeout(10_000) { vm.uiState.first { it.isDayClosed } }
            vm.navigateToToday()
            val second = withTimeout(10_000) { vm.uiState.first { it.isToday && it.todayTransactions.size == 10 } }
            assertEquals(first.inHandCashDrawerPaisa, second.openingCashPaisa)
            assertEquals(5_300_000L, second.todayCashInPaisa)
            assertEquals(2_500_000L, second.todayCashOutPaisa)
            assertEquals(DrawerReviewScenario.dayTwoClosing, second.inHandCashDrawerPaisa)
            assertTrue(second.bahiDayPage?.isBalanced == true, "Cash interest must appear on the receipts side")
            assertNull(second.physicalCountPaisa)
            vm.setPhysicalCashCount(80_400, "TEST shortage")
            val short = withTimeout(10_000) { vm.uiState.first { it.physicalCountPaisa == 8_040_000L } }
            assertEquals(-10_000L, short.discrepancyPaisa)
            assertFalse(short.isDayClosed)
            withTimeout(10_000) { vm.uiState.first { it.bahiDayPage?.differencePaisa == 10_000L } }
            vm.setPhysicalCashCount(80_500)
            withTimeout(10_000) { vm.uiState.first { it.physicalCountPaisa == 8_050_000L && it.bahiDayPage?.isBalanced == true } }
            assertEquals(5_250_000L, counts.load(DrawerReviewScenario.shopId, "2026-10-30")?.countedPaisa)
            assertEquals(8_050_000L, counts.load(DrawerReviewScenario.shopId, "2026-10-31")?.countedPaisa)
            assertEquals(18, cash.getTransactionsByShopStream(DrawerReviewScenario.shopId).first().size)
        } finally { scope.cancel() }
    }
}
