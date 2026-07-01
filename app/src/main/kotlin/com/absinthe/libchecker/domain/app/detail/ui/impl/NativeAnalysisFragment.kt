package com.absinthe.libchecker.domain.app.detail.ui.impl

import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.Constants.MULTI_ARCH
import com.absinthe.libchecker.databinding.FragmentLibNativeBinding
import com.absinthe.libchecker.domain.app.detail.TRACE_DETAIL_NATIVE_SET_ITEMS
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.traceDetailSuspendSection
import com.absinthe.libchecker.domain.app.detail.ui.Referable
import com.absinthe.libchecker.domain.app.detail.ui.base.BaseDetailFragment
import com.absinthe.libchecker.domain.app.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.utils.extensions.ABI_STRING_MAP
import com.absinthe.libchecker.utils.extensions.putArguments
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class NativeAnalysisFragment :
  BaseDetailFragment<FragmentLibNativeBinding>(),
  Referable {

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = true
  override val autoLoadItems = false

  override suspend fun getItems(): List<LibStringItemChip> {
    val flow = viewModel.contentState.nativeLibItems
    return flow.value ?: flow.filterNotNull().first()
  }

  override fun onItemsAvailable(items: List<LibStringItemChip>) {
    setItems(items)
  }

  override fun init() {
    binding.apply {
      list.apply {
        adapter = this@NativeAnalysisFragment.adapter
      }
      tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
          viewModel.loadSoAnalysisData(tab.text.toString())
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {}
        override fun onTabReselected(tab: TabLayout.Tab?) {}
      })
    }

    adapter.apply {
      animationEnable = false
      stateView = this@NativeAnalysisFragment.emptyView
      isStateViewEnable = true
    }

    viewModel.apply {
      packageInfoStateFlow.onEach {
        if (it != null) {
          viewModel.initSoAnalysisData()
        }
      }.launchIn(lifecycleScope)
      featureState.is64Bit.onEach {
        if (it != null) {
          adapter.set64Bit(it)
        }
      }.launchIn(lifecycleScope)
      contentState.nativeLibTabs.onEach {
        binding.tabLayout.removeAllTabs()
        it?.forEach { title ->
          binding.tabLayout.addTab(binding.tabLayout.newTab().setText(title), false)
        }
        binding.tabLayout.isVisible = binding.tabLayout.tabCount > 1
        lifecycleScope.launch {
          val flow = viewModel.featureState.abiBundleStateFlow
          val abi = (flow.value ?: flow.filterNotNull().first()).abi
          selectTabByAbi(abi)
        }
      }.launchIn(lifecycleScope)
      contentState.nativeLibItems.onEach {
        if (it != null) {
          setItems(it)
        }
      }.launchIn(lifecycleScope)

      packageInfoStateFlow.value?.run {
        contentState.nativeLibItems.value ?: run { initSoAnalysisData() }
      }
    }
  }

  private fun setItems(list: List<LibStringItemChip>) {
    if (list.isEmpty()) {
      emptyView.text.text = getString(R.string.empty_list)
    } else {
      lifecycleScope.launch {
        traceDetailSuspendSection(TRACE_DETAIL_NATIVE_SET_ITEMS) {
          setItemsWithFilter(list, viewModel.filterState.queriedText, viewModel.filterState.queriedProcess)
        }
      }
    }

    if (!isListReady) {
      viewModel.filterState.updateItemsCount(type, list.size)
      isListReady = true
    }
  }

  companion object {
    fun newInstance(packageName: String): NativeAnalysisFragment {
      return NativeAnalysisFragment().putArguments(
        EXTRA_PACKAGE_NAME to packageName,
        EXTRA_TYPE to NATIVE
      )
    }
  }

  private fun selectTabByAbi(abi: Int) {
    (0..binding.tabLayout.tabCount).forEach {
      val tab = binding.tabLayout.getTabAt(it)
      if (tab?.text == ABI_STRING_MAP[abi % MULTI_ARCH]) {
        tab?.select()
      }
    }
  }
}
