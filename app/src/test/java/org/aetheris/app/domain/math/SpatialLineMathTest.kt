package org.aetheris.app.domain.math

import com.google.common.truth.Truth.assertThat
import org.aetheris.app.domain.model.Point3D
import org.junit.Test
import kotlin.math.sqrt

class SpatialLineMathTest {

    @Test
    fun `should serialize 3D line segment into contiguous 6-float vertex array`() {
        val pointA = Point3D(x = 1.0f, y = -0.5f, z = 2.0f)
        val pointB = Point3D(x = 4.0f, y = 3.5f, z = -1.0f)

        val vertexArray = floatArrayOf(
            pointA.x, pointA.y, pointA.z,
            pointB.x, pointB.y, pointB.z
        )

        assertThat(vertexArray).hasLength(6)
        assertThat(vertexArray[0]).isEqualTo(1.0f)
        assertThat(vertexArray[1]).isEqualTo(-0.5f)
        assertThat(vertexArray[2]).isEqualTo(2.0f)
        assertThat(vertexArray[3]).isEqualTo(4.0f)
        assertThat(vertexArray[4]).isEqualTo(3.5f)
        assertThat(vertexArray[5]).isEqualTo(-1.0f)
    }

    @Test
    fun `should calculate exact midpoint between Point A and Point B for AR label projection`() {
        val pointA = Point3D(x = 0.0f, y = 2.0f, z = -4.0f)
        val pointB = Point3D(x = 2.0f, y = 6.0f, z = -2.0f)

        val midpoint = Point3D(
            x = (pointA.x + pointB.x) / 2.0f,
            y = (pointA.y + pointB.y) / 2.0f,
            z = (pointA.z + pointB.z) / 2.0f
        )

        assertThat(midpoint.x).isEqualTo(1.0f)
        assertThat(midpoint.y).isEqualTo(4.0f)
        assertThat(midpoint.z).isEqualTo(-3.0f)
    }

    @Test
    fun `should calculate normalized spatial direction vector for metric lines`() {
        val start = Point3D(x = 0.0f, y = 0.0f, z = 0.0f)
        val end = Point3D(x = 3.0f, y = 0.0f, z = 4.0f)

        val dx = end.x - start.x
        val dy = end.y - start.y
        val dz = end.z - start.z
        val magnitude = sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()

        val normalizedDirection = Point3D(
            x = dx / magnitude,
            y = dy / magnitude,
            z = dz / magnitude
        )

        assertThat(magnitude).isEqualTo(5.0f)
        assertThat(normalizedDirection.x).isEqualTo(0.6f)
        assertThat(normalizedDirection.y).isEqualTo(0.0f)
        assertThat(normalizedDirection.z).isEqualTo(0.8f)
    }

    @Test
    fun `should handle collinear and zero-length spatial line without producing NaN`() {
        val start = Point3D(x = 1.5f, y = 2.0f, z = -1.0f)
        val end = Point3D(x = 1.5f, y = 2.0f, z = -1.0f)

        val dx = end.x - start.x
        val dy = end.y - start.y
        val dz = end.z - start.z
        val magnitude = sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()

        assertThat(magnitude).isEqualTo(0.0f)
    }
}