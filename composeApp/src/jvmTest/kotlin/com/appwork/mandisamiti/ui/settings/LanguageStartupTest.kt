package com.appwork.mandisamiti.ui.settings

import com.appwork.mandisamiti.domain.model.TradeSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageStartupTest {

    @Test
    fun loadingUntilSettingsEmit() {
        assertEquals(StartupLanguage.Loading, startupLanguage(null))
    }

    @Test
    fun chosenLanguageOnceSettingsEmit() {
        assertEquals(StartupLanguage.Ready(true), startupLanguage(TradeSettings(isEnglish = true)))
        assertEquals(StartupLanguage.Ready(false), startupLanguage(TradeSettings(isEnglish = false)))
    }
}
