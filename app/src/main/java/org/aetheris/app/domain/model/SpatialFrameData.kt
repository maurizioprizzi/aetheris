package org.aetheris.app.domain.model

/**
 * Estado espacial consolidado produzido a partir
 * do frame mais recente do ARCore.
 *
 * @property trackingStatus Estado atual do rastreamento da câmera.
 * @property isDepthEnabled Indica se o Depth Mode está habilitado na sessão.
 * @property pointCount Quantidade de pontos que passaram pelo filtro de confiança.
 * @property isSurfaceDetected Indica se existe uma superfície convencional
 * válida sob a mira.
 * @property anchoredStartPoint Posição atual da âncora inicial.
 * @property anchoredEndPoint Posição atual da âncora final.
 * @property anchoredStartSource Origem utilizada para criar a âncora inicial.
 * @property anchoredEndSource Origem utilizada para criar a âncora final.
 */
data class SpatialFrameData(
    val trackingStatus: TrackingStatus =
        TrackingStatus.UNAVAILABLE,

    val isDepthEnabled: Boolean = false,

    val pointCount: Int = 0,

    val isSurfaceDetected: Boolean = false,

    val anchoredStartPoint: Point3D? = null,

    val anchoredEndPoint: Point3D? = null,

    /**
     * Pode permanecer null temporariamente enquanto consumidores
     * antigos ainda fornecem somente a posição da âncora.
     */
    val anchoredStartSource: AnchorPlacementSource? = null,

    /**
     * Pode permanecer null temporariamente enquanto consumidores
     * antigos ainda fornecem somente a posição da âncora.
     */
    val anchoredEndSource: AnchorPlacementSource? = null
) {

    init {
        require(pointCount >= 0) {
            "A quantidade de pontos não pode ser negativa."
        }

        require(
            anchoredStartSource == null ||
                    anchoredStartPoint != null
        ) {
            "A origem da âncora inicial exige uma posição inicial."
        }

        require(
            anchoredEndSource == null ||
                    anchoredEndPoint != null
        ) {
            "A origem da âncora final exige uma posição final."
        }
    }

    val isTracking: Boolean
        get() =
            trackingStatus ==
                    TrackingStatus.TRACKING

    val hasPointCloud: Boolean
        get() = pointCount > 0

    val hasStartAnchor: Boolean
        get() = anchoredStartPoint != null

    val hasEndAnchor: Boolean
        get() = anchoredEndPoint != null

    val anchorCount: Int
        get() =
            (if (hasStartAnchor) 1 else 0) +
                    (if (hasEndAnchor) 1 else 0)

    val hasCompleteMeasurement: Boolean
        get() =
            hasStartAnchor &&
                    hasEndAnchor

    /**
     * Representação consolidada da âncora inicial.
     *
     * Retorna null enquanto a posição ou sua origem
     * não estiverem disponíveis.
     */
    val anchoredStartPlacement: AnchorPlacement?
        get() {
            val point =
                anchoredStartPoint ?: return null

            val source =
                anchoredStartSource ?: return null

            return AnchorPlacement(
                position = point,
                source = source
            )
        }

    /**
     * Representação consolidada da âncora final.
     *
     * Retorna null enquanto a posição ou sua origem
     * não estiverem disponíveis.
     */
    val anchoredEndPlacement: AnchorPlacement?
        get() {
            val point =
                anchoredEndPoint ?: return null

            val source =
                anchoredEndSource ?: return null

            return AnchorPlacement(
                position = point,
                source = source
            )
        }

    /**
     * Indica que todas as âncoras existentes possuem
     * uma origem espacial conhecida.
     *
     * Quando nenhuma âncora existe, retorna true porque
     * não existe proveniência pendente.
     */
    val hasCompletePlacementProvenance: Boolean
        get() =
            (!hasStartAnchor ||
                    anchoredStartSource != null) &&
                    (!hasEndAnchor ||
                            anchoredEndSource != null)

    /**
     * Indica que pelo menos uma âncora foi criada por
     * Instant Placement com profundidade inicial aproximada.
     */
    val hasApproximateAnchor: Boolean
        get() =
            anchoredStartSource?.isApproximate == true ||
                    anchoredEndSource?.isApproximate == true

    /**
     * Indica que existem âncoras e que todas possuem
     * origem convencional conhecida.
     *
     * Isso não representa precisão metrológica certificada.
     */
    val hasOnlyConventionalAnchors: Boolean
        get() =
            anchorCount > 0 &&
                    hasCompletePlacementProvenance &&
                    !hasApproximateAnchor

    /**
     * Mantém o significado original: existe rastreamento
     * e uma superfície convencional válida sob a mira.
     *
     * Instant Placement pode permitir uma solicitação explícita
     * mesmo quando esta propriedade for false.
     */
    val isReadyForAnchorPlacement: Boolean
        get() =
            trackingStatus.allowsAnchorPlacement &&
                    isSurfaceDetected
}