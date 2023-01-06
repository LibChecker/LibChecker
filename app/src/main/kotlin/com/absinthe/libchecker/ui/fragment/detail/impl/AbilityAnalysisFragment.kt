package com.absinthe.libchecker.ui.fragment.detail.impl

import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.bean.DISABLED
import com.absinthe.libchecker.bean.EXPORTED
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.recyclerview.diff.LibStringDiffUtil
import com.absinthe.libchecker.ui.fragment.BaseDetailFragment
import com.absinthe.libchecker.ui.fragment.EXTRA_TYPE
import com.absinthe.libchecker.ui.fragment.detail.LocatedCount
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_LIB
import com.absinthe.libchecker.utils.extensions.putArguments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.core.util.ClipboardUtils

class AbilityAnalysisFragment : BaseDetailFragment<FragmentLibComponentBinding>() {

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = false

  private var itemsList: List<LibStringItemChip>? = null

  override fun init() {
    binding.apply {
      list.apply {
        adapter = this@AbilityAnalysisFragment.adapter
      }
    }

    viewModel.apply {
      abilitiesMap[adapter.type]?.observe(viewLifecycleOwner) { componentList ->
        if (componentList.isEmpty()) {
          emptyView.text.text = getString(R.string.empty_list)
        } else {
          lifecycleScope.launch(Dispatchers.Default) {
            val list = mutableListOf<LibStringItemChip>()

            for (item in componentList) {
              val source = if (!item.enabled) DISABLED else if (item.exported) EXPORTED else null

              list += LibStringItemChip(
                LibStringItem(
                  name = item.componentName,
                  source = source
                ),
                null
              )
            }

            if (sortMode == MODE_SORT_BY_LIB) {
              list.sortByDescending { it.chip != null }
            } else {
              adapter.data.sortedByDescending { it.item.name }
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
      setOnItemLongClickListener { _, _, position ->
        doOnLongClick(getItem(position).item.name)
        true
      }
      setDiffCallback(LibStringDiffUtil())
      setEmptyView(emptyView)
    }
  }

  override fun getFilterListByText(text: String): List<LibStringItemChip>? {
    return itemsList?.filter { it.item.name.contains(text, true) }
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
