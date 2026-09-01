package org.aetheris.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MaterialDensityCatalogTest {

    @Test
    fun `catalog contains expected materials in stable order`() {
        assertThat(MaterialDensityCatalog.all)
            .containsExactly(
                MaterialDensityCatalog.GENERIC_WOOD,
                MaterialDensityCatalog.POLYPROPYLENE,
                MaterialDensityCatalog.ALUMINUM,
                MaterialDensityCatalog.STRUCTURAL_STEEL,
                MaterialDensityCatalog.SOLID_GLASS,
                MaterialDensityCatalog.PORTLAND_CONCRETE,
                MaterialDensityCatalog.WATER_AT_ROOM_TEMPERATURE
            )
            .inOrder()
    }

    @Test
    fun `catalog contains seven initial materials`() {
        assertThat(MaterialDensityCatalog.all)
            .hasSize(EXPECTED_MATERIAL_COUNT)
    }

    @Test
    fun `catalog material names are unique`() {
        val names =
            MaterialDensityCatalog.all.map { material ->
                material.materialName
            }

        assertThat(names.distinct())
            .hasSize(names.size)
    }

    @Test
    fun `catalog material names are not blank`() {
        val containsBlankName =
            MaterialDensityCatalog.all.any { material ->
                material.materialName.isBlank()
            }

        assertThat(containsBlankName)
            .isFalse()
    }

    @Test
    fun `catalog densities are finite and positive`() {
        MaterialDensityCatalog.all.forEach { material ->
            assertThat(
                material.kilogramsPerCubicMeter
                    .isFinite()
            ).isTrue()

            assertThat(
                material.kilogramsPerCubicMeter
            ).isGreaterThan(0f)
        }
    }

    @Test
    fun `catalog uncertainties are finite and non negative`() {
        MaterialDensityCatalog.all.forEach { material ->
            assertThat(
                material
                    .uncertaintyKilogramsPerCubicMeter
                    .isFinite()
            ).isTrue()

            assertThat(
                material
                    .uncertaintyKilogramsPerCubicMeter
            ).isAtLeast(0f)
        }
    }

    @Test
    fun `catalog density ranges never become negative`() {
        MaterialDensityCatalog.all.forEach { material ->
            assertThat(material.minimumKilogramsPerCubicMeter)
                .isAtLeast(0f)

            assertThat(material.maximumKilogramsPerCubicMeter)
                .isAtLeast(
                    material.minimumKilogramsPerCubicMeter
                )
        }
    }

    @Test
    fun `findByName returns material using exact name`() {
        val result =
            MaterialDensityCatalog.findByName(
                materialName = "Alumínio sólido"
            )

        assertThat(result)
            .isSameInstanceAs(
                MaterialDensityCatalog.ALUMINUM
            )
    }

    @Test
    fun `findByName ignores letter case`() {
        val result =
            MaterialDensityCatalog.findByName(
                materialName = "aÇo EsTrUtUrAl SóLiDo"
            )

        assertThat(result)
            .isSameInstanceAs(
                MaterialDensityCatalog.STRUCTURAL_STEEL
            )
    }

    @Test
    fun `findByName ignores surrounding whitespace`() {
        val result =
            MaterialDensityCatalog.findByName(
                materialName =
                    "   Concreto Portland   "
            )

        assertThat(result)
            .isSameInstanceAs(
                MaterialDensityCatalog.PORTLAND_CONCRETE
            )
    }

    @Test
    fun `findByName returns null for blank name`() {
        assertThat(
            MaterialDensityCatalog.findByName(
                materialName = "   "
            )
        ).isNull()
    }

    @Test
    fun `findByName returns null for unknown material`() {
        assertThat(
            MaterialDensityCatalog.findByName(
                materialName = "Material inexistente"
            )
        ).isNull()
    }

    @Test
    fun `water reference uses room temperature density`() {
        val water =
            MaterialDensityCatalog
                .WATER_AT_ROOM_TEMPERATURE

        assertThat(water.kilogramsPerCubicMeter)
            .isWithin(FLOAT_TOLERANCE)
            .of(998f)

        assertThat(
            water.uncertaintyKilogramsPerCubicMeter
        )
            .isWithin(FLOAT_TOLERANCE)
            .of(5f)
    }

    @Test
    fun `structural steel reference uses solid density`() {
        val steel =
            MaterialDensityCatalog.STRUCTURAL_STEEL

        assertThat(steel.kilogramsPerCubicMeter)
            .isWithin(FLOAT_TOLERANCE)
            .of(7_850f)

        assertThat(
            steel.uncertaintyKilogramsPerCubicMeter
        )
            .isGreaterThan(0f)
    }

    @Test
    fun `generic wood has wider relative uncertainty than aluminum`() {
        val wood =
            MaterialDensityCatalog.GENERIC_WOOD

        val aluminum =
            MaterialDensityCatalog.ALUMINUM

        assertThat(
            wood.relativeUncertaintyPercentage
        ).isGreaterThan(
            aluminum.relativeUncertaintyPercentage
        )
    }

    private companion object {
        const val EXPECTED_MATERIAL_COUNT = 7
        const val FLOAT_TOLERANCE = 1e-3f
    }
}