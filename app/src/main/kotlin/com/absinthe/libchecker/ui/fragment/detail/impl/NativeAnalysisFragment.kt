package com.absinthe.libchecker.ui.fragment.detail.impl

import android.view.ViewGroup
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.databinding.FragmentLibNativeBinding
import com.absinthe.libchecker.recyclerview.diff.LibStringDiffUtil
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.fragment.BaseDetailFragment
import com.absinthe.libchecker.ui.fragment.EXTRA_TYPE
import com.absinthe.libchecker.ui.fragment.detail.LocatedCount
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libchecker.view.detail.NativeLibExtractTipView
import rikka.core.util.ClipboardUtils

class NativeAnalysisFragment : BaseDetailFragment<FragmentLibNativeBinding>() {

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
        if (viewModel.extractNativeLibs == false) {
          context?.let { ctx ->
            adapter.setHeaderView(
              NativeLibExtractTipView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT,
                  ViewGroup.LayoutParams.WRAP_CONTENT
                )
              }
            )
          }
        }

        if (viewModel.queriedText?.isNotEmpty() == true) {
          filterList(viewModel.queriedText!!)
        } else {
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
        ClipboardUtils.put(context, getItem(position).item.name)
        context.showToast(R.string.toast_copied_to_clipboard)
        true
      }
      setDiffCallback(LibStringDiffUtil())
      setEmptyView(emptyView)
    }

    viewModel.packageInfoLiveData.observe(viewLifecycleOwner) {
      if (it != null) {
        viewModel.initSoAnalysisData()
      }
    }
  }

  override fun getFilterList(text: String): List<LibStringItemChip>? {
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
