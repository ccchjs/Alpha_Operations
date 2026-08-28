package com.airemore.fieldapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

val AiremoreBlue = Color(0xFF0B5FA5)
val AiremoreBlueDark = Color(0xFF083F6E)
val AiremoreAmber = Color(0xFFE8A33D)
val StatusSynced = Color(0xFF2E7D32)
val StatusPending = Color(0xFFB58900)
val StatusFailed = Color(0xFFC62828)
val StatusDraft = Color(0xFF757575)

private val LightColors = lightColorScheme(
    primary = AiremoreBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E8F8),
    secondary = AiremoreAmber,
    surface = Color(0xFFFAFAFA),
    background = Color(0xFFF4F6F8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FB8E8),
    secondary = AiremoreAmber,
)

@Composable
fun AiremoreFieldTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        val context = LocalContext.current
        androidx.compose.runtime.SideEffect {
            val activity = context as? Activity ?: return@SideEffect
            activity.window.statusBarColor = colors.primary.toArgb()
            WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(colorScheme = colors, content = content)
}
