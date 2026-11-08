package ua.zvukaryk.app.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Озвучення інструкцій та зразків слів українською (Google TTS підтримує uk-UA).
 * Темп трохи повільніший — так дітям легше чути кожен звук.
 */
class TtsManager(context: Context) {
    @Volatile var ready: Boolean = false
        private set
    @Volatile var ukrainianAvailable: Boolean = false
        private set

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("uk", "UA"))
            ukrainianAvailable = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!ukrainianAvailable) tts.setLanguage(Locale.getDefault())
            tts.setSpeechRate(0.85f)
            tts.setPitch(1.05f)
            ready = true
        }
    }

    fun speak(text: String, slow: Boolean = false) {
        if (!ready) return
        tts.setSpeechRate(if (slow) 0.6f else 0.85f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "zvukaryk-${System.nanoTime()}")
    }

    fun stop() { if (ready) tts.stop() }

    fun shutdown() { runCatching { tts.stop(); tts.shutdown() } }
}
