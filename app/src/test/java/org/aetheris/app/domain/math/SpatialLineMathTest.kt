package org.aetheris.app.domain.math

import com.google.common.truth.Truth.assertThat
import org.aetheris.app.domain.model.Point3D
import org.junit.Test

class SpatialLineMathTest {

    @Test
    fun `toVertexArray returns start and end coordinates in OpenGL order`() {
        val start = Point3D(
            x = 1f,
            y = 2f,
            z = 3f
        )

        val end = Point3D(
            x = 4f,
            y = 5f,
            z = 6f
        )

        val result = SpatialLineMath.toVertexArray(
            start = start,
            end = end
        )

        assertThat(result.asList())
            .containsExactly(
                1f,
                2f,
                3f,
                4f,
                5f,
                6f
            )
            .inOrder()
    }

    @Test
    fun `midpoint returns center between two points`() {
        val start = Point3D(
            x = -2f,
            y = 1f,
            z = 4f
        )

        val end = Point3D(
            x = 6f,
            y = 5f,
            z = -2f
        )

        val result = SpatialLineMath.midpoint(
            start = start,
            end = end
        )

        assertThat(result).isEqualTo(
            Point3D(
                x = 2f,
                y = 3f,
                z = 1f
            )
        )
    }

    @Test
    fun `midpoint avoids float overflow for large coordinates`() {
        val start = Point3D(
            x = Float.MAX_VALUE,
            y = Float.MAX_VALUE,
            z = Float.MAX_VALUE
        )

        val end = Point3D(
            x = Float.MAX_VALUE,
            y = Float.MAX_VALUE,
            z = Float.MAX_VALUE
        )

        val result = SpatialLineMath.midpoint(
            start = start,
            end = end
        )

        assertThat(result.x).isEqualTo(Float.MAX_VALUE)
        assertThat(result.y).isEqualTo(Float.MAX_VALUE)
        assertThat(result.z).isEqualTo(Float.MAX_VALUE)
    }

    @Test
    fun `magnitude returns euclidean distance between points`() {
        val start = Point3D(
            x = 0f,
            y = 0f,
            z = 0f
        )

        val end = Point3D(
            x = 3f,
            y = 4f,
            z = 12f
        )

        val result = SpatialLineMath.magnitude(
            start = start,
            end = end
        )

        assertThat(result)
            .isWithin(1e-5f)
            .of(13f)
    }

    @Test
    fun `normalizedDirection returns unit vector toward end point`() {
        val start = Point3D(
            x = 1f,
            y = 2f,
            z = 3f
        )

        val end = Point3D(
            x = 4f,
            y = 6f,
            z = 3f
        )

        val result = requireNotNull(
            SpatialLineMath.normalizedDirection(
                start = start,
                end = end
            )
        )

        assertThat(result.x)
            .isWithin(1e-5f)
            .of(0.6f)

        assertThat(result.y)
            .isWithin(1e-5f)
            .of(0.8f)

        assertThat(result.z)
            .isWithin(1e-5f)
            .of(0f)

        assertThat(result.magnitude)
            .isWithin(1e-5f)
            .of(1f)
    }

    @Test
    fun `normalizedDirection returns null for identical points`() {
        val point = Point3D(
            x = 1f,
            y = 2f,
            z = 3f
        )

        val result = SpatialLineMath.normalizedDirection(
            start = point,
            end = point
        )

        assertThat(result).isNull()
    }

    @Test
    fun `normalizedDirection returns null for negligible segment`() {
        val start = Point3D.ORIGIN

        val end = Point3D(
            x = 0.0000001f,
            y = 0f,
            z = 0f
        )

        val result = SpatialLineMath.normalizedDirection(
            start = start,
            end = end
        )

        assertThat(result).isNull()
    }
}