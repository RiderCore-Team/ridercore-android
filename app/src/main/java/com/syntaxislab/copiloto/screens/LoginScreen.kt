package com.syntaxislab.copiloto.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syntaxislab.copiloto.ui.theme.*

@Composable
fun LoginScreen(
    onGoogleSignInClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroBase),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icono de Moto con Badge Amarillo de Alta Visibilidad
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(NegroSuperficie)
                    .border(3.dp, AmarilloVisibilidad, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TwoWheeler,
                    contentDescription = null,
                    tint = NaranjaFuego,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Título de la App
            Text(
                text = "Co-piloto",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BlancoTexto
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Badge RIDERCORE
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AmarilloVisibilidad
            ) {
                Text(
                    text = "RIDERCORE SYSTEM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NegroBase,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Tu acompañante inteligente en cada ruta y rodada",
                fontSize = 14.sp,
                color = GrisTextoSecundario
            )

            Spacer(modifier = Modifier.height(56.dp))

            // Botón de Google Sign-In con Naranja Fuego
            Button(
                onClick = onGoogleSignInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NaranjaFuego,
                    contentColor = BlancoTexto
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Iniciar Sesión con Google",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlancoTexto
                    )
                }
            }
        }
    }
}
