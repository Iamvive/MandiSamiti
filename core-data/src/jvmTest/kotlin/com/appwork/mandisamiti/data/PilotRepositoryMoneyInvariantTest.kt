package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstBuyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstExpenseRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstSellRepository
import com.appwork.mandisamiti.data.sync.SyncEngine
import com.appwork.mandisamiti.data.sync.model.SyncPullResponseDto
import com.appwork.mandisamiti.data.sync.model.SyncPushRequestDto
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.cash.CashDrawer
import com.appwork.mandisamiti.domain.cash.DayWindow
import com.appwork.mandisamiti.domain.model.*
import com.appwork.mandisamiti.domain.usecase.LendForm
import com.appwork.mandisamiti.domain.usecase.LendTransactionUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PilotRepositoryMoneyInvariantTest {
    private val fixedNow = Instant.fromEpochMilliseconds(10_000L)
    private val clock = object : Clock { override fun now() = fixedNow }

    @Test
    fun repaymentInterestIsCashInButOnlyPrincipalChangesPartyBalance() = runTest {
        val fixture = loanFixture(1_000_000L)

        fixture.useCase.repayWithInterest(
            lendId = fixture.lend.id,
            paymentPaisa = 120_000L,
            interestPaisa = 20_000L,
            calculatedInterestPaisa = 20_000L,
        )

        val transactions = fixture.cashRepository.getTransactionsByPartyStream(PARTY_ID).first()
        val drawer = CashDrawer.summarize(transactions, DayWindow(0L, 20_000L))
        assertEquals(120_000L, drawer.cashInPaisa)
        assertEquals(900_000L, fixture.balance(), "interest must not reduce the party principal balance")
    }

    @Test
    fun voidingPrincipalCashRowVoidsInterestSiblingAndSettlementTogether() = runTest {
        val fixture = loanFixture(1_000_000L)
        fixture.useCase.repayWithInterest(
            lendId = fixture.lend.id,
            paymentPaisa = 120_000L,
            interestPaisa = 20_000L,
            calculatedInterestPaisa = 20_000L,
        )
        val settlement = fixture.database.appDatabaseQueries
            .getLendSettlementsByLend(fixture.lend.id).executeAsOne()
        val principal = fixture.cashRepository.getTransactionsByPartyStream(PARTY_ID).first()
            .single { it.transactionType == TransactionType.JAMA_RECEIVED }

        fixture.cashRepository.voidTransaction(principal.id, VoidReason.WRONG_ENTRY)

        val cashRows = fixture.cashRepository.getTransactionsByPartyStream(PARTY_ID).first()
        assertTrue(cashRows.all { it.isVoid })
        assertEquals(2, cashRows.size)
        assertEquals(1L, fixture.database.appDatabaseQueries.getLendSettlementById(settlement.id).executeAsOne().is_void)
        assertEquals(1_000_000L, fixture.balance())
        assertEquals(0L, CashDrawer.summarize(cashRows, DayWindow(0L, 20_000L)).cashInPaisa)
    }

    @Test
    fun repaymentOverflowBecomesGeneralJamaAfterPrincipalAndInterest() = runTest {
        val fixture = loanFixture(100_000L)

        val split = fixture.useCase.repayWithInterest(
            lendId = fixture.lend.id,
            paymentPaisa = 170_000L,
            interestPaisa = 20_000L,
            calculatedInterestPaisa = 20_000L,
        )

        assertEquals(100_000L, split.loanPaisa)
        assertEquals(20_000L, split.interestPaisa)
        assertEquals(50_000L, split.generalPaisa)
        assertEquals(-50_000L, fixture.balance(), "only the general jama may leave a credit after principal is cleared")
    }

    @Test
    fun repositorySyncRoundTripPreservesTotalsSlipsAndLegacySettlement() = runTest {
        val source = createTestDatabase()
        seedShop(source)
        OfflineFirstPartyRepository(source).saveParty(party())
        OfflineFirstBuyRepository(source, clock = clock).saveBuyEntry(buy())
        OfflineFirstSellRepository(source, clock = clock).saveSellEntry(sell())
        val lendRepository = OfflineFirstLendRepository(source, clock = clock)
        lendRepository.saveLendEntry(lend())

        // Recreate a pre-D8 lend row with no slip and a pre-D4 amount-only settlement.
        source.appDatabaseQueries.getLendEntryById("lend-1").executeAsOne().let { row ->
            source.appDatabaseQueries.insertLendEntry(
                row.id, row.shop_id, row.party_id, row.lend_type, row.entry_date, row.amount_paisa,
                row.commodity_id, row.bags_count, row.weight_grams, row.expected_return_date, null,
                row.notes, row.status, row.created_at, row.updated_at, row.is_deleted, 0L,
                row.revision, row.is_void, row.void_reason,
            )
        }
        source.appDatabaseQueries.insertLendSettlement(
            id = "legacy-settlement", shop_id = SHOP_ID, lend_id = "lend-1", settled_at = 5_000L,
            amount_paisa = 40_000L, weight_grams = 0L, bags_count = 0L,
            created_at = 5_000L, updated_at = 5_000L, sync_status = 0L, revision = 1L,
            is_void = 0L, void_reason = null, principal_paisa = 0L, interest_paisa = 0L,
            calculated_interest_paisa = 0L,
        )
        OfflineFirstCashTransactionRepository(source, clock = clock).recordTransaction(
            CashTransaction(
                id = "cash-1", shopId = SHOP_ID, partyId = PARTY_ID,
                transactionType = TransactionType.JAMA_RECEIVED, amountPaisa = 30_000L,
                transactionDate = 6_000L, createdAt = 6_000L, updatedAt = 6_000L,
            )
        )
        OfflineFirstExpenseRepository(source, clock = clock).saveExpense(
            ExpenseEntry(
                id = "expense-1", shopId = SHOP_ID, entryDate = 7_000L,
                category = ExpenseCategory.OTHER, amountPaisa = 12_345L,
                createdAt = 7_000L, updatedAt = 7_000L,
            )
        )

        val uploadApi = FakeSyncApiClient()
        SyncEngine(source, uploadApi).pushPendingChanges().getOrThrow()
        val request = uploadApi.pushed.combine()
        val wireRequest = Json.decodeFromString<SyncPushRequestDto>(Json.encodeToString(request))

        val destination = createTestDatabase()
        seedShop(destination)
        val downloadApi = FakeSyncApiClient().apply {
            pullResponses += Result.success(
                SyncPullResponseDto(
                    next_seq = 1L,
                    server_sync_time = 20_000L,
                    parties = wireRequest.parties,
                    deals = wireRequest.deals,
                    transactions = wireRequest.transactions,
                    buy_entries = wireRequest.buy_entries,
                    sell_entries = wireRequest.sell_entries,
                    lend_entries = wireRequest.lend_entries,
                    lend_settlements = wireRequest.lend_settlements,
                    expenses = wireRequest.expenses,
                )
            )
        }
        SyncEngine(destination, downloadApi).pullRemoteChanges(SHOP_ID).getOrThrow()

        assertEquals(moneyTotals(source), moneyTotals(destination))
        assertEquals(7L, destination.appDatabaseQueries.getBuyEntryById("buy-1").executeAsOne().slip_no)
        assertEquals(null, destination.appDatabaseQueries.getLendEntryById("lend-1").executeAsOne().slip_no)
        assertEquals(
            40_000L,
            destination.appDatabaseQueries.getLendSettlementById("legacy-settlement").executeAsOne().principal_paisa,
        )
    }

    private suspend fun loanFixture(amountPaisa: Long): LoanFixture {
        val database = createTestDatabase()
        OfflineFirstPartyRepository(database).saveParty(party())
        val lendRepository = OfflineFirstLendRepository(database, clock = clock)
        var nextId = 0
        val useCase = LendTransactionUseCase(lendRepository, { "generated-${++nextId}" }, { 10_000L })
        val lend = useCase.lend(LendForm(SHOP_ID, PARTY_ID, LendType.MONEY, amountPaisa = amountPaisa))
        return LoanFixture(database, OfflineFirstCashTransactionRepository(database, clock = clock), useCase, lend)
    }

    private data class LoanFixture(
        val database: AppDatabase,
        val cashRepository: OfflineFirstCashTransactionRepository,
        val useCase: LendTransactionUseCase,
        val lend: LendEntry,
    ) {
        fun balance() = database.appDatabaseQueries.getPartyBalance(PARTY_ID).executeAsOne().balance_paisa ?: 0L
    }

    private fun seedShop(database: AppDatabase) {
        database.appDatabaseQueries.insertShopProfile(
            SHOP_ID, "Shop", "Owner", "Mandi", null, "9000000001", "hash", 1.5, 1L, 1L, 1L, 1L,
        )
    }

    private fun party() = Party(
        id = PARTY_ID, shopId = SHOP_ID, name = "Pilot party", phone = "9111111111",
        partyType = PartyType.FARMER, createdAt = 1_000L, updatedAt = 1_000L,
    )

    private fun buy() = BuyEntry(
        id = "buy-1", shopId = SHOP_ID, farmerId = PARTY_ID, commodityId = "commodity-1",
        entryDate = 2_000L, bagsCount = 10L, weightPerBagGrams = 50_000L,
        extraWeightGrams = 0L, totalWeightGrams = 500_000L, ratePaisaPerQuintal = 250_000L,
        commissionBps = 200L, labourPerBagPaisa = 500L, advancePaidPaisa = 0L,
        grossPaisa = 1_250_000L, commissionPaisa = 25_000L, labourPaisa = 5_000L,
        netPayablePaisa = 1_220_000L, slipNo = 7L, createdAt = 2_000L, updatedAt = 2_000L,
    )

    private fun sell() = SellEntry(
        id = "sell-1", shopId = SHOP_ID, buyerId = PARTY_ID, commodityId = "commodity-1",
        entryDate = 3_000L, billMode = BillMode.NORMAL, partyKind = SellPartyKind.MERCHANT,
        bagsCount = 10L, weightPerBagGrams = 50_000L, totalWeightGrams = 500_000L,
        ratePaisaPerQuintal = 270_000L, aratBps = 0L, tulakPaisa = 0L,
        grossPaisa = 1_350_000L, aratPaisa = 0L, mandiShulkPaisa = 0L,
        totalReceivablePaisa = 1_350_000L, receivedPaisa = 0L, balanceDuePaisa = 1_350_000L,
        slipNo = 8L, createdAt = 3_000L, updatedAt = 3_000L,
    )

    private fun lend() = LendEntry(
        id = "lend-1", shopId = SHOP_ID, partyId = PARTY_ID, lendType = LendType.MONEY,
        entryDate = 4_000L, amountPaisa = 100_000L, slipNo = 9L,
        createdAt = 4_000L, updatedAt = 4_000L,
    )

    private fun moneyTotals(database: AppDatabase): List<Long> {
        val q = database.appDatabaseQueries
        return listOf(
            q.getPartyBalance(PARTY_ID).executeAsOne().balance_paisa ?: 0L,
            q.getBuyEntryById("buy-1").executeAsOne().net_payable_paisa,
            q.getSellEntryById("sell-1").executeAsOne().total_receivable_paisa,
            q.getLendEntryById("lend-1").executeAsOne().amount_paisa,
            q.getLendSettlementById("legacy-settlement").executeAsOne().amount_paisa,
            q.getCashTransactionById("cash-1").executeAsOne().amount_paisa,
            q.getExpenseById("expense-1").executeAsOne().amount_paisa,
        )
    }

    private fun List<SyncPushRequestDto>.combine() = SyncPushRequestDto(
        parties = flatMap { it.parties },
        deals = flatMap { it.deals },
        transactions = flatMap { it.transactions },
        buy_entries = flatMap { it.buy_entries },
        sell_entries = flatMap { it.sell_entries },
        lend_entries = flatMap { it.lend_entries },
        lend_settlements = flatMap { it.lend_settlements },
        expenses = flatMap { it.expenses },
        revisions = flatMap { it.revisions },
    )

    private companion object {
        const val SHOP_ID = "shop-1"
        const val PARTY_ID = "party-1"
    }
}
