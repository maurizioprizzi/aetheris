package org.aetheris.app.di

import org.aetheris.app.domain.usecase.CalculateDistanceUseCase
import org.aetheris.app.domain.usecase.EstimateSpatialDimensionsUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val appModule = module {
    factoryOf(::CalculateDistanceUseCase)
    factoryOf(::EstimateSpatialDimensionsUseCase)
}