package com.syntaxislab.copiloto.communication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.syntaxislab.copiloto.MainActivity
import com.syntaxislab.copiloto.R

class WearableBridgeListenerService : WearableListenerService() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "WearableBridgeService"
        private const val SOS_CHANNEL_ID = "copiloto_emergency_sos_channel"
        private const val NOTIFICATION_ID = 2002

        const val WATCH_STATUS_PATH = "/watch_status"
        const val SOS_ALERT_PATH = "/sos_alert"
        const val SOS_CANCEL_PATH = "/sos_cancel"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val currentUserId = auth.currentUser?.uid ?: return

        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == WATCH_STATUS_PATH) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val isConnected = dataMap.getBoolean("isWatchConnected", false)

                Log.i(TAG, "Reloj enlazado/estado actualizado: isWatchConnected=$isConnected")

                // Actualizar la variable isWatchConnected a true/false en Firestore cuando el reloj se enlace
                updateWatchConnectionInFirestore(currentUserId, isConnected)
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val currentUserId = auth.currentUser?.uid ?: return
        val messageText = String(messageEvent.data)

        Log.w(TAG, "Mensaje recibido del reloj [${messageEvent.path}]: $messageText")

        when (messageEvent.path) {
            SOS_ALERT_PATH -> {
                // Recibir la señal de "Impacto Detectado" o "SOS" desde el reloj
                val isCrash = messageText.contains("IMPACT", ignoreCase = true) ||
                        messageText.contains("CRASH", ignoreCase = true)

                val alertTitle = if (isCrash) "¡IMPACTO DETECTADO EN RELOJ!" else "¡ALERTA SOS DESDE EL RELOJ!"
                val alertBody = if (isCrash) "Se ha detectado una caída o fuerte impacto. Notificando grupo y emergencias." else "El piloto ha activado la alerta de emergencia."

                Log.e(TAG, "$alertTitle -> $alertBody")

                // Activar SOS en Firestore en la telemetría del usuario
                triggerSosInFirestore(currentUserId, true)

                // Mostrar notificación urgente de emergencia en el celular
                showEmergencyNotification(alertTitle, alertBody)
            }
            SOS_CANCEL_PATH -> {
                Log.i(TAG, "Cancelación de SOS recibida del reloj")
                triggerSosInFirestore(currentUserId, false)
            }
        }
    }

    private fun updateWatchConnectionInFirestore(userId: String, isConnected: Boolean) {
        db.collection("telemetry").document(userId)
            .update("isWatchConnected", isConnected)
            .addOnSuccessListener {
                Log.d(TAG, "Firestore /telemetry/$userId actualizado: isWatchConnected=$isConnected")
            }
            .addOnFailureListener {
                // Si la telemetría no existe aún, se puede setear con merge
                db.collection("telemetry").document(userId)
                    .set(mapOf("isWatchConnected" to isConnected, "userId" to userId), SetOptions.merge())
            }
    }

    private fun triggerSosInFirestore(userId: String, isSosActive: Boolean) {
        db.collection("telemetry").document(userId)
            .update("isSosActive", isSosActive)
            .addOnSuccessListener {
                Log.d(TAG, "Firestore /telemetry/$userId actualizado: isSosActive=$isSosActive")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error actualizando estado SOS en Firestore", e)
            }
    }

    private fun showEmergencyNotification(title: String, message: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SOS_CHANNEL_ID,
                "Alertas SOS de Emergencia",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones urgentes de impacto detectado o llamadas SOS desde el reloj"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, SOS_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
