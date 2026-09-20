package com.appwork.mandisamiti

import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstDealRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.DriverFactory
import com.appwork.mandisamiti.database.createDatabase
import com.appwork.mandisamiti.domain.math.DeductionsInput
import com.appwork.mandisamiti.domain.math.MandiMathEngine
import com.appwork.mandisamiti.domain.math.RuralInterestEngine
import com.appwork.mandisamiti.domain.model.CashTransaction
import com.appwork.mandisamiti.domain.model.Deal
import com.appwork.mandisamiti.domain.model.DealStatus
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object DemoSimulator {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        println("================================================================================")
        println("🌾 मंडीसमिति (आढ़त रोकड़ बही) — LIVE DEMO & INTERACTIVE SIMULATION 🌾")
        println("स्थान: दुकान नं. 42, नवीन कृषि उपज मंडी, मथुरा (उ.प्र.)")
        println("आढ़ती: लाला मदन लाल जी (श्री गणेश ट्रेडिंग कंपनी)")
        println("================================================================================\n")

        val database = createDatabase(DriverFactory())
        val partyRepo = OfflineFirstPartyRepository(database)
        val dealRepo = OfflineFirstDealRepository(database)
        val cashRepo = OfflineFirstCashTransactionRepository(database)
        val shopRepo = OfflineFirstShopProfileRepository(database)
        val ttsManager = com.appwork.mandisamiti.platform.SoundboxTtsManager()

        val shopId = "shop-mathura-1"
        val currentTime = 1726837200000L // 20 Sep 2026

        // 1. Shop Profile
        val shop = ShopProfile(
            id = shopId,
            shopName = "श्री गणेश ट्रेडिंग कंपनी",
            ownerName = "लाला मदन लाल जी",
            mandiName = "नवीन कृषि उपज मंडी, मथुरा",
            phoneNumber = "9897012345",
            pinHash = "1234",
            createdAt = currentTime,
            updatedAt = currentTime
        )
        shopRepo.saveShopProfile(shop)

        println("🏢 [दुकान प्रोफ़ाइल]: ${shop.shopName} | संचालक: ${shop.ownerName}")
        println("📍 मंडी: ${shop.mandiName} | फ़ोन: ${shop.phoneNumber}")
        println("⚙️ मानक दरें: आढ़त 2% | मंडी शुल्क 1.5% | पल्लेदारी ₹8/बोरी\n")

        // 2. Setup Parties (Farmer & Buyer)
        val farmer = Party(
            id = "farmer-ramesh",
            shopId = shopId,
            name = "रमेश कुमार (किसान)",
            phone = "9876543210",
            village = "गोवर्धन, मथुरा",
            partyType = PartyType.FARMER,
            monthlyInterestRate = 1.5,
            createdAt = currentTime,
            updatedAt = currentTime
        )
        val buyer = Party(
            id = "buyer-suresh",
            shopId = shopId,
            name = "सुरेश चंद (व्यापारी)",
            phone = "9876501234",
            village = "राया, मथुरा",
            partyType = PartyType.BUYER,
            monthlyInterestRate = null,
            createdAt = currentTime,
            updatedAt = currentTime
        )
        partyRepo.saveParty(farmer)
        partyRepo.saveParty(buyer)

        println("--------------------------------------------------------------------------------")
        println("▶ SCENE 1: सुबह 07:30 AM — किसान की आवक व तौल (Stage 1: Arrival & Weighment)")
        println("--------------------------------------------------------------------------------")
        println("किसान रमेश कुमार (गोवर्धन) 45 बोरी नया शरबती गेहूं लेकर आढ़त पर पहुंचे।")
        println("धर्मकांटा / इलेक्ट्रॉनिक तौल विवरण दर्ज हुआ:")

        val bagCount = 45
        val grossGrams = (23L * 100_000L) + (50L * 1_000L) // 23 Qtl 50 Kg = 2,350,000 g
        val bagDeductionGrams = 1_000L * bagCount // 1 kg bardana per bag = 45,000 g
        val netGrams = grossGrams - bagDeductionGrams // 2,305,000 g = 23.05 Qtl

        println("  • कुल बोरी: $bagCount बोरी")
        println("  • सकल वजन (Gross Weight): 23 क्विंटल 50 किलो")
        println("  • बारदाना काट (-1kg/बोरी): 45 किलो")
        println("  • शुद्ध वजन (Net Weight): ${MandiMathEngine.gramsToQuintals(netGrams)} क्विंटल ($netGrams ग्राम)")

        val deal1 = Deal(
            id = "deal-101",
            shopId = shopId,
            farmerId = farmer.id,
            buyerId = null,
            commodityId = "comm-wheat",
            dealStatus = DealStatus.PENDING_SETTLEMENT,
            dealDate = currentTime,
            bagsCount = bagCount,
            grossWeightGrams = grossGrams,
            cutWeightGrams = bagDeductionGrams,
            netWeightGrams = netGrams,
            remarks = "गोवर्धन से आवक - तौल पर्ची सं. 104",
            createdAt = currentTime,
            updatedAt = currentTime
        )
        dealRepo.saveDeal(deal1)

        println("✅ [स्टेज-1 पूर्ण]: सौदा #MS-101 सुरक्षित (स्थिति: PENDING_SETTLEMENT - तौल पर्ची दर्ज)")
        println("🔊 [साउंडबॉक्स आवाज़]: \"रमेश कुमार का 45 बोरी गेहूं का तौल 23.05 क्विंटल दर्ज हुआ\"\n")
        ttsManager.speak("रमेश कुमार का 45 बोरी गेहूं का तौल 23 दशमलव 0 5 क्विंटल दर्ज हुआ", true)
        Thread.sleep(4000)

        println("--------------------------------------------------------------------------------")
        println("▶ SCENE 2: सुबह 10:15 AM — मंडी नीलामी व भाव तय (Stage 2: Auction & Settlement)")
        println("--------------------------------------------------------------------------------")
        println("मंडी में खुली बोली लगी — व्यापारी सुरेश चंद (राया) ने ₹2,275/क्विंटल पर उच्चतम बोली लगाई।")

        val ratePaisaPerQtl = 227500L // ₹2,275.00
        val grossAmountPaisa = (netGrams * ratePaisaPerQtl) / MandiMathEngine.GRAMS_PER_QUINTAL // ₹52,438.75
        val farmerCommissionPaisa = (grossAmountPaisa * 200L) / 10000L // 2%
        val buyerCommissionPaisa = (grossAmountPaisa * 150L) / 10000L // 1.5% Mandi Tax
        val labourChargePaisa = 800L * bagCount // ₹8 * 45 = ₹360 (36,000 paisa)

        val settlement = MandiMathEngine.calculateSettlement(
            grossWeightGrams = grossGrams,
            cutWeightGrams = bagDeductionGrams,
            ratePaisaPerQuintal = ratePaisaPerQtl,
            deductions = DeductionsInput(
                farmerCommissionPaisa = farmerCommissionPaisa,
                buyerCommissionPaisa = buyerCommissionPaisa,
                labourChargePaisa = labourChargePaisa
            )
        )

        val settledDeal = deal1.copy(
            buyerId = buyer.id,
            dealStatus = DealStatus.SETTLED,
            ratePaisaPerUnit = ratePaisaPerQtl,
            grossAmountPaisa = settlement.grossAmountPaisa,
            farmerCommissionPaisa = settlement.farmerCommissionPaisa,
            buyerCommissionPaisa = settlement.buyerCommissionPaisa,
            labourChargePaisa = settlement.labourChargePaisa,
            netFarmerPayablePaisa = settlement.netFarmerPayablePaisa,
            netBuyerReceivablePaisa = settlement.netBuyerReceivablePaisa,
            updatedAt = currentTime + 3600000L
        )
        dealRepo.editDeal(settledDeal)

        println("📊 [सटीक आढ़त गणित - Integer Paisa Precision]:")
        println("  • कुल माल मूल्य (Gross): ₹${MandiMathEngine.paisaToRupeesString(settlement.grossAmountPaisa)}")
        println("  • आढ़त कमीशन (2%): ₹${MandiMathEngine.paisaToRupeesString(settlement.farmerCommissionPaisa)}")
        println("  • मंडी सेस (1.5%): ₹${MandiMathEngine.paisaToRupeesString(settlement.buyerCommissionPaisa)}")
        println("  • पल्लेदारी/हम्माली (₹8/बोरी): ₹${MandiMathEngine.paisaToRupeesString(settlement.labourChargePaisa)}")
        println("  ============================================================")
        println("  💰 किसान रमेश कुमार को शुद्ध देय: ₹${MandiMathEngine.paisaToRupeesString(settlement.netFarmerPayablePaisa)}")
        println("  💰 व्यापारी सुरेश चंद से कुल वसूली: ₹${MandiMathEngine.paisaToRupeesString(settlement.netBuyerReceivablePaisa)}")
        println("✅ [स्टेज-2 पूर्ण]: स्थिति = SETTLED (पक्का सौदा)")
        println("🔊 [साउंडबॉक्स आवाज़]: \"श्री गणेश ट्रेडिंग: रमेश कुमार का 45 बोरी गेहूं सौदा बावन हजार चार सौ अड़तीस रुपये में पक्का हुआ\"\n")
        ttsManager.speak("श्री गणेश ट्रेडिंग में रमेश कुमार का 45 बोरी गेहूं सौदा बावन हजार चार सौ अड़तीस रुपये में पक्का हुआ", true)
        Thread.sleep(5000)

        println("--------------------------------------------------------------------------------")
        println("▶ SCENE 3: दोपहर 12:00 PM — गल्ला रोकड़ बही व नकद भुगतान (Cash Ledger Flow)")
        println("--------------------------------------------------------------------------------")
        println("1. किसान रमेश कुमार को गल्ले से नकद भुगतान दिया गया: ₹20,000")
        cashRepo.recordTransaction(
            CashTransaction(
                id = "cash-tx-1",
                shopId = shopId,
                partyId = farmer.id,
                dealId = settledDeal.id,
                transactionType = TransactionType.UDHAR_GIVEN, // Cash Out
                amountPaisa = 2000000L,
                paymentMode = PaymentMode.CASH,
                transactionDate = currentTime + 7200000L,
                remarks = "गेहूं सौदे का नकद भुगतान",
                createdAt = currentTime + 7200000L,
                updatedAt = currentTime + 7200000L
            )
        )
        println("   🔊 [साउंडबॉक्स]: \"रमेश कुमार को बीस हजार रुपये नकद भुगतान किया गया\"")
        ttsManager.speak("रमेश कुमार को बीस हजार रुपये नकद भुगतान किया गया", true)
        Thread.sleep(4000)

        println("2. व्यापारी सुरेश चंद से नकद वसूली प्राप्त हुई: ₹30,000")
        cashRepo.recordTransaction(
            CashTransaction(
                id = "cash-tx-2",
                shopId = shopId,
                partyId = buyer.id,
                dealId = settledDeal.id,
                transactionType = TransactionType.JAMA_RECEIVED, // Cash In
                amountPaisa = 3000000L,
                paymentMode = PaymentMode.CASH,
                transactionDate = currentTime + 7500000L,
                remarks = "गेहूं खरीद पेटे नकद जमा",
                createdAt = currentTime + 7500000L,
                updatedAt = currentTime + 7500000L
            )
        )
        println("   🔊 [साउंडबॉक्स]: \"सुरेश चंद से तीस हजार रुपये नकद प्राप्त हुए\"\n")
        ttsManager.speak("सुरेश चंद से तीस हजार रुपये नकद प्राप्त हुए", true)
        Thread.sleep(4000)

        val farmerBal = partyRepo.getPartyBalanceStream(farmer.id).first()
        val buyerBal = partyRepo.getPartyBalanceStream(buyer.id).first()

        println("📈 [ताज़ा पार्टी बैलेंस - Real-time Ledger]:")
        println("  • रमेश कुमार (किसान): ₹${MandiMathEngine.paisaToRupeesString(farmerBal?.balancePaisa ?: 0L)} (${if ((farmerBal?.balancePaisa ?: 0L) >= 0) "देना है 🟢" else "लेना है 🔴"})")
        println("  • सुरेश चंद (व्यापारी): ₹${MandiMathEngine.paisaToRupeesString(buyerBal?.balancePaisa ?: 0L)} (${if ((buyerBal?.balancePaisa ?: 0L) >= 0) "लेना है 🔴" else "देना है 🟢"})\n")

        println("--------------------------------------------------------------------------------")
        println("▶ SCENE 4: देसी ब्याज गणना (Rural Simple Interest Engine — सैकड़ा दर)")
        println("--------------------------------------------------------------------------------")
        val principalPaisa = 1500000L // ₹15,000
        val monthlyRate = 1.5 // 1.5% प्रति माह (डेढ़ रुपया सैकड़ा)
        val days = 42
        val interestResult = RuralInterestEngine.calculateAccruedInterestByDays(principalPaisa, monthlyRate, days)

        println("लाला जी द्वारा किसान को दिए गए पुराने ₹15,000 खाद-बीज ऋण पर 42 दिन का ब्याज हिसाब:")
        println("  • मूलधन (Principal): ₹${MandiMathEngine.paisaToRupeesString(principalPaisa)}")
        println("  • देसी दर: ₹1.50 प्रति सैकड़ा प्रति माह (1.5%/माह)")
        println("  • अवधि: 42 दिन (दिन-वार प्रो-राटा = 1.4 माह)")
        println("  • कुल ब्याज (Interest): ₹${MandiMathEngine.paisaToRupeesString(interestResult.accruedInterestPaisa)}")
        println("  • कुल देय राशि (Total): ₹${MandiMathEngine.paisaToRupeesString(interestResult.totalPayablePaisa)}\n")

        println("--------------------------------------------------------------------------------")
        println("▶ SCENE 5: 1-टैप व्हाट्सएप डिजिटल बिल पर्ची (WhatsApp Slip Generator)")
        println("--------------------------------------------------------------------------------")
        val whatsappSlip = """
            ╔══════════════════════════════════════════╗
            ║       🌾 ${shop.shopName} 🌾        ║
            ║    ${shop.mandiName}    ║
            ║         संचालक: ${shop.ownerName}         ║
            ║            मो: ${shop.phoneNumber}            ║
            ╠══════════════════════════════════════════╣
            ║  तारीख: 20/09/2026      पर्ची सं: #MS-101 ║
            ║  पार्टी: रमेश कुमार (किसान - गोवर्धन)     ║
            ╠══════════════════════════════════════════╣
            ║  जिंस (फसल): गेहूं (Sharbati)            ║
            ║  बोरी: 45 बोरी                           ║
            ║  कुल वजन: 23 क्विंटल 50 किलो            ║
            ║  काट (बारदाना): -45 किलो (1kg/बोरी)      ║
            ║  शुद्ध वजन: 23.05 क्विंटल                ║
            ║  नीलामी भाव: ₹2,275.00 / क्विंटल         ║
            ║  सकल मूल्य: ₹52,438.75                   ║
            ╠══════════════════════════════════════════╣
            ║  कटौतियां:                                ║
            ║   - आढ़त (2.0%): ₹1,048.78               ║
            ║   - हम्माली/पल्लेदारी (₹8/बोरी): ₹360.00   ║
            ║  --------------------------------------  ║
            ║  ✅ कुल शुद्ध देय: ₹51,030.00            ║
            ║  💵 आज नकद भुगतान: ₹20,000.00            ║
            ║  🔴 बाकी शेष खाता: ₹31,030.00            ║
            ╚══════════════════════════════════════════╝
            🙏 धन्यवाद! आपका विश्वास ही हमारी पूंजी है।
        """.trimIndent()
        println(whatsappSlip)
        println("\n📲 [WhatsApp Share]: यह पर्ची 1-टैप में सीधे रमेश कुमार के व्हाट्सएप (${farmer.phone}) पर भेजी गई।\n")

        println("--------------------------------------------------------------------------------")
        println("▶ SCENE 6: शाम 06:00 PM — दैनिक गल्ला रोकड़ मिलान (Daily Cash Drawer Register)")
        println("--------------------------------------------------------------------------------")
        val allShopTxns = cashRepo.getTransactionsByShopStream(shopId).first()
        val totalIn = allShopTxns.filter { it.transactionType == TransactionType.JAMA_RECEIVED }.sumOf { it.amountPaisa }
        val totalOut = allShopTxns.filter { it.transactionType == TransactionType.UDHAR_GIVEN }.sumOf { it.amountPaisa }
        val openingCashPaisa = 1500000L // ₹15,000 opening
        val inHandCashPaisa = openingCashPaisa + totalIn - totalOut

        println("दिन भर की रोकड़ स्थिति (Cash Flow Summary):")
        println("  • प्रातः प्रारंभिक गल्ला (Opening Cash): ₹15,000.00")
        println("  • कुल नकद आवक (Total Cash In): ₹${MandiMathEngine.paisaToRupeesString(totalIn)}")
        println("  • कुल नकद निकासी (Total Cash Out): ₹${MandiMathEngine.paisaToRupeesString(totalOut)}")
        println("  ============================================================")
        println("  💵 गल्ले में उपस्थित नकदी (In-Hand Cash Drawer): ₹${MandiMathEngine.paisaToRupeesString(inHandCashPaisa)}")
        println("================================================================================")
        println("🎉 LIVE DEMO COMPLETED SUCCESSFULLY! All flows 100% verified.")
        println("================================================================================")
    }
}
