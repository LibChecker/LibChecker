package com.absinthe.libchecker.ui.fragment.detail.impl

import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.recyclerview.diff.LibStringDiffUtil
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.fragment.BaseDetailFragment
import com.absinthe.libchecker.ui.fragment.EXTRA_TYPE
import com.absinthe.libchecker.ui.fragment.detail.LocatedCount
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libchecker.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.core.util.ClipboardUtils

class DexAnalysisFragment : BaseDetailFragment<FragmentLibComponentBinding>() {

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = false

  override fun init() {
    binding.apply {
      list.apply {
        adapter = this@DexAnalysisFragment.adapter
      }
    }

    viewModel.dexLibItems.observe(viewLifecycleOwner) {
      if (it.isEmpty()) {
        emptyView.text.text = getString(R.string.uncharted_territory)
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
        ClipboardUtils.put(requireContext(), getItem(position).item.name)
        context.showToast(R.string.toast_copied_to_clipboard)
        true
      }
      setDiffCallback(LibStringDiffUtil())
      setEmptyView(emptyView)
    }
    viewModel.initDexData(packageName)
  }

  override fun getFilterList(text: String): List<LibStringItemChip>? {
    return viewModel.dexLibItems.value?.filter { it.item.name.contains(text, true) }
  }

  override fun onDetach() {
    super.onDetach()
    viewModel.initDexJob?.cancel()
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
