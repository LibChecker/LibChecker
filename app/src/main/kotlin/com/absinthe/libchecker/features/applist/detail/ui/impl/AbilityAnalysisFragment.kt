package com.absinthe.libchecker.features.applist.detail.ui.impl

import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibStringDiffUtil
import com.absinthe.libchecker.features.applist.detail.ui.base.BaseDetailFragment
import com.absinthe.libchecker.features.applist.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.features.statistics.bean.DISABLED
import com.absinthe.libchecker.features.statistics.bean.EXPORTED
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.extensions.putArguments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import rikka.core.util.ClipboardUtils

class AbilityAnalysisFragment : BaseDetailFragment<FragmentLibComponentBinding>() {

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = false
  private var items: List<LibStringItemChip>? = null

  override suspend fun getItems(): List<LibStringItemChip> {
    return items ?: run {
      val flow = viewModel.abilitiesMap[adapter.type]
      items = (flow.value ?: flow?.first() ?: emptyList())
        .map { item ->
          val source = when {
            !item.enabled -> DISABLED
            item.exported -> EXPORTED
            else -> null
          }

          LibStringItemChip(
            LibStringItem(
              name = item.componentName,
              source = source
            ),
            null
          )
        }
      return items!!
    }
  }

  override fun onItemsAvailable(items: List<LibStringItemChip>) {
    if (items.isEmpty()) {
      emptyView.text.text = getString(R.string.empty_list)
    } else {
      lifecycleScope.launch(Dispatchers.IO) {
        setItemsWithFilter(viewModel.queriedText, viewModel.queriedProcess)
      }
    }
    if (!isListReady) {
      viewModel.updateItemsCountStateFlow(type, items.size)
      isListReady = true
    }
  }

  override fun init() {
    binding.apply {
      list.apply {
        adapter = this@AbilityAnalysisFragment.adapter
      }
    }
    val flow = viewModel.abilitiesMap[adapter.type]

    adapter.apply {
      setOnItemLongClickListener { _, _, position ->
        doOnLongClick(getItem(position).item.name)
        true
      }
      setDiffCallback(LibStringDiffUtil())
      setEmptyView(emptyView)
    }

    if (flow?.value?.isNotEmpty() == true) {
      lifecycleScope.launch {
        flow.emit(flow.value)
      }
    }
  }

  private fun doOnLongClick(componentName: String) {
    ClipboardUtils.put(requireContext(), componentName)
    context?.let {
      VersionCompat.showCopiedOnClipboardToast(it)
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
