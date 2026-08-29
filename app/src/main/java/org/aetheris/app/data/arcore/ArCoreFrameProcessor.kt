package org.aetheris.app.data.arcore

import com.google.ar.core.Frame
import com.google.ar.core.PointCloud
import org.aetheris.app.domain.model.Point3D
import java.nio.FloatBuffer

class ArCoreFrameProcessor(
    private val confidenceThreshold: Float = 0.3f
) {
    fun processPointCloud(frame: Frame): List<Point3D> {
        val pointCloud: PointCloud = frame.acquirePointCloud() ?: return emptyList()
        val points = mutableListOf<Point3D>()
        try {
            val buffer: FloatBuffer = pointCloud.points ?: return emptyList()
            val numPoints = buffer.remaining() / 4

            for (i in 0 until numPoints) {
                val x = buffer.get(i * 4)
                val y = buffer.get(i * 4 + 1)
                val z = buffer.get(i * 4 + 2)
                val confidence = buffer.get(i * 4 + 3)

                if (confidence >= confidenceThreshold) {
                    points.add(Point3D(x, y, z))
                }
            }
        } finally {
            pointCloud.release()
        }
        return points
    }
}