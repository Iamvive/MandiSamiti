package com.appwork.mandisamiti.ui.settings

import com.appwork.mandisamiti.data.repository.OfflineFirstTradeSettingsRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.TradeSettings
import com.appwork.mandisamiti.ui.buy.BuyEntryFormState
import com.appwork.mandisamiti.ui.sell.SellEntryFormState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TradeSettingsFormDefaultsF05Test {

    @Test
    fun buyForm_seedsFromTradeSettings_andNewSlipsSkipTare() {
        val settings = TradeSettings(
            farmerCommissionBps = 150L, // 1.5%
            buyerCommissionBps = 150L,  // 1.5%
            defaultLabourPaisa = 1000L,  // ₹10 / bag
            defaultTareGrams = 35000L   // 0.35 Qtl (35 kg)
        )

        val form = BuyEntryFormState(settings).apply {
            farmerName = "राधे श्याम"
            bags = "10"
            weightPerBagKg = "50"
            ratePerQuintal = "2500"
        }

        // 1. Defaults seeded correctly from TradeSettings
        assertEquals("1.50", form.commissionPercent)
        assertEquals("10", form.labourPerBag)
        assertEquals("", form.tareQuintals) // tare removed for new Buy slips (2026-10-31)

        // 2. No tare: the entered weight is the billed weight.
        // Weight 10 * 50 = 500 kg (5.00 Qtl); gross = 5 Qtl * 2500 = 12,500 Rs = 1,250,000 paisa
        val preview = form.aggregatePreview()
        assertEquals(500_000L, preview.totalWeightGrams)
        assertEquals(1_250_000L, preview.totalGrossPaisa)

        // Commission 1.5% = 18,750; final rounds to nearest Rs 10: 1,250,000 - 10,000 - 18,750 = 1,221,250 -> 1,221,000
        // Commission absorbs the rounding: 19,000. Labour = 10 bags * Rs 10 = 10,000.
        assertEquals(19_000L, preview.commissionPaisa)
        assertEquals(10_000L, preview.labourPaisa)
        assertEquals(1_221_000L, preview.netPayablePaisa)
    }

    @Test
    fun sellForm_seedsFromTradeSettings_andAppliesTareInWeight() {
        val settings = TradeSettings(
            farmerCommissionBps = 200L, // 2%
            buyerCommissionBps = 150L,  // 1.5%
            defaultLabourPaisa = 800L,   // ₹8 / bag
            defaultTareGrams = 20000L   // 0.20 Qtl (20 kg)
        )

        val form = SellEntryFormState(settings).apply {
            buyerName = "महावीर ट्रेडर्स"
            lots[0].bags = "20"
            lots[0].weightPerBagKg = "50"
            lots[0].ratePerQuintal = "3000"
        }

        // 1. Defaults seeded correctly
        assertEquals("1.50", form.commissionPercent)
        assertEquals("8", form.labourPerBag)
        assertEquals("0.2", form.tareQuintals)

        // 2. Tare deduction:
        // Gross: 20 * 50 = 1000 kg = 1,000,000 grams (10.00 Qtl)
        // Tare: 0.20 Qtl = 20,000 grams
        // Net: 980,000 grams (9.80 Qtl)
        val preview = form.aggregatePreview()
        assertEquals(980_000L, preview.totalWeightGrams)

        // Gross = 9.80 Qtl * 3000 Rs = 29,400 Rs = 2,940,000 paisa
        assertEquals(2_940_000L, preview.grossPaisa)
    }

    @Test
    fun tradeSettingsRepository_persistsChanges_andNewFormsReflectSavedRates() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstTradeSettingsRepository(
            database = db,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        // Initial defaults
        val initial = repo.getTradeSettingsStream().first()
        val form1 = BuyEntryFormState(initial)
        assertEquals("0.70", form1.commissionPercent)
        assertEquals("10", form1.labourPerBag)
        assertEquals("", form1.tareQuintals)

        // Update settings in repository
        val updated = TradeSettings(
            farmerCommissionBps = 250L,  // 2.5%
            buyerCommissionBps = 175L,   // 1.75%
            defaultLabourPaisa = 1200L,  // ₹12 / bag
            defaultTareGrams = 50000L    // 0.50 Qtl
        )
        repo.saveTradeSettings(updated)

        // Form seeded from updated settings
        val latest = repo.getTradeSettingsStream().first()
        val form2 = BuyEntryFormState(latest)
        assertEquals("2.50", form2.commissionPercent)
        assertEquals("12", form2.labourPerBag)
        assertEquals("", form2.tareQuintals)

        val sellForm = SellEntryFormState(latest)
        assertEquals("1.75", sellForm.commissionPercent)
        assertEquals("12", sellForm.labourPerBag)
        assertEquals("0.5", sellForm.tareQuintals)
    }
}
