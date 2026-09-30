package com.syntaxislab.copiloto.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.syntaxislab.copiloto.model.Telemetry
import com.syntaxislab.copiloto.model.User
import com.syntaxislab.copiloto.navigation.BottomTab
import com.syntaxislab.copiloto.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    user: User,
    authViewModel: AuthViewModel
) {
    var selectedTab by remember { mutableStateOf<BottomTab>(BottomTab.Map) }
    var isSosActiveByFirestore by remember { mutableStateOf(false) }

    DisposableEffect(user.userId) {
        if (user.userId.isBlank()) return@DisposableEffect onDispose {}
        val db = FirebaseFirestore.getInstance()
        val listener = db.collection("telemetry").document(user.userId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val telemetry = snapshot.toObject(Telemetry::class.java)
                    isSosActiveByFirestore = telemetry?.isSosActive == true
                }
            }
        onDispose {
            listener.remove()
        }
    }

    val tabs = listOf(
        BottomTab.Map,
        BottomTab.Squad,
        BottomTab.Watch,
        BottomTab.Settings
    )

    Box {
        Scaffold(
            containerColor = NegroBase,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(NegroSuperficie),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = null,
                                    tint = NaranjaFuego,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Co-piloto",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlancoTexto
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AmarilloVisibilidad
                            ) {
                                Text(
                                    text = "RIDER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NegroBase,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NegroBase,
                        titleContentColor = BlancoTexto
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = NegroSuperficie,
                    contentColor = BlancoTexto,
                    tonalElevation = 10.dp
                ) {
                    tabs.forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) NaranjaFuego else GrisTextoSecundario,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) AmarilloVisibilidad else GrisTextoSecundario
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = NegroElevado
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    is BottomTab.Map -> MapScreen(user = user)
                    is BottomTab.Squad -> SquadScreen(user = user)
                    is BottomTab.Watch -> WatchConnectionScreen(user = user)
                    is BottomTab.Settings -> ProfileSetupScreen(
                        user = user,
                        onProfileSaved = { selectedTab = BottomTab.Map },
                        authViewModel = authViewModel
                    )
                }
            }
        }

        // Pantalla Emergente Modal de Emergencia SOS
        AnimatedVisibility(
            visible = isSosActiveByFirestore,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SosCountdownPopup(
                user = user,
                onDismiss = {
                    isSosActiveByFirestore = false
                }
            )
        }
    }
}
