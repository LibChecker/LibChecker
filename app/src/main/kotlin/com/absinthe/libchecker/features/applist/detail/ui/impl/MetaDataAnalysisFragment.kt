package com.absinthe.libchecker.features.applist.detail.ui.impl

import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.databinding.FragmentLibNativeBinding
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibStringDiffUtil
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.base.BaseDetailFragment
import com.absinthe.libchecker.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.features.applist.LocatedCount
import com.absinthe.libchecker.utils.extensions.putArguments
import rikka.core.util.ClipboardUtils

class MetaDataAnalysisFragment : BaseDetailFragment<FragmentLibNativeBinding>() {

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = false

  override fun init() {
    binding.apply {
      list.apply {
        adapter = this@MetaDataAnalysisFragment.adapter
      }
    }

    viewModel.metaDataItems.observe(viewLifecycleOwner) {
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
        viewModel.itemsCountLiveData.value = LocatedCount(locate = type, count = it.size)
        viewModel.itemsCountList[type] = it.size
        isListReady = true
      }
    }

    adapter.apply {
      animationEnable = true
      setOnItemLongClickListener { _, _, position ->
        ClipboardUtils.put(
          requireContext(),
          getItem(position).item.name + ": " + getItem(position).item.source
        )
        VersionCompat.showCopiedOnClipboardToast(context)
        true
      }
      setDiffCallback(LibStringDiffUtil())
      setEmptyView(emptyView)
    }

    viewModel.packageInfoLiveData.observe(viewLifecycleOwner) {
      if (it != null) {
        viewModel.initMetaDataData()
      }
    }
  }

  override fun getFilterListByText(text: String): List<LibStringItemChip>? {
    return viewModel.metaDataItems.value?.filter {
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
