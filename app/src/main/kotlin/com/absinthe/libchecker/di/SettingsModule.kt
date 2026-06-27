package com.absinthe.libchecker.di

import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.data.settings.GlobalAppearanceSettingsRepository
import com.absinthe.libchecker.data.settings.GlobalDeveloperSettingsRepository
import com.absinthe.libchecker.domain.settings.presentation.SettingsViewModel
import com.absinthe.libchecker.domain.settings.repository.AppearanceSettingsRepository
import com.absinthe.libchecker.domain.settings.repository.DeveloperSettingsRepository
import com.absinthe.libchecker.domain.settings.usecase.BuildGetUpdatesItemsUseCase
import com.absinthe.libchecker.domain.settings.usecase.BuildLocalePreferenceDataUseCase
import com.absinthe.libchecker.domain.settings.usecase.BuildLogShareIntentUseCase
import com.absinthe.libchecker.domain.settings.usecase.SelectDarkModeUseCase
import com.absinthe.libchecker.domain.settings.usecase.SelectLocaleUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
  single<AppearanceSettingsRepository> { GlobalAppearanceSettingsRepository() }
  single<DeveloperSettingsRepository> { GlobalDeveloperSettingsRepository() }

  factory { BuildGetUpdatesItemsUseCase(androidContext()) }
  factory { BuildLocalePreferenceDataUseCase(get()) }
  factory { BuildLogShareIntentUseCase(androidContext(), BuildConfig.APPLICATION_ID) }
  factory { SelectDarkModeUseCase(get()) }
  factory { SelectLocaleUseCase(get()) }

  viewModel {
    SettingsViewModel(
      appUpdateRepository = get(),
      appListSettingsRepository = get(),
      cloudRulesRepository = get(),
      ruleSettingsRepository = get(),
      snapshotSettingsRepository = get(),
      buildGetUpdatesItemsUseCase = get(),
      buildLocalePreferenceDataUseCase = get(),
      buildLogShareIntentUseCase = get(),
      exportInstalledAppsToUriUseCase = get(),
      selectDarkModeUseCase = get(),
      selectLocaleUseCase = get(),
      setApkAnalysisEnabledUseCase = get(),
      libReferenceSettingsRepository = get(),
      updateLibReferenceThresholdUseCase = get()
    )
  }
}
