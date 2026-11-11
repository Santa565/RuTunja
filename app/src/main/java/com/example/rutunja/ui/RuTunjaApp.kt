package com.example.rutunja.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.rutunja.ui.screens.AlertasGeocercasScreen
import com.example.rutunja.ui.screens.MapaEnVivoScreen
import com.example.rutunja.ui.screens.RutasParaderosScreen
import com.example.rutunja.ui.theme.RuTunjaTheme

sealed class PantallaDestino(
    val ruta: String,
    val titulo: String,
    val icono: ImageVector
) {
    object Mapa : PantallaDestino("mapa", "Mapa en Vivo", Icons.Default.Map)
    object Rutas : PantallaDestino("rutas", "Rutas y Paraderos", Icons.Default.DirectionsBus)
    object Alertas : PantallaDestino("alertas", "Alertas y Geocercas", Icons.Default.Notifications)
}

val itemsNavegacion = listOf(
    PantallaDestino.Mapa,
    PantallaDestino.Rutas,
    PantallaDestino.Alertas
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuTunjaApp(
    viewModel: RuTunjaViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val modoOscuro by viewModel.modoOscuroActivo.collectAsState()

    RuTunjaTheme(darkTheme = modoOscuro) {
        // Inicializar canal y listener de notificaciones
        LaunchedEffect(Unit) {
            viewModel.inicializarNotificaciones(context)
        }

        // Solicitud de Permisos en Tiempo de Ejecución
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { _ -> }

        LaunchedEffect(Unit) {
            val permisosARequerir = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permisosARequerir.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            val permisosFaltantes = permisosARequerir.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }

            if (permisosFaltantes.isNotEmpty()) {
                permissionLauncher.launch(permisosFaltantes.toTypedArray())
            }
        }

        val backStackEntry by navController.currentBackStackEntryAsState()
        val rutaActual = backStackEntry?.destination?.route

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "RuTunja (TPCU Tunja)",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleModoOscuro() }) {
                            Icon(
                                imageVector = if (modoOscuro) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Modo Oscuro/Claro"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors()
                )
            },
            bottomBar = {
                NavigationBar {
                    itemsNavegacion.forEach { destino ->
                        val estaSeleccionado = rutaActual == destino.ruta
                        NavigationBarItem(
                            selected = estaSeleccionado,
                            onClick = {
                                if (rutaActual != destino.ruta) {
                                    navController.navigate(destino.ruta) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destino.icono,
                                    contentDescription = destino.titulo
                                )
                            },
                            label = { Text(destino.titulo) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = PantallaDestino.Mapa.ruta,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(PantallaDestino.Mapa.ruta) {
                    MapaEnVivoScreen(viewModel = viewModel)
                }
                composable(PantallaDestino.Rutas.ruta) {
                    RutasParaderosScreen(viewModel = viewModel)
                }
                composable(PantallaDestino.Alertas.ruta) {
                    AlertasGeocercasScreen(viewModel = viewModel)
                }
            }
        }
    }
}
