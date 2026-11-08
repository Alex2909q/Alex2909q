package ua.zvukaryk.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import ua.zvukaryk.app.ui.theme.Ink
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ua.zvukaryk.app.AppViewModel
import ua.zvukaryk.app.ai.ClaudeAgent
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import ua.zvukaryk.app.data.content.Sounds
import ua.zvukaryk.app.data.model.AppState
import ua.zvukaryk.app.domain.MasteryRules
import ua.zvukaryk.app.ui.components.*
import ua.zvukaryk.app.ui.theme.Coral
import java.time.LocalDate

/** Батьківський розділ: прогрес, налаштування, звіт для логопеда, методика й застереження. */
@Composable
fun ParentScreen(state: AppState, vm: AppViewModel, onBack: () -> Unit, onOpenSound: (String) -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var resetDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(top = 4.dp)) {
        BackTopBar("Для батьків", onBack)
        TabRow(selectedTabIndex = tab) {
            listOf("Прогрес", "Помічник", "Налаштування", "Методика").forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        if (tab == 1) {
            AssistantTab(state, vm)
            return@Column
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            when (tab) {
                0 -> ProgressTab(state, onOpenSound, onShare = {
                    val report = buildReport(state)
                    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "Звукарик: звіт про заняття"); putExtra(Intent.EXTRA_TEXT, report) }
                    context.startActivity(Intent.createChooser(intent, "Надіслати звіт логопеду"))
                })
                2 -> SettingsTab(state, vm, onReset = { resetDialog = true })
                else -> MethodTab()
            }
            Spacer(Modifier.height(32.dp))
        }
    }
    if (resetDialog) {
        AlertDialog(
            onDismissRequest = { resetDialog = false },
            title = { Text("Скинути всі дані?") },
            text = { Text("Прогрес, наліпки й налаштування буде видалено з цього пристрою. Дію не можна скасувати.") },
            confirmButton = { TextButton(onClick = { resetDialog = false; vm.resetAll(); onBack() }) { Text("Скинути", color = Coral) } },
            dismissButton = { TextButton(onClick = { resetDialog = false }) { Text("Скасувати") } },
        )
    }
}

@Composable
private fun ProgressTab(state: AppState, onOpenSound: (String) -> Unit, onShare: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatPill("📅", "${state.totalSessions}", "занять", Modifier.weight(1f))
        StatPill("🎯", "${state.totalTrials}", "спроб", Modifier.weight(1f))
        StatPill("🔥", "${state.streakDays}", "серія днів", Modifier.weight(1f))
    }
    Spacer(Modifier.height(16.dp))
    val flags = redFlags(state)
    if (flags.isNotEmpty()) {
        SoftCard(color = Color(0xFFFFE3E3)) {
            Text("Варто показати дитину логопеду", style = MaterialTheme.typography.titleLarge, color = Coral)
            Spacer(Modifier.height(6.dp))
            flags.forEach { Text("• $it", Modifier.padding(vertical = 2.dp)) }
            Text("Це не діагноз, а підказка на основі ваших відповідей і статистики занять.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(12.dp))
    }
    Text("Звуки", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    if (state.validTargets(Sounds.ids).isEmpty()) Text("Ще не обрано жодного звука. Додайте їх у розділі «Усі звуки».")
    state.validTargets(Sounds.ids).forEach { id ->
        val s = Sounds.get(id)
        val p = state.progressFor(id)
        SoftCard(Modifier.padding(bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${s.emoji} ${s.title}", style = MaterialTheme.typography.titleLarge)
                    Text("${p.level.emoji} ${p.level.title}${if (p.mastered) " — освоєно" else ""}")
                    Text("На рівні: ${p.trialsAtLevel} спроб · точність ${(p.accuracyAtLevel * 100).toInt()} % · занять ${p.sessionsAtLevel}", style = MaterialTheme.typography.bodyMedium)
                    Text("Перехід далі: ≥${MasteryRules.MIN_TRIALS_TO_ADVANCE} спроб, ≥${MasteryRules.MIN_SESSIONS_TO_ADVANCE} занять, ≥${(MasteryRules.ACCURACY_TO_ADVANCE * 100).toInt()} %", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (MasteryRules.isStalled(p)) Text("⚠️ Кілька занять без прогресу — порадьтеся з логопедом", color = Coral)
                }
                TextButton(onClick = { onOpenSound(id) }) { Text("Деталі") }
            }
            if (p.history.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("Останні заняття:", style = MaterialTheme.typography.titleMedium)
                p.history.takeLast(5).reversed().forEach { r ->
                    val acc = if (r.trials == 0) 0 else ((r.correct + r.almost / 2f) / r.trials * 100).toInt()
                    Text("${LocalDate.ofEpochDay(r.epochDay)} · ${r.level.shortTitle} · ${r.trials} спроб · $acc % · ${maxOf(1, r.durationSec / 60)} хв", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    BigButton("Звіт для логопеда", onClick = onShare, modifier = Modifier.fillMaxWidth(), emoji = "📤")
}

@Composable
private fun SettingsTab(state: AppState, vm: AppViewModel, onReset: () -> Unit) {
    val s = state.settings
    SoftCard {
        Text("Заняття", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        var minutes by remember(s.sessionMinutes) { mutableFloatStateOf(s.sessionMinutes.toFloat()) }
        Text("Тривалість: ${minutes.toInt()} хв (≈${minutes.toInt() * 6} спроб)")
        Slider(value = minutes, onValueChange = { minutes = it }, onValueChangeFinished = { vm.updateSettings { st -> st.copy(sessionMinutes = minutes.toInt()) } }, valueRange = 4f..15f, steps = 10)
        ToggleRow("Розминка для язичка (2–3 вправи)", s.warmupEnabled) { v -> vm.updateSettings { st -> st.copy(warmupEnabled = v) } }
        ToggleRow("Дихальна гра перед звуком", s.breathingEnabled) { v -> vm.updateSettings { st -> st.copy(breathingEnabled = v) } }
        ToggleRow("Озвучувати зразки та підказки", s.ttsEnabled) { v -> vm.updateSettings { st -> st.copy(ttsEnabled = v) } }
        ToggleRow("Голосовий супровід: промовляти завдання й приклади", s.voiceCoachEnabled) { v -> vm.updateSettings { st -> st.copy(voiceCoachEnabled = v) } }
        var recSec by remember(s.autoRecordSeconds) { mutableFloatStateOf(s.autoRecordSeconds.toFloat()) }
        Text("Тривалість запису: ${recSec.toInt()} с")
        Slider(value = recSec, onValueChange = { recSec = it }, onValueChangeFinished = { vm.updateSettings { st -> st.copy(autoRecordSeconds = recSec.toInt()) } }, valueRange = 2f..6f, steps = 3)
    }
    Spacer(Modifier.height(12.dp))
    SoftCard {
        Text("Розпізнавання мовлення", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        ToggleRow("Кнопка «Перевір мене» (експериментально)", s.speechRecognitionEnabled) { v -> vm.updateSettings { st -> st.copy(speechRecognitionEnabled = v) } }
        Text(
            "Системне розпізнавання погано розуміє дитяче мовлення (помилок у 2–5 разів більше, ніж у дорослих) і не чує окремі звуки. " +
                "Тому воно ніколи не ставить «погано» — лише підказує. Головний суддя — дорослий. " +
                if (vm.speechChecker.isAvailable) "На цьому пристрої розпізнавання доступне." else "На цьому пристрої розпізнавання недоступне.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(12.dp))
    ClaudeSettingsCard(state, vm)
    Spacer(Modifier.height(12.dp))
    SoftCard {
        Text("Озвучення", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            if (!vm.tts.ready) "Синтезатор мовлення ще завантажується…"
            else if (vm.tts.ukrainianAvailable) "Український голос доступний ✅"
            else "Український голос не знайдено. Встановіть «Google Синтезатор мовлення» та завантажте українську мову в налаштуваннях Android (Спеціальні можливості → Синтез мовлення).",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Spacer(Modifier.height(12.dp))
    SoftCard {
        Text("Приватність", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text("Записи голосу зберігаються лише тимчасово в кеші пристрою й перезаписуються, аудіо нікуди не надсилається. Інтернет використовується лише для помічника Claude, і лише якщо ви ввели API-ключ: тоді до Anthropic API надсилаються текст запитів, ім'я, вік і статистика занять. Реклами й аналітики немає.", style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(16.dp))
    OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Скинути всі дані", color = Coral) }
}

@Composable
private fun MethodTab() {
    SoftCard {
        Text("На чому побудовано Звукарик", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "• Класична ієрархія автоматизації (Van Riper; українська логопедична школа — Шеремет, Соботович, Рібцун): " +
                "ізольований звук → склади → слова → словосполучення → речення → чистомовки й вірші → диференціація → вільне мовлення.\n\n" +
                "• Критерій переходу — 80 % чітких спроб на ≥20 спробах упродовж ≥2 занять.\n\n" +
                "• Принципи моторного навчання (Maas et al.): багато спроб за заняття (мета 40–70), блочна практика на ранніх рівнях і перемішана — пізніше; " +
                "зворотний зв'язок не на кожну спробу.\n\n" +
                "• Інтенсивність: короткі щоденні заняття ефективніші за рідкі довгі (Allen 2013; Sugden et al. 2018).\n\n" +
                "• Батьки як головні тренери: домашня практика з підказками дає результат, якщо є «знаю чому» і «знаю як» (Sugden 2016; Pritchard 2024).\n\n" +
                "• Слуховий самоконтроль: дитина записує й слухає себе; фонематичний слух тренується грою «Вухо-детектив» (мінімальні пари).\n\n" +
                "• Чесна техніка: мікрофон вимірює гучність і тривалість, а не «правильність». Розпізнавання мовлення дитини ненадійне, тому воно лише підказує.",
        )
    }
    Spacer(Modifier.height(12.dp))
    SoftCard(color = Color(0xFFFFF1E6)) {
        Text("Про розминку для язичка", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Артикуляційна гімнастика — традиційна частина українських занять і допомагає дитині відчути язик і губи. " +
                "Водночас міжнародні огляди (McCauley 2009; Cochrane 2015) не підтверджують, що немовленнєві вправи самі по собі покращують вимову. " +
                "Тому в Звукарику розминка коротка (1–2 хвилини), а основний час іде на сам звук і слова. Її можна вимкнути в налаштуваннях.",
        )
    }
    Spacer(Modifier.height(12.dp))
    SoftCard(color = Color(0xFFFFE3E3)) {
        Text("Коли потрібен логопед", style = MaterialTheme.typography.titleLarge, color = Coral)
        Spacer(Modifier.height(6.dp))
        Text(
            "• Звук не з'явився після верхньої межі вікової норми (свистячі — 4 р., шиплячі — 5 р., Л і Р — 6 р.).\n" +
                "• Спотворення: язик між зубами, повітря збоку, «горлове» Р — у будь-якому віці.\n" +
                "• Після 4 років батьки не розуміють більшість мовлення; після 5 — не розуміють незнайомі.\n" +
                "• Помилки в голосних, перестановки складів, щоразу різна вимова того самого слова.\n" +
                "• Сумніви щодо слуху, заїкання, дитина соромиться говорити.\n" +
                "• 6 занять поспіль без прогресу на одному рівні.\n\n" +
                "Застосунок не ставить звуки й не замінює фахівця — він допомагає робити вдома те, що логопед «задав».",
        )
    }
    Spacer(Modifier.height(12.dp))
    SoftCard {
        Text("Правила домашніх занять", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "1. Щодня, в один і той самий час, 7–10 хвилин.\n" +
                "2. Сидимо поруч, перед дзеркалом, дорослий показує сам.\n" +
                "3. Хвалимо за зусилля і за чіткість, не за швидкість. Жодних «ти не вмієш».\n" +
                "4. Не більше 1–2 звуків одночасно, у порядку: свистячі → шиплячі → Л → Р.\n" +
                "5. Зупиняємося раніше, ніж дитина втомиться.\n" +
                "6. Нагадуємо про звук у побуті: «чую гарне Ш!» — так звук переходить у мовлення.",
        )
    }
}

private fun redFlags(state: AppState): List<String> {
    val f = mutableListOf<String>()
    val s = state.screening
    val age = state.profile.ageYears
    if (s.hearingConcern) f += "Є сумніви щодо слуху — спочатку перевірка слуху."
    if (s.vowelErrors) f += "Помилки у голосних — нетиповий тип помилок у будь-якому віці."
    if (s.inconsistent) f += "Непослідовна вимова того самого слова — потребує оцінки фахівця."
    if (s.stuttering) f += "Запинки / заїкання — окрема консультація логопеда."
    if (s.frustration) f += "Дитина засмучується через нерозуміння — не відкладайте консультацію."
    if (age >= 4 && s.intelligibilityParents <= 2) f += "У $age р. батьки розуміють дитину лише частково."
    if (age >= 5 && s.intelligibilityStrangers <= 1) f += "У $age р. незнайомі дорослі майже не розуміють дитину."
    s.problemSounds.map { Sounds.get(it) }.filter { it.ageNormTo <= age }.forEach { f += "Звук ${it.letter} ще не сформовано у $age р. (норма до ${it.ageNormTo} р.)." }
    state.validTargets(Sounds.ids).map { state.progressFor(it) }.filter { MasteryRules.isStalled(it) }.forEach { f += "Звук ${Sounds.get(it.soundId).letter}: ${it.sessionsAtLevel} занять без прогресу." }
    return f
}

internal fun buildReport(state: AppState): String = buildString {
    appendLine("Звукарик — звіт про домашні заняття")
    appendLine("Дитина: ${state.profile.name}, ${state.profile.ageYears} р.  Дата: ${LocalDate.now()}")
    appendLine("Занять: ${state.totalSessions}, спроб: ${state.totalTrials}, серія: ${state.streakDays} дн.")
    appendLine()
    state.validTargets(Sounds.ids).forEach { id ->
        val s = Sounds.get(id); val p = state.progressFor(id)
        appendLine("${s.title}: рівень «${p.level.title}»${if (p.mastered) " (освоєно)" else ""}; на рівні ${p.trialsAtLevel} спроб, точність ${(p.accuracyAtLevel * 100).toInt()} %, занять ${p.sessionsAtLevel}; усього спроб ${p.totalTrials}")
        p.history.takeLast(10).forEach { r ->
            val acc = if (r.trials == 0) 0 else ((r.correct + r.almost / 2f) / r.trials * 100).toInt()
            appendLine("  ${LocalDate.ofEpochDay(r.epochDay)} · ${r.level.shortTitle} · ${r.trials} спроб · $acc % чітко · ${maxOf(1, r.durationSec / 60)} хв")
        }
    }
    val flags = redFlags(state)
    if (flags.isNotEmpty()) { appendLine(); appendLine("Зауваження з анкети:"); flags.forEach { appendLine("  • $it") } }
    appendLine()
    appendLine("Сформовано застосунком Звукарик. Оцінки спроб виставляв дорослий під час занять.")
}


/* ----------------------------- Помічник Claude: налаштування ----------------------------- */

@Composable
private fun ClaudeSettingsCard(state: AppState, vm: AppViewModel) {
    val s = state.settings
    var key by remember(s.claudeApiKey) { mutableStateOf(s.claudeApiKey) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    SoftCard(color = Color(0xFFF3ECFF)) {
        Text("✨ Помічник Claude (Anthropic API)", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Claude говорить з дитиною голосом: вітає, пояснює звук по-своєму, складає казки й загадки зі словами на потрібний звук, " +
                "підбиває підсумок заняття і відповідає на запитання батьків із доступом до прогресу дитини (вкладка «Помічник»).",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        ToggleRow("Увімкнути помічника", s.claudeEnabled) { v -> vm.updateSettings { st -> st.copy(claudeEnabled = v) } }
        ToggleRow("Казка від Звукарика в заняттях (з рівня «Речення»)", s.claudeStoryEnabled) { v -> vm.updateSettings { st -> st.copy(claudeStoryEnabled = v) } }
        OutlinedTextField(
            value = key, onValueChange = { key = it.trim() },
            label = { Text("API-ключ Anthropic (sk-ant-…)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            supportingText = { Text("Ключ зберігається лише на цьому пристрої.") },
        )
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.updateSettings { st -> st.copy(claudeApiKey = key) } }, enabled = key != s.claudeApiKey) { Text("Зберегти ключ") }
            OutlinedButton(
                enabled = !testing && s.claudeConfigured,
                onClick = {
                    testing = true; testResult = null
                    scope.launch {
                        testResult = when (val r = vm.claude.testConnection()) {
                            is ClaudeAgent.Result.Text -> { vm.voice.say(r.text); "✅ ${r.text}" }
                            is ClaudeAgent.Result.Error -> "⚠️ ${r.message}"
                        }
                        testing = false
                    }
                },
            ) { Text(if (testing) "Перевіряю…" else "Перевірити зв'язок") }
        }
        testResult?.let { Text(it, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.height(10.dp))
        Text("Модель", style = MaterialTheme.typography.titleMedium)
        ClaudeAgent.models.forEach { (id, title) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = s.claudeModel == id, onClick = { vm.updateSettings { st -> st.copy(claudeModel = id) } })
                Text(title, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Як отримати ключ: console.anthropic.com → API Keys → Create Key. Увага: підписка Claude Pro/Max (claude.ai) не дає доступу до API — " +
                "потрібен окремий акаунт розробника з оплатою за використання. Одне заняття з казкою й підсумком коштує орієнтовно " +
                "кілька центів на Opus 5.5 і частки цента на Haiku 5.5.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/* ----------------------------- Помічник Claude: чат для батьків ----------------------------- */

private data class ChatMsg(val fromUser: Boolean, val text: String, val isError: Boolean = false)

@Composable
private fun AssistantTab(state: AppState, vm: AppViewModel) {
    val messages = remember { mutableStateListOf<ChatMsg>() }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val suggestions = listOf(
        "Як у нас справи зі звуками? Що робити далі?",
        "Дитина каже «либа» замість «риба». Це нормально для її віку?",
        "Склади план домашніх занять на тиждень.",
        "Як правильно хвалити, щоб дитина не втрачала інтерес?",
        "Коли нам точно потрібен логопед?",
    )
    fun send(text: String) {
        if (text.isBlank() || busy) return
        messages += ChatMsg(true, text)
        input = ""; busy = true
        scope.launch {
            when (val r = vm.claude.parentChat(text)) {
                is ClaudeAgent.Result.Text -> messages += ChatMsg(false, r.text)
                is ClaudeAgent.Result.Error -> messages += ChatMsg(false, r.message, isError = true)
            }
            busy = false
            listState.animateScrollToItem(maxOf(0, messages.size - 1))
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (!state.settings.claudeConfigured) {
            Spacer(Modifier.height(16.dp))
            SoftCard(color = Color(0xFFFFF1E6)) {
                Text("Помічник не налаштований", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text("Увімкніть помічника й додайте API-ключ Anthropic у вкладці «Налаштування». Після цього тут можна ставити запитання про прогрес дитини, а Звукарик зможе розповідати дитині казки голосом.")
            }
            return
        }
        LazyColumn(Modifier.weight(1f), state = listState, contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (messages.isEmpty()) {
                item {
                    SoftCard(color = Color(0xFFF3ECFF)) {
                        Text("✨ Запитайте Звукарика", style = MaterialTheme.typography.titleLarge)
                        Text("Він бачить прогрес дитини, матеріал звуків і методику. Не ставить діагнозів — радить, коли йти до логопеда.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        suggestions.forEach { q -> TextButton(onClick = { send(q) }) { Text("• $q") } }
                    }
                }
            }
            items(messages) { m ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start) {
                    Column(
                        Modifier
                            .fillMaxWidth(0.88f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (m.fromUser) MaterialTheme.colorScheme.primary else if (m.isError) Color(0xFFFFE3E3) else Color.White)
                            .padding(12.dp),
                    ) {
                        Text(m.text, color = if (m.fromUser) Color.White else Ink, style = MaterialTheme.typography.bodyLarge)
                        if (!m.fromUser && !m.isError) {
                            TextButton(onClick = { vm.voice.say(m.text) }, contentPadding = PaddingValues(0.dp)) { Text("🔊 Озвучити") }
                        }
                    }
                }
            }
            if (busy) item { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("Звукарик думає…") } }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Запитання про заняття…") }, maxLines = 3)
            Spacer(Modifier.width(8.dp))
            Button(onClick = { send(input) }, enabled = !busy && input.isNotBlank()) { Text("➤") }
        }
        TextButton(onClick = { messages.clear(); vm.claude.resetParentChat() }, modifier = Modifier.align(Alignment.End)) { Text("Нова розмова") }
    }
}
