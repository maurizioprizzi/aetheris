package org.aetheris.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnchorPlacementTest {

    @Test
    fun `plane placement is conventional and not approximate`() {
        val placement = AnchorPlacement(
            position = Point3D(
                x = 1f,
                y = 2f,
                z = 3f
            ),
            source = AnchorPlacementSource.PLANE
        )

        assertThat(placement.isConventional)
            .isTrue()

        assertThat(placement.isApproximate)
            .isFalse()

        assertThat(placement.usesDepth)
            .isFalse()
    }

    @Test
    fun `instant placement is marked as approximate`() {
        val placement = AnchorPlacement(
            position = Point3D(
                x = 0f,
                y = 0f,
                z = -1.5f
            ),
            source =
                AnchorPlacementSource.INSTANT_PLACEMENT
        )

        assertThat(placement.isApproximate)
            .isTrue()

        assertThat(placement.isConventional)
            .isFalse()

        assertThat(placement.usesDepth)
            .isFalse()
    }

    @Test
    fun `depth placement reports depth dependency`() {
        val placement = AnchorPlacement(
            position = Point3D(
                x = 1f,
                y = 0f,
                z = -2f
            ),
            source =
                AnchorPlacementSource.DEPTH_POINT
        )

        assertThat(placement.usesDepth)
            .isTrue()

        assertThat(placement.isConventional)
            .isTrue()

        assertThat(placement.isApproximate)
            .isFalse()
    }

    @Test
    fun `withUpdatedPosition changes position and preserves source`() {
        val original = AnchorPlacement(
            position = Point3D(
                x = 1f,
                y = 2f,
                z = 3f
            ),
            source =
                AnchorPlacementSource.FEATURE_POINT
        )

        val updated = original.withUpdatedPosition(
            newPosition = Point3D(
                x = 4f,
                y = 5f,
                z = 6f
            )
        )

        assertThat(updated.position)
            .isEqualTo(
                Point3D(
                    x = 4f,
                    y = 5f,
                    z = 6f
                )
            )

        assertThat(updated.source)
            .isEqualTo(
                AnchorPlacementSource.FEATURE_POINT
            )

        assertThat(updated)
            .isNotSameInstanceAs(original)
    }

    @Test
    fun `withUpdatedPosition does not modify original placement`() {
        val originalPosition = Point3D(
            x = 1f,
            y = 2f,
            z = 3f
        )

        val original = AnchorPlacement(
            position = originalPosition,
            source = AnchorPlacementSource.PLANE
        )

        original.withUpdatedPosition(
            newPosition = Point3D(
                x = 7f,
                y = 8f,
                z = 9f
            )
        )

        assertThat(original.position)
            .isEqualTo(originalPosition)

        assertThat(original.source)
            .isEqualTo(AnchorPlacementSource.PLANE)
    }

    @Test
    fun `withUpdatedPosition returns same instance when position is unchanged`() {
        val position = Point3D(
            x = 1f,
            y = 2f,
            z = 3f
        )

        val placement = AnchorPlacement(
            position = position,
            source =
                AnchorPlacementSource.INSTANT_PLACEMENT
        )

        val result = placement.withUpdatedPosition(
            newPosition = position
        )

        assertThat(result)
            .isSameInstanceAs(placement)
    }

    @Test
    fun `data class equality includes position and source`() {
        val position = Point3D(
            x = 1f,
            y = 2f,
            z = 3f
        )

        val planePlacement = AnchorPlacement(
            position = position,
            source = AnchorPlacementSource.PLANE
        )

        val equalPlanePlacement = AnchorPlacement(
            position = position,
            source = AnchorPlacementSource.PLANE
        )

        val instantPlacement = AnchorPlacement(
            position = position,
            source =
                AnchorPlacementSource.INSTANT_PLACEMENT
        )

        assertThat(planePlacement)
            .isEqualTo(equalPlanePlacement)

        assertThat(planePlacement)
            .isNotEqualTo(instantPlacement)
    }
}