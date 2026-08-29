package org.aetheris.app.di

import org.aetheris.app.data.arcore.ArCoreFrameProcessor
import org.aetheris.app.data.arcore.ArCoreHitTestProcessor
import org.aetheris.app.data.arcore.ArCoreSessionManager
import org.aetheris.app.data.repository.SpatialSensorRepositoryImpl
import org.aetheris.app.domain.repository.SpatialSensorRepository
import org.aetheris.app.domain.usecase.CalculateDistanceUseCase
import org.aetheris.app.domain.usecase.EstimateSpatialDimensionsUseCase
import org.aetheris.app.domain.usecase.ProjectWorldToScreenUseCase
import org.aetheris.app.presentation.measurement.MeasurementViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { ArCoreSessionManager(androidContext()) }
    single { ArCoreFrameProcessor() }
    single { ArCoreHitTestProcessor() }

    single<SpatialSensorRepository> {
        SpatialSensorRepositoryImpl(
            frameProcessor = get(),
            hitTestProcessor = get()
        )
    }

    factory { CalculateDistanceUseCase() }
    factory { EstimateSpatialDimensionsUseCase() }
    factory { ProjectWorldToScreenUseCase() }

    viewModel {
        MeasurementViewModel(
            spatialSensorRepository = get(),
            calculateDistanceUseCase = get(),
            projectWorldToScreenUseCase = get()
        )
    }
}