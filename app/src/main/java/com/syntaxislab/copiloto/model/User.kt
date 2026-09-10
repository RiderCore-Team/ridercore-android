package com.syntaxislab.copiloto.model

import com.google.firebase.Timestamp

data class User(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val emergencyPhone: String = "",
    val bloodType: String = "",
    val bikeModel: String = "",
    val createdAt: Timestamp? = null,
    val lastLogin: Timestamp? = null
)