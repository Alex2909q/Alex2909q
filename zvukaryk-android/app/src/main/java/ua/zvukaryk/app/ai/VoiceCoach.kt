package ua.zvukaryk.app.ai

import ua.zvukaryk.app.data.content.Sounds
import ua.zvukaryk.app.data.model.Level
import ua.zvukaryk.app.data.model.SpeechSound
import ua.zvukaryk.app.domain.SessionStep
import ua.zvukaryk.app.speech.TtsManager

/**
 * Голосовий супровід (працює офлайн через системний TTS): промовляє завдання кожного кроку,
 * наводить приклади й коментує. Це базовий «голос застосунку»; Claude-помічник додає персоналізовані репліки.
 */
class VoiceCoach(private val tts: TtsManager, private val enabled: () -> Boolean) {

    fun say(text: String, slow: Boolean = false) {
        if (enabled()) tts.speak(text, slow)
    }

    fun stop() = tts.stop()

    fun greeting(name: String, sound: SpeechSound, practicedToday: Boolean): String {
        val who = name.ifBlank { "друже" }
        return if (practicedToday) "Привіт, $who! Сьогодні ми вже займалися — молодець! Хочеш пограти ще зі звуком ${sound.letter}?"
        else "Привіт, $who! Сьогодні тренуємо звук ${sound.letter}. ${sound.onomatopoeia}. Натисни «Грати», і почнемо!"
    }

    /** Текст завдання для кроку заняття — з прикладами. */
    fun taskFor(step: SessionStep): String = when (step) {
        is SessionStep.Warmup -> {
            val names = step.exercises.joinToString(", ") { it.name }
            "Спочатку розминка для язичка: $names. Сідай зручно, дивись у дзеркало й повторюй за ротиком на екрані."
        }
        is SessionStep.Breathing -> "Дихальна гра «${step.game.title}». ${step.game.instruction}"
        is SessionStep.Placement -> "Подивись, як сказати звук ${step.sound.letter}. ${step.sound.articulation.childCue}"
        is SessionStep.Isolated -> "Гра «${step.sound.isolated.title}». ${step.sound.isolated.instruction}"
        is SessionStep.Practice -> practiceTask(step.sound, step.level, step.items.take(3).map { it.text })
        is SessionStep.Listening -> {
            val first = step.rounds.firstOrNull()
            val ex = first?.options?.joinToString(" або ") { it.text } ?: ""
            "Гра «Вухо-детектив». Я скажу слово, а ти торкнись правильної картинки. Наприклад: $ex. Слухай уважно!"
        }
        is SessionStep.Conversation -> "А тепер поговорімо. ${step.prompts.firstOrNull() ?: ""} Стеж за звуком ${step.sound.letter}."
        is SessionStep.Story -> "Зараз Звукарик розкаже тобі казку зі словами на звук ${step.sound.letter}. Слухай речення і повторюй за мною."
    }

    private fun practiceTask(sound: SpeechSound, level: Level, examples: List<String>): String {
        val ex = examples.filter { it.length <= 40 }.take(if (level <= Level.WORDS) 3 else 1).joinToString(", ")
        val example = if (ex.isNotBlank()) " Наприклад: $ex." else ""
        return when (level) {
            Level.SYLLABLES -> "Тепер склади зі звуком ${sound.letter}. Слухай і повторюй чітко, трохи протягуючи звук.$example"
            Level.WORDS -> "Тепер слова зі звуком ${sound.letter}. Дивись на картинку, слухай зразок і повторюй.$example"
            Level.PHRASES -> "Тепер словосполучення. Говори повільно й виділяй звук ${sound.letter}.$example"
            Level.SENTENCES -> "Тепер речення й чистомовки. Можна плескати в долоні на кожне слово.$example"
            Level.TEXTS -> "Тепер скоромовки й віршики. Спочатку повільно, потім швидше.$example"
            Level.DIFFERENTIATION -> "Тепер два схожих слова поруч. Скажи обидва й послухай, чим вони різняться.$example"
            Level.CONVERSATION -> "Тепер вільна розмова. Говори, що думаєш, а я слухатиму звук ${sound.letter}."
            Level.ISOLATED -> "Скажи звук ${sound.letter} окремо: ${sound.onomatopoeia}."
        }
    }

    fun levelUpAnnouncement(sound: SpeechSound, newLevel: Level): String =
        "Ура! Звук ${sound.letter} переходить на новий рівень: ${newLevel.title}. Ти велика молодчинка!"

    companion object {
        fun soundExamples(soundId: String, count: Int = 3): List<String> =
            Sounds.find(soundId)?.words?.initial?.take(count)?.map { it.text } ?: emptyList()
    }
}
