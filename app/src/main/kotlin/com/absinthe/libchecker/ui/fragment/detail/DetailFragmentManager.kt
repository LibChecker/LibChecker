package com.absinthe.libchecker.ui.fragment.detail

import android.util.SparseArray
import androidx.core.util.valueIterator
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.ui.fragment.BaseDetailFragment
import com.absinthe.libchecker.utils.SPUtils

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

    fun changeSortMode(mode: Int) {
        GlobalValues.libSortMode.value = mode
        SPUtils.putInt(Constants.PREF_LIB_SORT_MODE, mode)
    }

    fun navigateToComponent(component: String) {
        currentFragment?.setNavigatingToComponentTask(component)
    }
}