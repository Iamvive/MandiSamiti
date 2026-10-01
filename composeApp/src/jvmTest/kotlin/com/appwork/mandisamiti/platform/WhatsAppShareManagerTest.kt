package com.appwork.mandisamiti.platform

import kotlin.test.Test
import kotlin.test.assertTrue

class WhatsAppShareManagerTest {

    @Test
    fun testShareTextDoesNotThrow() {
        val manager = WhatsAppShareManager()
        // Should execute smoothly without crashing on Desktop/JVM
        manager.shareText("श्री गणेश ट्रेडिंग\nसौदा पर्ची #1234", "9837123456")
        assertTrue(true)
    }

    @Test
    fun testShareReceiptImageDoesNotThrow() {
        val manager = WhatsAppShareManager()
        val dummyBytes = ByteArray(10) { 1 }
        manager.shareReceiptImage(dummyBytes, "9837123456", "सौदा पर्ची")
        assertTrue(true)
    }
}
