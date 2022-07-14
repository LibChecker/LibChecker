package com.absinthe.libchecker.ui.fragment

import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.ui.fragment.detail.LocatedCount
import com.absinthe.libchecker.utils.doOnMainThreadIdle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseComponentFragment<T : ViewBinding> : BaseDetailFragment<T>() {

  abstract fun getFilterProcessesList(process: String?): List<LibStringItemChip>?

  fun filterProcesses(process: String?) {
    getFilterProcessesList(process)?.let {
      lifecycleScope.launch(Dispatchers.Main) {
        if (it.isEmpty()) {
          if (getRecyclerView().itemDecorationCount > 0) {
            getRecyclerView().removeItemDecoration(dividerItemDecoration)
          }
          emptyView.text.text = getString(R.string.empty_list)
        }
        adapter.setDiffNewData(it.toMutableList()) {
          viewModel.itemsCountLiveData.value = LocatedCount(locate = type, count = it.size)
          viewModel.itemsCountList[type] = it.size
          doOnMainThreadIdle {
            //noinspection NotifyDataSetChanged
            adapter.notifyDataSetChanged()
          }
        }
      }
    }
  }
}
