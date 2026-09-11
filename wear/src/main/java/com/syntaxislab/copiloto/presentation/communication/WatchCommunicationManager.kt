package com.syntaxislab.copiloto.presentation.communication

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.syntaxislab.copiloto.presentation.ui.TelemetryData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WatchCommunicationManager(private val context: Context) {

    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)

    // Definimos el nombre de la capacidad que debe tener la app del celular
    // (Declarado en res/values/wear.xml del módulo app)
    private val PHONE_CAPABILITY_NAME = "copiloto_phone_app"

    /**
     * Monitorea en tiempo real si el celular está conectado al reloj.
     * Retorna un Flow que emite true (conectado) o false (desconectado).
     */
    fun monitorConnectionStatus(): Flow<Boolean> = callbackFlow {
        // Chequeo inicial
        val capabilityInfo = capabilityClient.getCapability(
            PHONE_CAPABILITY_NAME, 
            CapabilityClient.FILTER_REACHABLE
        ).await()
        
        trySend(capabilityInfo.nodes.isNotEmpty())

        // Listener para cambios en tiempo real
        val listener = CapabilityClient.OnCapabilityChangedListener { info ->
            val isConnected = info.nodes.isNotEmpty()
            trySend(isConnected)
            
            // Sincronizamos el estado hacia el celular usando DataClient
            syncWatchState(isConnected)
        }

        capabilityClient.addListener(listener, PHONE_CAPABILITY_NAME)

        awaitClose {
            capabilityClient.removeListener(listener)
        }
    }

    /**
     * Escucha en tiempo real la telemetría enviada por el celular a través de DataClient.
     * Lee la ruta "/telemetry" y emite un nuevo TelemetryData ante cada cambio recibido.
     */
    fun monitorTelemetry(): Flow<TelemetryData> = callbackFlow {
        val listener = DataClient.OnDataChangedListener { dataEvents ->
            for (event in dataEvents) {
                if (event.type == DataEvent.TYPE_CHANGED) {
                    val item = event.dataItem
                    if (item.uri.path == "/telemetry") {
                        try {
                            val dataMap = DataMapItem.fromDataItem(item).dataMap
                            val speed = if (dataMap.containsKey("speedKmh")) {
                                dataMap.getInt("speedKmh")
                            } else {
                                dataMap.getInt("speed", 0)
                            }
                            val leaderDist = if (dataMap.containsKey("leaderDistanceMeters")) {
                                dataMap.getInt("leaderDistanceMeters")
                            } else {
                                dataMap.getInt("leaderDist", 0)
                            }
                            val hazard = dataMap.getBoolean("hazardAlert", false)

                            trySend(
                                TelemetryData(
                                    speedKmh = speed,
                                    leaderDistanceMeters = leaderDist,
                                    hazardAlert = hazard
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("WatchComm", "Error al procesar telemetría", e)
                        }
                    }
                }
            }
        }

        dataClient.addListener(listener)

        // Consultamos el último dato recibido para tenerlo disponible de inmediato
        dataClient.getDataItems(Uri.parse("wear://*/telemetry")).addOnSuccessListener { buffer ->
            try {
                for (item in buffer) {
                    val dataMap = DataMapItem.fromDataItem(item).dataMap
                    val speed = if (dataMap.containsKey("speedKmh")) {
                        dataMap.getInt("speedKmh")
                    } else {
                        dataMap.getInt("speed", 0)
                    }
                    val leaderDist = if (dataMap.containsKey("leaderDistanceMeters")) {
                        dataMap.getInt("leaderDistanceMeters")
                    } else {
                        dataMap.getInt("leaderDist", 0)
                    }
                    val hazard = dataMap.getBoolean("hazardAlert", false)

                    trySend(
                        TelemetryData(
                            speedKmh = speed,
                            leaderDistanceMeters = leaderDist,
                            hazardAlert = hazard
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("WatchComm", "Error al leer telemetría inicial", e)
            } finally {
                buffer.release()
            }
        }.addOnFailureListener { e ->
            Log.e("WatchComm", "Error al consultar telemetría inicial", e)
        }

        awaitClose {
            dataClient.removeListener(listener)
        }
    }

    /**
     * 1. DataClient: Sincroniza estados constantes (variables que viven en la nube local).
     * Manda el estado isWatchConnected = true/false al celular.
     */
    fun syncWatchState(isConnected: Boolean) {
        val putDataReq = PutDataMapRequest.create("/watch_status").apply {
            dataMap.putBoolean("isWatchConnected", isConnected)
            dataMap.putLong("timestamp", System.currentTimeMillis()) // Forzamos actualización
        }.asPutDataRequest().setUrgent()

        dataClient.putDataItem(putDataReq).addOnSuccessListener {
            Log.d("WatchComm", "Estado sincronizado con DataClient: isConnected=$isConnected")
        }.addOnFailureListener { e ->
            Log.e("WatchComm", "Error al sincronizar estado", e)
        }
    }

    /**
     * 2. MessageClient: Envía eventos instantáneos (Ej. disparar SOS).
     */
    fun sendSosMessage() {
        capabilityClient.getCapability(PHONE_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE)
            .addOnSuccessListener { capabilityInfo ->
                val connectedNodes = capabilityInfo.nodes
                if (connectedNodes.isEmpty()) {
                    Log.e("WatchComm", "No hay celular conectado para enviar SOS.")
                    return@addOnSuccessListener
                }

                for (node in connectedNodes) {
                    messageClient.sendMessage(
                        node.id,
                        "/sos_alert",
                        "SOS_TRIGGERED".toByteArray()
                    ).addOnSuccessListener {
                        Log.d("WatchComm", "Mensaje SOS instantáneo enviado exitosamente al nodo \${node.id}")
                    }.addOnFailureListener { e ->
                        Log.e("WatchComm", "Falló el envío del mensaje SOS", e)
                    }
                }
            }
    }
}
