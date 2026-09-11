package com.syntaxislab.copiloto.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.syntaxislab.copiloto.presentation.communication.WatchCommunicationManager
import com.syntaxislab.copiloto.presentation.sensors.CrashDetector
import com.syntaxislab.copiloto.presentation.sensors.FallState
import com.syntaxislab.copiloto.presentation.theme.CopilotoTheme
import com.syntaxislab.copiloto.presentation.ui.ManualSosScreen
import com.syntaxislab.copiloto.presentation.ui.RadarDashboard
import com.syntaxislab.copiloto.presentation.ui.SosCountdownScreen
import com.syntaxislab.copiloto.presentation.ui.SosSentScreen
import com.syntaxislab.copiloto.presentation.ui.TelemetryData

enum class PanicNavScreen {
    DASHBOARD,
    MANUAL_SOS,
    COUNTDOWN,
    SOS_SENT
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp("Piloto")
        }
    }
}

@Composable
fun WearApp(greetingName: String) {
    CopilotoTheme {
        val context = LocalContext.current
        val communicationManager = remember { WatchCommunicationManager(context) }
        val crashDetector = remember { CrashDetector(context) }

        var currentScreen by remember { mutableStateOf(PanicNavScreen.DASHBOARD) }

        // Colectamos el estado de conexión con el teléfono
        val isPhoneConnected by communicationManager.monitorConnectionStatus().collectAsState(initial = false)

        // Telemetría en tiempo real recibida desde el celular vía DataClient
        val liveTelemetry by communicationManager.monitorTelemetry().collectAsState(
            initial = TelemetryData()
        )

        // Monitoreamos eventos del acelerómetro / detección de caída
        val fallState by crashDetector.monitorFallEvents().collectAsState(initial = FallState.IDLE)

        // Si el sensor detecta impacto/caída, activamos automáticamente la cuenta regresiva de 10s
        LaunchedEffect(fallState) {
            if (fallState == FallState.IMPACT_DETECTED && currentScreen == PanicNavScreen.DASHBOARD) {
                currentScreen = PanicNavScreen.COUNTDOWN
            }
        }

        AppScaffold {
            when (currentScreen) {
                PanicNavScreen.DASHBOARD -> {
                    val telemetryData = if (!isPhoneConnected) {
                        liveTelemetry.copy(hazardAlert = true)
                    } else {
                        liveTelemetry
                    }

                    RadarDashboard(
                        telemetryData = telemetryData,
                        onOpenSos = {
                            currentScreen = PanicNavScreen.MANUAL_SOS
                        }
                    )
                }

                PanicNavScreen.MANUAL_SOS -> {
                    ManualSosScreen(
                        onTriggerSosCountdown = {
                            currentScreen = PanicNavScreen.COUNTDOWN
                        },
                        onBack = {
                            currentScreen = PanicNavScreen.DASHBOARD
                        }
                    )
                }

                PanicNavScreen.COUNTDOWN -> {
                    SosCountdownScreen(
                        initialSeconds = 10,
                        onSosTriggered = {
                            communicationManager.sendSosMessage()
                            currentScreen = PanicNavScreen.SOS_SENT
                        },
                        onCancelAlarm = {
                            communicationManager.sendSosCancelMessage()
                            currentScreen = PanicNavScreen.DASHBOARD
                        }
                    )
                }

                PanicNavScreen.SOS_SENT -> {
                    SosSentScreen(
                        onCancelAlarm = {
                            communicationManager.sendSosCancelMessage()
                            currentScreen = PanicNavScreen.DASHBOARD
                        }
                    )
                }
            }
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun DefaultPreview() {
    CopilotoTheme {
        RadarDashboard(
            telemetryData = TelemetryData(
                speedKmh = 124,
                leaderDistanceMeters = 150,
                hazardAlert = false
            )
        )
    }
}

@WearPreviewDevices
@Composable
fun ManualSosPreview() {
    CopilotoTheme {
        ManualSosScreen(
            onTriggerSosCountdown = {},
            onBack = {}
        )
    }
}

@WearPreviewDevices
@Composable
fun CountdownPreview() {
    CopilotoTheme {
        SosCountdownScreen(
            initialSeconds = 10,
            onSosTriggered = {},
            onCancelAlarm = {}
        )
    }
}