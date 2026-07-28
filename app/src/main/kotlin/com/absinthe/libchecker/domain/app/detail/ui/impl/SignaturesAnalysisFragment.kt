package com.absinthe.libchecker.domain.app.detail.ui.impl

import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.SIGNATURES
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.databinding.FragmentLibComponentBinding
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.ui.base.BaseDetailFragment
import com.absinthe.libchecker.domain.app.detail.ui.base.EXTRA_TYPE
import com.absinthe.libchecker.domain.app.detail.ui.dialog.SignatureDetailBSDFragment
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import rikka.core.util.ClipboardUtils

class SignaturesAnalysisFragment : BaseDetailFragment<FragmentLibComponentBinding>() {

  override fun getRecyclerView() = binding.list
  override val needShowLibDetailDialog = false

  override suspend fun getItems(): List<LibStringItemChip> {
    return viewModel.contentState.signaturesLibItems.valueOrAwait()
  }

  override fun onItemsAvailable(items: List<LibStringItemChip>) {
    if (items.isEmpty()) {
      emptyView.text.text = getString(R.string.uncharted_territory)
    } else {
      submitItemsWithFilter(items, viewModel.filterState.queriedText, null)
    }

    markListReady(items.size)
  }

  override fun init() {
    binding.list.adapter = adapter

    adapter.apply {
      animationEnable = false
      setOnItemClickListener { _, view, position ->
        if (AntiShakeUtils.isInvalidClick(view)) {
          return@setOnItemClickListener
        }
        val source = getItem(position).item.source
        SignatureDetailBSDFragment.newInstance(source.orEmpty())
          .show(childFragmentManager, SignatureDetailBSDFragment::class.java.name)
      }
      setOnItemLongClickListener { _, _, position ->
        ClipboardUtils.put(requireContext(), getItem(position).item.source)
        VersionCompat.showCopiedOnClipboardToast(context)
        true
      }
      stateView = this@SignaturesAnalysisFragment.emptyView
      isStateViewEnable = true
    }

    viewModel.packageInfoStateFlow.onEach {
      if (it != null) {
        viewModel.initSignatures()
      }
    }.launchIn(lifecycleScope)

    if (viewModel.packageInfoStateFlow.value != null && viewModel.contentState.signaturesLibItems.value == null) {
      viewModel.initSignatures()
    }
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
