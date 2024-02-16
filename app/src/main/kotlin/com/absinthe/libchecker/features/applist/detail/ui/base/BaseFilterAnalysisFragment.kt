package com.absinthe.libchecker.features.applist.detail.ui.base

import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.R
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseFilterAnalysisFragment<T : ViewBinding> : BaseDetailFragment<T>() {

  abstract fun getFilterList(process: String?): List<LibStringItemChip>?

  fun filterItems(process: String?) {
    getFilterList(process)?.let {
      lifecycleScope.launch(Dispatchers.Main) {
        if (it.isEmpty()) {
          if (getRecyclerView().itemDecorationCount > 0) {
            getRecyclerView().removeItemDecoration(dividerItemDecoration)
          }
          emptyView.text.text = getString(R.string.empty_list)
        }
        adapter.setDiffNewData(it.toMutableList()) {
          viewModel.updateItemsCountStateFlow(type, it.size)
          doOnMainThreadIdle {
            //noinspection NotifyDataSetChanged
            adapter.notifyDataSetChanged()
          }
        }
      }
    }
  }
}
