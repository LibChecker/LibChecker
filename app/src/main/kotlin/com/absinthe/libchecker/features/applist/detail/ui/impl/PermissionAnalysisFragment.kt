package com.absinthe.libchecker.features.applist.detail.ui.impl

import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.features.applist.LocatedCount
import com.absinthe.libchecker.features.applist.Referable
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibStringDiffUtil
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.ui.base.BaseFilterAnalysisFragment
import com.absinthe.libchecker.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.putArguments

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

    viewModel.permissionsItems.observe(viewLifecycleOwner) {
      if (it.isEmpty()) {
        emptyView.text.text = getString(R.string.empty_list)
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
      setDiffCallback(LibStringDiffUtil())
      setEmptyView(emptyView)
    }

    viewModel.packageInfoLiveData.observe(viewLifecycleOwner) {
      if (it != null) {
        viewModel.initPermissionData()
      }
    }
  }

  override fun onVisibilityChanged(visible: Boolean) {
    super.onVisibilityChanged(visible)
    if (context != null && visible) {
      if (hasNonGrantedPermissions()) {
        val label = requireContext().getString(R.string.permission_not_granted)
        val color = R.color.material_red_400.getColor(requireContext())
        viewModel.processMapLiveData.postValue(
          mapOf(label to color)
        )
      } else {
        viewModel.processMapLiveData.postValue(emptyMap())
      }
    } else {
      viewModel.processMapLiveData.postValue(viewModel.processesMap)
    }
  }

  override fun getFilterListByText(text: String): List<LibStringItemChip>? {
    return viewModel.permissionsItems.value?.filter { it.item.name.contains(text, true) }
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
