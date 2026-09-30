package com.syntaxislab.copiloto.emergency

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.syntaxislab.copiloto.communication.PhoneCommunicationManager
import com.syntaxislab.copiloto.model.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SosManager(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val phoneCommManager = PhoneCommunicationManager(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "SosManager"
    }

    fun startListeningToWearSos(userId: String, onSosReceived: (String) -> Unit) {
        scope.launch {
            phoneCommManager.monitorSosAlerts().collectLatest { message ->
                Log.w(TAG, "Alerta SOS recibida desde el reloj: $message")
                triggerSosAlert(userId, "Alerta desde Wear OS: $message")
                onSosReceived(message)
            }
        }
    }

    fun triggerSosAlert(userId: String, reason: String = "Alerta SOS Manual") {
        if (userId.isBlank()) return

        db.collection("telemetry").document(userId)
            .update("isSosActive", true)
            .addOnSuccessListener {
                Log.i(TAG, "SOS activado exitosamente en Firestore para usuario $userId ($reason)")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error activando SOS en Firestore", e)
            }
    }

    fun cancelSosAlert(userId: String) {
        if (userId.isBlank()) return

        db.collection("telemetry").document(userId)
            .update("isSosActive", false)
            .addOnSuccessListener {
                Log.i(TAG, "SOS desactivado para el usuario $userId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error desactivando SOS en Firestore", e)
            }
    }

    fun makeEmergencyCall(phone: String) {
        if (phone.isBlank()) return
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
        }
    }

    fun sendEmergencySms(phone: String, latitude: Double, longitude: Double) {
        if (phone.isBlank()) return
        val mapsUrl = "https://maps.google.com/?q=$latitude,$longitude"
        val message = "¡ALERTA DE EMERGENCIA RIDER! Necesito asistencia. Mi ubicación actual es: $mapsUrl"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$phone")).apply {
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error abriendo aplicación de mensajes", e)
        }
    }

    fun sendDirectSms(phone: String, userName: String, latitude: Double, longitude: Double) {
        if (phone.isBlank()) return
        val mapsUrl = "https://maps.google.com/?q=$latitude,$longitude"
        val messageText = "¡ALERTA SOS AUTOMÁTICA CO-PILOTO! Emergencia detectada con el rider ${userName.ifBlank { "Rider" }}. Ubicación GPS en vivo: $mapsUrl"

        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val parts = smsManager.divideMessage(messageText)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phone, null, messageText, null, null)
            }
            Log.i(TAG, "SMS directo de emergencia enviado exitosamente a $phone con link de Maps: $mapsUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando SMS directo", e)
            sendEmergencySms(phone, latitude, longitude)
        }
    }

    fun sendDirectSmsWithLatestTelemetry(userId: String, phone: String, userName: String, fallbackLat: Double = 0.0, fallbackLng: Double = 0.0) {
        if (userId.isBlank() || phone.isBlank()) return

        db.collection("telemetry").document(userId).get()
            .addOnSuccessListener { document ->
                val telemetry = document.toObject(Telemetry::class.java)
                val lat = if (telemetry != null && telemetry.latitude != 0.0) telemetry.latitude else fallbackLat
                val lng = if (telemetry != null && telemetry.longitude != 0.0) telemetry.longitude else fallbackLng

                sendDirectSms(phone, userName, lat, lng)
            }
            .addOnFailureListener {
                sendDirectSms(phone, userName, fallbackLat, fallbackLng)
            }
    }
}
