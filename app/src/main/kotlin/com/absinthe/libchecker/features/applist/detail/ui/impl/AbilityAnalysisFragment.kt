package com.absinthe.libchecker.features.applist.detail.ui.impl

import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.features.applist.MODE_SORT_BY_LIB
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibStringDiffUtil
import com.absinthe.libchecker.features.applist.detail.ui.base.BaseDetailFragment
import com.absinthe.libchecker.features.applist.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.features.statistics.bean.DISABLED
import com.absinthe.libchecker.features.statistics.bean.EXPORTED
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.extensions.putArguments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.core.util.ClipboardUtils

class AbilityAnalysisFragment : BaseDetailFragment<FragmentLibComponentBinding>() {

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = false

  override fun init() {
    binding.apply {
      list.apply {
        adapter = this@AbilityAnalysisFragment.adapter
      }
    }
    val flow = viewModel.abilitiesMap[adapter.type]

    viewModel.apply {
      flow?.onEach { componentList ->
        if (componentList.isEmpty()) {
          emptyView.text.text = getString(R.string.empty_list)
        } else {
          lifecycleScope.launch(Dispatchers.Default) {
            val list = mutableListOf<LibStringItemChip>()

            for (item in componentList) {
              val source = if (!item.enabled) {
                DISABLED
              } else if (item.exported) {
                EXPORTED
              } else {
                null
              }

              list += LibStringItemChip(
                LibStringItem(
                  name = item.componentName,
                  source = source
                ),
                null
              )
            }

            if (GlobalValues.libSortMode == MODE_SORT_BY_LIB) {
              list.sortByDescending { it.rule != null }
            } else {
              adapter.data.sortedByDescending { it.item.name }
            }

            if (viewModel.queriedText?.isNotEmpty() == true) {
              filterList(viewModel.queriedText!!)
            } else {
              withContext(Dispatchers.Main) {
                context?.let {
                  binding.list.addItemDecoration(dividerItemDecoration)
                }
                setList(list)
              }
            }
          }
        }
        if (!isListReady) {
          viewModel.updateItemsCountStateFlow(type, componentList.size)
          isListReady = true
        }
      }?.launchIn(lifecycleScope)
    }

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
