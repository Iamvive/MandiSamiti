package com.appwork.mandisamiti.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class SyncStatusPresentationTest {
    @Test
    fun pendingEntryIsLabelledAsStoredOnPhone() {
        assertEquals("फ़ोन में", syncStatusLabel(isSynced = false, isEnglish = false))
        assertEquals("On phone", syncStatusLabel(isSynced = false, isEnglish = true))
    }

    @Test
    fun syncedEntryIsLabelledAsStoredOnServer() {
        assertEquals("सर्वर पर", syncStatusLabel(isSynced = true, isEnglish = false))
        assertEquals("On server", syncStatusLabel(isSynced = true, isEnglish = true))
    }
}
