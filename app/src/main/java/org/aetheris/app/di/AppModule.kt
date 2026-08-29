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

    // Gerenciamento da sessão ARCore.
    single {
        ArCoreSessionManager(
            context = androidContext()
        )
    }

    // Processadores ARCore.
    single {
        ArCoreFrameProcessor()
    }

    single {
        ArCoreHitTestProcessor()
    }

    // Implementação concreta do repositório.
    single {
        val sessionManager = get<ArCoreSessionManager>()

        SpatialSensorRepositoryImpl(
            frameProcessor = get<ArCoreFrameProcessor>(),
            hitTestProcessor = get<ArCoreHitTestProcessor>(),
            isDepthEnabledProvider = {
                sessionManager.isDepthEnabled
            }
        )
    }

    // A interface utiliza a mesma instância concreta.
    single<SpatialSensorRepository> {
        get<SpatialSensorRepositoryImpl>()
    }

    // Casos de uso.
    factory {
        CalculateDistanceUseCase()
    }

    factory {
        EstimateSpatialDimensionsUseCase()
    }

    factory {
        ProjectWorldToScreenUseCase()
    }

    // ViewModel da tela de medição.
    viewModel {
        MeasurementViewModel(
            spatialSensorRepository = get<SpatialSensorRepository>(),
            calculateDistanceUseCase = get<CalculateDistanceUseCase>(),
            projectWorldToScreenUseCase = get<ProjectWorldToScreenUseCase>()
        )
    }
}