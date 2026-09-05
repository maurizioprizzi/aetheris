package org.aetheris.app.presentation.measurement

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.aetheris.app.domain.model.AnchorPlacementSource
import org.aetheris.app.domain.model.AnchorSlot
import org.aetheris.app.domain.model.DimensionAxis
import org.aetheris.app.domain.model.MaterialDensity
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.SpatialDimensions
import org.aetheris.app.domain.model.SpatialFrameData
import org.aetheris.app.domain.model.TrackingStatus
import org.aetheris.app.domain.repository.SpatialSensorRepository
import org.aetheris.app.domain.usecase.CalculateDistanceUseCase
import org.aetheris.app.domain.usecase.CalculateMassUseCase
import org.aetheris.app.domain.usecase.CalculateVolumeUseCase
import org.aetheris.app.domain.usecase.ProjectWorldToScreenUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementViewModelTest {

    private val testDispatcher =
        StandardTestDispatcher()

    private class FakeSpatialSensorRepository :
        SpatialSensorRepository {

        private val _stream =
            MutableStateFlow(
                SpatialFrameData(
                    trackingStatus =
                        TrackingStatus.TRACKING,
                    isDepthEnabled = true,
                    pointCount = 150,
                    isSurfaceDetected = true,
                    anchoredStartPoint = null,
                    anchoredStartSource = null,
                    anchoredEndPoint = null,
                    anchoredEndSource = null
                )
            )

        override val spatialDataStream:
                StateFlow<SpatialFrameData> =
            _stream

        var clearAnchorsCallCount: Int = 0
            private set

        fun updateAnchors(
            startPoint: Point3D?,
            startSource: AnchorPlacementSource? = null,
            endPoint: Point3D?,
            endSource: AnchorPlacementSource? = null
        ) {
            _stream.value =
                _stream.value.copy(
                    anchoredStartPoint = startPoint,
                    anchoredStartSource = startSource,
                    anchoredEndPoint = endPoint,
                    anchoredEndSource = endSource
                )
        }

        override suspend fun performHitTest(
            normalizedX: Float,
            normalizedY: Float
        ): Point3D? {
            return null
        }

        override suspend fun createAnchor(
            normalizedX: Float,
            normalizedY: Float,
            slot: AnchorSlot
        ): Point3D? {
            return null
        }

        override fun clearAnchors() {
            clearAnchorsCallCount++
        }
    }

    private lateinit var fakeRepository:
            FakeSpatialSensorRepository

    private val calculateDistanceUseCase =
        CalculateDistanceUseCase()

    private val calculateVolumeUseCase =
        CalculateVolumeUseCase()

    private val calculateMassUseCase =
        CalculateMassUseCase()

    private val projectWorldToScreenUseCase:
            ProjectWorldToScreenUseCase =
        mockk(relaxed = true)

    private lateinit var viewModel:
            MeasurementViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(
            testDispatcher
        )

        fakeRepository =
            FakeSpatialSensorRepository()

        viewModel = MeasurementViewModel(
            spatialSensorRepository =
                fakeRepository,
            calculateDistanceUseCase =
                calculateDistanceUseCase,
            calculateVolumeUseCase =
                calculateVolumeUseCase,
            calculateMassUseCase =
                calculateMassUseCase,
            projectWorldToScreenUseCase =
                projectWorldToScreenUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state reflects repository stream`() {
        advanceUntilIdle()

        val state =
            viewModel.uiState.value

        assertThat(state.trackingStatus)
            .isEqualTo(
                TrackingStatus.TRACKING
            )

        assertThat(state.isDepthEnabled)
            .isTrue()

        assertThat(state.detectedPointsCount)
            .isEqualTo(150)

        assertThat(state.isTargetingSurface)
            .isTrue()

        assertThat(state.currentDimensionAxis)
            .isEqualTo(DimensionAxis.WIDTH)

        assertThat(state.spatialDimensions)
            .isEqualTo(SpatialDimensions.EMPTY)

        assertThat(state.volumeMeasurement)
            .isNull()

        assertThat(state.selectedMaterialDensity)
            .isNull()

        assertThat(state.massEstimate)
            .isNull()

        assertThat(state.selectedStartSource)
            .isNull()

        assertThat(state.selectedEndSource)
            .isNull()
    }

    @Test
    fun `repository anchor provenance is propagated to UI state`() {
        val startPoint =
            Point3D(
                x = 1f,
                y = 2f,
                z = 3f
            )

        val endPoint =
            Point3D(
                x = 4f,
                y = 5f,
                z = 6f
            )

        fakeRepository.updateAnchors(
            startPoint = startPoint,
            startSource =
                AnchorPlacementSource.PLANE,
            endPoint = endPoint,
            endSource =
                AnchorPlacementSource.INSTANT_PLACEMENT
        )

        advanceUntilIdle()

        val state =
            viewModel.uiState.value

        assertThat(state.selectedStartPoint)
            .isEqualTo(startPoint)

        assertThat(state.selectedStartSource)
            .isEqualTo(
                AnchorPlacementSource.PLANE
            )

        assertThat(state.selectedEndPoint)
            .isEqualTo(endPoint)

        assertThat(state.selectedEndSource)
            .isEqualTo(
                AnchorPlacementSource.INSTANT_PLACEMENT
            )

        assertThat(state.selectedStartPlacement)
            .isNotNull()

        assertThat(state.selectedEndPlacement)
            .isNotNull()

        assertThat(state.hasCompletePlacementProvenance)
            .isTrue()

        assertThat(state.hasApproximatePlacement)
            .isTrue()

        assertThat(state.currentMeasurement)
            .isNotNull()
    }

    @Test
    fun `source update is propagated when anchor position is unchanged`() {
        val startPoint =
            Point3D.ORIGIN

        fakeRepository.updateAnchors(
            startPoint = startPoint,
            startSource =
                AnchorPlacementSource.INSTANT_PLACEMENT,
            endPoint = null,
            endSource = null
        )

        advanceUntilIdle()

        assertThat(
            viewModel.uiState.value
                .selectedStartSource
        ).isEqualTo(
            AnchorPlacementSource.INSTANT_PLACEMENT
        )

        fakeRepository.updateAnchors(
            startPoint = startPoint,
            startSource =
                AnchorPlacementSource.PLANE,
            endPoint = null,
            endSource = null
        )

        advanceUntilIdle()

        val state =
            viewModel.uiState.value

        assertThat(state.selectedStartPoint)
            .isEqualTo(startPoint)

        assertThat(state.selectedStartSource)
            .isEqualTo(
                AnchorPlacementSource.PLANE
            )

        assertThat(state.hasApproximatePlacement)
            .isFalse()

        assertThat(state.hasOnlyConventionalPlacements)
            .isTrue()
    }

    @Test
    fun `onSurfaceDimensionsChanged updates viewport dimensions`() {
        viewModel.onSurfaceDimensionsChanged(
            widthPx = 1080,
            heightPx = 1920
        )

        val state =
            viewModel.uiState.value

        assertThat(state.viewportWidthPx)
            .isEqualTo(1080)

        assertThat(state.viewportHeightPx)
            .isEqualTo(1920)

        assertThat(state.hasValidViewport)
            .isTrue()
    }

    @Test
    fun `invalid surface dimensions are ignored`() {
        viewModel.onSurfaceDimensionsChanged(
            widthPx = 1080,
            heightPx = 1920
        )

        viewModel.onSurfaceDimensionsChanged(
            widthPx = 0,
            heightPx = -1
        )

        val state =
            viewModel.uiState.value

        assertThat(state.viewportWidthPx)
            .isEqualTo(1080)

        assertThat(state.viewportHeightPx)
            .isEqualTo(1920)
    }

    @Test
    fun `clearing current dimension removes points and provenance`() {
        fakeRepository.updateAnchors(
            startPoint = Point3D.ORIGIN,
            startSource =
                AnchorPlacementSource.PLANE,
            endPoint =
                Point3D(
                    x = 2f,
                    y = 0f,
                    z = 0f
                ),
            endSource =
                AnchorPlacementSource.INSTANT_PLACEMENT
        )

        advanceUntilIdle()

        assertThat(
            viewModel.uiState.value
                .hasApproximatePlacement
        ).isTrue()

        viewModel.onClearCurrentDimension()

        val state =
            viewModel.uiState.value

        assertThat(state.selectedStartPoint)
            .isNull()

        assertThat(state.selectedStartSource)
            .isNull()

        assertThat(state.selectedEndPoint)
            .isNull()

        assertThat(state.selectedEndSource)
            .isNull()

        assertThat(state.currentMeasurement)
            .isNull()

        assertThat(state.hasApproximatePlacement)
            .isFalse()

        assertThat(
            fakeRepository.clearAnchorsCallCount
        ).isEqualTo(1)
    }

    @Test
    fun `confirming dimension preserves provenance and clears active anchors`() {
        fakeRepository.updateAnchors(
            startPoint = Point3D.ORIGIN,
            startSource =
                AnchorPlacementSource.PLANE,
            endPoint =
                Point3D(
                    x = 2f,
                    y = 0f,
                    z = 0f
                ),
            endSource =
                AnchorPlacementSource.FEATURE_POINT
        )

        advanceUntilIdle()

        assertThat(
            viewModel.uiState.value
                .hasCompletePlacementProvenance
        ).isTrue()

        viewModel.onConfirmCurrentDimension()

        val state =
            viewModel.uiState.value

        assertThat(state.selectedStartPoint)
            .isNull()

        assertThat(state.selectedStartSource)
            .isNull()

        assertThat(state.selectedEndPoint)
            .isNull()

        assertThat(state.selectedEndSource)
            .isNull()

        assertThat(state.currentMeasurement)
            .isNull()

        assertThat(state.spatialDimensions.width)
            .isNotNull()

        val confirmedWidth =
            state.spatialDimensions
                .getDimensionMeasurement(
                    DimensionAxis.WIDTH
                )

        assertThat(confirmedWidth)
            .isNotNull()

        assertThat(confirmedWidth?.startSource)
            .isEqualTo(
                AnchorPlacementSource.PLANE
            )

        assertThat(confirmedWidth?.endSource)
            .isEqualTo(
                AnchorPlacementSource.FEATURE_POINT
            )

        assertThat(confirmedWidth?.hasCompleteProvenance)
            .isTrue()

        assertThat(
            state.spatialDimensions
                .hasCompleteProvenance
        ).isTrue()

        assertThat(
            fakeRepository.clearAnchorsCallCount
        ).isEqualTo(1)
    }

    @Test
    fun `confirming approximate dimension preserves instant placement warning`() {
        fakeRepository.updateAnchors(
            startPoint = Point3D.ORIGIN,
            startSource =
                AnchorPlacementSource.PLANE,
            endPoint =
                Point3D(
                    x = 2f,
                    y = 0f,
                    z = 0f
                ),
            endSource =
                AnchorPlacementSource.INSTANT_PLACEMENT
        )

        advanceUntilIdle()

        viewModel.onConfirmCurrentDimension()

        val state =
            viewModel.uiState.value

        val confirmedWidth =
            state.spatialDimensions
                .getDimensionMeasurement(
                    DimensionAxis.WIDTH
                )

        assertThat(confirmedWidth)
            .isNotNull()

        assertThat(confirmedWidth?.startSource)
            .isEqualTo(
                AnchorPlacementSource.PLANE
            )

        assertThat(confirmedWidth?.endSource)
            .isEqualTo(
                AnchorPlacementSource.INSTANT_PLACEMENT
            )

        assertThat(confirmedWidth?.usesApproximatePlacement)
            .isTrue()

        assertThat(confirmedWidth?.mayRefineOverTime)
            .isTrue()

        assertThat(
            state.spatialDimensions
                .usesApproximatePlacement
        ).isTrue()

        assertThat(
            state.spatialDimensions
                .mayRefineOverTime
        ).isTrue()

        assertThat(state.selectedStartSource)
            .isNull()

        assertThat(state.selectedEndSource)
            .isNull()
    }

    @Test
    fun `confirming dimensions advances width height and depth`() {
        captureAndConfirmDimension(
            meters = 2f
        )

        var state =
            viewModel.uiState.value

        assertThat(state.spatialDimensions.width?.meters)
            .isWithin(FLOAT_TOLERANCE)
            .of(2f)

        assertThat(state.currentDimensionAxis)
            .isEqualTo(DimensionAxis.HEIGHT)

        assertThat(state.volumeMeasurement)
            .isNull()

        captureAndConfirmDimension(
            meters = 3f
        )

        state =
            viewModel.uiState.value

        assertThat(state.spatialDimensions.height?.meters)
            .isWithin(FLOAT_TOLERANCE)
            .of(3f)

        assertThat(state.currentDimensionAxis)
            .isEqualTo(DimensionAxis.DEPTH)

        assertThat(state.volumeMeasurement)
            .isNull()

        captureAndConfirmDimension(
            meters = 4f
        )

        state =
            viewModel.uiState.value

        assertThat(state.spatialDimensions.depth?.meters)
            .isWithin(FLOAT_TOLERANCE)
            .of(4f)

        assertThat(state.currentDimensionAxis)
            .isNull()

        assertThat(state.hasCompleteSpatialDimensions)
            .isTrue()

        assertThat(
            state.spatialDimensions
                .provenanceAxisCount
        ).isEqualTo(3)

        assertThat(
            state.spatialDimensions
                .hasCompleteProvenance
        ).isTrue()

        assertThat(
            state.spatialDimensions
                .usesApproximatePlacement
        ).isFalse()

        assertThat(state.volumeMeasurement)
            .isNotNull()

        assertThat(
            state.volumeMeasurement!!.cubicMeters
        )
            .isWithin(FLOAT_TOLERANCE)
            .of(24f)

        assertThat(
            fakeRepository.clearAnchorsCallCount
        ).isEqualTo(3)
    }

    @Test
    fun `selecting material after volume calculates mass immediately`() {
        completeSpatialMeasurement()

        viewModel.onMaterialDensitySelected(
            materialDensity = testMaterial(
                density = 1_000f
            )
        )

        val state =
            viewModel.uiState.value

        assertThat(state.selectedMaterialDensity)
            .isNotNull()

        assertThat(state.massEstimate)
            .isNotNull()

        assertThat(state.massEstimate!!.kilograms)
            .isWithin(FLOAT_TOLERANCE)
            .of(24_000f)

        assertThat(
            state.massEstimate.densityUsedKgPerM3
        )
            .isWithin(FLOAT_TOLERANCE)
            .of(1_000f)

        assertThat(state.hasCompleteObjectEstimate)
            .isTrue()
    }

    @Test
    fun `selecting material before volume calculates mass after final dimension`() {
        viewModel.onMaterialDensitySelected(
            materialDensity = testMaterial(
                density = 500f
            )
        )

        assertThat(
            viewModel.uiState.value.massEstimate
        ).isNull()

        completeSpatialMeasurement()

        val state =
            viewModel.uiState.value

        assertThat(state.selectedMaterialDensity)
            .isNotNull()

        assertThat(state.massEstimate)
            .isNotNull()

        assertThat(state.massEstimate!!.kilograms)
            .isWithin(FLOAT_TOLERANCE)
            .of(12_000f)

        assertThat(state.hasCompleteObjectEstimate)
            .isTrue()
    }

    @Test
    fun `changing selected material recalculates mass`() {
        completeSpatialMeasurement()

        viewModel.onMaterialDensitySelected(
            materialDensity = testMaterial(
                name = "Material A",
                density = 1_000f
            )
        )

        val firstMass =
            viewModel.uiState.value
                .massEstimate
                ?.kilograms

        viewModel.onMaterialDensitySelected(
            materialDensity = testMaterial(
                name = "Material B",
                density = 500f
            )
        )

        val state =
            viewModel.uiState.value

        assertThat(firstMass)
            .isNotNull()

        assertThat(firstMass!!)
            .isWithin(FLOAT_TOLERANCE)
            .of(24_000f)

        assertThat(state.massEstimate)
            .isNotNull()

        assertThat(state.massEstimate!!.kilograms)
            .isWithin(FLOAT_TOLERANCE)
            .of(12_000f)

        assertThat(
            state.selectedMaterialDensity
                ?.materialName
        ).isEqualTo("Material B")
    }

    @Test
    fun `clearing selected material removes mass estimate`() {
        completeSpatialMeasurement()

        viewModel.onMaterialDensitySelected(
            materialDensity = testMaterial()
        )

        assertThat(
            viewModel.uiState.value.massEstimate
        ).isNotNull()

        viewModel.onClearSelectedMaterial()

        val state =
            viewModel.uiState.value

        assertThat(state.selectedMaterialDensity)
            .isNull()

        assertThat(state.massEstimate)
            .isNull()

        assertThat(state.hasCompleteSpatialMeasurement)
            .isTrue()

        assertThat(state.hasCompleteObjectEstimate)
            .isFalse()
    }

    @Test
    fun `reset clears dimensions volume material mass and provenance`() {
        completeSpatialMeasurement()

        viewModel.onMaterialDensitySelected(
            materialDensity = testMaterial()
        )

        fakeRepository.updateAnchors(
            startPoint = Point3D.ORIGIN,
            startSource =
                AnchorPlacementSource.INSTANT_PLACEMENT,
            endPoint = null,
            endSource = null
        )

        advanceUntilIdle()

        viewModel.onResetMeasurements()

        val state =
            viewModel.uiState.value

        assertThat(state.selectedStartPoint)
            .isNull()

        assertThat(state.selectedStartSource)
            .isNull()

        assertThat(state.selectedEndPoint)
            .isNull()

        assertThat(state.selectedEndSource)
            .isNull()

        assertThat(state.currentMeasurement)
            .isNull()

        assertThat(state.spatialDimensions)
            .isEqualTo(SpatialDimensions.EMPTY)

        assertThat(state.volumeMeasurement)
            .isNull()

        assertThat(state.selectedMaterialDensity)
            .isNull()

        assertThat(state.massEstimate)
            .isNull()

        assertThat(state.badgePosition)
            .isNull()

        assertThat(state.hasApproximatePlacement)
            .isFalse()

        assertThat(
            fakeRepository.clearAnchorsCallCount
        ).isEqualTo(4)
    }

    private fun completeSpatialMeasurement() {
        captureAndConfirmDimension(
            meters = 2f
        )

        captureAndConfirmDimension(
            meters = 3f
        )

        captureAndConfirmDimension(
            meters = 4f
        )
    }

    private fun captureAndConfirmDimension(
        meters: Float
    ) {
        fakeRepository.updateAnchors(
            startPoint = Point3D.ORIGIN,
            startSource =
                AnchorPlacementSource.PLANE,
            endPoint =
                Point3D(
                    x = meters,
                    y = 0f,
                    z = 0f
                ),
            endSource =
                AnchorPlacementSource.FEATURE_POINT
        )

        advanceUntilIdle()

        assertThat(
            viewModel.uiState.value
                .currentMeasurement
        ).isNotNull()

        assertThat(
            viewModel.uiState.value
                .hasCompletePlacementProvenance
        ).isTrue()

        viewModel.onConfirmCurrentDimension()

        fakeRepository.updateAnchors(
            startPoint = null,
            startSource = null,
            endPoint = null,
            endSource = null
        )

        advanceUntilIdle()
    }

    private fun testMaterial(
        name: String = "Material de teste",
        density: Float = 1_000f
    ): MaterialDensity {
        return MaterialDensity(
            materialName = name,
            kilogramsPerCubicMeter = density,
            uncertaintyKilogramsPerCubicMeter = 25f
        )
    }

    private fun advanceUntilIdle() {
        testDispatcher.scheduler
            .advanceUntilIdle()
    }

    private companion object {
        const val FLOAT_TOLERANCE = 1e-3f
    }
}