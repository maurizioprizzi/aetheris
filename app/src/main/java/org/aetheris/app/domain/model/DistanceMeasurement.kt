package org.aetheris.app.domain.model

data class DistanceMeasurement(
    val meters: Float,
    val uncertaintyMeters: Float = 0.02f,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    val centimeters: Float get() = meters * 100f
    val millimeters: Float get() = meters * 1000f

    fun formattedMetric(): String = when {
        meters < 1f -> "%.1f cm (±%.1f cm)".format(centimeters, uncertaintyMeters * 100f)
        else -> "%.2f m (±%.2f m)".format(meters, uncertaintyMeters)
    }
}