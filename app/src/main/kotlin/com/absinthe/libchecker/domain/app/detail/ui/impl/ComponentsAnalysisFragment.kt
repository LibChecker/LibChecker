package com.absinthe.libchecker.domain.app.detail.ui.impl

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.ui.Referable
import com.absinthe.libchecker.domain.app.detail.ui.adapter.LibStringDiffUtil
import com.absinthe.libchecker.domain.app.detail.ui.base.BaseDetailFragment
import com.absinthe.libchecker.domain.app.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.utils.extensions.putArguments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ComponentsAnalysisFragment :
  BaseDetailFragment<FragmentLibComponentBinding>(),
  Referable {

  override val needShowLibDetailDialog: Boolean = true

  override fun getRecyclerView(): RecyclerView = binding.list

  override suspend fun getItems(): List<LibStringItemChip> {
    val flow = viewModel.contentState.componentsMap[adapter.type]
    return flow.value ?: flow.filterNotNull().first()
  }

  override fun onItemsAvailable(items: List<LibStringItemChip>) {
    if (items.isEmpty()) {
      emptyView.text.text = getString(R.string.empty_list)
    } else {
      adapter.processMap = viewModel.contentState.processesMap
      lifecycleScope.launch(Dispatchers.IO) {
        setItemsWithFilter(items, viewModel.filterState.queriedText, viewModel.filterState.queriedProcess)
      }
    }
    if (!isListReady) {
      viewModel.filterState.updateItemsCount(type, items.size)
      isListReady = true
    }
  }

  override fun init() {
    binding.list.adapter = adapter

    adapter.apply {
      setDiffCallback(LibStringDiffUtil())
      stateView = this@ComponentsAnalysisFragment.emptyView
      isStateViewEnable = true
    }
  }

  companion object {
    fun newInstance(@LibType type: Int): ComponentsAnalysisFragment {
      return ComponentsAnalysisFragment().putArguments(EXTRA_TYPE to type)
    }
  }
}
