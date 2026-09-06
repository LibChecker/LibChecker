package com.absinthe.libchecker.domain.app.detail.ui.impl

import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.ui.Referable
import com.absinthe.libchecker.domain.app.detail.ui.base.BaseDetailFragment
import com.absinthe.libchecker.domain.app.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.utils.extensions.putArguments
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

class PermissionAnalysisFragment :
  BaseDetailFragment<FragmentLibComponentBinding>(),
  Referable {

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = true

  override suspend fun getItems(): List<LibStringItemChip> {
    return viewModel.contentState.permissionsItems.valueOrAwait()
  }

  override fun onItemsAvailable(items: List<LibStringItemChip>) {
    Timber.d("onItemsAvailable: ${items.size}")
    showInitialItems(items)
  }

  override fun init() {
    initializeList()

    viewModel.packageInfoStateFlow.onEach {
      if (it != null) {
        viewModel.initPermissionData()
      }
    }.launchIn(lifecycleScope)

    if (viewModel.packageInfoStateFlow.value != null && viewModel.contentState.permissionsItems.value == null) {
      viewModel.initPermissionData()
    }
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
