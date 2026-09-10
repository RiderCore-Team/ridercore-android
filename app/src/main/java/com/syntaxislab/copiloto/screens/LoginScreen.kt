package com.syntaxislab.copiloto.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    onGoogleSignInClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)), // Fondo oscuro
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Título de la App
        Text(
            text = "Co-piloto",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00FFFF) // Acento cyan
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "RiderCore Team",
            fontSize = 16.sp,
            color = Color.LightGray
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Botón de Google Sign-In
        Button(
            onClick = onGoogleSignInClick,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8A2BE2) // Acento violeta
            )
        ) {
            Text(
                text = "Iniciar sesión con Google",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}