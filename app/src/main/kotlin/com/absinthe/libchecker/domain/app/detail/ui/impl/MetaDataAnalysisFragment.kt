package com.absinthe.libchecker.domain.app.detail.ui.impl

import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.ui.Referable
import com.absinthe.libchecker.domain.app.detail.ui.base.BaseDetailFragment
import com.absinthe.libchecker.domain.app.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.utils.extensions.putArguments

class MetaDataAnalysisFragment :
  BaseDetailFragment<FragmentLibComponentBinding>(),
  Referable {

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = false

  override suspend fun getItems(): List<LibStringItemChip> {
    return viewModel.contentState.metaDataItems.valueOrAwait()
  }

  override fun onItemsAvailable(items: List<LibStringItemChip>) {
    if (items.isEmpty()) {
      emptyView.text.text = getString(R.string.empty_list)
    } else {
      submitItemsWithFilter(items, viewModel.filterState.queriedText, null)
    }

    markListReady(items.size)
  }

  override fun init() {
    binding.list.adapter = adapter

    adapter.apply {
      animationEnable = false
      stateView = this@MetaDataAnalysisFragment.emptyView
      isStateViewEnable = true
    }

    if (viewModel.packageInfoStateFlow.value != null && viewModel.contentState.metaDataItems.value == null) {
      viewModel.initMetaDataData()
    }
  }

  companion object {
    fun newInstance(packageName: String): MetaDataAnalysisFragment {
      return MetaDataAnalysisFragment().putArguments(
        EXTRA_PACKAGE_NAME to packageName,
        EXTRA_TYPE to METADATA
      )
    }
  }
}
