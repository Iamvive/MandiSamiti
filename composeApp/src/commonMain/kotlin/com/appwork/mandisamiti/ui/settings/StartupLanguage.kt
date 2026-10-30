package com.appwork.mandisamiti.ui.settings

import com.appwork.mandisamiti.domain.model.TradeSettings

/** What the start-up block shows before trade settings have emitted: nothing, so no wrong-language flash. */
sealed interface StartupLanguage {
    data object Loading : StartupLanguage
    data class Ready(val isEnglish: Boolean) : StartupLanguage
}

fun startupLanguage(settings: TradeSettings?): StartupLanguage =
    if (settings == null) StartupLanguage.Loading else StartupLanguage.Ready(settings.isEnglish)
