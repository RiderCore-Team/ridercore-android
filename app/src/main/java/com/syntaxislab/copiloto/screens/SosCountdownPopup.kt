package com.syntaxislab.copiloto.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.syntaxislab.copiloto.emergency.SosManager
import com.syntaxislab.copiloto.model.User
import com.syntaxislab.copiloto.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SosCountdownPopup(
    user: User,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sosManager = remember { SosManager(context) }

    var secondsRemaining by remember { mutableIntStateOf(10) }
    var isCancelled by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_CONTACTS
            )
        )
    }

    LaunchedEffect(isCancelled) {
        if (!isCancelled) {
            secondsRemaining = 10
            while (secondsRemaining > 0 && !isCancelled) {
                delay(1000L)
                secondsRemaining--
            }

            if (secondsRemaining == 0 && !isCancelled) {
                sosManager.sendDirectSmsWithLatestTelemetry(
                    userId = user.userId,
                    phone = user.emergencyPhone,
                    userName = user.displayName
                )
                Toast.makeText(
                    context,
                    "¡SMS de emergencia enviado automáticamente con link de Google Maps!",
                    Toast.LENGTH_LONG
                ).show()
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = NegroSuperficie,
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = RojoEmergencia,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "¡ALERTA SOS DETECTADA!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = RojoEmergencia
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Se enviará un SMS automático a tu contacto de emergencia con tu ubicación GPS en Google Maps.",
                    fontSize = 13.sp,
                    color = GrisTextoSecundario,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .background(RojoEmergencia, CircleShape)
                ) {
                    Text(
                        text = "$secondsRemaining",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BlancoTexto
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        isCancelled = true
                        sosManager.cancelSosAlert(user.userId)
                        Toast.makeText(context, "Alerta cancelada (Falsa Alarma)", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NegroElevado)
                ) {
                    Text(
                        text = "CANCELAR FALSA ALARMA",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlancoTexto
                    )
                }
            }
        }
    }
}
