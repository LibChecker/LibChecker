package com.absinthe.libchecker.features.applist

import android.util.SparseArray
import androidx.core.util.forEach
import androidx.core.util.valueIterator
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.features.applist.detail.ui.base.BaseDetailFragment
import com.absinthe.libchecker.features.applist.detail.ui.base.BaseFilterAnalysisFragment

const val MODE_SORT_BY_SIZE = 0
const val MODE_SORT_BY_LIB = 1

class DetailFragmentManager {
  var selectedPosition: Int = 0
  var currentItemsCount = -1

  var currentFragment: BaseDetailFragment<*>? = null
    get() = map[selectedPosition]
    private set

  var currentSortable: Sortable? = null
    get() = map[selectedPosition]
    private set

  private val map = SparseArray<BaseDetailFragment<*>>()

  fun register(position: Int, fragment: BaseDetailFragment<*>) {
    map.put(position, fragment)
  }

  fun unregister(position: Int) {
    map.remove(position)
  }

  suspend fun sortAll() {
    val iterator = map.valueIterator()
    var entry: BaseDetailFragment<*>
    while (iterator.hasNext()) {
      entry = iterator.next()
      entry.sort()
    }
  }

  fun deliverFilterItemsByText(text: String) {
    map.forEach { _, value ->
      value.filterList(text)
    }
  }

  fun deliverSwitchProcessMode() {
    map.forEach { _, value ->
      value.switchProcessMode()
    }
  }

  fun deliverFilterItems(condition: String?) {
    map.forEach { _, value ->
      (value as? BaseFilterAnalysisFragment)?.filterItems(condition)
    }
  }

  fun changeSortMode(mode: Int) {
    GlobalValues.libSortModeLiveData.value = mode
    GlobalValues.libSortMode = mode
  }

  fun navigateToComponent(@LibType refType: Int, component: String) {
    navType = refType
    navComponent = component

    if (navType != NAV_TYPE_NONE) {
      currentFragment?.setupListReadyTask()
    }
  }

  companion object {
    const val NAV_TYPE_NONE = -1
    var navType: Int = NAV_TYPE_NONE
    var navComponent: String? = null

    fun resetNavigationParams() {
      navType = NAV_TYPE_NONE
      navComponent = null
    }
  }
}
