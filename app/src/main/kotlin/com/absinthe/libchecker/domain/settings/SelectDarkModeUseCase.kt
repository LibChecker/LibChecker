package com.absinthe.libchecker.domain.settings

class SelectDarkModeUseCase(
  private val appearanceSettingsRepository: AppearanceSettingsRepository
) {

  operator fun invoke(darkMode: String): Int {
    appearanceSettingsRepository.darkMode = darkMode
    return NightModeResolver.resolve(darkMode)
  }
}
