package com.absinthe.libchecker.features.applist.detail.ui.impl

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.features.applist.Referable
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibStringDiffUtil
import com.absinthe.libchecker.features.applist.detail.ui.base.BaseDetailFragment
import com.absinthe.libchecker.features.applist.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.features.statistics.bean.DISABLED
import com.absinthe.libchecker.features.statistics.bean.EXPORTED
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.rulesbundle.LCRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ComponentsAnalysisFragment :
  BaseDetailFragment<FragmentLibComponentBinding>(),
  Referable {

  private var itemsList: List<LibStringItemChip>? = null
  override val needShowLibDetailDialog: Boolean = true

  override fun getRecyclerView(): RecyclerView = binding.list

  override suspend fun getItems(): List<LibStringItemChip> {
    return itemsList ?: run {
      val flow = viewModel.componentsMap[adapter.type]
      itemsList = (flow.value ?: flow?.first() ?: emptyList())
        .map { item ->
          val rule = if (!item.componentName.startsWith(".")) {
            LCRules.getRule(item.componentName, adapter.type, true)
          } else {
            null
          }
          val source = when {
            !item.enabled -> DISABLED
            item.exported -> EXPORTED
            else -> null
          }

          LibStringItemChip(
            LibStringItem(
              name = item.componentName,
              source = source,
              process = item.processName.takeIf { it.isNotEmpty() }
            ),
            rule
          )
        }
      return itemsList!!
    }
  }

  override fun onItemsAvailable(items: List<LibStringItemChip>) {
    if (items.isEmpty()) {
      emptyView.text.text = getString(R.string.empty_list)
    } else {
      adapter.processMap = viewModel.processesMap
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
    binding.list.adapter = adapter

    adapter.apply {
      setDiffCallback(LibStringDiffUtil())
      setEmptyView(emptyView)
    }
  }

  companion object {
    fun newInstance(@LibType type: Int): ComponentsAnalysisFragment {
      return ComponentsAnalysisFragment().putArguments(EXTRA_TYPE to type)
    }
  }
}
