package com.appwork.mandisamiti.platform

actual class SoundboxTtsManager {
    actual fun speak(text: String, isSoundEnabled: Boolean) {
        // Desktop / JVM TTS stub or log
        if (isSoundEnabled) {
            println("[MandiSoundbox JVM]: $text")
        }
    }

    actual fun stop() {}
    actual fun shutdown() {}
}
