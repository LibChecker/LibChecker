package com.absinthe.libchecker.features.applist.detail.ui.impl

import android.content.pm.PackageInfo
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.features.applist.Referable
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibStringDiffUtil
import com.absinthe.libchecker.features.applist.detail.ui.base.BaseDetailFragment
import com.absinthe.libchecker.features.applist.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.putArguments
import kotlinx.coroutines.Dispatchers
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
    return viewModel.permissionsItems.value ?: viewModel.permissionsItems.first() ?: emptyList()
  }

  override fun onItemsAvailable(items: List<LibStringItemChip>) {
    Timber.d("onItemsAvailable: ${items.size}")
    if (items.isEmpty()) {
      emptyView.text.text = getString(R.string.empty_list)
    } else {
      lifecycleScope.launch(Dispatchers.IO) {
        setItemsWithFilter(viewModel.queriedText, null)
      }
    }

    if (!isListReady) {
      viewModel.updateItemsCountStateFlow(type, items.size)
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
      animationEnable = true
      setDiffCallback(LibStringDiffUtil())
      setEmptyView(emptyView)
    }

    viewModel.apply {
      permissionsItems.onEach {
        if (it == null) return@onEach
        onItemsAvailable(it)
      }.launchIn(lifecycleScope)
      packageInfoStateFlow.onEach {
        if (it != null) {
          viewModel.initPermissionData()
        }
      }.launchIn(lifecycleScope)

      packageInfoStateFlow.value?.run {
        permissionsItems.value ?: run { initPermissionData() }
      }
    }
  }

  override fun onVisibilityChanged(visible: Boolean) {
    super.onVisibilityChanged(visible)
    if (context != null && visible) {
      if (hasNonGrantedPermissions()) {
        val label = requireContext().getString(R.string.permission_not_granted)
        val color = R.color.material_red_400.getColor(requireContext())
        viewModel.updateProcessMap(mapOf(label to color))
      } else {
        viewModel.updateProcessMap(emptyMap())
      }
    } else {
      viewModel.updateProcessMap(viewModel.processesMap)
    }
  }

  override suspend fun getFilterList(
    searchWords: String?,
    process: String?
  ): List<LibStringItemChip>? {
    Timber.d("getFilterList: $searchWords, $process")
    return getItems().asSequence()
      .filter { searchWords == null || it.item.name.contains(searchWords, true) || it.item.source?.contains(searchWords, true) == true }
      .filter { process == null || it.item.process != PackageInfo.REQUESTED_PERMISSION_GRANTED.toString() }
      .toList()
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
