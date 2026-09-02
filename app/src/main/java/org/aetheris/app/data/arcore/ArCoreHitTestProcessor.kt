package org.aetheris.app.data.arcore

import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.core.exceptions.NotTrackingException
import com.google.ar.core.exceptions.ResourceExhaustedException
import com.google.ar.core.exceptions.SessionPausedException
import org.aetheris.app.domain.model.AnchorPlacement
import org.aetheris.app.domain.model.AnchorPlacementSource
import org.aetheris.app.domain.model.Point3D

/**
 * Resultado nativo da criação de uma âncora ARCore.
 *
 * Mantém a referência da âncora na camada de dados e associa
 * a origem espacial necessária para o estado de domínio.
 */
data class ArCoreAnchorPlacement(
    val anchor: Anchor,
    val source: AnchorPlacementSource
) {

    val isApproximate: Boolean
        get() = source.isApproximate
}

/**
 * Processa hit tests realizados contra superfícies
 * e pontos espaciais reconhecidos pelo ARCore.
 *
 * O hit test convencional é sempre priorizado.
 * O Instant Placement é utilizado somente durante uma
 * solicitação explícita de posicionamento.
 */
class ArCoreHitTestProcessor(
    private val approximateDistanceMeters: Float =
        DEFAULT_APPROXIMATE_DISTANCE_METERS,
    private val diagnosticsEnabled: Boolean = true
) {

    init {
        require(
            approximateDistanceMeters.isFinite() &&
                    approximateDistanceMeters > 0f
        ) {
            "A distância aproximada deve ser finita e maior que zero."
        }
    }

    /**
     * Verifica se existe geometria convencional reconhecida
     * pelo ARCore na coordenada indicada.
     *
     * Esta sondagem pode ser executada periodicamente e, por isso,
     * não utiliza Instant Placement nem produz logs diagnósticos.
     */
    fun hasValidSurfaceAt(
        frame: Frame,
        xPx: Float,
        yPx: Float
    ): Boolean {
        return findValidHit(
            frame = frame,
            xPx = xPx,
            yPx = yPx,
            allowInstantPlacement = false,
            diagnosticOperation = null
        ) != null
    }

    /**
     * Executa um hit test explícito e retorna somente sua posição.
     *
     * Mantido para compatibilidade com os consumidores existentes.
     * Novos consumidores que precisam conhecer a proveniência devem
     * utilizar [performHitTestWithSource].
     */
    fun performHitTest(
        frame: Frame,
        xPx: Float,
        yPx: Float
    ): Point3D? {
        return performHitTestWithSource(
            frame = frame,
            xPx = xPx,
            yPx = yPx
        )?.position
    }

    /**
     * Executa um hit test explícito e retorna a posição juntamente
     * com a origem espacial selecionada.
     */
    fun performHitTestWithSource(
        frame: Frame,
        xPx: Float,
        yPx: Float
    ): AnchorPlacement? {
        val selection =
            findValidHit(
                frame = frame,
                xPx = xPx,
                yPx = yPx,
                allowInstantPlacement = true,
                diagnosticOperation =
                    OPERATION_PERFORM_HIT_TEST
            ) ?: return null

        return try {
            val point =
                selection.hit.hitPose.toPoint3D()

            diagnostic(
                operation = OPERATION_PERFORM_HIT_TEST,
                message =
                    "pose resolved: x=${point.x}, " +
                            "y=${point.y}, z=${point.z}, " +
                            "source=${selection.source}"
            )

            AnchorPlacement(
                position = point,
                source = selection.source
            )
        } catch (exception: RuntimeException) {
            diagnosticWarning(
                operation = OPERATION_PERFORM_HIT_TEST,
                message =
                    "failed to read hit pose: " +
                            exception.description()
            )

            null
        }
    }

    /**
     * Cria uma âncora nativa e retorna somente sua referência.
     *
     * Mantido para compatibilidade com os consumidores existentes.
     * Novos consumidores que precisam conhecer a proveniência devem
     * utilizar [createAnchorAtWithSource].
     */
    fun createAnchorAt(
        frame: Frame,
        xPx: Float,
        yPx: Float
    ): Anchor? {
        return createAnchorAtWithSource(
            frame = frame,
            xPx = xPx,
            yPx = yPx
        )?.anchor
    }

    /**
     * Cria uma âncora nativa e retorna também a origem
     * espacial do hit utilizado.
     */
    fun createAnchorAtWithSource(
        frame: Frame,
        xPx: Float,
        yPx: Float
    ): ArCoreAnchorPlacement? {
        val selection =
            findValidHit(
                frame = frame,
                xPx = xPx,
                yPx = yPx,
                allowInstantPlacement = true,
                diagnosticOperation =
                    OPERATION_CREATE_ANCHOR
            )

        if (selection == null) {
            diagnosticWarning(
                operation = OPERATION_CREATE_ANCHOR,
                message = "anchor not created: no valid hit"
            )

            return null
        }

        return try {
            val anchor =
                selection.hit.createAnchor()

            diagnostic(
                operation = OPERATION_CREATE_ANCHOR,
                message =
                    "anchor created successfully from " +
                            describeSelection(selection)
            )

            ArCoreAnchorPlacement(
                anchor = anchor,
                source = selection.source
            )
        } catch (exception: NotTrackingException) {
            logAnchorFailure(exception)
            null
        } catch (exception: SessionPausedException) {
            logAnchorFailure(exception)
            null
        } catch (exception: DeadlineExceededException) {
            logAnchorFailure(exception)
            null
        } catch (exception: ResourceExhaustedException) {
            logAnchorFailure(exception)
            null
        } catch (exception: RuntimeException) {
            logAnchorFailure(exception)
            null
        }
    }

    /**
     * Procura primeiro geometria convencional e utiliza
     * Instant Placement somente quando permitido.
     */
    private fun findValidHit(
        frame: Frame,
        xPx: Float,
        yPx: Float,
        allowInstantPlacement: Boolean,
        diagnosticOperation: String?
    ): SelectedHit? {
        if (!areValidScreenCoordinates(xPx, yPx)) {
            diagnosticOperation?.let { operation ->
                diagnosticWarning(
                    operation = operation,
                    message =
                        "invalid screen coordinates: " +
                                "x=$xPx, y=$yPx"
                )
            }

            return null
        }

        return try {
            val cameraTrackingState =
                frame.camera.trackingState

            diagnosticOperation?.let { operation ->
                diagnostic(
                    operation = operation,
                    message =
                        "request started: x=$xPx, y=$yPx, " +
                                "camera=$cameraTrackingState, " +
                                "instantAllowed=$allowInstantPlacement"
                )
            }

            if (
                cameraTrackingState !=
                TrackingState.TRACKING
            ) {
                diagnosticOperation?.let { operation ->
                    diagnosticWarning(
                        operation = operation,
                        message =
                            "request rejected: camera is " +
                                    cameraTrackingState
                    )
                }

                return null
            }

            val conventionalResults =
                findConventionalHits(
                    frame = frame,
                    xPx = xPx,
                    yPx = yPx
                )

            val conventionalSelection =
                conventionalResults
                    .firstNotNullOfOrNull { hit ->
                        resolveConventionalSource(hit)
                            ?.let { source ->
                                SelectedHit(
                                    hit = hit,
                                    source = source
                                )
                            }
                    }

            diagnosticOperation?.let { operation ->
                diagnostic(
                    operation = operation,
                    message =
                        "conventional results=" +
                                conventionalResults.size +
                                ", valid=" +
                                (conventionalSelection != null) +
                                ", hits=" +
                                describeHits(conventionalResults)
                )
            }

            if (conventionalSelection != null) {
                diagnosticOperation?.let { operation ->
                    diagnostic(
                        operation = operation,
                        message =
                            "selected conventional hit: " +
                                    describeSelection(
                                        conventionalSelection
                                    )
                    )
                }

                return conventionalSelection
            }

            if (!allowInstantPlacement) {
                return null
            }

            val instantResults =
                findInstantPlacementHits(
                    frame = frame,
                    xPx = xPx,
                    yPx = yPx
                )

            val instantSelection =
                instantResults
                    .firstOrNull { hit ->
                        isValidInstantPlacementHit(hit)
                    }
                    ?.let { hit ->
                        SelectedHit(
                            hit = hit,
                            source =
                                AnchorPlacementSource
                                    .INSTANT_PLACEMENT
                        )
                    }

            diagnosticOperation?.let { operation ->
                diagnostic(
                    operation = operation,
                    message =
                        "instant results=" +
                                instantResults.size +
                                ", valid=" +
                                (instantSelection != null) +
                                ", approximateDistance=" +
                                approximateDistanceMeters +
                                ", hits=" +
                                describeHits(instantResults)
                )
            }

            if (instantSelection != null) {
                diagnosticOperation?.let { operation ->
                    diagnostic(
                        operation = operation,
                        message =
                            "selected instant hit: " +
                                    describeSelection(
                                        instantSelection
                                    )
                    )
                }
            } else {
                diagnosticOperation?.let { operation ->
                    diagnosticWarning(
                        operation = operation,
                        message =
                            "no valid conventional or " +
                                    "instant-placement hit"
                    )
                }
            }

            instantSelection
        } catch (exception: RuntimeException) {
            diagnosticOperation?.let { operation ->
                diagnosticWarning(
                    operation = operation,
                    message =
                        "hit-test pipeline failed: " +
                                exception.description()
                )
            }

            null
        }
    }

    private fun findConventionalHits(
        frame: Frame,
        xPx: Float,
        yPx: Float
    ): List<HitResult> {
        return frame.hitTest(
            xPx,
            yPx
        )
    }

    private fun findInstantPlacementHits(
        frame: Frame,
        xPx: Float,
        yPx: Float
    ): List<HitResult> {
        return frame.hitTestInstantPlacement(
            xPx,
            yPx,
            approximateDistanceMeters
        )
    }

    /**
     * Valida um hit convencional e identifica sua origem.
     */
    private fun resolveConventionalSource(
        hit: HitResult
    ): AnchorPlacementSource? {
        val trackable =
            hit.trackable

        if (
            trackable.trackingState !=
            TrackingState.TRACKING
        ) {
            return null
        }

        return when (trackable) {
            is Plane -> {
                if (
                    trackable.isPoseInPolygon(
                        hit.hitPose
                    )
                ) {
                    AnchorPlacementSource.PLANE
                } else {
                    null
                }
            }

            is Point -> {
                if (
                    trackable.orientationMode ==
                    Point.OrientationMode
                        .ESTIMATED_SURFACE_NORMAL
                ) {
                    AnchorPlacementSource.FEATURE_POINT
                } else {
                    null
                }
            }

            is DepthPoint -> {
                AnchorPlacementSource.DEPTH_POINT
            }

            else -> null
        }
    }

    private fun isValidInstantPlacementHit(
        hit: HitResult
    ): Boolean {
        val trackable =
            hit.trackable

        return trackable is InstantPlacementPoint &&
                trackable.trackingState ==
                TrackingState.TRACKING
    }

    private fun describeHits(
        hits: List<HitResult>
    ): String {
        if (hits.isEmpty()) {
            return "[]"
        }

        return hits.joinToString(
            prefix = "[",
            postfix = "]",
            separator = "; "
        ) { hit ->
            describeHit(hit)
        }
    }

    private fun describeSelection(
        selection: SelectedHit
    ): String {
        return describeHit(selection.hit) +
                ", source=${selection.source}"
    }

    private fun describeHit(
        hit: HitResult
    ): String {
        return try {
            val trackable =
                hit.trackable

            val baseDescription =
                "type=${trackable.javaClass.simpleName}, " +
                        "tracking=${trackable.trackingState}"

            if (trackable is InstantPlacementPoint) {
                val trackingMethod = try {
                    trackable.trackingMethod.toString()
                } catch (_: RuntimeException) {
                    "unavailable"
                }

                "$baseDescription, method=$trackingMethod"
            } else {
                baseDescription
            }
        } catch (exception: RuntimeException) {
            "unreadable=${exception.description()}"
        }
    }

    private fun logAnchorFailure(
        exception: RuntimeException
    ) {
        diagnosticWarning(
            operation = OPERATION_CREATE_ANCHOR,
            message =
                "anchor creation failed: " +
                        exception.description()
        )
    }

    private fun diagnostic(
        operation: String,
        message: String
    ) {
        if (!diagnosticsEnabled) {
            return
        }

        try {
            Log.d(
                DIAGNOSTIC_TAG,
                "[$operation] $message"
            )
        } catch (_: RuntimeException) {
            // android.util.Log não está disponível nos testes JVM puros.
        }
    }

    private fun diagnosticWarning(
        operation: String,
        message: String
    ) {
        if (!diagnosticsEnabled) {
            return
        }

        try {
            Log.w(
                DIAGNOSTIC_TAG,
                "[$operation] $message"
            )
        } catch (_: RuntimeException) {
            // android.util.Log não está disponível nos testes JVM puros.
        }
    }

    private fun RuntimeException.description(): String {
        val detail =
            message?.takeIf { it.isNotBlank() }

        return if (detail == null) {
            javaClass.simpleName
        } else {
            "${javaClass.simpleName}: $detail"
        }
    }

    private fun areValidScreenCoordinates(
        xPx: Float,
        yPx: Float
    ): Boolean {
        return xPx.isFinite() &&
                yPx.isFinite() &&
                xPx >= 0f &&
                yPx >= 0f
    }

    private fun Pose.toPoint3D(): Point3D {
        return Point3D(
            x = tx(),
            y = ty(),
            z = tz()
        )
    }

    private data class SelectedHit(
        val hit: HitResult,
        val source: AnchorPlacementSource
    )

    private companion object {
        const val DEFAULT_APPROXIMATE_DISTANCE_METERS =
            1.5f

        const val DIAGNOSTIC_TAG =
            "AetherisHitTest"

        const val OPERATION_PERFORM_HIT_TEST =
            "performHitTest"

        const val OPERATION_CREATE_ANCHOR =
            "createAnchorAt"
    }
}
