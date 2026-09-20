package com.appwork.mandisamiti.platform

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

actual class SoundboxTtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.ENGLISH)
            }
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(0.95f) // Slightly slower for maximum clarity in mandi environment
            isInitialized = true

            pendingText?.let {
                speak(it, true)
                pendingText = null
            }
        }
    }

    actual fun speak(text: String, isSoundEnabled: Boolean) {
        if (!isSoundEnabled) return

        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MANDI_SOUNDBOX_ID")
        } else {
            pendingText = text
        }
    }

    actual fun stop() {
        tts?.stop()
    }

    actual fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
