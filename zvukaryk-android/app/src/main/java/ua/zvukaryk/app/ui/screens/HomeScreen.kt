package ua.zvukaryk.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.zvukaryk.app.AppViewModel
import ua.zvukaryk.app.data.content.Sounds
import ua.zvukaryk.app.data.model.AppState
import ua.zvukaryk.app.domain.MasteryRules
import ua.zvukaryk.app.ui.components.*
import ua.zvukaryk.app.ui.theme.*
import java.time.LocalDate

/** Головний екран для дитини: одна велика кнопка «Грати», друзі-звуки, наліпки. */
@Composable
fun HomeScreen(
    state: AppState, vm: AppViewModel,
    onStartSession: (String) -> Unit, onOpenSound: (String) -> Unit, onOpenParent: () -> Unit,
    onOpenMirror: () -> Unit, onOpenStickers: () -> Unit, onOpenSounds: () -> Unit,
) {
    var gate by remember { mutableStateOf(false) }
    val today = LocalDate.now().toEpochDay()
    val practicedToday = state.lastPracticeEpochDay == today
    // Сьогоднішній звук: чергуємо цільові звуки за днями, щоб чергування було передбачуваним.
    val targets = state.validTargets(Sounds.ids).ifEmpty { listOf(Sounds.all.first().id) }
    val todaySound = Sounds.get(targets[(today % targets.size).toInt()])
    val progress = state.progressFor(todaySound.id)
    LaunchedEffect(Unit) {
        if (!vm.greeted) {
            vm.greeted = true
            vm.voice.say(vm.voice.greeting(state.profile.name, todaySound, practicedToday))
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.profile.avatar, fontSize = 40.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Привіт, ${state.profile.name.ifBlank { "друже" }}!", style = MaterialTheme.typography.headlineMedium)
                Text(if (practicedToday) "Сьогодні вже займалися — супер!" else "Час трохи пограти зі звуками", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { gate = true }) { Icon(Icons.Default.Settings, contentDescription = "Для батьків") }
        }
        Spacer(Modifier.height(20.dp))

        // Головна картка
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Brush.linearGradient(listOf(Grape, Color(0xFF9D4EDD))))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(todaySound.emoji, fontSize = 72.sp)
            Text(todaySound.title, style = MaterialTheme.typography.displayMedium, color = Color.White)
            Text("${progress.level.emoji} ${progress.level.title}", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            BigButton("Грати", onClick = { onStartSession(todaySound.id) }, color = Sunny, contentColor = Ink, emoji = "▶️", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = { onOpenSound(todaySound.id) }) { Text("Як сказати цей звук?", color = Color.White) }
                TextButton(onClick = { vm.voice.say(vm.voice.greeting(state.profile.name, todaySound, practicedToday)) }) { Text("🔊 Повтори", color = Color.White) }
            }
        }
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatPill("🔥", "${state.streakDays}", "днів поспіль", Modifier.weight(1f))
            StatPill("🎯", "${state.totalTrials}", "спроб", Modifier.weight(1f))
            Box(Modifier.weight(1f).clickable { onOpenStickers() }) {
                StatPill("🎁", "${state.stickers.size}", "наліпок", Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(20.dp))

        Text("Мої звуки", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(targets) { id ->
                val s = Sounds.get(id)
                val p = state.progressFor(id)
                Column(
                    Modifier
                        .width(120.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .clickable { onStartSession(id) }
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(s.emoji, fontSize = 36.sp)
                    Text(s.letter, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text(if (p.mastered) "Освоєно ⭐" else p.level.shortTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(
                        progress = { (p.level.ordinal + p.accuracyAtLevel.coerceIn(0f, 1f)) / 8f },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Mint, trackColor = Color(0xFFEFE6D6),
                    )
                }
            }
            item {
                Column(
                    Modifier.width(120.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFFF3EDE2)).clickable { onOpenSounds() }.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("📚", fontSize = 36.sp)
                    Text("Усі звуки", style = MaterialTheme.typography.titleMedium)
                    Text("і вікові норми", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("Дзеркало", onClick = onOpenMirror, modifier = Modifier.weight(1f), emoji = "🪞", color = Sky, contentColor = Ink)
            BigButton("Наліпки", onClick = onOpenStickers, modifier = Modifier.weight(1f), emoji = "🎁", color = SoftPink, contentColor = Ink)
        }
        Spacer(Modifier.height(16.dp))

        val stalled = targets.map { state.progressFor(it) }.filter { MasteryRules.isStalled(it) }
        if (stalled.isNotEmpty()) {
            SoftCard(color = Color(0xFFFFF1E6)) {
                Text("Порада для дорослих", style = MaterialTheme.typography.titleMedium)
                Text("Звук ${stalled.joinToString { Sounds.get(it.soundId).letter }}: кілька занять поспіль без прогресу. Варто показати дитину логопеду — можливо, звук ще не «поставлено».")
            }
            Spacer(Modifier.height(16.dp))
        }
        ParentTip("Займайтеся разом: ви — головний суддя й головний уболівальник. 7–10 хвилин щодня краще, ніж година раз на тиждень.")
        Spacer(Modifier.height(32.dp))
    }

    if (gate) ParentGateDialog(onDismiss = { gate = false }, onPassed = { gate = false; onOpenParent() })
}
