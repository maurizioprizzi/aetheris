package org.aetheris.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnchorPlacementSourceTest {

    @Test
    fun `enum contains all supported placement sources`() {
        assertThat(
            AnchorPlacementSource.entries
        ).containsExactly(
            AnchorPlacementSource.PLANE,
            AnchorPlacementSource.FEATURE_POINT,
            AnchorPlacementSource.DEPTH_POINT,
            AnchorPlacementSource.INSTANT_PLACEMENT
        ).inOrder()
    }

    @Test
    fun `instant placement is approximate and may refine over time`() {
        val source =
            AnchorPlacementSource.INSTANT_PLACEMENT

        assertThat(source.isApproximate)
            .isTrue()

        assertThat(source.isConventional)
            .isFalse()

        assertThat(source.usesDepth)
            .isFalse()

        assertThat(source.mayRefineOverTime)
            .isTrue()
    }

    @Test
    fun `conventional sources are not marked as approximate`() {
        val conventionalSources = listOf(
            AnchorPlacementSource.PLANE,
            AnchorPlacementSource.FEATURE_POINT,
            AnchorPlacementSource.DEPTH_POINT
        )

        conventionalSources.forEach { source ->
            assertThat(source.isConventional)
                .isTrue()

            assertThat(source.isApproximate)
                .isFalse()

            assertThat(source.mayRefineOverTime)
                .isFalse()
        }
    }

    @Test
    fun `only depth point depends on depth api`() {
        AnchorPlacementSource.entries
            .forEach { source ->
                val expectedUsesDepth =
                    source ==
                            AnchorPlacementSource.DEPTH_POINT

                assertThat(source.usesDepth)
                    .isEqualTo(expectedUsesDepth)
            }
    }

    @Test
    fun `approximate and conventional classifications are mutually exclusive`() {
        AnchorPlacementSource.entries
            .forEach { source ->
                assertThat(
                    source.isApproximate ==
                            source.isConventional
                ).isFalse()
            }
    }
}