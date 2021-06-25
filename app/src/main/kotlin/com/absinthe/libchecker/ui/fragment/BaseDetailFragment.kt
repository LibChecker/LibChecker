package com.absinthe.libchecker.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.recyclerview.adapter.detail.LibStringAdapter
import com.absinthe.libchecker.ui.detail.IDetailContainer
import com.absinthe.libchecker.ui.fragment.detail.DetailFragmentManager
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.ui.fragment.detail.Sortable
import com.absinthe.libchecker.ui.fragment.detail.impl.EXTRA_TYPE
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.viewmodel.DetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * <pre>
 * author : Absinthe
 * time : 2020/11/27
 * </pre>
 */
abstract class BaseDetailFragment<T : ViewBinding>(layoutId: Int) : BaseFragment<T>(layoutId), Sortable {

    protected val viewModel by activityViewModels<DetailViewModel>()
    protected val type by lazy { arguments?.getInt(EXTRA_TYPE) ?: NATIVE }
    protected val adapter by lazy { LibStringAdapter(type) }
    protected var isListReady = false
    protected var navigateToComponentTask: Runnable? = null

    abstract fun getRecyclerView(): RecyclerView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IDetailContainer) {
            context.detailFragmentManager.register(type, this)
        }
        if (DetailFragmentManager.navType == type) {
            DetailFragmentManager.navComponent?.let { navigateToComponentTask = Runnable { navigateToComponentImpl(it) } }
            DetailFragmentManager.resetNavigationParams()
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

    @SuppressLint("NotifyDataSetChanged")
    private fun navigateToComponentImpl(component: String) {
        val componentPosition = adapter.data.indexOfFirst { it.item.name == component }
        if (componentPosition == -1) {
            return
        }

        Timber.d("navigateToComponent: componentPosition = $componentPosition")
        getRecyclerView().scrollToPosition(componentPosition.coerceAtMost(adapter.itemCount - 1))

        with(getRecyclerView().layoutManager) {
            if (this is LinearLayoutManager) {
                scrollToPositionWithOffset(componentPosition, 0)
            } else if (this is StaggeredGridLayoutManager) {
                scrollToPositionWithOffset(componentPosition, 0)
            }
        }

        adapter.setHighlightBackgroundItem(componentPosition)
        adapter.notifyDataSetChanged()
    }

}