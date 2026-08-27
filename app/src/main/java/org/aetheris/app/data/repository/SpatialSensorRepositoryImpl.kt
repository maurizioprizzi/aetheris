package org.aetheris.app.data.repository

import com.google.ar.core.Frame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.aetheris.app.data.arcore.ArCoreHitTestProcessor
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.SpatialFrameData
import org.aetheris.app.domain.model.TrackingStatus
import org.aetheris.app.domain.repository.SpatialSensorRepository

class SpatialSensorRepositoryImpl(
    private val hitTestProcessor: ArCoreHitTestProcessor = ArCoreHitTestProcessor()
) : SpatialSensorRepository {

    private val _spatialFrameStream = MutableStateFlow(SpatialFrameData())
    override val spatialFrameStream: StateFlow<SpatialFrameData> = _spatialFrameStream.asStateFlow()

    @Volatile
    private var currentArFrame: Frame? = null

    @Volatile
    private var viewportWidth: Int = 1080

    @Volatile
    private var viewportHeight: Int = 1920

    /**
     * Atualiza as dimensões ativas do viewport renderizado pelo OpenGL / GLSurfaceView.
     */
    fun updateViewportDimensions(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            this.viewportWidth = width
            this.viewportHeight = height
        }
    }

    /**
     * Vincula o frame óptico mais recente processado na thread de renderização.
     */
    fun setLatestArFrame(frame: Frame?) {
        this.currentArFrame = frame
    }

    override fun updateFrame(
        cameraPosition: Point3D,
        points: List<Point3D>,
        status: TrackingStatus,
        hasDepth: Boolean
    ) {
        _spatialFrameStream.update {
            SpatialFrameData(
                cameraPose = cameraPosition,
                pointCloud = points,
                trackingStatus = status,
                isDepthAvailable = hasDepth,
                timestampMillis = System.currentTimeMillis()
            )
        }
    }

    override fun performHitTest(normalizedX: Float, normalizedY: Float): Point3D? {
        return hitTestProcessor.performRaycast(
            frame = currentArFrame,
            normalizedX = normalizedX,
            normalizedY = normalizedY,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight
        )
    }

    override fun resetTracking() {
        currentArFrame = null
        _spatialFrameStream.value = SpatialFrameData()
    }
}