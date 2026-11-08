package ua.zvukaryk.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import ua.zvukaryk.app.AppViewModel
import ua.zvukaryk.app.data.content.ArticulationExercises
import ua.zvukaryk.app.ui.components.*
import ua.zvukaryk.app.ui.theme.Ink
import ua.zvukaryk.app.ui.theme.Sunny

/** Режим «дзеркало»: фронтальна камера поруч із анімованим зразком вправи. */
@Composable
fun MirrorScreen(vm: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCamera by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }
    LaunchedEffect(Unit) { if (!hasCamera) permission.launch(Manifest.permission.CAMERA) }
    var selected by remember { mutableIntStateOf(0) }
    val exercise = ArticulationExercises.all[selected]
    var running by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize().padding(top = 4.dp)) {
        BackTopBar("Дзеркало", onBack)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(260.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                if (hasCamera) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                val future = ProcessCameraProvider.getInstance(ctx)
                                future.addListener({
                                    runCatching {
                                        val provider = future.get()
                                        val preview = Preview.Builder().build().also { it.surfaceProvider = this.surfaceProvider }
                                        provider.unbindAll()
                                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview)
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text("Дозвольте доступ до камери, щоб бачити себе поруч зі зразком. Або скористайтеся звичайним дзеркалом.", textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp))
                }
            }
            ExerciseDemo(exercise, running, Modifier.weight(1f).fillMaxHeight())
        }
        Spacer(Modifier.height(12.dp))
        Text("${exercise.emoji} ${exercise.name}", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 20.dp))
        Text(exercise.childCue, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp))
        ParentTip(exercise.parentInstruction, Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        Spacer(Modifier.height(4.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ArticulationExercises.all.indices.toList()) { i ->
                val ex = ArticulationExercises.all[i]
                SelectableChip(ex.name, selected == i, { selected = i; vm.tts.speak("${ex.name}. ${ex.childCue}") }, emoji = ex.emoji)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("Озвучити", onClick = { vm.tts.speak(exercise.childCue) }, modifier = Modifier.weight(1f), emoji = "🔊", color = Sunny, contentColor = Ink)
            BigButton(if (running) "Пауза" else "Рух", onClick = { running = !running }, modifier = Modifier.weight(1f), emoji = if (running) "⏸️" else "▶️")
        }
    }
}
