package ua.zvukaryk.app.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Допоміжна перевірка слова через системне розпізнавання мовлення (uk-UA).
 * Важливо: розпізнавання дитячого мовлення ненадійне (помилки у 2–5 разів частіші, ніж у дорослих),
 * тому результат показуємо як «здається, вийшло!», а не як вирок. Головний суддя — дорослий поруч.
 */
class SpeechChecker(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    data class Result(val heard: List<String>, val similarity: Float) {
        val looksRight: Boolean get() = similarity >= 0.75f
        val close: Boolean get() = similarity >= 0.5f
    }

    suspend fun listenFor(target: String): Result = suspendCancellableCoroutine { cont ->
        if (!isAvailable) { cont.resume(Result(emptyList(), 0f)); return@suspendCancellableCoroutine }
        val rec = SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "uk-UA")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "uk-UA")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
        }
        var finished = false
        fun finish(words: List<String>) {
            if (finished) return
            finished = true
            runCatching { rec.destroy() }
            recognizer = null
            if (cont.isActive) cont.resume(Result(words, bestSimilarity(target, words)))
        }
        rec.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                finish(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty())
            }
            override fun onError(error: Int) = finish(emptyList())
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        cont.invokeOnCancellation { finish(emptyList()) }
        rec.startListening(intent)
    }

    fun destroy() {
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    companion object {
        /** Схожість 0..1 на основі відстані Левенштейна між нормалізованими рядками. */
        fun bestSimilarity(target: String, heard: List<String>): Float {
            val t = normalize(target)
            if (t.isEmpty()) return 0f
            return heard.maxOfOrNull { h ->
                normalize(h).split(" ").maxOfOrNull { word -> similarity(t, word) } ?: 0f
            } ?: 0f
        }

        fun normalize(s: String): String = s.lowercase().replace("ʼ", "'").filter { it.isLetter() || it == ' ' || it == '\'' }.trim()

        fun similarity(a: String, b: String): Float {
            if (a == b) return 1f
            val d = levenshtein(a, b)
            val max = maxOf(a.length, b.length)
            return if (max == 0) 1f else 1f - d.toFloat() / max
        }

        fun levenshtein(a: String, b: String): Int {
            val prev = IntArray(b.length + 1) { it }
            val cur = IntArray(b.length + 1)
            for (i in 1..a.length) {
                cur[0] = i
                for (j in 1..b.length) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
                }
                cur.copyInto(prev)
            }
            return prev[b.length]
        }
    }
}
