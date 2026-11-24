package com.appwork.mandisamiti.ui.register

import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.TransactionType
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CashEntryTargetTest {

    @Test
    fun noKhataPicked_neverUsesAnotherParty() {
        val t = cashEntryTarget(null, "", false, "", "", "")
        assertEquals(CashEntryTarget("", null, null), t)
    }

    @Test
    fun linkedKhata_keepsNoteOnly() {
        val t = cashEntryTarget("p1", "रमेश", false, "", "", " डीजल ")
        assertEquals(CashEntryTarget("p1", "डीजल", null), t)
    }

    @Test
    fun nameOnly_putsNameAndNoteInRemarks() {
        assertEquals(CashEntryTarget("", "बाहर का ग्राहक · भाड़ा", null), cashEntryTarget(null, " बाहर का ग्राहक ", false, "", "", "भाड़ा"))
        assertEquals(CashEntryTarget("", "बाहर का ग्राहक", null), cashEntryTarget(null, "बाहर का ग्राहक", false, "", "", ""))
    }

    @Test
    fun newKisan_cleansPhoneAndVillage() {
        val t = cashEntryTarget(null, "सुरेश", true, "98-765 43210", " ", "")
        assertEquals(CashEntryTarget("", null, NewKisan("सुरेश", "9876543210", null)), t)
    }

    @Test
    fun newKisanWithBlankName_fallsBackToNoParty() {
        assertNull(cashEntryTarget(null, "  ", true, "98", "x", "").newKisan)
    }

    @Test
    fun recordDailyEntry_withNewKisan_savesPartyAndBooksEntryToIt() = runTest {
        val db = createTestDatabase()
        val io = StandardTestDispatcher(testScheduler)
        val clock = object : Clock { override fun now() = Instant.parse("2026-11-24T08:00:00Z") }
        val partyRepo = OfflineFirstPartyRepository(db, ioDispatcher = io)
        val cashRepo = OfflineFirstCashTransactionRepository(db, ioDispatcher = io, clock = clock)
        partyRepo.saveParty(Party(id = "first", shopId = "s1", name = "पहला", partyType = PartyType.FARMER, createdAt = 1L, updatedAt = 1L))
        val vm = DailyRegisterViewModel(
            shopId = "s1",
            cashRepository = cashRepo,
            partyRepository = partyRepo,
            shopProfileRepository = OfflineFirstShopProfileRepository(db, ioDispatcher = io),
            ttsManager = SoundboxTtsManager(),
            viewModelScope = backgroundScope,
            clock = clock,
            timeZone = TimeZone.of("Asia/Kolkata"),
        )

        vm.recordDailyEntry("", TransactionType.JAMA_RECEIVED, 500L, newKisan = NewKisan("सुरेश", "9876543210", "सिरसा"))

        val state = vm.uiState.first { it.todayTransactions.size == 1 }
        val tx = state.todayTransactions.single()
        assertEquals("सुरेश", tx.partyName)
        assertEquals("सिरसा", tx.village)
        val saved = partyRepo.getPartyById(tx.transaction.partyId)!!
        assertEquals(PartyType.FARMER, saved.partyType)
        assertEquals("9876543210", saved.phone)
    }
}
