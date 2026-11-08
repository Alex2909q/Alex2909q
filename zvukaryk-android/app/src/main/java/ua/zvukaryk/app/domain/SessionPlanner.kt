package ua.zvukaryk.app.domain

import ua.zvukaryk.app.data.content.ArticulationExercises
import ua.zvukaryk.app.data.content.BreathingGame
import ua.zvukaryk.app.data.content.BreathingGames
import ua.zvukaryk.app.data.content.Sounds
import ua.zvukaryk.app.data.model.*
import kotlin.random.Random

/** Один крок заняття. */
sealed class SessionStep {
    abstract val title: String

    data class Warmup(val exercises: List<ArticulationExercise>) : SessionStep() {
        override val title = "Розминка для язичка"
    }
    data class Breathing(val game: BreathingGame) : SessionStep() {
        override val title = "Дихальна гра"
    }
    data class Placement(val sound: SpeechSound) : SessionStep() {
        override val title = "Як сказати звук"
    }
    data class Isolated(val sound: SpeechSound, val trials: Int) : SessionStep() {
        override val title = "Звук ${sound.letter} окремо"
    }
    /** Основна практика за поточним рівнем: картки з мовним матеріалом. */
    data class Practice(val sound: SpeechSound, val level: Level, val items: List<PracticeItem>) : SessionStep() {
        override val title = level.title
    }
    /** Слухова гра: почуй слово — обери картинку (мінімальні пари / фонематичний слух). */
    data class Listening(val sound: SpeechSound, val rounds: List<ListeningRound>) : SessionStep() {
        override val title = "Вухо-детектив"
    }
    data class Conversation(val sound: SpeechSound, val prompts: List<String>) : SessionStep() {
        override val title = "Вільна розмова"
    }
    /** Казка від Звукарика (Claude): персоналізований текст із цільовими словами, дитина повторює речення за реченням. */
    data class Story(val sound: SpeechSound) : SessionStep() {
        override val title = "Казка від Звукарика"
    }
}

data class PracticeItem(val text: String, val emoji: String, val hint: String? = null)

data class ListeningRound(val target: WordItem, val options: List<WordItem>)

data class SessionPlan(val sound: SpeechSound, val level: Level, val steps: List<SessionStep>) {
    val productionTrials: Int
        get() = steps.sumOf {
            when (it) {
                is SessionStep.Isolated -> it.trials
                is SessionStep.Practice -> it.items.size
                is SessionStep.Conversation -> it.prompts.size
                is SessionStep.Story -> 6
                else -> 0
            }
        }
}

/**
 * Планувальник заняття. Спирається на доказові принципи:
 *  - «потроху, але щодня»: 5–12 хв, акцент на кількості продуктивних спроб (мета 40–70 за заняття);
 *  - ієрархія автоматизації: звук → склади → слова → фрази → речення → тексти → диференціація → мовлення;
 *  - блочна практика на ранніх рівнях, випадкова (перемішана) — зі слів і далі;
 *  - слухове розрізнення поруч із вимовою; розминка — коротка й необов'язкова.
 */
class SessionPlanner(private val random: Random = Random.Default) {

    fun plan(state: AppState, soundId: String): SessionPlan {
        val sound = Sounds.get(soundId)
        val progress = state.progressFor(soundId)
        val level = progress.level
        val settings = state.settings
        val budget = trialBudget(settings.sessionMinutes)
        val steps = mutableListOf<SessionStep>()

        if (settings.warmupEnabled) {
            val count = if (level <= Level.SYLLABLES) 3 else 2
            steps += SessionStep.Warmup(pickWarmup(sound, progress.sessionsAtLevel, count))
        }
        if (settings.breathingEnabled) {
            steps += SessionStep.Breathing(BreathingGames.all[progress.sessionsAtLevel % BreathingGames.all.size])
        }
        steps += SessionStep.Placement(sound)

        // Ізольований звук: завжди коротко як «включення», на першому рівні — основна робота.
        val isolatedTrials = if (level == Level.ISOLATED) Level.ISOLATED.trialsPerSession else 3
        steps += SessionStep.Isolated(sound, isolatedTrials)

        if (level != Level.ISOLATED) {
            val items = practiceItems(sound, level, progress.sessionsAtLevel, minOf(level.trialsPerSession, budget))
            steps += SessionStep.Practice(sound, level, items)
        }

        // Слухова гра: з рівня «Слова» і завжди на рівні диференціації.
        if (level >= Level.WORDS || level == Level.DIFFERENTIATION) {
            val rounds = listeningRounds(sound, if (level == Level.DIFFERENTIATION) 8 else 4)
            if (rounds.isNotEmpty()) steps += SessionStep.Listening(sound, rounds)
        }

        // Казка з цільовими словами — лише коли налаштовано помічника Claude і рівень уже «Речення» чи вище.
        if (settings.claudeConfigured && settings.claudeStoryEnabled && level >= Level.SENTENCES) {
            steps += SessionStep.Story(sound)
        }

        if (level == Level.CONVERSATION) {
            steps += SessionStep.Conversation(sound, sound.conversationPrompts)
        }
        return SessionPlan(sound, level, steps)
    }

    /** Приблизна кількість спроб, яку можна зробити за N хвилин (≈6 спроб/хв з урахуванням розминки). */
    internal fun trialBudget(minutes: Int): Int = (minutes * 6).coerceIn(16, 80)

    internal fun pickWarmup(sound: SpeechSound, sessionIndex: Int, count: Int): List<ArticulationExercise> {
        val pool = ArticulationExercises.forIds(sound.warmupExerciseIds)
        if (pool.isEmpty()) return emptyList()
        // Ротація: кожне заняття починаємо з іншої вправи, щоб не набридало.
        val start = (sessionIndex * count) % pool.size
        return List(minOf(count, pool.size)) { pool[(start + it) % pool.size] }
    }

    internal fun practiceItems(sound: SpeechSound, level: Level, sessionIndex: Int, count: Int): List<PracticeItem> {
        val items: List<PracticeItem> = when (level) {
            Level.SYLLABLES -> {
                // Блочна практика: спочатку прямі, далі обернені, потім між голосними.
                val s = sound.syllables
                val ordered = s.direct.map { PracticeItem(it, sound.emoji) } +
                    s.reverse.map { PracticeItem(it, sound.emoji) } +
                    s.intervocalic.map { PracticeItem(it, sound.emoji) }
                return cycle(ordered, count, shuffle = false)
            }
            Level.WORDS -> {
                // 10–16 слів за заняття, кожне повторюється кілька разів (методика: кожне слово 4–5 разів).
                val all = sound.words.all().distinctBy { it.text }
                val perSession = 12
                val start = (sessionIndex * perSession) % all.size
                val chosen = List(minOf(perSession, all.size)) { all[(start + it) % all.size] }
                cycle(chosen.map { PracticeItem(it.text, it.emoji) }, count, shuffle = true)
            }
            Level.PHRASES -> cycle(sound.phrases.map { PracticeItem(it, sound.emoji) }, count, shuffle = true)
            Level.SENTENCES -> cycle(
                (sound.sentences + sound.chystomovky).map { PracticeItem(it, sound.emoji) }, count, shuffle = true,
            )
            Level.TEXTS -> cycle(
                sound.tongueTwisters.map { PracticeItem(it, "🗣️", "Спочатку повільно, потім швидше") } +
                    sound.poems.map { PracticeItem(it, "📖", "Повторюй рядок за дорослим") },
                count, shuffle = false,
            )
            Level.DIFFERENTIATION -> {
                val pairs = differentiationPairs(sound)
                cycle(pairs.flatMap { listOf(PracticeItem("${it.a.text} — ${it.b.text}", "${it.a.emoji}${it.b.emoji}", "Скажи обидва слова, чуєш різницю?")) }, count, shuffle = true)
            }
            Level.CONVERSATION -> sound.conversationPrompts.map { PracticeItem(it, "💬") }
            Level.ISOLATED -> emptyList()
        }
        return items
    }

    /** Пари для диференціації; якщо словесних пар немає — контрастні склади. */
    internal fun differentiationPairs(sound: SpeechSound): List<MinimalPair> {
        if (sound.differentiation.isNotEmpty()) return sound.differentiation
        val other = Sounds.all.firstOrNull { it.group == sound.group && it.id != sound.id } ?: return emptyList()
        return sound.syllables.direct.zip(other.syllables.direct).map { (a, b) ->
            MinimalPair(WordItem(a, sound.emoji), WordItem(b, other.emoji), other.id)
        }
    }

    internal fun listeningRounds(sound: SpeechSound, count: Int): List<ListeningRound> {
        val pairs = differentiationPairs(sound)
        if (pairs.isEmpty()) return emptyList()
        return List(count) { i ->
            val p = pairs[i % pairs.size]
            val target = if (random.nextBoolean()) p.a else p.b
            ListeningRound(target, listOf(p.a, p.b).shuffled(random))
        }
    }

    private fun <T> cycle(source: List<T>, count: Int, shuffle: Boolean): List<T> {
        if (source.isEmpty()) return emptyList()
        val out = ArrayList<T>(count)
        while (out.size < count) {
            val chunk = if (shuffle) source.shuffled(random) else source
            for (item in chunk) {
                if (out.size >= count) break
                out += item
            }
        }
        return out
    }
}

/**
 * Правила просування рівнями (Van Riper: 80–90 % правильних; доказові огляди: ≥20 спроб і ≥2 заняття на рівні).
 */
object MasteryRules {
    const val ACCURACY_TO_ADVANCE = 0.8f
    const val MIN_TRIALS_TO_ADVANCE = 20
    const val MIN_SESSIONS_TO_ADVANCE = 2

    /** Якщо багато занять поспіль без прогресу — радимо консультацію фахівця. */
    const val STALL_SESSIONS = 6
    const val STALL_ACCURACY = 0.5f

    fun shouldAdvance(p: SoundProgress): Boolean =
        p.trialsAtLevel >= MIN_TRIALS_TO_ADVANCE &&
            p.sessionsAtLevel >= MIN_SESSIONS_TO_ADVANCE &&
            p.accuracyAtLevel >= ACCURACY_TO_ADVANCE

    fun isStalled(p: SoundProgress): Boolean =
        p.sessionsAtLevel >= STALL_SESSIONS && p.accuracyAtLevel < STALL_ACCURACY

    /** Застосувати результати заняття та за потреби перейти на наступний рівень. */
    fun applySession(p: SoundProgress, record: SessionRecord): SoundProgress {
        val scored = record.correct + record.almost / 2 // «майже» рахуємо як пів-бала
        var updated = p.copy(
            trialsAtLevel = p.trialsAtLevel + record.trials,
            correctAtLevel = p.correctAtLevel + scored,
            sessionsAtLevel = p.sessionsAtLevel + 1,
            totalTrials = p.totalTrials + record.trials,
            history = (p.history + record).takeLast(200),
        )
        if (shouldAdvance(updated)) {
            val next = updated.level.next()
            updated = if (next == null) {
                updated.copy(mastered = true)
            } else {
                updated.copy(level = next, trialsAtLevel = 0, correctAtLevel = 0, sessionsAtLevel = 0)
            }
        }
        return updated
    }
}
