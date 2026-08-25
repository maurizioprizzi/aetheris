package org.aetheris.app.presentation.measurement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aetheris.app.domain.model.TrackingStatus
import org.koin.androidx.compose.koinViewModel

@Composable
fun MeasurementScreen(
    viewModel: MeasurementViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Telemetria Superior (HUD)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackingStatusBadge(status = uiState.trackingStatus)
                DepthIndicatorBadge(isActive = uiState.isDepthActive)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "POINTS: ${uiState.detectedPointsCount}",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF58A6FF)
            )
        }

        // Retículo Central de Mira
        Box(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Center)
                .border(
                    width = 2.dp,
                    color = if (uiState.isTargetingSurface) Color(0xFF3FB950) else Color(0xFFF85149),
                    shape = CircleShape
                )
        )

        // Painel Inferior de Resultados
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22).copy(alpha = 0.92f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val measurement = uiState.currentMeasurement
                if (measurement != null) {
                    Text(
                        text = "${String.format("%.3f", measurement.meters)} m",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Text(
                        text = "± ${String.format("%.4f", measurement.uncertaintyMeters)} m",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF8B949E)
                    )
                } else {
                    Text(
                        text = if (uiState.selectedStartPoint == null) "Posicione a mira e fixe o Ponto A" else "Fixe o Ponto B para calcular",
                        fontSize = 14.sp,
                        color = Color(0xFFC9D1D9)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.onAnchorPointTapped() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636))
                    ) {
                        Text(if (uiState.selectedStartPoint == null) "Ponto A" else "Ponto B")
                    }

                    OutlinedButton(
                        onClick = { viewModel.onResetMeasurements() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF85149))
                    ) {
                        Text("Reset")
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackingStatusBadge(status: TrackingStatus) {
    val (bg, label) = when (status) {
        TrackingStatus.TRACKING -> Color(0xFF238636) to "TRACKING"
        TrackingStatus.INITIALIZING -> Color(0xFF9E6A03) to "INITIALIZING"
        else -> Color(0xFFDA3633) to status.name
    }

    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun DepthIndicatorBadge(isActive: Boolean) {
    Surface(
        color = if (isActive) Color(0xFF1F6FEB) else Color(0xFF30363D),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = if (isActive) "TOF / DEPTH ON" else "DEPTH RAW",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}