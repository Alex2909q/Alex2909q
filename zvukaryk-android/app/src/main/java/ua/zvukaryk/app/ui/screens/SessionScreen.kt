package ua.zvukaryk.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ua.zvukaryk.app.AppViewModel
import ua.zvukaryk.app.data.content.BreathingGame
import ua.zvukaryk.app.data.model.*
import ua.zvukaryk.app.domain.ListeningRound
import ua.zvukaryk.app.domain.PracticeItem
import ua.zvukaryk.app.domain.SessionStep
import ua.zvukaryk.app.speech.SpeechChecker
import ua.zvukaryk.app.ui.components.*
import ua.zvukaryk.app.ui.theme.*

private val praise = listOf("Молодець!", "Чудово!", "Супер!", "Так тримати!", "Класно виходить!", "Яка краса!", "Чую чіткий звук!")
private val encourage = listOf("Спробуємо ще разок!", "Майже! Ще раз, повільніше.", "Подивись на ротик і повтори.", "Гарна спроба! Ще раз.")

/** Заняття: послідовність кроків із лічильником продуктивних спроб і підсумком. */
@Composable
fun SessionScreen(state: AppState, vm: AppViewModel, soundId: String, onExit: () -> Unit) {
    val plan = remember(soundId) { vm.plan(state, soundId) }
    val context = LocalContext.current
    var hasMic by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasMic = it }
    LaunchedEffect(Unit) { if (!hasMic) permission.launch(Manifest.permission.RECORD_AUDIO) }

    var stepIndex by remember { mutableIntStateOf(0) }
    var trials by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var almost by remember { mutableIntStateOf(0) }
    var listeningCorrect by remember { mutableIntStateOf(0) }
    var listeningTotal by remember { mutableIntStateOf(0) }
    val startedAt = remember { System.currentTimeMillis() }
    var finished by remember { mutableStateOf(false) }
    var exitDialog by remember { mutableStateOf(false) }

    val onRate: (Rating) -> Unit = { r ->
        trials++
        when (r) { Rating.CORRECT -> correct++; Rating.ALMOST -> almost++; Rating.WRONG -> {} }
    }
    val nextStep: () -> Unit = { if (stepIndex + 1 < plan.steps.size) stepIndex++ else finished = true }

    DisposableEffect(Unit) { onDispose { vm.recorder.stop(); vm.recorder.stopPlayback(); vm.tts.stop() } }

    if (finished) {
        FinishScreen(
            vm = vm, sound = plan.sound, trials = trials, correct = correct, almost = almost,
            listeningCorrect = listeningCorrect, listeningTotal = listeningTotal,
            durationSec = ((System.currentTimeMillis() - startedAt) / 1000).toInt(), level = plan.level, onExit = onExit,
        )
        return
    }

    val step = plan.steps[stepIndex]
    Column(Modifier.fillMaxSize().padding(top = 4.dp)) {
        StepHeader(step.title, stepIndex, plan.steps.size, onExit = { exitDialog = true })
        Box(Modifier.weight(1f)) {
            key(stepIndex) {
                when (step) {
                    is SessionStep.Warmup -> WarmupStep(step, vm, onDone = nextStep)
                    is SessionStep.Breathing -> BreathingStep(step.game, vm, hasMic, onDone = nextStep)
                    is SessionStep.Placement -> PlacementStep(step.sound, vm, onDone = nextStep)
                    is SessionStep.Isolated -> IsolatedStep(step, vm, hasMic, onRate = onRate, onDone = nextStep)
                    is SessionStep.Practice -> PracticeStep(step, vm, state.settings, hasMic, onRate = onRate, onDone = nextStep)
                    is SessionStep.Listening -> ListeningStep(step, vm, onAnswer = { ok -> listeningTotal++; if (ok) listeningCorrect++ }, onDone = nextStep)
                    is SessionStep.Conversation -> ConversationStep(step, vm, onRate = onRate, onDone = nextStep)
                }
            }
        }
    }

    if (exitDialog) {
        AlertDialog(
            onDismissRequest = { exitDialog = false },
            title = { Text("Завершити заняття?") },
            text = { Text(if (trials > 0) "Зроблено $trials спроб — ми їх збережемо." else "Прогрес цього заняття не збережеться.") },
            confirmButton = { TextButton(onClick = { exitDialog = false; if (trials > 0) finished = true else onExit() }) { Text("Завершити") } },
            dismissButton = { TextButton(onClick = { exitDialog = false }) { Text("Продовжити") } },
        )
    }
}

/* ----------------------------- Розминка ----------------------------- */

@Composable
private fun WarmupStep(step: SessionStep.Warmup, vm: AppViewModel, onDone: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val ex = step.exercises.getOrNull(index)
    if (ex == null) { LaunchedEffect(Unit) { onDone() }; return }
    var running by remember(index) { mutableStateOf(false) }
    var counter by remember(index) { mutableIntStateOf(0) }
    var setsDone by remember(index) { mutableIntStateOf(0) }
    val target = if (ex.mode == ExerciseMode.HOLD) ex.holdSeconds else ex.repetitions
    val setsTarget = if (ex.mode == ExerciseMode.HOLD) 3 else 1

    LaunchedEffect(index) { vm.tts.speak("${ex.name}. ${ex.childCue}") }
    LaunchedEffect(running, index) {
        if (running) {
            counter = 0
            val tick = if (ex.mode == ExerciseMode.HOLD) 1000L else 900L
            while (counter < target) {
                delay(tick)
                counter++
            }
            running = false
            setsDone++
            vm.tts.speak(praise.random())
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ParentTip(ex.parentInstruction)
        Spacer(Modifier.height(8.dp))
        Text("${ex.emoji} ${ex.name}", style = MaterialTheme.typography.headlineMedium)
        Text("Вправа ${index + 1} з ${step.exercises.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        ExerciseDemo(ex, running = running || setsDone == 0, modifier = Modifier.fillMaxWidth().height(230.dp))
        Text(ex.childCue, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { if (target == 0) 1f else counter / target.toFloat() },
                modifier = Modifier.size(120.dp), strokeWidth = 10.dp, color = Sunny, trackColor = Color(0xFFEFE6D6),
            )
            Text(if (running) "$counter" else if (setsDone >= setsTarget) "✅" else "$target", style = MaterialTheme.typography.displayMedium)
        }
        Text(if (ex.mode == ExerciseMode.HOLD) "Тримаємо, рахуємо до $target · підхід ${minOf(setsDone + 1, setsTarget)} з $setsTarget" else "Повторюємо $target разів у ритмі", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton(if (running) "Тримай…" else if (setsDone == 0) "Старт" else "Ще раз", onClick = { if (!running) running = true }, modifier = Modifier.weight(1f), emoji = "⏱️", color = Sky, contentColor = Ink, enabled = !running)
            BigButton(if (index + 1 < step.exercises.size) "Далі" else "Готово", onClick = { if (index + 1 < step.exercises.size) index++ else onDone() }, modifier = Modifier.weight(1f), emoji = "➡️", enabled = !running)
        }
        Spacer(Modifier.height(20.dp))
    }
}

/* ----------------------------- Дихальна гра ----------------------------- */

@Composable
private fun BreathingStep(game: BreathingGame, vm: AppViewModel, hasMic: Boolean, onDone: () -> Unit) {
    val level by vm.recorder.amplitude.collectAsStateWithLifecycle()
    val recording by vm.recorder.isRecording.collectAsStateWithLifecycle()
    var blow by remember { mutableFloatStateOf(0f) }
    var rounds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { vm.tts.speak(game.instruction); if (hasMic) vm.recorder.start() }
    DisposableEffect(Unit) { onDispose { vm.recorder.stop() } }
    LaunchedEffect(recording) {
        while (recording) {
            delay(100)
            blow = if (level > 0.15f) minOf(1f, blow + 0.1f / game.targetSeconds) else maxOf(0f, blow - 0.02f)
            if (blow >= 1f) { rounds++; blow = 0f; vm.tts.speak(praise.random()); delay(1500) }
        }
    }
    val scale by animateFloatAsState(0.5f + blow, label = "balloon")
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ParentTip("Вдих носом, видих плавний і довгий, щоки не надуваємо. Не більше 3–4 разів поспіль, щоб не запаморочилося. Це гра-розминка, а не терапія.")
        Spacer(Modifier.height(8.dp))
        Text("${game.emoji} ${game.title}", style = MaterialTheme.typography.headlineMedium)
        Text(game.instruction, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(game.emoji, fontSize = 120.sp, modifier = Modifier.scale(scale))
        }
        if (!hasMic) Text("Без доступу до мікрофона гра працює «на око»: дорослий рахує видихи.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        MicMeter(level, Modifier.fillMaxWidth(), active = recording)
        Text("Виконано: $rounds з 3", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        BigButton(if (rounds >= 3) "Готово" else "Далі", onClick = { vm.recorder.stop(); onDone() }, modifier = Modifier.fillMaxWidth(), emoji = "➡️")
    }
}

/* ----------------------------- Як сказати звук ----------------------------- */

@Composable
private fun PlacementStep(sound: SpeechSound, vm: AppViewModel, onDone: () -> Unit) {
    LaunchedEffect(Unit) { vm.tts.speak(sound.articulation.childCue) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ParentTip("Покажіть положення язика на собі, нехай дитина повторить перед дзеркалом. Губи: ${sound.articulation.lips} Язик: ${sound.articulation.tongue}")
        MouthAnimation(sound.articulation.pose, Modifier.fillMaxWidth().height(240.dp))
        Text(sound.articulation.childCue, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("${sound.emoji} ${sound.onomatopoeia}", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("Послухати", onClick = { vm.tts.speak(sound.onomatopoeia) }, modifier = Modifier.weight(1f), emoji = "🔊", color = Sunny, contentColor = Ink)
            BigButton("Зрозуміло", onClick = onDone, modifier = Modifier.weight(1f), emoji = "👍")
        }
        Spacer(Modifier.height(20.dp))
    }
}

/* ----------------------------- Ізольований звук ----------------------------- */

@Composable
private fun IsolatedStep(step: SessionStep.Isolated, vm: AppViewModel, hasMic: Boolean, onRate: (Rating) -> Unit, onDone: () -> Unit) {
    val game = step.sound.isolated
    val level by vm.recorder.amplitude.collectAsStateWithLifecycle()
    val voiced by vm.recorder.voicedSeconds.collectAsStateWithLifecycle()
    val recording by vm.recorder.isRecording.collectAsStateWithLifecycle()
    var done by remember { mutableIntStateOf(0) }
    var reached by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { vm.tts.speak(game.instruction); if (hasMic) vm.recorder.start() }
    DisposableEffect(Unit) { onDispose { vm.recorder.stop() } }
    val autoMode = hasMic && game.type != IsolatedGameType.PULSED
    LaunchedEffect(voiced) { if (autoMode && !reached && voiced >= game.targetSeconds) reached = true }

    val progress = if (autoMode) (voiced / game.targetSeconds).coerceIn(0f, 1f) else 0f
    val scale by animateFloatAsState(0.7f + progress * 0.6f, label = "grow")

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ParentTip(if (autoMode) "Мікрофон вимірює лише тривалість звука. Якість (чи правильний звук) оцінюєте ви. Якщо звук спотворений — не тренуємо, а показуємо логопеду." else "Дитина вимовляє звук коротко кілька разів. Оцініть спробу кнопками нижче.")
        Spacer(Modifier.height(8.dp))
        Text("${game.emoji} ${game.title}", style = MaterialTheme.typography.headlineMedium)
        Text(game.instruction, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(game.emoji, fontSize = 110.sp, modifier = Modifier.scale(scale))
                Text(step.sound.letter.lowercase().repeat(3).chunked(1).joinToString("-"), style = MaterialTheme.typography.displayLarge, color = Grape)
                AnimatedVisibility(feedback != null, enter = fadeIn() + scaleIn()) {
                    Text(feedback ?: "", style = MaterialTheme.typography.headlineSmall, color = Mint)
                }
            }
        }
        if (autoMode) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(14.dp).clip(CircleShape), color = Mint, trackColor = Color(0xFFEFE6D6))
            Spacer(Modifier.height(6.dp))
        }
        MicMeter(level, Modifier.fillMaxWidth(), active = recording)
        Text("Спроб: $done з ${step.trials}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (autoMode && !reached) {
            Text("Тягни звук, поки смужка не заповниться…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
        }
        RatingRow(enabled = !autoMode || reached, onRate = { r ->
            onRate(r); done++; reached = false
            feedback = if (r == Rating.CORRECT) praise.random() else encourage.random()
            vm.tts.speak(feedback!!)
            scope.launch { delay(1200); feedback = null }
            if (done >= step.trials) { vm.recorder.stop(); onDone() }
        })
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { vm.recorder.stop(); onDone() }, modifier = Modifier.align(Alignment.End)) { Text("Пропустити") }
    }
}

/* ----------------------------- Основна практика ----------------------------- */

private enum class TrialPhase { IDLE, RECORDING, PLAYBACK, CHECKING, RATE }

@Composable
private fun PracticeStep(step: SessionStep.Practice, vm: AppViewModel, settings: Settings, hasMic: Boolean, onRate: (Rating) -> Unit, onDone: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val item: PracticeItem? = step.items.getOrNull(index)
    if (item == null) { LaunchedEffect(Unit) { onDone() }; return }
    var phase by remember(index) { mutableStateOf(TrialPhase.IDLE) }
    var asrResult by remember(index) { mutableStateOf<SpeechChecker.Result?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var correctStreak by remember { mutableIntStateOf(0) }
    val level by vm.recorder.amplitude.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val isWordLevel = step.level == Level.WORDS || step.level == Level.SYLLABLES

    // Зразок озвучуємо автоматично лише для коротких одиниць; речення — за кнопкою, щоб не збивати темп.
    LaunchedEffect(index) { if (settings.ttsEnabled && item.text.length <= 40) vm.tts.speak(item.text, slow = isWordLevel) }
    DisposableEffect(Unit) { onDispose { vm.recorder.stop(); vm.recorder.stopPlayback() } }

    fun record() {
        if (!hasMic) return
        phase = TrialPhase.RECORDING
        vm.recorder.start()
        scope.launch {
            delay(settings.autoRecordSeconds * 1000L + if (item.text.length > 20) 2500L else 0L)
            vm.recorder.stopAndAwait()
            phase = TrialPhase.PLAYBACK
            vm.recorder.play { phase = TrialPhase.RATE }
        }
    }

    fun check() {
        if (!vm.speechChecker.isAvailable) return
        phase = TrialPhase.CHECKING
        scope.launch {
            val result = vm.speechChecker.listenFor(item.text)
            asrResult = result
            phase = TrialPhase.RATE
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ParentTip(parentTipFor(step.level))
        Spacer(Modifier.height(6.dp))
        Text("${index + 1} / ${step.items.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        EmojiCard(item.emoji, item.text, Modifier.fillMaxWidth(), hint = item.hint, onClick = { vm.tts.speak(item.text, slow = isWordLevel) })
        Spacer(Modifier.height(10.dp))

        when (phase) {
            TrialPhase.RECORDING -> {
                Text("🎤 Говори…", style = MaterialTheme.typography.titleLarge, color = Coral)
                MicMeter(level, Modifier.fillMaxWidth())
            }
            TrialPhase.PLAYBACK -> Text("👂 Послухай себе…", style = MaterialTheme.typography.titleLarge, color = Sky)
            TrialPhase.CHECKING -> Text("🤖 Слухаю уважно…", style = MaterialTheme.typography.titleLarge, color = Grape)
            else -> {
                asrResult?.let { r ->
                    val txt = when {
                        r.looksRight -> "🤖 Здається, вийшло чітко!"
                        r.close -> "🤖 Схоже на правильне слово. Я почув: «${r.heard.firstOrNull() ?: "…"}»"
                        r.heard.isEmpty() -> "🤖 Я не розчув — спробуй ближче до мікрофона."
                        else -> "🤖 Я почув: «${r.heard.first()}». Дорослий вирішує, чи було правильно."
                    }
                    Text(txt, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
                AnimatedVisibility(feedback != null, enter = fadeIn() + scaleIn()) {
                    Text(feedback ?: "", style = MaterialTheme.typography.headlineSmall, color = Mint)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("Зразок", onClick = { vm.tts.speak(item.text, slow = isWordLevel) }, modifier = Modifier.weight(1f), emoji = "🔊", color = Sunny, contentColor = Ink, enabled = phase == TrialPhase.IDLE || phase == TrialPhase.RATE)
            BigButton("Запиши себе", onClick = { record() }, modifier = Modifier.weight(1.3f), emoji = "🎤", color = Coral, enabled = hasMic && (phase == TrialPhase.IDLE || phase == TrialPhase.RATE))
        }
        if (settings.speechRecognitionEnabled && vm.speechChecker.isAvailable && step.level >= Level.WORDS) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { check() }, enabled = phase == TrialPhase.IDLE || phase == TrialPhase.RATE, modifier = Modifier.fillMaxWidth()) { Text("🤖 Перевір мене (експериментально)") }
        }
        Spacer(Modifier.height(12.dp))
        Text("Як вийшло? Оцінює дорослий:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        RatingRow(enabled = phase == TrialPhase.IDLE || phase == TrialPhase.RATE, onRate = { r ->
            onRate(r)
            correctStreak = if (r == Rating.CORRECT) correctStreak + 1 else 0
            // Зворотний зв'язок не на кожну спробу (принцип зниженої частоти KR): хвалимо кожну 3-тю правильну.
            if (r == Rating.CORRECT && correctStreak % 3 == 0) { feedback = praise.random(); vm.tts.speak(feedback!!) }
            else if (r == Rating.WRONG) { feedback = encourage.random() }
            else feedback = null
            scope.launch { delay(900); feedback = null }
            if (index + 1 < step.items.size) index++ else onDone()
        })
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDone, modifier = Modifier.align(Alignment.End)) { Text("Пропустити крок") }
        Spacer(Modifier.height(16.dp))
    }
}

private fun parentTipFor(level: Level): String = when (level) {
    Level.SYLLABLES -> "Склади вимовляємо чітко, трохи протягуючи звук. Блоками: спочатку всі прямі, потім обернені. Хваліть за чіткість, не поспішайте."
    Level.WORDS -> "Дитина дивиться на картинку, слухає зразок і повторює. Слово — 4–5 разів за заняття. Якщо звук «губиться» — поверніться до складу."
    Level.PHRASES -> "Фрази говоримо повільно, виділяючи звук. Можна плескати на кожне слово."
    Level.SENTENCES -> "Речення та чистомовки — у ритмі, можна з рухами. Стежимо, щоб звук не зникав у довгій фразі."
    Level.TEXTS -> "Скоромовку: 1) повільно, 2) частинами, 3) швидше з ритмом, 4) як змагання. Вірш — рядок за дорослим."
    Level.DIFFERENTIATION -> "Два схожі слова поруч: дитина має почути й сказати різницю. Нагадуйте: де язичок (унизу/вгорі), холодний чи теплий вітерець."
    Level.CONVERSATION -> "Вільне мовлення: ставте відкриті запитання й м'яко нагадуйте про звук («чую гарне Р!»). Оцінюйте загальне враження."
    Level.ISOLATED -> "Ізольований звук: якість важливіша за кількість."
}

/* ----------------------------- Слухова гра ----------------------------- */

@Composable
private fun ListeningStep(step: SessionStep.Listening, vm: AppViewModel, onAnswer: (Boolean) -> Unit, onDone: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val round: ListeningRound? = step.rounds.getOrNull(index)
    if (round == null) { LaunchedEffect(Unit) { onDone() }; return }
    var chosen by remember(index) { mutableStateOf<WordItem?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(index) { delay(300); vm.tts.speak(round.target.text, slow = true) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ParentTip("Фонематичний слух: дитина спочатку має ПОЧУТИ різницю між звуками, тоді й вимова закріплюється швидше. Якщо озвучення не працює — прочитайте слово самі.")
        Spacer(Modifier.height(8.dp))
        Text("👂 Де «${round.target.text}»?", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text("Раунд ${index + 1} з ${step.rounds.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            round.options.forEach { opt ->
                val isTarget = opt == round.target
                val bg = when {
                    chosen == null -> Color.White
                    isTarget -> Color(0xFFD3F3E1)
                    chosen == opt -> Color(0xFFFFEDC2)
                    else -> Color.White
                }
                Card(
                    modifier = Modifier.weight(1f).height(200.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = bg),
                    onClick = {
                        if (chosen != null) return@Card
                        chosen = opt
                        val ok = isTarget
                        onAnswer(ok)
                        vm.tts.speak(if (ok) praise.random() else "Це «${opt.text}». А «${round.target.text}» — ось тут.")
                        scope.launch { delay(1600); if (index + 1 < step.rounds.size) index++ else onDone() }
                    },
                ) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(opt.emoji, fontSize = 72.sp)
                        Text(opt.text, style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        BigButton("Повторити слово", onClick = { vm.tts.speak(round.target.text, slow = true) }, modifier = Modifier.fillMaxWidth(), emoji = "🔊", color = Sunny, contentColor = Ink)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDone, modifier = Modifier.align(Alignment.End)) { Text("Пропустити крок") }
    }
}

/* ----------------------------- Вільна розмова ----------------------------- */

@Composable
private fun ConversationStep(step: SessionStep.Conversation, vm: AppViewModel, onRate: (Rating) -> Unit, onDone: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val prompt = step.prompts.getOrNull(index)
    if (prompt == null) { LaunchedEffect(Unit) { onDone() }; return }
    LaunchedEffect(index) { vm.tts.speak(prompt) }
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ParentTip("Мета — перенести звук у вільне мовлення. Слухайте 1–2 хвилини, потім оцініть загальне враження: чи звук ${step.sound.letter} звучав чисто.")
        Spacer(Modifier.height(12.dp))
        EmojiCard("💬", prompt, Modifier.fillMaxWidth())
        Spacer(Modifier.weight(1f))
        Text("Як звучав звук ${step.sound.letter} у розмові?", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        RatingRow(onRate = { r -> onRate(r); if (index + 1 < step.prompts.size) index++ else onDone() })
    }
}

/* ----------------------------- Підсумок ----------------------------- */

@Composable
private fun FinishScreen(
    vm: AppViewModel, sound: SpeechSound, trials: Int, correct: Int, almost: Int, listeningCorrect: Int, listeningTotal: Int,
    durationSec: Int, level: Level, onExit: () -> Unit,
) {
    var sticker by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        vm.recordSession(sound.id, level, trials, correct, almost, durationSec) { s -> sticker = s; saved = true }
        vm.tts.speak("Ура! Заняття завершено. Ти молодець!")
    }
    val accuracy = if (trials == 0) 0 else ((correct + almost / 2f) / trials * 100).toInt()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        Text("🎉", fontSize = 72.sp)
        Text("Молодець!", style = MaterialTheme.typography.displayMedium)
        Text("Заняття зі звуком ${sound.letter} завершено", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatPill("🎯", "$trials", "спроб", Modifier.weight(1f))
            StatPill("✅", "$accuracy %", "чітко", Modifier.weight(1f))
            StatPill("⏱️", "${maxOf(1, durationSec / 60)} хв", "тривалість", Modifier.weight(1f))
        }
        if (listeningTotal > 0) {
            Spacer(Modifier.height(10.dp))
            Text("👂 Вухо-детектив: $listeningCorrect з $listeningTotal", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(24.dp))
        AnimatedVisibility(saved, enter = fadeIn(tween(600)) + scaleIn(tween(600))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (sticker != null) {
                    Text("Нова наліпка!", style = MaterialTheme.typography.titleLarge)
                    Box(Modifier.size(140.dp).clip(RoundedCornerShape(36.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                        Text(sticker!!, fontSize = 84.sp)
                    }
                } else {
                    Text("Зроби хоча б 5 спроб наступного разу — і отримаєш наліпку!", textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        ParentTip(
            when {
                trials == 0 -> "Нічого страшного: іноді достатньо просто подивитися й послухати. Спробуйте завтра у той самий час."
                accuracy >= 80 -> "Чудовий результат. Якщо так буде 2 заняття поспіль (≥20 спроб) — перейдемо на наступний рівень автоматично."
                accuracy >= 50 -> "Хороша робота. Звук ще закріплюється — це нормально, залишаємося на цьому рівні."
                else -> "Якщо звук виходить рідко кілька занять поспіль — можливо, його ще не «поставлено». Порадьтеся з логопедом."
            },
        )
        Spacer(Modifier.height(24.dp))
        BigButton("На головну", onClick = onExit, modifier = Modifier.fillMaxWidth(), emoji = "🏠", enabled = saved)
    }
}
