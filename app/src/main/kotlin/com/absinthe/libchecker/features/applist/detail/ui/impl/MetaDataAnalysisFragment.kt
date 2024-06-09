package com.absinthe.libchecker.features.applist.detail.ui.impl

import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.databinding.FragmentLibNativeBinding
import com.absinthe.libchecker.features.applist.Referable
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibStringDiffUtil
import com.absinthe.libchecker.features.applist.detail.ui.base.BaseDetailFragment
import com.absinthe.libchecker.features.applist.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.extensions.putArguments
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MetaDataAnalysisFragment :
  BaseDetailFragment<FragmentLibNativeBinding>(),
  Referable {

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = false

  override fun init() {
    binding.apply {
      list.apply {
        adapter = this@MetaDataAnalysisFragment.adapter
      }
    }

    adapter.apply {
      animationEnable = true
      setDiffCallback(LibStringDiffUtil())
      setEmptyView(emptyView)
    }

    viewModel.apply {
      metaDataItems.onEach {
        if (it == null) return@onEach
        if (it.isEmpty()) {
          emptyView.text.text = getString(R.string.empty_list)
        } else {
          if (viewModel.queriedText?.isNotEmpty() == true) {
            filterList(viewModel.queriedText!!)
          } else {
            context?.let {
              binding.list.addItemDecoration(dividerItemDecoration)
            }
            adapter.setDiffNewData(it.toMutableList(), afterListReadyTask)
          }
        }

        if (!isListReady) {
          viewModel.updateItemsCountStateFlow(type, it.size)
          isListReady = true
        }
      }.launchIn(lifecycleScope)
      packageInfoStateFlow.onEach {
        if (it != null) {
          viewModel.initMetaDataData()
        }
      }.launchIn(lifecycleScope)
    }

    if (viewModel.metaDataItems.value.isNullOrEmpty().not()) {
      lifecycleScope.launch {
        viewModel.metaDataItems.emit(viewModel.metaDataItems.value)
      }
    }
  }

  override fun getFilterListByText(text: String): List<LibStringItemChip> {
    return adapter.data.filter {
      it.item.name.contains(text, true) || it.item.source?.contains(text, true) == true
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
