package com.appwork.mandisamiti.ui

import com.appwork.mandisamiti.Screen
import com.appwork.mandisamiti.backTarget
import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenRoutingTest {
    @Test
    fun entryScreensReturnToTheirList() {
        assertEquals(Screen.BuyList, Screen.BuyEntry(null).backTarget())
        assertEquals(Screen.SellList, Screen.SellReceipt("x").backTarget())
        assertEquals(Screen.Home, Screen.BuyList.backTarget())
    }
}
