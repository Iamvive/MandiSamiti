package com.appwork.mandisamiti.analytics

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.appwork.mandisamiti.Screen
import com.appwork.mandisamiti.data.analytics.Analytics
import com.appwork.mandisamiti.data.analytics.AnalyticsEventName
import com.appwork.mandisamiti.data.analytics.AnalyticsScreen
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.domain.model.Deal
import com.appwork.mandisamiti.domain.model.LendDirection
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.ui.navigation.NavigationTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class TelemetryAppHooksTest {

    private fun createTestDatabase(): AppDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        return AppDatabase(driver)
    }

    private fun dummyParty(id: String = "party_1") = Party(
        id = id,
        shopId = "shop_1",
        name = "Kisan",
        partyType = PartyType.FARMER,
        createdAt = 1_000L,
        updatedAt = 1_000L,
    )

    private fun dummyDeal(id: String = "deal_1") = Deal(
        id = id,
        shopId = "shop_1",
        farmerId = "party_1",
        commodityId = "comm_1",
        dealDate = 1_000L,
        bagsCount = 10,
        grossWeightGrams = 1_000_000L,
        netWeightGrams = 980_000L,
        createdAt = 1_000L,
        updatedAt = 1_000L,
    )

    @Test
    fun testScreenToAnalyticsScreenMapping() {
        assertEquals(AnalyticsScreen.REGISTER, Screen.Register().toAnalyticsScreen())
        assertEquals(AnalyticsScreen.FIRST_SYNC, Screen.FirstSync.toAnalyticsScreen())

        assertEquals(AnalyticsScreen.HOME, Screen.Home.toAnalyticsScreen(null))
        assertEquals(AnalyticsScreen.HOME_DASHBOARD, Screen.Home.toAnalyticsScreen(NavigationTab.DASHBOARD))
        assertEquals(AnalyticsScreen.HOME_PARCHI, Screen.Home.toAnalyticsScreen(NavigationTab.PARCHI))
        assertEquals(AnalyticsScreen.HOME_GALLA, Screen.Home.toAnalyticsScreen(NavigationTab.GALLA))
        assertEquals(AnalyticsScreen.HOME_GODOWN, Screen.Home.toAnalyticsScreen(NavigationTab.GODOWN))
        assertEquals(AnalyticsScreen.HOME_SETTINGS, Screen.Home.toAnalyticsScreen(NavigationTab.SETTINGS))

        assertEquals(AnalyticsScreen.DEAL_ENTRY, Screen.DealEntry().toAnalyticsScreen())
        assertEquals(AnalyticsScreen.PARTY_LEDGER, Screen.PartyLedger("p1").toAnalyticsScreen())
        assertEquals(AnalyticsScreen.RECEIPT_PREVIEW, Screen.ReceiptPreview(dummyDeal(), dummyParty(), null).toAnalyticsScreen())
        assertEquals(AnalyticsScreen.DAILY_REGISTER, Screen.DailyRegister.toAnalyticsScreen())
        assertEquals(AnalyticsScreen.BUY_ENTRY, Screen.BuyEntry().toAnalyticsScreen())
        assertEquals(AnalyticsScreen.BUY_PAYMENT, Screen.BuyPayment("b1").toAnalyticsScreen())
        assertEquals(AnalyticsScreen.BUY_LIST, Screen.BuyList.toAnalyticsScreen())
        assertEquals(AnalyticsScreen.BUY_RECEIPT, Screen.BuyReceipt("b1").toAnalyticsScreen())
        assertEquals(AnalyticsScreen.SELL_ENTRY, Screen.SellEntry().toAnalyticsScreen())
        assertEquals(AnalyticsScreen.SELL_LIST, Screen.SellList.toAnalyticsScreen())
        assertEquals(AnalyticsScreen.SELL_RECEIPT, Screen.SellReceipt("s1").toAnalyticsScreen())
        assertEquals(AnalyticsScreen.GRAIN_LEND_ENTRY, Screen.GrainLendEntry().toAnalyticsScreen())
        assertEquals(AnalyticsScreen.LEND_LIST, Screen.LendList.toAnalyticsScreen())
        assertEquals(AnalyticsScreen.UDHAAR_PERSON, Screen.UdhaarPerson("p1", LendDirection.GIVEN).toAnalyticsScreen())
        assertEquals(AnalyticsScreen.EXPENSE_ENTRY, Screen.ExpenseEntry().toAnalyticsScreen())
        assertEquals(AnalyticsScreen.EXPENSE_LIST, Screen.ExpenseList.toAnalyticsScreen())
        assertEquals(AnalyticsScreen.STOCK_SHEET, Screen.StockSheet().toAnalyticsScreen())
        assertEquals(AnalyticsScreen.ALL_KHATA, Screen.AllKhata.toAnalyticsScreen())
        assertEquals(AnalyticsScreen.SUBSCRIPTION_CONTACT, Screen.SubscriptionContact.toAnalyticsScreen())
    }

    @Test
    fun testBuySavedRecordedOnceOnSuccessAndNothingOnFailure() = runTest {
        val database = createTestDatabase()
        val analytics = Analytics(
            database = database,
            appVersion = "2.0.0",
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Failure path: throws exception and must record nothing
        assertFailsWith<IllegalStateException> {
            recordAction(analytics, AnalyticsEventName.BUY_SAVED, this) {
                error("Buy save failed!")
            }
        }
        advanceUntilIdle()
        val eventsAfterFailure = database.appDatabaseQueries.getOldestAnalyticsEvents(10).executeAsList()
        assertEquals(0, eventsAfterFailure.size, "Nothing should be recorded on failure")

        // Success path: records exactly once
        val result = recordAction(analytics, AnalyticsEventName.BUY_SAVED, this) {
            "buy_success"
        }
        assertEquals("buy_success", result)
        advanceUntilIdle()

        val eventsAfterSuccess = database.appDatabaseQueries.getOldestAnalyticsEvents(10).executeAsList()
        assertEquals(1, eventsAfterSuccess.size)
        assertEquals(AnalyticsEventName.BUY_SAVED.wire, eventsAfterSuccess.first().name)
    }

    @Test
    fun testDeletePathRecordedOnceOnSuccessAndNothingOnFailure() = runTest {
        val database = createTestDatabase()
        val analytics = Analytics(
            database = database,
            appVersion = "2.0.0",
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Failure path: throws exception and must record nothing
        assertFailsWith<IllegalStateException> {
            recordAction(analytics, AnalyticsEventName.ENTRY_DELETED, this) {
                error("Delete failed!")
            }
        }
        advanceUntilIdle()
        val eventsAfterFailure = database.appDatabaseQueries.getOldestAnalyticsEvents(10).executeAsList()
        assertEquals(0, eventsAfterFailure.size, "Nothing should be recorded on failure")

        // Success path: records exactly once
        val result = recordAction(analytics, AnalyticsEventName.ENTRY_DELETED, this) {
            "delete_success"
        }
        assertEquals("delete_success", result)
        advanceUntilIdle()

        val eventsAfterSuccess = database.appDatabaseQueries.getOldestAnalyticsEvents(10).executeAsList()
        assertEquals(1, eventsAfterSuccess.size)
        assertEquals(AnalyticsEventName.ENTRY_DELETED.wire, eventsAfterSuccess.first().name)
    }

    @Test
    fun testAppOpenTrackerRecordsOnlyOncePerProcess() = runTest {
        val database = createTestDatabase()
        val analytics = Analytics(
            database = database,
            appVersion = "2.0.0",
            ioDispatcher = Dispatchers.Unconfined,
        )

        AppOpenTracker.resetForTesting()

        AppOpenTracker.trackIfNeeded(analytics, this)
        advanceUntilIdle()
        val eventsFirst = database.appDatabaseQueries.getOldestAnalyticsEvents(10).executeAsList()
        assertEquals(1, eventsFirst.size)
        assertEquals(AnalyticsEventName.APP_OPEN.wire, eventsFirst.first().name)

        // Second call in same process must not record duplicate
        AppOpenTracker.trackIfNeeded(analytics, this)
        advanceUntilIdle()
        val eventsSecond = database.appDatabaseQueries.getOldestAnalyticsEvents(10).executeAsList()
        assertEquals(1, eventsSecond.size, "Duplicate app_open should not be recorded")
    }
}
