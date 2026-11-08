package ua.zvukaryk.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.zvukaryk.app.AppViewModel
import ua.zvukaryk.app.data.content.Sounds
import ua.zvukaryk.app.data.model.ChildProfile
import ua.zvukaryk.app.data.model.ScreeningAnswers
import ua.zvukaryk.app.ui.components.*
import ua.zvukaryk.app.ui.theme.Coral
import ua.zvukaryk.app.ui.theme.Mint

private val avatars = listOf("🦊", "🐻", "🐰", "🐼", "🦁", "🐸", "🦄", "🐬")

/**
 * Онбординг для дорослого: профіль дитини → короткий скринінг (червоні прапорці) →
 * перевірка звуків за картинками → вибір цільових звуків з урахуванням вікової норми.
 */
@Composable
fun OnboardingScreen(vm: AppViewModel, onFinished: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var profile by remember { mutableStateOf(ChildProfile()) }
    var screening by remember { mutableStateOf(ScreeningAnswers()) }
    val problemSounds = remember { mutableStateListOf<String>() }
    val targets = remember { mutableStateListOf<String>() }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        LinearProgressIndicator(progress = { (step + 1) / 5f }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Spacer(Modifier.height(16.dp))
        when (step) {
            0 -> WelcomeStep(vm, onNext = { step = 1 })
            1 -> ProfileStep(profile, onChange = { profile = it }, onNext = { step = 2 })
            2 -> ScreeningStep(screening, onChange = { screening = it }, onNext = { step = 3 })
            3 -> SoundCheckStep(profile.ageYears, problemSounds, onNext = {
                targets.clear()
                val recommended = Sounds.recommendedForAge(profile.ageYears).map { it.id }
                targets += problemSounds.filter { it in recommended }.ifEmpty { recommended.take(1) }
                step = 4
            })
            else -> TargetsStep(profile.ageYears, problemSounds, targets, onFinish = {
                vm.completeOnboarding(profile, targets.toList(), screening.copy(problemSounds = problemSounds.toList(), completed = true))
                onFinished()
            })
        }
    }
}

@Composable
private fun WelcomeStep(vm: AppViewModel, onNext: () -> Unit) {
    LaunchedEffect(Unit) { vm.voice.say("Привіт! Я Звукарик. Я допоможу вчитися гарно вимовляти звуки.") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Text("🗣️", fontSize = 80.sp)
        Text("Звукарик", style = MaterialTheme.typography.displayMedium)
        Text("Щоденний тренажер вимови для дітей 3–8 років", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        SoftCard {
            Text("Як це працює", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("• 7–10 хвилин щодня — так радять логопеди: «потроху, але часто».\n" +
                "• Класичний шлях: звук → склади → слова → речення → вірші → мовлення.\n" +
                "• Дитина бачить картинку, чує зразок, записує себе і слухає.\n" +
                "• Дорослий поруч оцінює спробу — це найважливіше.\n" +
                "• Наліпки за кожне заняття, серія днів, звіт для логопеда.")
        }
        Spacer(Modifier.height(16.dp))
        SoftCard(color = androidx.compose.ui.graphics.Color(0xFFFFF1E6)) {
            Text("Важливо", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("Застосунок допомагає закріплювати звуки вдома й не замінює логопеда. Постановку звука, " +
                "виправлення міжзубної чи горлової вимови робить фахівець. Усі записи голосу залишаються на пристрої.")
        }
        Spacer(Modifier.height(16.dp))
        SoundCheckCard(vm.tts, Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        BigButton("Почати", onClick = onNext, modifier = Modifier.fillMaxWidth(), emoji = "🚀")
    }
}

@Composable
private fun ProfileStep(profile: ChildProfile, onChange: (ChildProfile) -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Про дитину", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = profile.name, onValueChange = { onChange(profile.copy(name = it)) },
            label = { Text("Ім'я дитини") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        Text("Скільки років?", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (3..8).forEach { age ->
                SelectableChip(text = "$age", selected = profile.ageYears == age, onClick = { onChange(profile.copy(ageYears = age)) }, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Обери друга", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            avatars.take(4).forEach { a -> SelectableChip(a, profile.avatar == a, { onChange(profile.copy(avatar = a)) }, Modifier.weight(1f)) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            avatars.drop(4).forEach { a -> SelectableChip(a, profile.avatar == a, { onChange(profile.copy(avatar = a)) }, Modifier.weight(1f)) }
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(24.dp))
        BigButton("Далі", onClick = onNext, modifier = Modifier.fillMaxWidth(), enabled = profile.name.isNotBlank())
    }
}

@Composable
private fun ScreeningStep(answers: ScreeningAnswers, onChange: (ScreeningAnswers) -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Кілька запитань", style = MaterialTheme.typography.headlineLarge)
        Text("Це не діагностика — лише підказка, чи варто показати дитину логопеду.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text("Наскільки ви розумієте мовлення дитини?", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1 to "Мало", 2 to "Частково", 3 to "Майже все").forEach { (v, t) ->
                SelectableChip(t, answers.intelligibilityParents == v, { onChange(answers.copy(intelligibilityParents = v)) }, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Чи розуміють дитину незнайомі дорослі?", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1 to "Мало", 2 to "Частково", 3 to "Майже все").forEach { (v, t) ->
                SelectableChip(t, answers.intelligibilityStrangers == v, { onChange(answers.copy(intelligibilityStrangers = v)) }, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
        ToggleRow("Є сумніви щодо слуху", answers.hearingConcern) { onChange(answers.copy(hearingConcern = it)) }
        ToggleRow("Плутає або спотворює голосні (а/о/у)", answers.vowelErrors) { onChange(answers.copy(vowelErrors = it)) }
        ToggleRow("Одне й те саме слово щоразу звучить по-різному", answers.inconsistent) { onChange(answers.copy(inconsistent = it)) }
        ToggleRow("Дитина засмучується, що її не розуміють", answers.frustration) { onChange(answers.copy(frustration = it)) }
        ToggleRow("Є запинки, повтори складів (заїкання)", answers.stuttering) { onChange(answers.copy(stuttering = it)) }
        Spacer(Modifier.height(24.dp))
        BigButton("Далі", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun ToggleRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SoundCheckStep(age: Int, problemSounds: MutableList<String>, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("Перевірка звуків", style = MaterialTheme.typography.headlineLarge)
        Text("Попросіть дитину назвати картинки (не повторювати за вами). Позначте звуки, які звучать неправильно або замінюються.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Sounds.all.size) { i ->
                val sound = Sounds.all[i]
                val words = Sounds.screeningWords(sound)
                val selected = sound.id in problemSounds
                val inNorm = age < sound.ageNormTo
                SoftCard(color = if (selected) androidx.compose.ui.graphics.Color(0xFFFFE3E3) else androidx.compose.ui.graphics.Color.White) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${sound.emoji} ${sound.title}", style = MaterialTheme.typography.titleLarge)
                            Text(words.joinToString("   ") { "${it.emoji} ${it.text}" }, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (inNorm) "Норма появи: ${sound.ageNormFrom}–${sound.ageNormTo} р. — у ${age} р. ще може формуватися" else "Норма появи: ${sound.ageNormFrom}–${sound.ageNormTo} р.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Checkbox(checked = selected, onCheckedChange = { if (it) problemSounds += sound.id else problemSounds -= sound.id })
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        BigButton("Далі", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TargetsStep(age: Int, problemSounds: List<String>, targets: MutableList<String>, onFinish: () -> Unit) {
    val recommended = Sounds.recommendedForAge(age).map { it.id }
    val tooEarly = problemSounds.filter { it !in recommended }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Що тренуємо", style = MaterialTheme.typography.headlineLarge)
        Text("Логопеди радять працювати над 1–2 звуками одночасно в порядку: свистячі → шиплячі → Л → Р.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        if (tooEarly.isNotEmpty()) {
            SoftCard(color = androidx.compose.ui.graphics.Color(0xFFFFF1E6)) {
                Text("Зарано хвилюватися", style = MaterialTheme.typography.titleMedium)
                Text("Звуки ${tooEarly.joinToString { Sounds.get(it).letter }} у ${age} р. зазвичай ще формуються. Ми їх не пропонуємо, але ви можете додати їх у батьківському розділі, якщо так радить логопед.")
            }
            Spacer(Modifier.height(12.dp))
        }
        Sounds.all.forEach { sound ->
            val selected = sound.id in targets
            val available = sound.id in recommended
            SelectableChip(
                text = "${sound.title}  ·  ${sound.ageNormFrom}–${sound.ageNormTo} р." + if (sound.id in problemSounds) "  · позначено" else "",
                selected = selected, emoji = sound.emoji,
                onClick = { if (available) { if (selected) targets -= sound.id else if (targets.size < 3) targets += sound.id } },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        val flags = problemSounds.any { Sounds.get(it).ageNormTo <= age }
        if (flags) {
            SoftCard(color = androidx.compose.ui.graphics.Color(0xFFFFE3E3)) {
                Text("Порада", style = MaterialTheme.typography.titleMedium, color = Coral)
                Text("Якщо звук не з'явився після вікової норми або дитина його спотворює (язик між зубами, горлове Р) — покажіть дитину логопеду. Застосунок допоможе закріпити звук, який уже «поставлено».")
            }
        } else {
            SoftCard(color = androidx.compose.ui.graphics.Color(0xFFE3F7EA)) {
                Text("Чудово", style = MaterialTheme.typography.titleMedium, color = Mint)
                Text("Можна займатися профілактично: розминка для язичка, слухові ігри й чистомовки корисні всім.")
            }
        }
        Spacer(Modifier.height(24.dp))
        BigButton("Поїхали!", onClick = onFinish, modifier = Modifier.fillMaxWidth(), emoji = "🎉", enabled = targets.isNotEmpty())
    }
}
