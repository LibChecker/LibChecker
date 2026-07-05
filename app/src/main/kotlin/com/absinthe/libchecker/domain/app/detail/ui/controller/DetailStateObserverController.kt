package com.absinthe.libchecker.domain.app.detail.ui.controller

import com.absinthe.libchecker.domain.app.detail.model.LocatedCount
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.model.VersionedFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DetailStateObserverController(
  private val viewModel: DetailViewModel,
  private val coroutineScope: CoroutineScope,
  private val onItemsCountChanged: (LocatedCount) -> Unit,
  private val onProcessToolIconVisibilityChanged: (Boolean) -> Unit,
  private val onProcessMapChanged: (Map<String, Int>) -> Unit,
  private val onFeatureAdded: (VersionedFeature) -> Unit,
  private val onFeatureLoadingChanged: (Boolean) -> Unit,
  private val onAbiBundleChanged: (abi: Int, abiSet: Collection<Int>) -> Unit
) {

  fun observe() {
    viewModel.filterState.itemsCountStateFlow.onEach { live ->
      onItemsCountChanged(live)
    }.launchIn(coroutineScope)
    viewModel.filterState.processToolIconVisibilityStateFlow.onEach { visible ->
      onProcessToolIconVisibilityChanged(visible)
    }.launchIn(coroutineScope)
    viewModel.filterState.processMapStateFlow.onEach { map ->
      onProcessMapChanged(map)
    }.launchIn(coroutineScope)
    viewModel.featureState.featuresFlow.onEach { feature ->
      onFeatureAdded(feature)
    }.launchIn(coroutineScope)
    viewModel.featureState.isLoading.onEach { loading ->
      onFeatureLoadingChanged(loading)
    }.launchIn(coroutineScope)
    viewModel.featureState.abiBundleStateFlow.onEach { bundle ->
      if (bundle != null) {
        onAbiBundleChanged(bundle.abi, bundle.abiSet)
      }
    }.launchIn(coroutineScope)
  }
}
