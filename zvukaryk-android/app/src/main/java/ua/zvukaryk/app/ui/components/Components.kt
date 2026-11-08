package ua.zvukaryk.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.zvukaryk.app.data.model.Rating
import ua.zvukaryk.app.ui.theme.*
import kotlin.random.Random

/** Велика кнопка для дитячих пальчиків (висота ≥ 64dp, один жест — одна дія). */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 68.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
    ) {
        if (emoji != null) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun SoftCard(modifier: Modifier = Modifier, color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

/** Шапка кроку заняття з прогресом і кнопкою виходу. */
@Composable
fun StepHeader(title: String, stepIndex: Int, stepCount: Int, onExit: () -> Unit, parentTip: String? = null) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) { Icon(Icons.Default.Close, contentDescription = "Завершити") }
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text("${stepIndex + 1}/$stepCount", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { (stepIndex + 1f) / stepCount },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(8.dp).clip(CircleShape),
            color = Sunny, trackColor = Color(0xFFEFE6D6),
        )
        if (parentTip != null) {
            ParentTip(parentTip, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
    }
}

/** Підказка для дорослого: що сказати, на що звернути увагу. */
@Composable
fun ParentTip(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEFF7FF))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text("👩‍👧", fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2F4858))
    }
}

/** Індикатор гучності мікрофона (біологічний зворотний зв'язок «бачу свій голос»). */
@Composable
fun MicMeter(level: Float, modifier: Modifier = Modifier, active: Boolean = true) {
    Row(modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val bars = 16
        repeat(bars) { i ->
            val threshold = (i + 1f) / bars
            val on = active && level >= threshold * 0.9f
            Box(
                Modifier
                    .weight(1f)
                    .height((10 + 18 * threshold).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (on) lerpColor(Mint, Coral, threshold) else Color(0xFFE8E2D6)),
            )
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, 1f,
)

/** Оцінювання спроби дорослим: лише позитивна/нейтральна подача, без червоних хрестиків. */
@Composable
fun RatingRow(onRate: (Rating) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RateButton(Rating.WRONG, Color(0xFFE9E4F5), onRate, enabled, Modifier.weight(1f))
        RateButton(Rating.ALMOST, Color(0xFFFFEDC2), onRate, enabled, Modifier.weight(1f))
        RateButton(Rating.CORRECT, Color(0xFFD3F3E1), onRate, enabled, Modifier.weight(1.2f))
    }
}

@Composable
private fun RateButton(rating: Rating, color: Color, onRate: (Rating) -> Unit, enabled: Boolean, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (enabled) color else color.copy(alpha = 0.4f))
            .clickable(enabled = enabled) { onRate(rating) }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(rating.emoji, fontSize = 30.sp)
        Text(rating.label, style = MaterialTheme.typography.labelLarge, color = Ink)
    }
}

/** Картка з емодзі-картинкою і словом. Дитині не потрібно читати — картинка і озвучення. */
@Composable
fun EmojiCard(emoji: String, text: String, modifier: Modifier = Modifier, hint: String? = null, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp).animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(emoji, fontSize = if (text.length > 20) 56.sp else 92.sp, lineHeight = 100.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text,
                style = if (text.length > 20) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                color = Ink,
            )
            if (hint != null) {
                Spacer(Modifier.height(8.dp))
                Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun SelectableChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, emoji: String? = null) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.White
    val fg = if (selected) Color.White else Ink
    Row(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color(0xFFE2DACB), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (emoji != null) { Text(emoji, fontSize = 22.sp); Spacer(Modifier.width(8.dp)) }
        Text(text, color = fg, style = MaterialTheme.typography.titleMedium)
    }
}

/** Батьківський замок: простий арифметичний приклад, який дошкільник не розв'яже. */
@Composable
fun ParentGateDialog(onDismiss: () -> Unit, onPassed: () -> Unit) {
    val (a, b) = remember { Random.nextInt(11, 40) to Random.nextInt(11, 40) }
    var answer by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Для дорослих") },
        text = {
            Column {
                Text("Щоб відкрити батьківський розділ, розв'яжіть приклад:")
                Spacer(Modifier.height(12.dp))
                Text("$a + $b = ?", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = answer, onValueChange = { answer = it.filter { c -> c.isDigit() }; wrong = false },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, isError = wrong, label = { Text("Відповідь") },
                )
                if (wrong) Text("Спробуйте ще раз", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (answer.toIntOrNull() == a + b) onPassed() else wrong = true }) { Text("Відкрити") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )
}

@Composable
fun BackTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun StatPill(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
