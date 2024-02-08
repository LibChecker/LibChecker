package com.absinthe.libchecker.features.applist.detail.ui.impl

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.features.applist.LocatedCount
import com.absinthe.libchecker.features.applist.MODE_SORT_BY_LIB
import com.absinthe.libchecker.features.applist.Referable
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibStringDiffUtil
import com.absinthe.libchecker.features.applist.detail.ui.base.BaseFilterAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.features.statistics.bean.DISABLED
import com.absinthe.libchecker.features.statistics.bean.EXPORTED
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.rulesbundle.LCRules
import com.absinthe.rulesbundle.Rule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ComponentsAnalysisFragment : BaseFilterAnalysisFragment<FragmentLibComponentBinding>(), Referable {

  private var itemsList: List<LibStringItemChip>? = null

  override fun getRecyclerView(): RecyclerView = binding.list
  override val needShowLibDetailDialog: Boolean = true

  override fun init() {
    binding.list.adapter = adapter

    viewModel.apply {
      componentsMap.get(adapter.type).observe(viewLifecycleOwner) { componentList ->
        if (componentList.isEmpty()) {
          emptyView.text.text = getString(R.string.empty_list)
        } else {
          if (!componentList.isNullOrEmpty()) {
            adapter.processMap = viewModel.processesMap
          }
          lifecycleScope.launch(Dispatchers.IO) {
            val list = mutableListOf<LibStringItemChip>()
            var rule: Rule?
            var source: String?

            for (item in componentList) {
              rule = if (!item.componentName.startsWith(".")) {
                LCRules.getRule(item.componentName, adapter.type, true)
              } else {
                null
              }
              source = if (!item.enabled) {
                DISABLED
              } else if (item.exported) {
                EXPORTED
              } else {
                null
              }

              list += LibStringItemChip(
                LibStringItem(
                  name = item.componentName,
                  source = source,
                  process = item.processName.takeIf { it.isNotEmpty() }
                ),
                rule
              )
            }

            if (sortMode == MODE_SORT_BY_LIB) {
              list.sortWith(compareByDescending<LibStringItemChip> { it.rule != null }.thenBy { it.item.name })
            } else {
              list.sortBy { it.item.name }
            }
            itemsList = list

            if (viewModel.queriedText?.isNotEmpty() == true) {
              filterList(viewModel.queriedText!!)
            } else {
              withContext(Dispatchers.Main) {
                context?.let {
                  binding.list.addItemDecoration(dividerItemDecoration)
                }
                adapter.setDiffNewData(list, afterListReadyTask)
              }
            }

            if (viewModel.queriedProcess?.isNotEmpty() == true) {
              filterItems(viewModel.queriedProcess!!)
            }
          }
        }
        if (!isListReady) {
          viewModel.itemsCountLiveData.value =
            LocatedCount(locate = type, count = componentList.size)
          viewModel.itemsCountList[type] = componentList.size
          isListReady = true
        }
      }
    }

    adapter.apply {
      setDiffCallback(LibStringDiffUtil())
      setEmptyView(emptyView)
    }
  }

  override fun getFilterListByText(text: String): List<LibStringItemChip>? {
    return itemsList?.filter { it.item.name.contains(text, true) }
  }

  override fun getFilterList(process: String?): List<LibStringItemChip>? {
    return if (process.isNullOrEmpty()) {
      itemsList
    } else {
      itemsList?.filter { it.item.process == process }
    }
  }

  companion object {
    fun newInstance(@LibType type: Int): ComponentsAnalysisFragment {
      return ComponentsAnalysisFragment().putArguments(EXTRA_TYPE to type)
    }
  }
}
