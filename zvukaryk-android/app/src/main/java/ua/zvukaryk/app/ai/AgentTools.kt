package ua.zvukaryk.app.ai

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import ua.zvukaryk.app.data.content.ArticulationExercises
import ua.zvukaryk.app.data.content.Sounds
import ua.zvukaryk.app.data.model.AppState
import ua.zvukaryk.app.domain.MasteryRules

/**
 * Інструменти агента (tool use). Чиста логіка без SDK — її легко тестувати.
 * Claude викликає їх, щоб прочитати прогрес дитини, узяти мовний матеріал або сказати щось дитині вголос.
 */
object AgentTools {
    const val GET_CHILD_PROGRESS = "get_child_progress"
    const val GET_SOUND_MATERIAL = "get_sound_material"
    const val SPEAK_TO_CHILD = "speak_to_child"

    /** Опис інструментів у форматі JSON Schema (для BetaTool.inputSchema). */
    data class ToolSpec(val name: String, val description: String, val properties: Map<String, Map<String, Any>>, val required: List<String>)

    val specs: List<ToolSpec> = listOf(
        ToolSpec(
            GET_CHILD_PROGRESS,
            "Повертає профіль дитини (ім'я, вік), цільові звуки, рівень автоматизації й точність по кожному звуку, " +
                "історію останніх занять, відповіді скринінгу та червоні прапорці. Викликай перед тим, як давати пораду про прогрес.",
            emptyMap(), emptyList(),
        ),
        ToolSpec(
            GET_SOUND_MATERIAL,
            "Повертає логопедичний матеріал для звука: правильну артикуляцію, підказку для дитини, типові помилки, " +
                "слова з картинками за позицією, чистомовки, скоромовки, пари для розрізнення та вправи розминки. " +
                "Використовуй його слова, коли складаєш казку, загадки чи приклади — не вигадуй складних слів.",
            mapOf(
                "sound_id" to mapOf("type" to "string", "description" to "Ідентифікатор звука: s, z, c, sh, zh, ch, shch, l, r"),
            ),
            listOf("sound_id"),
        ),
        ToolSpec(
            SPEAK_TO_CHILD,
            "Промовляє текст уголос дитині через синтезатор мовлення. Одне-два короткі речення українською, без емодзі, списків і markdown. " +
                "Використовуй, коли тебе просять щось сказати дитині безпосередньо (привітання, підказку, похвалу, речення казки).",
            mapOf("text" to mapOf("type" to "string", "description" to "Що сказати дитині")),
            listOf("text"),
        ),
    )

    /** Виконати інструмент. [speak] — побічна дія озвучення. Повертає текст результату для Claude. */
    fun execute(name: String, input: Map<String, Any?>, state: AppState?, speak: (String) -> Unit): String = when (name) {
        GET_CHILD_PROGRESS -> childProgress(state).toString()
        GET_SOUND_MATERIAL -> soundMaterial(input["sound_id"]?.toString().orEmpty()).toString()
        SPEAK_TO_CHILD -> {
            val text = input["text"]?.toString().orEmpty().trim()
            if (text.isBlank()) "Порожній текст — нічого не сказано." else { speak(text); "Сказано дитині: «$text»" }
        }
        else -> "Невідомий інструмент: $name"
    }

    fun childProgress(state: AppState?): JsonObject = buildJsonObject {
        if (state == null) { put("error", "стан ще не завантажено"); return@buildJsonObject }
        putJsonObject("child") {
            put("name", state.profile.name)
            put("age_years", state.profile.ageYears)
        }
        put("streak_days", state.streakDays)
        put("total_sessions", state.totalSessions)
        put("total_trials", state.totalTrials)
        putJsonArray("target_sounds") {
            state.validTargets(Sounds.ids).forEach { id ->
                val s = Sounds.get(id); val p = state.progressFor(id)
                add(buildJsonObject {
                    put("sound_id", id); put("letter", s.letter); put("age_norm", "${s.ageNormFrom}-${s.ageNormTo}")
                    put("level", p.level.name); put("level_title", p.level.title); put("mastered", p.mastered)
                    put("trials_at_level", p.trialsAtLevel); put("accuracy_at_level", (p.accuracyAtLevel * 100).toInt())
                    put("sessions_at_level", p.sessionsAtLevel); put("total_trials", p.totalTrials)
                    put("ready_to_advance", MasteryRules.shouldAdvance(p)); put("stalled", MasteryRules.isStalled(p))
                    putJsonArray("recent_sessions") {
                        p.history.takeLast(5).forEach { r ->
                            add(buildJsonObject {
                                put("epoch_day", r.epochDay); put("level", r.level.name); put("trials", r.trials)
                                put("correct", r.correct); put("almost", r.almost); put("duration_sec", r.durationSec)
                            })
                        }
                    }
                })
            }
        }
        putJsonObject("screening") {
            val sc = state.screening
            put("parents_understand_1_3", sc.intelligibilityParents); put("strangers_understand_1_3", sc.intelligibilityStrangers)
            put("hearing_concern", sc.hearingConcern); put("vowel_errors", sc.vowelErrors); put("inconsistent", sc.inconsistent)
            put("frustration", sc.frustration); put("stuttering", sc.stuttering)
            putJsonArray("problem_sounds") { sc.problemSounds.forEach { add(JsonPrimitive(it)) } }
        }
    }

    fun soundMaterial(soundId: String): JsonObject {
        val s = Sounds.find(soundId) ?: return buildJsonObject { put("error", "невідомий звук '$soundId'; доступні: ${Sounds.ids.joinToString()}") }
        return buildJsonObject {
            put("sound_id", s.id); put("letter", s.letter); put("title", s.title); put("group", s.group.title)
            put("age_norm", "${s.ageNormFrom}-${s.ageNormTo} років"); put("onomatopoeia", s.onomatopoeia)
            putJsonObject("articulation") {
                put("lips", s.articulation.lips); put("tongue", s.articulation.tongue)
                put("air", s.articulation.air); put("voice", s.articulation.voice); put("child_cue", s.articulation.childCue)
            }
            put("common_mistakes", s.commonMistakes)
            putJsonObject("syllables") {
                put("direct", arr(s.syllables.direct)); put("reverse", arr(s.syllables.reverse)); put("intervocalic", arr(s.syllables.intervocalic))
            }
            putJsonObject("words") {
                put("initial", arr(s.words.initial.map { it.text })); put("medial", arr(s.words.medial.map { it.text })); put("final", arr(s.words.final.map { it.text }))
            }
            put("phrases", arr(s.phrases)); put("sentences", arr(s.sentences)); put("chystomovky", arr(s.chystomovky))
            put("tongue_twisters", arr(s.tongueTwisters)); put("conversation_prompts", arr(s.conversationPrompts))
            put("minimal_pairs", arr(s.differentiation.map { "${it.a.text} — ${it.b.text} (${it.otherSoundId})" }))
            put("warmup_exercises", arr(ArticulationExercises.forIds(s.warmupExerciseIds).map { "${it.name}: ${it.parentInstruction}" }))
        }
    }

    private fun arr(items: List<String>): JsonArray = buildJsonArray { items.forEach { add(JsonPrimitive(it)) } }
}
