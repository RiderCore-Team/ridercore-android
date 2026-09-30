package com.syntaxislab.copiloto.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syntaxislab.copiloto.model.User
import com.syntaxislab.copiloto.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    user: User,
    onProfileSaved: () -> Unit,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current

    var emergencyPhone by remember { mutableStateOf(user.emergencyPhone) }
    var bloodType by remember { mutableStateOf(if (user.bloodType.isNotBlank()) user.bloodType else "O+") }
    var bikeModel by remember { mutableStateOf(user.bikeModel) }

    var isLoading by remember { mutableStateOf(false) }

    val bloodTypes = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
    var bloodDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroBase)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Encabezado
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AmarilloVisibilidad,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = "CONFIGURACIÓN RIDER",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NegroBase,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }

        Text(
            text = "Perfil del Piloto",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = BlancoTexto
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Hola ${user.displayName.ifBlank { "Piloto" }}, registra tus datos para telemetría y emergencias SOS",
            fontSize = 14.sp,
            color = GrisTextoSecundario
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Tarjeta de Formulario
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NegroSuperficie),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                // Teléfono de Emergencia
                OutlinedTextField(
                    value = emergencyPhone,
                    onValueChange = { emergencyPhone = it },
                    label = { Text("Teléfono de Emergencia (SOS)") },
                    placeholder = { Text("+52 555 123 4567") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = NaranjaFuego) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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

                // Tipo de Sangre
                ExposedDropdownMenuBox(
                    expanded = bloodDropdownExpanded,
                    onExpandedChange = { bloodDropdownExpanded = !bloodDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = bloodType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Grupo Sanguíneo") },
                        leadingIcon = { Icon(Icons.Default.Bloodtype, contentDescription = null, tint = RojoEmergencia) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
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

                    ExposedDropdownMenu(
                        expanded = bloodDropdownExpanded,
                        onDismissRequest = { bloodDropdownExpanded = false },
                        modifier = Modifier.background(NegroElevado)
                    ) {
                        bloodTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type, color = BlancoTexto) },
                                onClick = {
                                    bloodType = type
                                    bloodDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Modelo de la Moto
                OutlinedTextField(
                    value = bikeModel,
                    onValueChange = { bikeModel = it },
                    label = { Text("Marca y Modelo de la Moto") },
                    placeholder = { Text("Ej. Yamaha MT-07 / BMW R1250GS") },
                    leadingIcon = { Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = AmarilloVisibilidad) },
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
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Botón Guardar Perfil
        Button(
            onClick = {
                if (emergencyPhone.isBlank()) {
                    Toast.makeText(context, "Ingresa un número de emergencia", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true
                authViewModel.saveProfileDetails(
                    emergencyPhone = emergencyPhone,
                    bloodType = bloodType,
                    bikeModel = bikeModel
                ) { success ->
                    isLoading = false
                    if (success) {
                        Toast.makeText(context, "Perfil guardado correctamente", Toast.LENGTH_SHORT).show()
                        onProfileSaved()
                    } else {
                        Toast.makeText(context, "Error al guardar el perfil", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NaranjaFuego,
                contentColor = BlancoTexto
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = BlancoTexto, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "Guardar Perfil y Continuar",
                    fontSize = 16.sp,
                    color = BlancoTexto,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón Cerrar Sesión
        Button(
            onClick = {
                authViewModel.signOut()
                Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NegroElevado,
                contentColor = RojoEmergencia
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Cerrar Sesión",
                tint = RojoEmergencia,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Cerrar Sesión",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = RojoEmergencia
            )
        }
    }
}
