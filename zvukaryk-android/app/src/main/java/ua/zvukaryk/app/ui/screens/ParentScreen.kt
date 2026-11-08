package ua.zvukaryk.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
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
            listOf("Прогрес", "Налаштування", "Методика").forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            when (tab) {
                0 -> ProgressTab(state, onOpenSound, onShare = {
                    val report = buildReport(state)
                    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "Звукарик: звіт про заняття"); putExtra(Intent.EXTRA_TEXT, report) }
                    context.startActivity(Intent.createChooser(intent, "Надіслати звіт логопеду"))
                })
                1 -> SettingsTab(state, vm, onReset = { resetDialog = true })
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
    if (state.targetSounds.isEmpty()) Text("Ще не обрано жодного звука. Додайте їх у розділі «Усі звуки».")
    state.targetSounds.forEach { id ->
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
        Text("Тривалість: ${s.sessionMinutes} хв (≈${s.sessionMinutes * 6} спроб)")
        Slider(value = s.sessionMinutes.toFloat(), onValueChange = { vm.updateSettings { st -> st.copy(sessionMinutes = it.toInt()) } }, valueRange = 4f..15f, steps = 10)
        ToggleRow("Розминка для язичка (2–3 вправи)", s.warmupEnabled) { v -> vm.updateSettings { st -> st.copy(warmupEnabled = v) } }
        ToggleRow("Дихальна гра перед звуком", s.breathingEnabled) { v -> vm.updateSettings { st -> st.copy(breathingEnabled = v) } }
        ToggleRow("Озвучувати зразки та підказки", s.ttsEnabled) { v -> vm.updateSettings { st -> st.copy(ttsEnabled = v) } }
        Text("Тривалість запису: ${s.autoRecordSeconds} с")
        Slider(value = s.autoRecordSeconds.toFloat(), onValueChange = { vm.updateSettings { st -> st.copy(autoRecordSeconds = it.toInt()) } }, valueRange = 2f..6f, steps = 3)
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
        Text("Записи голосу зберігаються лише тимчасово в кеші пристрою й перезаписуються. Застосунок не має доступу до інтернету, не збирає даних і не показує реклами.", style = MaterialTheme.typography.bodyMedium)
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
    state.targetSounds.map { state.progressFor(it) }.filter { MasteryRules.isStalled(it) }.forEach { f += "Звук ${Sounds.get(it.soundId).letter}: ${it.sessionsAtLevel} занять без прогресу." }
    return f
}

internal fun buildReport(state: AppState): String = buildString {
    appendLine("Звукарик — звіт про домашні заняття")
    appendLine("Дитина: ${state.profile.name}, ${state.profile.ageYears} р.  Дата: ${LocalDate.now()}")
    appendLine("Занять: ${state.totalSessions}, спроб: ${state.totalTrials}, серія: ${state.streakDays} дн.")
    appendLine()
    state.targetSounds.forEach { id ->
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
