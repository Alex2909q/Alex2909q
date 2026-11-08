package ua.zvukaryk.app.ai

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.BadRequestException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.beta.messages.BetaCacheControlEphemeral
import com.anthropic.models.beta.messages.BetaContentBlockParam
import com.anthropic.models.beta.messages.BetaFallbacksParam
import com.anthropic.models.beta.messages.BetaMessageParam
import com.anthropic.models.beta.messages.BetaOutputConfig
import com.anthropic.models.beta.messages.BetaStopReason
import com.anthropic.models.beta.messages.BetaTextBlockParam
import com.anthropic.models.beta.messages.BetaTool
import com.anthropic.models.beta.messages.BetaToolChoiceAuto
import com.anthropic.models.beta.messages.BetaToolResultBlockParam
import com.anthropic.models.beta.messages.MessageCreateParams
import com.fasterxml.jackson.core.type.TypeReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.zvukaryk.app.data.content.Sounds
import ua.zvukaryk.app.data.model.AppState
import ua.zvukaryk.app.data.model.Level
import ua.zvukaryk.app.data.model.Settings
import java.time.Duration

/**
 * «Звукарик-помічник»: агент на Claude API (Anthropic Java SDK) з інструментами.
 * Claude читає прогрес дитини та мовний матеріал через tool use і говорить з дитиною голосом (TTS).
 *
 * Чесність: це помічник для дорослого й оповідач для дитини, а не діагност. Усі дані, які йдуть до API, —
 * текст (ім'я, вік, статистика, тексти вправ). Аудіо ніколи не надсилається.
 */
class ClaudeAgent(
    private val settings: () -> Settings,
    private val state: () -> AppState?,
    private val speak: (String) -> Unit,
) {
    companion object {
        const val DEFAULT_MODEL = "claude-opus-5-5"
        val models = listOf(
            DEFAULT_MODEL to "Opus 5.5 — найрозумніший (за замовчуванням)",
            "claude-sonnet-5-5" to "Sonnet 5.5 — швидший і дешевший",
            "claude-haiku-5-5" to "Haiku 5.5 — найшвидший, найдешевший",
        )
        private const val FALLBACK_BETA = "server-side-fallback-2026-07-01"
        private const val MAX_TOOL_ROUNDS = 8

        /** Розбити текст казки на речення для почергового промовляння. */
        fun splitSentences(text: String): List<String> =
            text.replace("\n", " ").split(Regex("(?<=[.!?…][»\"]?)\\s+(?!—)")).map { it.trim() }.filter { it.length > 1 }
    }

    sealed class Result {
        data class Text(val text: String) : Result()
        data class Error(val message: String) : Result()
    }

    private var client: AnthropicClient? = null
    private var clientKey: String? = null

    /** Історія розмови батьків із помічником (багатоходова). */
    private val parentHistory = mutableListOf<BetaMessageParam>()

    val isConfigured: Boolean get() = settings().claudeConfigured

    fun resetParentChat() = parentHistory.clear()

    private fun clientFor(key: String): AnthropicClient {
        val existing = client
        if (existing != null && clientKey == key) return existing
        return AnthropicOkHttpClient.builder()
            .apiKey(key)
            .timeout(Duration.ofSeconds(90))
            .maxRetries(2)
            .build()
            .also { client = it; clientKey = key }
    }

    private val systemPrompt: String by lazy {
        val sounds = Sounds.all.joinToString("\n") { "- ${it.id}: звук ${it.letter} ${it.symbol}, ${it.group.title}, норма появи ${it.ageNormFrom}–${it.ageNormTo} р., образ: ${it.onomatopoeia}" }
        val levels = Level.entries.joinToString("\n") { "- ${it.name}: ${it.title}" }
        """
Ти — Звукарик, голосовий помічник-логопед у дитячому застосунку для тренування вимови українською (діти 3–8 років).
Твої відповіді озвучує синтезатор мовлення, тому пиши чистим розмовним українським текстом: без markdown, без списків, без емодзі, без англійських слів. Числа — словами.

Дві аудиторії:
1) Дитина. Коли тебе просять звернутися до дитини (привітання, пояснення звука, казка, загадка, похвала) — говори лагідно, коротко, простими словами, другою особою однини, по імені. Одне-три речення за раз. Використовуй лише слова з матеріалу звука (інструмент get_sound_material), щоб у них справді був потрібний звук. Хвали за зусилля, ніколи не кажи «неправильно» чи «погано» — кажи «спробуймо ще раз, повільніше».
2) Дорослий (батьки). Відповідай по суті, тепло, 3–6 речень, без лекцій. Спирайся на доказову базу: щоденні короткі заняття по сім-десять хвилин ефективніші за рідкі довгі; порядок автоматизації звук → склади → слова → словосполучення → речення → тексти → розрізнення → мовлення; перехід далі при вісімдесяти відсотках чітких спроб; батьки — головні судді й тренери; артикуляційна гімнастика лише коротка розминка; розпізнавання мовлення ненадійне для дітей.

Межі. Ти не ставиш діагнозів і не «ставиш» звуки — це робота логопеда. Обов'язково радь консультацію фахівця, якщо: звук не з'явився після верхньої межі вікової норми; є спотворення (язик між зубами, повітря збоку, горлове Р); сумніви щодо слуху; заїкання; помилки у голосних; шість занять без прогресу; дитина сильно засмучується. Не вигадуй статистику — спочатку виклич get_child_progress.

Інструменти: get_child_progress (профіль і прогрес), get_sound_material (матеріал звука), speak_to_child (сказати дитині вголос). Якщо тебе просять «скажи дитині» або «розкажи казку» — промовляй кожне речення окремо через speak_to_child, а у фінальній текстовій відповіді повтори весь текст для екрана.

Звуки в застосунку:
$sounds

Рівні автоматизації:
$levels
        """.trimIndent()
    }

    private fun tools(): List<BetaTool> = AgentTools.specs.map { spec ->
        val props = BetaTool.InputSchema.Properties.builder().apply {
            spec.properties.forEach { (k, v) -> putAdditionalProperty(k, JsonValue.from(v)) }
        }.build()
        BetaTool.builder()
            .name(spec.name)
            .description(spec.description)
            .inputSchema(
                BetaTool.InputSchema.builder()
                    .properties(props)
                    .required(spec.required)
                    .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                    .build(),
            )
            .strict(true)
            .build()
    }

    private fun supportsServerFallback(model: String) = !model.startsWith("claude-haiku")

    /**
     * Один хід агента: надсилаємо історію, виконуємо інструменти, поки Claude їх викликає, повертаємо текст.
     * Викликати з корутини; блокувальні виклики SDK виконуються на Dispatchers.IO.
     */
    private suspend fun run(history: MutableList<BetaMessageParam>, effort: BetaOutputConfig.Effort, maxTokens: Long = 1500L): Result = withContext(Dispatchers.IO) {
        val s = settings()
        if (!s.claudeConfigured) return@withContext Result.Error("Помічник Claude не налаштований. Додайте API-ключ у розділі «Для батьків» → «Налаштування».")
        val client = clientFor(s.claudeApiKey)
        val model = s.claudeModel.ifBlank { DEFAULT_MODEL }
        val collected = StringBuilder()
        try {
            repeat(MAX_TOOL_ROUNDS) {
                val builder = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(maxTokens)
                    .system(
                        MessageCreateParams.System.ofBetaTextBlockParams(
                            listOf(
                                BetaTextBlockParam.builder()
                                    .text(systemPrompt)
                                    .cacheControl(BetaCacheControlEphemeral.builder().ttl(BetaCacheControlEphemeral.Ttl.TTL_1H).build())
                                    .build(),
                            ),
                        ),
                    )
                    .outputConfig(BetaOutputConfig.builder().effort(effort).build())
                    .toolChoice(BetaToolChoiceAuto.builder().build())
                    .messages(history)
                tools().forEach { builder.addTool(it) }
                if (supportsServerFallback(model)) {
                    // Якщо класифікатор безпеки відхилить запит, сервер сам повторить його на резервній моделі.
                    builder.addBeta(FALLBACK_BETA).fallbacks(BetaFallbacksParam.ofDefault())
                }
                val response = client.beta().messages().create(builder.build())
                history += response.toParam()

                val stop = response.stopReason().orElse(null)
                val texts = response.content().mapNotNull { it.text().orElse(null)?.text() }
                if (texts.isNotEmpty()) collected.append(texts.joinToString(" ")).append(' ')

                if (stop == BetaStopReason.REFUSAL) {
                    return@withContext Result.Text(collected.toString().trim().ifBlank { "Вибач, на це запитання я відповісти не можу. Запитай мене про звуки й заняття." })
                }
                if (stop != BetaStopReason.TOOL_USE) {
                    return@withContext Result.Text(collected.toString().trim().ifBlank { "Я не знайшов, що відповісти. Спробуйте переформулювати." })
                }
                // Виконати всі tool_use з цієї відповіді й повернути результати одним user-повідомленням.
                val results = response.content().mapNotNull { it.toolUse().orElse(null) }.map { call ->
                    val input: Map<String, Any?> = runCatching {
                        call._input().convert(object : TypeReference<Map<String, Any?>>() {}) ?: emptyMap()
                    }.getOrDefault(emptyMap())
                    val output = runCatching { AgentTools.execute(call.name(), input, state(), speak) }
                    BetaContentBlockParam.ofToolResult(
                        BetaToolResultBlockParam.builder()
                            .toolUseId(call.id())
                            .content(output.getOrElse { "Помилка інструмента: ${it.message}" })
                            .isError(output.isFailure)
                            .build(),
                    )
                }
                history += BetaMessageParam.ofUserOfContentBlockParams(results)
            }
            Result.Text(collected.toString().trim().ifBlank { "Забагато кроків — спробуйте запитати простіше." })
        } catch (e: UnauthorizedException) {
            Result.Error("API-ключ не прийнято (401). Перевірте ключ у налаштуваннях.")
        } catch (e: PermissionDeniedException) {
            Result.Error("Доступ заборонено (403). Перевірте, чи ключ має доступ до моделі $model.")
        } catch (e: RateLimitException) {
            Result.Error("Забагато запитів або вичерпано ліміт (429). Спробуйте за хвилину.")
        } catch (e: BadRequestException) {
            Result.Error("Запит відхилено (400): ${e.message?.take(200)}")
        } catch (e: AnthropicServiceException) {
            Result.Error("Сервіс недоступний (${e.statusCode()}). Спробуйте пізніше.")
        } catch (e: AnthropicIoException) {
            Result.Error("Немає з'єднання з інтернетом.")
        } catch (e: Exception) {
            Result.Error("Помилка помічника: ${e.message?.take(200) ?: e.javaClass.simpleName}")
        }
    }

    /** Одноразовий запит «для дитини» (без збереження історії): Claude може сам викликати speak_to_child. */
    suspend fun oneShot(prompt: String, effort: BetaOutputConfig.Effort = BetaOutputConfig.Effort.LOW, maxTokens: Long = 1200L): Result =
        run(mutableListOf(BetaMessageParam.ofUser(prompt)), effort, maxTokens)

    /** Багатоходовий чат для батьків. */
    suspend fun parentChat(userText: String): Result {
        parentHistory += BetaMessageParam.ofUser(userText)
        val result = run(parentHistory, BetaOutputConfig.Effort.MEDIUM, 2000L)
        if (result is Result.Error) parentHistory.removeAt(parentHistory.lastIndex) // не лишати «висячий» user без відповіді
        return result
    }

    // ---- Готові сценарії для застосунку ----

    suspend fun greetChild(soundId: String): Result = oneShot(
        "Виклич get_child_progress, потім привітай дитину по імені одним-двома реченнями через speak_to_child і скажи, " +
            "що сьогодні тренуємо звук ${Sounds.get(soundId).letter}, і запроси натиснути кнопку «Грати». Тон веселий.",
    )

    suspend fun explainSoundDifferently(soundId: String): Result = oneShot(
        "Виклич get_sound_material для звука $soundId і get_child_progress. Поясни дитині по імені, як поставити язичок і губки " +
            "для цього звука, іншими словами, ніж стандартна підказка, через яскравий образ або міні-гру (два-три короткі речення). " +
            "Промов це через speak_to_child.",
    )

    suspend fun storyForChild(soundId: String): Result = oneShot(
        "Виклич get_sound_material для звука $soundId і get_child_progress. Склади коротку веселу казку з п'яти-шести простих речень " +
            "для дитини по імені, де в кожному реченні є два-три слова зі списку words цього звука. Речення короткі (до восьми слів), " +
            "щоб дитина могла повторити кожне за тобою. Не викликай speak_to_child — казку озвучить застосунок речення за реченням. " +
            "У відповіді дай лише текст казки.",
        effort = BetaOutputConfig.Effort.MEDIUM,
    )

    suspend fun riddlesForChild(soundId: String): Result = oneShot(
        "Виклич get_sound_material для звука $soundId. Склади три прості загадки для дошкільника, відгадки — слова зі списку words цього звука. " +
            "Формат: загадка, потім «Відгадка: слово». Без speak_to_child. Лише текст.",
    )

    suspend fun sessionSummary(soundId: String, level: Level, trials: Int, correct: Int, almost: Int): Result = oneShot(
        "Заняття зі звуком ${Sounds.get(soundId).letter} на рівні «${level.title}» завершено: спроб $trials, чітко $correct, майже $almost. " +
            "Виклич get_child_progress. Спочатку через speak_to_child похвали дитину по імені одним реченням конкретно за це заняття. " +
            "Потім у текстовій відповіді дай дорослому одну-дві поради, що робити завтра (без повторення статистики).",
    )

    suspend fun testConnection(): Result = oneShot(
        "Скажи одним реченням українською, що ти на зв'язку і готовий допомагати зі звуками. Без інструментів.",
        maxTokens = 200L,
    )
}
