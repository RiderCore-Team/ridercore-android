package com.syntaxislab.copiloto.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syntaxislab.copiloto.communication.PhoneCommunicationManager
import com.syntaxislab.copiloto.model.User
import com.syntaxislab.copiloto.ui.theme.*

@Composable
fun WatchConnectionScreen(
    user: User,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val phoneCommManager = remember { PhoneCommunicationManager(context) }

    var connectedNodes by remember { mutableStateOf<List<String>>(emptyList()) }
    var isChecking by remember { mutableStateOf(false) }
    var isWatchReportedConnected by remember { mutableStateOf(false) }

    // Monitorear si el reloj reporta conexión por DataClient
    LaunchedEffect(Unit) {
        phoneCommManager.getConnectedWatchNodes { nodes ->
            connectedNodes = nodes
        }
        phoneCommManager.monitorWatchConnection().collect { connected ->
            isWatchReportedConnected = connected
        }
    }

    val isConnected = connectedNodes.isNotEmpty() || isWatchReportedConnected

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NegroBase)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Badge Encabezado
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AmarilloVisibilidad,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = "WEAR OS BRIDGE",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NegroBase,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }

        Text(
            text = "Conexión con Reloj",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = BlancoTexto
        )

        Text(
            text = "Sincroniza la telemetría en tiempo real y la detección de caídas RiderSOS",
            fontSize = 13.sp,
            color = GrisTextoSecundario,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Tarjeta Principal de Estado de Conexión
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NegroSuperficie),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(NegroElevado)
                        .border(3.dp, if (isConnected) AmarilloVisibilidad else RojoEmergencia, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = null,
                        tint = if (isConnected) NaranjaFuego else GrisTextoSecundario,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) AmarilloVisibilidad else RojoEmergencia)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConnected) "Reloj Enlazado y Activo" else "Buscando Reloj Wear OS...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlancoTexto
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (connectedNodes.isNotEmpty()) {
                        "Dispositivo: ${connectedNodes.joinToString(", ")}"
                    } else if (isWatchReportedConnected) {
                        "Dispositivo: Wear OS Smartwatch"
                    } else {
                        "Asegúrate de tener la app Co-piloto abierta en tu reloj"
                    },
                    fontSize = 13.sp,
                    color = GrisTextoSecundario
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Panel de Estado de Sensores y Puente
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NegroSuperficie)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sensors, contentDescription = null, tint = AmarilloVisibilidad)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Estado de Sensores & Puente",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlancoTexto
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                SensorStatusItem(
                    title = "Detector de Caídas (Acelerómetro)",
                    subtitle = "Escuchando mensajes de impacto desde el reloj",
                    isActive = isConnected
                )

                Spacer(modifier = Modifier.height(12.dp))

                SensorStatusItem(
                    title = "Telemetría & Velocímetro en Vivo",
                    subtitle = "Sincronizando velocidad cada 10 segundos",
                    isActive = isConnected
                )

                Spacer(modifier = Modifier.height(12.dp))

                SensorStatusItem(
                    title = "Puente de Emergencia SOS",
                    subtitle = "MessageClient listo para disparar alertas",
                    isActive = isConnected
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botones de Acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    isChecking = true
                    phoneCommManager.getConnectedWatchNodes { nodes ->
                        connectedNodes = nodes
                        isChecking = false
                        Toast.makeText(context, "Escaneo de dispositivos Wear OS completado", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isChecking,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NegroElevado)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = AmarilloVisibilidad, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Escanear", color = BlancoTexto, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    phoneCommManager.sendTelemetry(
                        speedKmh = 88,
                        leaderDistanceMeters = 120,
                        hazardAlert = false
                    )
                    Toast.makeText(context, "¡Paquete de prueba enviado al reloj!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NaranjaFuego)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = BlancoTexto, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Probar Señal", color = BlancoTexto, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Guía Rápida de Emparejamiento
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NegroSuperficie)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = AmarilloVisibilidad,
                    modifier = Modifier.size(24.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "¿Cómo conectar tu reloj?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlancoTexto
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1. Vincula tu smartwatch Wear OS por Bluetooth con tu teléfono en la app oficial Wear OS o Galaxy Wearable.\n" +
                                "2. Abre la app Co-piloto en tu reloj.\n" +
                                "3. Presiona 'Escanear' o 'Probar Señal' para sincronizar.",
                        fontSize = 12.sp,
                        color = GrisTextoSecundario,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SensorStatusItem(title: String, subtitle: String, isActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BlancoTexto)
            Text(text = subtitle, fontSize = 12.sp, color = GrisTextoSecundario)
        }
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (isActive) AmarilloVisibilidad else GrisTextoSecundario,
            modifier = Modifier.size(20.dp)
        )
    }
}
