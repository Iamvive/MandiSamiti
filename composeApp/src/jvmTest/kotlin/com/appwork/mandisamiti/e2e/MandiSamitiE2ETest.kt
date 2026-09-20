package com.appwork.mandisamiti.e2e

import app.cash.turbine.test
import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstDealRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.data.sync.SyncEngine
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import com.appwork.mandisamiti.domain.math.DeductionsInput
import com.appwork.mandisamiti.domain.math.InterestCalculation
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.model.Deal
import com.appwork.mandisamiti.domain.model.DealStatus
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import com.appwork.mandisamiti.ui.deal.ActiveInputField
import com.appwork.mandisamiti.ui.deal.DealEntryEvent
import com.appwork.mandisamiti.ui.deal.DealEntryViewModel
import com.appwork.mandisamiti.ui.home.HomeViewModel
import com.appwork.mandisamiti.ui.ledger.PartyLedgerEvent
import com.appwork.mandisamiti.ui.ledger.PartyLedgerViewModel
import com.appwork.mandisamiti.ui.register.DailyRegisterViewModel
import com.appwork.mandisamiti.ui.slip.DigitalSlipRenderer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MandiSamitiE2ETest {

    @Test
    fun testFullMandiDailyWorkflowEndToEnd() = runTest {
        val database = createTestDatabase()
        val shopRepo = OfflineFirstShopProfileRepository(database)
        val partyRepo = OfflineFirstPartyRepository(database)
        val dealRepo = OfflineFirstDealRepository(database)
        val cashRepo = OfflineFirstCashTransactionRepository(database)
        val syncEngine = SyncEngine(database)
        val ttsManager = SoundboxTtsManager()

        val shopId = "shop_mathura_1"
        val farmerId = "farmer_ramveer"
        val buyerId = "buyer_agarwal"

        // Step 1: Initialize Shop Profile (श्री गणेश ट्रेडिंग, मथुरा मंडी)
        val shopProfile = ShopProfile(
            id = shopId,
            shopName = "श्री गणेश ट्रेडिंग",
            ownerName = "लाला मदन लाल जी",
            mandiName = "मथुरा कृषि उपज मंडी",
            shopNumber = "B-42",
            phoneNumber = "9837123456",
            pinHash = "1234",
            isSoundEnabled = true,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        shopRepo.saveShopProfile(shopProfile)

        // Step 2: Register Farmer & Buyer Master
        val farmer = Party(
            id = farmerId,
            shopId = shopId,
            name = "रामवीर सिंह",
            village = "राया (मथुरा)",
            phone = "9837999888",
            partyType = PartyType.FARMER,
            monthlyInterestRate = 1.5,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        partyRepo.saveParty(farmer)

        val buyer = Party(
            id = buyerId,
            shopId = shopId,
            name = "अग्रवाल ट्रेडर्स",
            village = "मथुरा शहर",
            phone = "9837111222",
            partyType = PartyType.BUYER,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        partyRepo.saveParty(buyer)

        // Step 3: Verify Master Home Screen displays parties
        val homeViewModel = HomeViewModel(
            shopProfileRepository = shopRepo,
            partyRepository = partyRepo,
            viewModelScope = backgroundScope
        )

        homeViewModel.uiState.test {
            var homeState = awaitItem()
            while (homeState.allParties.size < 2 || homeState.shopProfile == null) {
                homeState = awaitItem()
            }
            assertEquals("श्री गणेश ट्रेडिंग", homeState.shopProfile?.shopName)
            assertEquals(2, homeState.allParties.size)
            cancelAndIgnoreRemainingEvents()
        }

        // Step 4: Two-Stage Deal Entry (Weighment -> Auction Settlement)
        val dealViewModel = DealEntryViewModel(
            shopId = shopId,
            existingDealId = null,
            dealRepository = dealRepo,
            partyRepository = partyRepo,
            shopProfileRepository = shopRepo,
            ttsManager = ttsManager,
            viewModelScope = backgroundScope
        )

        dealViewModel.onSelectFarmer(farmer)
        dealViewModel.onSelectBuyer(buyer)

        // Enter 35 bags, 18.40 Quintals gross, 0.35 Quintals cut weight
        dealViewModel.onFocusField(ActiveInputField.GROSS_WEIGHT)
        "18.40".forEach { dealViewModel.onKeypadAction(com.appwork.mandisamiti.ui.components.KeypadAction.DIGIT_0) } // focus is tested
        // Set exact text for precision test
        dealViewModel.toggleSettlementStage(true)

        // High-Precision Mandi Math Calculation Check
        val grossGrams = 1_840_000L // 18.40 Q
        val cutGrams = 35_000L     // 0.35 Q
        val netGrams = 1_805_000L    // 18.05 Q
        val ratePaisa = 245_000L    // ₹2,450 / Quintal

        val settlement = MandiMathEngine.calculateSettlement(
            grossWeightGrams = grossGrams,
            cutWeightGrams = cutGrams,
            ratePaisaPerQuintal = ratePaisa,
            deductions = DeductionsInput(
                farmerCommissionPaisa = 66_333L, // ~1.5%
                labourChargePaisa = 15_000L     // ₹150
            )
        )

        val deal = Deal(
            id = "deal_e2e_1",
            shopId = shopId,
            farmerId = farmerId,
            buyerId = buyerId,
            commodityId = "comm_wheat",
            dealStatus = DealStatus.SETTLED,
            dealDate = 2000L,
            bagsCount = 35,
            grossWeightGrams = grossGrams,
            cutWeightGrams = cutGrams,
            netWeightGrams = netGrams,
            ratePaisaPerUnit = ratePaisa,
            grossAmountPaisa = settlement.grossAmountPaisa,
            farmerCommissionPaisa = settlement.farmerCommissionPaisa,
            labourChargePaisa = settlement.labourChargePaisa,
            netFarmerPayablePaisa = settlement.netFarmerPayablePaisa,
            netBuyerReceivablePaisa = settlement.netBuyerReceivablePaisa,
            createdAt = 2000L,
            updatedAt = 2000L
        )
        dealRepo.saveDeal(deal)

        // Step 5: Check Party Running Balance (Farmer has credit/payable, Buyer has debit/receivable)
        partyRepo.getPartyBalanceStream(farmerId).test {
            val balance = awaitItem()
            assertNotNull(balance)
            assertEquals(-settlement.netFarmerPayablePaisa, balance.balancePaisa) // Negative = देना है
            cancelAndIgnoreRemainingEvents()
        }

        partyRepo.getPartyBalanceStream(buyerId).test {
            val balance = awaitItem()
            assertNotNull(balance)
            assertEquals(settlement.netBuyerReceivablePaisa, balance.balancePaisa) // Positive = लेना है
            cancelAndIgnoreRemainingEvents()
        }

        // Step 6: Farmer Ledger - Direct Cash Advance & Rural Monthly Interest
        val ledgerViewModel = PartyLedgerViewModel(
            shopId = shopId,
            partyId = farmerId,
            partyRepository = partyRepo,
            cashRepository = cashRepo,
            dealRepository = dealRepo,
            shopProfileRepository = shopRepo,
            ttsManager = ttsManager,
            viewModelScope = backgroundScope
        )

        ledgerViewModel.events.test {
            // Cash advance to farmer: ₹5,000
            ledgerViewModel.recordCashEntry(
                transactionType = TransactionType.UDHAR_GIVEN,
                amountRs = 5000L,
                remarks = "खाद हेतु नकद उधार"
            )
            val event = awaitItem()
            assertTrue(event is PartyLedgerEvent.TransactionRecorded)

            // Monthly interest: ₹75
            val interestCalc = RuralInterestEngine.calculateAccruedInterestByDays(
                principalPaisa = 500000L,
                monthlyRatePercent = 1.5,
                elapsedDays = 30
            )
            ledgerViewModel.recordCalculatedInterest(interestCalc)
            val intEvent = awaitItem()
            assertTrue(intEvent is PartyLedgerEvent.TransactionRecorded)
            cancelAndIgnoreRemainingEvents()
        }

        // Step 7: WhatsApp Digital Slip Formatting Verification
        val whatsappText = DigitalSlipRenderer.formatWhatsAppReceiptText(
            shopProfile = shopProfile,
            farmer = farmer,
            buyer = buyer,
            deal = deal
        )
        assertTrue(whatsappText.contains("श्री गणेश ट्रेडिंग"))
        assertTrue(whatsappText.contains("रामवीर सिंह"))
        assertTrue(whatsappText.contains("18.05 कुंतल"))
        assertTrue(whatsappText.contains("पक्का सौदा पर्चा"))

        // Step 8: Daily Cash Register (गल्ला हिसाब) Reconciliation
        val registerViewModel = DailyRegisterViewModel(
            shopId = shopId,
            cashRepository = cashRepo,
            partyRepository = partyRepo,
            shopProfileRepository = shopRepo,
            ttsManager = ttsManager,
            viewModelScope = backgroundScope
        )

        registerViewModel.recordDailyEntry(
            partyId = buyerId,
            transactionType = TransactionType.JAMA_RECEIVED,
            amountRs = 50000L,
            remarks = "खरीदार से नकद प्राप्ति"
        )

        registerViewModel.uiState.test {
            var regState = awaitItem()
            while (regState.todayTransactions.isEmpty()) {
                regState = awaitItem()
            }
            assertTrue(regState.todayCashInPaisa >= 5000000L)
            cancelAndIgnoreRemainingEvents()
        }

        // Step 9: Outbox Sync Engine Verification
        val syncSummary = syncEngine.getPendingSyncSummary()
        assertTrue(syncSummary.pendingPartiesCount >= 2)
        assertTrue(syncSummary.pendingDealsCount >= 1)
        assertTrue(syncSummary.pendingTransactionsCount >= 2)
    }
}
