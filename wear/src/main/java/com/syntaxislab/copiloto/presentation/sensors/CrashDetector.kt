package com.syntaxislab.copiloto.presentation.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

class CrashDetector(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /**
     * Este Flow emite un booleano (true) únicamente cuando detecta un impacto que supera 
     * el umbral configurado, filtrando los baches normales.
     */
    fun getCrashEvents(): Flow<Boolean> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    // 1. Calcular la Magnitud del Vector de Aceleración (Fuerza G total)
                    // Fórmula física: A = √(x² + y² + z²)
                    val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

                    // 2. Lógica de Filtro (Descartar baches)
                    // La gravedad normal de la tierra es ~9.8 m/s² (1G)
                    // Una moto pasando un bache o tope fuerte genera vibraciones de 2G a 3G (20 a 30 m/s²)
                    // Un accidente real (colisión) genera un pico brusco de más de 4G a 6G (> 40 m/s²)
                    val crashThreshold = 45.0f // m/s²

                    if (magnitude > crashThreshold) {
                        // ¡Impacto fuerte detectado!
                        trySend(true)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No es necesario manejar cambios de precisión para el acelerómetro
            }
        }

        // Registramos el listener. Usamos SENSOR_DELAY_GAME o SENSOR_DELAY_UI.
        // GAME es suficientemente rápido para detectar el pico de un choque sin fundir la batería.
        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )

        // Cuando se cancele la corrutina que escuche este Flow, apagamos el sensor para ahorrar batería
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
