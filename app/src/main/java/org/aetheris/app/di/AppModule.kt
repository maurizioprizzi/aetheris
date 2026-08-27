package org.aetheris.app.di

import org.aetheris.app.data.arcore.ArCoreFrameProcessor
import org.aetheris.app.data.arcore.ArCoreHitTestProcessor
import org.aetheris.app.data.repository.SpatialSensorRepositoryImpl
import org.aetheris.app.domain.repository.SpatialSensorRepository
import org.aetheris.app.domain.usecase.CalculateDistanceUseCase
import org.aetheris.app.domain.usecase.EstimateSpatialDimensionsUseCase
import org.aetheris.app.presentation.measurement.MeasurementViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    // Processamento de Hardware & Óptica de Baixo Nível
    singleOf(::ArCoreFrameProcessor)
    singleOf(::ArCoreHitTestProcessor)

    // Repositórios
    singleOf(::SpatialSensorRepositoryImpl) { bind<SpatialSensorRepository>() }

    // Use Cases (Matemática Pura & Metrologia)
    factoryOf(::CalculateDistanceUseCase)
    factoryOf(::EstimateSpatialDimensionsUseCase)

    // ViewModels
    viewModel { MeasurementViewModel(get(), get()) }
}