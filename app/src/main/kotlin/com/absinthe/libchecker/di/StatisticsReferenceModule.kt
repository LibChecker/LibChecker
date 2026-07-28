package com.absinthe.libchecker.di

import com.absinthe.libchecker.data.statistics.GlobalLibReferenceSettingsRepository
import com.absinthe.libchecker.domain.statistics.reference.presentation.LibReferenceComputationController
import com.absinthe.libchecker.domain.statistics.reference.presentation.LibReferenceViewModel
import com.absinthe.libchecker.domain.statistics.reference.repository.LibReferenceSettingsRepository
import com.absinthe.libchecker.domain.statistics.reference.usecase.BuildLibReferenceDetailDialogRequestUseCase
import com.absinthe.libchecker.domain.statistics.reference.usecase.ComputeLibReferenceUseCase
import com.absinthe.libchecker.domain.statistics.reference.usecase.GetLibReferenceAppsUseCase
import com.absinthe.libchecker.domain.statistics.reference.usecase.GetLibReferenceConfigUseCase
import com.absinthe.libchecker.domain.statistics.reference.usecase.GetLibReferenceIconPackagesUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val statisticsReferenceModule = module {
  single<LibReferenceSettingsRepository> { GlobalLibReferenceSettingsRepository() }

  factory { ComputeLibReferenceUseCase(get()) }
  factory { GetLibReferenceConfigUseCase(get()) }
  factory { GetLibReferenceIconPackagesUseCase(get()) }
  factory { GetLibReferenceAppsUseCase(get()) }
  factory { BuildLibReferenceDetailDialogRequestUseCase() }
  factory { LibReferenceComputationController.Factory(get(), get(), get()) }

  viewModel {
    LibReferenceViewModel(
      appListRepository = get(),
      buildAppListItemViewStatesUseCase = get(),
      getLibReferenceAppsUseCase = get(),
      buildLibReferenceDetailDialogRequestUseCase = get(),
      libReferenceSettingsRepository = get(),
      libReferenceComputationControllerFactory = get()
    )
  }
}
