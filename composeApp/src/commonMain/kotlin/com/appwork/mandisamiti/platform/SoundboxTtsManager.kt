package com.appwork.mandisamiti.platform

expect class SoundboxTtsManager {
    fun speak(text: String, isSoundEnabled: Boolean)
    fun stop()
    fun shutdown()
}
