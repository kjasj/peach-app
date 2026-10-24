package video.peach.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(primary = Color(0xFF171717), onPrimary = Color.White, secondary = Color(0xFFB54566), secondaryContainer = Color(0xFFEBEBEB), onSecondaryContainer = Color(0xFF171717), background = Color.White, surface = Color(0xFFFAFAFA), surfaceVariant = Color(0xFFF2F2F2), outlineVariant = Color(0xFFE5E5E5), onBackground = Color(0xFF171717), onSurface = Color(0xFF171717))
private val Dark = darkColorScheme(primary = Color(0xFFF5F5F5), onPrimary = Color(0xFF171717), secondary = Color(0xFFFF9AAB), secondaryContainer = Color(0xFF292B31), onSecondaryContainer = Color(0xFFF3F3F3), background = Color(0xFF101114), surface = Color(0xFF191A1E), surfaceVariant = Color(0xFF26272D), outlineVariant = Color(0xFF34353C), onBackground = Color(0xFFF3F3F3), onSurface = Color(0xFFF3F3F3))
@Composable fun PeachTheme(mode: String, content: @Composable () -> Unit) {
    val dark = mode == "dark" || (mode == "system" && isSystemInDarkTheme())
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.let { activity ->
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = Typography(), content = content)
}
