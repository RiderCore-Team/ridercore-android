package com.syntaxislab.copiloto.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.syntaxislab.copiloto.R
import com.syntaxislab.copiloto.communication.PhoneCommunicationManager
import com.syntaxislab.copiloto.emergency.SosManager
import com.syntaxislab.copiloto.location.GpsTrackingService
import com.syntaxislab.copiloto.location.LocationTrackingManager
import com.syntaxislab.copiloto.model.Telemetry
import com.syntaxislab.copiloto.model.User
import com.syntaxislab.copiloto.ui.theme.*
import java.util.Locale

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    user: User,
    modifier: Modifier = Modifier,
    squadViewModel: SquadViewModel = viewModel()
) {
    val context = LocalContext.current
    val phoneCommManager = remember { PhoneCommunicationManager(context) }
    val sosManager = remember { SosManager(context) }

    LaunchedEffect(Unit) {
        try {
            val token = context.resources.getString(R.string.mapbox_access_token)
            if (token.isNotBlank()) {
                MapboxOptions.accessToken = token
            }
        } catch (e: Exception) {
            Log.e("MapScreen", "Error inicializando MapboxOptions", e)
        }
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val isTrackingRunning by GpsTrackingService.isTrackingRunning.collectAsState()
    val activeSquad by squadViewModel.activeSquad.collectAsState()
    val membersTelemetry by squadViewModel.membersTelemetry.collectAsState()

    var maxSpeed by remember { mutableFloatStateOf(0f) }

    val foregroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            Toast.makeText(context, "Permiso de ubicación concedido", Toast.LENGTH_SHORT).show()
        }
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            val permissionsToRequest = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            foregroundPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    var currentTelemetry by remember { mutableStateOf(Telemetry(userId = user.userId)) }

    LaunchedEffect(hasLocationPermission, user.userId) {
        if (hasLocationPermission && user.userId.isNotBlank()) {
            val locationTrackingManager = LocationTrackingManager(context)
            locationTrackingManager.getLocationUpdates(user.userId, activeSquad?.squadId).collect { telemetry ->
                currentTelemetry = telemetry
                if (telemetry.speed > maxSpeed) {
                    maxSpeed = telemetry.speed.toFloat()
                }
            }
        }
    }

    val distanceToLeaderMeters = remember(currentTelemetry.latitude, currentTelemetry.longitude, membersTelemetry) {
        squadViewModel.getDistanceToLeaderMeters(currentTelemetry.latitude, currentTelemetry.longitude)
    }

    LaunchedEffect(currentTelemetry.speed, distanceToLeaderMeters) {
        phoneCommManager.sendTelemetry(
            speedKmh = currentTelemetry.speed.toInt(),
            leaderDistanceMeters = distanceToLeaderMeters,
            hazardAlert = currentTelemetry.isSosActive
        )
    }

    val hasValidLocation = currentTelemetry.latitude != 0.0 && currentTelemetry.longitude != 0.0
    val userPoint = if (hasValidLocation) Point.fromLngLat(currentTelemetry.longitude, currentTelemetry.latitude) else Point.fromLngLat(-99.1332, 19.4326)

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(userPoint)
            zoom(15.5)
            pitch(25.0)
        }
    }

    LaunchedEffect(currentTelemetry.latitude, currentTelemetry.longitude) {
        if (hasValidLocation) {
            mapViewportState.easeTo(
                cameraOptions = CameraOptions.Builder()
                    .center(Point.fromLngLat(currentTelemetry.longitude, currentTelemetry.latitude))
                    .zoom(15.5)
                    .build()
            )
        }
    }

    val compassDirection = remember(currentTelemetry.heading) {
        getHeadingDirectionString(currentTelemetry.heading)
    }

    Box(modifier = modifier.fillMaxSize().background(NegroBase)) {
        if (hasLocationPermission) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
                style = {
                    MapStyle(style = Style.DARK)
                }
            ) {
                // Marcador personalizado de la Moto del Piloto Actual
                if (hasValidLocation) {
                    ViewAnnotation(
                        options = viewAnnotationOptions {
                            geometry(userPoint)
                            allowOverlap(true)
                        }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(NegroSuperficie)
                                .border(2.dp, AmarilloVisibilidad, CircleShape)
                                .rotate(currentTelemetry.heading.toFloat())
                        ) {
                            Icon(
                                imageVector = Icons.Default.TwoWheeler,
                                contentDescription = "Rider Icon",
                                tint = NaranjaFuego,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Marcadores Dinámicos de los Compañeros de Grupo (Squad)
                membersTelemetry.filter { it.userId != user.userId && it.latitude != 0.0 }.forEach { member ->
                    val memberPoint = Point.fromLngLat(member.longitude, member.latitude)
                    val isLeader = activeSquad?.hostId == member.userId

                    ViewAnnotation(
                        options = viewAnnotationOptions {
                            geometry(memberPoint)
                            allowOverlap(true)
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isLeader) AmarilloVisibilidad else NegroElevado
                            ) {
                                Text(
                                    text = if (isLeader) "👑 Líder" else "🏍 Rider",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLeader) NegroBase else BlancoTexto,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(NegroSuperficie)
                                    .border(2.dp, if (isLeader) AmarilloVisibilidad else NaranjaFuego, CircleShape)
                                    .rotate(member.heading.toFloat())
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = null,
                                    tint = BlancoTexto,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!hasValidLocation) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = NegroSuperficie,
                    shadowElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = AmarilloVisibilidad,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Buscando señal GPS en Mapbox...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlancoTexto
                            )
                            Text(
                                text = "Asegúrate de estar al aire libre con la ubicación encendida",
                                fontSize = 12.sp,
                                color = GrisTextoSecundario
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NegroBase),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Permiso de Ubicación Requerido",
                        color = BlancoTexto,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Para mostrar tu posición en el mapa de Mapbox y la telemetría en tiempo real, habilita los permisos de ubicación.",
                        color = GrisTextoSecundario,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            val permissionsToRequest = mutableListOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            foregroundPermissionLauncher.launch(permissionsToRequest.toTypedArray())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NaranjaFuego)
                    ) {
                        Text("Conceder Permisos de GPS", color = BlancoTexto, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // HUD Velocímetro Overlay (Top Right)
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = NegroSuperficie,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.0f", currentTelemetry.speed),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AmarilloVisibilidad
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "KM/H",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrisTextoSecundario
                    )
                }
                Text(
                    text = "MÁX: ${String.format(Locale.getDefault(), "%.0f", maxSpeed)} KM/H",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NaranjaFuego
                )
            }
        }

        // Status Indicator & Brújula (Top Left)
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = NegroSuperficie,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isTrackingRunning) AmarilloVisibilidad else if (hasLocationPermission && hasValidLocation) NaranjaFuego else RojoEmergencia)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTrackingRunning) "Ruta en 2do Plano" else if (hasValidLocation) "Mapbox GPS Listo" else "Buscando GPS",
                        fontSize = 12.sp,
                        color = BlancoTexto,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (hasValidLocation) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CompassCalibration,
                            contentDescription = null,
                            tint = AmarilloVisibilidad,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "RUMBO: $compassDirection (${currentTelemetry.heading.toInt()}°)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrisTextoSecundario
                        )
                    }
                }
            }
        }

        // HUD Distancia al Líder de Grupo
        if (activeSquad != null) {
            val isUserHost = activeSquad?.hostId == user.userId
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = NegroSuperficie,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = AmarilloVisibilidad,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isUserHost) "👑 Eres el Líder del Grupo" else "Distancia al Líder: $distanceToLeaderMeters m",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlancoTexto
                    )
                }
            }
        }

        // Botón SOS Flotante Rápido (Middle Right)
        FloatingActionButton(
            onClick = {
                sosManager.triggerSosAlert(user.userId, "SOS Rápido desde Mapa")
                Toast.makeText(context, "¡Activando Alerta SOS de Emergencia!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(52.dp),
            containerColor = RojoEmergencia,
            contentColor = BlancoTexto,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Warning, contentDescription = "SOS Rápido", modifier = Modifier.size(26.dp))
        }

        // Botón de Inicio/Fin de Ruta
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(30.dp),
            color = Color.Transparent
        ) {
            Button(
                onClick = {
                    if (isTrackingRunning) {
                        GpsTrackingService.stop(context)
                        Toast.makeText(context, "Ruta finalizada y rastreo detenido", Toast.LENGTH_SHORT).show()
                    } else {
                        if (!hasLocationPermission) {
                            Toast.makeText(context, "Concede permisos de ubicación primero", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }

                        if (user.userId.isNotBlank()) {
                            GpsTrackingService.start(context, user.userId, activeSquad?.squadId)
                            Toast.makeText(context, "Rastreo GPS en segundo plano iniciado (Cada 10s)", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Inicia sesión para grabar ruta", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTrackingRunning) RojoEmergencia else NaranjaFuego
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
            ) {
                Icon(
                    imageVector = if (isTrackingRunning) Icons.Default.Stop else Icons.Default.Navigation,
                    contentDescription = null,
                    tint = BlancoTexto
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isTrackingRunning) "Detener Rastreo en Ruta" else "Iniciar Rastreo en Segundo Plano",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlancoTexto
                )
            }
        }
    }
}

fun getHeadingDirectionString(headingDegree: Double): String {
    val h = (headingDegree % 360 + 360) % 360
    return when {
        h in 22.5..67.5 -> "NE"
        h in 67.5..112.5 -> "E"
        h in 112.5..157.5 -> "SE"
        h in 157.5..202.5 -> "S"
        h in 202.5..247.5 -> "SO"
        h in 247.5..292.5 -> "O"
        h in 292.5..337.5 -> "NO"
        else -> "N"
    }
}
