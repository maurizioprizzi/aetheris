package org.aetheris.app.data.repository

import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import org.aetheris.app.data.arcore.ArCoreFrameProcessor
import org.aetheris.app.data.arcore.ArCoreHitTestProcessor
import org.aetheris.app.domain.model.AnchorPlacementSource
import org.aetheris.app.domain.model.AnchorSlot
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.SpatialFrameData
import org.aetheris.app.domain.model.TrackingStatus
import org.aetheris.app.domain.repository.SpatialSensorRepository
import java.util.ArrayDeque

class SpatialSensorRepositoryImpl(
    private val frameProcessor: ArCoreFrameProcessor,
    private val hitTestProcessor: ArCoreHitTestProcessor,
    private val isDepthEnabledProvider: () -> Boolean,
    private val surfaceProbeIntervalNanos: Long =
        DEFAULT_SURFACE_PROBE_INTERVAL_NANOS,
    private val nanoTimeProvider: () -> Long =
        System::nanoTime
) : SpatialSensorRepository {

    init {
        require(surfaceProbeIntervalNanos >= 0L) {
            "O intervalo de detecção de superfície não pode ser negativo."
        }
    }

    private val _spatialDataStream =
        MutableStateFlow(
            SpatialFrameData()
        )

    override val spatialDataStream:
            StateFlow<SpatialFrameData> =
        _spatialDataStream.asStateFlow()

    private val anchorLock = Any()
    private val requestLock = Any()

    private val pendingFrameRequests =
        ArrayDeque<FrameRequest>()

    private val activeFrameRequests =
        mutableSetOf<FrameRequest>()

    @Volatile
    private var viewportSize: ViewportSize? = null

    /**
     * Cada slot mantém a âncora nativa junto da origem
     * espacial usada durante sua criação.
     */
    private var startAnchor: ActiveAnchor? = null
    private var endAnchor: ActiveAnchor? = null

    private var lastSurfaceProbeNanos: Long =
        NO_SURFACE_PROBE

    private var lastSurfaceDetected: Boolean = false

    /**
     * Atualiza as dimensões utilizadas para converter
     * coordenadas normalizadas em pixels.
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

        resetSurfaceProbe()
    }

    /**
     * Atualiza o estado espacial usando o frame atual.
     *
     * Hit tests e criações de âncora solicitados pela UI
     * são executados aqui, na mesma thread que processa os
     * frames do ARCore.
     */
    fun onFrameUpdate(
        frame: Frame
    ) {
        val camera = try {
            frame.camera
        } catch (_: RuntimeException) {
            completePendingRequestsWithNull()
            publishUnavailableFrame()
            return
        }

        val trackingStatus =
            camera.toDomainTrackingStatus()

        val isTracking =
            camera.trackingState ==
                    TrackingState.TRACKING

        processPendingFrameRequests(
            frame = frame,
            isTracking = isTracking
        )

        val points = if (isTracking) {
            frameProcessor.processPointCloud(
                frame = frame
            )
        } else {
            emptyList()
        }

        val centerPixels =
            normalizedToPixels(
                normalizedX =
                    CENTER_NORMALIZED_COORDINATE,
                normalizedY =
                    CENTER_NORMALIZED_COORDINATE
            )

        val isSurfaceDetected =
            resolveSurfaceDetection(
                frame = frame,
                isTracking = isTracking,
                centerPixels = centerPixels
            )

        val anchorSnapshot =
            resolveAnchors()

        _spatialDataStream.update { current ->
            val resolvedStartPoint =
                anchorSnapshot.start.resolvePoint(
                    previousPoint =
                        current.anchoredStartPoint
                )

            val resolvedEndPoint =
                anchorSnapshot.end.resolvePoint(
                    previousPoint =
                        current.anchoredEndPoint
                )

            current.copy(
                trackingStatus =
                    trackingStatus,
                isDepthEnabled =
                    isDepthEnabledProvider(),
                pointCount =
                    points.size,
                isSurfaceDetected =
                    isSurfaceDetected,
                anchoredStartPoint =
                    resolvedStartPoint,
                anchoredEndPoint =
                    resolvedEndPoint,
                anchoredStartSource =
                    anchorSnapshot.start.resolveSource(
                        resolvedPoint =
                            resolvedStartPoint
                    ),
                anchoredEndSource =
                    anchorSnapshot.end.resolveSource(
                        resolvedPoint =
                            resolvedEndPoint
                    )
            )
        }
    }

    override suspend fun performHitTest(
        normalizedX: Float,
        normalizedY: Float
    ): Point3D? {
        if (
            !areValidNormalizedCoordinates(
                normalizedX = normalizedX,
                normalizedY = normalizedY
            )
        ) {
            return null
        }

        if (viewportSize == null) {
            return null
        }

        val request = HitTestRequest(
            normalizedX = normalizedX,
            normalizedY = normalizedY
        )

        return enqueueAndAwait(request)
    }

    override suspend fun createAnchor(
        normalizedX: Float,
        normalizedY: Float,
        slot: AnchorSlot
    ): Point3D? {
        if (
            !areValidNormalizedCoordinates(
                normalizedX = normalizedX,
                normalizedY = normalizedY
            )
        ) {
            return null
        }

        if (viewportSize == null) {
            return null
        }

        val request = CreateAnchorRequest(
            normalizedX = normalizedX,
            normalizedY = normalizedY,
            slot = slot
        )

        return enqueueAndAwait(request)
    }

    override fun clearAnchors() {
        completePendingRequestsWithNull()

        val anchorsToDetach =
            synchronized(anchorLock) {
                val anchors =
                    listOfNotNull(
                        startAnchor,
                        endAnchor
                    ).map { activeAnchor ->
                        activeAnchor.anchor
                    }

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
                anchoredEndPoint = null,
                anchoredStartSource = null,
                anchoredEndSource = null
            )
        }
    }

    /**
     * Libera os recursos mantidos pelo repositório.
     */
    fun release() {
        clearAnchors()

        viewportSize = null
        resetSurfaceProbe()

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

    private suspend fun enqueueAndAwait(
        request: FrameRequest
    ): Point3D? {
        synchronized(requestLock) {
            pendingFrameRequests.addLast(request)
        }

        return try {
            withTimeoutOrNull(
                FRAME_REQUEST_TIMEOUT_MILLIS
            ) {
                request.completion.await()
            }
        } finally {
            if (!request.completion.isCompleted) {
                request.completion.complete(null)
            }

            synchronized(requestLock) {
                pendingFrameRequests.remove(request)
            }
        }
    }

    private fun processPendingFrameRequests(
        frame: Frame,
        isTracking: Boolean
    ) {
        while (true) {
            val request =
                takeNextFrameRequest()
                    ?: return

            try {
                if (!request.completion.isActive) {
                    continue
                }

                if (!isTracking) {
                    request.completion.complete(null)
                    continue
                }

                when (request) {
                    is HitTestRequest -> {
                        processHitTestRequest(
                            frame = frame,
                            request = request
                        )
                    }

                    is CreateAnchorRequest -> {
                        processCreateAnchorRequest(
                            frame = frame,
                            request = request
                        )
                    }
                }
            } finally {
                synchronized(requestLock) {
                    activeFrameRequests.remove(request)
                }
            }
        }
    }

    private fun takeNextFrameRequest():
            FrameRequest? {
        return synchronized(requestLock) {
            val request =
                pendingFrameRequests.pollFirst()
                    ?: return@synchronized null

            activeFrameRequests.add(request)
            request
        }
    }

    private fun processHitTestRequest(
        frame: Frame,
        request: HitTestRequest
    ) {
        val pixelCoordinates =
            normalizedToPixels(
                normalizedX = request.normalizedX,
                normalizedY = request.normalizedY
            )

        if (pixelCoordinates == null) {
            request.completion.complete(null)
            return
        }

        val point =
            hitTestProcessor.performHitTest(
                frame = frame,
                xPx = pixelCoordinates.x,
                yPx = pixelCoordinates.y
            )

        request.completion.complete(point)
    }

    private fun processCreateAnchorRequest(
        frame: Frame,
        request: CreateAnchorRequest
    ) {
        val pixelCoordinates =
            normalizedToPixels(
                normalizedX = request.normalizedX,
                normalizedY = request.normalizedY
            )

        if (pixelCoordinates == null) {
            request.completion.complete(null)
            return
        }

        val nativePlacement =
            hitTestProcessor.createAnchorAtWithSource(
                frame = frame,
                xPx = pixelCoordinates.x,
                yPx = pixelCoordinates.y
            )

        if (nativePlacement == null) {
            request.completion.complete(null)
            return
        }

        val activeAnchor =
            ActiveAnchor(
                anchor = nativePlacement.anchor,
                source = nativePlacement.source
            )

        val point = try {
            activeAnchor.anchor.pose.toPoint3D()
        } catch (_: RuntimeException) {
            activeAnchor.anchor.safeDetach()
            request.completion.complete(null)
            return
        }

        if (!request.completion.isActive) {
            activeAnchor.anchor.safeDetach()
            return
        }

        replaceAnchor(
            slot = request.slot,
            newAnchor = activeAnchor
        )

        updateAnchorPlacement(
            slot = request.slot,
            point = point,
            source = activeAnchor.source
        )

        if (!request.completion.complete(point)) {
            rollbackAnchor(
                slot = request.slot,
                anchor = activeAnchor
            )
        }
    }

    private fun completePendingRequestsWithNull() {
        val requests =
            synchronized(requestLock) {
                val snapshot =
                    buildList {
                        addAll(pendingFrameRequests)
                        addAll(activeFrameRequests)
                    }

                pendingFrameRequests.clear()
                snapshot
            }

        requests.forEach { request ->
            request.completion.complete(null)
        }
    }

    private fun resolveSurfaceDetection(
        frame: Frame,
        isTracking: Boolean,
        centerPixels: PixelCoordinates?
    ): Boolean {
        if (!isTracking || centerPixels == null) {
            resetSurfaceProbe()
            return false
        }

        val now =
            nanoTimeProvider()

        val shouldProbe =
            lastSurfaceProbeNanos ==
                    NO_SURFACE_PROBE ||
                    now < lastSurfaceProbeNanos ||
                    now - lastSurfaceProbeNanos >=
                    surfaceProbeIntervalNanos

        if (!shouldProbe) {
            return lastSurfaceDetected
        }

        lastSurfaceProbeNanos = now
        lastSurfaceDetected =
            hitTestProcessor.hasValidSurfaceAt(
                frame = frame,
                xPx = centerPixels.x,
                yPx = centerPixels.y
            )

        return lastSurfaceDetected
    }

    private fun resetSurfaceProbe() {
        lastSurfaceProbeNanos = NO_SURFACE_PROBE
        lastSurfaceDetected = false
    }

    private fun publishUnavailableFrame() {
        resetSurfaceProbe()

        _spatialDataStream.update { current ->
            current.copy(
                trackingStatus =
                    TrackingStatus.UNAVAILABLE,
                pointCount = 0,
                isSurfaceDetected = false
            )
        }
    }

    /**
     * Atualiza posição e origem de forma atômica para manter
     * as invariantes de [SpatialFrameData].
     */
    private fun updateAnchorPlacement(
        slot: AnchorSlot,
        point: Point3D?,
        source: AnchorPlacementSource?
    ) {
        _spatialDataStream.update { current ->
            when (slot) {
                AnchorSlot.START -> {
                    current.copy(
                        anchoredStartPoint = point,
                        anchoredStartSource = source
                    )
                }

                AnchorSlot.END -> {
                    current.copy(
                        anchoredEndPoint = point,
                        anchoredEndSource = source
                    )
                }
            }
        }
    }

    private fun replaceAnchor(
        slot: AnchorSlot,
        newAnchor: ActiveAnchor
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
            previousAnchor?.anchor?.safeDetach()
        }
    }

    private fun rollbackAnchor(
        slot: AnchorSlot,
        anchor: ActiveAnchor
    ) {
        val removed =
            synchronized(anchorLock) {
                when (slot) {
                    AnchorSlot.START -> {
                        if (startAnchor === anchor) {
                            startAnchor = null
                            true
                        } else {
                            false
                        }
                    }

                    AnchorSlot.END -> {
                        if (endAnchor === anchor) {
                            endAnchor = null
                            true
                        } else {
                            false
                        }
                    }
                }
            }

        if (removed) {
            anchor.anchor.safeDetach()

            updateAnchorPlacement(
                slot = slot,
                point = null,
                source = null
            )
        }
    }

    private fun resolveAnchors():
            AnchorSnapshot {
        val anchorsToDetach =
            mutableListOf<Anchor>()

        val snapshot =
            synchronized(anchorLock) {
                val resolvedStart =
                    resolveAnchor(
                        activeAnchor = startAnchor
                    )

                val resolvedEnd =
                    resolveAnchor(
                        activeAnchor = endAnchor
                    )

                if (resolvedStart.removeAnchor) {
                    startAnchor?.anchor?.let(
                        anchorsToDetach::add
                    )
                    startAnchor = null
                }

                if (resolvedEnd.removeAnchor) {
                    endAnchor?.anchor?.let(
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
        activeAnchor: ActiveAnchor?
    ): ResolvedAnchor {
        if (activeAnchor == null) {
            return ResolvedAnchor.empty()
        }

        return try {
            when (activeAnchor.anchor.trackingState) {
                TrackingState.TRACKING -> {
                    ResolvedAnchor(
                        point =
                            activeAnchor.anchor.pose
                                .toPoint3D(),
                        source =
                            activeAnchor.source,
                        keepPreviousPoint = false,
                        removeAnchor = false
                    )
                }

                TrackingState.PAUSED -> {
                    ResolvedAnchor(
                        point = null,
                        source =
                            activeAnchor.source,
                        keepPreviousPoint = true,
                        removeAnchor = false
                    )
                }

                TrackingState.STOPPED -> {
                    ResolvedAnchor(
                        point = null,
                        source = null,
                        keepPreviousPoint = false,
                        removeAnchor = true
                    )
                }
            }
        } catch (_: RuntimeException) {
            ResolvedAnchor(
                point = null,
                source = null,
                keepPreviousPoint = false,
                removeAnchor = true
            )
        }
    }

    private fun normalizedToPixels(
        normalizedX: Float,
        normalizedY: Float
    ): PixelCoordinates? {
        if (
            !areValidNormalizedCoordinates(
                normalizedX = normalizedX,
                normalizedY = normalizedY
            )
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

    private fun areValidNormalizedCoordinates(
        normalizedX: Float,
        normalizedY: Float
    ): Boolean {
        return normalizedX.isFinite() &&
                normalizedY.isFinite() &&
                normalizedX in NORMALIZED_RANGE &&
                normalizedY in NORMALIZED_RANGE
    }

    private fun Camera.toDomainTrackingStatus():
            TrackingStatus {
        return try {
            when (trackingState) {
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
        } catch (_: RuntimeException) {
            TrackingStatus.UNAVAILABLE
        }
    }

    private fun TrackingFailureReason
            .toDomainTrackingStatus():
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
        }
    }

    private fun Pose.toPoint3D():
            Point3D {
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
            // A âncora pode já ter sido liberada pelo ARCore.
        }
    }

    private fun ResolvedAnchor.resolvePoint(
        previousPoint: Point3D?
    ): Point3D? {
        return if (keepPreviousPoint) {
            previousPoint
        } else {
            point
        }
    }

    private fun ResolvedAnchor.resolveSource(
        resolvedPoint: Point3D?
    ): AnchorPlacementSource? {
        if (resolvedPoint == null) {
            return null
        }

        return source
    }

    private sealed interface FrameRequest {
        val completion: CompletableDeferred<Point3D?>
    }

    private class HitTestRequest(
        val normalizedX: Float,
        val normalizedY: Float,
        override val completion:
        CompletableDeferred<Point3D?> =
            CompletableDeferred()
    ) : FrameRequest

    private class CreateAnchorRequest(
        val normalizedX: Float,
        val normalizedY: Float,
        val slot: AnchorSlot,
        override val completion:
        CompletableDeferred<Point3D?> =
            CompletableDeferred()
    ) : FrameRequest

    private data class ActiveAnchor(
        val anchor: Anchor,
        val source: AnchorPlacementSource
    )

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
        val source: AnchorPlacementSource?,
        val keepPreviousPoint: Boolean,
        val removeAnchor: Boolean
    ) {

        companion object {
            fun empty(): ResolvedAnchor {
                return ResolvedAnchor(
                    point = null,
                    source = null,
                    keepPreviousPoint = false,
                    removeAnchor = false
                )
            }
        }
    }

    private companion object {
        const val CENTER_NORMALIZED_COORDINATE =
            0.5f

        const val DEFAULT_SURFACE_PROBE_INTERVAL_NANOS =
            200_000_000L

        const val FRAME_REQUEST_TIMEOUT_MILLIS =
            2_000L

        const val NO_SURFACE_PROBE =
            Long.MIN_VALUE

        val NORMALIZED_RANGE = 0f..1f
    }
}
