package com.example.rutunja.model

/**
 * Representa una coordenada geográfica (Latitud, Longitud) independiente del SDK de mapas.
 */
data class Coordenada(
    val latitud: Double,
    val longitud: Double
)

/**
 * Representa una Empresa de Transporte Público Colectivo Urbano (TPCU) en Tunja.
 */
data class EmpresaTransporte(
    val id: String,
    val nombre: String,
    val colorHex: String
)

/**
 * Representa un Paradero oficial de la red GTFS de Tunja.
 */
data class Paradero(
    val stopId: String,
    val nombre: String,
    val posicion: Coordenada,
    val direccion: String = ""
)

/**
 * Representa una Ruta Piloto (GTFS Route) operada en Tunja.
 */
data class RutaPiloto(
    val routeId: String,
    val codigoRuta: String,
    val nombre: String,
    val empresa: EmpresaTransporte,
    val listaParaderos: List<Paradero>,
    val polilineaRuta: List<Coordenada>,
    val horarioServicio: String = "05:30 AM - 09:30 PM"
)

/**
 * Representa la posición en tiempo real de un vehículo (GTFS-Realtime VehiclePosition) con rumbo en grados.
 */
data class PosicionBus(
    val vehicleId: String,
    val placa: String,
    val routeId: String,
    val empresa: EmpresaTransporte,
    val posicionActual: Coordenada,
    val velocidadKmh: Double,
    val proximoParaderoId: String,
    val distanciaAlParaderoMetros: Int,
    val etaMinutos: Int,
    val rumboGrados: Float = 0f
)

/**
 * Registro de una alerta activada por la geocerca de proximidad.
 */
data class AlertaGeocerca(
    val id: String,
    val titulo: String,
    val mensaje: String,
    val timestamp: Long,
    val vehicleId: String,
    val stopId: String
)
