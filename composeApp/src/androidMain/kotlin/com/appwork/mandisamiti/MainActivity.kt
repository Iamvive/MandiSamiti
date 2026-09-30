package com.appwork.mandisamiti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.appwork.mandisamiti.database.DriverFactory
import com.appwork.mandisamiti.database.createDatabase
import com.appwork.mandisamiti.platform.SoundboxTtsManager
import com.appwork.mandisamiti.platform.WhatsAppShareManager

class MainActivity : ComponentActivity() {

    private lateinit var ttsManager: SoundboxTtsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val database = createDatabase(DriverFactory(applicationContext))
        ttsManager = SoundboxTtsManager(applicationContext)
        val whatsAppShareManager = WhatsAppShareManager(applicationContext)

        setContent {
            App(
                database = database,
                ttsManager = ttsManager,
                whatsAppShareManager = whatsAppShareManager
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::ttsManager.isInitialized) {
            ttsManager.shutdown()
        }
    }
}
