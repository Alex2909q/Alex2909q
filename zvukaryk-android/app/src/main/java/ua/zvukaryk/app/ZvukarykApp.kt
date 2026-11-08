package ua.zvukaryk.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ua.zvukaryk.app.ui.screens.*

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SESSION = "session/{soundId}"
    const val SOUND = "sound/{soundId}"
    const val PARENT = "parent"
    const val MIRROR = "mirror"
    const val STICKERS = "stickers"
    const val SOUNDS = "sounds"
    fun session(soundId: String) = "session/$soundId"
    fun sound(soundId: String) = "sound/$soundId"
}

@Composable
fun ZvukarykApp(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val current = state
    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    NavHost(navController = nav, startDestination = if (current.onboarded) Routes.HOME else Routes.ONBOARDING) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(vm = vm, onFinished = {
                nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                state = current, vm = vm,
                onStartSession = { nav.navigate(Routes.session(it)) },
                onOpenSound = { nav.navigate(Routes.sound(it)) },
                onOpenParent = { nav.navigate(Routes.PARENT) },
                onOpenMirror = { nav.navigate(Routes.MIRROR) },
                onOpenStickers = { nav.navigate(Routes.STICKERS) },
                onOpenSounds = { nav.navigate(Routes.SOUNDS) },
            )
        }
        composable(Routes.SESSION, arguments = listOf(navArgument("soundId") { type = NavType.StringType })) { entry ->
            val soundId = entry.arguments?.getString("soundId") ?: return@composable
            SessionScreen(state = current, vm = vm, soundId = soundId, onExit = { nav.popBackStack() })
        }
        composable(Routes.SOUND, arguments = listOf(navArgument("soundId") { type = NavType.StringType })) { entry ->
            val soundId = entry.arguments?.getString("soundId") ?: return@composable
            SoundDetailScreen(state = current, vm = vm, soundId = soundId, onBack = { nav.popBackStack() }, onStart = { nav.navigate(Routes.session(soundId)) })
        }
        composable(Routes.SOUNDS) {
            SoundsScreen(state = current, onBack = { nav.popBackStack() }, onOpenSound = { nav.navigate(Routes.sound(it)) })
        }
        composable(Routes.PARENT) {
            ParentScreen(state = current, vm = vm, onBack = { nav.popBackStack() }, onOpenSound = { nav.navigate(Routes.sound(it)) })
        }
        composable(Routes.MIRROR) { MirrorScreen(vm = vm, onBack = { nav.popBackStack() }) }
        composable(Routes.STICKERS) { StickersScreen(state = current, onBack = { nav.popBackStack() }) }
    }
}
