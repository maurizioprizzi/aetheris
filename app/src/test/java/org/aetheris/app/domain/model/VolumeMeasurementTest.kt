package org.aetheris.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Locale

class VolumeMeasurementTest {

    @Test
    fun `constructor accepts valid volume measurement`() {
        val measurement = VolumeMeasurement(
            cubicMeters = 0.75f,
            uncertaintyCubicMeters = 0.05f,
            timestampMillis = 1_000L
        )

        assertThat(measurement.cubicMeters)
            .isEqualTo(0.75f)

        assertThat(measurement.uncertaintyCubicMeters)
            .isEqualTo(0.05f)

        assertThat(measurement.timestampMillis)
            .isEqualTo(1_000L)
    }

    @Test
    fun `constructor rejects negative volume`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            VolumeMeasurement(
                cubicMeters = -0.1f,
                uncertaintyCubicMeters = 0.01f
            )
        }
    }

    @Test
    fun `constructor rejects non finite volume`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            VolumeMeasurement(
                cubicMeters = Float.NaN,
                uncertaintyCubicMeters = 0.01f
            )
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            VolumeMeasurement(
                cubicMeters = Float.POSITIVE_INFINITY,
                uncertaintyCubicMeters = 0.01f
            )
        }
    }

    @Test
    fun `constructor rejects negative uncertainty`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            VolumeMeasurement(
                cubicMeters = 1f,
                uncertaintyCubicMeters = -0.01f
            )
        }
    }

    @Test
    fun `constructor rejects non finite uncertainty`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            VolumeMeasurement(
                cubicMeters = 1f,
                uncertaintyCubicMeters = Float.NaN
            )
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            VolumeMeasurement(
                cubicMeters = 1f,
                uncertaintyCubicMeters =
                    Float.POSITIVE_INFINITY
            )
        }
    }

    @Test
    fun `constructor rejects negative timestamp`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            VolumeMeasurement(
                cubicMeters = 1f,
                uncertaintyCubicMeters = 0.1f,
                timestampMillis = -1L
            )
        }
    }

    @Test
    fun `converts cubic meters and uncertainty to liters`() {
        val measurement = VolumeMeasurement(
            cubicMeters = 0.75f,
            uncertaintyCubicMeters = 0.05f
        )

        assertThat(measurement.liters)
            .isWithin(0.001f)
            .of(750f)

        assertThat(measurement.uncertaintyLiters)
            .isWithin(0.001f)
            .of(50f)
    }

    @Test
    fun `calculates minimum and maximum volume`() {
        val measurement = VolumeMeasurement(
            cubicMeters = 2f,
            uncertaintyCubicMeters = 0.25f
        )

        assertThat(measurement.minimumCubicMeters)
            .isWithin(0.0001f)
            .of(1.75f)

        assertThat(measurement.maximumCubicMeters)
            .isWithin(0.0001f)
            .of(2.25f)

        assertThat(measurement.minimumLiters)
            .isWithin(0.01f)
            .of(1_750f)

        assertThat(measurement.maximumLiters)
            .isWithin(0.01f)
            .of(2_250f)
    }

    @Test
    fun `minimum volume cannot be negative`() {
        val measurement = VolumeMeasurement(
            cubicMeters = 0.1f,
            uncertaintyCubicMeters = 0.25f
        )

        assertThat(measurement.minimumCubicMeters)
            .isEqualTo(0f)

        assertThat(measurement.minimumLiters)
            .isEqualTo(0f)
    }

    @Test
    fun `calculates relative uncertainty percentage`() {
        val measurement = VolumeMeasurement(
            cubicMeters = 2f,
            uncertaintyCubicMeters = 0.1f
        )

        assertThat(
            measurement.relativeUncertaintyPercentage
        )
            .isWithin(0.0001f)
            .of(5f)
    }

    @Test
    fun `relative uncertainty is zero when volume is zero`() {
        val measurement = VolumeMeasurement(
            cubicMeters = 0f,
            uncertaintyCubicMeters = 0.1f
        )

        assertThat(
            measurement.relativeUncertaintyPercentage
        ).isEqualTo(0f)
    }

    @Test
    fun `formats volume below one cubic meter in liters`() {
        val measurement = VolumeMeasurement(
            cubicMeters = 0.75f,
            uncertaintyCubicMeters = 0.05f
        )

        assertThat(
            measurement.formattedMetric(Locale.US)
        ).isEqualTo(
            "750.0 L (±50.0 L)"
        )

        assertThat(
            measurement.formattedValueOnly(Locale.US)
        ).isEqualTo(
            "750.0 L"
        )
    }

    @Test
    fun `formats volume from one cubic meter in cubic meters`() {
        val measurement = VolumeMeasurement(
            cubicMeters = 1.25f,
            uncertaintyCubicMeters = 0.1f
        )

        assertThat(
            measurement.formattedMetric(Locale.US)
        ).isEqualTo(
            "1.250 m³ (±0.100 m³)"
        )

        assertThat(
            measurement.formattedValueOnly(Locale.US)
        ).isEqualTo(
            "1.250 m³"
        )
    }
}