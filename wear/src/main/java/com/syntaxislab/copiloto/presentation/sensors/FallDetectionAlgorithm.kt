package com.syntaxislab.copiloto.presentation.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

enum class FallState {
    IDLE,
    IMPACT_DETECTED,
    SOS_TRIGGERED
}

class FallDetectionAlgorithm(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /**
     * Umbrales físicos:
     * - Gravedad normal (reposo): ~9.8 m/s² (1G)
     * - IMPACT_THRESHOLD: > 39.2 m/s² (Aprox 4G). Detecta colisiones reales.
     * - MOVEMENT_THRESHOLD: > 15 m/s² o < 4 m/s². Detecta si el piloto sigue moviéndose tras el impacto.
     */
    private val IMPACT_THRESHOLD = 39.2f
    private val MOVEMENT_UPPER_THRESHOLD = 15.0f
    private val MOVEMENT_LOWER_THRESHOLD = 4.0f
    private val POST_IMPACT_TIMER_MS = 5000L

    fun monitorFallEvents(): Flow<FallState> = callbackFlow {
        var currentState = FallState.IDLE
        var impactTime = 0L
        var hasMovedAfterImpact = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                // Magnitud del vector de aceleración (A = √(x² + y² + z²))
                val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val now = System.currentTimeMillis()

                when (currentState) {
                    FallState.IDLE -> {
                        if (magnitude > IMPACT_THRESHOLD) {
                            currentState = FallState.IMPACT_DETECTED
                            impactTime = now
                            hasMovedAfterImpact = false
                            trySend(currentState)
                            Log.d("FallDetection", "¡Impacto inicial detectado! (${magnitude} m/s²)")
                        }
                    }

                    FallState.IMPACT_DETECTED -> {
                        val timeElapsed = now - impactTime

                        if (timeElapsed < POST_IMPACT_TIMER_MS) {
                            // Estamos dentro de los 5 segundos de gracia. Revisamos si hay movimiento.
                            if (magnitude > MOVEMENT_UPPER_THRESHOLD || magnitude < MOVEMENT_LOWER_THRESHOLD) {
                                hasMovedAfterImpact = true
                            }
                        } else {
                            // Ya pasaron los 5 segundos
                            if (hasMovedAfterImpact) {
                                // Hubo movimiento, fue una falsa alarma (bache fuerte o el piloto se levantó)
                                currentState = FallState.IDLE
                                trySend(currentState)
                                Log.d("FallDetection", "Falsa alarma descartada por movimiento posterior.")
                            } else {
                                // NO hubo movimiento. Accidente grave confirmado.
                                currentState = FallState.SOS_TRIGGERED
                                trySend(currentState)
                                triggerSosMessageToPhone()
                                Log.e("FallDetection", "¡SOS DISPARADO! Sin movimiento post-impacto.")
                                
                                // Regresamos a IDLE para no ciclar el estado indefinidamente, o lo dejamos así
                                // dependiendo de cómo lo maneje la UI.
                                currentState = FallState.IDLE
                            }
                        }
                    }
                    else -> {}
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    /**
     * Utiliza la API de Wearable Data Layer para mandar un mensaje urgente al teléfono.
     */
    private fun triggerSosMessageToPhone() {
        val messageClient = Wearable.getMessageClient(context)
        val nodeClient = Wearable.getNodeClient(context)

        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                messageClient.sendMessage(
                    node.id, 
                    "/sos_alert", 
                    "SOS_TRIGGERED".toByteArray()
                ).addOnSuccessListener {
                    Log.d("FallDetection", "Mensaje SOS enviado al celular exitosamente.")
                }.addOnFailureListener { e ->
                    Log.e("FallDetection", "Error al enviar mensaje SOS al celular", e)
                }
            }
        }
    }
}
