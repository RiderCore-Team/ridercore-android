package com.syntaxislab.copiloto.communication

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class PhoneCommunicationManager(private val context: Context) {

    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val messageClient: MessageClient = Wearable.getMessageClient(context)

    companion object {
        private const val TAG = "PhoneComm"
        const val TELEMETRY_PATH = "/telemetry"
        const val WATCH_STATUS_PATH = "/watch_status"
        const val SOS_ALERT_PATH = "/sos_alert"
    }

    /**
     * Envía la telemetría actual (velocidad, distancia al líder, alerta de peligro)
     * al reloj emparejado usando DataClient con entrega urgente.
     */
    fun sendTelemetry(
        speedKmh: Int,
        leaderDistanceMeters: Int,
        hazardAlert: Boolean = false
    ) {
        val putDataReq = PutDataMapRequest.create(TELEMETRY_PATH).apply {
            dataMap.putInt("speedKmh", speedKmh)
            dataMap.putInt("leaderDistanceMeters", leaderDistanceMeters)
            dataMap.putBoolean("hazardAlert", hazardAlert)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        dataClient.putDataItem(putDataReq)
            .addOnSuccessListener {
                Log.d(TAG, "Telemetría enviada al reloj: $speedKmh km/h, $leaderDistanceMeters m, hazard=$hazardAlert")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error enviando telemetría al reloj", e)
            }
    }

    /**
     * Monitorea si el reloj reporta estar conectado a través de la ruta /watch_status.
     */
    fun monitorWatchConnection(): Flow<Boolean> = callbackFlow {
        val listener = DataClient.OnDataChangedListener { dataEvents ->
            for (event in dataEvents) {
                if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == WATCH_STATUS_PATH) {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val isConnected = dataMap.getBoolean("isWatchConnected", false)
                    trySend(isConnected)
                }
            }
        }

        dataClient.addListener(listener)

        awaitClose {
            dataClient.removeListener(listener)
        }
    }

    /**
     * Escucha alertas SOS instantáneas emitidas desde el reloj (MessageClient).
     */
    fun monitorSosAlerts(): Flow<String> = callbackFlow {
        val listener = MessageClient.OnMessageReceivedListener { messageEvent ->
            if (messageEvent.path == SOS_ALERT_PATH) {
                val message = String(messageEvent.data)
                Log.w(TAG, "Alerta SOS recibida del reloj: $message")
                trySend(message)
            }
        }

        messageClient.addListener(listener)

        awaitClose {
            messageClient.removeListener(listener)
        }
    }
}
