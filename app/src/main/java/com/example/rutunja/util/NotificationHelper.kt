package com.example.rutunja.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Helper para gestión de Notificaciones Locales y Alertas de Geocerca del TPCU Tunja.
 */
object NotificationHelper {

    private const val CHANNEL_ID = "rutunja_alerts_channel"
    private const val CHANNEL_NAME = "Alertas Proximidad RuTunja"
    private const val CHANNEL_DESC = "Notificaciones de geocerca cuando un bus está cerca a un paradero"
    private var notificationIdCounter = 1000

    /**
     * Inicializa el canal de notificaciones requerido para Android 8.0 (API 26) o superior.
     */
    fun crearCanalNotificaciones(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Verifica si se cuenta con el permiso POST_NOTIFICATIONS (Android 13+).
     */
    fun tienePermisoNotificacion(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Dispara una notificación nativa del sistema Android.
     */
    fun lanzarNotificacion(context: Context, titulo: String, mensaje: String) {
        crearCanalNotificaciones(context)

        if (!tienePermisoNotificacion(context)) {
            return
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationIdCounter++, builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
