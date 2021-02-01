package com.absinthe.libchecker.ui.fragment

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.recyclerview.adapter.LibStringAdapter
import com.absinthe.libchecker.ui.detail.IDetailContainer
import com.absinthe.libchecker.ui.fragment.applist.EXTRA_TYPE
import com.absinthe.libchecker.ui.fragment.applist.MODE_SORT_BY_LIB
import com.absinthe.libchecker.ui.fragment.applist.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.ui.fragment.applist.Sortable
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.viewmodel.DetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * <pre>
 * author : bozhen.zhao
 * time : 2020/11/27
 * </pre>
 */
abstract class BaseDetailFragment<T : ViewBinding>(layoutId: Int) : BaseFragment<T>(layoutId), Sortable {

    protected val viewModel by activityViewModels<DetailViewModel>()
    protected val type by lazy { arguments?.getInt(EXTRA_TYPE) ?: NATIVE }
    protected val adapter by lazy { LibStringAdapter(type) }
    protected var isListReady = false

    override fun onVisibilityChanged(visible: Boolean) {
        super.onVisibilityChanged(visible)
        if (visible) {
            (requireActivity() as IDetailContainer).currentFragment = this
        }
    }

    override fun sort() {
        lifecycleScope.launch(Dispatchers.IO) {
            val rules = viewModel.repository.getAllRules()
            val list = if (viewModel.sortMode == MODE_SORT_BY_SIZE) {
                viewModel.sortMode = MODE_SORT_BY_LIB
                adapter.data.sortedByDescending { rules.find { find -> it.item.name == find.name } != null }
            } else {
                viewModel.sortMode = MODE_SORT_BY_SIZE
                if (type == NATIVE) {
                    adapter.data.sortedByDescending { it.item.size }
                } else {
                    adapter.data.sortedByDescending { it.item.name }
                }
            }
            withContext(Dispatchers.Main) {
                adapter.setDiffNewData(list.toMutableList())
                GlobalValues.libSortMode.value = viewModel.sortMode
                SPUtils.putInt(Constants.PREF_LIB_SORT_MODE, viewModel.sortMode)
            }
        }
    }

    fun getItemsCount() = adapter.itemCount
}