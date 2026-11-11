package com.example.rutunja.data

import com.example.rutunja.model.AlertaGeocerca
import com.example.rutunja.model.Coordenada
import com.example.rutunja.model.EmpresaTransporte
import com.example.rutunja.model.Paradero
import com.example.rutunja.model.PosicionBus
import com.example.rutunja.model.RutaPiloto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Repositorio del simulador de telemetría de buses TPCU Tunja.
 * Unifica 5 empresas y 5 rutas reales de busetas con paraderos clave.
 */
class SimuladorTelemetriaRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    // --- Catálogo de 5 Empresas Operadoras de Tunja ---
    val empresaCootranscol = EmpresaTransporte("EMP01", "Cootranscol / Muiscas", "#E53935") // Rojo
    val empresaAutoboy = EmpresaTransporte("EMP02", "Autoboy S.A.", "#1E88E5")            // Azul
    val empresaCooservicios = EmpresaTransporte("EMP03", "Cooservicios", "#2E7D32")        // Verde
    val empresaTransTunjano = EmpresaTransporte("EMP04", "Cootax TPCU", "#FB8C00")        // Naranja
    val empresaMuiscas = EmpresaTransporte("EMP05", "Muiscas / Cootranscol", "#8E24AA")   // Morado

    val empresasLista = listOf(
        empresaCootranscol,
        empresaAutoboy,
        empresaCooservicios,
        empresaTransTunjano,
        empresaMuiscas
    )

    // --- Paraderos Clave GTFS RUTA R01 ---
    val paraderoMuiscasR1 = Paradero("PAR_R1_01", "Barrio Los Muiscas", Coordenada(5.55825, -73.35192), "Av. Universitaria Norte")
    val paraderoUniboyacaR1 = Paradero("PAR_R1_02", "Universidad de Boyacá", Coordenada(5.54842, -73.34952), "Av. Universitaria")
    val paraderoUnicentroR1 = Paradero("PAR_R1_03", "C.C. Unicentro", Coordenada(5.54418, -73.34875), "Av. Universitaria")
    val paraderoGlorietaR1 = Paradero("PAR_R1_04", "Glorieta Norte", Coordenada(5.54465, -73.35615), "Glorieta Norte")
    val paraderoMakroR1 = Paradero("PAR_R1_05", "Av. Oriental - Makro", Coordenada(5.54162, -73.35135), "Av. Oriental")
    val paraderoMedilaserR1 = Paradero("PAR_R1_06", "Clínica Mediláser", Coordenada(5.53185, -73.35505), "Av. Oriental / Calle 22")
    val paraderoTerminalR1 = Paradero("PAR_R1_07", "Nuevo Terminal de Transportes", Coordenada(5.52520, -73.35762), "Terminal Juana Velasco")
    val paraderoCiudadJardinR1 = Paradero("PAR_R1_08", "Ciudad Jardín / Salida Sur", Coordenada(5.51520, -73.36425), "Salida Sur")

    // --- Paraderos Clave GTFS RUTA R02 ---
    val paraderoLimbaniaR2 = Paradero("PAR_R2_01", "Doña Limbania / Norte", Coordenada(5.56445, -73.35322), "Av. Central del Norte")
    val paraderoSantaInesR2 = Paradero("PAR_R2_02", "Barrio Santa Inés", Coordenada(5.55885, -73.35472), "Av. Central del Norte")
    val paraderoUPTCR2 = Paradero("PAR_R2_03", "UPTC Central", Coordenada(5.55442, -73.35585), "Av. Central del Norte")
    val paraderoGlorietaR2 = Paradero("PAR_R2_04", "Glorieta Norte", Coordenada(5.54465, -73.35615), "Glorieta Norte")
    val paraderoHospitalR2 = Paradero("PAR_R2_05", "Hospital San Rafael / Estadio", Coordenada(5.53985, -73.35865), "Av. Maldonado")
    val paraderoPinzonR2 = Paradero("PAR_R2_06", "Parque Pinzón", Coordenada(5.53595, -73.36018), "Av. Norte")
    val paraderoCentroCra11R2 = Paradero("PAR_R2_07", "Centro Histórico (Cra 11)", Coordenada(5.53265, -73.36245), "Carrera 11 con Calle 19")
    val paraderoBosqueR2 = Paradero("PAR_R2_08", "Bosque de la República", Coordenada(5.52885, -73.36325), "Carrera 11")
    val paraderoPatriotasR2 = Paradero("PAR_R2_09", "Barrio Los Patriotas / Sur", Coordenada(5.51185, -73.36765), "Barrio Los Patriotas")

    // --- Paraderos Clave GTFS RUTA R03 ---
    val paraderoFuenteR3 = Paradero("PAR_R3_01", "Barrio La Fuente - Occidente", Coordenada(5.53725, -73.36985), "Occidente La Fuente")
    val paraderoJdeCR3 = Paradero("PAR_R3_02", "U. Juan de Castellanos", Coordenada(5.53385, -73.36465), "Calle 20")
    val paraderoCentroCra10R3 = Paradero("PAR_R3_03", "Centro (Cra 10 con Calle 20)", Coordenada(5.53315, -73.36125), "Carrera 10")
    val paraderoEstadioR3 = Paradero("PAR_R3_04", "Av. Maldonado - Estadio", Coordenada(5.54015, -73.35855), "Av. Maldonado")
    val paraderoGlorietaR3 = Paradero("PAR_R3_05", "Glorieta Norte", Coordenada(5.54465, -73.35615), "Glorieta Norte")
    val paraderoUPTCR3 = Paradero("PAR_R3_06", "UPTC Central", Coordenada(5.55442, -73.35585), "Av. Central del Norte")
    val paraderoOlimpicaR3 = Paradero("PAR_R3_07", "Villa Olímpica", Coordenada(5.55285, -73.35255), "Av. Olímpica")
    val paraderoUniboyacaR3 = Paradero("PAR_R3_08", "Universidad de Boyacá", Coordenada(5.54842, -73.34952), "Av. Universitaria")

    // --- Paraderos Clave GTFS RUTA R04 ---
    val paraderoTopoR4 = Paradero("PAR_R4_01", "Barrio El Topo / Milagroso", Coordenada(5.52985, -73.36885), "Occidente El Topo")
    val paraderoBosqueR4 = Paradero("PAR_R4_02", "Bosque de la República", Coordenada(5.52955, -73.36225), "Centro")
    val paraderoSantanderR4 = Paradero("PAR_R4_03", "Parque Santander (Cra 10)", Coordenada(5.53455, -73.36072), "Carrera 10")
    val paraderoGlorietaR4 = Paradero("PAR_R4_04", "Glorieta Norte", Coordenada(5.54465, -73.35615), "Glorieta Norte")
    val paraderoUnicentroR4 = Paradero("PAR_R4_05", "C.C. Unicentro Tunja", Coordenada(5.54418, -73.34875), "Av. Universitaria")
    val paraderoVivaR4 = Paradero("PAR_R4_06", "C.C. Viva Tunja", Coordenada(5.54715, -73.34915), "Av. Universitaria")
    val paraderoAsisR4 = Paradero("PAR_R4_07", "Sol de Oriente / Asís", Coordenada(5.55685, -73.34825), "Oriente")

    // --- Paraderos Clave GTFS RUTA R05 ---
    val paraderoCooserviciosR5 = Paradero("PAR_R5_01", "Comfaboy / Cooservicios Sur", Coordenada(5.51685, -73.35955), "Cooservicios Sur")
    val paraderoPlazaSurR5 = Paradero("PAR_R5_02", "Plaza del Sur - Av. Colón", Coordenada(5.52315, -73.36505), "Av. Colón")
    val paraderoCentroCra10R5 = Paradero("PAR_R5_03", "Centro (Cra 10 con Calle 18)", Coordenada(5.53155, -73.36180), "Carrera 10")
    val paraderoPinzonR5 = Paradero("PAR_R5_04", "Parque Pinzón - Maldonado", Coordenada(5.53595, -73.36018), "Maldonado")
    val paraderoEstadioR5 = Paradero("PAR_R5_05", "Estadio La Independencia", Coordenada(5.54015, -73.35855), "Av. Norte")
    val paraderoGlorietaR5 = Paradero("PAR_R5_06", "Glorieta Norte", Coordenada(5.54465, -73.35615), "Glorieta Norte")
    val paraderoUPTCR5 = Paradero("PAR_R5_07", "UPTC Central", Coordenada(5.55442, -73.35585), "Av. Central del Norte")

    val paraderosLista = listOf(
        paraderoMuiscasR1, paraderoUniboyacaR1, paraderoUnicentroR1, paraderoGlorietaR1, paraderoMakroR1, paraderoMedilaserR1, paraderoTerminalR1, paraderoCiudadJardinR1,
        paraderoLimbaniaR2, paraderoSantaInesR2, paraderoUPTCR2, paraderoGlorietaR2, paraderoHospitalR2, paraderoPinzonR2, paraderoCentroCra11R2, paraderoBosqueR2, paraderoPatriotasR2,
        paraderoFuenteR3, paraderoJdeCR3, paraderoCentroCra10R3, paraderoEstadioR3, paraderoOlimpicaR3, paraderoTopoR4, paraderoSantanderR4, paraderoVivaR4, paraderoAsisR4,
        paraderoCooserviciosR5, paraderoPlazaSurR5
    )

    // Puntos paraderos clave para consulta OSRM (7-9 puntos por ruta)
    private val anchorsR1 = listOf(
        Coordenada(5.55825, -73.35192), Coordenada(5.54842, -73.34952), Coordenada(5.54418, -73.34875),
        Coordenada(5.54465, -73.35615), Coordenada(5.54162, -73.35135), Coordenada(5.53185, -73.35505),
        Coordenada(5.52520, -73.35762), Coordenada(5.51520, -73.36425)
    )

    private val anchorsR2 = listOf(
        Coordenada(5.56445, -73.35322), Coordenada(5.55885, -73.35472), Coordenada(5.55442, -73.35585),
        Coordenada(5.54465, -73.35615), Coordenada(5.53985, -73.35865), Coordenada(5.53595, -73.36018),
        Coordenada(5.53265, -73.36245), Coordenada(5.52885, -73.36325), Coordenada(5.51185, -73.36765)
    )

    private val anchorsR3 = listOf(
        Coordenada(5.53725, -73.36985), Coordenada(5.53385, -73.36465), Coordenada(5.53315, -73.36125),
        Coordenada(5.54015, -73.35855), Coordenada(5.54465, -73.35615), Coordenada(5.55442, -73.35585),
        Coordenada(5.55285, -73.35255), Coordenada(5.54842, -73.34952)
    )

    private val anchorsR4 = listOf(
        Coordenada(5.52985, -73.36885), Coordenada(5.52955, -73.36225), Coordenada(5.53455, -73.36072),
        Coordenada(5.54465, -73.35615), Coordenada(5.54418, -73.34875), Coordenada(5.54715, -73.34915),
        Coordenada(5.55685, -73.34825)
    )

    private val anchorsR5 = listOf(
        Coordenada(5.51685, -73.35955), Coordenada(5.52315, -73.36505), Coordenada(5.53155, -73.36180),
        Coordenada(5.53595, -73.36018), Coordenada(5.54015, -73.35855), Coordenada(5.54465, -73.35615),
        Coordenada(5.55442, -73.35585)
    )

    // Fallbacks locales
    private var polyR1 = densificarSobreCalzada(anchorsR1, 15.0)
    private var polyR2 = densificarSobreCalzada(anchorsR2, 15.0)
    private var polyR3 = densificarSobreCalzada(anchorsR3, 15.0)
    private var polyR4 = densificarSobreCalzada(anchorsR4, 15.0)
    private var polyR5 = densificarSobreCalzada(anchorsR5, 15.0)

    // --- StateFlow de Geometrías Viales para MapLibre ---
    private val _geometriasVialesRutas = MutableStateFlow<Map<String, List<Coordenada>>>(emptyMap())
    val geometriasVialesRutas: StateFlow<Map<String, List<Coordenada>>> = _geometriasVialesRutas.asStateFlow()

    // --- StateFlow de Rutas ---
    private val _rutasFlow = MutableStateFlow<List<RutaPiloto>>(emptyList())
    val rutasFlow: StateFlow<List<RutaPiloto>> = _rutasFlow.asStateFlow()

    val rutasLista: List<RutaPiloto> get() = _rutasFlow.value

    // --- Estado Simulación Interno ---
    private data class BusEstadoInterno(
        val vehicleId: String,
        val placa: String,
        val routeId: String,
        val empresa: EmpresaTransporte,
        var polilinea: List<Coordenada>,
        var indicePolilinea: Int,
        var velocidadKmh: Double
    )

    private val busesInternos = mutableListOf(
        BusEstadoInterno("BUS_101", "TJA-101", "R01_MUISCAS_TERMINAL", empresaCootranscol, polyR1, 0, 32.0),
        BusEstadoInterno("BUS_102", "TJA-102", "R01_MUISCAS_TERMINAL", empresaCootranscol, polyR1, 15, 30.0),

        BusEstadoInterno("BUS_201", "TJA-201", "R02_LIMBANIA_PATRIOTAS", empresaAutoboy, polyR2, 2, 35.0),
        BusEstadoInterno("BUS_202", "TJA-202", "R02_LIMBANIA_PATRIOTAS", empresaAutoboy, polyR2, 12, 28.0),

        BusEstadoInterno("BUS_301", "TJA-301", "R03_FUENTE_UNIBOYACA", empresaCooservicios, polyR3, 1, 33.0),
        BusEstadoInterno("BUS_302", "TJA-302", "R03_FUENTE_UNIBOYACA", empresaCooservicios, polyR3, 10, 29.0),

        BusEstadoInterno("BUS_401", "TJA-401", "R04_TOPO_ASIS", empresaTransTunjano, polyR4, 0, 31.0),
        BusEstadoInterno("BUS_402", "TJA-402", "R04_TOPO_ASIS", empresaTransTunjano, polyR4, 8, 34.0),

        BusEstadoInterno("BUS_501", "TJA-501", "R05_COOSERVICIOS_UPTC", empresaMuiscas, polyR5, 3, 36.0),
        BusEstadoInterno("BUS_502", "TJA-502", "R05_COOSERVICIOS_UPTC", empresaMuiscas, polyR5, 11, 32.0)
    )

    // --- Flow Exposición ---
    private val _busesFlow = MutableStateFlow<List<PosicionBus>>(emptyList())
    val busesFlow: StateFlow<List<PosicionBus>> = _busesFlow.asStateFlow()

    private val _alertasFlow = MutableStateFlow<List<AlertaGeocerca>>(emptyList())
    val alertasFlow: StateFlow<List<AlertaGeocerca>> = _alertasFlow.asStateFlow()

    private val _modoDemoRapido = MutableStateFlow(false)
    val modoDemoRapido: StateFlow<Boolean> = _modoDemoRapido.asStateFlow()

    private val _paraderoMonitoreadoId = MutableStateFlow<String?>(paraderoUniboyacaR1.stopId)
    val paraderoMonitoreadoId: StateFlow<String?> = _paraderoMonitoreadoId.asStateFlow()

    private val _radioGeocercaMetros = MutableStateFlow(300)
    val radioGeocercaMetros: StateFlow<Int> = _radioGeocercaMetros.asStateFlow()

    private val alertasActivasClave = mutableSetOf<String>()
    var listenerAlertaProximidad: ((String, String) -> Unit)? = null

    private var jobSimulacion: Job? = null

    init {
        actualizarDefinicionRutas()
        actualizarTelemetria()
        iniciarSimulacionLoop()
        cargarRutasOSRMAsync()
    }

    private fun actualizarDefinicionRutas() {
        val r1 = RutaPiloto(
            routeId = "R01_MUISCAS_TERMINAL",
            codigoRuta = "R01 - Muiscas - Uniboyacá - Terminal - Ciudad Jardín",
            nombre = "Corredor Muiscas - Uniboyacá - Av. Oriental - Terminal - Ciudad Jardín",
            empresa = empresaCootranscol,
            listaParaderos = listOf(
                paraderoMuiscasR1, paraderoUniboyacaR1, paraderoUnicentroR1, paraderoGlorietaR1,
                paraderoMakroR1, paraderoMedilaserR1, paraderoTerminalR1, paraderoCiudadJardinR1
            ),
            polilineaRuta = polyR1
        )

        val r2 = RutaPiloto(
            routeId = "R02_LIMBANIA_PATRIOTAS",
            codigoRuta = "R02 - Doña Limbania - UPTC - Cra 11 - Patriotas",
            nombre = "Corredor Norte - Santa Inés - UPTC - Hospital - Centro Cra 11 - Patriotas",
            empresa = empresaAutoboy,
            listaParaderos = listOf(
                paraderoLimbaniaR2, paraderoSantaInesR2, paraderoUPTCR2, paraderoGlorietaR2,
                paraderoHospitalR2, paraderoPinzonR2, paraderoCentroCra11R2, paraderoBosqueR2, paraderoPatriotasR2
            ),
            polilineaRuta = polyR2
        )

        val r3 = RutaPiloto(
            routeId = "R03_FUENTE_UNIBOYACA",
            codigoRuta = "R03 - La Fuente - Calle 20 Centro - UPTC - Uniboyacá",
            nombre = "Corredor La Fuente - J. de Castellanos - Calle 20 - Av. Norte - UPTC - Uniboyacá",
            empresa = empresaCooservicios,
            listaParaderos = listOf(
                paraderoFuenteR3, paraderoJdeCR3, paraderoCentroCra10R3, paraderoEstadioR3,
                paraderoGlorietaR3, paraderoUPTCR3, paraderoOlimpicaR3, paraderoUniboyacaR3
            ),
            polilineaRuta = polyR3
        )

        val r4 = RutaPiloto(
            routeId = "R04_TOPO_ASIS",
            codigoRuta = "R04 - El Topo - Centro - Unicentro - Sol de Oriente / Asís",
            nombre = "Corredor El Topo - Bosque - Cra 10 - Unicentro - Viva - Asís",
            empresa = empresaTransTunjano,
            listaParaderos = listOf(
                paraderoTopoR4, paraderoBosqueR4, paraderoSantanderR4, paraderoGlorietaR4,
                paraderoUnicentroR4, paraderoVivaR4, paraderoAsisR4
            ),
            polilineaRuta = polyR4
        )

        val r5 = RutaPiloto(
            routeId = "R05_COOSERVICIOS_UPTC",
            codigoRuta = "R05 - Cooservicios Sur - Plaza Sur - Centro Cra 10 - UPTC",
            nombre = "Corredor Cooservicios Sur - Plaza del Sur - Cra 10 - Estadio - UPTC",
            empresa = empresaMuiscas,
            listaParaderos = listOf(
                paraderoCooserviciosR5, paraderoPlazaSurR5, paraderoCentroCra10R5, paraderoPinzonR5,
                paraderoEstadioR5, paraderoGlorietaR5, paraderoUPTCR5
            ),
            polilineaRuta = polyR5
        )

        _rutasFlow.value = listOf(r1, r2, r3, r4, r5)
        _geometriasVialesRutas.value = mapOf(
            "R01_MUISCAS_TERMINAL" to polyR1,
            "R02_LIMBANIA_PATRIOTAS" to polyR2,
            "R03_FUENTE_UNIBOYACA" to polyR3,
            "R04_TOPO_ASIS" to polyR4,
            "R05_COOSERVICIOS_UPTC" to polyR5
        )
    }

    private fun cargarRutasOSRMAsync() {
        scope.launch {
            val r1 = RoadRoutingHelper.obtenerRutaOSRM(anchorsR1)
            val r2 = RoadRoutingHelper.obtenerRutaOSRM(anchorsR2)
            val r3 = RoadRoutingHelper.obtenerRutaOSRM(anchorsR3)
            val r4 = RoadRoutingHelper.obtenerRutaOSRM(anchorsR4)
            val r5 = RoadRoutingHelper.obtenerRutaOSRM(anchorsR5)

            if (r1.isNotEmpty()) polyR1 = r1
            if (r2.isNotEmpty()) polyR2 = r2
            if (r3.isNotEmpty()) polyR3 = r3
            if (r4.isNotEmpty()) polyR4 = r4
            if (r5.isNotEmpty()) polyR5 = r5

            busesInternos.forEach { bus ->
                val nuevaPoly = when (bus.routeId) {
                    "R01_MUISCAS_TERMINAL" -> polyR1
                    "R02_LIMBANIA_PATRIOTAS" -> polyR2
                    "R03_FUENTE_UNIBOYACA" -> polyR3
                    "R04_TOPO_ASIS" -> polyR4
                    "R05_COOSERVICIOS_UPTC" -> polyR5
                    else -> polyR1
                }
                bus.polilinea = nuevaPoly
                bus.indicePolilinea = bus.indicePolilinea % nuevaPoly.size
            }

            actualizarDefinicionRutas()
            actualizarTelemetria()
        }
    }

    fun setModoDemoRapido(activo: Boolean) {
        _modoDemoRapido.value = activo
        iniciarSimulacionLoop()
    }

    fun setParaderoMonitoreado(stopId: String?) {
        _paraderoMonitoreadoId.value = stopId
        alertasActivasClave.clear()
        verificarGeocerca()
    }

    fun setRadioGeocerca(radioMetros: Int) {
        _radioGeocercaMetros.value = radioMetros
        verificarGeocerca()
    }

    private fun iniciarSimulacionLoop() {
        jobSimulacion?.cancel()
        jobSimulacion = scope.launch {
            while (isActive) {
                val delayMs = if (_modoDemoRapido.value) 2000L else 10000L
                delay(delayMs)
                avanzarPosicionBuses()
                actualizarTelemetria()
                verificarGeocerca()
            }
        }
    }

    private fun avanzarPosicionBuses() {
        busesInternos.forEach { bus ->
            bus.indicePolilinea = (bus.indicePolilinea + 1) % bus.polilinea.size
        }
    }

    private fun actualizarTelemetria() {
        val paraderoMonitoreado = paraderosLista.find { it.stopId == _paraderoMonitoreadoId.value }
        val rutasActuales = _rutasFlow.value

        val posicionesCalculadas = busesInternos.map { bus ->
            val posActual = bus.polilinea[bus.indicePolilinea]
            val indiceAnterior = if (bus.indicePolilinea > 0) bus.indicePolilinea - 1 else bus.polilinea.size - 1
            val posAnterior = bus.polilinea[indiceAnterior]

            val rumbo = calcularRumboGrados(posAnterior, posActual)

            val ruta = rutasActuales.find { it.routeId == bus.routeId } ?: rutasActuales.firstOrNull()
            val proximoParadero = if (ruta != null) encontrarProximoParadero(posActual, ruta.listaParaderos) else paraderosLista.first()

            val paraderoObjetivo = paraderoMonitoreado ?: proximoParadero
            val distAlObjetivo = calcularDistanciaMetros(posActual, paraderoObjetivo.posicion).roundToInt()
            val etaMinutos = calcularEtaMinutos(distAlObjetivo, bus.velocidadKmh)

            PosicionBus(
                vehicleId = bus.vehicleId,
                placa = bus.placa,
                routeId = bus.routeId,
                empresa = bus.empresa,
                posicionActual = posActual,
                velocidadKmh = bus.velocidadKmh,
                proximoParaderoId = proximoParadero.stopId,
                distanciaAlParaderoMetros = distAlObjetivo,
                etaMinutos = etaMinutos,
                rumboGrados = rumbo
            )
        }

        _busesFlow.value = posicionesCalculadas
    }

    private fun verificarGeocerca() {
        val stopIdMonitoreado = _paraderoMonitoreadoId.value ?: return
        val paraderoMonitoreado = paraderosLista.find { it.stopId == stopIdMonitoreado } ?: return
        val radioMax = _radioGeocercaMetros.value

        _busesFlow.value.forEach { bus ->
            val dist = calcularDistanciaMetros(bus.posicionActual, paraderoMonitoreado.posicion)
            val claveDeteccion = "${bus.vehicleId}_$stopIdMonitoreado"

            if (dist <= radioMax) {
                if (!alertasActivasClave.contains(claveDeteccion)) {
                    alertasActivasClave.add(claveDeteccion)
                    val titulo = "¡Bus Cercano a ${paraderoMonitoreado.nombre}!"
                    val mensaje = "La ruta ${bus.routeId} (${bus.empresa.nombre} - ${bus.placa}) está a ${dist.roundToInt()}m. ETA: ${bus.etaMinutos} min."

                    val nuevaAlerta = AlertaGeocerca(
                        id = "ALT_${System.currentTimeMillis()}",
                        titulo = titulo,
                        mensaje = mensaje,
                        timestamp = System.currentTimeMillis(),
                        vehicleId = bus.vehicleId,
                        stopId = stopIdMonitoreado
                    )

                    _alertasFlow.value = listOf(nuevaAlerta) + _alertasFlow.value
                    listenerAlertaProximidad?.invoke(titulo, mensaje)
                }
            } else if (dist > radioMax + 100) {
                alertasActivasClave.remove(claveDeteccion)
            }
        }
    }

    private fun encontrarProximoParadero(posBus: Coordenada, paraderos: List<Paradero>): Paradero {
        return paraderos.minByOrNull { calcularDistanciaMetros(posBus, it.posicion) } ?: paraderos.first()
    }

    companion object {
        fun calcularRumboGrados(p1: Coordenada, p2: Coordenada): Float {
            val lat1 = Math.toRadians(p1.latitud)
            val lon1 = Math.toRadians(p1.longitud)
            val lat2 = Math.toRadians(p2.latitud)
            val lon2 = Math.toRadians(p2.longitud)
            val dLon = lon2 - lon1
            val y = sin(dLon) * cos(lat2)
            val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
            var bearing = Math.toDegrees(atan2(y, x)).toFloat()
            if (bearing < 0) bearing += 360f
            return bearing
        }

        fun densificarSobreCalzada(puntos: List<Coordenada>, distanciaMaxMetros: Double = 15.0): List<Coordenada> {
            if (puntos.size < 2) return puntos
            val resultado = mutableListOf<Coordenada>()
            for (i in 0 until puntos.size - 1) {
                val p1 = puntos[i]
                val p2 = puntos[i + 1]
                resultado.add(p1)
                val distMetros = calcularDistanciaMetros(p1, p2)
                if (distMetros > distanciaMaxMetros) {
                    val pasos = (distMetros / distanciaMaxMetros).toInt()
                    for (step in 1 until pasos) {
                        val fraction = step.toDouble() / pasos
                        val lat = p1.latitud + fraction * (p2.latitud - p1.latitud)
                        val lng = p1.longitud + fraction * (p2.longitud - p1.longitud)
                        resultado.add(Coordenada(lat, lng))
                    }
                }
            }
            resultado.add(puntos.last())
            return resultado
        }

        fun generarInterpolacionPolilinea(waypoints: List<Coordenada>, pasosPorTramo: Int = 4): List<Coordenada> {
            return densificarSobreCalzada(waypoints, 15.0)
        }

        fun calcularDistanciaMetros(p1: Coordenada, p2: Coordenada): Double {
            val radioTierraMetros = 6371000.0
            val dLat = Math.toRadians(p2.latitud - p1.latitud)
            val dLng = Math.toRadians(p2.longitud - p1.longitud)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(p1.latitud)) * cos(Math.toRadians(p2.latitud)) *
                    sin(dLng / 2) * sin(dLng / 2)
            val c = 2 * atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
            return radioTierraMetros * c
        }

        fun calcularEtaMinutos(distanciaMetros: Int, velocidadKmh: Double): Int {
            if (distanciaMetros <= 50) return 0
            val velocidadMps = (if (velocidadKmh <= 0.0) 25.0 else velocidadKmh) * 1000.0 / 3600.0
            val segundos = distanciaMetros / velocidadMps
            val minutos = (segundos / 60.0).roundToInt()
            return if (minutos < 1) 1 else minutos
        }
    }
}
