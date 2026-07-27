package com.absinthe.libchecker.domain.app.detail.ui.impl

import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.ui.base.BaseDetailFragment
import com.absinthe.libchecker.domain.app.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.utils.extensions.putArguments
import kotlinx.coroutines.launch
import rikka.core.util.ClipboardUtils

class AbilityAnalysisFragment : BaseDetailFragment<FragmentLibComponentBinding>() {

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = false

  override suspend fun getItems(): List<LibStringItemChip> {
    return viewModel.contentState.abilitiesMap[type].valueOrAwait()
  }

  override fun onItemsAvailable(items: List<LibStringItemChip>) {
    if (items.isEmpty()) {
      emptyView.text.text = getString(R.string.empty_list)
    } else {
      submitItemsWithFilter(items, viewModel.filterState.queriedText, viewModel.filterState.queriedProcess)
    }
    markListReady(items.size)
  }

  override fun init() {
    binding.list.adapter = adapter
    val flow = viewModel.contentState.abilitiesMap[type]

    adapter.apply {
      animationEnable = false
      setOnItemLongClickListener { _, _, position ->
        val context = requireContext()
        ClipboardUtils.put(context, getItem(position).item.name)
        VersionCompat.showCopiedOnClipboardToast(context)
        true
      }
      stateView = this@AbilityAnalysisFragment.emptyView
      isStateViewEnable = true
    }

    if (flow?.value?.isNotEmpty() == true) {
      lifecycleScope.launch {
        flow.emit(flow.value)
      }
    }
  }

  companion object {
    fun newInstance(@LibType type: Int): AbilityAnalysisFragment {
      return AbilityAnalysisFragment().putArguments(
        EXTRA_TYPE to type
      )
    }
  }
}
