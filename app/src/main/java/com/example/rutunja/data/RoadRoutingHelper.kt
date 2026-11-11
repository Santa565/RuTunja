package com.example.rutunja.data

import com.example.rutunja.model.Coordenada
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Helper para consulta asíncrona de enrutamiento vial real enviando únicamente los paraderos clave.
 * OSRM calcula automáticamente todos los giros reales por las calles y carreras de Tunja devolviendo ~300 puntos exactos.
 */
object RoadRoutingHelper {

    private val cacheRutasOSRM = mutableMapOf<String, List<Coordenada>>()

    /**
     * Consulta OSRM enviando los 7-9 paraderos clave de la ruta.
     */
    suspend fun obtenerRutaOSRM(puntosParaderos: List<Coordenada>): List<Coordenada> = withContext(Dispatchers.IO) {
        if (puntosParaderos.size < 2) return@withContext puntosParaderos

        val cacheKey = puntosParaderos.joinToString(";") { "${it.latitud},${it.longitud}" }
        synchronized(cacheRutasOSRM) {
            if (cacheRutasOSRM.containsKey(cacheKey)) {
                return@withContext cacheRutasOSRM.getValue(cacheKey)
            }
        }

        val coordsQuery = puntosParaderos.joinToString(";") { "${it.longitud},${it.latitud}" }
        val servidoresOSRM = listOf(
            "https://routing.openstreetmap.de/routed-car/route/v1/driving/$coordsQuery?overview=full&geometries=geojson&continue_straight=true",
            "https://router.project-osrm.org/route/v1/driving/$coordsQuery?overview=full&geometries=geojson&continue_straight=true"
        )

        for (urlStr in servidoresOSRM) {
            try {
                val url = URL(urlStr)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 7000
                    readTimeout = 7000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android; RuTunja-Uniboyaca)")
                }

                if (connection.responseCode == 200) {
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val jsonText = reader.readText()
                    reader.close()
                    inputStream.close()

                    val jsonObject = JSONObject(jsonText)
                    val routes = jsonObject.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val primeraRuta = routes.getJSONObject(0)
                        val geometry = primeraRuta.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")

                        val puntosMapeados = mutableListOf<Coordenada>()
                        for (i in 0 until coordinates.length()) {
                            val punto = coordinates.getJSONArray(i)
                            val lon = punto.getDouble(0)
                            val lat = punto.getDouble(1)
                            puntosMapeados.add(Coordenada(lat, lon))
                        }

                        if (puntosMapeados.isNotEmpty()) {
                            val densificada = SimuladorTelemetriaRepository.densificarSobreCalzada(puntosMapeados, 15.0)
                            synchronized(cacheRutasOSRM) {
                                cacheRutasOSRM[cacheKey] = densificada
                            }
                            return@withContext densificada
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback local en caso de ausencia de red
        val fallback = SimuladorTelemetriaRepository.densificarSobreCalzada(puntosParaderos, 15.0)
        synchronized(cacheRutasOSRM) {
            cacheRutasOSRM[cacheKey] = fallback
        }
        return@withContext fallback
    }
}
