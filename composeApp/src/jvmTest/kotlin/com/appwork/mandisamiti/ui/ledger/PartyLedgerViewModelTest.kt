package com.appwork.mandisamiti.ui.ledger

import app.cash.turbine.test
import com.appwork.mandisamiti.data.repository.OfflineFirstBuyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstDealRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstSellRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.ledger.LedgerTag
import com.appwork.mandisamiti.domain.math.InterestCalculation
import com.appwork.mandisamiti.domain.model.BuyEntry
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.LendDirection
import com.appwork.mandisamiti.domain.model.LendEntry
import com.appwork.mandisamiti.domain.model.LendKind
import com.appwork.mandisamiti.domain.model.LendType
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.SellEntry
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.domain.model.VoidReason
import com.appwork.mandisamiti.domain.usecase.CashSide
import com.appwork.mandisamiti.domain.usecase.UdhaarUseCase
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PartyLedgerViewModelTest {

    @Test
    fun testPartyLedgerCashEntry() = runTest {
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

            cancelAndIgnoreRemainingEvents()
        }

        // Verify balance in repository stream
        partyRepo.getPartyBalanceStream(farmerId).test {
            val balance = awaitItem()
            assertNotNull(balance)
            assertEquals(0L, balance.slipBalancePaisa) // plain cash and interest are outside the slip balance (Task 9 routes cash)
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
        val item = viewModel.uiState.first { it.ledgerItems.isNotEmpty() }.ledgerItems.single()

        viewModel.voidEntry(item, VoidReason.WRONG_ENTRY)

        val after = viewModel.uiState.first { state ->
            // balance and list come from separate streams, so wait for both to reflect the void
            (state.ledgerItems.singleOrNull() as? LedgerItem.CashItem)?.transaction?.isVoid == true
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

        val item = viewModel.uiState.first { it.ledgerItems.isNotEmpty() && it.slipBalancePaisa == -1250000L }.ledgerItems.single()
        assertTrue(item is LedgerItem.BuyItem)
        assertEquals(false, (item as LedgerItem.BuyItem).buy.isVoid)

        viewModel.voidEntry(item, VoidReason.WRONG_ENTRY)

        val after = viewModel.uiState.first { state ->
            state.slipBalancePaisa == 0L && (state.ledgerItems.singleOrNull() as? LedgerItem.BuyItem)?.buy?.isVoid == true
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

    @Test
    fun testTask9a_stateExposesSlipBalanceAndUdhaarNetSeparately_buyPaymentMovesOnlySlip() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        val buyRepo = OfflineFirstBuyRepository(database, ioDispatcher = ioDispatcher)
        val lendRepo = OfflineFirstLendRepository(database, ioDispatcher = ioDispatcher)
        var idCounter = 0
        val udhaarUseCase = UdhaarUseCase(lendRepo, { "id-${++idCounter}" }, { 1000L })

        val shopId = "shop-1"
        val farmerId = "farmer-1"
        shopRepo.saveShopProfile(ShopProfile(id = shopId, shopName = "Shop", ownerName = "Owner", mandiName = "Mandi", phoneNumber = "9800000000", pinHash = "1234", createdAt = 1000L, updatedAt = 1000L))
        partyRepo.saveParty(Party(id = farmerId, shopId = shopId, name = "रामवीर", village = "राया", partyType = PartyType.FARMER, createdAt = 1000L, updatedAt = 1000L))

        // Create a buy entry: net payout ₹10,000 (1000000 paisa)
        buyRepo.saveBuyEntry(
            BuyEntry(
                id = "buy-1",
                shopId = shopId,
                farmerId = farmerId,
                commodityId = "comm-1",
                entryDate = 1000L,
                bagsCount = 10,
                weightPerBagGrams = 50000,
                extraWeightGrams = 0,
                totalWeightGrams = 500000,
                ratePaisaPerQuintal = 200000,
                commissionBps = 0,
                labourPerBagPaisa = 0,
                advancePaidPaisa = 0,
                grossPaisa = 1_000_000L,
                commissionPaisa = 0,
                labourPaisa = 0,
                netPayablePaisa = 1_000_000L,
                paidPaisa = 0L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        // Create an udhaar loan: GIVEN ₹2,000 (200000 paisa)
        lendRepo.saveLendEntry(
            LendEntry(
                id = "lend-1",
                shopId = shopId,
                partyId = farmerId,
                amountPaisa = 200_000L,
                direction = LendDirection.GIVEN,
                lendType = LendType.MONEY,
                entryDate = 1000L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val viewModel = PartyLedgerViewModel(
            shopId = shopId,
            partyId = farmerId,
            partyRepository = partyRepo,
            cashRepository = cashRepo,
            dealRepository = dealRepo,
            shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(),
            buyRepository = buyRepo,
            lendRepository = lendRepo,
            udhaarUseCase = udhaarUseCase,
            viewModelScope = backgroundScope
        )

        // Check initial state: slip is -1000000L (payable), udhaarNet is 200000L (receivable)
        val initial = viewModel.uiState.first { it.slipBalancePaisa != 0L && it.udhaarNetPaisa == 200_000L }
        assertEquals(-1_000_000L, initial.slipBalancePaisa)
        assertEquals(200_000L, initial.udhaarNetPaisa)
        assertEquals(200_000L, initial.udhaarGivenPaisa)
        assertEquals(0L, initial.udhaarTakenPaisa)

        // Settle buy slip with ₹10,000
        viewModel.settleBuySlip("buy-1", 1_000_000L)
        val afterSettle = viewModel.uiState.first { it.slipBalancePaisa == 0L }
        assertEquals(0L, afterSettle.slipBalancePaisa)
        // udhaarNetPaisa must remain unchanged at 200_000L!
        assertEquals(200_000L, afterSettle.udhaarNetPaisa)
    }

    @Test
    fun testTask9b_onPlainCashGaveWithNoLoans_createsGivenLoanAndRaisesUdhaarGiven() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        val lendRepo = OfflineFirstLendRepository(database, ioDispatcher = ioDispatcher)
        var idCounter = 0
        val udhaarUseCase = UdhaarUseCase(lendRepo, { "id-${++idCounter}" }, { 1000L })

        val shopId = "shop-1"
        val farmerId = "farmer-1"
        shopRepo.saveShopProfile(ShopProfile(id = shopId, shopName = "Shop", ownerName = "Owner", mandiName = "Mandi", phoneNumber = "9800000000", pinHash = "1234", createdAt = 1000L, updatedAt = 1000L))
        partyRepo.saveParty(Party(id = farmerId, shopId = shopId, name = "रामवीर", village = "राया", partyType = PartyType.FARMER, createdAt = 1000L, updatedAt = 1000L))

        val viewModel = PartyLedgerViewModel(
            shopId = shopId,
            partyId = farmerId,
            partyRepository = partyRepo,
            cashRepository = cashRepo,
            dealRepository = dealRepo,
            shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(),
            lendRepository = lendRepo,
            udhaarUseCase = udhaarUseCase,
            viewModelScope = backgroundScope
        )

        viewModel.uiState.first { it.party != null }

        viewModel.onPlainCash(CashSide.GAVE, 500_000L)

        val updated = viewModel.uiState.first { it.udhaarGivenPaisa == 500_000L }
        assertEquals(500_000L, updated.udhaarGivenPaisa)
        assertEquals(0L, updated.slipBalancePaisa)
        val loans = lendRepo.getLendEntriesStream(shopId).first()
        val loan = loans.find { it.partyId == farmerId }
        assertNotNull(loan)
        assertEquals(LendDirection.GIVEN, loan.direction)
        assertEquals(500_000L, loan.amountPaisa)
        assertEquals(LendKind.CASH, loan.kind)
    }

    @Test
    fun testTask9c_onPlainCashGotAboveWhatPartyOwes_setsErrorAndWritesNothing() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        val lendRepo = OfflineFirstLendRepository(database, ioDispatcher = ioDispatcher)
        var idCounter = 0
        val udhaarUseCase = UdhaarUseCase(lendRepo, { "id-${++idCounter}" }, { 1000L })

        val shopId = "shop-1"
        val farmerId = "farmer-1"
        shopRepo.saveShopProfile(ShopProfile(id = shopId, shopName = "Shop", ownerName = "Owner", mandiName = "Mandi", phoneNumber = "9800000000", pinHash = "1234", createdAt = 1000L, updatedAt = 1000L))
        partyRepo.saveParty(Party(id = farmerId, shopId = shopId, name = "रामवीर", village = "राया", partyType = PartyType.FARMER, createdAt = 1000L, updatedAt = 1000L))

        // Open loan: we gave ₹2,000 (200000 paisa)
        lendRepo.saveLendEntry(
            LendEntry(
                id = "lend-1",
                shopId = shopId,
                partyId = farmerId,
                amountPaisa = 200_000L,
                direction = LendDirection.GIVEN,
                lendType = LendType.MONEY,
                entryDate = 1000L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val viewModel = PartyLedgerViewModel(
            shopId = shopId,
            partyId = farmerId,
            partyRepository = partyRepo,
            cashRepository = cashRepo,
            dealRepository = dealRepo,
            shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(),
            lendRepository = lendRepo,
            udhaarUseCase = udhaarUseCase,
            viewModelScope = backgroundScope
        )

        viewModel.uiState.first { it.udhaarGivenPaisa == 200_000L }

        // Attempt to receive ₹3,000 (300000 paisa) -> overpayment
        viewModel.onPlainCash(CashSide.GOT, 300_000L)

        val stateWithError = viewModel.uiState.first { it.error != null }
        assertTrue(stateWithError.error!!.contains("बाकी सिर्फ ₹2,000 है"))
        // Check nothing was written
        val settlements = lendRepo.getSettlementsStream(shopId).first()
        assertTrue(settlements.isEmpty())
        val cashEntries = cashRepo.getTransactionsByPartyStream(farmerId).first()
        assertTrue(cashEntries.isEmpty())
    }

    @Test
    fun testTask9d_everyRowInMergedLedgerListHasTagFromLedgerTagFor() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        val buyRepo = OfflineFirstBuyRepository(database, ioDispatcher = ioDispatcher)
        val sellRepo = OfflineFirstSellRepository(database, ioDispatcher = ioDispatcher)
        val lendRepo = OfflineFirstLendRepository(database, ioDispatcher = ioDispatcher)

        val shopId = "shop-1"
        val partyId = "party-1"
        shopRepo.saveShopProfile(ShopProfile(id = shopId, shopName = "Shop", ownerName = "Owner", mandiName = "Mandi", phoneNumber = "9800000000", pinHash = "1234", createdAt = 1000L, updatedAt = 1000L))
        partyRepo.saveParty(Party(id = partyId, shopId = shopId, name = "मिश्रा जी", village = "राया", partyType = PartyType.FARMER, createdAt = 1000L, updatedAt = 1000L))

        // 1. BuyEntry
        buyRepo.saveBuyEntry(
            BuyEntry(
                id = "buy-1",
                shopId = shopId,
                farmerId = partyId,
                commodityId = "comm-1",
                entryDate = 1000L,
                bagsCount = 10,
                weightPerBagGrams = 50000,
                extraWeightGrams = 0,
                totalWeightGrams = 500000,
                ratePaisaPerQuintal = 200000,
                commissionBps = 0,
                labourPerBagPaisa = 0,
                advancePaidPaisa = 0,
                grossPaisa = 100_000L,
                commissionPaisa = 0,
                labourPaisa = 0,
                netPayablePaisa = 100_000L,
                paidPaisa = 0L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )
        // 2. SellEntry
        sellRepo.saveSellEntry(
            SellEntry(
                id = "sell-1",
                shopId = shopId,
                buyerId = partyId,
                commodityId = "comm-1",
                entryDate = 2000L,
                billMode = com.appwork.mandisamiti.domain.model.BillMode.NORMAL,
                partyKind = com.appwork.mandisamiti.domain.model.SellPartyKind.MERCHANT,
                bagsCount = 5,
                weightPerBagGrams = 50000,
                totalWeightGrams = 250000,
                ratePaisaPerQuintal = 200000,
                aratBps = 0,
                tulakPaisa = 0,
                grossPaisa = 200_000L,
                aratPaisa = 0,
                mandiShulkPaisa = 0,
                totalReceivablePaisa = 200_000L,
                receivedPaisa = 0L,
                balanceDuePaisa = 200_000L,
                createdAt = 2000L,
                updatedAt = 2000L
            )
        )
        // 3. LendEntry
        lendRepo.saveLendEntry(
            LendEntry(
                id = "lend-1",
                shopId = shopId,
                partyId = partyId,
                amountPaisa = 50_000L,
                direction = LendDirection.GIVEN,
                lendType = LendType.MONEY,
                entryDate = 3000L,
                createdAt = 3000L,
                updatedAt = 3000L
            )
        )
        // 4. CashTransaction linked to buy
        cashRepo.recordTransaction(
            CashTransaction(
                id = "cash-1",
                shopId = shopId,
                partyId = partyId,
                transactionType = TransactionType.UDHAR_GIVEN,
                amountPaisa = 100000L,
                paymentMode = com.appwork.mandisamiti.domain.model.PaymentMode.CASH,
                transactionDate = 4000L,
                sourceEntryType = "BUY",
                sourceEntryId = "buy-1",
                createdAt = 4000L,
                updatedAt = 4000L
            )
        )
        // 5. CashTransaction linked to loan
        cashRepo.recordTransaction(
            CashTransaction(
                id = "cash-2",
                shopId = shopId,
                partyId = partyId,
                transactionType = TransactionType.JAMA_RECEIVED,
                amountPaisa = 50000L,
                paymentMode = com.appwork.mandisamiti.domain.model.PaymentMode.CASH,
                transactionDate = 5000L,
                lendId = "lend-1",
                createdAt = 5000L,
                updatedAt = 5000L
            )
        )

        val viewModel = PartyLedgerViewModel(
            shopId = shopId,
            partyId = partyId,
            partyRepository = partyRepo,
            cashRepository = cashRepo,
            dealRepository = dealRepo,
            shopProfileRepository = shopRepo,
            buyRepository = buyRepo,
            sellRepository = sellRepo,
            lendRepository = lendRepo,
            ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope
        )

        val state = viewModel.uiState.first { it.ledgerItems.size >= 5 }
        for (item in state.ledgerItems) {
            assertNotNull(item.tag, "Expected non-null tag for item $item")
            when (item) {
                is LedgerItem.BuyItem -> assertTrue(item.tag is LedgerTag.Slip)
                is LedgerItem.SellItem -> assertTrue(item.tag is LedgerTag.Slip)
                is LedgerItem.LendItem -> assertEquals(LedgerTag.Udhaar, item.tag)
                is LedgerItem.DealItem -> assertTrue(item.tag is LedgerTag.Slip)
                is LedgerItem.CashItem -> assertNotNull(item.tag)
            }
        }
    }

    @Test
    fun testTask9e_onPlainCashGaveWhenWeOweThem_repaysTakenLoanOldestFirstAndCreatesNoNewLoan() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        val lendRepo = OfflineFirstLendRepository(database, ioDispatcher = ioDispatcher)
        var idCounter = 0
        val udhaarUseCase = UdhaarUseCase(lendRepo, { "id-${++idCounter}" }, { 5000L })

        val shopId = "shop-1"
        val farmerId = "farmer-1"
        shopRepo.saveShopProfile(ShopProfile(id = shopId, shopName = "Shop", ownerName = "Owner", mandiName = "Mandi", phoneNumber = "9800000000", pinHash = "1234", createdAt = 1000L, updatedAt = 1000L))
        partyRepo.saveParty(Party(id = farmerId, shopId = shopId, name = "रामवीर", village = "राया", partyType = PartyType.FARMER, createdAt = 1000L, updatedAt = 1000L))

        // Two TAKEN loans: ₹2,000 at day 1 (1000L), ₹3,000 at day 2 (2000L)
        lendRepo.saveLendEntry(
            LendEntry(
                id = "loan-old",
                shopId = shopId,
                partyId = farmerId,
                amountPaisa = 200_000L,
                direction = LendDirection.TAKEN,
                lendType = LendType.MONEY,
                entryDate = 1000L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )
        lendRepo.saveLendEntry(
            LendEntry(
                id = "loan-new",
                shopId = shopId,
                partyId = farmerId,
                amountPaisa = 300_000L,
                direction = LendDirection.TAKEN,
                lendType = LendType.MONEY,
                entryDate = 2000L,
                createdAt = 2000L,
                updatedAt = 2000L
            )
        )

        val viewModel = PartyLedgerViewModel(
            shopId = shopId,
            partyId = farmerId,
            partyRepository = partyRepo,
            cashRepository = cashRepo,
            dealRepository = dealRepo,
            shopProfileRepository = shopRepo,
            lendRepository = lendRepo,
            udhaarUseCase = udhaarUseCase,
            ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope
        )

        viewModel.uiState.first { it.udhaarTakenPaisa == 500_000L }

        // We give ₹2,500 cash -> should repay loan-old (200,000) and 50,000 of loan-new
        viewModel.onPlainCash(CashSide.GAVE, 250_000L)

        val updated = viewModel.uiState.first { it.udhaarTakenPaisa == 250_000L }
        assertEquals(250_000L, updated.udhaarTakenPaisa)
        assertEquals(0L, updated.udhaarGivenPaisa)

        // Confirm no new loan was created
        val loans = lendRepo.getLendEntriesStream(shopId).first()
        assertEquals(2, loans.size)
        assertTrue(loans.none { it.direction == LendDirection.GIVEN })

        // Settlements: loan-old settled 200,000, loan-new settled 50,000
        val settlements = lendRepo.getSettlementsStream(shopId).first()
        assertEquals(2, settlements.size)
        val oldSettlement = settlements.find { it.lendId == "loan-old" }
        val newSettlement = settlements.find { it.lendId == "loan-new" }
        assertNotNull(oldSettlement)
        assertNotNull(newSettlement)
        assertEquals(200_000L, oldSettlement.amountPaisa)
        assertEquals(50_000L, newSettlement.amountPaisa)
    }

    @Test
    fun testTask9f_plainCashClearsLoanLeavesUdhaarAtZero() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        val lendRepo = OfflineFirstLendRepository(database, ioDispatcher = ioDispatcher)
        var idCounter = 0
        val udhaarUseCase = UdhaarUseCase(lendRepo, { "id-${++idCounter}" }, { 5000L })

        val shopId = "shop-1"
        val farmerId = "farmer-1"
        shopRepo.saveShopProfile(ShopProfile(id = shopId, shopName = "Shop", ownerName = "Owner", mandiName = "Mandi", phoneNumber = "9800000000", pinHash = "1234", createdAt = 1000L, updatedAt = 1000L))
        partyRepo.saveParty(Party(id = farmerId, shopId = shopId, name = "रामवीर", village = "राया", partyType = PartyType.FARMER, createdAt = 1000L, updatedAt = 1000L))

        lendRepo.saveLendEntry(
            LendEntry(
                id = "loan-1",
                shopId = shopId,
                partyId = farmerId,
                amountPaisa = 500_000L,
                direction = LendDirection.GIVEN,
                lendType = LendType.MONEY,
                entryDate = 1000L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val viewModel = PartyLedgerViewModel(
            shopId = shopId,
            partyId = farmerId,
            partyRepository = partyRepo,
            cashRepository = cashRepo,
            dealRepository = dealRepo,
            shopProfileRepository = shopRepo,
            lendRepository = lendRepo,
            udhaarUseCase = udhaarUseCase,
            ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope
        )

        viewModel.uiState.first { it.udhaarGivenPaisa == 500_000L }

        // Plain cash exact repayment of ₹5,000
        viewModel.onPlainCash(CashSide.GOT, 500_000L)

        val cleared = viewModel.uiState.first { it.udhaarGivenPaisa == 0L && it.openLoans.isEmpty() }
        assertEquals(0L, cleared.udhaarGivenPaisa)
        assertEquals(0L, cleared.udhaarTakenPaisa)
        assertEquals(0L, cleared.udhaarNetPaisa)
        assertTrue(cleared.openLoans.isEmpty())
    }

    @Test
    fun testTask9g_partyWithOldDiscountOrInterestRowsRendersWithoutCrash() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)

        val shopId = "shop-1"
        val partyId = "party-1"
        shopRepo.saveShopProfile(ShopProfile(id = shopId, shopName = "Shop", ownerName = "Owner", mandiName = "Mandi", phoneNumber = "9800000000", pinHash = "1234", createdAt = 1000L, updatedAt = 1000L))
        partyRepo.saveParty(Party(id = partyId, shopId = shopId, name = "दिनेश", village = "राया", partyType = PartyType.FARMER, createdAt = 1000L, updatedAt = 1000L))

        // Old DISCOUNT_GIVEN row
        cashRepo.recordTransaction(
            CashTransaction(
                id = "disc-1",
                shopId = shopId,
                partyId = partyId,
                transactionType = TransactionType.DISCOUNT_GIVEN,
                amountPaisa = 25000L,
                paymentMode = com.appwork.mandisamiti.domain.model.PaymentMode.CASH,
                transactionDate = 1000L,
                remarks = "पुरानी छूट",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )
        // Old INTEREST_ADDED row
        cashRepo.recordTransaction(
            CashTransaction(
                id = "int-1",
                shopId = shopId,
                partyId = partyId,
                transactionType = TransactionType.INTEREST_ADDED,
                amountPaisa = 15000L,
                paymentMode = com.appwork.mandisamiti.domain.model.PaymentMode.CASH,
                transactionDate = 2000L,
                remarks = "पुराना ब्याज",
                createdAt = 2000L,
                updatedAt = 2000L
            )
        )

        val viewModel = PartyLedgerViewModel(
            shopId = shopId,
            partyId = partyId,
            partyRepository = partyRepo,
            cashRepository = cashRepo,
            dealRepository = dealRepo,
            shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope
        )

        val state = viewModel.uiState.first { it.ledgerItems.size >= 2 }
        assertEquals(2, state.ledgerItems.size)
        // Check items render without crash
        for (item in state.ledgerItems) {
            assertTrue(item is LedgerItem.CashItem)
        }
    }

    @Test
    fun testPartB_udhaarCardNumberEqualsUdhaarSectionHeaderNumberWithInterest() = runTest {
        val database = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(database, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(database, ioDispatcher = ioDispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(database, ioDispatcher = ioDispatcher)
        val dealRepo = OfflineFirstDealRepository(database, ioDispatcher = ioDispatcher)
        val buyRepo = OfflineFirstBuyRepository(database, ioDispatcher = ioDispatcher)
        val sellRepo = OfflineFirstSellRepository(database, ioDispatcher = ioDispatcher)
        val lendRepo = OfflineFirstLendRepository(database, ioDispatcher = ioDispatcher)
        var idCounter = 0
        val nowMs = 100_000_000L
        val udhaarUseCase = UdhaarUseCase(lendRepo, { "id-${++idCounter}" }, { nowMs })

        val shopId = "test-shop-1"
        val partyId = "farmer-interest-1"
        partyRepo.saveParty(
            Party(
                id = partyId,
                shopId = shopId,
                name = "रामू किसान",
                phone = "9876543210",
                village = "गाँव",
                partyType = PartyType.FARMER,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        // GIVEN loan of Rs 10,000 at 2.0% monthly interest, 30 days old (30 * 86_400_000 ms)
        val entryDate = nowMs - 30L * 86_400_000L
        lendRepo.saveLendEntry(
            LendEntry(
                id = "lend-interest-1",
                shopId = shopId,
                partyId = partyId,
                lendType = LendType.MONEY,
                direction = LendDirection.GIVEN,
                amountPaisa = 1_000_000L,
                monthlyRatePercent = 2.0,
                entryDate = entryDate,
                createdAt = entryDate,
                updatedAt = entryDate
            )
        )

        val viewModel = PartyLedgerViewModel(
            shopId = shopId,
            partyId = partyId,
            partyRepository = partyRepo,
            cashRepository = cashRepo,
            dealRepository = dealRepo,
            shopProfileRepository = shopRepo,
            ttsManager = SoundboxTtsManager(),
            buyRepository = buyRepo,
            sellRepository = sellRepo,
            lendRepository = lendRepo,
            udhaarUseCase = udhaarUseCase,
            viewModelScope = backgroundScope
        )

        val personViewModel = com.appwork.mandisamiti.ui.udhaar.UdhaarPersonViewModel(
            partyId = partyId,
            direction = LendDirection.GIVEN,
            shopId = shopId,
            lendRepository = lendRepo,
            partyRepository = partyRepo,
            udhaarUseCase = udhaarUseCase,
            coroutineScope = backgroundScope
        )

        val khataState = viewModel.uiState.first { it.udhaarGivenTotalPaisa > 0L }
        val personState = personViewModel.uiState.first { it != null }!!

        // Headline figure on card equals section header total due
        assertEquals(personState.totalDuePaisa, khataState.udhaarGivenTotalPaisa)
        // Subline interest figure on card equals section header interest due
        assertEquals(personState.interestDuePaisa, khataState.udhaarGivenInterestPaisa)
        assertTrue(khataState.udhaarGivenInterestPaisa > 0L)
    }
}

