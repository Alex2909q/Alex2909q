package ua.zvukaryk.app.speech

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Голос застосунку (системний синтез мовлення, офлайн).
 *
 * Надійність:
 *  - фрази, сказані до готовності синтезатора, не губляться: остання ставиться в чергу й звучить після запуску;
 *  - якщо стандартний синтезатор телефона не вміє української (часто на Samsung/Xiaomi/Huawei),
 *    автоматично перемикаємося на «Google Синтезатор мовлення», якщо він встановлений;
 *  - стан і причина тиші доступні для екрана діагностики з кнопками «виправити».
 */
class TtsManager(context: Context) {

    enum class Status {
        /** Синтезатор ще запускається. */
        STARTING,
        /** Український голос працює. */
        READY,
        /** Синтезатор уміє українську, але голос ще не завантажено. */
        MISSING_DATA,
        /** Синтезатор не підтримує українську. */
        NO_UKRAINIAN,
        /** На телефоні немає жодного синтезатора мовлення або він не запустився. */
        NO_ENGINE,
    }

    companion object {
        const val GOOGLE_TTS = "com.google.android.tts"
        private val UK = Locale("uk", "UA")
        private const val RATE_NORMAL = 0.85f
        private const val RATE_SLOW = 0.6f
    }

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _status = MutableStateFlow(Status.STARTING)
    val status: StateFlow<Status> = _status

    /** Назва синтезатора, яким зараз говоримо (для діагностики). */
    @Volatile var engineLabel: String = "—"
        private set

    /** Остання помилка відтворення (для діагностики). */
    @Volatile var lastError: String? = null
        private set

    private var tts: TextToSpeech? = null
    private var currentEngine: String? = null
    private var triedGoogle = false
    private var pending: Pair<String, Boolean>? = null

    val ready: Boolean get() = _status.value == Status.READY
    val ukrainianAvailable: Boolean get() = _status.value == Status.READY

    init {
        start(engine = null)
    }

    /** Перезапустити синтезатор (після встановлення голосу чи зміни налаштувань). */
    fun restart() {
        triedGoogle = false
        start(engine = null)
    }

    private fun start(engine: String?) {
        runCatching { tts?.stop(); tts?.shutdown() }
        tts = null
        currentEngine = engine
        _status.value = Status.STARTING
        lateinit var created: TextToSpeech
        // Колбек іноді викликається синхронно в конструкторі (коли синтезатора немає) — тому через post.
        created = TextToSpeech(appContext, { st -> main.post { onInit(created, st, engine) } }, engine)
        tts = created
        // Якщо синтезатор не відповів за 8 секунд — пробуємо Google або показуємо проблему.
        main.postDelayed({
            if (tts === created && _status.value == Status.STARTING) {
                if (!tryGoogleInstead()) { engineLabel = "не відповідає"; _status.value = Status.NO_ENGINE }
            }
        }, 8_000L)
    }

    private fun isInstalled(pkg: String): Boolean =
        runCatching { appContext.packageManager.getPackageInfo(pkg, 0); true }.getOrDefault(false)

    private fun tryGoogleInstead(): Boolean {
        if (triedGoogle || currentEngine == GOOGLE_TTS || !isInstalled(GOOGLE_TTS)) return false
        triedGoogle = true
        start(GOOGLE_TTS)
        return true
    }

    private fun onInit(t: TextToSpeech, status: Int, engine: String?) {
        if (t !== tts) return // застарілий екземпляр після перезапуску
        if (status != TextToSpeech.SUCCESS) {
            if (tryGoogleInstead()) return
            engineLabel = "не знайдено"
            _status.value = Status.NO_ENGINE
            return
        }
        val engineName = engine ?: runCatching { t.defaultEngine }.getOrNull()
        engineLabel = runCatching { t.engines.firstOrNull { it.name == engineName }?.label }.getOrNull() ?: engineName ?: "системний"

        val availability = runCatching { t.isLanguageAvailable(UK) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        val supported = availability >= TextToSpeech.LANG_AVAILABLE
        if (!supported && tryGoogleInstead()) return

        if (supported) {
            t.setLanguage(UK)
            _status.value = Status.READY
        } else {
            // Без української говоримо хоч якось, але показуємо батькам, як виправити.
            runCatching { t.setLanguage(UK) }
            _status.value = if (availability == TextToSpeech.LANG_MISSING_DATA) Status.MISSING_DATA else Status.NO_UKRAINIAN
        }
        t.setSpeechRate(RATE_NORMAL)
        t.setPitch(1.05f)
        t.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { lastError = null }
            override fun onDone(utteranceId: String?) {}
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { lastError = "помилка відтворення" }
            override fun onError(utteranceId: String?, errorCode: Int) { lastError = "помилка відтворення (код $errorCode)" }
        })
        pending?.let { (text, slow) -> pending = null; speakNow(t, text, slow) }
    }

    fun speak(text: String, slow: Boolean = false) {
        if (text.isBlank()) return
        val t = tts
        if (t == null || _status.value == Status.STARTING) {
            pending = text to slow // скажемо, щойно синтезатор запуститься
            return
        }
        if (_status.value == Status.NO_ENGINE) return
        speakNow(t, text, slow)
    }

    private fun speakNow(t: TextToSpeech, text: String, slow: Boolean) {
        t.setSpeechRate(if (slow) RATE_SLOW else RATE_NORMAL)
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f) }
        val result = t.speak(text, TextToSpeech.QUEUE_FLUSH, params, "zvukaryk-${System.nanoTime()}")
        if (result != TextToSpeech.SUCCESS) lastError = "синтезатор не прийняв фразу"
    }

    fun stop() { pending = null; runCatching { tts?.stop() } }

    fun shutdown() { runCatching { tts?.stop(); tts?.shutdown() } }

    // ---------- Діагностика гучності ----------

    /** Гучність медіа 0..1 (TTS іде через медіа-канал). */
    fun mediaVolume(): Float {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
    }

    /** Підняти гучність медіа до комфортного рівня (60 %), якщо зараз тихіше. */
    fun raiseVolume() {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (max * 0.6f).toInt().coerceAtLeast(1)
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) < target) {
            runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI) }
        }
    }

    // ---------- Інтенти для виправлення ----------

    /** Завантажити голосові дані (відкриває встановлення голосу в поточному синтезаторі). */
    fun installVoiceIntent(): Intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
        currentEngine?.let { setPackage(it) }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Системні налаштування синтезу мовлення (вибір синтезатора за замовчуванням). */
    fun ttsSettingsIntent(): Intent = Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Google Синтезатор мовлення в Play Market. */
    fun googleTtsStoreIntent(): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GOOGLE_TTS")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun googleTtsWebIntent(): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$GOOGLE_TTS")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    val googleTtsInstalled: Boolean get() = isInstalled(GOOGLE_TTS)

    /** Відкрити інтент, а якщо не вийшло — запасний. Повертає true, якщо щось відкрилося. */
    fun launch(context: Context, intent: Intent, fallback: Intent? = null): Boolean = try {
        context.startActivity(intent); true
    } catch (_: ActivityNotFoundException) {
        fallback?.let { runCatching { context.startActivity(it) }.isSuccess } ?: false
    } catch (_: SecurityException) {
        false
    }
}
