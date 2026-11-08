package ua.zvukaryk.app.data.model

import kotlinx.serialization.Serializable

/** Група звуків за логопедичною класифікацією. */
enum class SoundGroup(val title: String, val emoji: String) {
    SIBILANTS("Свистячі", "🎈"),
    HUSHING("Шиплячі", "🐍"),
    SONORANT_L("Звук Л", "✈️"),
    SONORANT_R("Звук Р", "🐯"),
}

/**
 * Рівні автоматизації за класичною ієрархією (Van Riper / українська методика):
 * ізольований звук → склади → слова → словосполучення → речення → тексти → диференціація → мовлення.
 */
enum class Level(val title: String, val shortTitle: String, val emoji: String, val trialsPerSession: Int) {
    ISOLATED("Ізольований звук", "Звук", "🔊", 8),
    SYLLABLES("Склади", "Склади", "🧩", 24),
    WORDS("Слова", "Слова", "🖼️", 30),
    PHRASES("Словосполучення", "Фрази", "🔗", 20),
    SENTENCES("Речення та чистомовки", "Речення", "💬", 16),
    TEXTS("Скоромовки та вірші", "Тексти", "📖", 10),
    DIFFERENTIATION("Розрізнення звуків", "Розрізнення", "👂", 20),
    CONVERSATION("Вільне мовлення", "Мовлення", "🌟", 10);

    fun next(): Level? = entries.getOrNull(ordinal + 1)
}

/** Слово з картинкою-емодзі, щоб дитині не потрібно було вміти читати. */
data class WordItem(val text: String, val emoji: String)

/** Мінімальна пара для диференціації змішуваних звуків. */
data class MinimalPair(val a: WordItem, val b: WordItem, val otherSoundId: String)

/** Кінестетичні підказки положення органів артикуляції (speech-specific pre-practice). */
data class Articulation(
    val lips: String,
    val tongue: String,
    val air: String,
    val voice: String,
    val childCue: String,
    val pose: MouthPose,
)

/**
 * Параметри положення губ/язика для анімації "ротика".
 * open: наскільки відкритий рот (0..1); round: округлення губ; smile: розтягнення в усмішку;
 * tongueY: -1 = кінчик за верхніми зубами/до піднебіння, 0 = по центру, 1 = за нижніми зубами / вниз;
 * tongueOut: наскільки язик висунутий (0..1); tongueWidth: 0 = вузький "голочка", 1 = широкий "лопаточка";
 * cup: підняті бічні краї ("чашечка"); vibrate: вібрація кінчика (для [р]).
 */
data class MouthPose(
    val open: Float = 0.3f,
    val round: Float = 0f,
    val smile: Float = 0.3f,
    val tongueY: Float = 0.3f,
    val tongueOut: Float = 0f,
    val tongueWidth: Float = 0.7f,
    val cup: Float = 0f,
    val vibrate: Boolean = false,
    val teethVisible: Boolean = true,
)

enum class ExerciseMode { HOLD, REPEAT }

/** Вправа артикуляційної розминки (перед дзеркалом). */
data class ArticulationExercise(
    val id: String,
    val name: String,
    val emoji: String,
    val childCue: String,
    val parentInstruction: String,
    val mode: ExerciseMode,
    val holdSeconds: Int = 5,
    val repetitions: Int = 6,
    val poseA: MouthPose,
    val poseB: MouthPose? = null,
)

enum class IsolatedGameType { LONG, PULSED, TRILL }

/** Гра для ізольованого звука: тягнемо довго ("насос"), або коротко повторюємо ("паровоз"). */
data class IsolatedGame(
    val title: String,
    val emoji: String,
    val instruction: String,
    val type: IsolatedGameType,
    val targetSeconds: Int = 3,
)

data class Syllables(
    val direct: List<String>,
    val reverse: List<String>,
    val intervocalic: List<String>,
)

data class Words(
    val initial: List<WordItem>,
    val medial: List<WordItem>,
    val final: List<WordItem>,
) {
    fun all(): List<WordItem> = initial + medial + final
}

data class SpeechSound(
    val id: String,
    val letter: String,
    val symbol: String,
    val title: String,
    val group: SoundGroup,
    val emoji: String,
    val onomatopoeia: String,
    val ageNormFrom: Int,
    val ageNormTo: Int,
    val articulation: Articulation,
    val warmupExerciseIds: List<String>,
    val isolated: IsolatedGame,
    val syllables: Syllables,
    val words: Words,
    val phrases: List<String>,
    val sentences: List<String>,
    val chystomovky: List<String>,
    val tongueTwisters: List<String>,
    val poems: List<String>,
    val conversationPrompts: List<String>,
    val differentiation: List<MinimalPair>,
    val commonMistakes: String,
)

/** Оцінка спроби батьками/дитиною: правильно, майже, неправильно. */
enum class Rating(val score: Int, val emoji: String, val label: String) {
    CORRECT(2, "✅", "Правильно"),
    ALMOST(1, "🟡", "Майже"),
    WRONG(0, "🔁", "Ще раз");
}

@Serializable
data class ChildProfile(
    val name: String = "",
    val ageYears: Int = 4,
    val avatar: String = "🦊",
)

@Serializable
data class SessionRecord(
    val epochDay: Long,
    val soundId: String,
    val level: Level,
    val trials: Int,
    val correct: Int,
    val almost: Int,
    val durationSec: Int,
)

@Serializable
data class SoundProgress(
    val soundId: String,
    val level: Level = Level.ISOLATED,
    val trialsAtLevel: Int = 0,
    val correctAtLevel: Int = 0,
    val sessionsAtLevel: Int = 0,
    val mastered: Boolean = false,
    val totalTrials: Int = 0,
    val history: List<SessionRecord> = emptyList(),
) {
    val accuracyAtLevel: Float get() = if (trialsAtLevel == 0) 0f else correctAtLevel.toFloat() / trialsAtLevel
}

@Serializable
data class Settings(
    val warmupEnabled: Boolean = true,
    val breathingEnabled: Boolean = false,
    val speechRecognitionEnabled: Boolean = false,
    val sessionMinutes: Int = 7,
    val ttsEnabled: Boolean = true,
    val autoRecordSeconds: Int = 3,
    val reminderHour: Int = 18,
)

@Serializable
data class ScreeningAnswers(
    val intelligibilityParents: Int = 3, // 1..3 (батьки розуміють мало / частково / майже все)
    val intelligibilityStrangers: Int = 2,
    val hearingConcern: Boolean = false,
    val vowelErrors: Boolean = false,
    val inconsistent: Boolean = false,
    val frustration: Boolean = false,
    val stuttering: Boolean = false,
    val problemSounds: List<String> = emptyList(),
    val completed: Boolean = false,
)

@Serializable
data class AppState(
    val onboarded: Boolean = false,
    val profile: ChildProfile = ChildProfile(),
    val targetSounds: List<String> = emptyList(),
    val progress: Map<String, SoundProgress> = emptyMap(),
    val stickers: List<String> = emptyList(),
    val streakDays: Int = 0,
    val lastPracticeEpochDay: Long = 0,
    val totalTrials: Int = 0,
    val totalSessions: Int = 0,
    val settings: Settings = Settings(),
    val screening: ScreeningAnswers = ScreeningAnswers(),
) {
    fun progressFor(soundId: String): SoundProgress = progress[soundId] ?: SoundProgress(soundId)
}
