package com.syntaxislab.copiloto.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

val NeonCyan = Color(0xFF00FFFF)
val NeonFuchsia = Color(0xFFFF00FF)
val SolidBlack = Color(0xFF000000)
val DarkGray = Color(0xFF1A1A1A)

val CopilotoWearColorScheme = ColorScheme(
    primary = NeonCyan,
    onPrimary = SolidBlack,
    primaryContainer = DarkGray,
    onPrimaryContainer = NeonCyan,
    secondary = NeonFuchsia,
    onSecondary = SolidBlack,
    secondaryContainer = DarkGray,
    onSecondaryContainer = NeonFuchsia,
    background = SolidBlack,
    onBackground = Color.White
)

@Composable
fun CopilotoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CopilotoWearColorScheme,
        content = content
    )
}