package com.syntaxislab.copiloto.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val OfficialRiderColorScheme = darkColorScheme(
    primary = NaranjaFuego,
    onPrimary = BlancoTexto,
    primaryContainer = NegroElevado,
    onPrimaryContainer = BlancoTexto,
    secondary = AmarilloVisibilidad,
    onSecondary = NegroBase,
    background = NegroBase,
    onBackground = BlancoTexto,
    surface = NegroSuperficie,
    onSurface = BlancoTexto,
    surfaceVariant = NegroElevado,
    onSurfaceVariant = GrisTextoSecundario,
    error = RojoEmergencia,
    onError = BlancoTexto
)

@Composable
fun CopilotoTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = NegroBase.toArgb()
            window.navigationBarColor = NegroBase.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = OfficialRiderColorScheme,
        typography = Typography,
        content = content
    )
}
