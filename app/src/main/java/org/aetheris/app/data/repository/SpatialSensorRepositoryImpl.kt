package org.aetheris.app.data.repository

import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.aetheris.app.data.arcore.ArCoreFrameProcessor
import org.aetheris.app.data.arcore.ArCoreHitTestProcessor
import org.aetheris.app.domain.model.AnchorSlot
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.SpatialFrameData
import org.aetheris.app.domain.model.TrackingStatus
import org.aetheris.app.domain.repository.SpatialSensorRepository

class SpatialSensorRepositoryImpl(
    private val frameProcessor: ArCoreFrameProcessor,
    private val hitTestProcessor: ArCoreHitTestProcessor,
    private val isDepthEnabledProvider: () -> Boolean
) : SpatialSensorRepository {

    private val _spatialDataStream =
        MutableStateFlow(SpatialFrameData())

    private val anchorLock = Any()

    @Volatile
    private var currentFrame: Frame? = null

    @Volatile
    private var viewportWidthPx: Int = 0

    @Volatile
    private var viewportHeightPx: Int = 0

    private var startAnchor: Anchor? = null
    private var endAnchor: Anchor? = null

    override val spatialDataStream: StateFlow<SpatialFrameData> =
        _spatialDataStream.asStateFlow()

    /**
     * Atualiza as dimensões usadas para converter coordenadas
     * normalizadas em pixels.
     *
     * Deve ser chamado a partir de onSurfaceChanged().
     */
    fun updateViewportSize(
        widthPx: Int,
        heightPx: Int
    ) {
        require(widthPx > 0) {
            "A largura da viewport deve ser maior que zero."
        }

        require(heightPx > 0) {
            "A altura da viewport deve ser maior que zero."
        }

        viewportWidthPx = widthPx
        viewportHeightPx = heightPx
    }

    /**
     * Atualiza o estado espacial a partir do frame ARCore mais recente.
     * Deve ser chamado a cada quadro na thread de renderização OpenGL.
     */
    fun onFrameUpdate(frame: Frame) {
        currentFrame = frame

        val camera = frame.camera
        val trackingStatus = camera.toDomainTrackingStatus()
        val isTracking = camera.trackingState == TrackingState.TRACKING

        val points = if (isTracking) {
            frameProcessor.processPointCloud(frame)
        } else {
            emptyList()
        }

        val centerPixels = normalizedToPixels(0.5f, 0.5f)
        val isSurfaceDetected = if (isTracking && centerPixels != null) {
            hitTestProcessor.hasValidSurfaceAt(
                frame = frame,
                xPx = centerPixels.x,
                yPx = centerPixels.y
            )
        } else {
            false
        }

        val anchorSnapshot = resolveAnchors()

        _spatialDataStream.update { current ->
            current.copy(
                trackingStatus = trackingStatus,
                isDepthEnabled = isDepthEnabledProvider(),
                pointCount = points.size,
                isSurfaceDetected = isSurfaceDetected,
                anchoredStartPoint = anchorSnapshot.start.resolve(current.anchoredStartPoint),
                anchoredEndPoint = anchorSnapshot.end.resolve(current.anchoredEndPoint)
            )
        }
    }

    override suspend fun performHitTest(
        normalizedX: Float,
        normalizedY: Float
    ): Point3D? {
        val frame = currentTrackingFrame()
            ?: return null

        val pixelCoordinates = normalizedToPixels(
            normalizedX = normalizedX,
            normalizedY = normalizedY
        ) ?: return null

        return hitTestProcessor.performHitTest(
            frame = frame,
            xPx = pixelCoordinates.x,
            yPx = pixelCoordinates.y
        )
    }

    override suspend fun createAnchor(
        normalizedX: Float,
        normalizedY: Float,
        slot: AnchorSlot
    ): Point3D? {
        val frame = currentTrackingFrame()
            ?: return null

        val pixelCoordinates = normalizedToPixels(
            normalizedX = normalizedX,
            normalizedY = normalizedY
        ) ?: return null

        val newAnchor = hitTestProcessor.createAnchorAt(
            frame = frame,
            xPx = pixelCoordinates.x,
            yPx = pixelCoordinates.y
        ) ?: return null

        val point = newAnchor.pose.toPoint3D()

        replaceAnchor(
            slot = slot,
            newAnchor = newAnchor
        )

        _spatialDataStream.update { current ->
            when (slot) {
                AnchorSlot.START -> {
                    current.copy(
                        anchoredStartPoint = point
                    )
                }

                AnchorSlot.END -> {
                    current.copy(
                        anchoredEndPoint = point
                    )
                }
            }
        }

        return point
    }

    override fun clearAnchors() {
        val anchorsToDetach = synchronized(anchorLock) {
            val anchors = listOfNotNull(
                startAnchor,
                endAnchor
            )

            startAnchor = null
            endAnchor = null

            anchors
        }

        anchorsToDetach.forEach { anchor ->
            anchor.detach()
        }

        _spatialDataStream.update { current ->
            current.copy(
                anchoredStartPoint = null,
                anchoredEndPoint = null
            )
        }
    }

    /**
     * Deve ser chamado antes de destruir a sessão ARCore.
     */
    fun release() {
        clearAnchors()

        currentFrame = null
        viewportWidthPx = 0
        viewportHeightPx = 0

        _spatialDataStream.update { current ->
            current.copy(
                trackingStatus = TrackingStatus.UNAVAILABLE,
                isDepthEnabled = false,
                pointCount = 0,
                isSurfaceDetected = false
            )
        }
    }

    private fun replaceAnchor(
        slot: AnchorSlot,
        newAnchor: Anchor
    ) {
        val previousAnchor = synchronized(anchorLock) {
            when (slot) {
                AnchorSlot.START -> {
                    val previous = startAnchor
                    startAnchor = newAnchor
                    previous
                }

                AnchorSlot.END -> {
                    val previous = endAnchor
                    endAnchor = newAnchor
                    previous
                }
            }
        }

        if (previousAnchor !== newAnchor) {
            previousAnchor?.detach()
        }
    }

    private fun resolveAnchors(): AnchorSnapshot {
        val anchorsToDetach = mutableListOf<Anchor>()

        val snapshot = synchronized(anchorLock) {
            val resolvedStart = resolveAnchor(startAnchor)
            val resolvedEnd = resolveAnchor(endAnchor)

            if (resolvedStart.removeAnchor) {
                startAnchor?.let(anchorsToDetach::add)
                startAnchor = null
            }

            if (resolvedEnd.removeAnchor) {
                endAnchor?.let(anchorsToDetach::add)
                endAnchor = null
            }

            AnchorSnapshot(
                start = resolvedStart,
                end = resolvedEnd
            )
        }

        anchorsToDetach.forEach { anchor ->
            anchor.detach()
        }

        return snapshot
    }

    private fun resolveAnchor(
        anchor: Anchor?
    ): ResolvedAnchor {
        if (anchor == null) {
            return ResolvedAnchor.empty()
        }

        return when (anchor.trackingState) {
            TrackingState.TRACKING -> {
                ResolvedAnchor(
                    point = anchor.pose.toPoint3D(),
                    keepPreviousPoint = false,
                    removeAnchor = false
                )
            }

            TrackingState.PAUSED -> {
                /*
                 * Mantém a última posição conhecida enquanto
                 * o ARCore tenta recuperar o rastreamento.
                 */
                ResolvedAnchor(
                    point = null,
                    keepPreviousPoint = true,
                    removeAnchor = false
                )
            }

            TrackingState.STOPPED -> {
                /*
                 * Uma âncora STOPPED não voltará a rastrear.
                 */
                ResolvedAnchor(
                    point = null,
                    keepPreviousPoint = false,
                    removeAnchor = true
                )
            }
        }
    }

    private fun currentTrackingFrame(): Frame? {
        val frame = currentFrame ?: return null

        return frame.takeIf {
            it.camera.trackingState == TrackingState.TRACKING
        }
    }

    private fun normalizedToPixels(
        normalizedX: Float,
        normalizedY: Float
    ): PixelCoordinates? {
        if (
            !normalizedX.isFinite() ||
            !normalizedY.isFinite()
        ) {
            return null
        }

        if (
            normalizedX !in NORMALIZED_RANGE ||
            normalizedY !in NORMALIZED_RANGE
        ) {
            return null
        }

        val width = viewportWidthPx
        val height = viewportHeightPx

        if (width <= 0 || height <= 0) {
            return null
        }

        /*
         * width - 1 e height - 1 evitam produzir uma coordenada
         * fora da View quando o valor normalizado for exatamente 1.
         */
        return PixelCoordinates(
            x = normalizedX * (width - 1).coerceAtLeast(0),
            y = normalizedY * (height - 1).coerceAtLeast(0)
        )
    }

    private fun Camera.toDomainTrackingStatus(): TrackingStatus {
        return when (trackingState) {
            TrackingState.TRACKING -> {
                TrackingStatus.TRACKING
            }

            TrackingState.STOPPED -> {
                TrackingStatus.UNAVAILABLE
            }

            TrackingState.PAUSED -> {
                trackingFailureReason.toDomainTrackingStatus()
            }
        }
    }

    private fun TrackingFailureReason.toDomainTrackingStatus():
            TrackingStatus {
        return when (this) {
            TrackingFailureReason.NONE -> {
                TrackingStatus.INITIALIZING
            }

            TrackingFailureReason.EXCESSIVE_MOTION -> {
                TrackingStatus.EXCESSIVE_MOTION
            }

            TrackingFailureReason.INSUFFICIENT_FEATURES -> {
                TrackingStatus.INSUFFICIENT_FEATURES
            }

            TrackingFailureReason.INSUFFICIENT_LIGHT -> {
                TrackingStatus.INSUFFICIENT_LIGHT
            }

            TrackingFailureReason.CAMERA_UNAVAILABLE -> {
                TrackingStatus.CAMERA_UNAVAILABLE
            }

            TrackingFailureReason.BAD_STATE -> {
                TrackingStatus.UNAVAILABLE
            }

            else -> {
                /*
                 * Protege o aplicativo caso uma versão futura
                 * do ARCore acrescente uma nova razão.
                 */
                TrackingStatus.UNAVAILABLE
            }
        }
    }

    private fun Pose.toPoint3D(): Point3D {
        return Point3D(
            x = tx(),
            y = ty(),
            z = tz()
        )
    }

    private fun ResolvedAnchor.resolve(
        previousPoint: Point3D?
    ): Point3D? {
        return if (keepPreviousPoint) {
            previousPoint
        } else {
            point
        }
    }

    private data class PixelCoordinates(
        val x: Float,
        val y: Float
    )

    private data class AnchorSnapshot(
        val start: ResolvedAnchor,
        val end: ResolvedAnchor
    )

    private data class ResolvedAnchor(
        val point: Point3D?,
        val keepPreviousPoint: Boolean,
        val removeAnchor: Boolean
    ) {
        companion object {
            fun empty(): ResolvedAnchor {
                return ResolvedAnchor(
                    point = null,
                    keepPreviousPoint = false,
                    removeAnchor = false
                )
            }
        }
    }

    private companion object {
        val NORMALIZED_RANGE = 0f..1f
    }
}