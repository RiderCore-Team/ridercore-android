package com.syntaxislab.copiloto.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syntaxislab.copiloto.emergency.SosManager
import com.syntaxislab.copiloto.model.User
import com.syntaxislab.copiloto.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun EmergencyScreen(
    user: User,
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sosManager = remember { SosManager(context) }

    var isSosActive by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            progress = 0f
            while (progress < 1f && isPressed) {
                delay(30)
                progress += 0.01f
            }
            if (progress >= 1f) {
                isSosActive = true
                sosManager.triggerSosAlert(user.userId, "Botón SOS en Pantalla de Emergencia")
                Toast.makeText(context, "¡ALERTA SOS ACTIVADA!", Toast.LENGTH_LONG).show()
            }
        } else {
            progress = 0f
        }
    }

    val buttonBgColor by animateColorAsState(
        targetValue = if (isSosActive) RojoEmergencia else if (isPressed) NaranjaFuego else NegroElevado,
        label = "SosColorAnimation"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NegroBase)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Encabezado
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSosActive) RojoEmergencia else AmarilloVisibilidad
            ) {
                Text(
                    text = if (isSosActive) "¡ALERTA SOS ACTIVA!" else "SISTEMA DE EMERGENCIA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSosActive) BlancoTexto else NegroBase,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isSosActive) "¡SOS ACTIVO!" else "Puente SOS Rider",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSosActive) RojoEmergencia else BlancoTexto
            )
            Text(
                text = if (isSosActive) "Se ha notificado a tu grupo y sistema" else "Mantén presionado 3 segundos para activar SOS",
                fontSize = 13.sp,
                color = GrisTextoSecundario
            )
        }

        // Botón SOS Central
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .background(buttonBgColor, CircleShape)
                .pointerInput(isSosActive) {
                    if (!isSosActive) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            }
                        )
                    }
                }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isSosActive || isPressed) BlancoTexto else AmarilloVisibilidad,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isSosActive) "SOS" else "PRESIONAR",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BlancoTexto
                )
                if (!isSosActive) {
                    Text(
                        text = "3 SEG",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmarilloVisibilidad
                    )
                }
            }

            if (isPressed && !isSosActive) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(210.dp),
                    color = AmarilloVisibilidad,
                    strokeWidth = 6.dp
                )
            }
        }

        if (isSosActive) {
            Button(
                onClick = {
                    isSosActive = false
                    sosManager.cancelSosAlert(user.userId)
                    Toast.makeText(context, "Alerta SOS cancelada", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NegroElevado),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Desactivar Alerta SOS", color = BlancoTexto)
            }
        }

        // Ficha Médica
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NegroSuperficie)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MedicalServices, contentDescription = null, tint = AmarilloVisibilidad)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ficha Médica del Rider", fontWeight = FontWeight.Bold, color = BlancoTexto, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Tipo de Sangre", fontSize = 12.sp, color = GrisTextoSecundario)
                        Text(user.bloodType.ifBlank { "No especificado" }, fontWeight = FontWeight.Bold, color = AmarilloVisibilidad)
                    }
                    Column {
                        Text("Moto", fontSize = 12.sp, color = GrisTextoSecundario)
                        Text(user.bikeModel.ifBlank { "No especificado" }, fontWeight = FontWeight.Bold, color = BlancoTexto)
                    }
                    Column {
                        Text("Contacto SOS", fontSize = 12.sp, color = GrisTextoSecundario)
                        Text(user.emergencyPhone.ifBlank { "Sin número" }, fontWeight = FontWeight.Bold, color = NaranjaFuego)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { sosManager.makeEmergencyCall(user.emergencyPhone) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NaranjaFuego),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp), tint = BlancoTexto)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Llamar", fontSize = 13.sp, color = BlancoTexto, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { sosManager.sendEmergencySms(user.emergencyPhone, latitude, longitude) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NegroElevado),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp), tint = AmarilloVisibilidad)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SMS GPS", fontSize = 13.sp, color = BlancoTexto, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
