package org.aetheris.app.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.aetheris.app.domain.model.AnchorSlot
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.SpatialFrameData

/**
 * Contrato de repositório para acesso aos sensores
 * espaciais e ao rastreamento do ARCore.
 */
interface SpatialSensorRepository {

    /**
     * Fluxo reativo do estado de rastreamento,
     * nuvem de pontos e âncoras.
     */
    val spatialDataStream: StateFlow<SpatialFrameData>

    /**
     * Executa um hit test usando coordenadas normalizadas
     * no intervalo [0.0, 1.0].
     *
     * Retorna o ponto no espaço métrico 3D ou null quando
     * nenhuma superfície física válida for encontrada.
     */
    suspend fun performHitTest(
        normalizedX: Float,
        normalizedY: Float
    ): Point3D?

    /**
     * Cria uma âncora em uma superfície física detectada
     * e a associa ao slot especificado.
     *
     * Caso o slot já possua uma âncora, a anterior
     * é desanexada e substituída.
     */
    suspend fun createAnchor(
        normalizedX: Float,
        normalizedY: Float,
        slot: AnchorSlot
    ): Point3D?

    /**
     * Desanexa as âncoras ativas e limpa
     * os pontos da medição atual.
     */
    fun clearAnchors()
}