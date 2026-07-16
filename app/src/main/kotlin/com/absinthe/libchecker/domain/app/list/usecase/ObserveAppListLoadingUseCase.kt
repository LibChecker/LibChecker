package com.absinthe.libchecker.domain.app.list.usecase

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.repository.FeatureInitializationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class ObserveAppListLoadingUseCase(
  private val featureInitializationRepository: FeatureInitializationRepository
) {

  operator fun invoke(
    requestChangeRunning: Flow<Boolean>,
    items: Flow<List<LCItem>>
  ): Flow<Boolean> {
    return combine(
      requestChangeRunning,
      featureInitializationRepository.state,
      items
    ) { isRequestChangeRunning, featureInitializationState, appItems ->
      isRequestChangeRunning ||
        featureInitializationState.running ||
        appItems.any { item -> item.features == FEATURES_NOT_INITIALIZED }
    }.distinctUntilChanged()
  }

  private companion object {
    const val FEATURES_NOT_INITIALIZED = -1
  }
}
