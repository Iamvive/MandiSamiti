package com.appwork.mandisamiti.ui.ledger

import app.cash.turbine.test
import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstDealRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.math.InterestCalculation
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PartyLedgerViewModelTest {

    @Test
    fun testPartyLedgerCashEntryAndInterestAddition() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler) // repos and VM share the test scheduler: no real threads
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        val ttsManager = SoundboxTtsManager()

        val shopId = "shop-1"
        val farmerId = "farmer-1"

        shopRepo.saveShopProfile(
            ShopProfile(
                id = shopId,
                shopName = "श्री गणेश ट्रेडिंग",
                ownerName = "लाला जी",
                mandiName = "मथुरा मंडी",
                phoneNumber = "9837000000",
                pinHash = "1234",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val farmer = Party(
            id = farmerId,
            shopId = shopId,
            name = "रामवीर सिंह",
            village = "राया",
            partyType = PartyType.FARMER,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        partyRepo.saveParty(farmer)

        val viewModel = PartyLedgerViewModel(
            shopId = shopId,
            partyId = farmerId,
            partyRepository = partyRepo,
            cashRepository = cashRepo,
            dealRepository = dealRepo,
            shopProfileRepository = shopRepo,
            ttsManager = ttsManager,
            viewModelScope = backgroundScope
        )

        // 1. Record Cash Advance: ₹5,000 (UDHAR_GIVEN)
        viewModel.events.test {
            viewModel.recordCashEntry(
                transactionType = TransactionType.UDHAR_GIVEN,
                amountRs = 5000L,
                remarks = "खाद हेतु नकद"
            )
            val event = awaitItem()
            assertTrue(event is PartyLedgerEvent.TransactionRecorded)
            assertTrue((event as PartyLedgerEvent.TransactionRecorded).speechText.contains("रामवीर सिंह"))
            assertTrue(event.speechText.contains("5,000"))

            // 2. Add Rural Simple Interest: ₹75
            val interestResult = InterestCalculation(
                principalPaisa = 500000L,
                monthlyRatePercent = 1.5,
                daysElapsed = 30,
                approxMonthsElapsed = 1.0,
                accruedInterestPaisa = 7500L,
                totalPayablePaisa = 507500L
            )
            viewModel.recordCalculatedInterest(interestResult)
            val intEvent = awaitItem()
            assertTrue(intEvent is PartyLedgerEvent.TransactionRecorded)
            assertTrue((intEvent as PartyLedgerEvent.TransactionRecorded).speechText.contains("ब्याज"))

            cancelAndIgnoreRemainingEvents()
        }

        // Verify balance in repository stream
        partyRepo.getPartyBalanceStream(farmerId).test {
            val balance = awaitItem()
            assertNotNull(balance)
            assertEquals(507500L, balance.balancePaisa) // ₹5,000 + ₹75 interest = ₹5,075
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun voidingACashEntryClearsItFromBalanceButKeepsItListed() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        partyRepo.saveParty(Party(id = "farmer-1", shopId = "shop-1", name = "रामवीर सिंह", village = "राया", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L))
        cashRepo.recordTransaction(
            CashTransaction(
                id = "tx-1", shopId = "shop-1", partyId = "farmer-1", transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = 500_000L, transactionDate = 1L, createdAt = 1L, updatedAt = 1L
            )
        )
        val viewModel = PartyLedgerViewModel(
            shopId = "shop-1", partyId = "farmer-1", partyRepository = partyRepo, cashRepository = cashRepo,
            dealRepository = dealRepo, shopProfileRepository = shopRepo, ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope
        )
        val item = viewModel.uiState.first { it.ledgerItems.isNotEmpty() && it.balancePaisa == 500_000L }.ledgerItems.single()

        viewModel.voidEntry(item, VoidReason.WRONG_ENTRY)

        val after = viewModel.uiState.first { state ->
            // balance and list come from separate streams, so wait for both to reflect the void
            state.balancePaisa == 0L && (state.ledgerItems.singleOrNull() as? LedgerItem.CashItem)?.transaction?.isVoid == true
        }
        val listed = after.ledgerItems.single() as LedgerItem.CashItem
        assertTrue(listed.transaction.isVoid)
    }
}
