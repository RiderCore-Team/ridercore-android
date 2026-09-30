package com.syntaxislab.copiloto.screens

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.syntaxislab.copiloto.model.Squad
import com.syntaxislab.copiloto.model.Telemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class SquadViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _activeSquad = MutableStateFlow<Squad?>(null)
    val activeSquad: StateFlow<Squad?> = _activeSquad

    private val _membersTelemetry = MutableStateFlow<List<Telemetry>>(emptyList())
    val membersTelemetry: StateFlow<List<Telemetry>> = _membersTelemetry

    companion object {
        private const val TAG = "SquadViewModel"
    }

    fun createSquad(hostUserId: String, onComplete: (String?) -> Unit) {
        val squadCode = UUID.randomUUID().toString().take(6).uppercase()
        val newSquad = Squad(
            squadId = squadCode,
            hostId = hostUserId,
            status = "active",
            activeMembers = listOf(hostUserId)
        )

        db.collection("squads").document(squadCode)
            .set(newSquad)
            .addOnSuccessListener {
                _activeSquad.value = newSquad
                updateCurrentSquadInTelemetry(hostUserId, squadCode)
                listenToSquad(squadCode)
                onComplete(squadCode)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al crear grupo", e)
                onComplete(null)
            }
    }

    fun joinSquad(squadCode: String, userId: String, onComplete: (Boolean, String) -> Unit) {
        val cleanCode = squadCode.trim().uppercase()
        val squadRef = db.collection("squads").document(cleanCode)

        squadRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                squadRef.update("activeMembers", FieldValue.arrayUnion(userId))
                    .addOnSuccessListener {
                        updateCurrentSquadInTelemetry(userId, cleanCode)
                        listenToSquad(cleanCode)
                        onComplete(true, "Unido con éxito al grupo $cleanCode")
                    }
                    .addOnFailureListener { e ->
                        onComplete(false, "Error al unirse al grupo: ${e.message}")
                    }
            } else {
                onComplete(false, "El código de grupo no existe")
            }
        }.addOnFailureListener { e ->
            onComplete(false, "Error al consultar grupo: ${e.message}")
        }
    }

    fun listenToSquad(squadId: String) {
        db.collection("squads").document(squadId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando grupo", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val squad = snapshot.toObject(Squad::class.java)
                    _activeSquad.value = squad
                    if (squad != null && squad.activeMembers.isNotEmpty()) {
                        listenToMembersTelemetry(squad.squadId, squad.activeMembers)
                    }
                }
            }
    }

    /**
     * Suscribirse (SnapshotListener) a la colección telemetry filtrando por los compañeros del mismo squadId
     */
    private fun listenToMembersTelemetry(squadId: String, memberIds: List<String>) {
        if (squadId.isBlank() && memberIds.isEmpty()) return

        db.collection("telemetry")
            .whereEqualTo("currentSquadId", squadId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando telemetría por squadId", error)
                    // Fallback a whereIn por IDs
                    listenToMembersByMemberIds(memberIds)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val telemetries = snapshot.documents.mapNotNull { it.toObject(Telemetry::class.java) }
                    _membersTelemetry.value = telemetries
                }
            }
    }

    private fun listenToMembersByMemberIds(memberIds: List<String>) {
        if (memberIds.isEmpty()) return
        db.collection("telemetry")
            .whereIn("userId", memberIds.take(10))
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val telemetries = snapshot.documents.mapNotNull { it.toObject(Telemetry::class.java) }
                    _membersTelemetry.value = telemetries
                }
            }
    }

    fun leaveSquad(userId: String) {
        val squad = _activeSquad.value ?: return
        db.collection("squads").document(squad.squadId)
            .update("activeMembers", FieldValue.arrayRemove(userId))
            .addOnSuccessListener {
                updateCurrentSquadInTelemetry(userId, null)
                _activeSquad.value = null
                _membersTelemetry.value = emptyList()
            }
    }

    private fun updateCurrentSquadInTelemetry(userId: String, squadId: String?) {
        if (userId.isBlank()) return
        db.collection("telemetry").document(userId)
            .update("currentSquadId", squadId)
            .addOnFailureListener {
                db.collection("telemetry").document(userId)
                    .set(
                        mapOf("currentSquadId" to (squadId ?: ""), "userId" to userId),
                        SetOptions.merge()
                    )
            }
    }

    /**
     * Calcula la distancia en metros hacia el líder del grupo (Host)
     */
    fun getDistanceToLeaderMeters(currentLat: Double, currentLng: Double): Int {
        val squad = _activeSquad.value ?: return 0
        if (squad.hostId.isBlank()) return 0

        val leaderTelemetry = _membersTelemetry.value.find { it.userId == squad.hostId } ?: return 0
        if (leaderTelemetry.latitude == 0.0 || leaderTelemetry.longitude == 0.0) return 0

        val results = FloatArray(1)
        Location.distanceBetween(
            currentLat, currentLng,
            leaderTelemetry.latitude, leaderTelemetry.longitude,
            results
        )
        return results[0].toInt()
    }
}
