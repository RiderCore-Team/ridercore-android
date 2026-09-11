package com.syntaxislab.copiloto.presentation.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.syntaxislab.copiloto.presentation.theme.AmarilloAltaVisibilidad
import com.syntaxislab.copiloto.presentation.theme.BlancoTexto
import com.syntaxislab.copiloto.presentation.theme.GrisTextoSecundario
import com.syntaxislab.copiloto.presentation.theme.NegroBase
import com.syntaxislab.copiloto.presentation.theme.NegroSuperficie
import kotlinx.coroutines.delay

private val EmergencyRed = Color(0xFFD32F2F)
private val EmergencyDarkRed = Color(0xFF8B0000)
private val SafeGreen = Color(0xFF2E7D32)

/**
 * Pantalla con el botón táctil rojo que ocupa el 70% de la pantalla para activar el SOS manualmente.
 */
@Composable
fun ManualSosScreen(
    onTriggerSosCountdown: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NegroBase),
        contentAlignment = Alignment.Center
    ) {
        // Botón circular táctil rojo que ocupa exactamente el 70% de la pantalla
        Box(
            modifier = Modifier
                .fillMaxWidth(0.70f)
                .aspectRatio(1f)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(EmergencyRed)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTriggerSosCountdown
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Alerta SOS",
                    tint = BlancoTexto,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "SOS",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = BlancoTexto,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "TOCAR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlancoTexto.copy(alpha = 0.85f)
                )
            }
        }

        // Botón superior para volver al dashboard
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Volver",
                tint = GrisTextoSecundario,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onBack)
            )
        }
    }
}

/**
 * Pantalla de "Cuenta Regresiva" (10 segundos) con vibración háptica fuerte y botón de cancelar.
 */
@Composable
fun SosCountdownScreen(
    initialSeconds: Int = 10,
    onSosTriggered: () -> Unit,
    onCancelAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var remainingSeconds by remember { mutableIntStateOf(initialSeconds) }

    // Vibración Háptica Fuerte durante la cuenta regresiva
    DisposableEffect(Unit) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Patrón continuo de pulsos fuertes de alta intensidad
                val timings = longArrayOf(0, 450, 200, 450, 200, 600, 250)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, 0) // 0 = repetir indefinidamente
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 450, 200, 450), 0)
            }
        }

        onDispose {
            vibrator?.cancel()
        }
    }

    // Cuenta regresiva segundo a segundo
    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        } else {
            onSosTriggered()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NegroBase),
        contentAlignment = Alignment.Center
    ) {
        // Anillo de progreso circular de 10 segundos
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
        ) {
            val strokeWidth = 5.dp.toPx()
            val sweepAngle = (remainingSeconds / initialSeconds.toFloat()) * 360f

            // Anillo base gris oscuro
            drawCircle(
                color = NegroSuperficie,
                style = Stroke(width = strokeWidth)
            )

            // Arco activo rojo de emergencia
            drawArc(
                color = EmergencyRed,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Información central: Contador regresivo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "¡POSIBLE CAÍDA!",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AmarilloAltaVisibilidad,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = remainingSeconds.toString(),
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = EmergencyRed
            )

            Text(
                text = "Enviando SOS...",
                fontSize = 12.sp,
                color = BlancoTexto
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Botón de "Cancelar Alarma"
            Button(
                onClick = onCancelAlarm,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafeGreen,
                    contentColor = BlancoTexto
                )
            ) {
                Text(
                    text = "CANCELAR ALARMA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Pantalla mostrada una vez que el SOS ha sido disparado, con opción de cancelar falsa alarma.
 */
@Composable
fun SosSentScreen(
    onCancelAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NegroBase),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "SOS Enviado",
                tint = EmergencyRed,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "SOS ENVIADO",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = EmergencyRed
            )

            Text(
                text = "Alerta enviada a tu celular y red.",
                fontSize = 11.sp,
                color = GrisTextoSecundario,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onCancelAlarm,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NegroSuperficie,
                    contentColor = AmarilloAltaVisibilidad
                )
            ) {
                Text(
                    text = "ESTOY BIEN / CANCELAR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
