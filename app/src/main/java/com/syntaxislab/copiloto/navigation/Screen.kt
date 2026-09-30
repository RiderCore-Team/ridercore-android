package com.syntaxislab.copiloto.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Watch
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object ProfileSetup : Screen("profile_setup")
    object MainDashboard : Screen("main_dashboard")
}

sealed class BottomTab(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Map : BottomTab("map_tab", "Mapa", Icons.Default.Map)
    object Squad : BottomTab("squad_tab", "MotoSquad", Icons.Default.Group)
    object Watch : BottomTab("watch_tab", "Reloj", Icons.Default.Watch)
    object Settings : BottomTab("settings_tab", "Perfil & SOS", Icons.Default.Person)
}
