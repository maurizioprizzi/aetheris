package org.aetheris.app.domain.model

/**
 * Conjunto imutável das três dimensões espaciais
 * de uma pessoa ou objeto.
 *
 * Cada dimensão utiliza [DistanceMeasurement] para preservar
 * o valor medido, sua incerteza e o instante de captura.
 *
 * A procedência espacial pode ser associada gradualmente por
 * meio de [withDimensionMeasurement]. Medições legadas continuam
 * válidas e são representadas sem fontes de posicionamento.
 *
 * As propriedades podem ser nulas enquanto a sessão
 * de medição ainda estiver incompleta.
 */
data class SpatialDimensions(
    val width: DistanceMeasurement? = null,
    val height: DistanceMeasurement? = null,
    val depth: DistanceMeasurement? = null,
    private val provenanceByAxis:
    Map<DimensionAxis, DimensionMeasurement> =
        emptyMap()
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
     * Eixos medidos que possuem ao menos uma origem
     * espacial conhecida.
     */
    val axesWithProvenance: List<DimensionAxis>
        get() = DimensionAxis.entries.filter { axis ->
            getDimensionMeasurement(axis)
                ?.hasAnyProvenance == true
        }

    /**
     * Quantidade de dimensões com alguma procedência conhecida.
     */
    val provenanceAxisCount: Int
        get() = axesWithProvenance.size

    /**
     * Indica que ao menos uma dimensão possui procedência.
     */
    val hasAnyProvenance: Boolean
        get() = provenanceAxisCount > 0

    /**
     * Indica que todos os eixos medidos possuem procedência
     * completa para os pontos inicial e final.
     *
     * Um conjunto vazio não é considerado completamente
     * documentado.
     */
    val hasCompleteProvenance: Boolean
        get() = measuredAxisCount > 0 &&
                DimensionAxis.entries
                    .filter { axis ->
                        this[axis] != null
                    }
                    .all { axis ->
                        getDimensionMeasurement(axis)
                            ?.hasCompleteProvenance == true
                    }

    /**
     * Indica que pelo menos uma dimensão utilizou
     * Instant Placement.
     */
    val usesApproximatePlacement: Boolean
        get() = DimensionAxis.entries.any { axis ->
            getDimensionMeasurement(axis)
                ?.usesApproximatePlacement == true
        }

    /**
     * Indica que pelo menos uma dimensão dependeu
     * de dados da Depth API.
     */
    val usesDepth: Boolean
        get() = DimensionAxis.entries.any { axis ->
            getDimensionMeasurement(axis)
                ?.usesDepth == true
        }

    /**
     * Indica que pelo menos uma dimensão contém uma pose
     * espacial que pode sofrer refinamento relevante.
     */
    val mayRefineOverTime: Boolean
        get() = DimensionAxis.entries.any { axis ->
            getDimensionMeasurement(axis)
                ?.mayRefineOverTime == true
        }

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
     * Retorna a distância correspondente ao eixo informado.
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
     * Retorna a dimensão com sua procedência espacial.
     *
     * Medições criadas antes da introdução da procedência são
     * adaptadas para [DimensionMeasurement] com fontes nulas.
     *
     * Uma entrada interna incompatível com a distância atual é
     * ignorada defensivamente, evitando associação de procedência
     * obsoleta após operações de cópia.
     */
    fun getDimensionMeasurement(
        axis: DimensionAxis
    ): DimensionMeasurement? {
        val currentMeasurement =
            this[axis] ?: return null

        val measurementWithProvenance =
            provenanceByAxis[axis]

        if (
            measurementWithProvenance?.measurement ==
            currentMeasurement
        ) {
            return measurementWithProvenance
        }

        return DimensionMeasurement(
            measurement = currentMeasurement
        )
    }

    /**
     * Retorna uma nova instância contendo uma distância sem
     * procedência conhecida no eixo selecionado.
     *
     * Qualquer procedência anterior desse eixo é removida para
     * impedir que fontes antigas sejam associadas ao novo valor.
     */
    fun withMeasurement(
        axis: DimensionAxis,
        measurement: DistanceMeasurement
    ): SpatialDimensions {
        return withStoredMeasurement(
            axis = axis,
            measurement = measurement,
            updatedProvenance =
                provenanceByAxis - axis
        )
    }

    /**
     * Retorna uma nova instância contendo uma dimensão e as
     * origens espaciais dos seus pontos inicial e final.
     */
    fun withDimensionMeasurement(
        axis: DimensionAxis,
        dimensionMeasurement: DimensionMeasurement
    ): SpatialDimensions {
        return withStoredMeasurement(
            axis = axis,
            measurement =
                dimensionMeasurement.measurement,
            updatedProvenance =
                provenanceByAxis +
                        (axis to dimensionMeasurement)
        )
    }

    /**
     * Retorna uma nova instância removendo a medição e a
     * procedência espacial do eixo selecionado.
     */
    fun withoutMeasurement(
        axis: DimensionAxis
    ): SpatialDimensions {
        return when (axis) {
            DimensionAxis.WIDTH -> {
                copy(
                    width = null,
                    provenanceByAxis =
                        provenanceByAxis - axis
                )
            }

            DimensionAxis.HEIGHT -> {
                copy(
                    height = null,
                    provenanceByAxis =
                        provenanceByAxis - axis
                )
            }

            DimensionAxis.DEPTH -> {
                copy(
                    depth = null,
                    provenanceByAxis =
                        provenanceByAxis - axis
                )
            }
        }
    }

    private fun withStoredMeasurement(
        axis: DimensionAxis,
        measurement: DistanceMeasurement,
        updatedProvenance:
        Map<DimensionAxis, DimensionMeasurement>
    ): SpatialDimensions {
        return when (axis) {
            DimensionAxis.WIDTH -> {
                copy(
                    width = measurement,
                    provenanceByAxis =
                        updatedProvenance
                )
            }

            DimensionAxis.HEIGHT -> {
                copy(
                    height = measurement,
                    provenanceByAxis =
                        updatedProvenance
                )
            }

            DimensionAxis.DEPTH -> {
                copy(
                    depth = measurement,
                    provenanceByAxis =
                        updatedProvenance
                )
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