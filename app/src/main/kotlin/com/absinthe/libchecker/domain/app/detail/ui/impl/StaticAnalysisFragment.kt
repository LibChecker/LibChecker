package com.absinthe.libchecker.domain.app.detail.ui.impl

import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.ui.base.BaseDetailFragment
import com.absinthe.libchecker.domain.app.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.utils.extensions.putArguments
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import rikka.core.util.ClipboardUtils

class StaticAnalysisFragment : BaseDetailFragment<FragmentLibComponentBinding>() {

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = true

  override suspend fun getItems(): List<LibStringItemChip> {
    return viewModel.contentState.staticLibItems.valueOrAwait()
  }

  override fun onItemsAvailable(items: List<LibStringItemChip>) {
    showInitialItems(items)
  }

  override fun init() {
    initializeList()

    adapter.apply {
      setOnItemLongClickListener { _, _, position ->
        ClipboardUtils.put(requireContext(), getItem(position).item.name)
        VersionCompat.showCopiedOnClipboardToast(context)
        true
      }
    }

    viewModel.packageInfoStateFlow.onEach {
      if (it != null) {
        viewModel.initStaticData()
      }
    }.launchIn(lifecycleScope)

    if (viewModel.packageInfoStateFlow.value != null && viewModel.contentState.staticLibItems.value == null) {
      viewModel.initStaticData()
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
