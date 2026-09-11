/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.syntaxislab.copiloto.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.syntaxislab.copiloto.R
import com.syntaxislab.copiloto.presentation.communication.WatchCommunicationManager
import com.syntaxislab.copiloto.presentation.ui.RadarDashboard
import com.syntaxislab.copiloto.presentation.ui.TelemetryData
import com.syntaxislab.copiloto.presentation.theme.CopilotoTheme

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
        
        // Colectamos el estado de conexión con el teléfono
        val isPhoneConnected by communicationManager.monitorConnectionStatus().collectAsState(initial = false)

        // Telemetría en tiempo real recibida desde el celular vía DataClient
        val liveTelemetry by communicationManager.monitorTelemetry().collectAsState(
            initial = TelemetryData()
        )

        AppScaffold {
            // Si el celular se encuentra desconectado, activamos alerta de peligro visual para advertir al piloto
            val telemetryData = if (!isPhoneConnected) {
                liveTelemetry.copy(hazardAlert = true)
            } else {
                liveTelemetry
            }

            RadarDashboard(telemetryData = telemetryData)
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun DefaultPreview() {
    CopilotoTheme {
        // En preview mostramos datos de prueba visuales para maquetar cómodamente
        RadarDashboard(
            telemetryData = TelemetryData(
                speedKmh = 124,
                leaderDistanceMeters = 150,
                hazardAlert = false
            )
        )
    }
}