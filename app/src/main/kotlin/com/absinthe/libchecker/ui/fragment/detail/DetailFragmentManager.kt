package com.absinthe.libchecker.ui.fragment.detail

import android.util.SparseArray
import androidx.core.util.valueIterator
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.ui.fragment.BaseDetailFragment

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

  fun deliverFilter(text: String) {
    val iterator = map.valueIterator()
    var entry: BaseDetailFragment<*>
    while (iterator.hasNext()) {
      entry = iterator.next()
      entry.filterList(text)
    }
  }

  fun changeSortMode(mode: Int) {
    GlobalValues.libSortModeLiveData.value = mode
    GlobalValues.libSortMode = mode
  }

  fun navigateToComponent(@LibType refType: Int, component: String) {
    navType = refType
    navComponent = component
  }

  companion object {
    var navType: Int = -1
    var navComponent: String? = null

    fun resetNavigationParams() {
      navType = -1
      navComponent = null
    }
  }
}
