package com.absinthe.libchecker.domain.statistics

class UpdateLibReferenceThresholdUseCase(
  private val libReferenceSettingsRepository: LibReferenceSettingsRepository
) {

  suspend operator fun invoke(threshold: Int) {
    libReferenceSettingsRepository.setThreshold(
      threshold.coerceIn(MIN_THRESHOLD, MAX_THRESHOLD)
    )
  }

  private companion object {
    const val MIN_THRESHOLD = 1
    const val MAX_THRESHOLD = 50
  }
}
