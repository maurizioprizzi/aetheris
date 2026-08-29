package org.aetheris.app.domain.repository

import com.google.ar.core.Frame
import kotlinx.coroutines.flow.StateFlow
import org.aetheris.app.domain.model.AnchorSlot
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.SpatialFrameData

interface SpatialSensorRepository {
    fun getSpatialDataStream(): StateFlow<SpatialFrameData>
    fun updateFrameData(frame: Frame)
    suspend fun performHitTest(normalizedX: Float, normalizedY: Float): Point3D?
    suspend fun createAnchor(normalizedX: Float, normalizedY: Float, slot: AnchorSlot): Point3D?
    fun clearAnchors()
}