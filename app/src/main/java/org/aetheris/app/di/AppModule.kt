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

    /**
     * Gerenciador do ciclo de vida da sessão ARCore.
     */
    single {
        ArCoreSessionManager(
            context = androidContext()
        )
    }

    /**
     * Processadores responsáveis pelos dados
     * produzidos pelo ARCore.
     */
    single {
        ArCoreFrameProcessor()
    }

    single {
        ArCoreHitTestProcessor()
    }

    /**
     * Implementação concreta do repositório espacial.
     */
    single {
        val sessionManager =
            get<ArCoreSessionManager>()

        SpatialSensorRepositoryImpl(
            frameProcessor =
                get<ArCoreFrameProcessor>(),
            hitTestProcessor =
                get<ArCoreHitTestProcessor>(),
            isDepthEnabledProvider = {
                sessionManager.isDepthEnabled
            }
        )
    }

    /**
     * Disponibiliza a mesma instância concreta
     * por meio da interface do domínio.
     */
    single<SpatialSensorRepository> {
        get<SpatialSensorRepositoryImpl>()
    }

    /**
     * Casos de uso do domínio.
     */
    factory {
        CalculateDistanceUseCase()
    }

    factory {
        EstimateSpatialDimensionsUseCase()
    }

    factory {
        ProjectWorldToScreenUseCase()
    }

    /**
     * ViewModel da tela de medição.
     */
    viewModel {
        MeasurementViewModel(
            spatialSensorRepository =
                get<SpatialSensorRepository>(),
            calculateDistanceUseCase =
                get<CalculateDistanceUseCase>(),
            projectWorldToScreenUseCase =
                get<ProjectWorldToScreenUseCase>()
        )
    }
}