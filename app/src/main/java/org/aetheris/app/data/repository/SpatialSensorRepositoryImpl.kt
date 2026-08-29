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

    override val spatialDataStream: StateFlow<SpatialFrameData> =
        _spatialDataStream.asStateFlow()

    private val anchorLock = Any()

    @Volatile
    private var currentFrame: Frame? = null

    @Volatile
    private var viewportSize: ViewportSize? = null

    private var startAnchor: Anchor? = null
    private var endAnchor: Anchor? = null

    /**
     * Atualiza as dimensões utilizadas para converter
     * coordenadas normalizadas em pixels.
     *
     * Deve ser chamado quando a superfície gráfica
     * for criada ou redimensionada.
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

        viewportSize = ViewportSize(
            widthPx = widthPx,
            heightPx = heightPx
        )
    }

    /**
     * Atualiza o estado espacial utilizando o frame
     * mais recente do ARCore.
     *
     * Deve ser chamado a cada quadro na thread
     * responsável pela renderização.
     */
    fun onFrameUpdate(frame: Frame) {
        currentFrame = frame

        val camera = frame.camera
        val trackingStatus =
            camera.toDomainTrackingStatus()

        val isTracking =
            camera.trackingState == TrackingState.TRACKING

        val points = if (isTracking) {
            frameProcessor.processPointCloud(frame)
        } else {
            emptyList()
        }

        val centerPixels = normalizedToPixels(
            normalizedX = CENTER_NORMALIZED_COORDINATE,
            normalizedY = CENTER_NORMALIZED_COORDINATE
        )

        val isSurfaceDetected =
            if (isTracking && centerPixels != null) {
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
                isDepthEnabled =
                    isDepthEnabledProvider(),
                pointCount = points.size,
                isSurfaceDetected =
                    isSurfaceDetected,
                anchoredStartPoint =
                    anchorSnapshot.start.resolve(
                        previousPoint =
                            current.anchoredStartPoint
                    ),
                anchoredEndPoint =
                    anchorSnapshot.end.resolve(
                        previousPoint =
                            current.anchoredEndPoint
                    )
            )
        }
    }

    override suspend fun performHitTest(
        normalizedX: Float,
        normalizedY: Float
    ): Point3D? {
        val frame =
            currentTrackingFrame() ?: return null

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
        val frame =
            currentTrackingFrame() ?: return null

        val pixelCoordinates = normalizedToPixels(
            normalizedX = normalizedX,
            normalizedY = normalizedY
        ) ?: return null

        val newAnchor =
            hitTestProcessor.createAnchorAt(
                frame = frame,
                xPx = pixelCoordinates.x,
                yPx = pixelCoordinates.y
            ) ?: return null

        val point = try {
            newAnchor.pose.toPoint3D()
        } catch (_: RuntimeException) {
            newAnchor.safeDetach()
            return null
        }

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
        val anchorsToDetach =
            synchronized(anchorLock) {
                val anchors = listOfNotNull(
                    startAnchor,
                    endAnchor
                )

                startAnchor = null
                endAnchor = null

                anchors
            }

        anchorsToDetach.forEach { anchor ->
            anchor.safeDetach()
        }

        _spatialDataStream.update { current ->
            current.copy(
                anchoredStartPoint = null,
                anchoredEndPoint = null
            )
        }
    }

    /**
     * Libera os recursos mantidos pelo repositório.
     *
     * Deve ser chamado antes de destruir
     * a sessão do ARCore.
     */
    fun release() {
        clearAnchors()

        currentFrame = null
        viewportSize = null

        _spatialDataStream.update { current ->
            current.copy(
                trackingStatus =
                    TrackingStatus.UNAVAILABLE,
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
        val previousAnchor =
            synchronized(anchorLock) {
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
            previousAnchor?.safeDetach()
        }
    }

    private fun resolveAnchors(): AnchorSnapshot {
        val anchorsToDetach =
            mutableListOf<Anchor>()

        val snapshot =
            synchronized(anchorLock) {
                val resolvedStart =
                    resolveAnchor(startAnchor)

                val resolvedEnd =
                    resolveAnchor(endAnchor)

                if (resolvedStart.removeAnchor) {
                    startAnchor?.let(
                        anchorsToDetach::add
                    )

                    startAnchor = null
                }

                if (resolvedEnd.removeAnchor) {
                    endAnchor?.let(
                        anchorsToDetach::add
                    )

                    endAnchor = null
                }

                AnchorSnapshot(
                    start = resolvedStart,
                    end = resolvedEnd
                )
            }

        anchorsToDetach.forEach { anchor ->
            anchor.safeDetach()
        }

        return snapshot
    }

    private fun resolveAnchor(
        anchor: Anchor?
    ): ResolvedAnchor {
        if (anchor == null) {
            return ResolvedAnchor.empty()
        }

        return try {
            when (anchor.trackingState) {
                TrackingState.TRACKING -> {
                    ResolvedAnchor(
                        point =
                            anchor.pose.toPoint3D(),
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
                     * Uma âncora parada não voltará
                     * a ser rastreada.
                     */
                    ResolvedAnchor(
                        point = null,
                        keepPreviousPoint = false,
                        removeAnchor = true
                    )
                }
            }
        } catch (_: RuntimeException) {
            ResolvedAnchor(
                point = null,
                keepPreviousPoint = false,
                removeAnchor = true
            )
        }
    }

    private fun currentTrackingFrame(): Frame? {
        val frame = currentFrame ?: return null

        return try {
            frame.takeIf {
                it.camera.trackingState ==
                        TrackingState.TRACKING
            }
        } catch (_: RuntimeException) {
            null
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

        val currentViewport =
            viewportSize ?: return null

        val maximumX =
            (currentViewport.widthPx - 1)
                .coerceAtLeast(0)

        val maximumY =
            (currentViewport.heightPx - 1)
                .coerceAtLeast(0)

        return PixelCoordinates(
            x = normalizedX * maximumX,
            y = normalizedY * maximumY
        )
    }

    private fun Camera.toDomainTrackingStatus():
            TrackingStatus {
        return when (trackingState) {
            TrackingState.TRACKING -> {
                TrackingStatus.TRACKING
            }

            TrackingState.STOPPED -> {
                TrackingStatus.UNAVAILABLE
            }

            TrackingState.PAUSED -> {
                trackingFailureReason
                    .toDomainTrackingStatus()
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

    private fun Anchor.safeDetach() {
        try {
            detach()
        } catch (_: RuntimeException) {
            /*
             * A âncora pode já ter sido liberada
             * pela sessão do ARCore.
             */
        }
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

    private data class ViewportSize(
        val widthPx: Int,
        val heightPx: Int
    )

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
        const val CENTER_NORMALIZED_COORDINATE = 0.5f

        val NORMALIZED_RANGE = 0f..1f
    }
}