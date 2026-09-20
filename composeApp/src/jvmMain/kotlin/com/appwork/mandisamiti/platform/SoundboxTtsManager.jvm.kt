package com.appwork.mandisamiti.platform

import java.util.concurrent.Executors

actual class SoundboxTtsManager {
    private val executor = Executors.newSingleThreadExecutor()
    private var currentProcess: Process? = null

    actual fun speak(text: String, isSoundEnabled: Boolean) {
        if (!isSoundEnabled || text.isBlank()) return

        println("[MandiSoundbox Live Audio]: $text")

        executor.execute {
            try {
                currentProcess?.destroyForcibly()
                val os = System.getProperty("os.name", "").lowercase()
                val process = when {
                    os.contains("mac") -> {
                        // macOS native Hindi voice 'Lekha'
                        ProcessBuilder("say", "-v", "Lekha", text).start()
                    }
                    os.contains("win") -> {
                        val powershellScript = "Add-Type -AssemblyName System.speech; (New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak('$text')"
                        ProcessBuilder("powershell", "-Command", powershellScript).start()
                    }
                    os.contains("linux") -> {
                        ProcessBuilder("espeak", "-v", "hi", text).start()
                    }
                    else -> null
                }
                currentProcess = process
                process?.waitFor()
            } catch (e: Exception) {
                println("[MandiSoundbox Audio Error]: ${e.message}")
            }
        }
    }

    actual fun stop() {
        try {
            currentProcess?.destroyForcibly()
            currentProcess = null
        } catch (_: Exception) {}
    }

    actual fun shutdown() {
        stop()
        executor.shutdownNow()
    }
}
