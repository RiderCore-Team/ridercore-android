package com.syntaxislab.copiloto.presentation.model

data class Squad(
    val squadId: String = "",
    val hostId: String = "",
    val status: String = "pending", // Puede ser "pending", "active", o "finished"
    val destinationLat: Double = 0.0,
    val destinationLng: Double = 0.0,
    val activeMembers: List<String> = emptyList()
)