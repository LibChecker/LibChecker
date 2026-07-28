package com.absinthe.libchecker.di

import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.data.app.update.AndroidAppUpdateRepository
import com.absinthe.libchecker.data.settings.GlobalAppearanceSettingsRepository
import com.absinthe.libchecker.data.settings.GlobalDeveloperSettingsRepository
import com.absinthe.libchecker.domain.app.detail.ui.ApkDetailActivity
import com.absinthe.libchecker.domain.app.update.AppUpdateRepository
import com.absinthe.libchecker.domain.app.update.BuildInAppUpdateDiffDataUseCase
import com.absinthe.libchecker.domain.settings.presentation.SettingsViewModel
import com.absinthe.libchecker.domain.settings.presentation.SettingsWorkflow
import com.absinthe.libchecker.domain.settings.repository.AppearanceSettingsRepository
import com.absinthe.libchecker.domain.settings.repository.DeveloperSettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
  single<AppearanceSettingsRepository> { GlobalAppearanceSettingsRepository() }
  single<DeveloperSettingsRepository> { GlobalDeveloperSettingsRepository() }
  single<AppUpdateRepository> { AndroidAppUpdateRepository(androidContext()) }

  factory { BuildInAppUpdateDiffDataUseCase(BuildConfig.APPLICATION_ID, androidContext().packageManager, get()) }
  factory {
    SettingsWorkflow(
      context = androidContext(),
      applicationId = BuildConfig.APPLICATION_ID,
      apkAnalysisActivityClassName = ApkDetailActivity::class.java.name,
      appUpdateRepository = get(),
      appListSettingsRepository = get(),
      appearanceSettingsRepository = get(),
      cloudRulesRepository = get(),
      ruleSettingsRepository = get(),
      snapshotSettingsRepository = get(),
      exportInstalledAppsToUriUseCase = get(),
      libReferenceSettingsRepository = get()
    )
  }

  viewModel {
    SettingsViewModel(
      settingsWorkflow = get()
    )
  }
}
