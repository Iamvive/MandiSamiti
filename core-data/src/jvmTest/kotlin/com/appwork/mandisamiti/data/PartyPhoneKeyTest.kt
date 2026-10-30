package com.appwork.mandisamiti.data

import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.DuplicatePhoneException
import com.appwork.mandisamiti.domain.model.Party
import com.appwork.mandisamiti.domain.model.PartyType
import com.appwork.mandisamiti.domain.model.normalizePhone
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PartyPhoneKeyTest {
    private fun party(id: String, phone: String?) = Party(
        id = id,
        shopId = "shop",
        name = "Party $id",
        phone = phone,
        partyType = PartyType.FARMER,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun normalizesIndianPhoneToLastTenDigits() {
        assertEquals("9876543210", normalizePhone("+91 98765-43210"))
        assertNull(normalizePhone("1234"))
    }

    @Test
    fun duplicatePhoneIsRejectedAndOriginalRemains() = runTest {
        val repo = OfflineFirstPartyRepository(createTestDatabase())
        repo.saveParty(party("first", "+91 98765-43210"))

        val error = assertFailsWith<DuplicatePhoneException> {
            repo.saveParty(party("second", "9876543210"))
        }

        assertEquals("first", error.existing.id)
        assertEquals("first", repo.getPartyByPhone("shop", "9876543210")?.id)
        assertNull(repo.getPartyById("second"))
    }

    @Test
    fun phoneLessAutoProvisionedPartiesRemainAllowed() = runTest {
        val repo = OfflineFirstPartyRepository(createTestDatabase())
        repo.saveParty(party("one", null))
        repo.saveParty(party("two", null))

        assertNotNull(repo.getPartyById("one"))
        assertNotNull(repo.getPartyById("two"))
    }

    @Test
    fun editingPartyKeepsItsOwnPhone() = runTest {
        val repo = OfflineFirstPartyRepository(createTestDatabase())
        repo.saveParty(party("one", "9876543210"))
        repo.saveParty(party("one", "9876543210").copy(name = "Renamed", updatedAt = 2L))

        assertEquals("Renamed", repo.getPartyById("one")?.name)
    }
}
