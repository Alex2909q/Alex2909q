package ua.zvukaryk.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.zvukaryk.app.ai.ClaudeAgent
import ua.zvukaryk.app.ai.VoiceCoach
import ua.zvukaryk.app.data.content.Stickers
import ua.zvukaryk.app.data.model.*
import ua.zvukaryk.app.data.progress.ProgressRepository
import ua.zvukaryk.app.domain.MasteryRules
import ua.zvukaryk.app.domain.SessionPlan
import ua.zvukaryk.app.domain.SessionPlanner
import ua.zvukaryk.app.speech.AudioRecorderManager
import ua.zvukaryk.app.speech.SpeechChecker
import ua.zvukaryk.app.speech.TtsManager
import java.time.LocalDate

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ProgressRepository(app)
    val tts = TtsManager(app)
    val recorder = AudioRecorderManager(app)
    val speechChecker = SpeechChecker(app)
    private val planner = SessionPlanner()

    val state: StateFlow<AppState?> = repo.state.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Голосовий супровід (офлайн TTS). */
    val voice = VoiceCoach(tts) { state.value?.settings?.let { it.ttsEnabled && it.voiceCoachEnabled } ?: true }

    /** Помічник Claude (Anthropic API, tool use). Говорить з дитиною через voice. */
    val claude = ClaudeAgent(
        settings = { state.value?.settings ?: Settings() },
        state = { state.value },
        speak = { text -> voice.say(text) },
    )

    /** Щоб привітання на головному екрані звучало один раз за запуск. */
    var greeted: Boolean = false

    fun plan(state: AppState, soundId: String): SessionPlan = planner.plan(state, soundId)

    fun completeOnboarding(profile: ChildProfile, targets: List<String>, screening: ScreeningAnswers) {
        viewModelScope.launch {
            repo.update {
                it.copy(
                    onboarded = true, profile = profile, targetSounds = targets, screening = screening,
                    progress = it.progress + targets.filter { id -> id !in it.progress }.associateWith { id -> SoundProgress(id) },
                )
            }
        }
    }

    fun updateProfile(profile: ChildProfile) = viewModelScope.launch { repo.update { it.copy(profile = profile) } }

    fun setTargets(targets: List<String>) = viewModelScope.launch {
        repo.update { s ->
            s.copy(targetSounds = targets, progress = s.progress + targets.filter { it !in s.progress }.associateWith { SoundProgress(it) })
        }
    }

    fun updateSettings(transform: (Settings) -> Settings) = viewModelScope.launch { repo.update { it.copy(settings = transform(it.settings)) } }

    /** Пропустити рівень вручну (наприклад, за рекомендацією логопеда). */
    fun setLevel(soundId: String, level: Level) = viewModelScope.launch {
        repo.update { s ->
            val p = s.progressFor(soundId).copy(level = level, trialsAtLevel = 0, correctAtLevel = 0, sessionsAtLevel = 0, mastered = false)
            s.copy(progress = s.progress + (soundId to p))
        }
    }

    /** Зафіксувати результати заняття: прогрес, серія днів, наліпка. */
    fun recordSession(soundId: String, level: Level, trials: Int, correct: Int, almost: Int, durationSec: Int, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            var sticker: String? = null
            repo.update { s ->
                val today = LocalDate.now().toEpochDay()
                val record = SessionRecord(today, soundId, level, trials, correct, almost, durationSec)
                val updated = MasteryRules.applySession(s.progressFor(soundId), record)
                val streak = when (s.lastPracticeEpochDay) {
                    today -> s.streakDays
                    today - 1 -> s.streakDays + 1
                    else -> 1
                }
                sticker = if (trials >= 5) Stickers.all[(s.stickers.size) % Stickers.all.size] else null
                s.copy(
                    progress = s.progress + (soundId to updated),
                    streakDays = streak,
                    lastPracticeEpochDay = today,
                    totalTrials = s.totalTrials + trials,
                    totalSessions = s.totalSessions + 1,
                    stickers = if (sticker != null) s.stickers + sticker!! else s.stickers,
                )
            }
            onDone(sticker)
        }
    }

    fun resetAll() = viewModelScope.launch { repo.reset() }

    override fun onCleared() {
        tts.shutdown()
        recorder.release()
        speechChecker.destroy()
        super.onCleared()
    }
}
