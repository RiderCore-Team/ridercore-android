package com.syntaxislab.copiloto.presentation.model

import com.google.firebase.Timestamp

data class Telemetry(
    val userId: String = "",
    val currentSquadId: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val heading: Double = 0.0,
    val speed: Double = 0.0,
    val isSosActive: Boolean = false,
    val isWatchConnected: Boolean = false,
    val lastUpdatedAt: Timestamp? = null
)