package org.aetheris.app.domain.model

/**
 * Estado espacial consolidado produzido a partir
 * do frame mais recente do ARCore.
 *
 * @property trackingStatus Estado atual do rastreamento da câmera.
 * @property isDepthEnabled Indica se o Depth Mode está habilitado na sessão.
 * @property pointCount Quantidade de pontos que passaram pelo filtro de confiança.
 * @property isSurfaceDetected Indica se existe uma superfície física válida sob a mira.
 * @property anchoredStartPoint Posição no mundo da âncora inicial (Ponto A).
 * @property anchoredEndPoint Posição no mundo da âncora final (Ponto B).
 */
data class SpatialFrameData(
    val trackingStatus: TrackingStatus =
        TrackingStatus.UNAVAILABLE,
    val isDepthEnabled: Boolean = false,
    val pointCount: Int = 0,
    val isSurfaceDetected: Boolean = false,
    val anchoredStartPoint: Point3D? = null,
    val anchoredEndPoint: Point3D? = null
) {

    init {
        require(pointCount >= 0) {
            "A quantidade de pontos não pode ser negativa."
        }
    }

    val isTracking: Boolean
        get() = trackingStatus == TrackingStatus.TRACKING

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
        get() = hasStartAnchor && hasEndAnchor

    val isReadyForAnchorPlacement: Boolean
        get() =
            trackingStatus.allowsAnchorPlacement &&
                    isSurfaceDetected
}