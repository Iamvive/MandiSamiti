package com.appwork.mandisamiti

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.appwork.mandisamiti.data.auth.AuthApi
import com.appwork.mandisamiti.data.auth.InMemorySessionStore
import com.appwork.mandisamiti.data.auth.mandiHttpClient
import com.appwork.mandisamiti.database.DriverFactory
import com.appwork.mandisamiti.database.createDatabase
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import com.appwork.mandisamiti.platform.WhatsAppShareManager
import com.appwork.mandisamiti.platform.apiBaseUrl

fun main() = application {
    val database = createDatabase(DriverFactory())
    val ttsManager = SoundboxTtsManager()
    val whatsAppShareManager = WhatsAppShareManager()
    // Desktop is a dev harness: the session lives only as long as the window.
    val sessionStore = InMemorySessionStore()
    val authApi = AuthApi(mandiHttpClient(), apiBaseUrl)

    Window(
        onCloseRequest = ::exitApplication,
        title = "मंडी समिति — आढ़त खाता (MandiSamiti)",
        state = rememberWindowState(width = 460.dp, height = 850.dp)
    ) {
        App(
            database = database,
            ttsManager = ttsManager,
            whatsAppShareManager = whatsAppShareManager,
            sessionStore = sessionStore,
            authApi = authApi
        )
    }
}
