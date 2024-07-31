package com.absinthe.libchecker.features.applist.detail.ui.impl

import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.features.applist.Referable
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibStringDiffUtil
import com.absinthe.libchecker.features.applist.detail.ui.base.BaseFilterAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.putArguments
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class PermissionAnalysisFragment :
  BaseFilterAnalysisFragment<FragmentLibComponentBinding>(),
  Referable {

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = true

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
        if (it.isEmpty()) {
          emptyView.text.text = getString(R.string.empty_list)
        } else {
          if (viewModel.queriedText?.isNotEmpty() == true) {
            filterList(viewModel.queriedText!!)
          } else {
            context?.let {
              binding.list.addItemDecoration(dividerItemDecoration)
            }
            setList(it)
          }
        }

        if (!isListReady) {
          viewModel.updateItemsCountStateFlow(type, it.size)
          isListReady = true
        }
      }.launchIn(lifecycleScope)
      packageInfoStateFlow.onEach {
        if (it != null) {
          viewModel.initPermissionData()
        }
      }.launchIn(lifecycleScope)
    }

    if (viewModel.permissionsItems.value.isNullOrEmpty().not()) {
      lifecycleScope.launch {
        viewModel.permissionsItems.emit(viewModel.permissionsItems.value)
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

  override fun getFilterList(process: String?): List<LibStringItemChip>? {
    return if (process.isNullOrEmpty()) {
      viewModel.permissionsItems.value
    } else {
      viewModel.permissionsItems.value?.filter { it.item.size == 0L }
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
