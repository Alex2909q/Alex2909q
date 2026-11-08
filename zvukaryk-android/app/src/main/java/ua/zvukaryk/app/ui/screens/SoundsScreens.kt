package ua.zvukaryk.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.zvukaryk.app.AppViewModel
import ua.zvukaryk.app.ai.ClaudeAgent
import ua.zvukaryk.app.data.content.ArticulationExercises
import ua.zvukaryk.app.data.content.Sounds
import ua.zvukaryk.app.data.model.AppState
import ua.zvukaryk.app.data.model.Level
import ua.zvukaryk.app.ui.components.*
import ua.zvukaryk.app.ui.theme.Mint
import ua.zvukaryk.app.ui.theme.Sunny

/** Усі звуки з віковими нормами (довідник для батьків). */
@Composable
fun SoundsScreen(state: AppState, onBack: () -> Unit, onOpenSound: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 4.dp)) {
        BackTopBar("Усі звуки", onBack)
        Text(
            "Орієнтовні вікові норми появи звуків в українській мові. Порядок корекції: свистячі → шиплячі → Л → Р. Якщо звук не з'явився після верхньої межі — консультація логопеда.",
            Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Sounds.all) { s ->
                val p = state.progressFor(s.id)
                val isTarget = s.id in state.targetSounds
                SoftCard(color = if (isTarget) Color(0xFFEBD9FF) else Color.White) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(s.emoji, fontSize = 40.sp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.title, style = MaterialTheme.typography.titleLarge)
                            Text("${s.group.title} · норма ${s.ageNormFrom}–${s.ageNormTo} р.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (isTarget) Text("${p.level.emoji} ${p.level.title}", style = MaterialTheme.typography.bodyMedium)
                        }
                        TextButton(onClick = { onOpenSound(s.id) }) { Text("Відкрити") }
                    }
                }
            }
        }
    }
}

/** Картка звука: як вимовляти, типові помилки, матеріал, керування рівнем. */
@Composable
fun SoundDetailScreen(state: AppState, vm: AppViewModel, soundId: String, onBack: () -> Unit, onStart: () -> Unit) {
    val sound = Sounds.get(soundId)
    val p = state.progressFor(soundId)
    val isTarget = soundId in state.targetSounds
    var gate by remember { mutableStateOf(false) }
    var levelMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 32.dp)) {
        BackTopBar(sound.title, onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {
            MouthAnimation(sound.articulation.pose, Modifier.fillMaxWidth().height(220.dp))
            Spacer(Modifier.height(8.dp))
            Text("${sound.emoji} ${sound.onomatopoeia}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton("Послухати", onClick = { vm.voice.say(sound.onomatopoeia) }, modifier = Modifier.weight(1f), emoji = "🔊", color = Sunny, contentColor = Color(0xFF2B2D42))
                BigButton("Заняття", onClick = onStart, modifier = Modifier.weight(1f), emoji = "▶️")
            }
            Spacer(Modifier.height(16.dp))

            SoftCard {
                Text("Підказка для дитини", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(sound.articulation.childCue, style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = { vm.voice.say(sound.articulation.childCue) }) { Text("🔊 Озвучити підказку") }
            }
            if (vm.claude.isConfigured) {
                Spacer(Modifier.height(12.dp))
                SoftCard(color = Color(0xFFF3ECFF)) {
                    Text("✨ Звукарик-помічник", style = MaterialTheme.typography.titleLarge)
                    Text("Персональні пояснення, казки й загадки зі словами на звук ${sound.letter}. Озвучується голосом.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    ClaudeHintButton("Поясни звук інакше", onRequest = { vm.claude.explainSoundDifferently(sound.id) })
                    Spacer(Modifier.height(6.dp))
                    ClaudeHintButton("Розкажи казку", onRequest = { speakStory(vm, sound.id) })
                    Spacer(Modifier.height(6.dp))
                    ClaudeHintButton("Загадай загадки", onRequest = { vm.claude.riddlesForChild(sound.id).also { r -> if (r is ClaudeAgent.Result.Text) vm.voice.say(r.text) } })
                }
            }
            Spacer(Modifier.height(12.dp))
            SoftCard {
                Text("Правильна артикуляція (для дорослих)", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text("👄 Губи: ${sound.articulation.lips}")
                Text("👅 Язик: ${sound.articulation.tongue}")
                Text("💨 Повітря: ${sound.articulation.air}")
                Text("🎵 Голос: ${sound.articulation.voice}")
            }
            Spacer(Modifier.height(12.dp))
            SoftCard(color = Color(0xFFFFF1E6)) {
                Text("Типові помилки", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(sound.commonMistakes)
            }
            Spacer(Modifier.height(12.dp))
            SoftCard {
                Text("Розминка для цього звука", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                ArticulationExercises.forIds(sound.warmupExerciseIds).forEach { Text("${it.emoji} ${it.name} — ${it.parentInstruction}", Modifier.padding(vertical = 3.dp)) }
                Text("Розминка — коротка й ігрова; головний тренувальний час — на самих звуках і словах.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
            Spacer(Modifier.height(12.dp))
            SoftCard {
                Text("Мовний матеріал", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text("Склади: ${(sound.syllables.direct + sound.syllables.reverse).joinToString(", ")}")
                Text("Слів з картинками: ${sound.words.all().distinctBy { it.text }.size}  ·  речень: ${sound.sentences.size}  ·  чистомовок: ${sound.chystomovky.size}")
                Text("Скоромовок: ${sound.tongueTwisters.size}  ·  віршів: ${sound.poems.size}  ·  пар для розрізнення: ${sound.differentiation.size}")
                Spacer(Modifier.height(6.dp))
                sound.chystomovky.take(3).forEach { Text("• $it") }
            }
            Spacer(Modifier.height(12.dp))
            SoftCard(color = Color(0xFFE3F7EA)) {
                Text("Прогрес", style = MaterialTheme.typography.titleLarge, color = Mint)
                Spacer(Modifier.height(6.dp))
                if (isTarget) {
                    Text("${p.level.emoji} Рівень: ${p.level.title}${if (p.mastered) " — освоєно ⭐" else ""}")
                    Text("На цьому рівні: ${p.trialsAtLevel} спроб, точність ${(p.accuracyAtLevel * 100).toInt()} %, занять ${p.sessionsAtLevel}")
                    Text("Усього спроб зі звуком: ${p.totalTrials}")
                    TextButton(onClick = { gate = true }) { Text("Змінити рівень (для дорослих)") }
                } else {
                    Text("Цей звук ще не у списку для тренування.")
                    TextButton(onClick = { gate = true }) { Text("Додати до моїх звуків (для дорослих)") }
                }
            }
        }
    }

    if (gate) ParentGateDialog(onDismiss = { gate = false }, onPassed = {
        gate = false
        if (isTarget) levelMenu = true else vm.setTargets(state.targetSounds + soundId)
    })
    if (levelMenu) {
        AlertDialog(
            onDismissRequest = { levelMenu = false },
            title = { Text("Рівень для ${sound.title}") },
            text = {
                Column {
                    Level.entries.forEach { lvl ->
                        TextButton(onClick = { vm.setLevel(soundId, lvl); levelMenu = false }) { Text("${lvl.emoji} ${lvl.title}${if (lvl == p.level) "  ✓" else ""}") }
                    }
                    TextButton(onClick = { vm.setTargets(state.targetSounds - soundId); levelMenu = false }) { Text("Прибрати зі списку", color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(onClick = { levelMenu = false }) { Text("Закрити") } },
        )
    }
}

/** Альбом наліпок. */
@Composable
fun StickersScreen(state: AppState, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 4.dp)) {
        BackTopBar("Мої наліпки", onBack)
        if (state.stickers.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("🎁", fontSize = 80.sp)
                Text("Наліпки з'являться після першого заняття!", style = MaterialTheme.typography.titleLarge)
            }
        } else {
            Text("Зібрано ${state.stickers.size} · серія ${state.streakDays} дн.", Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleMedium)
            val rows = state.stickers.chunked(4)
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(rows) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { s ->
                            SoftCard(Modifier.weight(1f)) { Text(s, fontSize = 44.sp, modifier = Modifier.align(Alignment.CenterHorizontally)) }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}


/** Казка: озвучити цілком (на екрані звука); у занятті вона йде реченнями. */
private suspend fun speakStory(vm: AppViewModel, soundId: String): ClaudeAgent.Result {
    val r = vm.claude.storyForChild(soundId)
    if (r is ClaudeAgent.Result.Text) vm.voice.say(r.text, slow = true)
    return r
}
