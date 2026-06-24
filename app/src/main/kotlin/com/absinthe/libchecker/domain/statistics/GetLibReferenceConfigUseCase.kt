package com.absinthe.libchecker.domain.statistics

import com.absinthe.libchecker.constant.options.LibReferenceOptions

class GetLibReferenceConfigUseCase(
  private val settingsRepository: LibReferenceSettingsRepository
) {

  val threshold: Int
    get() = settingsRepository.threshold

  fun getReferenceConfig(): ComputeLibReferenceUseCase.ReferenceConfig {
    return ComputeLibReferenceUseCase.ReferenceConfig(
      showSystemApps = settingsRepository.showSystemApps,
      options = settingsRepository.options
    )
  }

  fun getMatchConfig(): ComputeLibReferenceUseCase.MatchConfig {
    return ComputeLibReferenceUseCase.MatchConfig(
      threshold = settingsRepository.threshold,
      onlyNotMarked = settingsRepository.options and LibReferenceOptions.ONLY_NOT_MARKED > 0
    )
  }
}
