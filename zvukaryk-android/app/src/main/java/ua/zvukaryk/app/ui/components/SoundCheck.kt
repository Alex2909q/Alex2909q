package ua.zvukaryk.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.zvukaryk.app.speech.TtsManager

/** Пояснення стану голосу простими словами. */
fun voiceProblem(status: TtsManager.Status, volume: Float): String? = when {
    status == TtsManager.Status.NO_ENGINE -> "На телефоні немає синтезатора мовлення, тому Звукарик мовчить."
    status == TtsManager.Status.NO_UKRAINIAN -> "Синтезатор мовлення на телефоні не вміє говорити українською."
    status == TtsManager.Status.MISSING_DATA -> "Український голос ще не завантажено в синтезатор мовлення."
    volume <= 0.01f -> "Гучність медіа на телефоні вимкнена."
    else -> null
}

/**
 * Перевірка звуку з кнопками виправлення. [compact] — банер для головного екрана (лише коли є проблема).
 * Після повернення з налаштувань синтезатор перезапускається сам.
 */
@Composable
fun SoundCheckCard(tts: TtsManager, modifier: Modifier = Modifier, compact: Boolean = false) {
    val context = LocalContext.current
    val status by tts.status.collectAsStateWithLifecycle()
    var volume by remember { mutableFloatStateOf(tts.mediaVolume()) }
    var leftToFix by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        volume = tts.mediaVolume()
        if (leftToFix && status != TtsManager.Status.READY) tts.restart()
        leftToFix = false
        onPauseOrDispose { }
    }

    val problem = voiceProblem(status, volume)
    if (compact && (problem == null || status == TtsManager.Status.STARTING)) return

    SoftCard(modifier, color = if (problem == null) Color(0xFFE3F7EA) else Color(0xFFFFF1E6)) {
        Text(if (problem == null) "🔊 Голос працює" else "🔇 Немає голосу", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            when {
                status == TtsManager.Status.STARTING -> "Синтезатор мовлення запускається…"
                problem != null -> problem
                else -> "Синтезатор: ${tts.engineLabel}. Якщо все одно тихо — додайте гучність медіа кнопками збоку телефона."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        tts.lastError?.let { Text("Остання помилка: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(8.dp))

        when (status) {
            TtsManager.Status.NO_ENGINE, TtsManager.Status.NO_UKRAINIAN -> {
                if (!tts.googleTtsInstalled) {
                    Text("Встановіть безкоштовний «Google Синтезатор мовлення», у ньому є український голос.", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { leftToFix = true; tts.launch(context, tts.googleTtsStoreIntent(), tts.googleTtsWebIntent()) }, modifier = Modifier.fillMaxWidth()) {
                        Text("1. Встановити Google Синтезатор")
                    }
                    OutlinedButton(onClick = { leftToFix = true; tts.launch(context, tts.ttsSettingsIntent()) }, modifier = Modifier.fillMaxWidth()) {
                        Text("2. Вибрати його в налаштуваннях")
                    }
                } else {
                    Text("Google Синтезатор уже є. Виберіть його основним і завантажте українську мову.", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { leftToFix = true; tts.launch(context, tts.ttsSettingsIntent()) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Відкрити налаштування синтезу мовлення")
                    }
                }
            }
            TtsManager.Status.MISSING_DATA -> {
                Button(onClick = { leftToFix = true; tts.launch(context, tts.installVoiceIntent(), tts.ttsSettingsIntent()) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Завантажити український голос")
                }
            }
            else -> {}
        }
        if (volume <= 0.3f) {
            OutlinedButton(onClick = { tts.raiseVolume(); volume = tts.mediaVolume() }, modifier = Modifier.fillMaxWidth()) {
                Text("Зробити гучніше")
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { volume = tts.mediaVolume(); tts.speak("Привіт! Я Звукарик. Тепер ти мене чуєш?") }, modifier = Modifier.weight(1f)) {
                Text("Тест звуку")
            }
            OutlinedButton(onClick = { tts.restart(); volume = tts.mediaVolume() }, modifier = Modifier.weight(1f)) {
                Text("Перевірити ще раз")
            }
        }
    }
}
