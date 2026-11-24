package com.appwork.mandisamiti.ui

import com.appwork.mandisamiti.Screen
import com.appwork.mandisamiti.backTarget
import com.appwork.mandisamiti.domain.model.LendDirection
import com.appwork.mandisamiti.partyKhataScreen
import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenRoutingTest {
    @Test
    fun entryScreensReturnToTheirList() {
        assertEquals(Screen.BuyList, Screen.BuyEntry(null).backTarget())
        assertEquals(Screen.SellList, Screen.SellReceipt("x").backTarget())
        assertEquals(Screen.Home, Screen.BuyList.backTarget())
    }

    @Test
    fun udhaarRoutesReturnToExpectedTargets() {
        // Udhaar landing page back button returns to Home
        assertEquals(Screen.Home, Screen.LendList.backTarget())

        // Grain lend entry defaults to returning to Udhaar landing (Screen.LendList)
        val defaultGrainLend = Screen.GrainLendEntry()
        assertEquals(Screen.LendList, defaultGrainLend.returnScreen)
        assertEquals(Screen.LendList, defaultGrainLend.backTarget())

        // Custom return target is respected
        val customGrainLend = Screen.GrainLendEntry(returnScreen = Screen.Home)
        assertEquals(Screen.Home, customGrainLend.backTarget())

        // Udhaar person screen back button returns to Udhaar landing (Screen.LendList)
        assertEquals(Screen.LendList, Screen.UdhaarPerson("p1", LendDirection.GIVEN).backTarget())
        assertEquals(Screen.LendList, Screen.UdhaarPerson("p2", LendDirection.TAKEN).backTarget())
    }

    @Test
    fun partyKhataScreenResolvesToPartyLedgerAndBackTargetMatchesCaller() {
        // Navigating to khata from Udhaar person screen returns back to person screen
        val personScreen = Screen.UdhaarPerson("farmer-1", LendDirection.GIVEN)
        val khataFromPerson = partyKhataScreen("farmer-1", returnScreen = personScreen)
        assertEquals("farmer-1", khataFromPerson.partyId)
        assertEquals(personScreen, khataFromPerson.returnScreen)
        assertEquals(personScreen, khataFromPerson.backTarget())

        // Navigating to khata from Parchi slip (on Home) returns back to Home
        val khataFromParchi = partyKhataScreen("buyer-2", returnScreen = Screen.Home)
        assertEquals("buyer-2", khataFromParchi.partyId)
        assertEquals(Screen.Home, khataFromParchi.returnScreen)
        assertEquals(Screen.Home, khataFromParchi.backTarget())

        // Default return target is Screen.Home
        val defaultKhata = partyKhataScreen("farmer-3")
        assertEquals("farmer-3", defaultKhata.partyId)
        assertEquals(Screen.Home, defaultKhata.returnScreen)
        assertEquals(Screen.Home, defaultKhata.backTarget())

        // StockSheet returns to its caller (Home by default, AllKhata when opened from AllKhata)
        assertEquals(Screen.Home, Screen.StockSheet().backTarget())
        assertEquals(Screen.AllKhata, Screen.StockSheet(returnScreen = Screen.AllKhata).backTarget())
    }
}
