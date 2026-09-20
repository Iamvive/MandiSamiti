package com.appwork.mandisamiti.platform

import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

actual class SoundboxTtsManager {
    private val synthesizer = AVSpeechSynthesizer()

    actual fun speak(text: String, isSoundEnabled: Boolean) {
        if (!isSoundEnabled) return
        val utterance = AVSpeechUtterance(string = text).apply {
            voice = AVSpeechSynthesisVoice.voiceWithLanguage("hi-IN")
            rate = 0.5f
        }
        synthesizer.speakUtterance(utterance)
    }

    actual fun stop() {
        synthesizer.stopSpeakingAtBoundary(platform.AVFAudio.AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    actual fun shutdown() {
        stop()
    }
}
