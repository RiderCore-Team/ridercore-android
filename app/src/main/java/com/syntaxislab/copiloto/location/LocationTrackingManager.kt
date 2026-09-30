package com.syntaxislab.copiloto.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.syntaxislab.copiloto.communication.PhoneCommunicationManager
import com.syntaxislab.copiloto.model.Telemetry
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationTrackingManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val db = FirebaseFirestore.getInstance()
    private val phoneCommManager = PhoneCommunicationManager(context)

    companion object {
        private const val TAG = "LocationTracking"
        private const val UPDATE_INTERVAL_MS = 3000L
        private const val FASTEST_UPDATE_INTERVAL_MS = 1500L
    }

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(userId: String, currentSquadId: String? = null): Flow<Telemetry> = callbackFlow {
        // Emitir última ubicación conocida inmediatamente si está disponible
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val speedKmh = (location.speed * 3.6)
                val initialTelemetry = Telemetry(
                    userId = userId,
                    currentSquadId = currentSquadId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    heading = location.bearing.toDouble(),
                    speed = speedKmh,
                    lastUpdatedAt = Timestamp.now()
                )
                trySend(initialTelemetry)
            }
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL_MS)
        }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    val speedKmh = (location.speed * 3.6)
                    val telemetry = Telemetry(
                        userId = userId,
                        currentSquadId = currentSquadId,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        heading = location.bearing.toDouble(),
                        speed = speedKmh,
                        lastUpdatedAt = Timestamp.now()
                    )

                    trySend(telemetry)
                    saveTelemetryToFirestore(telemetry)

                    phoneCommManager.sendTelemetry(
                        speedKmh = speedKmh.toInt(),
                        leaderDistanceMeters = 0,
                        hazardAlert = false
                    )
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        ).addOnFailureListener { e ->
            Log.e(TAG, "Error iniciando actualizaciones de GPS", e)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun saveTelemetryToFirestore(telemetry: Telemetry) {
        if (telemetry.userId.isBlank()) return
        db.collection("telemetry")
            .document(telemetry.userId)
            .set(telemetry)
            .addOnFailureListener { e ->
                Log.e(TAG, "Error actualizando telemetría en Firestore", e)
            }
    }
}
