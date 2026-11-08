package ua.zvukaryk.app.speech

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Запис голосу дитини з живим індикатором гучності та миттєвим прослуховуванням.
 * Це «слуховий самоконтроль» — ключовий прийом автоматизації. Усе зберігається лише в кеші пристрою.
 * Мікрофон використовується як датчик (гучність, тривалість), а не як суддя правильності.
 */
class AudioRecorderManager(private val context: Context) {
    private val sampleRate = 16_000
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var record: AudioRecord? = null
    private var player: MediaPlayer? = null

    private val _amplitude = MutableStateFlow(0f)
    /** Нормалізована гучність 0..1 (RMS, згладжена). */
    val amplitude: StateFlow<Float> = _amplitude

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _voicedSeconds = MutableStateFlow(0f)
    /** Скільки секунд поспіль звучить голос (для ігор «тягни звук»). */
    val voicedSeconds: StateFlow<Float> = _voicedSeconds

    private var lastFile: File? = null
    val hasRecording: Boolean get() = lastFile?.let { it.exists() && it.length() > 44 } == true

    @SuppressLint("MissingPermission")
    fun start() {
        if (job?.isActive == true && _isRecording.value) return
        stopPlayback()
        val previous = job
        _isRecording.value = true
        _voicedSeconds.value = 0f
        job = scope.launch {
            previous?.cancelAndJoin()
            val minBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val bufSize = maxOf(minBuf, 4096)
            val rec = runCatching {
                AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize)
            }.getOrNull()
            if (rec == null || rec.state != AudioRecord.STATE_INITIALIZED) {
                rec?.release()
                _isRecording.value = false
                return@launch
            }
            record = rec
            val pcm = ByteArrayOutputStream()
            val buffer = ShortArray(bufSize / 2)
            var smooth = 0f
            var voiced = 0f
            try {
                rec.startRecording()
                while (isActive && _isRecording.value) {
                    val n = rec.read(buffer, 0, buffer.size)
                    if (n < 0) break // мікрофон зайнятий іншим застосунком
                    if (n == 0) continue
                    var sum = 0.0
                    for (i in 0 until n) sum += buffer[i].toDouble() * buffer[i]
                    val rms = sqrt(sum / n) / 32768.0
                    val level = (rms * 6).coerceIn(0.0, 1.0).toFloat()
                    smooth = smooth * 0.6f + level * 0.4f
                    _amplitude.value = smooth
                    val seconds = n.toFloat() / sampleRate
                    voiced = if (smooth > 0.12f) voiced + seconds else maxOf(0f, voiced - seconds * 2)
                    _voicedSeconds.value = voiced
                    val bytes = ByteBuffer.allocate(n * 2).order(ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until n) bytes.putShort(buffer[i])
                    pcm.write(bytes.array(), 0, n * 2)
                }
            } catch (_: Exception) {
                // IllegalStateException від startRecording тощо — тихо завершуємо, UI побачить isRecording=false
            } finally {
                runCatching { rec.stop() }
                rec.release()
                record = null
                _amplitude.value = 0f
                _isRecording.value = false
                lastFile = if (pcm.size() > 0) runCatching { writeWav(pcm.toByteArray()) }.getOrNull() else null
            }
        }
    }

    fun stop() {
        _isRecording.value = false
        job?.cancel()
    }

    /** Зупинити запис і дочекатися, поки файл буде збережено (щоб одразу відтворити). */
    suspend fun stopAndAwait() {
        _isRecording.value = false
        job?.cancelAndJoin()
    }

    /** Відтворити останній запис («послухай себе»). */
    fun play(onComplete: () -> Unit = {}) {
        val file = lastFile?.takeIf { it.exists() && it.length() > 44 } ?: return onComplete()
        stopPlayback()
        runCatching {
            MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { stopPlayback(); onComplete() }
                setOnErrorListener { _, _, _ -> stopPlayback(); onComplete(); true }
                prepare()
                start()
            }.also { player = it }
        }.onFailure { stopPlayback(); onComplete() }
    }

    fun stopPlayback() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
    }

    fun release() {
        stop()
        stopPlayback()
        job?.cancel()
        lastFile?.delete()
    }

    private fun writeWav(pcm: ByteArray): File {
        val file = File(context.cacheDir, "last_take.wav")
        FileOutputStream(file).use { out ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            val byteRate = sampleRate * 2
            header.put("RIFF".toByteArray()).putInt(36 + pcm.size).put("WAVE".toByteArray())
            header.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            header.putInt(sampleRate).putInt(byteRate).putShort(2).putShort(16)
            header.put("data".toByteArray()).putInt(pcm.size)
            out.write(header.array())
            out.write(pcm)
        }
        return file
    }
}
