package org.aetheris.app.data.arcore

import com.google.ar.core.Frame
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.core.exceptions.NotYetAvailableException
import org.aetheris.app.domain.model.Point3D

class ArCoreFrameProcessor(
    private val confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
) {
    init {
        require(
            confidenceThreshold.isFinite() &&
                    confidenceThreshold in 0f..1f
        ) {
            "O limite de confiança deve estar entre 0 e 1."
        }
    }

    fun processPointCloud(frame: Frame): List<Point3D> {
        return try {
            frame.acquirePointCloud().use { pointCloud ->
                val buffer = pointCloud.points.duplicate()
                val pointCount = buffer.remaining() / VALUES_PER_POINT
                val points = ArrayList<Point3D>(pointCount)

                repeat(pointCount) {
                    val x = buffer.get()
                    val y = buffer.get()
                    val z = buffer.get()
                    val confidence = buffer.get()

                    if (
                        confidence.isFinite() &&
                        confidence >= confidenceThreshold &&
                        x.isFinite() &&
                        y.isFinite() &&
                        z.isFinite()
                    ) {
                        points.add(Point3D(x = x, y = y, z = z))
                    }
                }

                points
            }
        } catch (_: DeadlineExceededException) {
            emptyList()
        } catch (_: NotYetAvailableException) {
            emptyList()
        }
    }

    private companion object {
        const val VALUES_PER_POINT = 4
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.3f
    }
}