package com.example.rutunja.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rutunja.data.SimuladorTelemetriaRepository
import com.example.rutunja.model.AlertaGeocerca
import com.example.rutunja.model.Coordenada
import com.example.rutunja.model.EmpresaTransporte
import com.example.rutunja.model.Paradero
import com.example.rutunja.model.PosicionBus
import com.example.rutunja.model.RutaPiloto
import com.example.rutunja.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel principal para el MVP de RuTunja.
 */
class RuTunjaViewModel(
    val repository: SimuladorTelemetriaRepository = SimuladorTelemetriaRepository()
) : ViewModel() {

    // --- Modo Oscuro Completo ---
    private val _modoOscuroActivo = MutableStateFlow(false)
    val modoOscuroActivo: StateFlow<Boolean> = _modoOscuroActivo.asStateFlow()

    // --- Ubicación Simulada del Estudiante / Usuario en Tunja ---
    private val _ubicacionUsuario = MutableStateFlow(Coordenada(5.54840, -73.34950)) // Cerca a Uniboyacá
    val ubicacionUsuario: StateFlow<Coordenada> = _ubicacionUsuario.asStateFlow()

    // --- Estado Enfocar Mi Bus ---
    private val _enfocarSoloMiBus = MutableStateFlow(false)
    val enfocarSoloMiBus: StateFlow<Boolean> = _enfocarSoloMiBus.asStateFlow()

    // --- Listas Catálogo ---
    val empresas: List<EmpresaTransporte> = repository.empresasLista
    val paraderos: List<Paradero> = repository.paraderosLista

    // --- Estado de Filtros ---
    private val _empresaFiltroId = MutableStateFlow<String?>(null)
    val empresaFiltroId: StateFlow<String?> = _empresaFiltroId.asStateFlow()

    private val _rutaFiltroId = MutableStateFlow<String?>(null)
    val rutaFiltroId: StateFlow<String?> = _rutaFiltroId.asStateFlow()

    // --- Estado Paradero Seleccionado ---
    private val _paraderoSeleccionadoId = MutableStateFlow<String?>(repository.paraderoUniboyacaR1.stopId)
    val paraderoSeleccionadoId: StateFlow<String?> = _paraderoSeleccionadoId.asStateFlow()

    // --- Flows del Repositorio ---
    val buses: StateFlow<List<PosicionBus>> = repository.busesFlow
    val rutas: StateFlow<List<RutaPiloto>> = repository.rutasFlow
    val geometriasVialesRutas: StateFlow<Map<String, List<Coordenada>>> = repository.geometriasVialesRutas
    val alertasHistorial: StateFlow<List<AlertaGeocerca>> = repository.alertasFlow
    val modoDemoRapido: StateFlow<Boolean> = repository.modoDemoRapido
    val radioGeocercaMetros: StateFlow<Int> = repository.radioGeocercaMetros
    val paraderoMonitoreadoId: StateFlow<String?> = repository.paraderoMonitoreadoId

    // --- Buses Filtrados Dinámicamente ---
    val busesFiltrados: StateFlow<List<PosicionBus>> = combine(
        buses,
        _empresaFiltroId,
        _rutaFiltroId
    ) { listaBuses, empId, rutId ->
        listaBuses.filter { bus ->
            (empId == null || bus.empresa.id == empId) &&
            (rutId == null || bus.routeId == rutId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Bus Óptimo Más Cercano al Usuario para Abordar ---
    val busMasCercanoAlUsuario: StateFlow<PosicionBus?> = combine(
        busesFiltrados,
        _ubicacionUsuario
    ) { listaBuses, coordsUser ->
        listaBuses.minByOrNull { bus ->
            SimuladorTelemetriaRepository.calcularDistanciaMetros(bus.posicionActual, coordsUser)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Rutas Filtradas Dinámicamente ---
    val rutasFiltradas: StateFlow<List<RutaPiloto>> = combine(
        rutas,
        _empresaFiltroId,
        _rutaFiltroId
    ) { listaRutas, empId, rutId ->
        listaRutas.filter { ruta ->
            (empId == null || ruta.empresa.id == empId) &&
            (rutId == null || ruta.routeId == rutId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var appContext: Context? = null

    fun inicializarNotificaciones(context: Context) {
        this.appContext = context.applicationContext
        NotificationHelper.crearCanalNotificaciones(context)

        repository.listenerAlertaProximidad = { titulo, mensaje ->
            appContext?.let { ctx ->
                NotificationHelper.lanzarNotificacion(ctx, titulo, mensaje)
            }
        }
    }

    fun toggleModoOscuro() {
        _modoOscuroActivo.value = !_modoOscuroActivo.value
    }

    fun setEnfocarSoloMiBus(enfocar: Boolean) {
        _enfocarSoloMiBus.value = enfocar
    }

    fun cambiarUbicacionUsuario(nuevaCoordenada: Coordenada) {
        _ubicacionUsuario.value = nuevaCoordenada
    }

    fun seleccionarParadero(stopId: String?) {
        _paraderoSeleccionadoId.value = stopId
    }

    fun fijarParaderoMonitoreado(stopId: String?) {
        repository.setParaderoMonitoreado(stopId)
        _paraderoSeleccionadoId.value = stopId
    }

    fun setFiltroEmpresa(empresaId: String?) {
        _empresaFiltroId.value = if (_empresaFiltroId.value == empresaId) null else empresaId
    }

    fun setFiltroRuta(rutaId: String?) {
        _rutaFiltroId.value = if (_rutaFiltroId.value == rutaId) null else rutaId
    }

    fun toggleModoDemoRapido() {
        repository.setModoDemoRapido(!modoDemoRapido.value)
    }

    fun setRadioGeocerca(radioMetros: Int) {
        repository.setRadioGeocerca(radioMetros)
    }

    fun probarNotificacionPush(context: Context) {
        NotificationHelper.lanzarNotificacion(
            context = context,
            titulo = "¡Prueba Alerta Proximidad RuTunja!",
            mensaje = "Geocerca activa: Demostración exitosa para la red de transporte colectivo urbano de Tunja."
        )
    }
}
