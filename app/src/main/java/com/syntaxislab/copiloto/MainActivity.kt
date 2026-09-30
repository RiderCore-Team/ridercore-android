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
import com.syntaxislab.copiloto.screens.MainDashboardScreen
import com.syntaxislab.copiloto.screens.ProfileSetupScreen
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

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(stringResource(R.string.default_web_client_id))
        .requestEmail()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context, gso)

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

    when (val state = loginState) {
        is LoginState.Idle -> {
            LoginScreen(
                onGoogleSignInClick = {
                    launcher.launch(googleSignInClient.signInIntent)
                }
            )
        }
        is LoginState.Loading -> {
            LoginScreen(onGoogleSignInClick = {})
        }
        is LoginState.RequiresProfileSetup -> {
            ProfileSetupScreen(
                user = state.user,
                onProfileSaved = {
                    // El estado ya cambia automáticamente a LoginState.Success
                },
                authViewModel = authViewModel
            )
        }
        is LoginState.Success -> {
            MainDashboardScreen(
                user = state.user,
                authViewModel = authViewModel
            )
        }
        is LoginState.Error -> {
            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            LoginScreen(
                onGoogleSignInClick = {
                    launcher.launch(googleSignInClient.signInIntent)
                }
            )
        }
    }
}
