package com.absinthe.libchecker.ui.fragment

import androidx.fragment.app.activityViewModels
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.librarymap.BaseMap
import com.absinthe.libchecker.recyclerview.adapter.LibStringAdapter
import com.absinthe.libchecker.ui.detail.IDetailContainer
import com.absinthe.libchecker.ui.fragment.applist.EXTRA_TYPE
import com.absinthe.libchecker.ui.fragment.applist.MODE_SORT_BY_LIB
import com.absinthe.libchecker.ui.fragment.applist.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.ui.fragment.applist.Sortable
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.viewmodel.DetailViewModel

/**
 * <pre>
 * author : bozhen.zhao
 * time : 2020/11/27
 * </pre>
 */
abstract class BaseDetailFragment<T : ViewBinding>(layoutId: Int) : BaseFragment<T>(layoutId), Sortable {

    protected val viewModel by activityViewModels<DetailViewModel>()
    protected val adapter by lazy { LibStringAdapter(arguments?.getInt(EXTRA_TYPE) ?: NATIVE) }
    protected var isListReady = false

    override fun onVisibilityChanged(visible: Boolean) {
        super.onVisibilityChanged(visible)
        if (visible) {
            (requireActivity() as IDetailContainer).currentFragment = this@BaseDetailFragment

            if (isListReady) {
                viewModel.itemsCountLiveData.value = adapter.data.size
            }
        }
    }

    override fun sort() {
        viewModel.sortMode = if (viewModel.sortMode == MODE_SORT_BY_SIZE) {
            val map = BaseMap.getMap(adapter.type)
            adapter.setDiffNewData(adapter.data.sortedByDescending { map.contains(it.item.name) }.toMutableList())
            MODE_SORT_BY_LIB
        } else {
            adapter.setDiffNewData(adapter.data.sortedByDescending { it.item.size }.toMutableList())
            MODE_SORT_BY_SIZE
        }
        GlobalValues.libSortMode.value = viewModel.sortMode
        SPUtils.putInt(Constants.PREF_LIB_SORT_MODE, viewModel.sortMode)
    }
}