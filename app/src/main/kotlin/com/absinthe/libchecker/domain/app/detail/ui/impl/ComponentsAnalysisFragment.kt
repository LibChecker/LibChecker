package com.absinthe.libchecker.domain.app.detail.ui.impl

import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.ui.Referable
import com.absinthe.libchecker.domain.app.detail.ui.base.BaseDetailFragment
import com.absinthe.libchecker.domain.app.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.utils.extensions.putArguments
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

class ComponentsAnalysisFragment :
  BaseDetailFragment<FragmentLibComponentBinding>(),
  Referable {

  override val needShowLibDetailDialog: Boolean = true

  override fun getRecyclerView(): RecyclerView = binding.list

  override suspend fun getItems(): List<LibStringItemChip> {
    val flow = viewModel.contentState.componentsMap[type]
    return flow.value ?: flow.filterNotNull().first()
  }

  override fun onItemsAvailable(items: List<LibStringItemChip>) {
    if (items.isEmpty()) {
      emptyView.text.text = getString(R.string.empty_list)
    } else {
      bindProcessColors(viewModel.contentState.processesMap)
      submitItemsWithFilter(items, viewModel.filterState.queriedText, viewModel.filterState.queriedProcess)
    }
    if (!isListReady) {
      viewModel.filterState.updateItemsCount(type, items.size)
      isListReady = true
    }
  }

  override fun init() {
    binding.list.adapter = adapter

    adapter.apply {
      animationEnable = false
      stateView = this@ComponentsAnalysisFragment.emptyView
      isStateViewEnable = true
    }
  }

  override fun onVisibilityChanged(visible: Boolean) {
    super.onVisibilityChanged(visible)
    if (visible && viewModel.contentState.componentsMap[type]?.value == null && viewModel.packageInfoStateFlow.value != null) {
      viewModel.initComponentsData()
    }
  }

  companion object {
    fun newInstance(packageName: String, @LibType type: Int): ComponentsAnalysisFragment {
      return ComponentsAnalysisFragment().putArguments(
        EXTRA_PACKAGE_NAME to packageName,
        EXTRA_TYPE to type
      )
    }
  }
}
