package com.absinthe.libchecker.features.applist.detail.ui.impl

import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.SIGNATURES
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.features.applist.detail.ui.SignatureDetailBSDFragment
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibStringDiffUtil
import com.absinthe.libchecker.features.applist.detail.ui.base.BaseDetailFragment
import com.absinthe.libchecker.features.applist.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import rikka.core.util.ClipboardUtils

class SignaturesAnalysisFragment : BaseDetailFragment<FragmentLibComponentBinding>() {

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = false

  override suspend fun getItems(): List<LibStringItemChip> {
    val flow = viewModel.signaturesLibItems
    return flow.value ?: flow.filterNotNull().first()
  }

  override fun onItemsAvailable(items: List<LibStringItemChip>) {
    if (items.isEmpty()) {
      emptyView.text.text = getString(R.string.uncharted_territory)
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
    binding.list.adapter = adapter

    adapter.apply {
      animationEnable = true
      setOnItemClickListener { _, view, position ->
        if (AntiShakeUtils.isInvalidClick(view)) {
          return@setOnItemClickListener
        }
        openSignatureDetailDialog(position)
      }
      setOnItemLongClickListener { _, _, position ->
        ClipboardUtils.put(requireContext(), getItem(position).item.source)
        VersionCompat.showCopiedOnClipboardToast(context)
        true
      }
      setDiffCallback(LibStringDiffUtil())
      setEmptyView(emptyView)
    }

    viewModel.apply {
      packageInfoStateFlow.onEach {
        if (it != null) {
          viewModel.initSignatures(requireContext())
        }
      }.launchIn(lifecycleScope)

      packageInfoStateFlow.value?.run {
        signaturesLibItems.value ?: run { initSignatures(requireContext()) }
      }
    }
  }

  private fun openSignatureDetailDialog(position: Int) {
    val source = adapter.getItem(position).item.source
    SignatureDetailBSDFragment.newInstance(source.orEmpty())
      .show(childFragmentManager, SignatureDetailBSDFragment::class.java.name)
  }

  companion object {
    fun newInstance(packageName: String): SignaturesAnalysisFragment {
      return SignaturesAnalysisFragment().putArguments(
        EXTRA_PACKAGE_NAME to packageName,
        EXTRA_TYPE to SIGNATURES
      )
    }
  }
}
