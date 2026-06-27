package com.absinthe.libchecker.features.applist.detail.ui.impl

import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.ui.Referable
import com.absinthe.libchecker.domain.app.detail.ui.base.BaseDetailFragment
import com.absinthe.libchecker.domain.app.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibStringDiffUtil
import com.absinthe.libchecker.utils.extensions.putArguments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

class PermissionAnalysisFragment :
  BaseDetailFragment<FragmentLibComponentBinding>(),
  Referable {

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = true

  override suspend fun getItems(): List<LibStringItemChip> {
    val flow = viewModel.contentState.permissionsItems
    return flow.value ?: flow.filterNotNull().first()
  }

  override fun onItemsAvailable(items: List<LibStringItemChip>) {
    Timber.d("onItemsAvailable: ${items.size}")
    if (items.isEmpty()) {
      emptyView.text.text = getString(R.string.empty_list)
    } else {
      lifecycleScope.launch(Dispatchers.IO) {
        setItemsWithFilter(items, viewModel.filterState.queriedText, null)
      }
    }

    if (!isListReady) {
      viewModel.filterState.updateItemsCount(type, items.size)
      isListReady = true
    }
  }

  override fun init() {
    binding.apply {
      list.apply {
        adapter = this@PermissionAnalysisFragment.adapter
      }
    }

    adapter.apply {
      animationEnable = false
      setDiffCallback(LibStringDiffUtil())
      stateView = this@PermissionAnalysisFragment.emptyView
      isStateViewEnable = true
    }

    viewModel.apply {
      packageInfoStateFlow.onEach {
        if (it != null) {
          viewModel.initPermissionData()
        }
      }.launchIn(lifecycleScope)

      packageInfoStateFlow.value?.run {
        contentState.permissionsItems.value ?: run { initPermissionData() }
      }
    }
  }

  override fun filterItems(
    items: List<LibStringItemChip>,
    searchWords: String?,
    process: String?
  ): List<LibStringItemChip> {
    return viewModel.filterPermissionDetailItems(items, searchWords, process)
  }

  companion object {
    fun newInstance(packageName: String): PermissionAnalysisFragment {
      return PermissionAnalysisFragment().putArguments(
        EXTRA_PACKAGE_NAME to packageName,
        EXTRA_TYPE to PERMISSION
      )
    }
  }
}
