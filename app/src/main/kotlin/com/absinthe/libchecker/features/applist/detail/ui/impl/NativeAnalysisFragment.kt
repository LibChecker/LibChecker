package com.absinthe.libchecker.features.applist.detail.ui.impl

import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.databinding.FragmentLibNativeBinding
import com.absinthe.libchecker.features.applist.Referable
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibStringDiffUtil
import com.absinthe.libchecker.features.applist.detail.ui.base.BaseFilterAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.extensions.putArguments
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class NativeAnalysisFragment :
  BaseFilterAnalysisFragment<FragmentLibNativeBinding>(),
  Referable {

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = true

  override fun init() {
    binding.apply {
      list.apply {
        adapter = this@NativeAnalysisFragment.adapter
      }
    }

    adapter.apply {
      animationEnable = true
      setDiffCallback(LibStringDiffUtil())
      setEmptyView(emptyView)
    }

    viewModel.nativeLibItems.value?.let {
      setList(it)
    }

    viewModel.apply {
      nativeLibItems.onEach {
        if (it == null) return@onEach
        setList(it)
      }.launchIn(lifecycleScope)
      packageInfoStateFlow.onEach {
        if (it != null) {
          viewModel.initSoAnalysisData()
        }
      }.launchIn(lifecycleScope)
      is64Bit.onEach {
        if (it != null) {
          adapter.set64Bit(it)
        }
      }.launchIn(lifecycleScope)
    }
  }

  override fun onVisibilityChanged(visible: Boolean) {
    if (context != null && visible) {
      viewModel.updateProcessMap(viewModel.nativeSourceMap)
    } else {
      viewModel.updateProcessMap(viewModel.processesMap)
    }
    super.onVisibilityChanged(visible)
  }

  override fun getFilterList(process: String?): List<LibStringItemChip>? {
    return if (process.isNullOrEmpty()) {
      viewModel.nativeLibItems.value
    } else {
      viewModel.nativeLibItems.value?.filter { it.item.process == process }
    }
  }

  private fun setList(list: List<LibStringItemChip>) {
    if (list.isEmpty()) {
      emptyView.text.text = getString(R.string.empty_list)
    } else {
      adapter.processMap = viewModel.nativeSourceMap
      if (viewModel.queriedText?.isNotEmpty() == true) {
        filterList(viewModel.queriedText!!)
      } else {
        adapter.setDiffNewData(list.toMutableList(), afterListReadyTask)
      }
      if (viewModel.queriedProcess?.isNotEmpty() == true) {
        filterItems(viewModel.queriedProcess!!)
      }
    }

    if (!isListReady) {
      viewModel.updateItemsCountStateFlow(type, list.size)
      isListReady = true
    }
  }

  companion object {
    fun newInstance(packageName: String): NativeAnalysisFragment {
      return NativeAnalysisFragment().putArguments(
        EXTRA_PACKAGE_NAME to packageName,
        EXTRA_TYPE to NATIVE
      )
    }
  }
}
