package com.absinthe.libchecker.ui.fragment

import android.content.Context
import androidx.fragment.app.activityViewModels
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.recyclerview.adapter.LibStringAdapter
import com.absinthe.libchecker.ui.detail.IDetailContainer
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.ui.fragment.detail.Sortable
import com.absinthe.libchecker.ui.fragment.detail.impl.EXTRA_TYPE
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.viewmodel.DetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IDetailContainer) {
            context.detailFragmentManager.register(type, this)
        }
    }

    override fun onDetach() {
        super.onDetach()
        if (requireContext() is IDetailContainer) {
            (requireContext() as IDetailContainer).detailFragmentManager.unregister(type)
        }
    }

    override suspend fun sort() {
        coroutineScope {
            val rules = viewModel.repository.getAllRules()
            val list = if (viewModel.sortMode == MODE_SORT_BY_SIZE) {
                adapter.data.sortedByDescending { rules.any { find -> it.item.name == find.name } || LCAppUtils.findRuleRegex(it.item.name, type) != null }
            } else {
                if (type == NATIVE) {
                    adapter.data.sortedByDescending { it.item.size }
                } else {
                    adapter.data.sortedByDescending { it.item.name }
                }
            }
            withContext(Dispatchers.Main) {
                adapter.setDiffNewData(list.toMutableList())
            }
        }
    }

    fun getItemsCount() = adapter.itemCount
}