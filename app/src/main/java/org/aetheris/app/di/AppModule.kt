package org.aetheris.app.di

import org.aetheris.app.data.repository.SpatialSensorRepositoryImpl
import org.aetheris.app.domain.repository.SpatialSensorRepository
import org.aetheris.app.domain.usecase.CalculateDistanceUseCase
import org.aetheris.app.domain.usecase.EstimateSpatialDimensionsUseCase
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    // Repositórios
    singleOf(::SpatialSensorRepositoryImpl) { bind<SpatialSensorRepository>() }

    // UseCases
    factoryOf(::CalculateDistanceUseCase)
    factoryOf(::EstimateSpatialDimensionsUseCase)
}