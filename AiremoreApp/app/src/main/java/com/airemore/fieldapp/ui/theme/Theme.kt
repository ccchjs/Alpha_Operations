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

// Palette pulled 1:1 from the web app's :root CSS variables
// (assets/css/style.css) so the app matches the web exactly.
val AiremoreNavy = Color(0xFF0B2E5C)      // --navy
val AiremoreSteel = Color(0xFF1450A3)     // --steel
val AiremoreAccent = Color(0xFF111111)    // --accent (buttons, "more" wordmark)
val AiremoreBg = Color(0xFFF4F6F8)        // --bg
val AiremorePanel = Color(0xFFFFFFFF)     // --panel
val AiremoreBorder = Color(0xFFD9DEE3)    // --border
val AiremoreText = Color(0xFF171717)      // --text
val AiremoreTextMuted = Color(0xFF5B6570) // --text-muted
val AiremoreOk = Color(0xFF2E8B57)        // --ok
val AiremoreBad = Color(0xFFC0392B)       // --bad
val AiremoreFieldBg = Color(0xFFFBFCFD)   // .field input background
val AiremoreBtnHover = Color(0xFFCE5423)  // .btn-primary:hover

// Kept as aliases so any existing references elsewhere still resolve.
val AiremoreBlue = AiremoreSteel
val AiremoreBlueDark = AiremoreNavy
val AiremoreAmber = Color(0xFFC6790A)
val StatusSynced = AiremoreOk
val StatusPending = Color(0xFFB58900)
val StatusFailed = AiremoreBad
val StatusDraft = Color(0xFF757575)

private val LightColors = lightColorScheme(
    primary = AiremoreAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E8F8),
    secondary = AiremoreSteel,
    surface = AiremorePanel,
    background = AiremoreBg,
    error = AiremoreBad,
    outline = AiremoreBorder,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FB8E8),
    secondary = AiremoreSteel,
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
