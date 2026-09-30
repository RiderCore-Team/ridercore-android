package com.syntaxislab.copiloto.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.syntaxislab.copiloto.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class RequiresProfileSetup(val user: User) : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    companion object {
        private const val TAG = "AuthViewModel"
    }

    init {
        val existingUser = auth.currentUser
        if (existingUser != null) {
            fetchUserProfile(existingUser.uid)
        }
    }

    fun signInWithGoogleToken(idToken: String) {
        _loginState.value = LoginState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    saveUserToFirestore(firebaseUser)
                } else {
                    _loginState.value = LoginState.Error("No se pudo obtener información del usuario de Google")
                }
            }
            .addOnFailureListener { e ->
                _loginState.value = LoginState.Error(e.message ?: "Error al conectar con Firebase Auth")
            }
    }

    private fun saveUserToFirestore(firebaseUser: FirebaseUser) {
        val userRef = db.collection("users").document(firebaseUser.uid)

        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val newUser = User(
                    userId = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    profileCompleted = false,
                    createdAt = Timestamp.now(),
                    lastLogin = Timestamp.now()
                )

                userRef.set(newUser)
                    .addOnSuccessListener {
                        _currentUser.value = newUser
                        _loginState.value = LoginState.RequiresProfileSetup(newUser)
                    }
                    .addOnFailureListener { e ->
                        _loginState.value = LoginState.Error(e.message ?: "Error al crear perfil en base de datos")
                    }
            } else {
                val user = document.toObject(User::class.java)
                val updatedUser = user?.copy(lastLogin = Timestamp.now()) ?: User(
                    userId = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: ""
                )

                userRef.update("lastLogin", Timestamp.now())
                    .addOnSuccessListener {
                        _currentUser.value = updatedUser
                        if (updatedUser.profileCompleted && updatedUser.emergencyPhone.isNotBlank()) {
                            _loginState.value = LoginState.Success(updatedUser)
                        } else {
                            _loginState.value = LoginState.RequiresProfileSetup(updatedUser)
                        }
                    }
                    .addOnFailureListener { e ->
                        _loginState.value = LoginState.Error(e.message ?: "Error al actualizar sesión")
                    }
            }
        }.addOnFailureListener { e ->
            _loginState.value = LoginState.Error(e.message ?: "Error al consultar Firestore")
        }
    }

    fun fetchUserProfile(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        _currentUser.value = user
                        if (user.profileCompleted && user.emergencyPhone.isNotBlank()) {
                            _loginState.value = LoginState.Success(user)
                        } else {
                            _loginState.value = LoginState.RequiresProfileSetup(user)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error buscando perfil del usuario", e)
            }
    }

    fun saveProfileDetails(
        emergencyPhone: String,
        bloodType: String,
        bikeModel: String,
        onComplete: (Boolean) -> Unit
    ) {
        val firebaseUser = auth.currentUser
        val user = _currentUser.value ?: if (firebaseUser != null) {
            User(userId = firebaseUser.uid, email = firebaseUser.email ?: "", displayName = firebaseUser.displayName ?: "")
        } else {
            onComplete(false)
            return
        }

        val updatedUser = user.copy(
            emergencyPhone = emergencyPhone,
            bloodType = bloodType,
            bikeModel = bikeModel,
            profileCompleted = true
        )

        db.collection("users").document(user.userId)
            .set(updatedUser)
            .addOnSuccessListener {
                Log.i(TAG, "Perfil guardado con éxito en Firestore")
                _currentUser.value = updatedUser
                _loginState.value = LoginState.Success(updatedUser)
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error guardando perfil en Firestore", e)
                onComplete(false)
            }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        _loginState.value = LoginState.Idle
    }
}
