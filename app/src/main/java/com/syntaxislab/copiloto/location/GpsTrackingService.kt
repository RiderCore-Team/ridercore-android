package com.syntaxislab.copiloto.location

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.syntaxislab.copiloto.MainActivity
import com.syntaxislab.copiloto.R
import com.syntaxislab.copiloto.communication.PhoneCommunicationManager
import com.syntaxislab.copiloto.model.Telemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class GpsTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val db = FirebaseFirestore.getInstance()
    private lateinit var phoneCommManager: PhoneCommunicationManager

    private var userId: String = ""
    private var squadId: String? = null

    companion object {
        private const val TAG = "GpsTrackingService"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "copiloto_gps_tracking_channel"

        const val ACTION_START = "ACTION_START_GPS_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_GPS_TRACKING"

        const val EXTRA_USER_ID = "EXTRA_USER_ID"
        const val EXTRA_SQUAD_ID = "EXTRA_SQUAD_ID"

        private const val UPDATE_INTERVAL_MS = 10000L // 10 segundos
        private const val FASTEST_UPDATE_INTERVAL_MS = 5000L

        private val _isTrackingRunning = MutableStateFlow(false)
        val isTrackingRunning: StateFlow<Boolean> = _isTrackingRunning

        fun start(context: Context, userId: String, squadId: String? = null) {
            val intent = Intent(context, GpsTrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_SQUAD_ID, squadId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GpsTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        phoneCommManager = PhoneCommunicationManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                userId = intent.getStringExtra(EXTRA_USER_ID) ?: ""
                squadId = intent.getStringExtra(EXTRA_SQUAD_ID)
                startForegroundTracking()
            }
            ACTION_STOP -> {
                stopForegroundTracking()
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startForegroundTracking() {
        val notification = createNotification("Iniciando rastreo GPS para la ruta...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _isTrackingRunning.value = true

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL_MS)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val speedKmh = location.speed * 3.6

                val telemetry = Telemetry(
                    userId = userId,
                    currentSquadId = squadId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    heading = location.bearing.toDouble(),
                    speed = speedKmh,
                    lastUpdatedAt = Timestamp.now()
                )

                // Enviar objeto UserTelemetry a Firestore cada 10 segundos
                saveTelemetryToFirestore(telemetry)

                // Enviar telemetría al Reloj Wear OS
                phoneCommManager.sendTelemetry(
                    speedKmh = speedKmh.toInt(),
                    leaderDistanceMeters = 0,
                    hazardAlert = false
                )

                // Actualizar la notificación persistente con la velocidad actual
                updateNotification("Rastreo GPS Activo - Velocidad: ${String.format(Locale.getDefault(), "%.0f", speedKmh)} km/h")
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        Log.i(TAG, "Rastreo GPS iniciado en primer plano cada 10 segundos")
    }

    private fun saveTelemetryToFirestore(telemetry: Telemetry) {
        if (telemetry.userId.isBlank()) return
        db.collection("telemetry")
            .document(telemetry.userId)
            .set(telemetry)
            .addOnSuccessListener {
                Log.d(TAG, "Telemetría enviada a Firestore: ${telemetry.latitude}, ${telemetry.longitude}, ${telemetry.speed} km/h")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error enviando telemetría a Firestore", e)
            }
    }

    private fun stopForegroundTracking() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        _isTrackingRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.i(TAG, "Rastreo GPS en segundo plano detenido")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rastreo GPS Co-piloto",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación constante para el rastreo GPS en ruta en segundo plano"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Co-piloto RiderCore")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(contentText))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopForegroundTracking()
        super.onDestroy()
    }
}
