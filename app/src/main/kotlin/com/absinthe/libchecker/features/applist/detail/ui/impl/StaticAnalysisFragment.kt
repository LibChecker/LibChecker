package com.absinthe.libchecker.features.applist.detail.ui.impl

import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.databinding.FragmentLibNativeBinding
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibStringDiffUtil
import com.absinthe.libchecker.features.applist.detail.ui.base.BaseDetailFragment
import com.absinthe.libchecker.features.applist.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.utils.extensions.putArguments
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import rikka.core.util.ClipboardUtils

class StaticAnalysisFragment : BaseDetailFragment<FragmentLibNativeBinding>() {

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = true

  override fun init() {
    binding.apply {
      list.apply {
        adapter = this@StaticAnalysisFragment.adapter
      }
    }

    adapter.apply {
      animationEnable = true
      setOnItemLongClickListener { _, _, position ->
        ClipboardUtils.put(requireContext(), getItem(position).item.name)
        VersionCompat.showCopiedOnClipboardToast(context)
        true
      }
      setDiffCallback(LibStringDiffUtil())
      setEmptyView(emptyView)
    }

    viewModel.apply {
      staticLibItems.onEach {
        if (it == null) return@onEach
        if (it.isEmpty()) {
          emptyView.text.text = getString(R.string.empty_list)
        } else {
          if (viewModel.queriedText?.isNotEmpty() == true) {
            filterList(viewModel.queriedText!!)
          } else {
            setList(it)
            context?.let {
              binding.list.addItemDecoration(dividerItemDecoration)
            }
            adapter.setDiffNewData(it.toMutableList(), afterListReadyTask)
          }
        }

        if (!isListReady) {
          viewModel.updateItemsCountStateFlow(type, it.size)
          isListReady = true
        }
      }.launchIn(lifecycleScope)
      packageInfoStateFlow.onEach {
        if (it != null) {
          viewModel.initStaticData()
        }
      }.launchIn(lifecycleScope)
    }

    if (viewModel.staticLibItems.value.isNullOrEmpty().not()) {
      lifecycleScope.launch {
        viewModel.staticLibItems.emit(viewModel.staticLibItems.value)
      }
    }
  }

  companion object {
    fun newInstance(packageName: String): StaticAnalysisFragment {
      return StaticAnalysisFragment().putArguments(
        EXTRA_PACKAGE_NAME to packageName,
        EXTRA_TYPE to STATIC
      )
    }
  }
}
