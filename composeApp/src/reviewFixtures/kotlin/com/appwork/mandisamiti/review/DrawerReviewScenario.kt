package com.appwork.mandisamiti.review

import com.appwork.mandisamiti.data.repository.*
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.domain.model.*
import kotlinx.datetime.Instant

/** Shared by debug review app and integration tests; never included in release builds. */
object DrawerReviewScenario {
    const val shopId = "DRAWER_REVIEW_ONLY"
    val now: Instant = Instant.parse("2026-10-31T12:00:00Z")
    const val dayOneClosing = 5_250_000L
    const val dayTwoClosing = 8_050_000L

    suspend fun seed(database: AppDatabase) {
        val queries = database.appDatabaseQueries
        if (queries.getSyncMetadataLong("drawer_review_seed_v1").executeAsOneOrNull() == 1L) return
        val shops = OfflineFirstShopProfileRepository(database)
        val parties = OfflineFirstPartyRepository(database)
        val cash = OfflineFirstCashTransactionRepository(database)
        val expenses = OfflineFirstExpenseRepository(database)
        shops.saveShopProfile(ShopProfile(shopId, "TEST Cash Drawer", "Test Owner", "Mathura",
            phoneNumber = "9000000000", pinHash = "review-only", isSoundEnabled = false, createdAt = 1, updatedAt = 1))
        listOf(
            Party("TEST_FARMER", shopId, "TEST रमेश कुमार शर्मा", village = "गोवर्धन", partyType = PartyType.FARMER, createdAt = 1, updatedAt = 1),
            Party("TEST_BUYER", shopId, "TEST सुरेश चंद अग्रवाल", village = "मथुरा", partyType = PartyType.BUYER, createdAt = 1, updatedAt = 1),
        ).forEach { parties.saveParty(it) }
        suspend fun entry(id: String, timestamp: String, rupees: Long, out: Boolean = false,
                          mode: PaymentMode = PaymentMode.CASH, party: String = "TEST_BUYER", remarks: String,
                          type: TransactionType = if (out) TransactionType.UDHAR_GIVEN else TransactionType.JAMA_RECEIVED) {
            val time = Instant.parse(timestamp).toEpochMilliseconds()
            cash.recordTransaction(CashTransaction(id, shopId, party, transactionType = type,
                amountPaisa = rupees * 100, paymentMode = mode, transactionDate = time,
                remarks = remarks, createdAt = time, updatedAt = time))
        }
        entry("REVIEW_OPENING", "2026-10-29T12:30:00Z", 20_000, remarks = "TEST opening cash")
        entry("D1_RECEIPT_1", "2026-10-30T04:00:00Z", 35_000, remarks = "TEST buyer receipt")
        entry("D1_RECEIPT_2", "2026-10-30T05:00:00Z", 12_000, remarks = "TEST repayment")
        entry("D1_ADVANCE", "2026-10-30T06:00:00Z", 8_000, out = true, party = "TEST_FARMER", remarks = "TEST farmer advance")
        entry("D1_PAYMENT", "2026-10-30T07:00:00Z", 5_000, out = true, party = "TEST_FARMER", remarks = "TEST farmer payment")
        val dayOneExpenseTime = Instant.parse("2026-10-30T08:00:00Z").toEpochMilliseconds()
        expenses.saveExpense(ExpenseEntry("D1_EXPENSE", shopId, dayOneExpenseTime, ExpenseCategory.HAMMALI,
            150_000, paymentMode = ExpensePaymentMode.CASH, remarks = "TEST labour", createdAt = dayOneExpenseTime, updatedAt = dayOneExpenseTime))
        entry("D1_EXPENSE_CASH", "2026-10-30T08:00:00Z", 1_500, out = true, party = "REVIEW_EXPENSE", remarks = "TEST labour [EXPENSE:D1_EXPENSE]")
        entry("D1_UPI", "2026-10-30T09:00:00Z", 9_000, mode = PaymentMode.UPI, remarks = "TEST UPI receipt")
        entry("D1_BANK", "2026-10-30T10:00:00Z", 3_500, out = true, mode = PaymentMode.NEFT, remarks = "TEST bank payment")
        entry("D2_RECEIPT_1", "2026-10-31T04:00:00Z", 18_000, remarks = "TEST buyer receipt")
        entry("D2_RECEIPT_2", "2026-10-31T05:00:00Z", 22_000, remarks = "TEST cash sale receipt")
        entry("D2_RECEIPT_3", "2026-10-31T06:00:00Z", 12_500, remarks = "TEST loan repayment")
        entry("D2_INTEREST", "2026-10-31T06:30:00Z", 500, remarks = "TEST interest received", type = TransactionType.INTEREST_RECEIVED)
        entry("D2_ADVANCE", "2026-10-31T07:00:00Z", 14_000, out = true, party = "TEST_FARMER", remarks = "TEST farmer advance")
        entry("D2_PAYMENT", "2026-10-31T08:00:00Z", 6_000, out = true, party = "TEST_FARMER", remarks = "TEST farmer payment")
        val dayTwoExpenseTime = Instant.parse("2026-10-31T09:00:00Z").toEpochMilliseconds()
        expenses.saveExpense(ExpenseEntry("D2_EXPENSE", shopId, dayTwoExpenseTime, ExpenseCategory.HAMMALI,
            200_000, paymentMode = ExpensePaymentMode.CASH, remarks = "TEST labour", createdAt = dayTwoExpenseTime, updatedAt = dayTwoExpenseTime))
        entry("D2_EXPENSE_CASH", "2026-10-31T09:00:00Z", 2_000, out = true, party = "REVIEW_EXPENSE", remarks = "TEST labour [EXPENSE:D2_EXPENSE]")
        entry("D2_HOME", "2026-10-31T10:00:00Z", 3_000, out = true, party = "REVIEW_HOME", remarks = "TEST cash to home")
        entry("D2_UPI", "2026-10-31T10:30:00Z", 11_000, mode = PaymentMode.UPI, remarks = "TEST UPI receipt")
        entry("D2_BANK", "2026-10-31T11:00:00Z", 4_000, out = true, mode = PaymentMode.NEFT, remarks = "TEST bank payment")
        queries.setSyncMetadataLong("drawer_review_seed_v1", 1)
    }
}
