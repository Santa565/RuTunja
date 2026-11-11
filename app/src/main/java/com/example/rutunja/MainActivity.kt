package com.example.rutunja

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.rutunja.ui.RuTunjaApp
import com.example.rutunja.ui.theme.RuTunjaTheme

/**
 * Actividad Principal de la Aplicación RuTunja (TPCU Tunja, Boyacá).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RuTunjaTheme {
                RuTunjaApp()
            }
        }
    }
}
