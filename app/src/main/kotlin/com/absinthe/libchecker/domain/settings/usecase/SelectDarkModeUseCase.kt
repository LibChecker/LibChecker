package com.absinthe.libchecker.domain.settings.usecase

import com.absinthe.libchecker.domain.settings.repository.AppearanceSettingsRepository

class SelectDarkModeUseCase(
  private val appearanceSettingsRepository: AppearanceSettingsRepository
) {

  operator fun invoke(darkMode: String): Int {
    appearanceSettingsRepository.darkMode = darkMode
    return NightModeResolver.resolve(darkMode)
  }
}
