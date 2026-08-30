package org.aetheris.app.domain.model

/**
 * Conjunto imutável das três dimensões espaciais
 * de uma pessoa ou objeto.
 *
 * Cada dimensão utiliza [DistanceMeasurement] para
 * preservar o valor medido e sua incerteza.
 *
 * As propriedades podem ser nulas enquanto a sessão
 * de medição ainda estiver incompleta.
 */
data class SpatialDimensions(
    val width: DistanceMeasurement? = null,
    val height: DistanceMeasurement? = null,
    val depth: DistanceMeasurement? = null
) {

    /**
     * Quantidade de eixos que já possuem medição.
     */
    val measuredAxisCount: Int
        get() = DimensionAxis.entries.count { axis ->
            this[axis] != null
        }

    /**
     * Indica se nenhuma dimensão foi medida.
     */
    val isEmpty: Boolean
        get() = measuredAxisCount == 0

    /**
     * Indica se as três dimensões foram medidas.
     */
    val isComplete: Boolean
        get() =
            width != null &&
                    height != null &&
                    depth != null

    /**
     * Eixos que ainda precisam ser medidos,
     * preservando a ordem oficial da sessão.
     */
    val pendingAxes: List<DimensionAxis>
        get() = DimensionAxis.entries.filter { axis ->
            this[axis] == null
        }

    /**
     * Próximo eixo que deve ser medido.
     *
     * Retorna null quando todas as dimensões
     * já foram preenchidas.
     */
    val nextPendingAxis: DimensionAxis?
        get() = pendingAxes.firstOrNull()

    /**
     * Volume central em metros cúbicos.
     *
     * Retorna null enquanto alguma dimensão
     * ainda não tiver sido medida.
     */
    val volumeCubicMeters: Float?
        get() {
            val widthMeters =
                width?.meters ?: return null

            val heightMeters =
                height?.meters ?: return null

            val depthMeters =
                depth?.meters ?: return null

            val volume =
                widthMeters.toDouble() *
                        heightMeters.toDouble() *
                        depthMeters.toDouble()

            require(
                volume.isFinite() &&
                        volume <= Float.MAX_VALUE
            ) {
                "O volume calculado deve ser finito."
            }

            return volume.toFloat()
        }

    /**
     * Volume central em litros.
     *
     * Um metro cúbico corresponde a 1.000 litros.
     * Retorna null enquanto a medição estiver incompleta.
     */
    val volumeLiters: Float?
        get() {
            val cubicMeters =
                volumeCubicMeters ?: return null

            val liters =
                cubicMeters.toDouble() *
                        LITERS_PER_CUBIC_METER

            require(
                liters.isFinite() &&
                        liters <= Float.MAX_VALUE
            ) {
                "O volume em litros deve ser finito."
            }

            return liters.toFloat()
        }

    /**
     * Retorna a medição correspondente ao eixo informado.
     */
    operator fun get(
        axis: DimensionAxis
    ): DistanceMeasurement? {
        return when (axis) {
            DimensionAxis.WIDTH -> width
            DimensionAxis.HEIGHT -> height
            DimensionAxis.DEPTH -> depth
        }
    }

    /**
     * Retorna uma nova instância contendo a medição
     * informada no eixo selecionado.
     *
     * A instância atual permanece inalterada.
     */
    fun withMeasurement(
        axis: DimensionAxis,
        measurement: DistanceMeasurement
    ): SpatialDimensions {
        return when (axis) {
            DimensionAxis.WIDTH -> {
                copy(width = measurement)
            }

            DimensionAxis.HEIGHT -> {
                copy(height = measurement)
            }

            DimensionAxis.DEPTH -> {
                copy(depth = measurement)
            }
        }
    }

    /**
     * Retorna uma nova instância removendo
     * a medição do eixo selecionado.
     */
    fun withoutMeasurement(
        axis: DimensionAxis
    ): SpatialDimensions {
        return when (axis) {
            DimensionAxis.WIDTH -> {
                copy(width = null)
            }

            DimensionAxis.HEIGHT -> {
                copy(height = null)
            }

            DimensionAxis.DEPTH -> {
                copy(depth = null)
            }
        }
    }

    companion object {

        /**
         * Estado inicial de uma sessão dimensional.
         */
        val EMPTY = SpatialDimensions()

        private const val LITERS_PER_CUBIC_METER =
            1_000.0
    }
}