package com.absinthe.libchecker.domain.app.detail.ui.impl

import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.ui.base.BaseDetailFragment
import com.absinthe.libchecker.domain.app.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.utils.extensions.putArguments
import rikka.core.util.ClipboardUtils

class DexAnalysisFragment : BaseDetailFragment<FragmentLibComponentBinding>() {

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = false

  override suspend fun getItems(): List<LibStringItemChip> {
    return viewModel.contentState.dexLibItems.valueOrAwait()
  }

  override fun onItemsAvailable(items: List<LibStringItemChip>) {
    if (items.isEmpty()) {
      emptyView.text.text = getString(R.string.uncharted_territory)
    } else {
      submitItemsWithFilter(items, viewModel.filterState.queriedText, null)
    }

    markListReady(items.size)
  }

  override fun init() {
    binding.list.adapter = adapter

    adapter.apply {
      animationEnable = false
      setOnItemLongClickListener { _, _, position ->
        ClipboardUtils.put(requireContext(), getItem(position).item.name)
        VersionCompat.showCopiedOnClipboardToast(context)
        true
      }
      stateView = this@DexAnalysisFragment.emptyView
      isStateViewEnable = true
    }

    if (viewModel.packageInfoStateFlow.value != null && viewModel.contentState.dexLibItems.value == null) {
      viewModel.initDexData()
    }
  }

  override fun onDetach() {
    super.onDetach()
    viewModel.cancelInitDexDataJob()
  }

  companion object {
    fun newInstance(packageName: String): DexAnalysisFragment {
      return DexAnalysisFragment().putArguments(
        EXTRA_PACKAGE_NAME to packageName,
        EXTRA_TYPE to DEX
      )
    }
  }
}
