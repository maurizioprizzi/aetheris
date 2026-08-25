package org.aetheris.app.data.arcore

import org.aetheris.app.domain.model.Point3D
import java.nio.FloatBuffer

/**
 * Processador de baixo nível responsável por converter buffers brutos do ARCore
 * em entidades matemáticas imutáveis do domínio com filtro de confiança.
 */
class ArCoreFrameProcessor(
    private val minConfidenceThreshold: Float = 0.3f
) {
    /**
     * Extrai pontos 3D a partir do FloatBuffer de PointCloud do ARCore.
     * Estrutura do buffer do ARCore: [x, y, z, confidence, x, y, z, confidence, ...]
     */
    fun extractPointCloud(buffer: FloatBuffer?): List<Point3D> {
        if (buffer == null || buffer.remaining() == 0) return emptyList()

        val points = mutableListOf<Point3D>()
        val floatArray = FloatArray(buffer.remaining())
        val duplicate = buffer.duplicate()
        duplicate.get(floatArray)

        var index = 0
        while (index + 3 < floatArray.size) {
            val x = floatArray[index]
            val y = floatArray[index + 1]
            val z = floatArray[index + 2]
            val confidence = floatArray[index + 3]

            if (confidence >= minConfidenceThreshold) {
                points.add(Point3D(x = x, y = y, z = z))
            }
            index += 4
        }

        return points
    }
}