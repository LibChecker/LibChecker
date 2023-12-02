package com.absinthe.libchecker.features.applist.detail.ui.impl

import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.databinding.FragmentLibNativeBinding
import com.absinthe.libchecker.features.applist.LocatedCount
import com.absinthe.libchecker.features.applist.Referable
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibStringDiffUtil
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.ui.base.BaseFilterAnalysisFragment
import com.absinthe.libchecker.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.utils.extensions.putArguments

class NativeAnalysisFragment : BaseFilterAnalysisFragment<FragmentLibNativeBinding>(), Referable {

  private var itemsList: List<LibStringItemChip>? = null

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = true

  override fun init() {
    binding.apply {
      list.apply {
        adapter = this@NativeAnalysisFragment.adapter
      }
    }

    viewModel.nativeLibItems.observe(viewLifecycleOwner) {
      if (it.isEmpty()) {
        emptyView.text.text = getString(R.string.empty_list)
      } else {
        adapter.processMap = viewModel.nativeSourceMap
        itemsList = it
        if (viewModel.queriedText?.isNotEmpty() == true) {
          filterList(viewModel.queriedText!!)
        } else {
          adapter.setDiffNewData(it.toMutableList(), afterListReadyTask)
        }
        if (viewModel.queriedProcess?.isNotEmpty() == true) {
          filterItems(viewModel.queriedProcess!!)
        }
      }

      if (!isListReady) {
        viewModel.itemsCountLiveData.value = LocatedCount(locate = type, count = it.size)
        viewModel.itemsCountList[type] = it.size
        isListReady = true
      }
    }

    adapter.apply {
      animationEnable = true
      setDiffCallback(LibStringDiffUtil())
      setEmptyView(emptyView)
    }

    viewModel.apply {
      packageInfoLiveData.observe(viewLifecycleOwner) {
        if (it != null) {
          viewModel.initSoAnalysisData()
        }
      }
      is64Bit.observe(viewLifecycleOwner) {
        if (it != null) {
          adapter.set64Bit(it)
        }
      }
    }
  }

  override fun onVisibilityChanged(visible: Boolean) {
    if (context != null && visible) {
      viewModel.processMapLiveData.postValue(viewModel.nativeSourceMap)
    } else {
      viewModel.processMapLiveData.postValue(viewModel.processesMap)
    }
    super.onVisibilityChanged(visible)
  }

  override fun getFilterList(process: String?): List<LibStringItemChip>? {
    return if (process.isNullOrEmpty()) {
      itemsList
    } else {
      itemsList?.filter { it.item.process == process }
    }
  }

  override fun getFilterListByText(text: String): List<LibStringItemChip>? {
    return viewModel.nativeLibItems.value?.filter { it.item.name.contains(text, true) }
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
