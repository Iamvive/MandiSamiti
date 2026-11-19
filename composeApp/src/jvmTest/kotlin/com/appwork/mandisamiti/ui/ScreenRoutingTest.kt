package com.appwork.mandisamiti.ui

import com.appwork.mandisamiti.Screen
import com.appwork.mandisamiti.backTarget
import com.appwork.mandisamiti.domain.model.LendDirection
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
}
