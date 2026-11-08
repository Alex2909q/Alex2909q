package ua.zvukaryk.app.domain

import ua.zvukaryk.app.data.model.Level
import ua.zvukaryk.app.data.model.SpeechSound

/**
 * Підсумок заняття без інтернету: конкретна похвала дитині (озвучується) і порада дорослому на завтра.
 * Похвала — за зусилля і конкретний результат, без порівнянь і без слова «погано».
 */
object SessionFeedback {
    data class Feedback(val praise: String, val tip: String)

    fun build(name: String, sound: SpeechSound, level: Level, trials: Int, correct: Int, almost: Int): Feedback {
        val who = name.ifBlank { "друже" }
        val accuracy = if (trials == 0) 0 else ((correct + almost / 2f) / trials * 100).toInt()
        val praise = when {
            trials == 0 -> "$who, дякую, що позаймався зі мною! Завтра пограємо ще."
            accuracy >= 80 -> "$who, ти чудово сказав звук ${sound.letter}! Я чув його майже в кожному слові. Ти справжній чемпіон!"
            accuracy >= 50 -> "$who, ти дуже старався, і звук ${sound.letter} уже звучить усе краще. Так тримати!"
            else -> "$who, ти сьогодні багато працював. Звук ${sound.letter} складний, але ми з тобою впораємося!"
        }
        val next = level.next()
        val tip = buildString {
            when {
                trials == 0 -> append("Сьогодні не було оцінених спроб. Завтра спробуйте в той самий час, навіть 3–4 хвилини дадуть користь.")
                trials < 15 -> append("Спроб було небагато ($trials). Для закріплення звука потрібно 40–70 спроб за заняття — завтра спробуйте пройти всі картки.")
                accuracy >= 80 && next != null -> append("Чудова точність ($accuracy %). Ще одне таке заняття — і рівень «${next.title}» відкриється автоматично.")
                accuracy >= 80 -> append("Чудова точність ($accuracy %). Залишилось перенести звук у звичайні розмови — нагадуйте про нього в побуті.")
                accuracy >= 50 -> append("Точність $accuracy % — звук ще закріплюється, це нормально. ${levelTip(level, sound)}")
                else -> append("Точність $accuracy %. Поверніться на крок назад: кілька хвилин складів або ізольованого звука перед словами. Якщо так кілька занять поспіль — порадьтеся з логопедом.")
            }
            append(' ')
            append(dailyLifeTip(sound))
        }
        return Feedback(praise, tip)
    }

    private fun levelTip(level: Level, sound: SpeechSound): String = when (level) {
        Level.ISOLATED -> "Покажіть положення язика на собі перед дзеркалом і тягніть звук разом."
        Level.SYLLABLES -> "Протягуйте звук у складах: «с-с-са», так дитині легше його не загубити."
        Level.WORDS -> "Якщо звук губиться в слові, скажіть спочатку склад, а потім слово: «са — сани»."
        Level.PHRASES -> "Говоріть фрази повільно, плескаючи на кожне слово."
        Level.SENTENCES -> "Чистомовки краще заходять у ритмі: тупайте або плескайте в такт."
        Level.TEXTS -> "Скоромовку вчіть частинами: спочатку повільно, потім швидше."
        Level.DIFFERENTIATION -> "Нагадуйте різницю: де язичок і який вітерець, холодний чи теплий."
        Level.CONVERSATION -> "У розмові м'яко відзначайте вдалі слова: «чую гарне ${sound.letter}!»"
    }

    private fun dailyLifeTip(sound: SpeechSound): String {
        val examples = sound.words.initial.take(3).joinToString(", ") { it.text }
        return "Гра на день: знайдіть удома предмети зі звуком ${sound.letter}, наприклад $examples."
    }
}
