package ua.zvukaryk.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay
import ua.zvukaryk.app.data.model.ArticulationExercise
import ua.zvukaryk.app.data.model.ExerciseMode
import ua.zvukaryk.app.data.model.MouthPose
import kotlin.math.sin

/** Анімований зразок артикуляції: обличчя з губами, зубами й язиком за параметрами [MouthPose]. */
@Composable
fun MouthAnimation(pose: MouthPose, modifier: Modifier = Modifier) {
    val spec = tween<Float>(durationMillis = 450, easing = FastOutSlowInEasing)
    val open by animateFloatAsState(pose.open, spec, label = "open")
    val round by animateFloatAsState(pose.round, spec, label = "round")
    val smile by animateFloatAsState(pose.smile, spec, label = "smile")
    val tongueY by animateFloatAsState(pose.tongueY, spec, label = "tongueY")
    val tongueOut by animateFloatAsState(pose.tongueOut, spec, label = "tongueOut")
    val tongueWidth by animateFloatAsState(pose.tongueWidth, spec, label = "tongueWidth")
    val cup by animateFloatAsState(pose.cup, spec, label = "cup")

    val vibrate = if (pose.vibrate) {
        val infinite = rememberInfiniteTransition(label = "vibe")
        val phase by infinite.animateFloat(
            0f, (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(120, easing = LinearEasing)), label = "phase",
        )
        sin(phase)
    } else 0f

    Canvas(modifier) {
        val s = size.minDimension
        val cx = size.width / 2
        val top = (size.height - s) / 2

        // Обличчя
        drawCircle(Color(0xFFFFD9B3), radius = s * 0.48f, center = Offset(cx, top + s * 0.5f))
        drawCircle(Color(0xFFE8B98C), radius = s * 0.48f, center = Offset(cx, top + s * 0.5f), style = Stroke(s * 0.012f))
        // Щічки
        drawCircle(Color(0x66FF8FA3), radius = s * 0.07f, center = Offset(cx - s * 0.3f, top + s * 0.6f))
        drawCircle(Color(0x66FF8FA3), radius = s * 0.07f, center = Offset(cx + s * 0.3f, top + s * 0.6f))
        // Очі
        for (dx in listOf(-0.16f, 0.16f)) {
            drawCircle(Color.White, radius = s * 0.075f, center = Offset(cx + s * dx, top + s * 0.4f))
            drawCircle(Color(0xFF2B2D42), radius = s * 0.04f, center = Offset(cx + s * dx, top + s * 0.41f))
            drawCircle(Color.White, radius = s * 0.013f, center = Offset(cx + s * dx + s * 0.015f, top + s * 0.395f))
        }
        // Ніс
        val nose = Path().apply {
            moveTo(cx - s * 0.03f, top + s * 0.5f); quadraticTo(cx, top + s * 0.58f, cx + s * 0.03f, top + s * 0.5f)
        }
        drawPath(nose, Color(0xFFCF9A6F), style = Stroke(s * 0.012f))

        // Рот
        val mouthCy = top + s * 0.72f
        val mouthW = s * (0.30f + smile * 0.12f - round * 0.16f)
        val mouthH = s * (0.035f + open * 0.22f)
        val lipThickness = s * (0.028f + round * 0.02f)
        val outer = Size(mouthW + lipThickness * 2, mouthH + lipThickness * 2)
        // Губи
        drawOval(Color(0xFFE2606B), topLeft = Offset(cx - outer.width / 2, mouthCy - outer.height / 2), size = outer)
        // Усмішка: куточки трохи вгору
        if (smile > 0.3f && open < 0.5f) {
            val curve = Path().apply {
                moveTo(cx - outer.width / 2, mouthCy)
                quadraticTo(cx, mouthCy + s * 0.08f * smile, cx + outer.width / 2, mouthCy)
            }
            drawPath(curve, Color(0xFFC94C5A), style = Stroke(s * 0.012f))
        }
        // Порожнина рота
        val cavity = Size(mouthW, mouthH)
        val cavityTop = mouthCy - cavity.height / 2
        drawOval(Color(0xFF5A1E2B), topLeft = Offset(cx - cavity.width / 2, cavityTop), size = cavity)

        // Зуби
        if (pose.teethVisible && open > 0.06f) {
            val teethH = minOf(s * 0.035f, mouthH * 0.35f)
            drawRoundRectClipped(Color.White, Offset(cx - mouthW * 0.38f, cavityTop + s * 0.004f), Size(mouthW * 0.76f, teethH), s * 0.01f)
            if (open > 0.35f) {
                drawRoundRectClipped(Color(0xFFF4F4F4), Offset(cx - mouthW * 0.34f, cavityTop + cavity.height - teethH - s * 0.004f), Size(mouthW * 0.68f, teethH * 0.8f), s * 0.01f)
            }
        }

        // Язик
        val tongueW = mouthW * (0.3f + 0.6f * tongueWidth)
        val tongueH = cavity.height * 0.55f + s * 0.1f * maxOf(0f, tongueOut)
        val baseY = mouthCy + tongueY * (cavity.height * 0.32f) + maxOf(0f, tongueOut) * s * 0.11f + vibrate * s * 0.012f
        val sideShift = if (tongueOut < 0f && pose.tongueWidth < 0.5f) 0f else 0f
        val tongueLeft = cx - tongueW / 2 + sideShift
        // Для «годинничка» негативний tongueOut означає зсув ліворуч, позитивний — праворуч
        val lateral = if (pose.tongueWidth <= 0.35f && pose.cup == 0f && pose.tongueOut != 0f && pose.open < 0.5f) tongueOut * mouthW * 0.35f else 0f
        val tongueColor = Color(0xFFF07A8A)
        val tongue = Path().apply {
            val l = tongueLeft + lateral
            val r = l + tongueW
            val t = baseY - tongueH / 2
            val b = baseY + tongueH / 2
            moveTo(l, b)
            // Верхній край: чашечка (cup>0) — краї вгору, центр нижче; гірка (cup<0) — центр вище
            val midTop = t + cup * tongueH * 0.35f
            cubicTo(l, t - maxOf(0f, cup) * tongueH * 0.2f, l + tongueW * 0.3f, midTop, cx + lateral, midTop)
            cubicTo(r - tongueW * 0.3f, midTop, r, t - maxOf(0f, cup) * tongueH * 0.2f, r, b)
            quadraticTo(cx + lateral, b + tongueH * 0.45f, l, b)
            close()
        }
        drawPath(tongue, tongueColor)
        drawPath(tongue, Color(0xFFD95F70), style = Stroke(s * 0.008f))
        // Серединна лінія язика
        drawLine(Color(0x55B23A4B), Offset(cx + lateral, baseY - tongueH * 0.25f), Offset(cx + lateral, baseY + tongueH * 0.35f), strokeWidth = s * 0.006f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundRectClipped(color: Color, topLeft: Offset, size: Size, radius: Float) {
    drawRoundRect(color, topLeft = topLeft, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius))
}

/** Зразок виконання вправи: статична поза або чергування двох поз у ритмі. */
@Composable
fun ExerciseDemo(exercise: ArticulationExercise, running: Boolean, modifier: Modifier = Modifier, periodMs: Long = 900L) {
    var toggle by remember(exercise.id) { mutableStateOf(false) }
    LaunchedEffect(exercise.id, running) {
        if (exercise.mode == ExerciseMode.REPEAT && exercise.poseB != null) {
            while (running) {
                delay(periodMs)
                toggle = !toggle
            }
        }
    }
    val pose = if (toggle && exercise.poseB != null) exercise.poseB else exercise.poseA
    MouthAnimation(pose = pose, modifier = modifier)
}
