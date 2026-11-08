package ua.zvukaryk.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Sunny = Color(0xFFFFB703)
val Coral = Color(0xFFFF6B6B)
val Sky = Color(0xFF4CC9F0)
val Mint = Color(0xFF52B788)
val Grape = Color(0xFF7B2CBF)
val Cream = Color(0xFFFFF8EC)
val Ink = Color(0xFF2B2D42)
val SoftPink = Color(0xFFFFD6E0)

private val LightColors = lightColorScheme(
    primary = Grape,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEBD9FF),
    onPrimaryContainer = Ink,
    secondary = Sky,
    onSecondary = Ink,
    secondaryContainer = Color(0xFFD6F4FC),
    tertiary = Sunny,
    tertiaryContainer = Color(0xFFFFEDC2),
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF3EDE2),
    onSurfaceVariant = Color(0xFF5B5E73),
    error = Coral,
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 64.sp),
    displayMedium = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 52.sp),
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(40.dp),
)

@Composable
fun ZvukarykTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, typography = AppTypography, shapes = AppShapes, content = content)
}
