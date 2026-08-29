package org.aetheris.app.presentation.measurement

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.aetheris.app.data.arcore.ArCoreSessionManager
import org.aetheris.app.domain.model.DistanceMeasurement
import org.aetheris.app.domain.model.TrackingStatus
import org.aetheris.app.presentation.components.ArCameraFeed
import org.aetheris.app.presentation.components.FloatingMeasurementBadge
import org.aetheris.app.presentation.permissions.CameraPermissionHandler
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.Locale

@Composable
fun MeasurementScreen(
    viewModel: MeasurementViewModel = koinViewModel(),
    sessionManager: ArCoreSessionManager = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val coroutineScope = rememberCoroutineScope()

    CameraPermissionHandler {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            ArCameraFeed(
                sessionManager = sessionManager,
                modifier = Modifier.fillMaxSize(),
                startPoint = uiState.selectedStartPoint,
                endPoint = uiState.selectedEndPoint,
                onSurfaceChanged = viewModel::onSurfaceDimensionsChanged,
                onMatricesUpdated = viewModel::onCameraMatricesUpdated,
                onFrameAvailable = viewModel::processFrame,
                onError = { error ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = error.message
                                ?: "Falha ao executar o ARCore."
                        )
                    }
                }
            )

            /*
             * O badge ocupa a mesma área usada pela câmera para que
             * as coordenadas projetadas correspondam com exatidão.
             */
            FloatingMeasurementBadge(
                measurement = uiState.currentMeasurement,
                screenPosition = uiState.badgePosition,
                modifier = Modifier.fillMaxSize()
            )

            MeasurementTelemetryHud(
                trackingStatus = uiState.trackingStatus,
                isDepthActive = uiState.isDepthEnabled,
                detectedPointCount = uiState.detectedPointsCount,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
            )

            TargetReticle(
                isTargetingSurface = uiState.isTargetingSurface,
                modifier = Modifier.align(Alignment.Center)
            )

            MeasurementControlPanel(
                measurement = uiState.currentMeasurement,
                trackingStatus = uiState.trackingStatus,
                isTargetingSurface = uiState.isTargetingSurface,
                hasStartPoint = uiState.selectedStartPoint != null,
                hasEndPoint = uiState.selectedEndPoint != null,
                onPlaceAnchor = viewModel::onAnchorPointTapped,
                onReset = viewModel::onResetMeasurements,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp)
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(
                        start = 16.dp,
                        top = 72.dp,
                        end = 16.dp
                    )
            )
        }
    }
}

@Composable
private fun MeasurementTelemetryHud(
    trackingStatus: TrackingStatus,
    isDepthActive: Boolean,
    detectedPointCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackingStatusBadge(
                status = trackingStatus
            )

            DepthIndicatorBadge(
                isActive = isDepthActive
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "PONTOS: $detectedPointCount",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TelemetryColor
        )
    }
}

@Composable
private fun TargetReticle(
    isTargetingSurface: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isTargetingSurface) {
        SuccessColor
    } else {
        ErrorColor
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .border(
                width = 2.dp,
                color = color,
                shape = CircleShape
            )
    )
}

@Composable
private fun MeasurementControlPanel(
    measurement: DistanceMeasurement?,
    trackingStatus: TrackingStatus,
    isTargetingSurface: Boolean,
    hasStartPoint: Boolean,
    hasEndPoint: Boolean,
    onPlaceAnchor: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canPlaceAnchor =
        trackingStatus.allowsAnchorPlacement &&
                isTargetingSurface &&
                !hasEndPoint

    val hasAnyAnchor =
        hasStartPoint || hasEndPoint

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = PanelColor.copy(alpha = 0.92f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (measurement != null) {
                MeasurementResult(
                    measurement = measurement
                )
            } else {
                MeasurementInstruction(
                    trackingStatus = trackingStatus,
                    isTargetingSurface = isTargetingSurface,
                    hasStartPoint = hasStartPoint,
                    hasEndPoint = hasEndPoint
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPlaceAnchor,
                    enabled = canPlaceAnchor,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessColor
                    )
                ) {
                    Text(
                        text = anchorButtonLabel(
                            hasStartPoint = hasStartPoint,
                            hasEndPoint = hasEndPoint
                        )
                    )
                }

                OutlinedButton(
                    onClick = onReset,
                    enabled = hasAnyAnchor,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ErrorColor
                    )
                ) {
                    Text("Limpar")
                }
            }
        }
    }
}

@Composable
private fun MeasurementResult(
    measurement: DistanceMeasurement
) {
    val locale = Locale.getDefault()

    Text(
        text = measurement.formattedValueOnly(locale),
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = Color.White
    )

    val uncertaintyText = if (measurement.meters < 1f) {
        String.format(locale, "±%.1f cm", measurement.uncertaintyCentimeters)
    } else {
        String.format(locale, "±%.3f m", measurement.uncertaintyMeters)
    }

    Text(
        text = uncertaintyText,
        fontSize = 13.sp,
        fontFamily = FontFamily.Monospace,
        color = SecondaryTextColor
    )
}

@Composable
private fun MeasurementInstruction(
    trackingStatus: TrackingStatus,
    isTargetingSurface: Boolean,
    hasStartPoint: Boolean,
    hasEndPoint: Boolean
) {
    val instruction = when {
        trackingStatus != TrackingStatus.TRACKING ->
            trackingInstruction(trackingStatus)

        !isTargetingSurface ->
            "Aponte a mira para uma superfície detectada"

        !hasStartPoint ->
            "Posicione a mira e fixe o ponto A"

        !hasEndPoint ->
            "Posicione a mira e fixe o ponto B"

        else ->
            "Medição concluída"
    }

    Text(
        text = instruction,
        fontSize = 14.sp,
        color = PrimaryTextColor
    )
}

@Composable
private fun TrackingStatusBadge(
    status: TrackingStatus
) {
    val presentation = when (status) {
        TrackingStatus.TRACKING -> {
            StatusPresentation(
                color = SuccessColor,
                label = "RASTREAMENTO"
            )
        }

        TrackingStatus.INITIALIZING -> {
            StatusPresentation(
                color = WarningColor,
                label = "INICIALIZANDO"
            )
        }

        TrackingStatus.EXCESSIVE_MOTION -> {
            StatusPresentation(
                color = WarningColor,
                label = "MOVIMENTO RÁPIDO"
            )
        }

        TrackingStatus.INSUFFICIENT_FEATURES -> {
            StatusPresentation(
                color = WarningColor,
                label = "POUCOS DETALHES"
            )
        }

        TrackingStatus.INSUFFICIENT_LIGHT -> {
            StatusPresentation(
                color = WarningColor,
                label = "POUCA LUZ"
            )
        }

        TrackingStatus.CAMERA_UNAVAILABLE -> {
            StatusPresentation(
                color = ErrorColor,
                label = "CÂMERA INDISPONÍVEL"
            )
        }

        TrackingStatus.PAUSED -> {
            StatusPresentation(
                color = InactiveColor,
                label = "PAUSADO"
            )
        }

        TrackingStatus.UNAVAILABLE -> {
            StatusPresentation(
                color = ErrorColor,
                label = "INDISPONÍVEL"
            )
        }
    }

    Surface(
        color = presentation.color,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = presentation.label,
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 4.dp
            ),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun DepthIndicatorBadge(
    isActive: Boolean
) {
    Surface(
        color = if (isActive) {
            DepthActiveColor
        } else {
            InactiveColor
        },
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = if (isActive) {
                "DEPTH ON"
            } else {
                "DEPTH OFF"
            },
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 4.dp
            ),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

private fun anchorButtonLabel(
    hasStartPoint: Boolean,
    hasEndPoint: Boolean
): String {
    return when {
        !hasStartPoint -> "Fixar ponto A"
        !hasEndPoint -> "Fixar ponto B"
        else -> "Medição concluída"
    }
}

private fun trackingInstruction(
    status: TrackingStatus
): String {
    return when (status) {
        TrackingStatus.INITIALIZING ->
            "Inicializando o rastreamento"

        TrackingStatus.EXCESSIVE_MOTION ->
            "Movimente o aparelho mais devagar"

        TrackingStatus.INSUFFICIENT_FEATURES ->
            "Aponte para uma superfície com mais detalhes"

        TrackingStatus.INSUFFICIENT_LIGHT ->
            "Melhore a iluminação do ambiente"

        TrackingStatus.CAMERA_UNAVAILABLE ->
            "A câmera está indisponível"

        TrackingStatus.PAUSED ->
            "Rastreamento pausado"

        TrackingStatus.UNAVAILABLE ->
            "Rastreamento indisponível"

        TrackingStatus.TRACKING ->
            ""
    }
}

private data class StatusPresentation(
    val color: Color,
    val label: String
)

private val PanelColor = Color(0xFF161B22)
private val PrimaryTextColor = Color(0xFFC9D1D9)
private val SecondaryTextColor = Color(0xFF8B949E)
private val TelemetryColor = Color(0xFF58A6FF)
private val SuccessColor = Color(0xFF238636)
private val WarningColor = Color(0xFF9E6A03)
private val ErrorColor = Color(0xFFDA3633)
private val InactiveColor = Color(0xFF30363D)
private val DepthActiveColor = Color(0xFF1F6FEB)