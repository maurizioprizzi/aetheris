package org.aetheris.app.data.repository

import com.google.ar.core.Anchor
import com.google.ar.core.Frame
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
    private val hitTestProcessor: ArCoreHitTestProcessor
) : SpatialSensorRepository {

    private val _spatialDataStream = MutableStateFlow(SpatialFrameData())
    private var currentFrame: Frame? = null

    private var startAnchor: Anchor? = null
    private var endAnchor: Anchor? = null

    override fun getSpatialDataStream(): StateFlow<SpatialFrameData> = _spatialDataStream.asStateFlow()

    override fun updateFrameData(frame: Frame) {
        this.currentFrame = frame
        val trackingStatus = when (frame.camera.trackingState) {
            TrackingState.TRACKING -> TrackingStatus.TRACKING
            TrackingState.PAUSED -> TrackingStatus.INITIALIZING
            else -> TrackingStatus.UNAVAILABLE
        }

        val pointCloudData = frameProcessor.processPointCloud(frame)

        val resolvedStart = startAnchor?.takeIf { it.trackingState == TrackingState.TRACKING }?.pose?.let {
            Point3D(it.tx(), it.ty(), it.tz())
        }

        val resolvedEnd = endAnchor?.takeIf { it.trackingState == TrackingState.TRACKING }?.pose?.let {
            Point3D(it.tx(), it.ty(), it.tz())
        }

        _spatialDataStream.update { current ->
            current.copy(
                trackingStatus = trackingStatus,
                isDepthEnabled = frame.camera.trackingState == TrackingState.TRACKING,
                pointCount = pointCloudData.size,
                isSurfaceDetected = hitTestProcessor.hasValidSurfaceAt(frame, 0.5f, 0.5f),
                anchoredStartPoint = resolvedStart ?: current.anchoredStartPoint,
                anchoredEndPoint = resolvedEnd ?: current.anchoredEndPoint
            )
        }
    }

    override suspend fun performHitTest(normalizedX: Float, normalizedY: Float): Point3D? {
        val frame = currentFrame ?: return null
        return hitTestProcessor.performHitTest(frame, normalizedX, normalizedY)
    }

    override suspend fun createAnchor(normalizedX: Float, normalizedY: Float, slot: AnchorSlot): Point3D? {
        val frame = currentFrame ?: return null
        val anchor = hitTestProcessor.createAnchorAt(frame, normalizedX, normalizedY) ?: return null

        when (slot) {
            AnchorSlot.START -> {
                startAnchor?.detach()
                startAnchor = anchor
            }
            AnchorSlot.END -> {
                endAnchor?.detach()
                endAnchor = anchor
            }
        }

        val pose = anchor.pose
        val point = Point3D(pose.tx(), pose.ty(), pose.tz())

        _spatialDataStream.update { current ->
            when (slot) {
                AnchorSlot.START -> current.copy(anchoredStartPoint = point)
                AnchorSlot.END -> current.copy(anchoredEndPoint = point)
            }
        }

        return point
    }

    override fun clearAnchors() {
        startAnchor?.detach()
        startAnchor = null
        endAnchor?.detach()
        endAnchor = null

        _spatialDataStream.update { current ->
            current.copy(
                anchoredStartPoint = null,
                anchoredEndPoint = null
            )
        }
    }
}
