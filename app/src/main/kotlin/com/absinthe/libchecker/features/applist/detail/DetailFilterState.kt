package com.absinthe.libchecker.features.applist.detail

import com.absinthe.libchecker.features.applist.LocatedCount
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

  fun updateProcessMap(map: Map<String, Int>) {
    processMapStateFlow.value = map
  }

  fun updateProcessToolIconVisibility(visible: Boolean) {
    processToolIconVisibilityStateFlow.value = visible
  }
}
