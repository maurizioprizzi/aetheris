package org.aetheris.app.data.arcore

import com.google.ar.core.Frame
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.core.exceptions.ResourceExhaustedException
import org.aetheris.app.domain.model.Point3D

/**
 * Extrai e filtra a nuvem de pontos produzida
 * pelo frame mais recente do ARCore.
 */
class ArCoreFrameProcessor(
    private val confidenceThreshold: Float =
        DEFAULT_CONFIDENCE_THRESHOLD
) {

    init {
        require(
            confidenceThreshold.isFinite() &&
                    confidenceThreshold in VALID_CONFIDENCE_RANGE
        ) {
            "O limite de confiança deve estar entre 0 e 1."
        }
    }

    /**
     * Retorna os pontos com coordenadas finitas e confiança
     * igual ou superior ao limite configurado.
     *
     * A nuvem de pontos adquirida é sempre liberada por
     * meio de PointCloud.close().
     */
    fun processPointCloud(
        frame: Frame
    ): List<Point3D> {
        return try {
            frame.acquirePointCloud().use { pointCloud ->
                val buffer =
                    pointCloud.points.duplicate()

                val pointCount =
                    buffer.remaining() /
                            VALUES_PER_POINT

                val points =
                    ArrayList<Point3D>(
                        pointCount
                    )

                repeat(pointCount) {
                    val x = buffer.get()
                    val y = buffer.get()
                    val z = buffer.get()
                    val confidence =
                        buffer.get()

                    if (
                        isValidPoint(
                            x = x,
                            y = y,
                            z = z,
                            confidence = confidence
                        )
                    ) {
                        points.add(
                            Point3D(
                                x = x,
                                y = y,
                                z = z
                            )
                        )
                    }
                }

                points
            }
        } catch (_: DeadlineExceededException) {
            emptyList()
        } catch (_: NotYetAvailableException) {
            emptyList()
        } catch (_: ResourceExhaustedException) {
            emptyList()
        }
    }

    private fun isValidPoint(
        x: Float,
        y: Float,
        z: Float,
        confidence: Float
    ): Boolean {
        return x.isFinite() &&
                y.isFinite() &&
                z.isFinite() &&
                confidence.isFinite() &&
                confidence >= confidenceThreshold
    }

    private companion object {
        const val VALUES_PER_POINT = 4
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.3f

        val VALID_CONFIDENCE_RANGE = 0f..1f
    }
}