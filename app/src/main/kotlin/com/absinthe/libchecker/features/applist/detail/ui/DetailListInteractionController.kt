package com.absinthe.libchecker.features.applist.detail.ui

import com.absinthe.libchecker.domain.app.AppDetailSettingsRepository
import com.absinthe.libchecker.domain.app.detail.model.LocatedCount
import com.absinthe.libchecker.domain.app.detail.ui.DetailFragmentManager
import com.absinthe.libchecker.domain.app.detail.ui.MODE_SORT_BY_LIB
import com.absinthe.libchecker.domain.app.detail.ui.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import kotlinx.coroutines.CoroutineScope

class DetailListInteractionController(
  private val viewModel: DetailViewModel,
  private val detailFragmentManager: DetailFragmentManager,
  private val appDetailSettingsRepository: AppDetailSettingsRepository,
  private val selectedType: () -> Int?,
  private val processBarController: DetailProcessBarController,
  private val coroutineScope: CoroutineScope,
  private val onCurrentItemsCountChanged: (Int) -> Unit
) {

  fun onSearchTextChanged(newText: String) {
    viewModel.filterState.queriedText = newText
    detailFragmentManager.deliverFilterItemsByText(newText, coroutineScope)
  }

  fun onProcessFilterChanged(process: String?) {
    viewModel.filterState.queriedProcess = process
    detailFragmentManager.deliverFilterItems(null, process, coroutineScope)
  }

  fun onItemsCountChanged(live: LocatedCount) {
    if (detailFragmentManager.currentItemsCount != live.count && selectedType() == live.locate) {
      updateCurrentItemsCount(live.count)
    }
  }

  fun onDetailTabSelected(type: Int) {
    val count = viewModel.filterState.itemsCountList[type]
    if (detailFragmentManager.currentItemsCount != count) {
      updateCurrentItemsCount(count)
    }
    detailFragmentManager.selectedPosition = type
  }

  fun toggleProcessMode() {
    val processMode = !appDetailSettingsRepository.processMode
    appDetailSettingsRepository.setProcessMode(processMode)
    detailFragmentManager.deliverProcessMode(processMode)

    processBarController.refreshVisibility()
    if (!processMode) {
      doOnMainThreadIdle {
        viewModel.filterState.queriedProcess = null
        detailFragmentManager.deliverFilterItems(null, null, coroutineScope)
      }
    }
  }

  fun toggleSortMode() {
    val sortMode = if (appDetailSettingsRepository.sortMode == MODE_SORT_BY_LIB) {
      MODE_SORT_BY_SIZE
    } else {
      MODE_SORT_BY_LIB
    }
    appDetailSettingsRepository.setSortMode(sortMode)
    detailFragmentManager.sortAll(coroutineScope)
  }

  private fun updateCurrentItemsCount(count: Int) {
    onCurrentItemsCountChanged(count)
    detailFragmentManager.currentItemsCount = count
  }
}
