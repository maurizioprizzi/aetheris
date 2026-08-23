package org.aetheris.app.domain.model

data class MassEstimate(
    val kilograms: Float,
    val confidenceIntervalKg: Float,
    val densityUsedKgPerM3: Float
) {
    val grams: Float get() = kilograms * 1000f

    fun formatted(): String = "%.2f kg (±%.2f kg)".format(kilograms, confidenceIntervalKg)
}