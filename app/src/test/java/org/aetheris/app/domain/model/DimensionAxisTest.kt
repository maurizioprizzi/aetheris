package org.aetheris.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DimensionAxisTest {

    @Test
    fun `first axis is width`() {
        assertThat(DimensionAxis.FIRST)
            .isEqualTo(DimensionAxis.WIDTH)
    }

    @Test
    fun `width is followed by height`() {
        assertThat(
            DimensionAxis.WIDTH.nextAxis
        ).isEqualTo(
            DimensionAxis.HEIGHT
        )
    }

    @Test
    fun `height is followed by depth`() {
        assertThat(
            DimensionAxis.HEIGHT.nextAxis
        ).isEqualTo(
            DimensionAxis.DEPTH
        )
    }

    @Test
    fun `depth has no next axis`() {
        assertThat(
            DimensionAxis.DEPTH.nextAxis
        ).isNull()
    }

    @Test
    fun `only width is the first axis`() {
        assertThat(
            DimensionAxis.WIDTH.isFirst
        ).isTrue()

        assertThat(
            DimensionAxis.HEIGHT.isFirst
        ).isFalse()

        assertThat(
            DimensionAxis.DEPTH.isFirst
        ).isFalse()
    }

    @Test
    fun `only depth is the last axis`() {
        assertThat(
            DimensionAxis.WIDTH.isLast
        ).isFalse()

        assertThat(
            DimensionAxis.HEIGHT.isLast
        ).isFalse()

        assertThat(
            DimensionAxis.DEPTH.isLast
        ).isTrue()
    }

    @Test
    fun `entries preserve measurement order`() {
        assertThat(
            DimensionAxis.entries
        ).containsExactly(
            DimensionAxis.WIDTH,
            DimensionAxis.HEIGHT,
            DimensionAxis.DEPTH
        ).inOrder()
    }
}