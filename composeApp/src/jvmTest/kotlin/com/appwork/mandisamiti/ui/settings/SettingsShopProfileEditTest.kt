package com.appwork.mandisamiti.ui.settings

import com.appwork.mandisamiti.data.repository.OfflineFirstPartyRepository
import com.appwork.mandisamiti.data.repository.OfflineFirstShopProfileRepository
import com.appwork.mandisamiti.database.createTestDatabase
import com.appwork.mandisamiti.domain.model.ShopProfile
import com.appwork.mandisamiti.ui.home.HomeViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsShopProfileEditTest {

    @Test
    fun repository_savesAndLoadsShopProfileWithAddressGstinAndLicence() = runTest {
        val db = createTestDatabase()
        val repo = OfflineFirstShopProfileRepository(db)

        val initial = ShopProfile(
            id = "shop-1",
            shopName = "शर्मा ट्रेडिंग",
            ownerName = "सुरेश शर्मा",
            mandiName = "मथुरा मंडी",
            shopNumber = "दुकान 12",
            phoneNumber = "9876543210",
            pinHash = "pin",
            createdAt = 1000L,
            updatedAt = 1000L,
            syncStatus = 1
        )
        repo.saveShopProfile(initial)

        val loadedInitial = repo.getShopProfileStream().first()
        assertEquals("शर्मा ट्रेडिंग", loadedInitial?.shopName)
        assertNull(loadedInitial?.address)
        assertNull(loadedInitial?.gstin)
        assertNull(loadedInitial?.licenceNo)

        // Update with new fields
        val updated = initial.copy(
            shopName = "श्री शर्मा एंड संस",
            ownerName = "सुरेश चंद्र शर्मा",
            mandiName = "नवीन अनाज मंडी, मथुरा",
            address = "दुकान नं. 12, ब्लॉक बी, मंडी प्रांगण",
            gstin = "09AAAAA0000A1Z5",
            licenceNo = "MANDI-LIC-2026-42",
            updatedAt = 2000L,
            syncStatus = 0
        )
        repo.saveShopProfile(updated)

        val loadedUpdated = repo.getShopProfileStream().first()
        assertEquals("श्री शर्मा एंड संस", loadedUpdated?.shopName)
        assertEquals("सुरेश चंद्र शर्मा", loadedUpdated?.ownerName)
        assertEquals("नवीन अनाज मंडी, मथुरा", loadedUpdated?.mandiName)
        assertEquals("9876543210", loadedUpdated?.phoneNumber, "Phone number must remain unchanged")
        assertEquals("दुकान नं. 12, ब्लॉक बी, मंडी प्रांगण", loadedUpdated?.address)
        assertEquals("09AAAAA0000A1Z5", loadedUpdated?.gstin)
        assertEquals("MANDI-LIC-2026-42", loadedUpdated?.licenceNo)
        assertEquals(0, loadedUpdated?.syncStatus, "Sync status must be reset to 0 for local edits")
    }

    @Test
    fun gstin_validationLogic() {
        fun isValidGstin(input: String): Boolean {
            val trimmed = input.trim()
            return trimmed.isEmpty() || (trimmed.length == 15 && trimmed.all { it.isLetterOrDigit() })
        }

        assertTrue(isValidGstin(""), "Empty GSTIN is optional and valid")
        assertTrue(isValidGstin("   "), "Blank GSTIN is optional and valid")
        assertTrue(isValidGstin("09AAAAA0000A1Z5"), "15 alphanumeric is valid GSTIN")
        assertTrue(!isValidGstin("09AAAAA0000"), "Less than 15 chars is invalid")
        assertTrue(!isValidGstin("09AAAAA0000A1Z599"), "More than 15 chars is invalid")
        assertTrue(!isValidGstin("09AAAAA0000A1Z#"), "Special characters in GSTIN are invalid")
    }

    @Test
    fun homeViewModel_updateShopProfile_persistsToRepository() = runTest {
        val db = createTestDatabase()
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val shopRepo = OfflineFirstShopProfileRepository(db, ioDispatcher = ioDispatcher)
        val partyRepo = OfflineFirstPartyRepository(db, ioDispatcher = ioDispatcher)

        val profile = ShopProfile(
            id = "shop-vm-1",
            shopName = "किशन ब्रदर्स",
            ownerName = "किशन लाल",
            mandiName = "हाथरस मंडी",
            phoneNumber = "9123456789",
            pinHash = "pin",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        shopRepo.saveShopProfile(profile)

        val vm = HomeViewModel(
            shopId = "shop-vm-1",
            shopProfileRepository = shopRepo,
            partyRepository = partyRepo,
            viewModelScope = backgroundScope,
            backgroundDispatcher = ioDispatcher
        )

        testScheduler.advanceUntilIdle()

        val edited = profile.copy(
            shopName = "किशन एंड ब्रदर्स प्राइवेट लिमिटेड",
            address = "गेट नं. 2, हाथरस",
            gstin = "09KKKKK0000K1Z9",
            licenceNo = "HTR-888"
        )
        vm.updateShopProfile(edited)
        testScheduler.advanceUntilIdle()

        val loaded = shopRepo.getShopProfileStream().first()
        assertEquals("किशन एंड ब्रदर्स प्राइवेट लिमिटेड", loaded?.shopName)
        assertEquals("गेट नं. 2, हाथरस", loaded?.address)
        assertEquals("09KKKKK0000K1Z9", loaded?.gstin)
        assertEquals("HTR-888", loaded?.licenceNo)
    }
}
