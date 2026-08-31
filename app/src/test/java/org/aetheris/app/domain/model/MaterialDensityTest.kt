package org.aetheris.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Locale

class MaterialDensityTest {

    @Test
    fun `constructor accepts valid material density`() {
        val density = MaterialDensity(
            materialName = "Madeira",
            kilogramsPerCubicMeter = 650f,
            uncertaintyKilogramsPerCubicMeter = 75f
        )

        assertThat(density.materialName)
            .isEqualTo("Madeira")

        assertThat(density.kilogramsPerCubicMeter)
            .isEqualTo(650f)

        assertThat(
            density.uncertaintyKilogramsPerCubicMeter
        ).isEqualTo(75f)
    }

    @Test
    fun `constructor rejects blank material name`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            MaterialDensity(
                materialName = "   ",
                kilogramsPerCubicMeter = 650f,
                uncertaintyKilogramsPerCubicMeter = 75f
            )
        }
    }

    @Test
    fun `constructor rejects zero density`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            MaterialDensity(
                materialName = "Material",
                kilogramsPerCubicMeter = 0f,
                uncertaintyKilogramsPerCubicMeter = 0f
            )
        }
    }

    @Test
    fun `constructor rejects negative density`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            MaterialDensity(
                materialName = "Material",
                kilogramsPerCubicMeter = -1f,
                uncertaintyKilogramsPerCubicMeter = 0f
            )
        }
    }

    @Test
    fun `constructor rejects non finite density`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            MaterialDensity(
                materialName = "Material",
                kilogramsPerCubicMeter = Float.NaN,
                uncertaintyKilogramsPerCubicMeter = 0f
            )
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            MaterialDensity(
                materialName = "Material",
                kilogramsPerCubicMeter =
                    Float.POSITIVE_INFINITY,
                uncertaintyKilogramsPerCubicMeter = 0f
            )
        }
    }

    @Test
    fun `constructor rejects negative uncertainty`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            MaterialDensity(
                materialName = "Material",
                kilogramsPerCubicMeter = 650f,
                uncertaintyKilogramsPerCubicMeter = -1f
            )
        }
    }

    @Test
    fun `constructor rejects non finite uncertainty`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            MaterialDensity(
                materialName = "Material",
                kilogramsPerCubicMeter = 650f,
                uncertaintyKilogramsPerCubicMeter =
                    Float.NaN
            )
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            MaterialDensity(
                materialName = "Material",
                kilogramsPerCubicMeter = 650f,
                uncertaintyKilogramsPerCubicMeter =
                    Float.POSITIVE_INFINITY
            )
        }
    }

    @Test
    fun `constructor rejects density range overflow`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            MaterialDensity(
                materialName = "Material",
                kilogramsPerCubicMeter =
                    Float.MAX_VALUE,
                uncertaintyKilogramsPerCubicMeter =
                    Float.MAX_VALUE
            )
        }
    }

    @Test
    fun `converts density to grams per cubic centimeter`() {
        val density = MaterialDensity(
            materialName = "Água",
            kilogramsPerCubicMeter = 1_000f,
            uncertaintyKilogramsPerCubicMeter = 100f
        )

        assertThat(
            density.gramsPerCubicCentimeter
        )
            .isWithin(0.0001f)
            .of(1f)

        assertThat(
            density
                .uncertaintyGramsPerCubicCentimeter
        )
            .isWithin(0.0001f)
            .of(0.1f)
    }

    @Test
    fun `calculates minimum and maximum density`() {
        val density = MaterialDensity(
            materialName = "Madeira",
            kilogramsPerCubicMeter = 650f,
            uncertaintyKilogramsPerCubicMeter = 75f
        )

        assertThat(
            density.minimumKilogramsPerCubicMeter
        )
            .isWithin(0.0001f)
            .of(575f)

        assertThat(
            density.maximumKilogramsPerCubicMeter
        )
            .isWithin(0.0001f)
            .of(725f)
    }

    @Test
    fun `minimum density cannot be negative`() {
        val density = MaterialDensity(
            materialName = "Material",
            kilogramsPerCubicMeter = 50f,
            uncertaintyKilogramsPerCubicMeter = 100f
        )

        assertThat(
            density.minimumKilogramsPerCubicMeter
        ).isEqualTo(0f)

        assertThat(
            density.maximumKilogramsPerCubicMeter
        ).isEqualTo(150f)
    }

    @Test
    fun `calculates relative uncertainty percentage`() {
        val density = MaterialDensity(
            materialName = "Material",
            kilogramsPerCubicMeter = 800f,
            uncertaintyKilogramsPerCubicMeter = 80f
        )

        assertThat(
            density.relativeUncertaintyPercentage
        )
            .isWithin(0.0001f)
            .of(10f)
    }

    @Test
    fun `identifies reference without declared uncertainty`() {
        val exactReference = MaterialDensity(
            materialName = "Referência",
            kilogramsPerCubicMeter = 1_000f,
            uncertaintyKilogramsPerCubicMeter = 0f
        )

        val estimatedReference = MaterialDensity(
            materialName = "Estimativa",
            kilogramsPerCubicMeter = 1_000f,
            uncertaintyKilogramsPerCubicMeter = 100f
        )

        assertThat(exactReference.isExactReference)
            .isTrue()

        assertThat(estimatedReference.isExactReference)
            .isFalse()
    }

    @Test
    fun `formats material density with uncertainty`() {
        val density = MaterialDensity(
            materialName = "Madeira",
            kilogramsPerCubicMeter = 650f,
            uncertaintyKilogramsPerCubicMeter = 75f
        )

        assertThat(
            density.formattedMetric(Locale.US)
        ).isEqualTo(
            "Madeira: 650.0 kg/m³ (±75.0 kg/m³)"
        )
    }

    @Test
    fun `formats density value without material name`() {
        val density = MaterialDensity(
            materialName = "Madeira",
            kilogramsPerCubicMeter = 650f,
            uncertaintyKilogramsPerCubicMeter = 75f
        )

        assertThat(
            density.formattedValueOnly(Locale.US)
        ).isEqualTo(
            "650.0 kg/m³"
        )
    }
}