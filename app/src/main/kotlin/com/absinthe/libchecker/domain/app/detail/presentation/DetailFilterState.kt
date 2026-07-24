package com.absinthe.libchecker.domain.app.detail.presentation

import com.absinthe.libchecker.domain.app.detail.model.LocatedCount
import kotlinx.coroutines.flow.MutableStateFlow

class DetailFilterState {
  val itemsCountStateFlow: MutableStateFlow<LocatedCount> = MutableStateFlow(LocatedCount(0, 0))
  val processToolIconVisibilityStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
  val processMapStateFlow = MutableStateFlow<Map<String, Int>>(emptyMap())
  val itemsCountList = MutableList(12) { 0 }

  var queriedText: String? = null
  var queriedProcess: String? = null

  fun reset() {
    itemsCountStateFlow.value = LocatedCount(0, 0)
    processToolIconVisibilityStateFlow.value = false
    processMapStateFlow.value = emptyMap()
    itemsCountList.fill(0)
    queriedText = null
    queriedProcess = null
  }

  fun updateItemsCount(locate: Int, count: Int) {
    itemsCountStateFlow.value = LocatedCount(locate, count)
    itemsCountList[locate] = count
  }

  fun updateProcessFilterData(data: DetailProcessFilterData) {
    processMapStateFlow.value = data.processMap
    processToolIconVisibilityStateFlow.value = data.processToolIconVisible
  }
}
