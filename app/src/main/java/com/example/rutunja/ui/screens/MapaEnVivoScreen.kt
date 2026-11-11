package com.example.rutunja.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.rutunja.model.PosicionBus
import com.example.rutunja.ui.RuTunjaViewModel
import com.example.rutunja.util.MarkerBitmapHelper
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/**
 * Convierte un código Hexadecimal en Color Int con canal Alpha.
 */
fun parseHexColorInt(hex: String, alphaFraction: Float = 0.88f): Int {
    return try {
        val colorInt = android.graphics.Color.parseColor(hex)
        val alphaInt = (alphaFraction * 255).toInt().coerceIn(0, 255)
        (colorInt and 0x00FFFFFF) or (alphaInt shl 24)
    } catch (_: Exception) {
        android.graphics.Color.RED
    }
}

/**
 * Convierte un código Hexadecimal en Color de Jetpack Compose.
 */
fun parseHexColor(hex: String): Color {
    return Color(parseHexColorInt(hex, 1.0f))
}

/**
 * Pantalla 1: Mapa en Vivo con trazo único limpio (4.5f), estilo fiord nocturno y OSRM por paraderos clave.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaEnVivoScreen(viewModel: RuTunjaViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    remember { MapLibre.getInstance(context) }

    val modoOscuro by viewModel.modoOscuroActivo.collectAsState()
    val buses by viewModel.busesFiltrados.collectAsState()
    val rutas by viewModel.rutasFiltradas.collectAsState()
    val geometriasViales by viewModel.geometriasVialesRutas.collectAsState()

    val paraderos = viewModel.paraderos
    val empresas = viewModel.empresas

    val empresaFiltroId by viewModel.empresaFiltroId.collectAsState()
    val rutaFiltroId by viewModel.rutaFiltroId.collectAsState()

    val paraderoSeleccionadoId by viewModel.paraderoSeleccionadoId.collectAsState()
    val paraderoMonitoreadoId by viewModel.paraderoMonitoreadoId.collectAsState()

    val busMasCercano by viewModel.busMasCercanoAlUsuario.collectAsState()
    val enfocarSoloMiBus by viewModel.enfocarSoloMiBus.collectAsState()

    val paraderoSeleccionado = paraderos.find { it.stopId == paraderoSeleccionadoId }

    var filtrosExpandidos by remember { mutableStateOf(false) }
    var panelInferiorExpandido by remember { mutableStateOf(false) }

    val mapView = remember { MapView(context) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var isStyleLoaded by remember { mutableStateOf(false) }

    // Referencias en memoria para mutating a 60 FPS sin llamar a map.clear() en los GPS Ticks
    val busMarkersMap = remember { mutableMapOf<String, Marker>() }
    val stopMarkersMap = remember { mutableMapOf<String, Marker>() }
    val polylinesList = remember { mutableListOf<Polyline>() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 1. Redibujar Polilínas Viales con Trazo Único Limpio (width = 4.5f, alpha = 0.88f) y Paraderos Fijos
    fun redibujarEstructuraBase(map: MapLibreMap) {
        if (!isStyleLoaded) return

        // Limpiar polilínas anteriores
        polylinesList.forEach { map.removePolyline(it) }
        polylinesList.clear()

        // Limpiar paraderos anteriores
        stopMarkersMap.values.forEach { map.removeMarker(it) }
        stopMarkersMap.clear()

        // Dibujar ÚNICAMENTE UNA (1) Polyline limpia por cada ruta seleccionada
        rutas.forEach { ruta ->
            val puntos = geometriasViales[ruta.routeId] ?: ruta.polilineaRuta
            val puntosLatLng = puntos.map { LatLng(it.latitud, it.longitud) }
            if (puntosLatLng.isNotEmpty()) {
                val poly = map.addPolyline(
                    PolylineOptions()
                        .addAll(puntosLatLng)
                        .color(parseHexColorInt(ruta.empresa.colorHex, 0.88f))
                        .width(4.5f)
                )
                polylinesList.add(poly)
            }
        }

        // Dibujar Paraderos TPCU
        paraderos.forEach { paradero ->
            val esSeleccionado = paradero.stopId == paraderoSeleccionadoId
            val esMonitoreado = paradero.stopId == paraderoMonitoreadoId
            val iconParadero = MarkerBitmapHelper.obtenerIconoParadero(context, esMonitoreado, esSeleccionado)

            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(paradero.posicion.latitud, paradero.posicion.longitud))
                    .title(paradero.nombre)
                    .snippet(paradero.stopId)
                    .icon(iconParadero)
            )
            stopMarkersMap[paradero.stopId] = marker
        }
    }

    // 2. Actualizar Posición de Buses a 60 FPS mutating marcadores existentes (SIN map.clear())
    fun actualizarPosicionesBuses60FPS(map: MapLibreMap) {
        if (!isStyleLoaded) return

        val idsBusesActuales = buses.map { it.vehicleId }.toSet()

        // Eliminar marcadores de buses que ya no estén en los filtros
        val iter = busMarkersMap.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            if (!idsBusesActuales.contains(entry.key)) {
                map.removeMarker(entry.value)
                iter.remove()
            }
        }

        // Mutar o agregar marcadores para los buses filtrados
        buses.forEach { bus ->
            val esMiBus = bus.vehicleId == busMasCercano?.vehicleId && enfocarSoloMiBus
            val codigoRutaCagula = when {
                bus.routeId.contains("R01") || bus.routeId.contains("R1") -> "R01"
                bus.routeId.contains("R02") || bus.routeId.contains("R2") -> "R02"
                bus.routeId.contains("R03") || bus.routeId.contains("R3") -> "R03"
                bus.routeId.contains("R04") || bus.routeId.contains("R4") -> "R04"
                else -> "R05"
            }

            val iconBus = MarkerBitmapHelper.obtenerIconoBusDireccional(
                context = context,
                codigoRuta = codigoRutaCagula,
                colorHex = bus.empresa.colorHex,
                rumboGrados = bus.rumboGrados,
                esMiBusAbordar = esMiBus
            )

            val nuevaPosicion = LatLng(bus.posicionActual.latitud, bus.posicionActual.longitud)
            val snippetText = "Velocidad: ${bus.velocidadKmh.toInt()} km/h | ETA: ${bus.etaMinutos} min"

            val markerExistente = busMarkersMap[bus.vehicleId]
            if (markerExistente != null) {
                // Mutación fluida de posición e ícono a 60 FPS
                markerExistente.position = nuevaPosicion
                markerExistente.icon = iconBus
                markerExistente.title = if (esMiBus) "🌟 BUS A ABORDAR (${bus.placa})" else "Bus ${bus.placa} (${bus.empresa.nombre})"
                markerExistente.snippet = snippetText
            } else {
                val nuevoMarker = map.addMarker(
                    MarkerOptions()
                        .position(nuevaPosicion)
                        .title(if (esMiBus) "🌟 BUS A ABORDAR (${bus.placa})" else "Bus ${bus.placa} (${bus.empresa.nombre})")
                        .snippet(snippetText)
                        .icon(iconBus)
                )
                busMarkersMap[bus.vehicleId] = nuevoMarker
            }
        }
    }

    // Estilo nocturno azulado fiord para alto contraste en modo oscuro
    val styleUrl = if (modoOscuro) "https://tiles.openfreemap.org/styles/fiord" else "https://tiles.openfreemap.org/styles/liberty"

    // Cambiar estilo de mapa dinámicamente según Modo Oscuro
    LaunchedEffect(modoOscuro) {
        mapLibreMap?.let { map ->
            isStyleLoaded = false
            map.setStyle(styleUrl) { _ ->
                isStyleLoaded = true
                redibujarEstructuraBase(map)
                actualizarPosicionesBuses60FPS(map)
            }
        }
    }

    // Actualizar estructura base de paraderos y polilínas tan pronto como OSRM entregue los puntos
    LaunchedEffect(isStyleLoaded, rutas, geometriasViales, paraderoSeleccionadoId, paraderoMonitoreadoId) {
        mapLibreMap?.let { map ->
            if (isStyleLoaded) {
                redibujarEstructuraBase(map)
            }
        }
    }

    // Actualizar posición de buses continuamente a 60 FPS
    LaunchedEffect(isStyleLoaded, buses, busMasCercano, enfocarSoloMiBus) {
        mapLibreMap?.let { map ->
            if (isStyleLoaded) {
                actualizarPosicionesBuses60FPS(map)
            }
        }
    }

    val nombreEmpresaActiva = empresas.find { it.id == empresaFiltroId }?.nombre ?: "Todas las Empresas"
    val codigoRutaActiva = viewModel.rutas.collectAsState().value.find { it.routeId == rutaFiltroId }?.codigoRuta?.split("-")?.firstOrNull()?.trim() ?: "Todas las Rutas"

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        mapLibreMap = map

                        // Habilitar y reposicionar la brújula debajo de la tarjeta de filtros
                        val density = context.resources.displayMetrics.density
                        map.uiSettings.isCompassEnabled = true
                        map.uiSettings.setCompassMargins(0, (130 * density).toInt(), (16 * density).toInt(), 0)

                        map.setStyle(styleUrl) { _ ->
                            val centroTunja = LatLng(5.5390, -73.3560)
                            map.cameraPosition = CameraPosition.Builder()
                                .target(centroTunja)
                                .zoom(13.5)
                                .build()

                            isStyleLoaded = true
                            redibujarEstructuraBase(map)
                            actualizarPosicionesBuses60FPS(map)
                        }

                        map.setOnMarkerClickListener { marker ->
                            val stopEncontrado = paraderos.find {
                                it.nombre == marker.title || it.stopId == marker.snippet
                            }
                            if (stopEncontrado != null) {
                                viewModel.seleccionarParadero(stopEncontrado.stopId)
                                panelInferiorExpandido = true
                            }
                            false
                        }

                        map.addOnMapClickListener {
                            panelInferiorExpandido = false
                            filtrosExpandidos = false
                            true
                        }
                    }
                }
            },
            update = { _ ->
                mapLibreMap?.let { map ->
                    if (isStyleLoaded) {
                        actualizarPosicionesBuses60FPS(map)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Panel Superior Colapsable de Filtros
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .align(Alignment.TopCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { filtrosExpandidos = !filtrosExpandidos }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Filtros TPCU: $nombreEmpresaActiva • $codigoRutaActiva",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Icon(
                            imageVector = if (filtrosExpandidos) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expandir Filtros",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "🚌 Bus en vivo   |   🚏 Paradero TPCU",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = filtrosExpandidos,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = "Filtrar por Empresa Operadora (5):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = empresaFiltroId == null,
                                    onClick = { viewModel.setFiltroEmpresa(null) },
                                    label = { Text("Todas") }
                                )

                                empresas.forEach { emp ->
                                    FilterChip(
                                        selected = empresaFiltroId == emp.id,
                                        onClick = { viewModel.setFiltroEmpresa(emp.id) },
                                        label = { Text(emp.nombre) },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(parseHexColor(emp.colorHex), CircleShape)
                                            )
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Filtrar por Ruta Piloto (5):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = rutaFiltroId == null,
                                    onClick = { viewModel.setFiltroRuta(null) },
                                    label = { Text("Todas") }
                                )

                                viewModel.rutas.collectAsState().value.forEach { ruta ->
                                    FilterChip(
                                        selected = rutaFiltroId == ruta.routeId,
                                        onClick = { viewModel.setFiltroRuta(ruta.routeId) },
                                        label = { Text(ruta.codigoRuta.split("-").first().trim()) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Botón Flotante "🎯 Mi Bus Más Cercano"
        busMasCercano?.let { miBus ->
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.setEnfocarSoloMiBus(!enfocarSoloMiBus)
                    mapLibreMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(miBus.posicionActual.latitud, miBus.posicionActual.longitud),
                            15.5
                        )
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700)
                    )
                },
                text = {
                    Text(
                        text = if (enfocarSoloMiBus) "Siguiendo mi bus (ETA: ${miBus.etaMinutos} min)"
                        else "🎯 Mi Bus Más Cercano (${miBus.etaMinutos} min)",
                        fontWeight = FontWeight.Bold
                    )
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = if (paraderoSeleccionado != null) 180.dp else 24.dp)
            )
        }

        // Panel Inferior del Paradero Seleccionado
        paraderoSeleccionado?.let { paradero ->
            val esMonitoreado = paradero.stopId == paraderoMonitoreadoId
            val busesOrdenados = buses.sortedBy { it.distanciaAlParaderoMetros }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Sección Destacada "Próximo Bus para Subirte"
                        busMasCercano?.let { miBus ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFD50000),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Próximo bus para subirte",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "${miBus.empresa.nombre} (${miBus.placa}) • A ${miBus.distanciaAlParaderoMetros}m",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "ETA ${miBus.etaMinutos}m",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Estado Minimizado
                        if (!panelInferiorExpandido) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { panelInferiorExpandido = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = paradero.nombre,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = paradero.direccion,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = "Ver buses ▲",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                IconButton(
                                    onClick = { viewModel.seleccionarParadero(null) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cerrar Panel",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            // Estado Expandido
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = paradero.nombre,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = paradero.direccion,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { panelInferiorExpandido = false },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = "Minimizar"
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.seleccionarParadero(null) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cerrar"
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { viewModel.fijarParaderoMonitoreado(paradero.stopId) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (esMonitoreado) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (esMonitoreado) "Monitoreando (Geocerca 300m Activa)"
                                    else "Monitorear Paradero (Geocerca 300m)"
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Buses en Camino hacia este paradero:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            busesOrdenados.take(3).forEach { bus ->
                                ItemBusEnCamino(bus)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemBusEnCamino(bus: PosicionBus) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(parseHexColor(bus.empresa.colorHex), CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${bus.empresa.nombre} (${bus.placa})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "A ${bus.distanciaAlParaderoMetros} metros de distancia",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "ETA: ${bus.etaMinutos} min",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
