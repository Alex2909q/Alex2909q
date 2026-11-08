package ua.zvukaryk.app.ai

import ua.zvukaryk.app.data.content.HelperContent
import ua.zvukaryk.app.data.content.Sounds
import ua.zvukaryk.app.data.model.AppState
import ua.zvukaryk.app.data.model.Level
import ua.zvukaryk.app.domain.Advisor
import ua.zvukaryk.app.domain.SessionFeedback

/**
 * «Звукарик-помічник». За замовчуванням працює повністю офлайн (вбудовані пояснення, казки, загадки,
 * підсумки й порадник). Якщо батьки увімкнули Claude і ввели API-ключ, використовує Claude, а при будь-якій
 * помилці мережі чи ключа тихо повертається до офлайн-режиму. Дитина ніколи не лишається без відповіді.
 */
class Assistant(
    private val claude: ClaudeAgent,
    private val voice: VoiceCoach,
    private val state: () -> AppState?,
) {
    data class Reply(val text: String, val fromClaude: Boolean)

    private var rotation = 0

    val usesClaude: Boolean get() = claude.isConfigured

    private fun <T> pick(list: List<T>?): T? = list?.takeIf { it.isNotEmpty() }?.let { it[(rotation++) % it.size] }

    /** Пояснити звук іншими словами (озвучується). */
    suspend fun explain(soundId: String): Reply {
        if (claude.isConfigured) {
            val r = claude.explainSoundDifferently(soundId)
            if (r is ClaudeAgent.Result.Text) return Reply(r.text, true)
        }
        val text = pick(HelperContent.explanations[soundId]) ?: Sounds.get(soundId).articulation.childCue
        voice.say(text)
        return Reply(text, false)
    }

    /** Загадка: озвучуємо загадку, відгадку показуємо дорослому на екрані. */
    suspend fun riddle(soundId: String): Reply {
        if (claude.isConfigured) {
            val r = claude.riddlesForChild(soundId)
            if (r is ClaudeAgent.Result.Text) { voice.say(r.text.lineSequence().firstOrNull().orEmpty()); return Reply(r.text, true) }
        }
        val riddle = pick(HelperContent.riddles[soundId]) ?: return Reply("Загадок для цього звука поки немає.", false)
        voice.say(riddle.text)
        return Reply("${riddle.text}\nВідгадка: ${riddle.answer}", false)
    }

    /** Речення казки для повторення. */
    suspend fun storySentences(soundId: String): Pair<List<String>, Boolean> {
        if (claude.isConfigured) {
            val r = claude.storyForChild(soundId)
            if (r is ClaudeAgent.Result.Text) {
                val parts = ClaudeAgent.splitSentences(r.text).take(8)
                if (parts.size >= 3) return parts to true
            }
        }
        val story = pick(HelperContent.stories[soundId]) ?: emptyList()
        return story to false
    }

    /** Казка цілком (на екрані звука): озвучується повільно. */
    suspend fun tellStory(soundId: String): Reply {
        val (sentences, fromClaude) = storySentences(soundId)
        val text = sentences.joinToString(" ")
        if (text.isNotBlank()) voice.say(text, slow = true)
        return Reply(text.ifBlank { "Казки для цього звука поки немає." }, fromClaude)
    }

    /** Підсумок заняття: похвала дитині озвучується, порада дорослому повертається текстом. */
    suspend fun sessionSummary(soundId: String, level: Level, trials: Int, correct: Int, almost: Int): Reply {
        if (claude.isConfigured && trials > 0) {
            val r = claude.sessionSummary(soundId, level, trials, correct, almost)
            if (r is ClaudeAgent.Result.Text) return Reply(r.text, true)
        }
        val fb = SessionFeedback.build(state()?.profile?.name.orEmpty(), Sounds.get(soundId), level, trials, correct, almost)
        voice.say(fb.praise)
        return Reply(fb.tip, false)
    }

    /** Запитання батьків: Claude, якщо налаштовано, інакше вбудований порадник. */
    suspend fun parentAnswer(question: String): Reply {
        if (claude.isConfigured) {
            val r = claude.parentChat(question)
            if (r is ClaudeAgent.Result.Text) return Reply(r.text, true)
        }
        val s = state() ?: AppState()
        return Reply(Advisor.answer(question, s), false)
    }

    fun resetParentChat() = claude.resetParentChat()
}
