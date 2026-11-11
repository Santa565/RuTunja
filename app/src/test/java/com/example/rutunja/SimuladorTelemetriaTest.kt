package com.example.rutunja

import com.example.rutunja.data.SimuladorTelemetriaRepository
import com.example.rutunja.model.Coordenada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias locales para la lógica de Haversine, ETA y catálogo GTFS de Tunja.
 */
class SimuladorTelemetriaTest {

    @Test
    fun calcularDistanciaHaversine_entrePuntosTunja_esCorrecta() {
        val muiscas = Coordenada(5.55820, -73.35190)
        val uniboyaca = Coordenada(5.54840, -73.34950)

        val distanciaMetros = SimuladorTelemetriaRepository.calcularDistanciaMetros(muiscas, uniboyaca)

        assertTrue("La distancia debe estar entre 800m y 1400m", distanciaMetros in 800.0..1400.0)
    }

    @Test
    fun calcularEtaMinutos_devuelveValorCasiReal() {
        // 1500 metros a 30 km/h -> 3 minutos
        val eta = SimuladorTelemetriaRepository.calcularEtaMinutos(1500, 30.0)
        assertEquals(3, eta)
    }

    @Test
    fun catalogoGTFS_contieneParaderosYRutasTunja() {
        val repo = SimuladorTelemetriaRepository()

        assertEquals("Debe haber 5 rutas piloto completas", 5, repo.rutasLista.size)
        assertEquals("Debe haber 5 empresas operadoras", 5, repo.empresasLista.size)
        assertTrue("Debe haber múltiples paraderos oficiales en Tunja", repo.paraderosLista.size >= 10)
    }
}
