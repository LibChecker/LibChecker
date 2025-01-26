package com.absinthe.libchecker.features.applist

import android.util.SparseArray
import androidx.core.util.forEach
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.features.applist.detail.ui.base.BaseDetailFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
  private var deliverFilterJob: Job? = null

  fun register(position: Int, fragment: BaseDetailFragment<*>) {
    map.put(position, fragment)
  }

  fun unregister(position: Int) {
    map.remove(position)
  }

  suspend fun sortAll() {
    map.forEach { _, value ->
      value.sort()
    }
  }

  fun deliverFilterItemsByText(text: String, coroutineScope: CoroutineScope) {
    deliverFilterJob?.cancel()
    deliverFilterJob = coroutineScope.launch(Dispatchers.IO) {
      map.forEach { _, value ->
        value.setItemsWithFilter(text, null)
      }
    }
  }

  fun deliverSwitchProcessMode() {
    map.forEach { _, value ->
      value.switchProcessMode()
    }
  }

  fun deliverFilterItems(searchWords: String?, process: String?, coroutineScope: CoroutineScope) {
    deliverFilterJob?.cancel()
    deliverFilterJob = coroutineScope.launch(Dispatchers.IO) {
      map.forEach { _, value ->
        value.setItemsWithFilter(searchWords, process)
      }
    }
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
