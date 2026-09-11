package com.syntaxislab.copiloto.presentation.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.syntaxislab.copiloto.presentation.theme.NeonCyan
import com.syntaxislab.copiloto.presentation.theme.NeonFuchsia
import com.syntaxislab.copiloto.presentation.theme.SolidBlack
import kotlin.math.cos
import kotlin.math.sin

/**
 * Representa los datos que vienen del celular.
 */
data class TelemetryData(
    val speedKmh: Int = 0,
    val leaderDistanceMeters: Int = 0, // Distancia al líder (ej. 150m)
    val hazardAlert: Boolean = false // Alerta de peligro adelante
)

@Composable
fun RadarDashboard(
    telemetryData: TelemetryData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SolidBlack),
        contentAlignment = Alignment.Center
    ) {
        // 1. Fondo: Radar Circular (Rings)
        RadarBackground(hasHazard = telemetryData.hazardAlert)

        // 2. Info Central: Velocidad
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = telemetryData.speedKmh.toString(),
                fontSize = 54.sp,
                fontWeight = FontWeight.Bold,
                color = if (telemetryData.hazardAlert) Color.Red else Color.White
            )
            Text(
                text = "km/h",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        // 3. Indicador del Líder (Punto Adelante)
        if (telemetryData.leaderDistanceMeters > 0) {
            LeaderDot(distance = telemetryData.leaderDistanceMeters)
        }

        // 4. Alerta de Peligro Gigante (Parpadeo)
        if (telemetryData.hazardAlert) {
            HazardWarning()
        }
    }
}

@Composable
fun RadarBackground(hasHazard: Boolean) {
    val radarColor = if (hasHazard) Color.Red.copy(alpha = 0.5f) else NeonCyan.copy(alpha = 0.3f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.width / 2

        // Circulos concentricos
        drawCircle(
            color = radarColor,
            radius = maxRadius * 0.4f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = radarColor,
            radius = maxRadius * 0.7f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = radarColor,
            radius = maxRadius * 0.95f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun LeaderDot(distance: Int) {
    // Simulamos la posición del líder. Siempre está "adelante" (arriba en el reloj, a -90 grados).
    // Entre más lejos, más se acerca al borde exterior.
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.width / 2

        // Mapeo simple: 0m = centro, 500m = borde
        val distanceRatio = (distance / 500f).coerceIn(0.1f, 0.95f)
        val dotRadiusOffset = maxRadius * distanceRatio

        // Posición a las 12 en punto (-PI/2 radianes)
        val angleRad = -Math.PI / 2
        val dotX = center.x + (dotRadiusOffset * cos(angleRad)).toFloat()
        val dotY = center.y + (dotRadiusOffset * sin(angleRad)).toFloat()

        // Dibujamos el líder en color Fucsia (Secondary)
        drawCircle(
            color = NeonFuchsia,
            radius = 6.dp.toPx(),
            center = Offset(dotX, dotY)
        )
    }

    // Texto de la distancia arriba
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Text(
            text = "Líder: ${distance}m",
            color = NeonFuchsia,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
fun HazardWarning() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Peligro Adelante",
            tint = Color.Red.copy(alpha = alpha),
            modifier = Modifier.size(48.dp)
        )
    }
}
