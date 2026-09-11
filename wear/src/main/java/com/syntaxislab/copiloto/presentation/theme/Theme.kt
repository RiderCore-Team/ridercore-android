package com.syntaxislab.copiloto.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

val CopilotoWearColorScheme = ColorScheme(
    primary = NaranjaFuego,
    onPrimary = BlancoTexto,
    primaryContainer = NegroSuperficie,
    onPrimaryContainer = NaranjaFuego,
    secondary = AmarilloAltaVisibilidad,
    onSecondary = NegroBase,
    secondaryContainer = NegroSuperficie,
    onSecondaryContainer = AmarilloAltaVisibilidad,
    tertiary = AmarilloAltaVisibilidad,
    onTertiary = NegroBase,
    tertiaryContainer = NegroSuperficie,
    onTertiaryContainer = AmarilloAltaVisibilidad,
    background = NegroBase,
    onBackground = BlancoTexto,
    surfaceContainer = NegroSuperficie,
    surfaceContainerLow = NegroSuperficie,
    surfaceContainerHigh = NegroSuperficie,
    onSurface = BlancoTexto,
    onSurfaceVariant = GrisTextoSecundario,
    outline = GrisTextoSecundario,
    outlineVariant = NegroSuperficie
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