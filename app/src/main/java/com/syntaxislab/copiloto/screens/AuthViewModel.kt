package com.syntaxislab.copiloto.screens

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.syntaxislab.copiloto.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Definimos los posibles estados de la pantalla
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    // Esta función recibe el Token que nos da Google al seleccionar la cuenta
    fun signInWithGoogleToken(idToken: String) {
        _loginState.value = LoginState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    saveUserToFirestore(firebaseUser)
                }
            }
            .addOnFailureListener { e ->
                _loginState.value = LoginState.Error(e.message ?: "Error al conectar con Firebase Auth")
            }
    }

    // Comprueba si el usuario ya existe, lo crea o actualiza su última sesión
    private fun saveUserToFirestore(firebaseUser: FirebaseUser) {
        val userRef = db.collection("users").document(firebaseUser.uid)

        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                // Es un piloto nuevo, usamos la Data Class que creaste en 'model'
                val newUser = User(
                    userId = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    createdAt = Timestamp.now(),
                    lastLogin = Timestamp.now()
                )

                userRef.set(newUser)
                    .addOnSuccessListener { _loginState.value = LoginState.Success }
                    .addOnFailureListener { e -> _loginState.value = LoginState.Error(e.message ?: "Error al crear perfil en base de datos") }
            } else {
                // El piloto ya estaba registrado, solo actualizamos su último login
                userRef.update("lastLogin", Timestamp.now())
                    .addOnSuccessListener { _loginState.value = LoginState.Success }
                    .addOnFailureListener { e -> _loginState.value = LoginState.Error(e.message ?: "Error al actualizar sesión") }
            }
        }.addOnFailureListener { e ->
            _loginState.value = LoginState.Error(e.message ?: "Error al consultar Firestore")
        }
    }
}