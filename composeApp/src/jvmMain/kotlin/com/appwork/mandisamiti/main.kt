package com.appwork.mandisamiti

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.appwork.mandisamiti.database.DriverFactory
import com.appwork.mandisamiti.database.createDatabase
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import com.appwork.mandisamiti.platform.WhatsAppShareManager

fun main() = application {
    val database = createDatabase(DriverFactory())
    val ttsManager = SoundboxTtsManager()
    val whatsAppShareManager = WhatsAppShareManager()

    Window(
        onCloseRequest = ::exitApplication,
        title = "मंडी समिति — आढ़त खाता (MandiSamiti)",
        state = rememberWindowState(width = 460.dp, height = 850.dp)
    ) {
        App(
            database = database,
            ttsManager = ttsManager,
            whatsAppShareManager = whatsAppShareManager
        )
    }
}
