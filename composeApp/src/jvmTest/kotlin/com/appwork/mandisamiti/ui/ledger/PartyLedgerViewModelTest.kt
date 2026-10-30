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
import kotlinx.coroutines.async
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

    @Test
    fun voidingABuyEntryClearsItFromBalanceButKeepsItListedWithStrikethrough() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        val buyRepo = com.appwork.mandisamiti.data.repository.OfflineFirstBuyRepository(database, ioDispatcher = ioDispatcher)

        partyRepo.saveParty(Party(id = "farmer-1", shopId = "shop-1", name = "रामवीर सिंह", village = "राया", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L))
        buyRepo.saveBuyEntry(
            com.appwork.mandisamiti.domain.model.BuyEntry(
                id = "buy-1",
                shopId = "shop-1",
                farmerId = "farmer-1",
                commodityId = "गेहूं",
                entryDate = 1000L,
                bagsCount = 10,
                weightPerBagGrams = 50_000L,
                extraWeightGrams = 0L,
                totalWeightGrams = 500_000L,
                ratePaisaPerQuintal = 250000L,
                commissionBps = 0L,
                labourPerBagPaisa = 0L,
                advancePaidPaisa = 0L,
                grossPaisa = 1250000L,
                commissionPaisa = 0L,
                labourPaisa = 0L,
                netPayablePaisa = 1250000L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val viewModel = PartyLedgerViewModel(
            shopId = "shop-1", partyId = "farmer-1", partyRepository = partyRepo, cashRepository = cashRepo,
            dealRepository = dealRepo, shopProfileRepository = shopRepo, ttsManager = SoundboxTtsManager(),
            buyRepository = buyRepo,
            viewModelScope = backgroundScope
        )

        val item = viewModel.uiState.first { it.ledgerItems.isNotEmpty() && it.balancePaisa == -1250000L }.ledgerItems.single()
        assertTrue(item is LedgerItem.BuyItem)
        assertEquals(false, (item as LedgerItem.BuyItem).buy.isVoid)

        viewModel.voidEntry(item, VoidReason.WRONG_ENTRY)

        val after = viewModel.uiState.first { state ->
            state.balancePaisa == 0L && (state.ledgerItems.singleOrNull() as? LedgerItem.BuyItem)?.buy?.isVoid == true
        }
        val listed = after.ledgerItems.single() as LedgerItem.BuyItem
        assertTrue(listed.buy.isVoid)
        assertEquals("WRONG_ENTRY", listed.buy.voidReason)
    }

    @Test
    fun testPartyLedgerIncludesBuySellAndLendEntries() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        val buyRepo = com.appwork.mandisamiti.data.repository.OfflineFirstBuyRepository(database, ioDispatcher = ioDispatcher)
        val sellRepo = com.appwork.mandisamiti.data.repository.OfflineFirstSellRepository(database, ioDispatcher = ioDispatcher)
        val lendRepo = com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository(database, ioDispatcher = ioDispatcher)

        val farmer = Party(id = "farmer-1", shopId = "shop-1", name = "Bhagvan Singh", village = "Nagla", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L)
        partyRepo.saveParty(farmer)

        val buy = com.appwork.mandisamiti.domain.model.BuyEntry(
            id = "buy-1",
            shopId = "shop-1",
            farmerId = "farmer-1",
            commodityId = "गेहूं",
            entryDate = 2000L,
            bagsCount = 14,
            weightPerBagGrams = 50_000L,
            extraWeightGrams = 0L,
            totalWeightGrams = 700_000L,
            ratePaisaPerQuintal = 361600L,
            commissionBps = 0L,
            labourPerBagPaisa = 0L,
            advancePaidPaisa = 0L,
            grossPaisa = 2531200L,
            commissionPaisa = 0L,
            labourPaisa = 0L,
            netPayablePaisa = 2531200L,
            createdAt = 2000L,
            updatedAt = 2000L
        )
        buyRepo.saveBuyEntry(buy)

        val viewModel = PartyLedgerViewModel(
            shopId = "shop-1",
            partyId = "farmer-1",
            partyRepository = partyRepo,
            cashRepository = cashRepo,
            dealRepository = dealRepo,
            shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(),
            buyRepository = buyRepo,
            sellRepository = sellRepo,
            lendRepository = lendRepo,
            viewModelScope = backgroundScope
        )

        val item = viewModel.uiState.first { it.ledgerItems.isNotEmpty() }.ledgerItems.single()
        assertTrue(item is LedgerItem.BuyItem)
        assertEquals("buy-1", item.buy.id)
        assertEquals(2531200L, item.buy.netPayablePaisa)
    }

    @Test
    fun testApplyCashEntryToLoanSettlesLendAndUpdatesLedger() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        val lendRepo = com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository(database, ioDispatcher = ioDispatcher)

        val farmer = Party(id = "farmer-1", shopId = "shop-1", name = "Kailash", village = "Nagla", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L)
        partyRepo.saveParty(farmer)

        val lend = com.appwork.mandisamiti.domain.model.LendEntry(
            id = "lend-1",
            shopId = "shop-1",
            partyId = "farmer-1",
            lendType = com.appwork.mandisamiti.domain.model.LendType.MONEY,
            entryDate = 1000L,
            amountPaisa = 500_000L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        lendRepo.saveLendEntry(lend)

        val viewModel = PartyLedgerViewModel(
            shopId = "shop-1",
            partyId = "farmer-1",
            partyRepository = partyRepo,
            cashRepository = cashRepo,
            dealRepository = dealRepo,
            shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(),
            lendRepository = lendRepo,
            viewModelScope = backgroundScope
        )

        val stateWithLoan = viewModel.uiState.first { it.openLoans.isNotEmpty() }
        assertEquals(1, stateWithLoan.openLoans.size)
        assertEquals("lend-1", stateWithLoan.openLoans.first().lend.id)
        assertEquals(500_000L, stateWithLoan.openLoans.first().outstandingPaisa)

        viewModel.applyCashEntryToLoan(lendId = "lend-1", amountRs = 5000L, note = "पूर्ण भुगतान")

        val stateAfterSettle = viewModel.uiState.first { it.openLoans.isEmpty() }
        assertTrue(stateAfterSettle.openLoans.isEmpty())

        val updatedLend = lendRepo.getLendEntryById("lend-1")
        assertNotNull(updatedLend)
        assertEquals(com.appwork.mandisamiti.domain.model.LendStatus.SETTLED, updatedLend.status)

        val settlements = lendRepo.getSettlements("lend-1")
        assertEquals(1, settlements.size)
        assertEquals(500_000L, settlements.first().amountPaisa)
    }

    @Test
    fun repayLoanWritesPrincipalAndInterestRowsAndReportsBoth() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        val lendRepo = com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository(database, ioDispatcher = ioDispatcher)

        partyRepo.saveParty(Party(id = "farmer-1", shopId = "shop-1", name = "Kailash", village = "Nagla", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L))
        lendRepo.saveLendEntry(
            com.appwork.mandisamiti.domain.model.LendEntry(
                id = "lend-2", shopId = "shop-1", partyId = "farmer-1",
                lendType = com.appwork.mandisamiti.domain.model.LendType.MONEY,
                entryDate = 1000L, amountPaisa = 1_000_000L, createdAt = 1000L, updatedAt = 1000L
            )
        )

        val viewModel = PartyLedgerViewModel(
            shopId = "shop-1", partyId = "farmer-1", partyRepository = partyRepo, cashRepository = cashRepo,
            dealRepository = dealRepo, shopProfileRepository = shopRepo, ttsManager = SoundboxTtsManager(),
            lendRepository = lendRepo, viewModelScope = backgroundScope
        )
        viewModel.uiState.first { it.openLoans.isNotEmpty() }

        val event = async { viewModel.events.first() }
        viewModel.repayLoan("lend-2", paymentPaisa = 120_000L, interestPaisa = 20_000L, calculatedInterestPaisa = 20_000L)

        val recorded = event.await() as PartyLedgerEvent.TransactionRecorded
        assertTrue("200" in recorded.speechText, recorded.speechText)
        assertTrue("1000" in recorded.speechText || "1,000" in recorded.speechText, recorded.speechText)

        val s = lendRepo.getSettlements("lend-2").single()
        assertEquals(100_000L, s.principalPaisa)
        assertEquals(20_000L, s.interestPaisa)
        val state = viewModel.uiState.first { it.openLoans.firstOrNull()?.outstandingPaisa == 900_000L }
        assertEquals(900_000L, state.openLoans.single().outstandingPaisa)
    }

    @Test
    fun updatePartyDetailsUpdatesBuyerTaxFields() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)

        val buyer = Party(
            id = "buyer-1",
            shopId = "shop-1",
            name = "गोयल ब्रदर्स",
            village = "कोसी",
            phone = "9876543210",
            partyType = PartyType.BUYER,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        partyRepo.saveParty(buyer)

        val viewModel = PartyLedgerViewModel(
            shopId = "shop-1",
            partyId = "buyer-1",
            partyRepository = partyRepo,
            cashRepository = cashRepo,
            dealRepository = dealRepo,
            shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope
        )

        viewModel.uiState.first { it.party != null }

        viewModel.updatePartyDetails(
            name = "गोयल ब्रदर्स प्रा. लि.",
            village = "कोसी कलां",
            phone = "9876543210",
            gstin = "09BBBBB0000B1Z6",
            mandiLicense = "LIC-555"
        )

        val updated = viewModel.uiState.first { it.party?.gstin == "09BBBBB0000B1Z6" }.party
        assertNotNull(updated)
        assertEquals("गोयल ब्रदर्स प्रा. लि.", updated.name)
        assertEquals("कोसी कलां", updated.village)
        assertEquals("09BBBBB0000B1Z6", updated.gstin)
        assertEquals("LIC-555", updated.mandiLicense)

        val fromRepo = partyRepo.getPartyById("buyer-1")
        assertNotNull(fromRepo)
        assertEquals("09BBBBB0000B1Z6", fromRepo.gstin)
        assertEquals("LIC-555", fromRepo.mandiLicense)
    }
}

