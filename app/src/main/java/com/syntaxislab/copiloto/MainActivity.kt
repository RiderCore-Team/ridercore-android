package com.syntaxislab.copiloto

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.syntaxislab.copiloto.screens.AuthViewModel
import com.syntaxislab.copiloto.screens.LoginScreen
import com.syntaxislab.copiloto.screens.LoginState
import com.syntaxislab.copiloto.ui.theme.CopilotoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CopilotoTheme {
                CopilotoApp()
            }
        }
    }
}

@Composable
fun CopilotoApp(authViewModel: AuthViewModel = viewModel()) {
    val context = LocalContext.current
    val loginState by authViewModel.loginState.collectAsState()

    // Configuración del cliente de Google Sign-In
    // Firebase reconoce R.string.default_web_client_id gracias a tu google-services.json
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(stringResource(R.string.default_web_client_id))
        .requestEmail()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    // Lanzador para atrapar el resultado de la ventana de selección de cuenta
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { token ->
                authViewModel.signInWithGoogleToken(token)
            }
        } catch (e: ApiException) {
            Toast.makeText(context, "Error de Google Sign-In: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Observamos en qué estado se encuentra la autenticación
    when (loginState) {
        is LoginState.Idle -> {
            LoginScreen(
                onGoogleSignInClick = {
                    launcher.launch(googleSignInClient.signInIntent)
                }
            )
        }
        is LoginState.Loading -> {
            // Mantiene la pantalla visible pero deshabilitamos el click temporalmente
            LoginScreen(onGoogleSignInClick = {})
        }
        is LoginState.Success -> {
            Toast.makeText(context, "¡Piloto Autenticado!", Toast.LENGTH_SHORT).show()
            // TODO: En el futuro, aquí pondremos la navegación al mapa
        }
        is LoginState.Error -> {
            val errorMessage = (loginState as LoginState.Error).message
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()

            // Volvemos a mostrar la pantalla por si el piloto quiere volver a intentar
            LoginScreen(
                onGoogleSignInClick = {
                    launcher.launch(googleSignInClient.signInIntent)
                }
            )
        }
    }
}