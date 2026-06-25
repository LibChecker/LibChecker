package com.absinthe.libchecker.domain.statistics

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.FeatureInitializationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

class ObserveChartFeatureInitializationPlansUseCase(
  private val featureInitializationRepository: FeatureInitializationRepository
) {

  val initialPending: Boolean
    get() = featureInitializationRepository.isRunning

  operator fun invoke(items: Flow<List<LCItem>?>): Flow<ChartFeatureInitializationPlan> = flow {
    var previousPending = initialPending
    combine(
      items,
      featureInitializationRepository.state
    ) { appListItems, state ->
      val hasUninitializedFeatureItems = appListItems?.any { item ->
        item.features == FEATURES_NOT_INITIALIZED
      } == true
      state.running || (appListItems != null && !state.completed && hasUninitializedFeatureItems)
    }.distinctUntilChanged().collect { isPending ->
      emit(
        ChartFeatureInitializationPlan(
          isPending = isPending,
          shouldRefreshData = previousPending && !isPending
        )
      )
      previousPending = isPending
    }
  }

  private companion object {
    const val FEATURES_NOT_INITIALIZED = -1
  }
}

data class ChartFeatureInitializationPlan(
  val isPending: Boolean,
  val shouldRefreshData: Boolean
)
