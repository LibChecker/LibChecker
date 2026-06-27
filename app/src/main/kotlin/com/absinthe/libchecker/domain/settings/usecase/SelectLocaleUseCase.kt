package com.absinthe.libchecker.domain.settings.usecase

import com.absinthe.libchecker.domain.settings.repository.AppearanceSettingsRepository
import java.util.Locale

class SelectLocaleUseCase(
  private val appearanceSettingsRepository: AppearanceSettingsRepository
) {

  operator fun invoke(localeTag: String): Locale {
    appearanceSettingsRepository.localeTag = localeTag
    return if (localeTag == FOLLOW_SYSTEM) {
      Locale.getDefault()
    } else {
      Locale.forLanguageTag(localeTag)
    }
  }

  companion object {
    const val FOLLOW_SYSTEM = "SYSTEM"
  }
}
