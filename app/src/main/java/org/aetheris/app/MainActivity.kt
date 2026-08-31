package org.aetheris.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import org.aetheris.app.data.arcore.ArCoreSessionManager
import org.aetheris.app.presentation.measurement.MeasurementScreen
import org.aetheris.app.presentation.measurement.MeasurementViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    /**
     * O ViewModel fica associado ao ciclo de vida da Activity.
     *
     * A resolução ocorre pelo contexto global iniciado em
     * AetherisApplication, sem depender de um contexto Koin
     * adicional dentro da composição.
     */
    private val measurementViewModel:
            MeasurementViewModel by viewModel()

    /**
     * ArCoreSessionManager é um singleton registrado no Koin.
     */
    private val arCoreSessionManager:
            ArCoreSessionManager by inject()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme
                        .colorScheme
                        .background
                ) {
                    MeasurementScreen(
                        viewModel = measurementViewModel,
                        sessionManager = arCoreSessionManager
                    )
                }
            }
        }
    }
}