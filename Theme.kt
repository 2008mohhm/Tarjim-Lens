package app.lenstranslatorpro

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4A7FF),
    onPrimary = Color(0xFF2A1A6B),
    primaryContainer = Color(0xFF3F3590),
    onPrimaryContainer = Color(0xFFE6DEFF),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFFCCFBF1),
    background = Color(0xFF0F1020),
    onBackground = Color(0xFFE8E8F5),
    surface = Color(0xFF171829),
    onSurface = Color(0xFFE8E8F5),
    surfaceVariant = Color(0xFF23243A),
    onSurfaceVariant = Color(0xFFB8B8D0),
    outline = Color(0xFF3A3B5A)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B4BDB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6E2FF),
    onPrimaryContainer = Color(0xFF1B1060),
    secondary = Color(0xFF0D9488),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF042F2E),
    background = Color(0xFFF6F5FF),
    onBackground = Color(0xFF1A1A2E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFEEEDFA),
    onSurfaceVariant = Color(0xFF55557A),
    outline = Color(0xFFDAD9EE)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        shapes = AppShapes,
        content = content
    )
}
