package com.example.rutunja.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory

/**
 * Generador y caché de íconos Bitmap para MapLibre Native SDK usando Canvas nativo de Android (60 FPS).
 * Soporta marcadores de bus direccionales rotados según rumboGrados cuantificado a 15° y el resaltado "BUS A ABORDAR".
 */
object MarkerBitmapHelper {

    private val busIconCache = mutableMapOf<String, Icon>()
    private val stopIconCache = mutableMapOf<String, Icon>()

    fun obtenerIconoBusDireccional(
        context: Context,
        codigoRuta: String,
        colorHex: String,
        rumboGrados: Float,
        esMiBusAbordar: Boolean = false
    ): Icon {
        val anguloCuantificado = ((rumboGrados % 360 + 360) % 360 / 15).toInt() * 15
        val cacheKey = "${codigoRuta}_${colorHex}_${anguloCuantificado}_$esMiBusAbordar"

        busIconCache[cacheKey]?.let { return it }

        val size = 180
        val center = size / 2f
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val parsedColor = if (esMiBusAbordar) Color.parseColor("#FFD700") else try {
            Color.parseColor(colorHex)
        } catch (_: Exception) {
            Color.RED
        }

        val strokeColor = if (esMiBusAbordar) Color.parseColor("#D50000") else Color.WHITE

        // 1. Dibujar Flecha Direccional Rotada
        canvas.save()
        canvas.rotate(anguloCuantificado.toFloat(), center, center)

        val arrowPath = Path().apply {
            moveTo(center, 10f)        // Punta de la flecha
            lineTo(center - 26f, 54f)  // Ala izquierda
            lineTo(center + 26f, 54f)  // Ala derecha
            close()
        }

        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parsedColor
            style = Paint.Style.FILL
        }

        val arrowStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        canvas.drawPath(arrowPath, arrowPaint)
        canvas.drawPath(arrowPath, arrowStroke)
        canvas.restore()

        // 2. Dibujar Escudo Central (Fijo en posición vertical)
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (esMiBusAbordar) Color.parseColor("#D50000") else parsedColor
            style = Paint.Style.FILL
        }

        val circleStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        canvas.drawCircle(center, center, 44f, circlePaint)
        canvas.drawCircle(center, center, 44f, circleStroke)

        // 3. Dibujar Texto de la Ruta
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val textoVisual = if (esMiBusAbordar) "🌟 $codigoRuta" else codigoRuta
        val textY = center - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(textoVisual, center, textY, textPaint)

        val icon = IconFactory.getInstance(context).fromBitmap(bitmap)
        busIconCache[cacheKey] = icon
        return icon
    }

    fun obtenerIconoParadero(context: Context, esMonitoreado: Boolean, esSeleccionado: Boolean): Icon {
        val cacheKey = "STOP_${esMonitoreado}_$esSeleccionado"
        stopIconCache[cacheKey]?.let { return it }

        val size = 64
        val center = size / 2f
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val borderColor = when {
            esMonitoreado -> Color.parseColor("#2E7D32")  // Verde brillante
            esSeleccionado -> Color.parseColor("#1565C0") // Azul seleccionado
            else -> Color.parseColor("#0D47A1")          // Azul marino
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, center - 6f, fillPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        canvas.drawCircle(center, center, center - 6f, strokePaint)

        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, 10f, centerPaint)

        val icon = IconFactory.getInstance(context).fromBitmap(bitmap)
        stopIconCache[cacheKey] = icon
        return icon
    }
}
