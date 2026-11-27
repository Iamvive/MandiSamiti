package com.appwork.mandisamiti.ui.udhaar

import com.appwork.mandisamiti.data.repository.OfflineFirstCashTransactionRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstLendRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.KhataPartyPhoneException
import com.appwork.mandisamiti.domain.model.LendDirection
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.PaymentMode
import com.appwork.mandisamiti.domain.usecase.UdhaarUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UdhaarSaveLoanTest {

    private val fixedNow = 1_700_000_000_000L
    private var idCounter = 0

    private fun setup(testScheduler: kotlinx.coroutines.test.TestCoroutineScheduler) {
        // helper
    }

    @Test
    fun deletedPartyRestoredAndReusedWithFormNameAndStoredVillage() = runTest {
        val db = createTestDatabase()
        val queries = db.appDatabaseQueries
        val dispatcher = StandardTestDispatcher(testScheduler)
        val partyRepo = OfflineFirstPartyRepository(db, ioDispatcher = dispatcher)
        val lendRepo = OfflineFirstLendRepository(db, ioDispatcher = dispatcher)
        val useCase = UdhaarUseCase(lendRepo, { "loan-${++idCounter}" }, { fixedNow })
        val viewModel = UdhaarLandingViewModel("shop1", lendRepo, partyRepo, useCase, backgroundScope)

        // Seed soft-deleted party
        queries.insertParty(
            id = "party-del-1",
            shop_id = "shop1",
            name = "Old Deleted Farmer",
            phone = "9876500009",
            village = "Rampur",
            party_type = "FARMER",
            monthly_interest_rate = null,
            photo_uri = null,
            gstin = null,
            mandi_license = null,
            created_at = 1000L,
            updated_at = 1000L,
            is_deleted = 1L,
            sync_status = 1L,
        )

        val formState = UdhaarAddFormState(direction = LendDirection.GIVEN, todayMs = fixedNow)
        formState.updatePhone("9876500009")
        formState.name = "Restored Farmer"
        formState.amountRupees = "1500"

        val result = viewModel.saveLoan(formState)
        assertTrue(result.isSuccess, "Save loan must succeed on deleted party reuse")

        // 1. Exactly one party row exists for this phone
        val allPartiesForPhone = queries.getPartyByIdAny("party-del-1").executeAsOneOrNull()
        assertNotNull(allPartiesForPhone)
        assertEquals("party-del-1", allPartiesForPhone.id)
        assertEquals("Restored Farmer", allPartiesForPhone.name, "Name from form wins")
        assertEquals("Rampur", allPartiesForPhone.village, "Stored village must be preserved")
        assertEquals(0L, allPartiesForPhone.is_deleted, "Must be restored (is_deleted = 0)")

        // 2. Loan points to the restored party id
        val lends = lendRepo.getLendEntriesStream("shop1").first()
        assertEquals(1, lends.size)
        assertEquals("party-del-1", lends.first().partyId)
        assertEquals(150000L, lends.first().amountPaisa)
    }

    @Test
    fun khataPartyPhoneMatchIsBlocked() = runTest {
        val db = createTestDatabase()
        val queries = db.appDatabaseQueries
        val dispatcher = StandardTestDispatcher(testScheduler)
        val partyRepo = OfflineFirstPartyRepository(db, ioDispatcher = dispatcher)
        val lendRepo = OfflineFirstLendRepository(db, ioDispatcher = dispatcher)
        val useCase = UdhaarUseCase(lendRepo, { "loan-${++idCounter}" }, { fixedNow })
        val viewModel = UdhaarLandingViewModel("shop1", lendRepo, partyRepo, useCase, backgroundScope)

        // Seed a khaate party with a phone
        queries.insertParty(
            id = "khaate-shop1",
            shop_id = "shop1",
            name = "खाते",
            phone = "9876500099",
            village = null,
            party_type = "FARMER",
            monthly_interest_rate = null,
            photo_uri = null,
            gstin = null,
            mandi_license = null,
            created_at = 1000L,
            updated_at = 1000L,
            is_deleted = 0L,
            sync_status = 1L,
        )

        val formState = UdhaarAddFormState(direction = LendDirection.GIVEN, todayMs = fixedNow)
        formState.updatePhone("9876500099")
        formState.name = "Khata Attempt"
        formState.amountRupees = "1000"

        val result = viewModel.saveLoan(formState)
        assertTrue(result.isFailure, "Must fail when matching a khaate-% party")
        val exception = result.exceptionOrNull()
        assertIs<KhataPartyPhoneException>(exception)
    }

    @Test
    fun livePartyIsReusedAndRenamedIfNameDiffersWhileKeepingVillage() = runTest {
        val db = createTestDatabase()
        val queries = db.appDatabaseQueries
        val dispatcher = StandardTestDispatcher(testScheduler)
        val partyRepo = OfflineFirstPartyRepository(db, ioDispatcher = dispatcher)
        val lendRepo = OfflineFirstLendRepository(db, ioDispatcher = dispatcher)
        val useCase = UdhaarUseCase(lendRepo, { "loan-${++idCounter}" }, { fixedNow })
        val viewModel = UdhaarLandingViewModel("shop1", lendRepo, partyRepo, useCase, backgroundScope)

        queries.insertParty(
            id = "party-live-1",
            shop_id = "shop1",
            name = "Live Farmer Original",
            phone = "9876500001",
            village = "Sultanpur",
            party_type = "FARMER",
            monthly_interest_rate = null,
            photo_uri = null,
            gstin = null,
            mandi_license = null,
            created_at = 1000L,
            updated_at = 1000L,
            is_deleted = 0L,
            sync_status = 1L,
        )

        val formState = UdhaarAddFormState(direction = LendDirection.GIVEN, todayMs = fixedNow)
        formState.updatePhone("9876500001")
        formState.name = "Live Farmer Edited"
        formState.amountRupees = "2000"

        val result = viewModel.saveLoan(formState)
        assertTrue(result.isSuccess)

        val party = queries.getPartyById("party-live-1").executeAsOneOrNull()
        assertNotNull(party)
        assertEquals("Live Farmer Edited", party.name)
        assertEquals("Sultanpur", party.village)

        val lends = lendRepo.getLendEntriesStream("shop1").first()
        assertEquals(1, lends.size)
        assertEquals("party-live-1", lends.first().partyId)
    }

    @Test
    fun livePartySameNameReusedWithoutModifyingParty() = runTest {
        val db = createTestDatabase()
        val queries = db.appDatabaseQueries
        val dispatcher = StandardTestDispatcher(testScheduler)
        val partyRepo = OfflineFirstPartyRepository(db, ioDispatcher = dispatcher)
        val lendRepo = OfflineFirstLendRepository(db, ioDispatcher = dispatcher)
        val useCase = UdhaarUseCase(lendRepo, { "loan-${++idCounter}" }, { fixedNow })
        val viewModel = UdhaarLandingViewModel("shop1", lendRepo, partyRepo, useCase, backgroundScope)

        queries.insertParty(
            id = "party-live-2",
            shop_id = "shop1",
            name = "Existing Farmer",
            phone = "9876500002",
            village = "Kalyanpur",
            party_type = "FARMER",
            monthly_interest_rate = null,
            photo_uri = null,
            gstin = null,
            mandi_license = null,
            created_at = 1000L,
            updated_at = 1000L,
            is_deleted = 0L,
            sync_status = 1L,
        )

        val formState = UdhaarAddFormState(direction = LendDirection.GIVEN, todayMs = fixedNow)
        formState.updatePhone("9876500002")
        formState.name = "Existing Farmer"
        formState.amountRupees = "3000"

        val result = viewModel.saveLoan(formState)
        assertTrue(result.isSuccess)

        val party = queries.getPartyById("party-live-2").executeAsOneOrNull()
        assertNotNull(party)
        assertEquals(1000L, party.updated_at, "Party updatedAt unchanged because no write needed")

        val lends = lendRepo.getLendEntriesStream("shop1").first()
        assertEquals(1, lends.size)
        assertEquals("party-live-2", lends.first().partyId)
    }

    @Test
    fun puranaHisaabSavesLoanWithBookEntryAndPersistsNewPartyVillage() = runTest {
        val db = createTestDatabase()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val partyRepo = OfflineFirstPartyRepository(db, ioDispatcher = dispatcher)
        val lendRepo = OfflineFirstLendRepository(db, ioDispatcher = dispatcher)
        val cashRepo = OfflineFirstCashTransactionRepository(db, ioDispatcher = dispatcher)
        val useCase = UdhaarUseCase(lendRepo, { "loan-${++idCounter}" }, { fixedNow })
        val viewModel = UdhaarLandingViewModel("shop1", lendRepo, partyRepo, useCase, backgroundScope)

        val formState = UdhaarAddFormState(direction = LendDirection.GIVEN, todayMs = fixedNow)
        formState.updatePhone("9876500077")
        formState.name = "Purana Party"
        formState.village = "Bhopal"
        formState.amountRupees = "5000"
        formState.dateMs = fixedNow - 3L * 86_400_000L

        assertTrue(formState.isPuranaHisaab)
        val result = viewModel.saveLoan(formState)
        assertTrue(result.isSuccess)

        val savedParty = partyRepo.getPartiesStream("shop1").first().single { it.phone == "9876500077" }
        assertEquals("Purana Party", savedParty.name)
        assertEquals("Bhopal", savedParty.village)

        val cashRows = cashRepo.getTransactionsByPartyStream(savedParty.id).first()
        assertEquals(1, cashRows.size)
        assertEquals(PaymentMode.BOOK_ENTRY, cashRows.single().paymentMode)
        assertEquals(500_000L, cashRows.single().amountPaisa)
    }
}
