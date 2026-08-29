package org.aetheris.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aetheris.app.domain.model.DistanceMeasurement
import org.aetheris.app.domain.model.ScreenPoint2D
import kotlin.math.roundToInt

@Composable
fun FloatingMeasurementBadge(
    measurement: DistanceMeasurement?,
    screenPosition: ScreenPoint2D?,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val visibleMeasurement = measurement?.takeIf {
        screenPosition?.isVisible == true
    }

    val visiblePosition = screenPosition?.takeIf {
        measurement != null && it.isVisible
    }

    val isVisible =
        visibleMeasurement != null &&
                visiblePosition != null

    /*
     * Mantém os últimos valores válidos durante
     * a animação de saída.
     *
     * Sem essa retenção, o conteúdo desapareceria
     * imediatamente quando os parâmetros virassem null.
     */
    var retainedMeasurement by remember {
        mutableStateOf<DistanceMeasurement?>(null)
    }

    var retainedPosition by remember {
        mutableStateOf<ScreenPoint2D?>(null)
    }

    if (
        visibleMeasurement != null &&
        visiblePosition != null
    ) {
        SideEffect {
            if (retainedMeasurement != visibleMeasurement) {
                retainedMeasurement = visibleMeasurement
            }

            if (retainedPosition != visiblePosition) {
                retainedPosition = visiblePosition
            }
        }
    }

    val displayedMeasurement =
        visibleMeasurement ?: retainedMeasurement

    val displayedPosition =
        visiblePosition ?: retainedPosition

    var containerSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    var badgeSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    val spacingPx = with(density) {
        BADGE_SPACING_DP.dp.roundToPx()
    }

    Box(
        modifier = modifier
            .onSizeChanged { size ->
                if (containerSize != size) {
                    containerSize = size
                }
            },
        contentAlignment = Alignment.TopStart
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis =
                        ENTER_DURATION_MILLIS
                )
            ),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis =
                        EXIT_DURATION_MILLIS
                )
            ),
            modifier = Modifier.offset {
                calculateBadgeOffset(
                    screenPosition =
                        displayedPosition,
                    containerSize =
                        containerSize,
                    badgeSize =
                        badgeSize,
                    spacingPx =
                        spacingPx
                )
            }
        ) {
            if (displayedMeasurement != null) {
                Text(
                    text =
                        displayedMeasurement
                            .formattedMetric(),
                    color = BadgePrimaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    modifier = Modifier
                        .onSizeChanged { size ->
                            if (badgeSize != size) {
                                badgeSize = size
                            }
                        }
                        .clip(BadgeShape)
                        .background(
                            BadgeBackgroundColor
                        )
                        .border(
                            width = 1.dp,
                            color = BadgeBorderColor,
                            shape = BadgeShape
                        )
                        .padding(
                            horizontal = 8.dp,
                            vertical = 5.dp
                        )
                )
            }
        }
    }
}

private fun calculateBadgeOffset(
    screenPosition: ScreenPoint2D?,
    containerSize: IntSize,
    badgeSize: IntSize,
    spacingPx: Int
): IntOffset {
    if (
        screenPosition == null ||
        containerSize == IntSize.Zero ||
        badgeSize == IntSize.Zero
    ) {
        return IntOffset.Zero
    }

    val desiredX =
        screenPosition.x -
                badgeSize.width / 2f

    val positionAbove =
        screenPosition.y -
                badgeSize.height -
                spacingPx

    val positionBelow =
        screenPosition.y +
                spacingPx

    /*
     * Posiciona o indicador acima do ponto.
     * Quando não existe espaço suficiente,
     * tenta posicioná-lo abaixo.
     */
    val desiredY =
        if (positionAbove >= 0f) {
            positionAbove
        } else {
            positionBelow
        }

    val maximumX =
        (containerSize.width - badgeSize.width)
            .coerceAtLeast(0)

    val maximumY =
        (containerSize.height - badgeSize.height)
            .coerceAtLeast(0)

    return IntOffset(
        x = desiredX
            .roundToInt()
            .coerceIn(
                minimumValue = 0,
                maximumValue = maximumX
            ),
        y = desiredY
            .roundToInt()
            .coerceIn(
                minimumValue = 0,
                maximumValue = maximumY
            )
    )
}

private const val ENTER_DURATION_MILLIS = 150
private const val EXIT_DURATION_MILLIS = 150
private const val BADGE_SPACING_DP = 10

private val BadgeShape =
    RoundedCornerShape(6.dp)

private val BadgeBackgroundColor =
    Color(0xE60A0E17)

private val BadgeBorderColor =
    Color(0xFF00E5FF)

private val BadgePrimaryColor =
    Color(0xFF00E5FF)