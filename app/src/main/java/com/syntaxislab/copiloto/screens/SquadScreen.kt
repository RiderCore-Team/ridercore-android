package com.syntaxislab.copiloto.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Navigation
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.syntaxislab.copiloto.model.Telemetry
import com.syntaxislab.copiloto.model.User
import com.syntaxislab.copiloto.ui.theme.*
import java.util.Locale

@Composable
fun SquadScreen(
    user: User,
    modifier: Modifier = Modifier,
    squadViewModel: SquadViewModel = viewModel()
) {
    val context = LocalContext.current
    val activeSquad by squadViewModel.activeSquad.collectAsState()
    val membersTelemetry by squadViewModel.membersTelemetry.collectAsState()

    var squadCodeInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NegroBase)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = AmarilloVisibilidad,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "MotoSquad en Ruta",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BlancoTexto
            )
        }

        if (activeSquad == null) {
            // Crear o unirse a grupo
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = NegroSuperficie),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Crear Nuevo MotoSquad",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlancoTexto
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Genera un código único para transmitir telemetría y liderar la ruta",
                        fontSize = 13.sp,
                        color = GrisTextoSecundario
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            squadViewModel.createSquad(user.userId) { code ->
                                isLoading = false
                                if (code != null) {
                                    Toast.makeText(context, "MotoSquad creado: $code", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error al crear MotoSquad", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NaranjaFuego)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = BlancoTexto)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Crear MotoSquad", fontWeight = FontWeight.Bold, color = BlancoTexto)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = NegroSuperficie),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Unirse a un MotoSquad",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlancoTexto
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = squadCodeInput,
                        onValueChange = { squadCodeInput = it.uppercase() },
                        label = { Text("Código de 6 Caracteres") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NaranjaFuego,
                            unfocusedBorderColor = NegroElevado,
                            focusedLabelColor = NaranjaFuego,
                            unfocusedLabelColor = GrisTextoSecundario,
                            focusedTextColor = BlancoTexto,
                            unfocusedTextColor = BlancoTexto,
                            focusedContainerColor = NegroElevado,
                            unfocusedContainerColor = NegroElevado
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (squadCodeInput.isBlank()) {
                                Toast.makeText(context, "Ingresa el código del grupo", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isLoading = true
                            squadViewModel.joinSquad(squadCodeInput, user.userId) { _, message ->
                                isLoading = false
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NegroElevado)
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, tint = AmarilloVisibilidad)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unirse con Código", fontWeight = FontWeight.Bold, color = AmarilloVisibilidad)
                    }
                }
            }
        } else {
            val squad = activeSquad!!
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = NegroSuperficie),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "CÓDIGO DE MOTOSQUAD", fontSize = 11.sp, color = GrisTextoSecundario, fontWeight = FontWeight.Bold)
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = squad.squadId,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AmarilloVisibilidad
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("SquadCode", squad.squadId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Código copiado: ${squad.squadId}", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar Código", tint = NaranjaFuego)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Riders Activos en Ruta: ${squad.activeMembers.size}",
                        fontSize = 13.sp,
                        color = BlancoTexto,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { squadViewModel.leaveSquad(user.userId) },
                        colors = ButtonDefaults.buttonColors(containerColor = RojoEmergencia),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Salir del MotoSquad", color = BlancoTexto, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Telemetría de Integrantes en Vivo",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BlancoTexto,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 10.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(membersTelemetry) { telemetry ->
                    MemberTelemetryCard(
                        telemetry = telemetry,
                        currentUserId = user.userId,
                        leaderId = squad.hostId
                    )
                }
            }
        }
    }
}

@Composable
fun MemberTelemetryCard(telemetry: Telemetry, currentUserId: String, leaderId: String) {
    val isMe = telemetry.userId == currentUserId
    val isLeader = telemetry.userId == leaderId

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMe) NegroElevado else NegroSuperficie
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (telemetry.isSosActive) RojoEmergencia else AmarilloVisibilidad)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isMe) "Tú (Piloto)" else "Rider: ${telemetry.userId.take(6)}",
                            fontWeight = FontWeight.Bold,
                            color = BlancoTexto,
                            fontSize = 15.sp
                        )
                        if (isLeader) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AmarilloVisibilidad
                            ) {
                                Text(
                                    text = "👑 LÍDER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NegroBase,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (telemetry.isSosActive) "¡ALERTA SOS!" else "Velocidad: ${String.format(Locale.getDefault(), "%.0f", telemetry.speed)} km/h",
                            fontSize = 12.sp,
                            color = if (telemetry.isSosActive) RojoEmergencia else GrisTextoSecundario
                        )
                        if (telemetry.isWatchConnected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Watch, contentDescription = "Watch Connected", tint = AmarilloVisibilidad, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = null,
                tint = NaranjaFuego,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
